# MC Storage Area Network

**The shared item-network library behind _Storage Network_, _Crafting Network_ and _Network Routing_.**

MC Storage Area Network scans the world around a block, finds every inventory-bearing
container (chests, barrels, hoppers, shulker boxes, modded storage — anything with an
inventory), and exposes them as one shared **item network**. It provides the loader-neutral
API that the gameplay mods use, and lets **other mods register their own item sources**.

A single jar works on **Fabric** and **NeoForge**.

---

## What it does

- **Chunk storage scanning** — discovers inventory blocks in a configurable chunk radius around a host block.
- **Item-source registry** — mods contribute virtual sources (shulker boxes, backpacks, linked inventories, …) without depending on a gameplay mod.
- **Nested sources** — exposes sub-containers (e.g. a shulker box inside a chest) as child rows.
- **Hidden-item filters** — hides "container" items that are represented by their contents instead.
- **Shulker-box helpers** — read/write shulker-box contents as a normal item list.
- **Ownership model** — records who placed each container so hosts can show only global + player-owned storage.
- **Routing hooks** — `StorageRouter` lets a mod bias which container receives an inserted stack (e.g. labeled containers get priority); `NetworkHost` lets hardware bind to an existing host's scan.

## Where it's used

| Mod | What it adds |
|-----|--------------|
| **Storage Network** | Storage Terminal — one searchable interface for every nearby container |
| **Crafting Network** | Crafting / Smelting / Blasting / Smoking / Brewing Terminals — craft using the network |
| **Network Routing** | Label containers with item filters; matching labels win routing priority, plus a chest-shaped Routing Terminal to sort/defrag/trim |

## Installation

1. Install **Fabric Loader + Fabric API**, or **NeoForge 21.1.235+**, for **Minecraft 1.21.1**.
2. Drop the **universal** API jar into your `mods/` folder:

   ```
   universal_mc_san_api.<version>.jar
   ```

   One jar for both loaders — a thin container holding the Fabric and NeoForge builds, where each
   loader loads only its own nested copy.

> This API is a **required dependency** of Storage Network, Crafting Network and Network Routing. For
> the whole suite, use a **bundle**: `universal-bundle-all.<version>.jar` (API + all gameplay mods),
> or `universal-bundle-storage` / `universal-bundle-crafting` for one gameplay mod each, or
> `universal-bundle-routing` for Storage Network + Network Routing.

### Downloads

| File | Loader | Contents |
|------|--------|----------|
| `universal_mc_san_api.<version>.jar` | Fabric + NeoForge | This API |
| `universal-storage-network.<version>.jar` | Fabric + NeoForge | Storage Network (needs the API) |
| `universal-crafting-network.<version>.jar` | Fabric + NeoForge | Crafting Network (needs the API) |
| `universal-network-routing.<version>.jar` | Fabric + NeoForge | Network Routing (needs the API) |
| `universal-bundle-all.<version>.jar` | Fabric + NeoForge | API + Storage Network + Crafting Network + Network Routing |
| `universal-bundle-storage` / `-crafting` / `-routing` | Fabric + NeoForge | API + one gameplay mod (routing also includes Storage Network) |
| `fabric-*` / `neoforge-*` | single loader | any of the above, loader-specific |

Universal jars are published to **CurseForge / Modrinth**; the loader-specific (`fabric-*` /
`neoforge-*`) builds are on
**[GitHub Releases](https://github.com/retiredroca/mc-storage-area-network/releases)**.

### Releases & versions

Each mod is versioned independently, and a release only gets a **new version for the component that
actually changed** — releasing Storage Network does not bump the API, and vice versa. Every GitHub
release still carries the full set of jars (including the current, unchanged API) so one page has
everything. Versions are `<major>.<minor>.<patch>.<yymmddhh>`: the trailing stamp is bumped
automatically, the patch is manual. The API is `1.0.<patch>.<yymmddhh>`; the gameplay mods and
bundles are `1.0.0.<yymmddhh>`. Releases are tagged `v1.0.<patch>.<stamp>` (e.g. `v1.0.2.26091512`).

### Releasing

Releases can run **locally** (build + GitHub release, optionally CurseForge/Modrinth) or on
**GitHub** (build + publish) as a fallback.

**Locally** (recommended)

```bash
python tools/secrets.py init       # once: store CURSEFORGE_API_KEY / MODRINTH_TOKEN in an encrypted vault

# build + sign + commit the version bump + signed tag + GitHub release (+ platforms)
python tools/secrets.py run -- python tools/release.py --mod routing --curseforge --modrinth
python tools/release.py --mod all --dry-run      # preview; no changes
```

`--mod` is `api` / `storage` / `crafting` / `routing` / `all`. The tag is a single series
`v1.0.<patch>.<stamp>` (e.g. `v1.0.2.26091512`). The local run publishes the API to `repo/` first
(hosts require an API version **floor**, so the artifact must be resolvable before they compile),
builds, commits/tags, creates the release, and optionally uploads to the platforms. Creating the
release triggers the publish-only workflow — unless the local run already published (it marks the
release so CI skips it). Add `--unsigned` to skip GPG.

**On GitHub (fallback)**

Run **Release All** (or a per-mod workflow) from the Actions tab, or comment `/release-all`. That
path bumps versions, publishes the API, builds and publishes from source; the tag is computed the
same way (`v1.0.2.<stamp>`). CI cannot sign with your key, so its tags are unsigned.

### Repository configuration (maintainers)

| Kind | Name | Purpose |
|------|------|---------|
| Secret | `CURSEFORGE_API_KEY` | CurseForge upload key |
| Secret | `MODRINTH_TOKEN` | Modrinth personal access token |
| Variable | `PUBLISH_MODRINTH` | `true` to publish to Modrinth (skipped when unset/`false`) |
| Variable | `MODRINTH_ID` | SAN API project slug/ID |
| Variable | `MODRINTH_STORAGE_ID` / `MODRINTH_CRAFTING_ID` / `MODRINTH_ROUTING_ID` | gameplay mod projects |

**Token scopes** — CurseForge: a standard upload key. Modrinth PAT: `VERSION_CREATE` (upload) and
`VERSION_WRITE` (archive superseded versions); `VERSION_DELETE` is not used.

The CurseForge/Modrinth changelog is the last few commits plus a link to the full changelog on the
GitHub release. A Modrinth project only becomes publicly visible once **Approved**; versions can be
uploaded while it is still *Processing*.

### Commit & tag signing (GPG)

Commits and tags are signed with the maintainer's GPG key (on Windows, point Git at Gpg4win's gpg
rather than Git for Windows' bundled copy, which can fight over the keybox):

```bash
git config --global gpg.program "C:/Program Files/GnuPG/bin/gpg.exe"
git config --global user.signingkey <fingerprint>
git config --global commit.gpgsign true
git config --global tag.gpgSign true
```

Upload the public key to GitHub (Settings → SSH and GPG keys) so commits show **Verified**.
`tools/release.py` creates the tag with `git tag -s`; pass `--unsigned` to skip signing.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric Loader + Fabric API, or NeoForge 21.1.235+ |
| Java | 21 |
| Side | Client & Server |

## For modders

Dependency (from the in-repo Maven):

```groovy
repositories {
    maven { url = 'https://raw.githubusercontent.com/retiredroca/mc-storage-area-network/main/repo' }
}
dependencies {
    // Use a floor range: it resolves the newest 1.0.x but refuses anything older than the API
    // the host was built against.
    modImplementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-fabric-1.21.1:[1.0.2,1.1)' // Fabric
    // implementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-neoforge-1.21.1:[1.0.2,1.1)' // NeoForge
}
```

The API's version is `1.0.<patch>.<yymmddhh>` and only advances when the API itself changes; the
**patch** is the compatibility floor. Depend on `[<patch>,1.1)` (Fabric metadata `~1.0.2`) so an
older API can never silently satisfy your mod. The hosts in this repo use the same floor.

Register an item source:

- **Fabric:** add an entrypoint key `mc_storage_area_network` returning `ItemSource` instances.
- **NeoForge:** send the InterModComms message `register_item_source`.
- Or call `ItemSourceRegistry.register(source)` directly, and hide container items with `ItemSourceRegistry.addHiddenItemFilter(...)`.

See **[API-README.md](API-README.md)** for the full API reference.

## Configuration

- Fabric: `config/mc_storage_area_network.json`
- NeoForge: `config/mc_storage_area_network-server.toml`

| Key | Default | Description |
|-----|---------|-------------|
| `flattenDepth` | `1` | How many shulker-box levels to flatten (1 = boxes in containers, 2 = boxes in boxes). |
| `boxRowHidden` | `false` | Hide raw shulker-box stacks so only their contents show. |
| `sameTypeFirst` | `true` | Prefer inserting into boxes that already hold the same item. |
| `ownershipEnabled` | `true` | Terminals show only global storage + storage placed by the terminal's owner. |
| `teamSharing` | `true` | Players on the same scoreboard team share their placed storage. |

## Compatibility

- Minecraft **1.21.1** only.
- Fabric and NeoForge versions of a mod can't be mixed, but this single jar covers both loaders.

## License

This project's own source code is licensed under [Apache-2.0](LICENSE).

## Credits & third-party notices

- **Minecraft** is a trademark of Mojang Synergies AB. This is an unofficial, fan-made
  modification and is **not affiliated with, endorsed by, or sponsored by** Mojang Studios or
  Microsoft Corporation.
- Developed against the **official Mojang mappings**. The mappings are used only for development
  and are **not redistributed** by this project. The mappings are (c) 2020 Microsoft Corporation,
  provided "as-is"; use and modification of Minecraft: Java Edition is governed by the
  [Minecraft EULA](https://account.mojang.com/documents/minecraft_eula).
- Built on **Fabric Loader/API** (Apache-2.0) and **NeoForge** (LGPL-2.1).

See [NOTICE](NOTICE) for full attribution.

## Author

Retired Roca
