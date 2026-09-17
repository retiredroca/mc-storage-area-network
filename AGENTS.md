# Agent notes

## Git

- **Never commit, amend, tag, push, or GPG-sign automatically.** Finish the work, then tell the
  user it is ready and wait for an explicit instruction before creating any commit or tag.
- When notifying, report the working-tree status and the commit message you would use.
- Commits and tags are GPG-signed with the maintainer's key; the gpg-agent passphrase cache can
  expire (a timed-out pinentry fails the commit — retry after the user unlocks it).

## Build & verify

- Gradle 8.14 must *run* on JDK 17–23; the machine default is Java 25 and fails with
  `Unsupported class file major version 69`. Export
  `JAVA_HOME=/c/Users/Ric/.gradle/jdks/eclipse_adoptium-21-amd64-windows.2`.
- Select the Minecraft version with `-Pmc=<version>` (default `1.21.1`).
- Full release build (24 jars into `build/release/`):
  `./gradlew -Pmc=1.21.1 clean releaseJars`
- When API symbols change, the API must be in `repo/` before the hosts compile (they resolve a
  version floor from it):
  `./gradlew -p versions/<mc>/api/fabric publishMavenJavaPublicationToRepoRepository`
  `./gradlew -p versions/<mc>/api/neoforge publishMavenJavaPublicationToRepoRepository`
  then `./gradlew -Pmc=<mc> --refresh-dependencies clean releaseJars publishRepo`.
- Report `pruneRepo` output; it refreshes `maven-metadata.xml` (stale entries break range resolution).
- Deploy to the local Prism test instances: `./deploy.sh --build --local --no-restart`.

## Build performance

Measured on the maintainer's machine (28 cores / 64 GB), full staged sequence cold from clean:

| | baseline | after |
|---|---|---|
| `clean releaseLoaderJars` | 105 s | 87 s |
| full staged sequence (30 jars) | 104 s | 70 s |
| release-style build warmed (`-p` API publish + 4 staged groups) | n/a | 33 s |

What changed, and the rules that keep it fast:

- **JVM/build settings live in the root `gradle.properties` only.** `org.gradle.jvmargs` is read
  from a build's *own root* `gradle.properties`: for the composite build that is the repo root, but
  for `-p versions/<mc>/api/neoforge` it is that directory. So the module/loader files repeat the
  same value — that is deliberate, not duplication to clean up. If they disagree, Gradle starts a
  second daemon with the smaller heap (the four neoforge files used to say `-Xmx1G`, and a 1 GiB
  daemon really was running NeoGradle).
- Current values: `-Xmx12G -XX:MaxMetaspaceSize=1G -XX:+UseParallelGC`, `org.gradle.parallel=true`,
  `org.gradle.workers.max=8`, `org.gradle.caching=true`. `workers.max` is below the core count
  because each worker is its own JVM; do not raise the daemon heap to fix a worker OOM.
- **Do not pass `--no-daemon` locally.** Six Gradle invocations per release each used to cold-start
  and reconfigure all 12 included builds. `tools/release.py` now reuses the warm daemon and exposes
  `--no-daemon` to reproduce a CI-like cold build; CI always passes it (fresh container).
- Configuration cache is deliberately **off**: Loom 1.9.2 / NeoGradle 7.1.38 on Gradle 8.14 are not
  reliable with it.

## Conventions

- **Anything common across the gameplay mods belongs in the API** (`versions/<mc>/api`), never
  duplicated per loader or per mod (e.g. break protection, container exclusions, scanning).
- Follow the existing per-loader layout: `common` (no Gradle project) + `fabric` + `neoforge`
  included builds, `relocateCommonSources`/`relocateLoaderSources`, and a loader-agnostic platform
  seam installed by each loader initializer.
- Blocks from this mod set implement `com.retiredroca.mcstorageareanetwork.api.NetworkBlock`.
- Keep versions in the `1.0.<patch>.<yymmddhh>` scheme (`tools/versioning.py`); release tags are
  `v1.0.<patch>.<stamp>`.
- Version bumps are **declarative**: only the component named by `--mod` / the dispatch `mod` is
  re-versioned; nothing infers it from the files touched. When a change spans the API and one or
  more hosts (e.g. moving shared code into the API), release with `all` / **Release All** so the
  bundles cannot mix new and old component versions.
