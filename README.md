# Konfig

Konfig is a multiloader configuration library for Minecraft mods.

It is built for shared common code, synchronized config values, and generated config screens that stay aligned across Fabric, Forge, and NeoForge. In this repo, the supported matrix is driven by `versions/*/gradle.properties` and exposed by `just list-nodes`.

## What Konfig provides

- Typed config values from common code
- Side-aware scopes: `CLIENT`, `COMMON`, and `SERVER`
- Commented TOML files under `config/<modid>/<name>.toml`
- Built-in sync modes: `NONE`, `LOGIN`, and `LOGIN_AND_RELOAD`
- Schema versioning and step-by-step migrations
- Generated config screens for registered handles
- Inline screen decorations:
  - banners
  - inline strings
  - clickable URLs
- Editor support for:
  - booleans
  - numbers and ranged numbers
  - enums
  - string lists
  - RGB and ARGB colors
  - registry-backed string values with autocomplete
- Fabric Mod Menu integration
- Forge and NeoForge mod-list config button helpers

## Support Matrix

The source of truth is the build graph generated from `versions/*/gradle.properties`. At the time of this README, `just list-nodes` reports:

- Fabric: every line from `1.14.4` through `26.1.2`
- Forge:
  - `1.16.5`
  - `1.17.1`
  - `1.18`, `1.18.1`, `1.18.2`
  - `1.19`, `1.19.1`, `1.19.2`, `1.19.3`, `1.19.4`
  - `1.20`, `1.20.1`, `1.20.2`, `1.20.3`, `1.20.4`, `1.20.6`
  - `1.21`, `1.21.1`
  - `1.21.3` through `26.1.2`
- NeoForge: `1.21.1` through `26.1.2`

Notable floors:

- Fabric support starts at `1.14.4`
- Forge support starts at `1.16.5`
- NeoForge support starts at `1.21.1`

If you need the exact current matrix, run:

```bash
just list-nodes
```

## Coordinates

Add the Kaf Maven repository:

```groovy
repositories {
    maven { url = "https://maven.kaf.sh" }
}
```

Published coordinates are loader-specific:

- Fabric: `com.iamkaf.konfig:konfig-fabric:<version>`
- Forge: `com.iamkaf.konfig:konfig-forge:<version>`
- NeoForge: `com.iamkaf.konfig:konfig-neoforge:<version>`

Versioning is parity-based across supported Minecraft lines. The semantic release is shared, and the `+<mc>` suffix identifies the target line. Example:

- `0.3.0+1.21.11`
- `0.3.0+26.1.2`

Do not depend on Konfig `common` directly.

## Basic Usage

```java
import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigHandle;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.SyncMode;

public final class ExampleConfig {
    public static final ConfigHandle HANDLE;
    public static final ConfigValue<Boolean> ENABLED;
    public static final ConfigValue<Integer> RANGE;

    static {
        ConfigBuilder builder = Konfig.builder("examplemod", "common")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.LOGIN)
                .comment("Example mod config");

        builder.push("general");

        ENABLED = builder.bool("enabled", true)
                .comment("Master toggle")
                .sync(true)
                .build();

        RANGE = builder.intRange("range", 8, 1, 64)
                .comment("Effect radius")
                .sync(true)
                .restart(RestartRequirement.WORLD)
                .build();

        builder.pop();
        HANDLE = builder.build();
    }
}
```

## Inline Decorations

Konfig can add non-persistent visual entries to generated config screens:

```java
ConfigBuilder builder = Konfig.builder("examplemod", "client")
        .scope(ConfigScope.CLIENT);

builder.banner("Example Mod");
builder.inlineText("These entries are UI-only decorations.");
builder.url("Documentation", "https://example.invalid/docs");
```

These entries are meant for section headers, explanatory text, and links. They do not create stored config values.

## Migrations

Konfig supports explicit schema migrations:

```java
ConfigBuilder builder = Konfig.builder("examplemod", "common")
        .scope(ConfigScope.COMMON)
        .syncMode(SyncMode.LOGIN)
        .schemaVersion(2)
        .migrate(0, ctx -> ctx.rename("general.enabled", "general.master_toggle"))
        .migrate(1, ctx -> {
            if (!ctx.contains("general.range")) {
                ctx.set("general.range", 8);
            }
        });
```

Rules:

- Konfig stores schema metadata at `[__konfig] version = <n>`
- Missing schema metadata is treated as version `0`
- Migrations run one step at a time
- Missing required migration steps fail loudly

## Config Screen Integration

Fabric:

- Konfig exposes consumer config screens through Mod Menu automatically.

Forge:

- Consumers register a config button through the Forge helper.

```java
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

KonfigForgeClientScreens.register("examplemod");
```

NeoForge:

- Consumers register the extension point through the NeoForge helper.

```java
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import net.neoforged.fml.ModContainer;

KonfigNeoForgeClientScreens.register(container, "examplemod");
```

## Repository Layout

Konfig uses a branch-based Stonecutter layout:

```text
konfig/
├── common/                 shared code and resources
├── fabric/                 Fabric-specific code and metadata
├── forge/                  Forge-specific code and metadata
├── neoforge/               NeoForge-specific code and metadata
├── versions/<mc>/          per-version properties and overlays
├── settings.gradle.kts     Stonecutter project graph entrypoint
├── stonecutter.gradle.kts  root task wiring
└── justfile                developer workflows
```

The effective source for a node comes from the shared roots plus the matching `versions/<mc>/...` overlays. The exact enabled loaders for each line come from `versions/<mc>/gradle.properties`.

## Development Workflow

Common commands:

```bash
./gradlew build
just list-nodes
just run 1.21.11 forge runClient
just run 1.16.5 forge runClient
just run 26.1 publish
just run downloadTranslations
```

`just run` accepts three forms:

- `just run <version> <loader> <task...>`
- `just run <version> <aggregate-task...>`
- `just run <root-task...>`

Examples:

```bash
just run 1.21.11 fabric build
just run 1.21.11 publishMod
just run downloadTranslations
```

## Runtime Validation

Konfig has three useful runtime validation layers:

```bash
just boot-check 1.21.11-forge 60
just teakit-boot-check 1.21.11-forge 60
just scenario-check 1.21.11-forge 240
```

What they mean:

- `boot-check`
  - confirms Konfig initializes on that node from logs
- `teakit-boot-check`
  - enables TeaKit as an optional dev runtime dependency when that MC line has a TeaKit catalog entry
  - confirms both Konfig and TeaKit initialize
  - lets TeaKit close the client cleanly after the title screen
- `scenario-check`
  - runs the checked-in TeaKit UI scenario
  - opens the title-screen Mods menu
  - opens Konfig’s config screen
  - asserts that `Enable Debug Logging` is present

Matrix-wide helpers:

```bash
just boot-check-all 60
just teakit-boot-check-all 60
just scenario-check-all 240
```

## Notes

- Konfig keeps one semantic release across all supported Minecraft lines.
- Loader- and version-specific divergence is isolated in `versions/<mc>/` or loader roots rather than split into independent per-version repos.
- The checked-in debug config exists specifically to exercise Konfig’s own screen, sync, and editor paths during runtime validation.

## License

MIT
