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
- **Recipe-book selector** — pick a recipe from the book; it only starts if the materials (and, for processors, fuel) are available.
- **Batch crafting** — the result slot shows how many you can craft; **click** to craft one, **shift-click** to craft a stack (or as many as the network allows).
- **No redstone, no item filters** — say goodbye to hopper chains, sorters and filter systems; a Terminal replaces your whole automated storage setup.
- **Craft even when your inventory is full** — output is routed into the network, or straight back to your inventory with the *Inventory first* option.
- **"Shulkers first" toggle** — a per-machine checkbox that makes output fill shulker boxes in the network before other containers.
- **"Inventory first" toggle** — a per-machine checkbox that sends crafted output to your inventory first. On processor terminals it drops the output next to the machine when you're within 2 blocks, so you can pick it up.
- **XP collection** — Smelting/Blasting/Smoking terminals build up experience; **crouch + right-click** to collect it (owner and scoreboard team).
- **Sources dropdown** — scope ingredients to *All Storage*, your *Inventory*, or a single container.
- **Smart output routing** — crafted items go to an **Output Terminal** first, then a container already holding that item, then the nearest container, then your inventory.
- **Range tiers** — upgrade terminals to scan a larger area (1×1 up to 11×11 chunks).
- **Ownership & privacy** — terminals only use global (worldgen) storage plus storage placed by the terminal's owner.

## The Crafting Terminal

- Open it to see the vanilla 3×3 grid and recipe book.
- Select a recipe; it only places if the network (or your inventory) has the materials.
- The result slot shows **how many** you can make. **Click** to craft one; **shift-click** to craft a stack, or as many as your materials allow.
- Shift-click items in your inventory to deposit them into the network.
- The **Shulkers first** checkbox next to the result fills shulker boxes in the network before other containers; the **Inventory first** checkbox below it sends crafted output to your inventory first (on processor terminals it drops the output next to the machine when you're within 2 blocks).

## Processor Terminals

Each processor terminal runs automatically once it has a recipe to work on; clicking a recipe only starts it if the input and fuel are available:

| Terminal | Processes | Notes |
|----------|-----------|-------|
| **Smelting Terminal** | Furnace recipes | Needs fuel from the network; earns XP |
| **Blasting Terminal** | Blast-furnace recipes | Faster than smelting; earns XP |
| **Smoking Terminal** | Smoker recipes | Fast food cooking; earns XP |
| **Brewing Terminal** | Potions | Select a target potion; pulls water, blaze powder and ingredients and brews **one batch (3 potions)**, then waits until you re-select the target |

- **Collect XP:** crouch + right-click a Smelting/Blasting/Smoking terminal to collect the experience it has earned.
- **Shulkers first:** the checkbox makes output fill shulker boxes before other containers.
- Higher tiers process **faster** (tier speed multiplier) and scan more chunks.

## Ownership & privacy

- Terminals are **openable by anyone** but **breakable only by their owner**.
- A terminal only sees **global (worldgen) storage** and **storage placed by the terminal's owner** (plus scoreboard-team members, if enabled).
- Configurable on the server via the required MC Storage Area Network API.

## Getting started

1. Craft a **Crafting Terminal** and place it near your storage.
2. Right-click to open; pick a recipe from the recipe book.
3. Craft the processor terminals you need and let them run.
4. Add an **Output Terminal** (from Storage Network) so outputs are collected automatically.

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

> Pair with **Storage Network** for the Storage Terminal and the Output Terminal used for output collection.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric Loader + Fabric API, or NeoForge 21.1.235+ |
| Java | 21 |
| Side | Client & Server |
| Dependencies | MC Storage Area Network (`mc_storage_area_network`) — **required**, install separately; Storage Network recommended for the output terminal |

> Easiest install: the **bundle** `universal-bundle-all.<version>.jar` (or `universal-bundle-crafting`
> for just this one) bundles this mod, Storage Network and the API. To install this mod on its own,
> also drop in the universal API jar (`universal_mc_san_api.<version>.jar`).

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
