#!/usr/bin/env python3
"""GPG-encrypted vault for the local release tokens.

The vault is a GPG symmetric (AES-256) file at the repository root, `.release-env.gpg`, holding
KEY=value lines such as:

    CURSEFORGE_API_KEY=...
    MODRINTH_TOKEN=...

Nothing is ever written in plaintext: `init` feeds the values to gpg over stdin, and `run`
decrypts into the child process environment only.

Usage:
    python tools/secrets.py init [--vault .release-env.gpg]
    python tools/secrets.py run -- <command> [args...]

`init` prompts for each key and then for a passphrase (gpg's pinentry). `run` prompts for the
passphrase and executes the command with the decrypted variables exported.
"""

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_VAULT = ROOT / ".release-env.gpg"
DEFAULT_KEYS = ["CURSEFORGE_API_KEY", "MODRINTH_TOKEN", "GITHUB_TOKEN"]


def find_gpg() -> str:
    """Prefer $GPG, then the gpg git is configured to use, then PATH.

    On Windows this matters: Git for Windows ships its own gpg which can conflict with the
    Gpg4win/Kleopatra keyboxd; `git config gpg.program` points at the working one.
    """
    gpg = os.environ.get("GPG")
    if not gpg:
        try:
            gpg = subprocess.run(
                ["git", "config", "--get", "gpg.program"],
                cwd=ROOT, capture_output=True, text=True,
            ).stdout.strip() or None
        except OSError:
            gpg = None
    if not gpg:
        gpg = shutil.which("gpg")
    if not gpg:
        sys.exit("gpg not found (set GPG=/path/to/gpg, or git config gpg.program).")
    return gpg


def cmd_init(args) -> int:
    vault = Path(args.vault)
    print("Enter the release secrets. Leave a value blank to skip it.")
    lines = []
    for key in args.keys:
        value = input(f"{key} (blank to skip): ").strip()
        if value:
            lines.append(f"{key}={value}")
    if not lines:
        print("Nothing to store.", file=sys.stderr)
        return 1

    payload = ("\n".join(lines) + "\n").encode()
    # --symmetric: gpg derives the key from a passphrase and prompts via pinentry.
    proc = subprocess.run(
        [find_gpg(), "--quiet", "--yes", "--symmetric", "--cipher-algo", "AES256", "--output", str(vault)],
        input=payload,
    )
    if proc.returncode != 0:
        return proc.returncode
    print(f"wrote {vault} ({len(lines)} key(s))")
    print("Add it to your password manager if you like; keep the passphrase safe.")
    return 0


def decrypt(vault: Path) -> dict:
    if not vault.exists():
        sys.exit(f"{vault} not found. Run 'python tools/secrets.py init' first.")
    proc = subprocess.run([find_gpg(), "--quiet", "--decrypt", str(vault)], capture_output=True)
    if proc.returncode != 0:
        sys.stderr.write(proc.stderr.decode(errors="replace"))
        sys.exit("gpg decryption failed.")
    env = {}
    for line in proc.stdout.decode().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        env[key.strip()] = value.strip()
    return env


def cmd_run(args) -> int:
    if not args.command:
        sys.exit("usage: python tools/secrets.py run -- <command> [args...]")
    secrets = decrypt(Path(args.vault))
    env = dict(os.environ)
    env.update(secrets)
    print(f"secrets.py: loaded {', '.join(sorted(secrets))} into the environment", file=sys.stderr)
    try:
        return subprocess.run(args.command, env=env).returncode
    except FileNotFoundError as exc:
        sys.exit(str(exc))


def main() -> int:
    parser = argparse.ArgumentParser(description="GPG-encrypted vault for local release tokens.")
    parser.add_argument("--vault", default=str(DEFAULT_VAULT), help="vault path (default .release-env.gpg)")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_init = sub.add_parser("init", help="create/overwrite the encrypted vault")
    p_init.add_argument("--keys", nargs="*", default=DEFAULT_KEYS, help="variable names to store")
    p_init.set_defaults(func=cmd_init)

    p_run = sub.add_parser("run", help="run a command with the decrypted secrets in the environment")
    p_run.add_argument("command", nargs=argparse.REMAINDER, help="command (use '--' before it)")
    p_run.set_defaults(func=cmd_run)

    args = parser.parse_args()
    # cmd_run consumes a REMAINDER that starts with '--'; drop the separator.
    if args.cmd == "run" and args.command and args.command[0] == "--":
        args.command = args.command[1:]
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
