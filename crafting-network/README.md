# Crafting Network

**Craft using every chest near you — no more running back and forth for materials.**

Crafting Network turns nearby storage into a shared item network and adds a family of upgradable
**Terminals** that craft, smelt, blast, smoke and brew directly from it. Pull ingredients from your
chests, process them, and send the results back — automatically.

![Crafting Network demo](https://raw.githubusercontent.com/retiredroca/mc-storage-area-network/main/crafting-network/crafting-network.gif)

---

## Features

- **Crafting Terminal** — an upgradable crafting table with a recipe book that crafts using items drawn from the network plus your inventory.
- **Processor Terminals** — **Smelting**, **Blasting**, **Smoking** and **Brewing** terminals that pull inputs and fuel from the network, process them, and push results back.
- **Recipe-book selector** — pick a recipe from the book; the terminal gathers the ingredients for you.
- **Sources dropdown** — scope ingredients to *All Storage*, your *Inventory*, or a single container.
- **Smart output routing** — crafted items go to a **Network Share Terminal** first, then a container already holding that item, then your inventory, then the nearest container.
- **Range tiers** — upgrade terminals to scan a larger area (1×1 up to 11×11 chunks).
- **Ownership & privacy** — terminals only use global (worldgen) storage plus storage placed by the terminal's owner.

## The Crafting Terminal

- Open it to see the vanilla 3×3 grid and recipe book.
- Select a recipe; the terminal reports whether you have the materials and, on click, crafts it.
- **Click** the result to craft one; **shift-click** to craft as many as your materials allow.
- Shift-click items in your inventory to deposit them into the network.

## Processor Terminals

Each processor terminal runs automatically once it has a recipe to work on:

| Terminal | Processes | Notes |
|----------|-----------|-------|
| **Smelting Terminal** | Furnace recipes | Needs fuel from the network |
| **Blasting Terminal** | Blast-furnace recipes | Faster than smelting |
| **Smoking Terminal** | Smoker recipes | Fast food cooking |
| **Brewing Terminal** | Potions | Select a target potion from the brew book; pulls water, blaze powder and ingredients |

Higher tiers process **faster** (tier speed multiplier) and scan more chunks.

## Ownership & privacy

- Terminals are **openable by anyone** but **breakable only by their owner**.
- A terminal only sees **global (worldgen) storage** and **storage placed by the terminal's owner** (plus scoreboard-team members, if enabled).
- Configurable on the server via the required MC Storage Area Network API.

## Getting started

1. Craft a **Crafting Terminal** and place it near your storage.
2. Right-click to open; pick a recipe from the recipe book.
3. Craft the processor terminals you need and let them run.
4. Add a **Network Share Terminal** (from Storage Network) so outputs are collected automatically.

## Recipes

All terminals are shaped 3×3: **8× Copper Ingot** around a core block.

| Result | Core block |
|--------|-----------|
| **Crafting Terminal** | Crafting Table |
| **Smelting Terminal** | Furnace |
| **Blasting Terminal** | Blast Furnace |
| **Smoking Terminal** | Smoker |
| **Brewing Terminal** | Brewing Stand |

**Tier upgrades** place the previous terminal in the center of a 3×3 and surround it with 4 ingots/gems:

| Tier | Material |
|------|----------|
| Iron (1) | 4× Iron Ingot |
| Gold (2) | 4× Gold Ingot |
| Emerald (3) | 4× Emerald |
| Diamond (4) | 4× Diamond |
| Netherite (5) | 4× Netherite Ingot |

**Scan radius by tier:** Copper 1×1 → Iron 3×3 → Gold 5×5 → Emerald 7×7 → Diamond 9×9 → Netherite 11×11 chunks.

> Pair with **Storage Network** for the Storage Terminal and the Network Share Terminal used for output collection.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric Loader + Fabric API, or NeoForge 21.1.235+ |
| Java | 21 |
| Side | Client & Server |
| Dependencies | MC Storage Area Network (`mc_storage_area_network`) — **required**, install separately; Storage Network recommended for the share terminal |

## Configuration

- Fabric: `config/crafting_network.json`
- NeoForge: `config/crafting_network-server.toml`

| Key | Default | Description |
|-----|---------|-------------|
| `maxTier` | `5` | Highest upgrade tier allowed on the server (0–5). |

The API's ownership options (`ownershipEnabled`, `teamSharing`) live in the `mc_storage_area_network` config.

## Compatibility

- Works with any container that exposes a standard inventory (vanilla and most modded storage).
- JEI/REI are not required; the terminal uses the vanilla recipe book.

## License

[Apache-2.0](LICENSE)

## Author

Retired Roca
