# Konfig

Konfig is a multiloader config library for Minecraft mods.

It gives you a typed API, side-aware config scopes (`CLIENT`, `COMMON`, `SERVER`), and built-in server to client sync for shared values.

## What You Get

- Typed config values in common code
- Category-based builder API (`push` / `pop`)
- Runtime validation and fallback to defaults
- Built-in sync modes (`NONE`, `LOGIN`, `LOGIN_AND_RELOAD`)
- Commented TOML config files at `config/<modid>/<name>.toml`
- Built-in config screen generation for registered handles with auto-save status
- Rich generated editors for string lists and RGB/ARGB color values
- Registry-backed autocomplete for string and string-list entries
- Fabric ModMenu integration (`modmenu` entrypoint)
- Forge and NeoForge config button integration in the mod list
- Fabric + Forge support across old and new versions
- NeoForge support on modern versions

## Supported Versions

| Minecraft | Fabric | Forge | NeoForge |
| --- | --- | --- | --- |
| 26.1 | yes | no | yes |
| 1.21.11 | yes | yes | yes |
| 1.21.1 | yes | yes | yes |
| 1.20.1 | yes | yes | no |
| 1.19.2 | yes | yes | no |
| 1.18.2 | yes | yes | no |
| 1.16.5 | yes | yes | no |

## Adding Konfig

Konfig is published as a loader-specific artifact. Add the Kaf Maven repository to your project:

```groovy
repositories {
    maven { url = "https://maven.kaf.sh" }
}
```

If you want to browse published versions in a UI, start here:

- https://z.kaf.sh/artifact/com.iamkaf.konfig/konfig-fabric

Use the artifact that matches your loader and Minecraft version. Versions are branch-aligned, for example `0.1.0+26.1`.

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

- NeoForge is available on `1.21.1+`. `1.20.1` and older publish Fabric and Forge only.
- Do not depend on `common` directly.

## Config Screen Integration

Konfig now exposes the same consumer-facing config screen model across every supported version line, but the loader contract is not identical.

Fabric:

- Konfig exposes consumer config screens automatically through Mod Menu.
- If your mod registers one or more Konfig handles, Mod Menu will show a config button without extra loader-specific code.

Forge:

- Your mod must register its own config screen button.
- Konfig provides the helper for that registration.

`1.18.2+`:

```java
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

KonfigForgeClientScreens.register("examplemod");
```

`1.16.5`:

```java
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

KonfigForgeClientScreens.register("examplemod");
```

NeoForge:

- Your mod must register its own config screen extension point on its mod container.
- Konfig provides the helper for that registration on `1.21.1+`.

```java
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import net.neoforged.fml.ModContainer;

KonfigNeoForgeClientScreens.register(container, "examplemod");
```

Why this differs:

- Fabric Mod Menu supports library-provided config screen factories for other mods.
- Forge and NeoForge resolve config buttons from the selected mod's own container, so the consumer mod has to opt in explicitly.

Konfig is consistent across supported versions in the API it offers:

- Fabric: automatic consumer buttons
- Forge: `KonfigForgeClientScreens.register(modId)`
- NeoForge: `KonfigNeoForgeClientScreens.register(container, modId)` on `1.21.1+`

On `1.16.5`, the underlying client `Screen` types still diverge between Fabric and Forge, so the public helper layout is loader-local there even though the behavior is the same.

## Toolchain Notes

- ForgeGradle 7 is used on `1.18.2+`.
- `1.16.5` stays on ForgeGradle 6 because the FG7 pre-1.17 MCP pipeline is not stable.
- `1.18.2` Forge dev runs use a Java 17 launcher, patch the cached `bootstraplauncher-1.0.0.jar` with `1.1.2` contents, and extract LWJGL natives explicitly. FG7's Slime Launcher path for Forge `40.3.12` does not boot cleanly without those runtime fixes.
- `1.16.5` Forge dev runs intentionally keep `build/resources/main` and `build/classes/java/main` separate. FG6's exploded `MOD_CLASSES` scanner needs a resource root plus at least one class root; collapsing them into one directory prevents `@Mod` discovery.
- `1.16.5` Fabric still targets Java 8 bytecode, but the dev run tasks use a Java 17 launcher. Old Fabric Loader/Mixin builds do not run cleanly on Java 21.
- Gradle wrapper versions follow that split:
  - `1.18.2+`: Gradle `9.3.1`
  - `1.16.5`: Gradle `8.14`

## TODO

- Preserve stable declaration order in generated config screens instead of path-based alphabetical sorting.
- Define and implement live reload behavior for in-game config edits, especially for synced common configs.
- Define and implement RestartRequirement behavior in the screen and config lifecycle.

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
        builder.categoryComment("General gameplay tuning");
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

Konfig can migrate persisted configs forward as your schema changes.

```java
import com.iamkaf.konfig.api.v1.*;

public final class ExampleConfig {
    public static final ConfigHandle HANDLE;

    static {
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

        builder.push("general");
        builder.bool("master_toggle", true).build();
        builder.intRange("range", 8, 1, 64).build();
        builder.pop();

        HANDLE = builder.build();
    }
}
```

Migration rules:

- Konfig stores schema metadata in the TOML file at `[__konfig] version = <n>`.
- `__konfig` is a reserved top-level namespace and cannot be used by consumer config keys.
- Missing schema metadata is treated as version `0`.
- Konfig runs migrations step-by-step from `fromVersion` to `fromVersion + 1`.
- If a file is newer than the running schema version, Konfig warns and skips the automatic rewrite during load.
- If a required migration step is missing, Konfig fails loudly instead of silently dropping data.

## Screen Labels

Konfig localizes its own screen chrome out of the box. Consumer mods can also localize generated config entry labels.

Entry label key format:

```text
konfig.config.<modid>.<handle>.<path>
```

Example:

```json
{
  "konfig.config.examplemod.common.general.enabled": "Master Toggle",
  "konfig.config.examplemod.common.general.range": "Effect Radius"
}
```

If a key is missing, Konfig falls back to a humanized breadcrumb label such as `Common > General > Enabled`.

Hover tooltips come from the config definition itself:

- `categoryComment(...)` contributes category-level tooltip lines
- `ValueBuilder.comment(...)` contributes the entry tooltip line
- Konfig combines them in path order, so nested categories read naturally

## Rich Editors

Generated screens now cover more than plain scalar values:

```java
builder.stringList("quickbar_items", List.of("minecraft:torch", "minecraft:bread"))
        .comment("Suggested quickbar items")
        .build();

builder.colorRgb("beam_color", 0x3FA7FF)
        .comment("Primary beam color")
        .build();

builder.colorArgb("overlay_tint", 0xCC3366FF)
        .comment("Overlay tint with alpha")
        .build();
```

Konfig renders string lists in a dedicated list editor and color values in a dedicated color editor with a swatch, hex input, and channel sliders. Screen edits auto-save as you work, and legacy branches use scrollable layouts to keep the same workflow usable on older UI APIs.

## Registry Suggestions

String values can opt into registry-backed suggestions in the generated screen:

```java
builder.string("target_item", "minecraft:iron_ingot", 3, 64)
        .registry(Registries.ITEM)
        .comment("Suggested item id")
        .build();

builder.stringList("quickbar_items", List.of("minecraft:torch", "minecraft:bread"))
        .registry(Registries.ITEM)
        .comment("Suggested item ids")
        .build();
```

On `1.18.2+`, pass a registry key such as `Registries.ITEM`. On `1.16.5`, the legacy API takes a registry id string such as `"minecraft:item"` instead.

This is suggestion-only UX. Konfig does not hard-reject unknown ids here; it simply offers autocompletion from built-in registry contents in the screen.

## Build

From the repo root:

```bash
just versions
just build version=1.21.11
```

Or directly:

```bash
cd 1.21.11
./gradlew :common:build :fabric:build :neoforge:build :forge:build
```

For versions without NeoForge (`1.20.1` and older):

```bash
./gradlew :common:build :fabric:build :forge:build
```

## Debug Logging

Konfig can dogfood itself through `config/konfig/konfig.toml`:

```toml
# Konfig internal debug settings.

[debug]
# Verbose diagnostics for config lifecycle and screen creation.

# Enable verbose Konfig internal logs
enabled = true
```

When enabled, Konfig logs runtime internals and config lifecycle events, including:

- `config found ...`
- `config not found, created defaults ...`
- `creating screen ...`

## Project Layout

- `1.21.11`, `1.21.10`, `1.21.1`, `1.20.1`, `1.19.2`, `1.18.2`, `1.16.5`: version-specific multiloader projects

## Status

Konfig provides first-class config definitions, sync, and built-in loader-integrated screens across the supported version matrix.
