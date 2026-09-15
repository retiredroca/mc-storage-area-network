# MC Storage Area Network API

A small, standalone, multi-loader API for exposing **item sources** to mc-storage-area-network hosts
(Storage Network, Crafting Network, and anything else that queries a shared network).

It provides:

- **Chunk storage scanning** — find inventory-bearing blocks near a host.
- **Item-source registration** — other mods contribute virtual item sources (shulker boxes,
  backpacks, linked inventories, …) without depending on a gameplay mod.
- **Nested sources** — expose sub-containers (e.g. a shulker box inside a chest) as child rows.
- **Hidden-item filters** — hide "container" items that are represented by their contents instead.
- **Shulker-box helpers** — read/write shulker box contents as a normal item list.

The mod id is `mc_storage_area_network`. Package: `com.retiredroca.mcstorageareanetwork.api`.

---

## Installation (players)

Download the **universal** jar:

```
universal_mc_san_api.1.0.<yymmddhh>.jar
```

It is a thin container that bundles the Fabric and NeoForge builds; each loader loads only its own
nested copy. Drop it in `mods/` on either loader (no separate per-loader download needed).

- **Fabric:** requires Fabric Loader + Fabric API.
- **NeoForge:** requires NeoForge `21.1.235+` for Minecraft `1.21.1`.

Hosts such as Storage Network / Crafting Network declare `mc_storage_area_network` as a dependency, so
install this jar alongside them.

---

## Using the API (modders)

### Repository

```groovy
repositories {
    maven {
        name = 'itemNetwork'
        url = 'https://raw.githubusercontent.com/retiredroca/mc-storage-area-network/main/repo'
    }
}
```

### Dependency

```groovy
// Fabric
modImplementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-fabric-1.21.1:1.0.+'

// NeoForge
implementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-neoforge-1.21.1:1.0.+'
```

The artifact id ends with the **Minecraft version** (`-1.21.1`), so one Maven repository serves every
supported version. The API version is `1.0.<yymmddhh>` (a date-based patch) and only advances when the
API source changes — releasing the gameplay mods does not bump it. `1.0.+` tracks the latest `1.0.x`
build. Sources jars are published alongside (`-sources.jar`). The universal install jar is published as:

```
com.retiredroca.mcstorageareanetwork:mc-storage-area-network-1.21.1:1.0.<yymmddhh>
```

> Compile against the **per-loader** artifact for your mod. The `com.retiredroca.mcstorageareanetwork.api`
> package is mapping-specific (Fabric intermediary / NeoForge Mojang), which is why the API ships
> per-loader artifacts and a bundled universal install jar rather than a single class-merged jar.

### Registering an item source

Implement `com.retiredroca.mcstorageareanetwork.api.ItemSource`:

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
    "mc_storage_area_network": [
      "com.example.mymod.MySource"
    ]
  }
}
```

Each entrypoint value is instantiated and registered as an `ItemSource`.

**NeoForge** — send an InterModComms message during construction:

```java
InterModComms.sendTo("mc_storage_area_network", "register_item_source", () -> new MySource());
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
./gradlew build           # API + both hosts + every bundle -> build/release/ (final file names)
./gradlew releaseJars     # just collect the release jars into build/release/
./gradlew publishApi      # per-loader jars -> mavenLocal (for local host builds)
./gradlew publishRepo     # per-loader jars + universal API -> ./repo (committed maven for distribution)
```

The `repo/` directory in this repository is the published Maven repository consumed by hosts and
modders. Commit it after `publishRepo`; it keeps only the newest 3 versions per artifact so it stays
small.

Component versions live in `versions.properties` at the repository root and are bumped by the release
workflow (only for the component that changed), never by the clock at build time.

## Compatibility

| | |
|---|---|
| Minecraft | 1.21.1 |
| Fabric | Loader 0.16.14+ / Fabric API |
| NeoForge | 21.1.235+ |
| Java | 21 |
| API version | `1.0.<yymmddhh>` (date-based patch) |
