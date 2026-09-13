# Storage Network

![Storage Network demo](storage-network.gif)

A Minecraft 1.21.1 mod for **NeoForge** and **Fabric** that adds a **Storage Terminal** — a block that scans nearby chunks for any storage container and lets you access, search, and manage all of your items from a single, fast interface.

> Currently in development. Tested in single-player and on LAN/dedicated servers.

## Features

- **Storage Terminal block** — place it down and open a searchable interface that aggregates the contents of every inventory-bearing block in range (chests, barrels, furnaces, hoppers, and more).
- **Click-to-extract & deposit** — click an item in the terminal to pull it into your inventory (shift-click for a full stack), or shift-click items in your inventory to push them into the network.
- **Live search** — type to filter items by name or ID.
- **Same feel as a double chest** — the terminal renders with the familiar double-chest window and works like a normal container.
- **Range upgrades** — six tiers that expand the scan radius:
  - Tier 0 → 1×1 chunks (base) - no upgrade needed
  - Tier 1 → 3×3 chunks
  - Tier 2 → 5×5 chunks
  - Tier 3 → 7×7 chunks
  - Tier 4 → 9×9 chunks
  - Tier 5 → 11×11 chunks (max)
- **Server-aware range limits** — tiers are automatically capped so the scan radius never exceeds the server's `view-distance`/`simulation-distance` from `server.properties`, and can be further limited in a config file.
- **Fast scanning** — the terminal only scans actual inventory-bearing block entities, so it stays responsive even with many containers nearby.
- **Server-authoritative** — works in single-player, LAN, and dedicated servers; no client-side hacks.
- **Aggregate reads** — the terminal shows the total count of each item across the whole network.

## Requirements

- **Minecraft**: 1.21.1
- **NeoForge** edition: NeoForge 21.1.235 or later
- **Fabric** edition: Fabric Loader 0.16.14 or later + [Fabric API](https://modrinth.com/mod/fabric-api)

## Installation

**NeoForge edition**

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1.
2. Place `neoforge-storage_network-1.YY.MM.DD.HH.jar` from the [Releases](https://github.com/RetiredRoca/storage-network/releases) page (NeoForge edition) into your `mods/` folder.
3. Launch the game.

**Fabric edition**

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1, plus Fabric API.
2. Place the Fabric `fabric-storage_network-1.YY.MM.DD.HH.jar` from the [Releases](https://github.com/RetiredRoca/storage-network/releases) page into your `mods/` folder.
3. Launch the game.

## Building from source

Requires **Java 21** (auto-provisioned by the Gradle toolchain).

**NeoForge edition** (from the `neoforge/` folder):

```bash
cd neoforge
./gradlew build
```

The built mod JAR will be at `neoforge/build/libs/neoforge-storage_network-1.YY.MM.DD.HH.jar`.

**Fabric edition** (from the `fabric/` folder):

```bash
cd fabric
./gradlew build
```

The built mod JAR will be at `fabric/build/libs/fabric-storage_network-1.YY.MM.DD.HH.jar`.

> Note: run `build` only. Do **not** run `runClient`/`runServer` during development if you prefer to test via a launcher (e.g. Prism Launcher) pointing at an existing installation.

## Usage

1. Craft the **Storage Terminal** and place it somewhere with your storage nearby.
2. **Right-click** the terminal to open the interface.
3. Browse/search the aggregated contents.
4. **Click** an item to take one, **shift-click** to take a full stack.
5. **Shift-click** items from your own inventory to deposit them into the network.
6. Craft and apply a **Range Upgrade** (right-click it on the terminal) to increase scan range.

## Recipes

Recipes for the terminal and the five range upgrades are included in `neoforge/src/main/resources/data/storage_network/recipe/` (NeoForge) and `fabric/src/main/resources/data/storage_network/recipe/` (Fabric).

## Configuration

The max tier can be limited per server. The effective tier is also capped automatically by the smaller of the server's `view-distance` and `simulation-distance` from `server.properties`, so upgrades never scan further than chunks are actually generated/loaded.

- **NeoForge**: `config/storage_network-server.toml` (generated on first run) — `maxTier` hard caps the highest tier that may be applied (default `5`).
- **Fabric**: `config/storage_network.json` (generated on first run) — `maxTier` behaves the same (default `5`).

## Configuration / Credits

- **Mod ID**: `storage_network`
- **Package**: `com.retiredroca.storagenetwork`
- **Server config (NeoForge)**: `config/storage_network-server.toml`
- **Server config (Fabric)**: `config/storage_network.json`

## License

Released under the [Apache License 2.0](LICENSE).

*NeoForge edition built with the [MDK](https://github.com/neoforged/MDK); Fabric edition built with the [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod). Minecraft, NeoForge and Fabric are property of their respective owners.*
