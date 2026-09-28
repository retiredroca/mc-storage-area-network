#!/usr/bin/env python3
"""Version helpers shared by the release tooling and CI.

Scheme: ``<major>.<minor>.<patch>.<yymmddhh>`` — e.g. ``1.0.2.26091512``.

* the trailing ``yymmddhh`` stamp is bumped automatically (Hawaii Standard Time, UTC-10, no DST);
* the semantic line (first three components) moves via `bump_semantic` -- run it through
  `tools/release.py --bump patch|minor|major`, or let the branch/merge workflow do it; it is
  never hand-edited, so the tooling stays the only thing that writes versions.properties;
* the tag is ``v<major>.<minor>.<patch>.<stamp>`` (single series, e.g. ``v1.0.2.26091512``).

Hosts depend on the API with a **floor range** derived from the API's first three components, so an
older API can never silently satisfy a host:
    gradle.properties   api_version=[<floor>,FLOOR_UPPER)
    fabric.mod.json     "mc_storage_area_network": "~<floor>"
    neoforge.mods.toml  versionRange="[<floor>,FLOOR_UPPER)"

CLI:
    python tools/versioning.py stamp
    python tools/versioning.py bump <value> <stamp>
    python tools/versioning.py tag  --api <apiVersion> --stamp <stamp>
    python tools/versioning.py floor --api <apiVersion> [--mc <mc>]   # apply to hosts when --mc given
"""

import argparse
import datetime
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# Stamp timezone: Hawaii Standard Time, a fixed UTC-10 with no daylight saving, so a stamp is
# always an exact hour offset from UTC and never jumps back and forth across a DST boundary.
# gradle/versions.gradle reads this constant *by name* to stamp versions at build time, so keep
# the name and the double-quoted form. Templates leave it as __VERSION_TZ__ and substitute the
# zone chosen at project creation.
TIMEZONE = "GMT-10"

# Range upper bound written into the API floor. gradle/versions.gradle mirrors this as
# ext.floorUpper, so the two must be changed together when the API's major version line moves.
FLOOR_UPPER = "1.1"

HAWAII = datetime.timezone(datetime.timedelta(hours=-10))


def stamp() -> str:
    return datetime.datetime.now(HAWAII).strftime("%y%m%d%H")


def bump(value: str, stamp: str) -> str:
    """Replace only the trailing component, keeping the semantic prefix."""
    return f"{value.rsplit('.', 1)[0]}.{stamp}"


def bump_semantic(value: str, part: str = "patch") -> str:
    """Increment the semantic version line and drop the stamp, so the build re-stamps it.

    The stamp is the tool's job (``bump``); the ``<major>.<minor>.<patch>`` line is a deliberate
    decision, made by running this rather than by editing versions.properties by hand. A branch
    takes the next patch so its jars are distinguishable from the released line's by filename;
    a merged release takes the next patch unless a minor or major is called for.

    Returns a three-component value -- ``1.0.1`` from ``1.0.0.26092517`` -- because
    ``stampVersion`` appends the build-time stamp to anything shorter than four components.
    """
    if part not in ("patch", "minor", "major"):
        raise ValueError(f"part must be patch, minor or major, not {part!r}")
    parts = [int(p) if p.isdigit() else 0 for p in value.split(".")[:3]]
    while len(parts) < 3:
        parts.append(0)
    if part == "major":
        return f"{parts[0] + 1}.0.0"
    if part == "minor":
        return f"{parts[0]}.{parts[1] + 1}.0"
    return f"{parts[0]}.{parts[1]}.{parts[2] + 1}"


def floor(api_version: str) -> str:
    """The API's first three components, e.g. 1.0.2 from 1.0.2.26091512."""
    return ".".join(api_version.split(".")[:3])


def tag(api_version: str, stamp: str) -> str:
    return f"v{floor(api_version)}.{stamp}"


# --- host floor rewriting -----------------------------------------------------------


def _sub(path: Path, pattern: str, repl: str, dry: bool = False) -> bool:
    text = path.read_text(encoding="utf-8")
    new = re.sub(pattern, repl, text)
    if new != text:
        # dry: report the change without writing, so a dry run leaves the tree clean.
        if not dry:
            path.write_text(new, encoding="utf-8")
        return True
    return False


def _rewrite_props(path: Path, f: str, dry: bool = False) -> bool:
    return _sub(path, r"(?m)^api_version=.*$", f"api_version=[{f},{FLOOR_UPPER})", dry)


def _rewrite_fabric(path: Path, f: str, dry: bool = False) -> bool:
    return _sub(path, r'("mc_storage_area_network"\s*:\s*")[^"]*(")',
                rf"\g<1>~{f}\g<2>", dry)


def _rewrite_neoforge(path: Path, f: str, dry: bool = False) -> bool:
    return _sub(path, r'(?s)(modId="mc_storage_area_network".*?versionRange=")[^"]*(")',
                rf"\g<1>[{f},{FLOOR_UPPER})\g<2>", dry)


def apply_floor(mc: str, api_version: str, dry: bool = False) -> list:
    """Point every gameplay host in versions/<mc> at the API floor. Returns the changed paths."""
    f = floor(api_version)
    base = ROOT / "versions" / mc
    if not base.is_dir():
        sys.exit(f"versioning: versions/{mc} not found")
    changed = []
    for mod_dir in sorted(p for p in base.iterdir() if p.is_dir()):
        for loader in ("fabric", "neoforge"):
            loader_dir = mod_dir / loader
            if not loader_dir.is_dir():
                continue
            props = loader_dir / "gradle.properties"
            if props.exists() and _rewrite_props(props, f, dry):
                changed.append(str(props.relative_to(ROOT)))
            if loader == "fabric":
                meta = loader_dir / "src/main/resources/fabric.mod.json"
                if meta.exists() and _rewrite_fabric(meta, f, dry):
                    changed.append(str(meta.relative_to(ROOT)))
            else:
                meta = loader_dir / "src/main/resources/META-INF/neoforge.mods.toml"
                if meta.exists() and _rewrite_neoforge(meta, f, dry):
                    changed.append(str(meta.relative_to(ROOT)))
    return changed


def main() -> int:
    ap = argparse.ArgumentParser(description="Version helpers (scheme <major>.<minor>.<patch>.<yymmddhh>).")
    sub = ap.add_subparsers(dest="cmd", required=True)

    sub.add_parser("stamp").set_defaults(func=lambda a: print(stamp()))

    p_bump = sub.add_parser("bump")
    p_bump.add_argument("value")
    p_bump.add_argument("stamp")
    p_bump.set_defaults(func=lambda a: print(bump(a.value, a.stamp)))

    p_tag = sub.add_parser("tag")
    p_tag.add_argument("--api", required=True)
    p_tag.add_argument("--stamp", required=True)
    p_tag.set_defaults(func=lambda a: print(tag(a.api, a.stamp)))

    p_floor = sub.add_parser("floor")
    p_floor.add_argument("--api", required=True)
    p_floor.add_argument("--mc", default=None)

    def do_floor(a):
        f = floor(a.api)
        if a.mc:
            changed = apply_floor(a.mc, a.api)
            for path in changed:
                print(f"floor -> [{f},{FLOOR_UPPER})  {path}")
            if not changed:
                print(f"floor already [{f},{FLOOR_UPPER})")
        else:
            print(f)

    p_floor.set_defaults(func=do_floor)

    args = ap.parse_args()
    args.func(args)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
