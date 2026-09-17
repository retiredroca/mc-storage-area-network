# Plan: new mod `Segmentation`

Status: **design in progress** — name, interaction model and several decisions locked; open questions
1–11 in §7. Component key: `segmentation`. Dependency: **API only** (works without the other
gameplay mods).

---

## 1. Objective

A gameplay mod that partitions the world and the network into **segments**: bounded areas a player
owns, which act as

1. **land** — an area you can build in and others cannot;
2. **network scope** — a boundary that storage scanning (and, when present, routing/terminals) respect;
3. **permission scope** — the unit that the API's existing trust rules (owner / team / invited) apply to.

Segments are created and edited with a **Segmentation Stick** (§4). It needs only the API; the other
gameplay mods are optional companions that make segments do more.

## 2. Locked decisions

| Item | Value |
|---|---|
| Mod name | Segmentation |
| Mod id | `segmentation` |
| Java package | `com.retiredroca.segmentation` (planned) |
| Component key | `segmentation` |
| Command root | `/segmentation` (planned; §7 Q11) |
| Dependency | **standalone** — API only |
| Creation tool | **Segmentation Stick** (§4); the Routing Linker gains the same behaviour when Network Routing is installed (§7 Q5) |
| Trust model | **reuses the API's** (`NetworkPermissions` + `ContainerOwnership`); no new trust implementation |
| Segment size | the anchor host's **tier**-derived scan radius when inside a range, otherwise the marked block extended corner-to-corner; config override |
| Overlap | allowed **with mutual consent**; both owners keep permissions in the shared space |
| Privacy/lock flag | **owner-only to set**; owner always retains access |
| Lock scope | blocks **interaction** (doors, levers, chests — anything interactable), not just storage |
| Lock state on transfer | **reset** when ownership transfers |
| Crouch-click with the stick | **unset**; performs no other function |

## 3. Why the API is not extended for trust

The API already provides everything needed for permission evaluation:

| Existing API | What Segmentation uses it for |
|---|---|
| `NetworkPermissions` | open/private, owner invites, `canUse` / `canEdit` / `canBreak` |
| `ContainerOwnership` | per-position owner + name, `canSee` (owner or scoreboard team) |
| `NetworkSettings` | `ownershipEnabled()`, `teamSharing()` policy flags |
| `BreakProtection` | `isProtected` / `canBreak` / `needsOpConfirmation` / `forceBreak` |
| `NetworkBlock` | opt-in marker; Segmentation's own blocks become protected automatically |
| `InteractionHooks` | crouch/use dispatch for setup gestures |

Genuine gaps the API has: **areas** (everything is keyed by a single `BlockPos`) and, per the
locked decision, **interaction locking of arbitrary blocks** (the API only covers breaking
`NetworkBlock`s and using a mod's own blocks). Both are Segmentation features, built on API trust.

## 4. Interaction model — the Segmentation Stick (locked)

One tool, **Segmentation Stick**, whose behaviour depends on *whether the targeted block is inside a
network scan range*. Crouch-clicking does **nothing** on its own (it is reserved for unset, below).

| Target | Behaviour |
|---|---|
| **Outside any network scan range** | Offer to turn that block into a **claim block**, with a particle effect |
| **Inside a network scan range** | Set the **privacy range** corner-to-corner (two uses = the two corners) |
| **On a Storage Terminal** (inside range) | Ask whether the terminal becomes the **claim block / centre of the claim**. No texture change; play the vanilla "enter chest" particle effect |
| **Any of the above + crouch-click** | **Unset** |
| **Stick held** | Client overlay / particles showing the **privacy range** bounds **and** the network scan range |

Notes:

- Marking a claim block is a **marker, not a new block type** — so there is no parallel tier ladder.
  Sizing comes from the network (the anchor host's scan radius) when inside a range; outside a range
  the marked block is the segment root, extended by the corner-to-corner selection.
- **If Network Routing is installed, the Routing Linker gains the same functionality** (planned; see
  open question 5 for the capability direction).
- The visualisation (privacy bounds + scan range) is the one genuinely new client feature, and the
  scan-range half needs a cheap data source — see "HostRegistry" under open question 3.

## 5. Tier-based sizing (locked)

- A Storage Terminal's **tier** already determines its `NetworkHost.chunkRadius()` (tier 0–5 → 1×1 …
  11×11 chunks). When a segment is anchored to a host, its extent is that radius, so **no new sizing
  concept**.
- Servers can **override the size in config** (fixed size, or a different mapping).
- Outside any scan range, a segment starts as the marked block and is extended by the
  corner-to-corner privacy selection, so Segmentation works with **no Storage Network installed** and
  needs no tiered block of its own.

## 6. Privacy / interaction lock (locked, pending mechanism detail)

- An **owner-only** flag on a block that makes it **unusable** by anyone else, *including teammates*
  — i.e. it overrides the API's `teamSharing` trust for that block.
- Applies to **any interactable block**: doors, levers, chests, buttons, trapdoors.
- The owner always retains access; explicit invites can still grant access (invites are per-owner in
  `NetworkPermissions`, so this composes).
- **Reset on ownership transfer.**
- Because it covers non-storage blocks, this is a **general "this block is locked" flag**, not a
  storage-container flag — so it likely lives in a Segmentation-owned store keyed by position rather
  than in `ContainerOwnership`.
- Implementing it requires hooking the **loader interaction events** (Fabric/NeoForge) and cancelling
  them. This is the main piece of machinery Segmentation adds that the API does not already have.

## 7. Open questions

The stick model answered **A** (sizing: the anchor host's scan radius, or the marked block) and **B**
(the anchor: the marked block, optionally a Storage Terminal), and replaced **G** (the lock gesture
is now the stick). The remaining questions are:

### 1. Claim block vs privacy range — one concept or two?
The model implies **two**: a *claim block* (the segment's anchor/centre, possibly a Storage Terminal)
and a *privacy range* (a cuboid set corner-to-corner). Does a privacy range require a claim block, or
can it exist alone? If two, do they have separate permission stores?

### 2. What does "outside scan range → claim block" create, exactly?
Just the single marked block as the segment root (extended later by corner-to-corner), or an
immediate size prompt? And is marking owner-only (the block must be yours per
`ContainerOwnership.isOwner`)? Can you claim inside someone else's segment?

### 3. The client overlay's scan-range data source
`NetworkHostLocator.findNearest(level, pos, radius)` iterates `chunkRadius` chunks of block entities,
and nothing currently publishes host positions/radii. So the overlay needs either:
- **(a)** a new API capability, e.g. **`HostRegistry`** publishing `NetworkHost` pos + radius,
  mirroring `TerminalRegistry` / `RouteProvider`; or
- **(b)** only showing ranges for hosts Segmentation can find server-side and sync to the client.

**(a)** is the clean answer and is small; it also benefits the API generally. Needs confirmation,
since it means an API addition.

### 4. "Storage Terminal becomes the centre of the claim"
Does the claim's extent equal that terminal's scan radius (tier-derived sizing), or is the terminal
merely a labelled anchor with the extent still set corner-to-corner?

### 5. Routing Linker sharing the functionality
If Network Routing is installed, the Linker gains the same behaviour. The Linker lives in
`remote-access-terminal` (`RoutingLinker`), and the suite's pattern is that a mod reads a
**published capability** rather than linking against another mod:
- **(a)** Segmentation publishes a capability the Linker consults (keeps Segmentation the owner;
  requires Segmentation to be loaded); or
- **(b)** the behaviour moves into the API / `InteractionHooks` (shared, but the API then knows about
  segmentation).

**(a)** matches the existing `RouteProvider` / `TerminalRegistry` pattern. Confirm.

### 6. Crouch-click interaction with the API builtins
Crouch-click is currently the API's container-exclusion toggle (`InteractionHooks` builtin). Does the
stick **suppress** that while held (crouching with the stick neither locks nor excludes), or does
"does nothing" only mean the stick has no crouch function of its own?

### 7. Overlap consent model
Explicit request/accept, a config flag allowing open overlap, or refuse-and-rely-on-invites.
Previously C.

### 8. What a segment enforces with only the API present
Protection only (build/break/interact), or protection **plus** storage scoping when Storage Network
is installed. **Proposed: protection always, storage scoping activates automatically when the sister
mod exists.** Previously D.

### 9. Ownership of blocks placed inside someone else's segment
If Bob places a chest inside Alice's segment: Bob's, Alice's, or shared? More important now that a
privacy range can be laid over shared ground. Previously E.

### 10. Placement blocking
Should **placing** blocks inside someone else's segment be blocked by default? Previously F.

### 11. Command root
`/segmentation` (explicit) vs a short alias vs a shared `/network` root. Previously H.

## 8. Naming rationale (why "Segmentation")

- **Network segmentation** is real infrastructure vocabulary and already implies isolation/boundaries.
- It avoids collisions: `remote_access_terminal` owns "access", so `access_zone` / `access_control`
  are out.
- A "segment" is the one noun that covers all three layers (land / network scope / permission scope).
  Alternatives were narrower: `storage_pool` (ownership only), `network_isolation` (boundary only),
  `zero_trust_network` (policy only).
- It reads as a capability (like `network_routing`) rather than an object.

## 9. Reusable API facts for implementation

- **SavedData stores**: `NetworkPermissions` (`mc_storage_area_network_permissions`, **overworld-anchored**),
  `ContainerOwnership` (`mc_storage_area_network_owners`, **per-dimension**),
  `NetworkExclusions` (per-dimension), `CrafterLinks` (per-dimension).
- **`NetworkPermissions.isTrusted` is package-private** — a mod cannot call it; use `canUse` /
  `canEdit` / `canBreak` instead, or extend the API if a public trust query is genuinely needed.
- **`ContainerOwnership` is per-position** — there is no area concept anywhere in the API.
- **Scanning** is positional: `NetworkHostLocator.findNearest(level, origin, chunkRadius)` and
  `ItemScanner.scan(level, center, chunkRadius)`. Making scanning segment-aware means resolving
  "which segment owns this chunk" **per chunk on a hot path** — measure before committing (§9).
- **Routing already has an area concept** (`NetworkRoutingAreas`, `RoutingBindings`): a segment/route
  relationship must be defined (does a routing range clamp to its segment?).
- **`NetworkAwareness` / `SisterMods`** is how a mod checks for a companion (`SisterMods.STORAGE`,
  `ROUTING`, `CRAFTING`, `ACCESS`) — the mechanism for optional integration.
- Modules are wired in **6 places**: `versions.properties`, `gradle/versions.gradle`, root
  `settings.gradle` (`includeBuild`), root `build.gradle` (`componentVersion`, `modBuild`, jar maps,
  `release*` tasks, `clean`), bundle dirs, plus the module's own `build.gradle` / `settings.gradle` /
  `fabric` + `neoforge` included builds. There is **no `module.properties`** in this repo.

## 10. Risks

| Risk | Note |
|---|---|
| Scanning hot path | Per-chunk segment resolution could be expensive; must be measured, and must not slow `ItemScanner` for players not using Segmentation. |
| Client overlay cost | Showing the network scan range needs the `HostRegistry` capability (§7 Q3); without it the client would have to walk chunk block entities every frame. |
| Interaction hooks | Cancelling vanilla interaction for arbitrary blocks is new machinery; needs care not to break unrelated mods' blocks. |
| Overlap semantics | Shared-permission overlap is the most likely source of bugs; the consent model (Q7) and inside-block ownership (Q9) must be settled together. |
| Standalone constraint | The mod must not require Storage Network, so outside a scan range a segment is the marked block plus a corner-to-corner selection (no tiered block of our own). |
| Two-concept risk | If "claim block" and "privacy range" are separate stores (Q1), the permission model has to say which one grants access where. |
| Scope | All three layers in one mod is the largest in the suite; phasing is advisable (land + permission first, network scoping second). |

## 11. Next step

Answer the open questions in §7 (Q3 `HostRegistry` and Q1 claim-block-vs-privacy-range are the two
that block a design). Then this document becomes the design of record and an implementation plan
(phases, stores, blocks/items, commands, loader hooks, API-vs-mod split) can be written before any
code.

---

### Related documents

- `AGENTS.md` — repo conventions, build commands, build-performance notes.
- `API-README.md` — the API surface used above.
- `docs/archive/remote-access-terminal-plan.md` — the previous mod's plan (shipped), kept for
  reference on the per-module layout and release wiring.
