# Konfig

Konfig is a multiloader config library for Minecraft mods.

It provides a typed config API, side-aware scopes (`CLIENT`, `COMMON`, `SERVER`), built-in sync for shared values, and generated config screens.

## Features

- Typed config values in common code
- Commented TOML files at `config/<modid>/<name>.toml`
- Built-in sync modes: `NONE`, `LOGIN`, `LOGIN_AND_RELOAD`
- Generated config screens for registered handles
- String list and RGB/ARGB editors
- Registry-backed autocomplete for string values
- Mod Menu integration on Fabric
- Mod list config-button helpers on Forge and NeoForge

## Supported Versions

Current support bands:

- Fabric: every line from `1.14.4` through `26.1.2`
- Forge: `1.16.5`, `1.17.1`, `1.18` through `1.20.4`, `1.20.6`, `1.21`, `1.21.1`, `1.21.3` through `26.1.2`
- NeoForge: `1.20.2` through `26.1.2`

Use `just list-nodes` for the exact current matrix.

## Adding Konfig

Add the Kaf Maven repository:

```groovy
repositories {
    maven { url = "https://maven.kaf.sh" }
}
```

Use the artifact that matches your loader and Minecraft line. Example versions are branch-aligned, such as `0.2.0+26.1.2`.

Fabric:

```groovy
dependencies {
    modImplementation "com.iamkaf.konfig:konfig-fabric:<version>"
}
```

Forge:

```groovy
dependencies {
    implementation "com.iamkaf.konfig:konfig-forge:<version>"
}
```

NeoForge:

```groovy
dependencies {
    implementation "com.iamkaf.konfig:konfig-neoforge:<version>"
}
```

- NeoForge is available on `1.21.1+`
- Do not depend on `common` directly

## Development

Repo layout:

- `common/`, `fabric/`, `forge/`, `neoforge/`: shared loader source sets
- `versions/<mc>/`: per-version properties and overrides
- `stonecutter.gradle.kts`: Stonecutter root entrypoint

Common commands:

```bash
./gradlew build
just list-nodes
just run 1.21.11 forge runClient
just run 1.16.5 forge runClient
just run 26.1 publish
just run downloadTranslations
```

`just run` accepts:

- `just run <version> <loader> <gradle task>`
- `just run <version> <gradle task>` for version-wide aggregates like `build`, `publish`, `publishMod`, `publishModrinth`, and `publishCurseforge`
- `just run <root task>` for global tasks like `downloadTranslations`

Runtime validation:

```bash
just boot-check 1.21.11-forge 60
just teakit-boot-check 1.21.11-forge 60
just teakit-boot-check-all 60
just scenario-check 1.21.11-forge 240
just scenario-check-all 240
```

`just teakit-boot-check` enables TeaKit as an optional dev runtime dependency when that Minecraft line has a shared-catalog TeaKit artifact. It verifies startup from logs, then lets TeaKit close the client cleanly after the title screen appears.
`just scenario-check` runs a checked-in TeaKit scenario that opens the title-screen Mods menu, opens Konfig’s config screen, and waits for the `Enable Debug Logging` entry.

## Config Screen Integration

Fabric:

- Konfig exposes consumer config screens automatically through Mod Menu.

Forge:

- Consumer mods register a config button through Konfig’s helper.

```java
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

KonfigForgeClientScreens.register("examplemod");
```

NeoForge:

- Consumer mods register the extension point on their mod container through Konfig’s helper.

```java
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import net.neoforged.fml.ModContainer;

KonfigNeoForgeClientScreens.register(container, "examplemod");
```

## Quick Example

```java
import com.iamkaf.konfig.api.v1.*;

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

## Migrations

Konfig supports step-by-step schema migrations:

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
- missing schema metadata is treated as version `0`
- migrations run one step at a time
- missing required migration steps fail loudly
