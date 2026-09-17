# MC Storage Area Network

**The shared item-network library behind _Storage Network_, _Crafting Network_, _Network Routing_ and _Remote Access Terminal_.**

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
- **Shared interaction dispatch** — one place for block uses, sneak-clicks, item-on-block and air uses, so mods register hooks instead of their own loader events (`InteractionHooks`).
- **Permissions** — ownership plus scoreboard-team sharing and per-owner invitation lists; hosts are **open by default** and an owner can privatise one. `NetworkPermissions` is the single `canUse` / `canEdit` / `canBreak` rule every mod consults.
- **Break protection & container exclusions** — owner-only breaking with an operator override, and a shared exclusion list (`BreakProtection`, `NetworkExclusions`).
- **Sister-mod awareness** — mods ask which suite mods are installed (`NetworkAwareness`) and publish or consume capabilities (`NetworkCapabilities`), so cross-mod features need no compile dependency on each other.

## Where it's used

| Mod | What it adds |
|-----|--------------|
| **Storage Network** | Storage Terminal — one searchable interface for every nearby container |
| **Crafting Network** | Crafting / Smelting / Blasting / Smoking / Brewing Terminals — craft using the network |
| **Network Routing** | Label containers with item filters; matching labels win routing priority, plus a chest-shaped Routing Terminal to sort/defrag/trim |
| **Remote Access Terminal** | Dye-coloured terminals you name and travel between, with an optional chunk-loader mode; a held Routing Linker becomes a one-way trip back |

## Installation

1. Install **Fabric Loader + Fabric API**, or **NeoForge 21.1.235+**, for **Minecraft 1.21.1**.
2. Drop the **universal** API jar into your `mods/` folder:

   ```
   universal_mc_san_api.<version>.jar
   ```

   One jar for both loaders — a thin container holding the Fabric and NeoForge builds, where each
   loader loads only its own nested copy.

> This API is a **required dependency** of every gameplay mod. For the whole suite, use a
> **bundle**: `universal-bundle-all.<version>.jar` (API + all gameplay mods), or one of the
> per-mod bundles — `universal-bundle-storage` / `-crafting` / `-access` (API + that mod), or
> `universal-bundle-routing` for Storage Network + Network Routing.

### Downloads

| File | Loader | Contents |
|------|--------|----------|
| `universal_mc_san_api.<version>.jar` | Fabric + NeoForge | This API |
| `universal-storage-network.<version>.jar` | Fabric + NeoForge | Storage Network (needs the API) |
| `universal-crafting-network.<version>.jar` | Fabric + NeoForge | Crafting Network (needs the API) |
| `universal-network-routing.<version>.jar` | Fabric + NeoForge | Network Routing (needs the API) |
| `universal-remote-access-terminal.<version>.jar` | Fabric + NeoForge | Remote Access Terminal (needs the API) |
| `universal-bundle-all.<version>.jar` | Fabric + NeoForge | API + Storage Network + Crafting Network + Network Routing + Remote Access Terminal |
| `universal-bundle-storage` / `-crafting` / `-routing` / `-access` | Fabric + NeoForge | API + one gameplay mod (routing also includes Storage Network) |
| `fabric-*` / `neoforge-*` | single loader | any of the above, loader-specific |

Universal jars are published to **CurseForge / Modrinth**; the loader-specific (`fabric-*` /
`neoforge-*`) builds are on
**[GitHub Releases](https://github.com/retiredroca/mc-storage-area-network/releases)**.

### Releases & versions

Each mod is versioned independently, and a release only re-versions the **component you name** —
releasing Storage Network does not bump the API, and vice versa. Nothing infers this from the files
you touched, so when one change spans the API and one or more hosts (for example moving shared code
into the API) release with `all` / **Release All**: a narrow release leaves the other components at
their old versions, and those mix into the bundle jars. Every GitHub release still carries the full
set of jars (including the current, unchanged API) so one page has everything. Versions are
`<major>.<minor>.<patch>.<yymmddhh>`: the trailing stamp is bumped automatically (Hawaii Standard
Time, UTC-10), the patch is manual. The components are `api`, `storage`, `crafting`, `routing` and
`access`; the API is `1.0.<patch>.<yymmddhh>`, the gameplay mods and bundles are
`1.0.0.<yymmddhh>`. Releases are tagged `v1.0.<patch>.<stamp>` (e.g. `v1.0.2.26091512`).

### Building

The build runs in four ordered groups, so a release is produced in stages rather than all at once,
and each group can be built on its own:

```bash
./gradlew -Pmc=1.21.1 releaseLoaderJars        # 1. per-loader API + mod jars
./gradlew -Pmc=1.21.1 releaseLoaderBundles     # 2. per-loader bundles
./gradlew -Pmc=1.21.1 releaseUniversal         # 3. universal API + mod jars
./gradlew -Pmc=1.21.1 releaseUniversalBundles  # 4. universal bundles
./gradlew -Pmc=1.21.1 releaseJars              # the whole set (all four groups, in order)
```

`releaseLoaderJars` / `releaseLoaderBundles` accept `-Ploader=fabric|neoforge` for the fast inner
loop, and `cleanLoader` / `cleanUniversal` wipe just their group. When the API sources changed,
`ensureApi` republishes the API into `repo/`, drops dependents' cached API jars and lets the model
pick the new artifact up automatically.

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

`--mod` is `api` / `storage` / `crafting` / `routing` / `access` / `all`; pass `all` (or run
**Release All**) whenever several components changed together, so no old version ends up inside a
bundle. The tag is a single series `v1.0.<patch>.<stamp>` (e.g. `v1.0.2.26091512`). The local run
publishes the API to `repo/` first (hosts require an API version **floor**, so the artifact must be
resolvable before they compile), builds the four groups in order, commits/tags, creates the release,
and optionally uploads to the platforms. Creating the release triggers the publish-only workflow —
unless the local run already published (it marks the release so CI skips it). Add `--unsigned` to
skip GPG.

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
| Variable | `MODRINTH_ID` | The suite's Modrinth project (all mods publish versions into it) |
| Variable | `MODRINTH_STORAGE_ID` / `MODRINTH_CRAFTING_ID` / `MODRINTH_ROUTING_ID` | per-mod Modrinth projects (default to the shared project when unset) |

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

Other API surfaces a companion can use:

- **Interaction hooks** — `InteractionHooks.register(InteractionType.BLOCK_USE | ITEM_ON_BLOCK | AIR_USE, hook)` runs your handler from the API's own loader events (priority-ordered, `HANDLED` consumes the interaction); no mod registers `UseBlockCallback` / `PlayerInteractEvent` itself.
- **Permissions** — `NetworkPermissions.canUse/canEdit/canBreak`, `isOpen/setOpen`, and per-owner `invite/uninvite/invitesOf/setInvites`.
- **Presence & capabilities** — `NetworkAwareness.isPresent(SisterMods.X)` / `present()`, and `NetworkCapabilities.register(YourInterface.class, impl)` / `get(...)` for publishing data that sister mods consume without a compile dependency (the suite's `TerminalRegistry` and `RouteProvider` work this way).
- **Protection** — mark blocks with `NetworkBlock` to get owner-only breaking (`BreakProtection`) and to be skipped by the container-exclusion toggle.

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
