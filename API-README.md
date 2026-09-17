# MC Storage Area Network API

A small, standalone, multi-loader API for the mc-storage-area-network suite — Storage Network,
Crafting Network, Network Routing, Remote Access Terminal, and anything else that queries or hooks
into a shared network.

It provides:

- **Chunk storage scanning** — find inventory-bearing blocks near a host.
- **Item-source registration** — other mods contribute virtual item sources (shulker boxes,
  backpacks, linked inventories, …) without depending on a gameplay mod.
- **Nested sources** — expose sub-containers (e.g. a shulker box inside a chest) as child rows.
- **Hidden-item filters** — hide "container" items that are represented by their contents instead.
- **Shulker-box helpers** — read/write shulker box contents as a normal item list.
- **Shared interaction dispatch** — block uses, sneak-clicks, item-on-block and air uses in one
  place, so mods register hooks instead of their own loader events.
- **Permissions** — ownership, scoreboard-team sharing and per-owner invitations, with hosts
  **open by default** (an owner can privatise one).
- **Presence & capabilities** — ask which suite mods are installed, and publish/consume data between
  them without a compile dependency.
- **Break protection** — `NetworkBlock` blocks are owner-only to break, with an operator override.

The mod id is `mc_storage_area_network`. Package: `com.retiredroca.mcstorageareanetwork.api`.

---

## Installation (players)

Download the **universal** jar:

```
universal_mc_san_api.1.0.<patch>.<yymmddhh>.jar
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
// Fabric — use a floor range: newest 1.0.x, but never older than the API you built against.
modImplementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-fabric-1.21.1:[1.0.2,1.1)'

// NeoForge
implementation 'com.retiredroca.mcstorageareanetwork:mc_storage_area_network-neoforge-1.21.1:[1.0.2,1.1)'
```

The artifact id ends with the **Minecraft version** (`-1.21.1`), so one Maven repository serves every
supported version. The API version is `1.0.<patch>.<yymmddhh>` — the trailing stamp is bumped on
every API release, the **patch** is the compatibility floor — and it only advances when the API source
changes (releasing the gameplay mods does not bump it). Depend on `[<patch>,1.1)` so an older API
cannot silently satisfy your mod. Sources jars are published alongside (`-sources.jar`). The universal
install jar is published as:

```
com.retiredroca.mcstorageareanetwork:mc-storage-area-network-1.21.1:[1.0.2,1.1)
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

### Routing priority (companions)

A companion mod can bias which container receives an inserted stack without the host mods depending
on it. Register a `StoragePriority`; hosts consult the registry via `StorageRouter` on every insert:

```java
StorageRouter.register((level, refPos, pos, stack) ->
        matches(level, pos, stack) ? 1_000_000 - (int) Math.sqrt(refPos.distSqr(pos)) : 0);

List<ScannedStorage> ordered = StorageRouter.order(level, refPos, storages, stack); // highest first
int score = StorageRouter.priority(level, refPos, pos, stack);                     // 0 = no opinion
```

`NetworkHost` is implemented by host block entities (the Storage Terminal), so companion hardware
can bind to an existing host and reuse its `pos()`, `chunkRadius()` and `tier()`.

### Interaction hooks (companions and hosts)

The API owns the world event registrations; mods register hooks and never wire loader events
themselves:

```java
InteractionHooks.register(InteractionType.BLOCK_USE, new InteractionHook() {
    @Override public InteractionOutcome onUse(InteractionContext context) {
        if (!context.emptyHand() || !context.sneaking()) {
            return InteractionOutcome.PASS;          // let other hooks / vanilla handle it
        }
        // ... do the thing ...
        return InteractionOutcome.HANDLED;
    }

    @Override public boolean runsOnClient() { return true; }  // also consumed on the client
    @Override public int priority() { return 0; }             // lower runs first
});
```

- Slots: `BLOCK_USE` (a block is targeted), `ITEM_ON_BLOCK` (targeted with an item in the main
  hand, dispatched before `BLOCK_USE`) and `AIR_USE` (a genuine ray miss — aiming at air).
- Hooks run in ascending `priority()`; the first `HANDLED` wins. `runsOnClient()` lets a hook run on
  the client too, so the interaction is consumed there as well.
- The API's own behaviours (the Crafter link toggle and the container-exclusion toggle) are
  registered as the lowest-priority hooks.

### Permissions

```java
NetworkPermissions.canUse(level, pos, player);   // open || owner || invited || team
NetworkPermissions.canEdit(level, pos, player);  // owner || invited || team || op — not "open"
NetworkPermissions.canBreak(level, pos, player); // as canEdit
NetworkPermissions.isOpen(level, pos);
NetworkPermissions.setOpen(level, pos, false);   // privatise a host

NetworkPermissions.invite(level, owner, target);
NetworkPermissions.uninvite(level, owner, target);
NetworkPermissions.invitesOf(level, owner);
NetworkPermissions.setInvites(level, owner, targets);   // one-shot "amend" for a GUI or command
```

Invites are **per owner** and apply to every host that player placed. Public (open) hosts stay
usable by anyone; editing follows `canEdit`, so a public host is only reconfigured by its owner,
their team, invitees or an operator. `ContainerOwnership` remains the per-position owner store, and
`NetworkSettings` carries the server's `ownershipEnabled` / `teamSharing` switches.

### Presence and capabilities

```java
if (NetworkAwareness.isPresent(SisterMods.CRAFTING)) { ... }      // is a suite mod installed?
NetworkCapabilities.register(MyApiInterface.class, implementation);  // publish
Optional<MyApiInterface> mine = NetworkCapabilities.get(MyApiInterface.class);  // consume
```

Capabilities are plain interfaces in the API: the mod that owns some data publishes an
implementation, and any other mod reads it — no compile dependency between them. The suite's
`TerminalRegistry` (placed terminal positions, published by Remote Access Terminal) and
`RouteProvider` (routing-linker destinations, published by Network Routing) are examples. A missing
capability simply means the feature is unavailable.

### Break protection and exclusions

Mark a block with `com.retiredroca.mcstorageareanetwork.api.NetworkBlock` and the API handles
owner-only breaking (with an operator confirmation path) and skips it in the general
container-exclusion toggle:

```java
BreakProtection.canBreak(level, pos, player);
NetworkExclusions.toggle(player, containerPos);
CrafterAutomation.toggle(player, crafterPos);    // crafter ↔ network link
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
| `StoragePriority` | A routing rule: `priority(level, refPos, pos, stack)`, higher wins. |
| `StorageRouter` | Register rules; `order(...)`/`priority(...)` for insertion priority. |
| `NetworkHost` | A host block entity a companion can bind to (`pos()`, `chunkRadius()`, `tier()`). |
| `ShulkerBoxHelper` | Read/write shulker-box contents. |
| `InteractionHooks` / `InteractionType` / `InteractionHook` / `InteractionContext` | Shared world-interaction dispatch (block use, item-on-block, air use). |
| `NetworkPermissions` | Owner/team/invite permissions: `canUse`, `canEdit`, `canBreak`, open/private hosts. |
| `ContainerOwnership` | Per-position owner store (`ownerOf`, `canSee`, `isOwner`). |
| `NetworkSettings` | Server policy: `ownershipEnabled`, `teamSharing`, excluded containers. |
| `NetworkAwareness` / `SisterMods` | Which suite mods are installed. |
| `NetworkCapabilities` | Publish/consume interfaces between sister mods without a compile dependency. |
| `TerminalRegistry` / `RouteProvider` | The suite's first capabilities: placed terminals, and routing-linker destinations. |
| `BreakProtection` / `NetworkExclusions` / `CrafterAutomation` | Owner-only breaking, container-exclusion list, crafter ↔ network links. |
| `NetworkBlock` | Marker: gives a block break protection and skips it in the exclusion toggle. |
| `NetworkHostLocator` | Find a `NetworkHost` (e.g. a Storage Terminal) near a position. |

### Semantics

- **`hostPos`** — every source call receives the host block position, so one registered source can
  serve many hosts at once by keying its state by `hostPos`.
- **`refresh`** — called whenever a host rescans; discard stale state for that `hostPos` and re-read.
- **`shulkerFirst`** — when `true`, prefer shulker-box contents over plain container slots.
- **Hidden items** — filtered from plain listings but still reachable through sources.

---

## Building / publishing

```bash
# The build runs in four ordered groups; each can be run on its own.
./gradlew -Pmc=1.21.1 releaseLoaderJars        # per-loader API + mod jars
./gradlew -Pmc=1.21.1 releaseLoaderBundles     # per-loader bundles
./gradlew -Pmc=1.21.1 releaseUniversal         # universal API + mod jars
./gradlew -Pmc=1.21.1 releaseUniversalBundles  # universal bundles
./gradlew -Pmc=1.21.1 releaseJars              # everything -> build/release/ (final file names)

./gradlew publishApi      # per-loader jars -> mavenLocal (for local host builds)
./gradlew publishRepo     # per-loader jars + universal API -> ./repo (committed maven for distribution)
```

`releaseLoaderJars` / `releaseLoaderBundles` take `-Ploader=fabric|neoforge` for the fast inner loop.
When the API sources change, `ensureApi` republishes the API into `repo/` and drops dependents'
cached API jars, so hosts always compile against the current API.

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
| API version | `1.0.<patch>.<yymmddhh>` (patch = compatibility floor) |
