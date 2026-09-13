# Item Network API

A small, standalone, multi-loader API for exposing **item sources** to item-network hosts
(Storage Central, Crafting Central, and anything else that queries a shared network).

It provides:

- **Chunk storage scanning** — find inventory-bearing blocks near a host.
- **Item-source registration** — other mods contribute virtual item sources (shulker boxes,
  backpacks, linked inventories, …) without depending on a gameplay mod.
- **Nested sources** — expose sub-containers (e.g. a shulker box inside a chest) as child rows.
- **Hidden-item filters** — hide "container" items that are represented by their contents instead.
- **Shulker-box helpers** — read/write shulker box contents as a normal item list.

The mod id is `item_network_api`. Package: `com.retiredroca.itemnetwork.api`.

---

## Installation (players)

Download the **universal** jar:

```
item-network-api-1.0.0-universal.jar
```

It is a thin container that bundles the Fabric and NeoForge builds; each loader loads only its own
nested copy. Drop it in `mods/` on either loader (no separate per-loader download needed).

- **Fabric:** requires Fabric Loader + Fabric API.
- **NeoForge:** requires NeoForge `21.1.235+` for Minecraft `1.21.1`.

Hosts such as Storage Central / Crafting Central declare `item_network_api` as a dependency, so
install this jar alongside them.

---

## Using the API (modders)

### Repository

```groovy
repositories {
    maven {
        name = 'itemNetwork'
        url = 'https://raw.githubusercontent.com/retiredroca/item-network/main/repo'
    }
}
```

### Dependency

```groovy
// Fabric
modImplementation 'com.retiredroca.itemnetwork:item_network_api-fabric:1.0.0'

// NeoForge
implementation 'com.retiredroca.itemnetwork:item_network_api-neoforge:1.0.0'
```

Sources jars are published alongside (`-sources.jar`). The universal install jar is published as:

```
com.retiredroca.itemnetwork:item-network-api:1.0.0:universal
```

> Compile against the **per-loader** artifact for your mod. The `com.retiredroca.itemnetwork.api`
> package is mapping-specific (Fabric intermediary / NeoForge Mojang), which is why the API ships
> per-loader artifacts and a bundled universal install jar rather than a single class-merged jar.

### Registering an item source

Implement `com.retiredroca.itemnetwork.api.ItemSource`:

```java
public final class MySource implements ItemSource {
    @Override public String getSourceName() { return "My Source"; }

    @Override public void refresh(ServerLevel level, BlockPos hostPos, int chunkRadius) {
        // Re-read state for this host (key your state by hostPos).
    }

    @Override public List<ItemStack> enumerate(BlockPos hostPos) { ... }

    @Override public List<NestedSource> nestedSources(BlockPos hostPos) {
        return List.of(); // optional sub-rows
    }

    @Override public int extract(BlockPos hostPos, ItemStack item, int maxCount, boolean shulkerFirst) { ... }

    @Override public ItemStack insert(BlockPos hostPos, ItemStack stack, boolean shulkerFirst) { ... }

    @Override public ItemStack insertIntoChild(BlockPos hostPos, BlockPos containerPos, String childLabel,
            ItemStack stack, boolean shulkerFirst) { ... } // optional
}
```

Then register it:

**Fabric** — add an entrypoint to your `fabric.mod.json`:

```json
{
  "entrypoints": {
    "item_network_api": [
      "com.example.mymod.MySource"
    ]
  }
}
```

Each entrypoint value is instantiated and registered as an `ItemSource`.

**NeoForge** — send an InterModComms message during construction:

```java
InterModComms.sendTo("item_network_api", "register_item_source", () -> new MySource());
```

You can also register directly at mod init via `ItemSourceRegistry.register(source)` on either
loader, and hide container items with:

```java
ItemSourceRegistry.addHiddenItemFilter(stack -> stack.is(Items.SHULKER_BOX));
```

### Reading storage (hosts)

```java
ItemScanner scanner = ItemNetworkServices.scanner();          // installed by the API
List<ScannedStorage> storages = scanner.scan(level, pos, chunkRadius);

for (ItemSource source : ItemSourceRegistry.getSources()) {
    source.refresh(level, pos, chunkRadius);
    List<ItemStack> items = source.enumerate(pos);
}
```

`ScannedStorage` (`pos()`, `label()`, `enumerate()`, `count()`, `extract()`, `insert()`,
`supportsExtraction()`) is the loader-neutral view of a discovered container.

### Shulker boxes

`ShulkerBoxHelper` reads/writes shulker-box contents without touching loader APIs:

```java
boolean box = ShulkerBoxHelper.isShulkerBox(stack);
List<ItemStack> contents = ShulkerBoxHelper.contents(stack);
ItemStack leaf = ShulkerBoxHelper.stackAt(container, new int[]{ slot });
```

---

## API surface

| Type | Purpose |
|------|---------|
| `ItemSource` | A virtual item source attached to a host network. |
| `NestedSource` | A child row of an `ItemSource` (label, position, items, counts). |
| `ItemSourceRegistry` | Register sources + hidden-item filters; refresh all for a host. |
| `ScannedStorage` | Loader-neutral view of a scanned container. |
| `ItemScanner` | Loader-specific chunk scanner (`scan(level, center, chunkRadius)`). |
| `ItemNetworkServices` | Holds/resolves the active `ItemScanner`. |
| `ShulkerBoxHelper` | Read/write shulker-box contents. |

### Semantics

- **`hostPos`** — every source call receives the host block position, so one registered source can
  serve many hosts at once by keying its state by `hostPos`.
- **`refresh`** — called whenever a host rescans; discard stale state for that `hostPos` and re-read.
- **`shulkerFirst`** — when `true`, prefer shulker-box contents over plain container slots.
- **Hidden items** — filtered from plain listings but still reachable through sources.

---

## Building / publishing

```bash
./gradlew build           # universal bundle -> build/libs/item-network-api-1.0.0-universal.jar
./gradlew publishApi      # per-loader jars -> mavenLocal (for local host builds)
./gradlew publishRepo     # per-loader jars + bundle -> ./repo (committed maven for distribution)
```

The `repo/` directory in this repository is the published Maven repository consumed by hosts and
modders. Commit it after `publishRepo`.

## Compatibility

| | |
|---|---|
| Minecraft | 1.21.1 |
| Fabric | Loader 0.16.14+ / Fabric API |
| NeoForge | 21.1.235+ |
| Java | 21 |
| API version | 1.0.0 |
