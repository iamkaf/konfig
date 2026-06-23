<p align="center">
  <img src="assets/banner.png" alt="Konfig banner" width="600" />
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-a78bfa?style=for-the-badge&labelColor=0d1117" alt="MIT License" /></a>
  <img src="https://img.shields.io/badge/minecraft-1.14.4%E2%86%9226.2-5eead4?style=for-the-badge&labelColor=0d1117" alt="Minecraft 1.14.4 to 26.2" />
  <img src="https://img.shields.io/badge/loaders-Fabric%20%7C%20Forge%20%7C%20NeoForge-fbbf24?style=for-the-badge&labelColor=0d1117" alt="Fabric, Forge, and NeoForge" />
</p>

<h1 align="center">Konfig</h1>

<p align="center">
  <strong>A multiloader configuration library for Minecraft mods.</strong>
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> &middot;
  <a href="#basic-usage">Basic Usage</a> &middot;
  <a href="#generated-screens">Generated Screens</a> &middot;
  <a href="#support-matrix">Support Matrix</a> &middot;
  <a href="#development">Development</a>
</p>

---

Konfig lets Minecraft mods define typed config values in common code, persist them as commented TOML, sync selected values to clients, and generate config screens for Fabric, Forge, and NeoForge.

It is built for shared common code. Loader-specific integration stays in the loader roots, while config declaration, validation, migration, and screen metadata can live beside the rest of your common mod logic.

## What Konfig Provides

| Area | Details |
|------|---------|
| Typed values | Booleans, ranged integers, ranged longs, ranged doubles, dropdowns, enums, strings, string lists, RGB colors, ARGB colors, and custom codecs |
| Side-aware scopes | `CLIENT`, `COMMON`, and `SERVER` configs |
| Files | Commented TOML under `config/<modid>/<name>.toml` |
| Sync | `NONE`, `LOGIN`, and `LOGIN_AND_RELOAD` sync modes |
| Migrations | Explicit schema versions and step-by-step migration functions |
| Screens | Generated config screens for registered handles |
| Screen content | Value editors, category headers, images, inline text, links, and info panels |
| Loader hooks | Fabric Mod Menu integration plus Forge and NeoForge mod-list config button helpers |
| Version graph | Stonecutter-backed Fabric, Forge, and NeoForge nodes across many Minecraft lines |

## How It Works

```text
common mod code
    |
    v
Konfig.builder(modid, name)
    |
    +-- typed values, comments, validators, migration steps
    |
    v
ConfigHandle
    |
    +-- config/<modid>/<name>.toml
    +-- optional network sync
    +-- generated client config screen
```

Each config value is declared once. Konfig uses that declaration for disk persistence, validation, synchronization metadata, and the generated screen editor.

## Quick Start

Add the Kaf Maven repository:

```groovy
repositories {
    maven { url = "https://maven.kaf.sh" }
}
```

Use the loader artifact for the Minecraft line you target:

| Loader | Dependency |
|--------|------------|
| Fabric | `com.iamkaf.konfig:konfig-fabric:<version>` |
| Forge | `com.iamkaf.konfig:konfig-forge:<version>` |
| NeoForge | `com.iamkaf.konfig:konfig-neoforge:<version>` |

Versioning is parity-based across supported Minecraft lines. The semantic release is shared, and the `+<mc>` suffix identifies the target line.

| Example version | Meaning |
|-----------------|---------|
| `0.4.0+1.21.11` | Konfig `0.4.0` for Minecraft `1.21.11` |
| `0.4.0+26.2` | Konfig `0.4.0` for Minecraft `26.2` |

Do not depend on Konfig `common` directly. Use the loader-specific artifact.

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
    public static final ConfigValue<String> MODE;

    static {
        ConfigBuilder builder = Konfig.builder("examplemod", "common")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.LOGIN)
                .comment("Example mod config");

        builder.push("general");

        ENABLED = builder.bool("enabled", true)
                .comment("Master toggle")
                .tooltip("Enable example mod features")
                .sync(true)
                .build();

        RANGE = builder.intRange("range", 8, 1, 64)
                .comment("Effect radius")
                .tooltip("Controls the effect radius in blocks")
                .sync(true)
                .restart(RestartRequirement.WORLD)
                .build();

        MODE = builder.dropdown("mode", "balanced", options -> options
                        .option("quiet", option -> option.tooltip("Prefer fewer effects"))
                        .option("balanced", "Balanced")
                        .option("loud", option -> option
                                .labelKey("examplemod.config.mode.loud")
                                .tooltipKey("examplemod.config.mode.loud.tooltip")))
                .comment("Effect intensity preset")
                .tooltip("Select the default intensity preset")
                .build();

        builder.pop();
        HANDLE = builder.build();
    }
}
```

Use `ConfigValue#get()` when reading a value and `ConfigValue#set(value)` when changing it programmatically.

```java
if (ExampleConfig.ENABLED.get()) {
    int radius = ExampleConfig.RANGE.get();
}
```

## Builder API

`Konfig.builder(modId, name)` creates one config file and one handle.

| Builder method | Purpose |
|----------------|---------|
| `scope(ConfigScope.CLIENT)` | Client-only config |
| `scope(ConfigScope.COMMON)` | Config that may matter on both sides |
| `scope(ConfigScope.SERVER)` | Server-side config |
| `syncMode(SyncMode.NONE)` | Never sync values |
| `syncMode(SyncMode.LOGIN)` | Sync selected values when a client logs in |
| `syncMode(SyncMode.LOGIN_AND_RELOAD)` | Sync selected values on login and reload |
| `fileName(String)` | Override the generated file name |
| `schemaVersion(int)` | Set the expected schema version |
| `migrate(int, ConfigMigration)` | Register one migration step |
| `push(String)` / `pop()` | Enter and leave a category path |
| `comment(String)` | Add file-level or category-level comments |
| `categoryComment(String)` | Add a comment to the current category |
| `categoryTooltip(String)` | Add a tooltip to the current category in generated screens |
| `build()` | Finalize the handle |

Value builders share the same metadata methods:

| Value method | Purpose |
|--------------|---------|
| `comment(String)` | TOML comment for the value |
| `tooltip(String)` | Screen tooltip for the generated editor |
| `info(Consumer<InfoPanelBuilder>)` | Add richer generated-screen help |
| `restart(RestartRequirement)` | Mark values that need a restart or reload |
| `sync(boolean)` | Include or exclude a value from sync |
| `clientOnly()` | Restrict the value to client-side use |
| `serverOnly()` | Restrict the value to server-side use |
| `validate(Predicate<T>, String)` | Reject invalid values with an error message |
| `build()` | Register the value and return `ConfigValue<T>` |

## Value Types

| Method | Type | Notes |
|--------|------|-------|
| `bool(key, defaultValue)` | `Boolean` | Renders as a toggle |
| `intRange(key, defaultValue, min, max)` | `Integer` | Enforces and displays an integer range |
| `longRange(key, defaultValue, min, max)` | `Long` | Enforces and displays a long range |
| `doubleRange(key, defaultValue, min, max)` | `Double` | Enforces and displays a double range |
| `string(key, defaultValue, minLen, maxLen)` | `String` | Supports length bounds and registry autocomplete |
| `stringList(key, defaultValue)` | `List<String>` | Supports registry autocomplete per entry |
| `dropdown(key, defaultValue, options)` | `String` | Restricts values to a fixed option list and renders as a dropdown |
| `enumValue(key, defaultValue)` | enum | Uses the enum constants as choices |
| `colorRgb(key, defaultValue)` | `Integer` | RGB color value |
| `colorArgb(key, defaultValue)` | `Integer` | ARGB color value |
| `custom(key, defaultValue, codec)` | custom | Uses a `KonfigCodec<T>` |

String and string-list values can be connected to a registry for autocomplete. On newer lines, use a `ResourceKey<? extends Registry<?>>`. On older lines, use the registry id string overload.

Dropdown values are stored as strings and must always match one of the declared options. For simple menus, pass a list:

```java
ConfigValue<String> QUALITY = builder.dropdown(
        "quality",
        "balanced",
        java.util.Arrays.asList("fast", "balanced", "fancy")
).build();
```

Use the builder overload when options need display metadata:

```java
ConfigValue<String> QUALITY = builder.dropdown("quality", "balanced", options -> options
        .option("fast", "Fast")
        .option("balanced", option -> option
                .labelKey("examplemod.config.quality.balanced")
                .tooltipKey("examplemod.config.quality.balanced.tooltip"))
        .option("fancy", option -> option
                .label("Fancy")
                .tooltip("Prefer visuals over speed")
                .info(info -> info.inlineText("This may cost extra frames."))))
        .build();
```

When an option has no explicit label, generated screens try `konfig.value.<modid>.<config>.<path>.<option>` first, then the legacy `<modid>.config.<lastPathSegment>.<option>` key, and finally fall back to a prettified option value.

## Files And Sync

Konfig writes TOML files under `config/<modid>/<name>.toml`. Comments from the builder are written beside the values they describe.

| Sync mode | Behavior |
|-----------|----------|
| `NONE` | Values stay local to their side |
| `LOGIN` | Values marked with `.sync(true)` sync during login |
| `LOGIN_AND_RELOAD` | Values marked with `.sync(true)` sync during login and reload |

Sync is opt-in per value. Set the config-level sync mode first, then mark the individual values that should cross the wire.

## Generated Screens

Konfig generates screens from the registered config handles. The screen uses the value metadata from your config declarations, including comments, tooltips, restart requirements, validators, categories, and info panels.

| Editor support | Screen behavior |
|----------------|-----------------|
| Booleans | Toggle editor |
| Numbers | Numeric editor with range metadata |
| Dropdowns | Dropdown menu with fixed string options, option labels, option tooltips, scrolling, and keyboard navigation |
| Enums | Choice editor |
| String lists | List editor |
| RGB and ARGB colors | Color editor |
| Registry-backed strings | Autocomplete where supported |

Fabric exposes consumer config screens through Mod Menu automatically.

Forge consumers register a config button through the Forge helper:

```java
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

KonfigForgeClientScreens.register("examplemod");
```

NeoForge consumers register the extension point through the NeoForge helper:

```java
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import net.neoforged.fml.ModContainer;

KonfigNeoForgeClientScreens.register(container, "examplemod");
```

Consumers can pass a display title when creating a screen directly:

```java
KonfigClientScreens.create(modId, title, parent);
```

## Inline Decorations

Konfig can add non-persistent visual entries to generated config screens:

```java
ConfigBuilder builder = Konfig.builder("examplemod", "client")
        .scope(ConfigScope.CLIENT);

builder.header("Example Mod");
builder.image(Identifier.fromNamespaceAndPath("mymod", "gui/example"));
builder.image(Identifier.fromNamespaceAndPath("mymod", "gui/example_captioned"), "Example Image");
builder.image(Identifier.fromNamespaceAndPath("mymod", "gui/status"), ImageOptions.icon());
builder.image(
        Identifier.fromNamespaceAndPath("mymod", "gui/banner"),
        "Example Banner",
        ImageOptions.banner(180, 28)
);
builder.image(
        Identifier.fromNamespaceAndPath("mymod", "gui/action"),
        ImageOptions.builder()
                .size(20, 20)
                .align(ImageOptions.Align.RIGHT)
                .captionPosition(ImageOptions.CaptionPosition.NONE)
                .build()
);
builder.inlineText("These entries are UI-only decorations.");
builder.url("Documentation", "https://example.invalid/docs");
```

These entries are meant for section headers, images, explanatory text, and links. Images can be sized, padded, left/center/right aligned, and rendered with a right-side caption, below-image caption, or no caption. They do not create stored config values.

You can also attach richer help content to a category or individual value:

```java
builder.categoryInfo(info -> info
        .header("General")
        .inlineText("These values affect the whole mod.")
        .url("Documentation", "https://example.invalid/docs"));
```

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

| Rule | Behavior |
|------|----------|
| Schema metadata | Konfig stores `[__konfig] version = <n>` |
| Missing metadata | Treated as schema version `0` |
| Step order | Migrations run one version step at a time |
| Missing steps | Missing required migration steps fail loudly |

Migration functions operate on the stored config data before the handle is finalized. Use them for renames, default insertion, and shape changes between releases.

## Support Matrix

The source of truth is the build graph generated from `versions/*/gradle.properties`. At the time of this README, `just list-nodes` reports:

| Loader | Supported lines |
|--------|-----------------|
| Fabric | Every line from `1.14.4` through `26.2` |
| Forge | `1.16.5`; `1.17.1`; `1.18`, `1.18.1`, `1.18.2`; `1.19`, `1.19.1`, `1.19.2`, `1.19.3`, `1.19.4`; `1.20`, `1.20.1`, `1.20.2`, `1.20.3`, `1.20.4`, `1.20.6`; `1.21`, `1.21.1`; `1.21.3` through `26.2` |
| NeoForge | `1.21.1` through `26.2` |

Notable floors:

| Loader | First supported line |
|--------|----------------------|
| Fabric | `1.14.4` |
| Forge | `1.16.5` |
| NeoForge | `1.21.1` |

If you need the exact current matrix, run:

```bash
just list-nodes
```

## Development

Common commands:

| Command | Purpose |
|---------|---------|
| `./gradlew build` | Build the full Gradle graph |
| `just list-nodes` | Print every enabled Minecraft/loader node |
| `just run 1.21.11 forge runClient` | Run the Forge client for `1.21.11` |
| `just run 1.16.5 forge runClient` | Run the legacy Forge client helper for `1.16.5` |
| `just run 26.1 publish` | Publish all enabled loaders for `26.1` |
| `just run downloadTranslations` | Download translations |

`just run` accepts three forms:

| Form | Example |
|------|---------|
| `just run <version> <loader> <task...>` | `just run 1.21.11 fabric build` |
| `just run <version> <aggregate-task...>` | `just run 1.21.11 publishMod` |
| `just run <root-task...>` | `just run downloadTranslations` |

## Runtime Validation

Konfig has three useful runtime validation layers:

| Command | What it checks |
|---------|----------------|
| `just boot-check 1.21.11-forge 60` | Starts the client and confirms Konfig initializes from logs |
| `just teakit-boot-check 1.21.11-forge 60` | Enables TeaKit as an optional dev runtime dependency when that Minecraft line has a TeaKit catalog entry, then confirms Konfig and TeaKit initialize |
| `just scenario-check 1.21.11-forge 240` | Runs the checked-in TeaKit UI scenario, opens the title-screen Mods menu, opens Konfig's config screen, and asserts that `Enable Debug Logging` is present |

Matrix-wide helpers:

| Command | Purpose |
|---------|---------|
| `just boot-check-all 60` | Run boot checks across the matrix |
| `just teakit-boot-check-all 60` | Run TeaKit boot checks across the matrix |
| `just scenario-check-all 240` | Run UI scenario checks across the matrix |

## Repository Layout

Konfig uses a branch-based Stonecutter layout:

```text
konfig/
|-- common/                 shared code and resources
|-- fabric/                 Fabric-specific code and metadata
|-- forge/                  Forge-specific code and metadata
|-- neoforge/               NeoForge-specific code and metadata
|-- versions/<mc>/          per-version properties and overlays
|-- settings.gradle.kts     Stonecutter project graph entrypoint
|-- stonecutter.gradle.kts  root task wiring
`-- justfile                developer workflows
```

The effective source for a node comes from the shared roots plus the matching `versions/<mc>/...` overlays. The exact enabled loaders for each line come from `versions/<mc>/gradle.properties`.

## Notes

- Konfig keeps one semantic release across all supported Minecraft lines.
- Loader- and version-specific divergence is isolated in `versions/<mc>/` or loader roots rather than split into independent per-version repos.
- The checked-in debug config exists specifically to exercise Konfig's own screen, sync, and editor paths during runtime validation.

## License

Konfig is licensed under [MIT](LICENSE).
