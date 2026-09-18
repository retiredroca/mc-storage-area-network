#!/usr/bin/env python3
"""Local release driver.

Builds the release jars, commits the version bump, creates the GitHub tag + release, and then asks
CI to publish that release to CurseForge/Modrinth.

Typical use:

    python tools/release.py --mod all                 # build here, CI publishes it
    python tools/release.py --mod all --dry-run
    python tools/secrets.py run -- python tools/release.py --mod routing --curseforge --modrinth

By default the GitHub release happens and then the publish-only workflow
(.github/workflows/publish-release.yml) is dispatched to upload the jars to CurseForge/Modrinth;
--no-ci-publish skips that. Passing --curseforge / --modrinth uploads from this machine instead
(reading CURSEFORGE_API_KEY / MODRINTH_TOKEN from the environment - use secrets.py run to supply
them from the encrypted vault) and marks the release, so a CI run would skip it. The tag is a
single series `v<api 3 parts>.<stamp>` (e.g. `v1.0.2.26091512`).

The build reuses a warm Gradle daemon (the 12G one configured in the root gradle.properties), so
the six Gradle invocations do not each pay cold-start and reconfiguration of every included build.
Pass --no-daemon to reproduce a CI-like cold build instead.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

import versioning

ROOT = Path(__file__).resolve().parent.parent
VERSIONS = ROOT / "versions.properties"
STATE = ROOT / "release-state.properties"
DIST = ROOT / "dist"
UA = "retiredroca-release (github.com/retiredroca/mc-storage-area-network)"

MODS = ("api", "storage", "crafting", "routing", "access", "all")
COMPONENTS = ("api", "storage", "crafting", "routing", "access")
COMP_JAR = {"storage": "storage-network", "crafting": "crafting-network", "routing": "network-routing",
            "access": "remote-access-terminal"}
ALLOWED_JAR = re.compile(
    r"^(universal|fabric|neoforge)(_mc_san_api|-storage-network|-crafting-network|-network-routing"
    r"|-remote-access-terminal|-bundle-all|-bundle-storage|-bundle-crafting|-bundle-routing|-bundle-access)"
    r"\.[0-9].*\.jar$"
)


def log(msg):
    print(f"release: {msg}")


def die(msg):
    sys.exit(f"release: error: {msg}")


# --- process helpers ----------------------------------------------------------------


def run(cmd, dry=False, **kw):
    printable = " ".join(str(c) for c in cmd)
    if dry:
        print(f"  [dry-run] {printable}")
        return subprocess.CompletedProcess(cmd, 0, b"", b"")
    log(printable)
    return subprocess.run(cmd, cwd=ROOT, check=True, **kw)


def capture(cmd):
    return subprocess.run(cmd, cwd=ROOT, check=True, capture_output=True, text=True).stdout


def gradlew():
    # On Windows run the wrapper through cmd.exe: a bare "bash <win-path>" resolves to WSL's bash
    # (or a bash that strips the backslashes) and fails with "No such file or directory".
    if os.name == "nt":
        bat = ROOT / "gradlew.bat"
        if bat.exists():
            return ["cmd", "/c", str(bat)]
    return [str(ROOT / "gradlew")]


# --- versions.properties ------------------------------------------------------------


def read_props(path):
    props = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, _, value = line.partition("=")
            props[key.strip()] = value.strip()
    return props


def set_prop(text, key, value):
    # [^\r\n]* rather than .* so a CRLF file gets a clean LF-terminated replacement.
    pattern = re.compile(rf"(?m)^{re.escape(key)}=[^\r\n]*")
    if not pattern.search(text):
        die(f"versions.properties has no '{key}=' line")
    return pattern.sub(f"{key}={value}", text)


def bump(mod, stamp):
    text = VERSIONS.read_text(encoding="utf-8")
    current = read_props(VERSIONS)
    changed = {c: False for c in COMPONENTS}
    targets = COMPONENTS if mod == "all" else (mod,)
    for comp in targets:
        if comp not in COMPONENTS:
            die(f"unknown mod '{mod}'")
        text = set_prop(text, comp, versioning.bump(current[comp], stamp))
        changed[comp] = True
    # newline="\n": without it Windows text mode writes CRLF, and CI then reads the version
    # values with a trailing \r (which breaks mc-publish's file paths).
    VERSIONS.write_text(text, encoding="utf-8", newline="\n")
    log(f"bumped {', '.join(targets)} to stamp {stamp}")
    return changed


# --- build --------------------------------------------------------------------------


def build(mc, dry, no_daemon=False):
    g = gradlew()
    # Reuse the warm 12G daemon across these invocations by default: each one used to start a cold
    # JVM and reconfigure all 12 included builds. --no-daemon is available to reproduce a CI-like
    # cold build (CI always passes it, where each run is a fresh container).
    daemon = ["--no-daemon"] if no_daemon else []
    # Bootstrap: the API artifacts must be in repo/ before the hosts compile, because hosts
    # require an API version floor ([<api>,1.1)).
    run(g + daemon + ["-p", f"versions/{mc}/api/fabric",
             "publishMavenJavaPublicationToRepoRepository"], dry=dry)
    run(g + daemon + ["-p", f"versions/{mc}/api/neoforge",
             "publishMavenJavaPublicationToRepoRepository"], dry=dry)
    # Staged build (loader jars -> loader bundles -> universal -> universal bundles) as four
    # invocations, so a failure names the layer that broke. The last one also unions the staged
    # dirs into build/release/ and republishes the API to ./repo.
    # --refresh-dependencies: the floor range was just republished, so don't use a cached resolution.
    run(g + daemon + ["--console=plain", f"-Pmc={mc}", "--refresh-dependencies", "releaseLoaderJars"],
        dry=dry)
    run(g + daemon + ["--console=plain", f"-Pmc={mc}", "releaseLoaderBundles"], dry=dry)
    run(g + daemon + ["--console=plain", f"-Pmc={mc}", "releaseUniversal"], dry=dry)
    run(g + daemon + ["--console=plain", f"-Pmc={mc}", "releaseUniversalBundles", "releaseJars",
             "publishRepo"], dry=dry)


def collect(dry):
    if not dry:
        # Clear stale artifacts, but keep the tracked dist/.gitkeep.
        DIST.mkdir(exist_ok=True)
        for path in DIST.iterdir():
            if path.name == ".gitkeep":
                continue
            if path.is_dir():
                shutil.rmtree(path)
            else:
                path.unlink()
        release = ROOT / "build" / "release"
        jars = sorted(release.glob("*.jar"))
        if not jars:
            die(f"no jars in {release}; did the build run?")
        for jar in jars:
            shutil.copy2(jar, DIST)
    verify(dry)


def verify(dry):
    if dry:
        return
    jars = sorted(p.name for p in DIST.glob("*.jar"))
    bad = [j for j in jars if not ALLOWED_JAR.match(j)]
    if bad:
        die(f"unexpected files in dist/: {bad}")
    if len(jars) != 30:
        die(f"expected 30 release jars, found {len(jars)}")
    log(f"release jar set OK ({len(jars)} files)")


def changelog(tag, dry):
    if dry:
        return
    lines = capture(["git", "log", "-3", "--no-merges", "--pretty=format:- %s (`%h`)"])
    link = f"https://github.com/{repo_slug()}/releases/tag/{tag}"
    (DIST / "changelog.md").write_text(f"{lines}\n\nFull changelog: {link}\n", encoding="utf-8")
    log("wrote dist/changelog.md")


def bundle_versions():
    out = {}
    for key in ("all", "storage", "crafting", "routing", "access"):
        matches = sorted(DIST.glob(f"universal-bundle-{key}.*.jar"))
        if not matches:
            die(f"missing universal-bundle-{key}.*.jar in dist/")
        out[key] = matches[0].name[len(f"universal-bundle-{key}."):-len(".jar")]
    return out


# --- git ----------------------------------------------------------------------------


def repo_slug():
    url = capture(["git", "remote", "get-url", "origin"]).strip()
    m = re.search(r"github\.com[:/](?P<slug>[^/]+/[^/.]+?)(?:\.git)?$", url)
    if not m:
        die(f"cannot parse origin URL: {url}")
    return m.group("slug")


def current_branch():
    return capture(["git", "rev-parse", "--abbrev-ref", "HEAD"]).strip()


def ensure_clean(allow_dirty):
    if allow_dirty:
        return
    if capture(["git", "status", "--porcelain"]).strip():
        die("working tree is dirty; commit/stash first or pass --allow-dirty")


def commit(paths, message, dry, sign=True):
    run(["git", "add", "--"] + [str(p) for p in paths], dry=dry)
    if not dry and capture(["git", "diff", "--cached", "--name-only"]).strip() == "":
        log("nothing staged; skipping commit")
        return False
    cmd = ["git"]
    if not sign:
        cmd += ["-c", "commit.gpgsign=false"]
    cmd += ["commit", "-m", message]
    run(cmd, dry=dry)
    return True


def tag_release(tag, message, sign, dry):
    """Create the release tag locally (signed unless --unsigned) so it carries a GPG signature."""
    if not dry and capture(["git", "tag", "--list", tag]).strip():
        die(f"tag {tag} already exists; delete it or release with --tag")
    cmd = ["git"]
    if sign:
        cmd += ["tag", "-s", tag, "-m", message]
    else:
        cmd += ["-c", "tag.gpgSign=false", "tag", tag, "-m", message]
    run(cmd, dry=dry)


# --- GitHub API ---------------------------------------------------------------------


def github_token():
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    if token:
        return token
    out = subprocess.run(
        ["git", "credential", "fill"],
        input=b"protocol=https\nhost=github.com\n\n",
        capture_output=True,
    ).stdout.decode()
    for line in out.splitlines():
        if line.startswith("password="):
            return line[len("password="):]
    die("no GITHUB_TOKEN and no stored github.com credential")


def gh_request(method, url, token, data=None, content_type="application/vnd.github+json"):
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Authorization", f"token {token}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("User-Agent", UA)
    if body is not None:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req) as resp:
            payload = resp.read().decode()
            return resp.status, (json.loads(payload) if payload else None)
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode()


def github_release(token, tag, target, body, dry):
    url = f"https://api.github.com/repos/{repo_slug()}/releases"
    if dry:
        print(f"  [dry-run] POST {url} tag={tag} target={target} generate_release_notes=true")
        return None
    status, resp = gh_request("POST", url, token, {
        "tag_name": tag,
        "target_commitish": target,
        "name": f"MC Storage Area Network {tag}",
        "body": body,
        "draft": False,
        "prerelease": False,
        "generate_release_notes": True,
    })
    if status not in (200, 201):
        die(f"GitHub release create failed ({status}): {resp}")
    return resp


def upload_assets(token, release_id, dry):
    for jar in sorted(DIST.glob("*.jar")):
        url = f"https://uploads.github.com/repos/{repo_slug()}/releases/{release_id}/assets?name={jar.name}"
        if dry:
            print(f"  [dry-run] upload {jar.name}")
            continue
        req = urllib.request.Request(url, data=jar.read_bytes(), method="POST")
        req.add_header("Authorization", f"token {token}")
        req.add_header("Accept", "application/vnd.github+json")
        req.add_header("User-Agent", UA)
        req.add_header("Content-Type", "application/octet-stream")
        try:
            with urllib.request.urlopen(req) as resp:
                log(f"uploaded {jar.name} ({resp.status})")
        except urllib.error.HTTPError as exc:
            die(f"asset upload failed for {jar.name} ({exc.code}): {exc.read().decode()}")


def dispatch_publish_ci(token, tag, mc, dry):
    """Ask the publish-only workflow to upload this release to CurseForge/Modrinth.

    GitHub has never fired `release: published` for this repository, so the local release starts
    the workflow explicitly. Best-effort: by now the release is public, so a failure warns and
    prints the manual fallback instead of aborting the whole run.
    """
    workflow = "publish-release.yml"
    url = f"https://api.github.com/repos/{repo_slug()}/actions/workflows/{workflow}/dispatches"
    page = f"https://github.com/{repo_slug()}/actions/workflows/{workflow}"
    ref = current_branch()
    if dry:
        print(f"  [dry-run] POST {url} ref={ref} tag={tag} minecraft={mc}")
        return
    status, resp = gh_request("POST", url, token, {"ref": ref, "inputs": {"tag": tag, "minecraft": mc}})
    if status in (200, 204):
        log(f"CI will publish {tag} to CurseForge/Modrinth: {page}")
    else:
        log(f"could not start the publish workflow ({status}): {resp}")
        log(f"  publish by hand instead: {page} -> Run workflow -> tag {tag}")


# --- CurseForge ---------------------------------------------------------------------

def cf_headers(token):
    return {"X-Api-Token": token, "Accept": "application/json", "User-Agent": UA}


def cf_game_version_ids(token, mc, dry):
    url = "https://minecraft.curseforge.com/api/game/versions"
    if dry:
        print(f"  [dry-run] GET {url}")
        return []
    req = urllib.request.Request(url, headers=cf_headers(token))
    with urllib.request.urlopen(req) as resp:
        rows = json.load(resp)
    wanted = {mc: True, "Fabric": True, "NeoForge": True}
    ids = [r["id"] for r in rows if r.get("name") in wanted]
    log(f"CurseForge game version ids for {mc}: {ids}")
    return ids


def cf_upload(token, project_id, file_path, metadata, dry, attempt_retries=5):
    url = f"https://minecraft.curseforge.com/api/projects/{project_id}/upload-file"
    if dry:
        print(f"  [dry-run] POST {url} <- {file_path.name}")
        return None
    for attempt in range(1, attempt_retries + 1):
        proc = subprocess.run(
            ["curl", "-fsS", "-X", "POST", url, "-H", f"X-Api-Token: {token}",
             "-F", f"metadata={json.dumps(metadata)};type=application/json",
             "-F", f"file=@{file_path}"],
            cwd=ROOT, capture_output=True, text=True,
        )
        if proc.returncode == 0:
            try:
                return json.loads(proc.stdout)
            except json.JSONDecodeError:
                die(f"CurseForge returned non-JSON: {proc.stdout[:200]}")
        log(f"CurseForge upload attempt {attempt} failed: {proc.stderr.strip()[:160]}")
        import time
        time.sleep(15)
    die(f"CurseForge upload gave up on {file_path.name}")


def cf_publish(token, project_id, mc, changed, versions, bundles, dry):
    version_ids = cf_game_version_ids(token, mc, dry)
    common = {"releaseType": "release", "gameVersions": version_ids,
              "changelog": (DIST / "changelog.md").read_text(encoding="utf-8") if (DIST / "changelog.md").exists() else "",
              "changelogType": "markdown"}
    parent_key = f"cf.api.file.{mc}"

    if changed["api"]:
        api_jar = DIST / f"universal_mc_san_api.{versions['api']}.jar"
        result = cf_upload(token, project_id, api_jar, {**common, "displayName": api_jar.name}, dry)
        file_id = (result or {}).get("id")
        if file_id:
            set_state(parent_key, str(file_id))
            log(f"{parent_key}={file_id}")
            for comp in ("storage", "crafting", "routing", "access"):
                jar = DIST / f"universal-{COMP_JAR[comp]}.{versions[comp]}.jar"
                if jar.exists():
                    cf_upload(token, project_id, jar, {**common, "displayName": jar.name, "parentFileID": file_id}, dry)
    else:
        parent = read_props(STATE).get(parent_key) if STATE.exists() else None
        if parent:
            for comp in ("storage", "crafting", "routing", "access"):
                if not changed[comp]:
                    continue
                jar = DIST / f"universal-{COMP_JAR[comp]}.{versions[comp]}.jar"
                if jar.exists():
                    cf_upload(token, project_id, jar, {**common, "displayName": jar.name, "parentFileID": int(parent)}, dry)

    # Bundles (bundle-all always; the rest when their content changed).
    bundle_jobs = [f"universal-bundle-all.{bundles['all']}.jar"]
    if changed["api"] or changed["storage"]:
        bundle_jobs.append(f"universal-bundle-storage.{bundles['storage']}.jar")
    if changed["api"] or changed["crafting"]:
        bundle_jobs.append(f"universal-bundle-crafting.{bundles['crafting']}.jar")
    if changed["api"] or changed["storage"] or changed["routing"]:
        bundle_jobs.append(f"universal-bundle-routing.{bundles['routing']}.jar")
    if changed["api"] or changed["access"]:
        bundle_jobs.append(f"universal-bundle-access.{bundles['access']}.jar")
    for filename in bundle_jobs:
        jar = DIST / filename
        if jar.exists():
            cf_upload(token, project_id, jar, {**common, "displayName": jar.name}, dry)


# --- Modrinth -----------------------------------------------------------------------


def modrinth_sync(token, project, state_key, version_number, version_name, mc, primary, extras, dry):
    data = {
        "project_id": project,
        "name": version_name,
        "version_number": version_number,
        "changelog": (DIST / "changelog.md").read_text(encoding="utf-8") if (DIST / "changelog.md").exists() else "",
        "version_type": "release",
        "status": "listed",
        "featured": False,
        "environment": "client_and_server",
        "loaders": ["fabric", "neoforge"],
        "game_versions": [mc],
        "dependencies": [],
    }
    previous = read_props(STATE).get(state_key) if STATE.exists() else None
    if dry:
        print(f"  [dry-run] modrinth POST /v2/version project={project} v={version_number} files={[primary.name] + [e.name for e in extras]}")
        return
    cmd = ["curl", "-fsS", "-X", "POST", "https://api.modrinth.com/v2/version",
           "-H", f"Authorization: {token}",
           "-F", f"data={json.dumps(data)};type=application/json",
           "-F", f"file=@{primary}"]
    for extra in extras:
        cmd += ["-F", f"file=@{extra}"]
    proc = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
    if proc.returncode != 0:
        log(f"modrinth: upload failed: {proc.stderr.strip()[:200]}")
        return
    new_id = json.loads(proc.stdout).get("id")
    if not new_id:
        log(f"modrinth: no id in response: {proc.stdout[:200]}")
        return
    set_state(state_key, new_id)
    log(f"modrinth: created {new_id} ({version_number})")
    if previous and previous != new_id:
        req = urllib.request.Request(
            f"https://api.modrinth.com/v2/version/{previous}",
            data=b'{"requested_status":"archived"}', method="PATCH")
        req.add_header("Authorization", token)
        req.add_header("Content-Type", "application/json")
        req.add_header("User-Agent", UA)
        try:
            urllib.request.urlopen(req)
            log(f"modrinth: archived {previous}")
        except urllib.error.HTTPError as exc:
            log(f"modrinth: could not archive {previous} ({exc.code})")


# --- release-state.properties -------------------------------------------------------


def set_state(key, value):
    lines = STATE.read_text(encoding="utf-8").splitlines() if STATE.exists() else []
    pattern = re.compile(rf"^{re.escape(key)}=.*$")
    for i, line in enumerate(lines):
        if pattern.match(line):
            lines[i] = f"{key}={value}"
            break
    else:
        lines.append(f"{key}={value}")
    STATE.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")


# --- orchestration ------------------------------------------------------------------


def main():
    ap = argparse.ArgumentParser(description="Build and publish a release locally.")
    ap.add_argument("--mod", required=True, choices=list(MODS),
                    help="which component is changing (tag is always v<api>.<stamp>)")
    ap.add_argument("--mc", default="1.21.1")
    ap.add_argument("--tag", default=None)
    ap.add_argument("--curseforge", action="store_true", help="also upload to CurseForge")
    ap.add_argument("--modrinth", action="store_true", help="also upload to Modrinth")
    ap.add_argument("--no-ci-publish", action="store_true",
                    help="do not ask CI to publish the release to CurseForge/Modrinth")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--unsigned", action="store_true", help="do not GPG-sign the commit and tag")
    ap.add_argument("--allow-dirty", action="store_true")
    ap.add_argument("--no-push", action="store_true")
    ap.add_argument("--skip-build", action="store_true", help="reuse the existing dist/")
    ap.add_argument("--no-daemon", action="store_true",
                    help="do not reuse a Gradle daemon (slower; reproduces a CI-like cold build)")
    ap.add_argument("--local-only", action="store_true",
                    help="build and stage the jars locally only: no commit, tag, push, GitHub release "
                         "or platform/CI publishing. versions.properties is still bumped so the local "
                         "jars carry the next version; undo with `git checkout -- versions.properties`.")
    args = ap.parse_args()

    dry = args.dry_run
    ensure_clean(args.allow_dirty)

    stamp = versioning.stamp()
    changed = bump(args.mod, stamp)
    versions = read_props(VERSIONS)

    # When the API changes, point the hosts at the new version floor.
    floor_paths = versioning.apply_floor(args.mc, versions["api"]) if changed["api"] else []

    tag = args.tag or versioning.tag(versions["api"], stamp)
    log(f"tag: {tag}")

    if not args.skip_build:
        build(args.mc, dry, args.no_daemon)
        collect(dry)
    else:
        verify(dry)
    changelog(tag, dry)

    if args.local_only:
        log(f"local-only: built the {tag} jars into build/release/ and dist/")
        log("no commit, tag, push, GitHub release or platform publishing was performed")
        log("versions.properties was bumped; undo with: git checkout -- versions.properties")
        return

    sign = not args.unsigned

    # Commit the bump + host floor (so the tag points at the bumped versions), then sign+push the tag.
    bump_paths = [VERSIONS, ROOT / "repo"] + [Path(p) for p in floor_paths]
    if commit(bump_paths, f"Release {tag}: bump {args.mod} version", dry, sign):
        if not args.no_push:
            run(["git", "push", "origin", current_branch()], dry=dry)
    tag_release(tag, f"Release {tag}", sign, dry)
    if not args.no_push:
        run(["git", "push", "origin", tag], dry=dry)

    # GitHub release against the tag we just pushed.
    token = github_token()
    target = capture(["git", "rev-parse", "HEAD"]).strip()
    marker = "<!-- mc-san:published -->" if (args.curseforge or args.modrinth) else ""
    release = github_release(token, tag, target, marker, dry)
    if release:
        upload_assets(token, release["id"], dry)

    # CurseForge/Modrinth are published by CI by default. A local upload (--curseforge /
    # --modrinth) also marks the release body, so a stray CI run would skip it anyway.
    if not (args.no_ci_publish or args.curseforge or args.modrinth):
        dispatch_publish_ci(token, tag, args.mc, dry)

    bundles = bundle_versions()

    if args.curseforge:
        cf = os.environ.get("CURSEFORGE_API_KEY")
        if not cf:
            die("--curseforge needs CURSEFORGE_API_KEY (use tools/secrets.py run)")
        cf_publish(cf, os.environ.get("CURSEFORGE_PROJECT_ID", "1690770"), args.mc, changed, versions, bundles, dry)

    if args.modrinth:
        mr = os.environ.get("MODRINTH_TOKEN")
        if not mr:
            die("--modrinth needs MODRINTH_TOKEN (use tools/secrets.py run)")
        modrinth_sync(mr, os.environ.get("MODRINTH_ID", "mc-storage-area-network"),
                      f"mr.api.version.{args.mc}", versions["api"], f"SAN API {versions['api']}", args.mc,
                      DIST / f"universal_mc_san_api.{versions['api']}.jar",
                      [DIST / f"universal-bundle-all.{bundles['all']}.jar"], dry)

    if (args.curseforge or args.modrinth) and commit([STATE], f"Release {tag}: update release state", dry, sign) and not args.no_push:
        run(["git", "push", "origin", current_branch()], dry=dry)

    log("done")


if __name__ == "__main__":
    try:
        main()
    finally:
        # An idle Gradle daemon keeps Loom's cached mapping jars open, and the next build needs
        # exclusive access to re-merge them (FileSystemException: "being used by another process").
        # Stopping it here releases those handles; the next run pays one cold start instead.
        try:
            subprocess.run(gradlew() + ["--stop"], cwd=ROOT,
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=120)
        except Exception:
            pass
