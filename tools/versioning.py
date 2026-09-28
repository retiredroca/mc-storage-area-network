#!/usr/bin/env python3
"""Version helpers shared by the release tooling and CI.

Scheme: ``<major>.<minor>.<patch>.<yymmddhh>`` — e.g. ``1.0.2.26091512``.

* the trailing ``yymmddhh`` stamp is bumped automatically (Hawaii Standard Time, UTC-10, no DST);
* the semantic line (first three components) moves via `bump_semantic` -- run it through
  `tools/release.py --bump patch|minor|major`, or let the branch/merge workflow do it; it is
  never hand-edited, so the tooling stays the only thing that writes versions.properties;
* the tag is ``v<major>.<minor>.<patch>.<stamp>`` (single series, e.g. ``v1.0.2.26091512``).

Modules depend on the library through a **floor range** derived from the library's first three
components, so an older library can never silently satisfy a dependant. The range is computed at
configuration time from versions.properties (see moduleFloor in gradle/versions.gradle) and the
metadata carries it as a token the build substitutes:

    fabric.mod.json     "mc_storage_area_network": "~${mc_storage_area_network_range}"
    neoforge.mods.toml  versionRange="[${mc_storage_area_network_range},FLOOR_UPPER)"

So there is nothing to rewrite on a bump, and no file in which a stale range can survive.

CLI:
    python tools/versioning.py stamp
    python tools/versioning.py bump <value> <stamp>
    python tools/versioning.py tag  --api <libraryVersion> --stamp <stamp>
    python tools/versioning.py floor --api <libraryVersion> [--mc <mc>] [--lib <moduleId>]
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


def floor(version: str) -> str:
    """The first three components, e.g. 1.0.2 from 1.0.2.26091512 (and 1.0.0 from 1.0).

    Rejects anything that is not a dotted run of digits. The floor is what the hosts' dependency
    ranges are built from, so a bad value does not fail the release that produced it -- it
    publishes a range matching nothing, and the failure surfaces much later as mods that quietly do
    not load. An empty string is the case worth naming: it is what a missing, renamed or
    unpopulated versions.properties key yields, and silently produced ",1.1)" until this rejected
    it.
    """
    text = (version or "").strip()
    if not text:
        raise ValueError("no version to take a floor from (the value was empty)")
    parts = text.split(".")
    if not all(p.isascii() and p.isdigit() for p in parts):
        raise ValueError(f"expected a dotted run of digits, got {version!r}")
    while len(parts) < 3:
        parts.append("0")
    return ".".join(parts[:3])


def tag(version: str, stamp: str) -> str:
    return f"v{floor(version)}.{stamp}"


# --- modules ---------------------------------------------------------------------------
# Module identity lives in each module's versions/<mc>/<module>/module.properties, which is the same
# file gradle/versions.gradle reads. A module's id is its versions.properties key, its release jar
# name, its maven artifactId and its assets/data namespace, so nothing here needs to map between
# those spellings.


def load_props(path: Path) -> dict:
    """A .properties file as a dict. Comments and blank lines are dropped."""
    props = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("!"):
            continue
        if "=" in line:
            key, _, value = line.partition("=")
            props[key.strip()] = value.strip()
    return props


def modules(mc: str) -> list:
    """Every module under versions/<mc>/ as (dir, properties)."""
    base = ROOT / "versions" / mc
    if not base.is_dir():
        return []
    result = []
    for module_dir in sorted(p for p in base.iterdir() if p.is_dir()):
        props_file = module_dir / "module.properties"
        if props_file.exists():
            result.append((module_dir, load_props(props_file)))
    return result


def module_ids(mc: str) -> list:
    return [props.get("id") for _, props in modules(mc) if props.get("id")]


def libraries(mc: str) -> list:
    """Modules marked library=true: (module id, directory, group)."""
    out = []
    for module_dir, props in modules(mc):
        if (props.get("library") or "false").strip().lower() == "true":
            out.append((props.get("id"), module_dir, props.get("group")))
    return out


# --- dependents ---------------------------------------------------------------------
# A module asks for a library through a floor range computed at configuration time from
# versions.properties (see gradle/versions.gradle's moduleFloor), and its metadata carries the floor
# as a token the build substitutes. So there is nothing to rewrite on a bump, and nothing here that
# could write a range matching nothing: versions.properties is the single source of truth.
#
# This replaces a set of regexes that rewrote the range into every dependant's fabric.mod.json,
# neoforge.mods.toml and gradle.properties in place. Those rewrites were the one version constraint
# in the project no build step checked, and a missing key produced "[,1.1)" rather than an error.


def floor_paths(mc: str, lib_id: str) -> list:
    """The metadata files that declare the library dependency, for reporting."""
    paths = []
    for module_dir, props in modules(mc):
        if (props.get("id") or "").strip() == lib_id:
            continue
        depends = [d.strip() for d in (props.get("depends") or "").split(",") if d.strip()]
        if lib_id not in depends:
            continue
        for loader in ("fabric", "neoforge"):
            loader_dir = module_dir / loader
            if not loader_dir.is_dir():
                continue
            meta = (loader_dir / "src/main/resources/fabric.mod.json" if loader == "fabric"
                    else loader_dir / "src/main/resources/META-INF/neoforge.mods.toml")
            if meta.exists():
                paths.append(str(meta.relative_to(ROOT)))
    return paths


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
    p_floor.add_argument("--api", required=True, help="the library version to derive the floor from")
    p_floor.add_argument("--mc", default=None)
    p_floor.add_argument("--lib", default=None, help="library module id (default: the only library)")

    def do_floor(a):
        f = floor(a.api)
        if a.mc is None:
            print(f)
            return
        libs = libraries(a.mc)
        lib_id = a.lib
        if lib_id is None:
            if len(libs) != 1:
                sys.exit(f"versioning: --lib is required when the project has "
                         f"{len(libs)} libraries ({[x[0] for x in libs]})")
            lib_id = libs[0][0]
        # Nothing is rewritten: dependants resolve this floor at configuration time from
        # versions.properties. Listing them makes the dependency visible in CI logs.
        print(f"floor [{f},{FLOOR_UPPER}) for '{lib_id}'")
        for path in floor_paths(a.mc, lib_id):
            print(f"  dependant: {path}")

    p_floor.set_defaults(func=do_floor)

    args = ap.parse_args()
    try:
        args.func(args)
    except ValueError as exc:
        sys.exit(f"versioning: {exc}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
