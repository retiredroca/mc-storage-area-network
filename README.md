# MC Storage Area Network

**The shared item-network library behind _Storage Network_ and _Crafting Network_.**

MC Storage Area Network scans the world around a block, finds every inventory-bearing
container (chests, barrels, hoppers, shulker boxes, modded storage — anything with an
inventory), and exposes them as one shared **item network**. It provides the loader-neutral
API that the two gameplay mods use, and lets **other mods register their own item sources**.

A single jar works on **Fabric** and **NeoForge**.

---

## What it does

- **Chunk storage scanning** — discovers inventory blocks in a configurable chunk radius around a host block.
- **Item-source registry** — mods contribute virtual sources (shulker boxes, backpacks, linked inventories, …) without depending on a gameplay mod.
- **Nested sources** — exposes sub-containers (e.g. a shulker box inside a chest) as child rows.
- **Hidden-item filters** — hides "container" items that are represented by their contents instead.
- **Shulker-box helpers** — read/write shulker-box contents as a normal item list.
- **Ownership model** — records who placed each container so hosts can show only global + player-owned storage.

## Where it's used

| Mod | What it adds |
|-----|--------------|
| **Storage Network** | Storage Terminal — one searchable interface for every nearby container |
| **Crafting Network** | Crafting / Smelting / Blasting / Smoking / Brewing Terminals — craft using the network |

## Installation

1. Install **Fabric Loader + Fabric API**, or **NeoForge 21.1.235+**, for **Minecraft 1.21.1**.
2. Drop `mc-storage-area-network-<version>-universal.jar` into your `mods/` folder.

> This API is a **required dependency** of Storage Network and Crafting Network — install it
> alongside them (and with any other mod that hooks into the API).

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
    modImplementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-fabric:1.0.2' // Fabric
    // implementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-neoforge:1.0.2' // NeoForge
}
```

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
