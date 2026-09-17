# Remote Access Terminal

Dye-coloured terminals that link together. Place one and name it, right-click it to pick a
destination, and crouch-click it (or right-click while aiming at air) to change its settings —
colour, name, sorting and who may use it.

Standalone: it needs only the **MC Storage Area Network** API, which supplies break protection and
scoreboard-team permissions. Other mods in the suite are optional.

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric (Loader + Fabric API) or NeoForge 21.1.235+ |
| Java | 21 |
| Side | Client & Server |
| Depends on | MC Storage Area Network (`mc_storage_area_network`) |

## Layout

- `common/` — loader-neutral sources (no Gradle project; compiled once per loader)
- `fabric/`, `neoforge/` — the two included loader builds and their metadata
- `tools/generate_textures.py` — regenerates the terminal texture

## Building

```bash
./gradlew -p versions/1.21.1/remote-access-terminal build
```

The mod root also assembles the universal jar (`build/libs/remote-access-terminal-<version>-universal.jar`)
containing both loader builds.

## Credits

Inspired by **Jake Teleports** by **Neurlimon**:

- CurseForge — <https://www.curseforge.com/minecraft/mc-mods/jake-teleports>
- MCreator — <https://mcreator.net/modification/99707/jake-teleports>

The original mod's source is not available; this is an independent implementation.

All other code in this module is part of MC Storage Area Network (Apache-2.0).
