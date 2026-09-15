# Storage Network

**One searchable interface for every chest, barrel and container near you.**

Tired of running between chests? Place a **Storage Terminal** and it scans the chunks around it,
gathering every inventory-bearing block — vanilla or modded — into a single, searchable index.
Browse, search, sort, take and deposit from one screen.

![Storage Network demo](https://raw.githubusercontent.com/retiredroca/mc-storage-area-network/main/versions/1.21.1/storage-network/storage-network.gif)

---

## Features

- **Storage Terminal** — aggregates chests, barrels, hoppers, shulker boxes and any modded container within range into one searchable grid.
- **Search & sort** — search by name or id; sort by name, type, tag, mod or equipment slot.
- **Virtual item grid** — stacks are merged and counted, with draggable search and source panels.
- **Range tiers** — upgrade the terminal to scan a larger area (1×1 up to 11×11 chunks).
- **Output Terminal** — a collection-only sink for crafted outputs (see below).
- **Shulker flattening** — shulker-box contents (and boxes inside boxes) appear as ordinary items.
- **Ownership & privacy** — a terminal only shows global (worldgen) storage plus storage placed by the terminal's owner.

## The Storage Terminal

- Aggregates all discovered containers into one searchable list.
- Left-click an entry to take a stack; holds shift to take more.
- Shift-click items in your inventory to deposit them into the network.
- The **source dropdown** lets you scope the view to *All Storage*, your *Inventory*, or a single container.

## The Output Terminal

A companion block that **receives crafted output** from Crafting Network terminals and holds it for you.

- Collection-only: its contents are hidden from normal terminal listings.
- **Crouch + right-click** it to toggle whether it is shown in the network (then its contents appear in the Storage Terminal grid).
- Anyone can open it and deposit items; only the owner can break it.
- Rendered as a tinted ender chest.

## Ownership & privacy

Storage Network records who places each container:

- **Global/worldgen containers** (dungeon chests, etc.) are visible to everyone.
- **Player-placed storage** is visible only to the terminal's owner (and, optionally, their scoreboard team).
- Terminals are **openable by anyone** but **breakable only by their owner**.

All of this is configurable on the server (see below).

## Getting started

1. Craft a **Storage Terminal**: 8 copper ingots around a chest.
2. Place it near your storage and right-click to open.
3. Upgrade it with range tiers (see recipes below) to expand the scan radius.

## Recipes

| Result | Recipe |
|--------|--------|
| **Storage Terminal** | 3×3: 8× Copper Ingot + 1× Chest (center) |
| **Output Terminal** | 3×3: 4× Chest around a Storage Terminal |
| **Iron (tier 1) upgrade** | Terminal (center) + 4× Iron Ingot |
| **Gold (tier 2) upgrade** | Iron Terminal (center) + 4× Gold Ingot |
| **Emerald (tier 3) upgrade** | Gold Terminal (center) + 4× Emerald |
| **Diamond (tier 4) upgrade** | Emerald Terminal (center) + 4× Diamond |
| **Netherite (tier 5) upgrade** | Diamond Terminal (center) + 4× Netherite Ingot |

**Scan radius by tier:** Copper 1×1 → Iron 3×3 → Gold 5×5 → Emerald 7×7 → Diamond 9×9 → Netherite 11×11 chunks
(automatically capped by the server's view/simulation distance).

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric Loader + Fabric API, or NeoForge 21.1.235+ |
| Java | 21 |
| Side | Client & Server |
| Dependency | MC Storage Area Network (`mc_storage_area_network`) — **required**, install separately |

> Easiest install: the **bundle** `universal-bundle-all.<version>.jar` (or `universal-bundle-storage`
> for just this one) bundles this mod, Crafting Network and the API. To install this mod on its own,
> also drop in the universal API jar (`universal_mc_san_api.<version>.jar`).

## Configuration

- Fabric: `config/storage_network.json`
- NeoForge: `config/storage_network-server.toml`

| Key | Default | Description |
|-----|---------|-------------|
| `maxTier` | `5` | Highest upgrade tier allowed on the server (0–5). |

The API's ownership options (`ownershipEnabled`, `teamSharing`) live in the `mc_storage_area_network` config.

## Compatibility

- Works with any container that exposes a standard inventory (vanilla and most modded storage).
- JEI/REI are not required.

## License

[Apache-2.0](LICENSE)

## Author

Retired Roca
