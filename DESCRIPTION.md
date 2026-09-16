# MC Storage Area Network

**Turn every chest, barrel and container around you into one shared, searchable item network — then craft with it.**

Have you ever filled a chunk with chests? Now you can — and access, search, sort, and craft with
everything inside them from a single block. MC Storage Area Network is a suite of four mods for
**Minecraft 1.21.1** on **Fabric** and **NeoForge** — install them all from one **all-in-one jar**,
or pick only the mods you want:

| Mod | What it adds |
|-----|--------------|
| **Storage Network** | **Storage Terminal** — browse, search, take and deposit from every nearby container from one interface. Plus the **Output Terminal** for collecting crafted output. |
| **Crafting Network** | **Crafting / Smelting / Blasting / Smoking / Brewing Terminals** — upgradable machines that craft, cook and brew using items pulled from the network. |
| **Network Routing** | **Routing Terminal** and **Routing Linker** — label containers with item filters so matching containers win routing priority; the terminal can also sort, defrag and trim the network. |
| **MC Storage Area Network** | The shared library/API that scans storage and powers the mods above. **Required by all**; other mods can hook into it. |

![Storage Network demo](https://raw.githubusercontent.com/retiredroca/mc-storage-area-network/main/versions/1.21.1/storage-network/storage-network.gif)
![Crafting Network demo](https://raw.githubusercontent.com/retiredroca/mc-storage-area-network/main/versions/1.21.1/crafting-network/crafting-network.gif)

---

## The three gameplay mods

One column per mod, starting with the block you will use most. A `*` marks something that needs a
companion mod — install the **all-in-one jar** (or the matching bundle) and nothing is missing.

| **Storage Network** | **Crafting Network** | **Network Routing** * |
|---|---|---|
| **Storage Terminal** — browse, search and sort the whole network from one grid (search by name or id; sort by name, type, tag, mod or equipment)<br>• Left-click to take a stack, shift-click to take more<br>• Shift-click your own items to deposit them back<br>• **Sources** dropdown — scope it to *all storage*, *your inventory*, or a single container<br>• **Tiers 0–5** — Copper 1×1 → Iron 3×3 → Gold 5×5 → Emerald 7×7 → Diamond 9×9 → Netherite 11×11 chunks (see Recipes)<br>• Recipe: 8× Copper Ingot around a Chest<br>**Also in this mod**<br>• **Output Terminal** — a collection-only sink that receives crafted output and keeps it out of normal listings (recipe: 4× Chest around a Storage Terminal); **crouch + right-click** toggles whether its contents appear in the network<br>• **Shulkers first** — a per-terminal checkbox that fills shulker boxes before other containers<br>• **Shulker flattening** — shulker-box contents, and boxes inside boxes, appear as ordinary items you can craft with | **Crafting Terminal** — the vanilla 3×3 grid plus the full recipe book, crafting from the network and your inventory<br>• The result slot shows **how many** you can make<br>• **Click** to craft one, **shift-click** to craft a stack<br>• **Tiers 0–5** as Storage; higher tiers also work faster<br>• Recipe: 8× Copper Ingot around a Crafting Table<br>• Crafted output can be collected automatically by an **Output Terminal** **<br>**Other craftable terminals** — 8× Copper Ingot around a …<br>• **Smelting** (Furnace) • **Blasting** (Blast Furnace) • **Smoking** (Smoker) — pull inputs and fuel from the network and process automatically; **crouch + right-click** collects the experience they earn<br>• **Brewing** (Brewing Stand) — pick a target potion, and it pulls water bottles, blaze powder and ingredients to brew **one batch (3 potions)**<br>• **Inventory first** — send crafted output straight to your inventory<br>• **Shulkers first** — the same per-machine toggle as Storage | **Routing Terminal** — label containers with item filters using the **Routing Linker**, so matching containers win routing priority<br>• Inherits its range from the **Storage Terminal** it connects to (it shows *Connected to Storage Terminal (Tier N)*)<br>• **Sort**, **defragment** and **trim** the network from the terminal<br>**Requires Storage Network** — Network Routing does not run without it, which is why `universal-bundle-routing` contains both |

**All four together**

- **One network for all your storage** — chests, barrels, hoppers, shulker boxes and any modded inventory-bearing block within range.
- **No redstone, no item filters** — one terminal replaces hopper chains, sorters and filter systems.
- **Craft even when your inventory is full** — output goes into the network, or straight back to you with *Inventory first*.
- **Smart output routing** — crafted items go to an **Output Terminal** first, then a container already holding that item, then the nearest container, then your inventory.
- **Ownership & privacy** — worldgen storage is visible to everyone; player-placed storage only to the terminal's owner (and optionally their scoreboard team). Terminals are openable by anyone, but breakable only by their owner.
- **Configurable** — every option can be tuned per server (see Configuration).

> `*` needs its companion mod — with the all-in-one jar (or the matching bundle) nothing is missing.
> `**` collecting crafted output in an **Output Terminal** needs **Storage Network**.

## Recipes

| Result | Recipe |
|--------|--------|
| **Storage Terminal** | 3×3: 8× Copper Ingot around a Chest |
| **Output Terminal** | 3×3: 4× Chest around a Storage Terminal |
| **Crafting Terminal** | 3×3: 8× Copper Ingot around a Crafting Table |
| **Smelting Terminal** | 3×3: 8× Copper Ingot around a Furnace |
| **Blasting Terminal** | 3×3: 8× Copper Ingot around a Blast Furnace |
| **Smoking Terminal** | 3×3: 8× Copper Ingot around a Smoker |
| **Brewing Terminal** | 3×3: 8× Copper Ingot around a Brewing Stand |

**Tier upgrades** — place the previous terminal in the center of a 3×3 crafting grid and surround it with four of the tier material:

| Tier | Name | Material | Scan radius |
|------|------|----------|-------------|
| 0 | Copper | — (base terminal) | 1×1 chunks |
| 1 | Iron | 4× Iron Ingot | 3×3 chunks |
| 2 | Gold | 4× Gold Ingot | 5×5 chunks |
| 3 | Emerald | 4× Emerald | 7×7 chunks |
| 4 | Diamond | 4× Diamond | 9×9 chunks |
| 5 | Netherite | 4× Netherite Ingot | 11×11 chunks |

Higher tiers also make processor terminals work faster.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | **Fabric** (Loaders + Fabric API) **or** **NeoForge 21.1.235+** |
| Java | 21 |
| Side | Client & Server |

> **MC Storage Area Network (`mc_storage_area_network`) is required by every gameplay mod.** It is
> bundled in the all-in-one jar; when installing the mods individually, install the API jar too.

## Installation

Every jar below is **universal** — the same file works on both Fabric and NeoForge (it holds the
loader-specific builds inside and each loader loads only its own copy).

**It is best to use the bundle.** `universal-bundle-all.<version>.jar` is the whole suite in one
file — drop it in `mods/` and you are done. Want only part of it? `universal-bundle-storage`,
`universal-bundle-crafting` and `universal-bundle-routing` are the same idea for one gameplay mod
each, and they always bring the API along.

**But if you want separate mods, here are the correct combinations:**

| You want | Install these jars |
|----------|--------------------|
| Storage only | `universal_mc_san_api` + `universal-storage-network` |
| Crafting only | `universal_mc_san_api` + `universal-crafting-network` |
| Storage + Crafting | `universal_mc_san_api` + `universal-storage-network` + `universal-crafting-network` |
| Storage + Routing | `universal_mc_san_api` + `universal-storage-network` + `universal-network-routing` |
| Everything | `universal-bundle-all` (or all four jars) |

- **Every gameplay mod needs the API jar** (`universal_mc_san_api.<version>.jar`).
- **Network Routing does not run without Storage Network** — installing the routing jar on its own
  will not load. That is why `universal-bundle-routing` contains both.
- **Never mix a bundle with individual jars** — the same mods would load twice.

Install **Fabric Loader + Fabric API**, or **NeoForge 21.1.235+**, for Minecraft 1.21.1 first.

The universal jars are on CurseForge/Modrinth. If you want a single-loader build (`fabric-*` /
`neoforge-*`), grab it from [GitHub Releases](https://github.com/retiredroca/mc-storage-area-network/releases).

Each mod is versioned on its own: a release only bumps the mod that changed, so Storage Network,
Crafting Network, Network Routing and the API can be updated independently. Every GitHub release
lists all current jars together, so you can always grab a matching set from one place.

## Configuration

- Fabric: `config/<mod>.json` — NeoForge: `config/<mod>-server.toml`

| Mod | Key | Default | Description |
|-----|-----|---------|-------------|
| `mc_storage_area_network` | `flattenDepth` | `1` | How many shulker-box levels to flatten (1 = boxes in containers, 2 = boxes in boxes). |
| `mc_storage_area_network` | `boxRowHidden` | `false` | Hide raw shulker-box stacks so only their contents show. |
| `mc_storage_area_network` | `sameTypeFirst` | `true` | Prefer inserting into boxes that already hold the same item. |
| `mc_storage_area_network` | `ownershipEnabled` | `true` | Terminals show only global + owner-placed storage. |
| `mc_storage_area_network` | `teamSharing` | `true` | Scoreboard-team members share their placed storage. |
| `storage_network` | `maxTier` | `5` | Highest upgrade tier allowed (0–5). |
| `crafting_network` | `maxTier` | `5` | Highest upgrade tier allowed (0–5). |

## Compatibility

- Works with any container that exposes a standard inventory — vanilla and most modded storage.
- No JEI/REI required; crafting uses the vanilla recipe book.
- Client & server: install on both for full functionality.

## License

This project's own source code is licensed under
[Apache-2.0](https://github.com/retiredroca/mc-storage-area-network/blob/main/LICENSE).

## Credits & third-party notices

- **Minecraft** is a trademark of Mojang Synergies AB. This is an unofficial, fan-made
  modification and is **not affiliated with, endorsed by, or sponsored by** Mojang Studios or
  Microsoft Corporation.
- Developed against the **official Mojang mappings**. The mappings are used only for development
  and are **not redistributed** by this project. The mappings are (c) 2020 Microsoft Corporation,
  provided "as-is"; use and modification of Minecraft: Java Edition is governed by the
  [Minecraft EULA](https://account.mojang.com/documents/minecraft_eula).
- Built on **Fabric Loader/API** (Apache-2.0) and **NeoForge** (LGPL-2.1).

## Author

Retired Roca
