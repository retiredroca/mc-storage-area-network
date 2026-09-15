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

## Features

- **One network for all your storage** — chests, barrels, hoppers, shulker boxes, and any modded inventory-bearing block within range.
- **No redstone, no item filters** — say goodbye to hopper chains, sorters and filter systems; one Terminal replaces your whole automated storage setup.
- **Craft even when your inventory is full** — crafted output is routed into the network, or straight back to your inventory with the *Inventory first* option (on processor terminals it drops the output next to the machine when you're within 2 blocks).
- **Storage Terminal** — a searchable, sortable grid of every item in the network, with draggable search and source panels. Search by name or id; sort by name, type, tag, mod or equipment.
- **Crafting Terminal** — an upgradable crafting table with a full recipe book that crafts using items drawn from the network plus your inventory. The result shows how many you can make; **click** to craft one, **shift-click** to craft a stack.
- **Processor Terminals** — Smelting, Blasting, Smoking and Brewing terminals pull inputs and fuel from the network, process automatically, and push results back. Smelting/Blasting/Smoking earn experience you can collect with **crouch + right-click**.
- **"Shulkers first" toggle** — a per-machine checkbox that fills shulker boxes in the network before other containers.
- **Sources dropdown** — scope any terminal to *All Storage*, your *Inventory*, or a single container.
- **Smart output routing** — crafted items go to an **Output Terminal** first, then a container already holding that item, then the nearest container, then your inventory.
- **Range tiers** — upgrade terminals to scan from **1×1 up to 11×11 chunks** (capped by the server's view/simulation distance).
- **Shulker flattening** — shulker-box contents (and boxes inside boxes) appear as ordinary items, ready to craft with.
- **Ownership & privacy** — a terminal only shows global (worldgen) storage plus storage placed by the terminal's owner.

## Storage Terminal

Place it near your storage and it scans the surrounding chunks, gathering every inventory block into
one searchable index.

- Left-click an entry to take a stack; shift to take more.
- Shift-click items in your inventory to deposit them back into the network.
- Use the source dropdown to view everything, just your inventory, or one container.

## Output Terminal

A collection-only sink that **receives crafted output** and holds it for you.

- Its contents are hidden from normal terminal listings.
- **Crouch + right-click** to toggle whether it is shown in the network (its contents then appear in the Storage Terminal grid).
- Anyone can open and deposit into it; only the owner can break it.
- Rendered as a tinted ender chest.

> Requires **Storage Network** (it's part of that mod).

## Crafting Terminal & Processor Terminals

- The **Crafting Terminal** shows the vanilla 3×3 grid and recipe book. Selecting a recipe only places it if the network (or your inventory) has the materials; the result slot shows **how many** you can make. **Click** to craft one, **shift-click** to craft a stack (or as many as you can).
- The **Smelting / Blasting / Smoking Terminals** run automatically once they have a recipe and fuel, drawing inputs and fuel from the network. **Crouch + right-click** to collect the experience they earn (owner and scoreboard team).
- The **Brewing Terminal** lets you pick a target potion; it pulls water bottles, blaze powder and ingredients and brews **one batch (3 potions)**, then waits until you re-select the target.
- Every machine has a **"Shulkers first"** checkbox that fills shulker boxes before other containers, and an **"Inventory first"** checkbox that sends crafted output straight to your inventory.
- Higher tiers process **faster** as well as scanning further.

## Ownership & privacy

Storage Network records who places each container:

- **Global / worldgen containers** (dungeon chests, etc.) are visible to everyone.
- **Player-placed storage** is visible only to the terminal's owner — and, optionally, to their scoreboard team.
- Terminals are **openable by anyone** but **breakable only by their owner**.

All of this is configurable on the server.

## Getting started

1. Craft a **Storage Terminal** (8 copper ingots around a chest) and place it near your storage.
2. Craft a **Crafting Terminal** (8 copper ingots around a crafting table) to craft from the network.
3. Craft the processor terminals you need (8 copper ingots around a Furnace / Blast Furnace / Smoker / Brewing Stand).
4. Add an **Output Terminal** (4 chests around a Storage Terminal) to collect crafted output automatically.
5. Upgrade any terminal with range tiers to cover more chunks.

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

**Bundle (recommended):** drop `universal-bundle-all.<version>.jar` into your `mods/` folder — it
bundles the API, Storage Network, Crafting Network and Network Routing in a single file. Want just
one gameplay mod? Use `universal-bundle-storage`, `universal-bundle-crafting` or
`universal-bundle-routing` instead.

**Or install the mods individually:**

1. Install **Fabric Loader + Fabric API**, or **NeoForge 21.1.235+**, for Minecraft 1.21.1.
2. Install the **MC Storage Area Network API** jar (`universal_mc_san_api.<version>.jar`) — required
   by every gameplay mod.
3. Drop the **Storage Network** (`universal-storage-network.<version>.jar`), **Crafting Network**
   (`universal-crafting-network.<version>.jar`) and/or **Network Routing**
   (`universal-network-routing.<version>.jar`) jar into `mods/`.

> Never use a bundle together with the individual jars — the same mods would load twice.

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
