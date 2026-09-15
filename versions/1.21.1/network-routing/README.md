# Network Routing

**Label your chests, and let the network sort itself.**

Network Routing lets you tag any container with an item filter. When the network inserts items —
depositing from a terminal, crafting output, or the Routing Terminal's own sorter — containers whose
label matches the item are filled **first**. Label a chest `#c:ores` and another `minecraft:iron_ingot`
and your ore and iron go to the right place automatically.

---

## Features

- **Routing Linker** — a handheld tool: right-click any container to open its filter editor; crouch + right-click to clear it. Grass, stone and other non-containers are left untouched.
- **Routing Terminal** — a chest-shaped computer that binds to the nearest Storage Terminal and manages the whole network from one screen.
- **Container filters** — mix **items** (`minecraft:iron_ingot`), **item tags** (`#c:ores`) and **mod ids** (`create`) in a single list. Orders are ignored: a stack matches if **any** entry matches.
- **Routing priority** — matching labeled containers are tried first on every insert; when several match, the **nearest** wins. If a labeled chest is full, items fall back to normal routing.
- **Auto-sort** — the Routing Terminal moves stray stacks into their nearest matching labeled container.
- **Defrag** — merges partial stacks inside each container to free slots.
- **Trim** — evicts items from a labeled container that don't match its filter: nearest **unfiltered** container first, then the **Output Terminal**, otherwise the operation is skipped.

## The Routing Linker

- Right-click a container while holding it to open that container's filter editor.
- Add an entry by typing it into the filter box and pressing **Add**; click an entry to remove it.
- **Crouch + right-click** a container to clear its filter.
- The editor is slotless — nothing is stored on the tool, so one Linker can label everything.

## The Routing Terminal

- Binds to the **nearest Storage Terminal within range** and reuses its scan radius, container list and tier colour.
- No Storage Terminal in range? It shows *"No network to connect to. Please install a Storage Terminal."* until you place one.
- Lists every container the bound Storage Terminal can see; select one to edit its filter.
- Three toggles, all **off** by default, plus one-shot buttons:

| Control | What it does |
|---------|--------------|
| **Sort** | Continuously move stacks into their nearest matching labeled container |
| **Sort now** | Run one sort pass |
| **Defrag** | Continuously merge partial stacks to free slots |
| **Defrag now** | Run one defrag pass |
| **Trim** | Continuously evict non-matching items from labeled containers |
| **Trim now** | Run one trim pass |

Automatic passes run about every 2 seconds while their toggle is on.

## Filters

| Entry | Matches |
|-------|---------|
| `minecraft:iron_ingot` | that item type, ignoring NBT/components |
| `#c:ores` | any item in the tag (datapack-friendly) |
| `create` | every item from the mod `create` |

## Getting started

1. Craft a **Routing Linker** and right-click the container you want to dedicate (e.g. an ore chest).
2. Type `#c:ores` and press **Add**.
3. Craft a **Routing Terminal** and place it near a Storage Terminal to manage and sort the whole network.
4. Turn on **Sort** (or press **Sort now**) to redistribute.

## Recipes

| Result | Recipe |
|--------|--------|
| **Routing Terminal** | 3×3: 4× Redstone Block around a **Storage Terminal** (center) |
| **Routing Linker** | 3×3: Copper Ingot (left, right, bottom) + Redstone (center) + Lightning Rod (top) |

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric Loader + Fabric API, or NeoForge 21.1.235+ |
| Java | 21 |
| Side | Client & Server |
| Dependencies | MC Storage Area Network (`mc_storage_area_network`) — **required**, install separately. **Storage Network** provides the Storage Terminal the Routing Terminal binds to (and the deposit flow routing applies to); **Crafting Network** adds crafted-output routing. Without a host mod there is nothing to route. |

> Easiest install: the **bundle** `universal-bundle-all.<version>.jar` (or `universal-bundle-routing`
> for just this one) bundles this mod, the API and the other gameplay mods. To install this mod on its
> own, also drop in the universal API jar (`universal_mc_san_api.<version>.jar`).

## Configuration

None yet — the routing defaults are built in (host search radius 2 chunks, scan every 20 ticks,
maintenance every 40 ticks). The API's ownership options (`ownershipEnabled`, `teamSharing`) live in
the `mc_storage_area_network` config.

## Compatibility

- Labels work on any container that exposes a standard inventory (vanilla and most modded storage).
- The **Output Terminal** (from Storage Network) is used as the fallback destination when trimming.
- Labels are stored per-dimension in the world save, keyed by block position — breaking a container
  leaves a stale entry that is simply ignored.

## License

[Apache-2.0](LICENSE)

## Author

Retired Roca
