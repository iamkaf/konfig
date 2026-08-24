<p align="center">
  <img src="assets/banner.png" alt="Konfig banner" width="600" />
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-a78bfa?style=for-the-badge&labelColor=0d1117" alt="MIT License" /></a>
</p>

<h1 align="center">Konfig</h1>

<p align="center">
  <strong>A multiloader configuration library for Minecraft mods.</strong>
</p>

---

Konfig lets Minecraft mods define typed config values and structured rule collections in common code, persist them as commented TOML, synchronize selected values, and generate in-game config screens.

It is built for shared common code. Loader-specific integration stays in the loader roots, while config declaration, validation, migration, and screen metadata can live beside the rest of your common mod logic.

## Table of Contents

- [What Konfig Provides](#what-konfig-provides)
- [How It Works](#how-it-works)
- [Quick Start](#quick-start)
- [Basic Usage](#basic-usage)
- [Builder API](#builder-api)
- [Value Types](#value-types)
- [Files And Sync](#files-and-sync)
- [Generated Screens](#generated-screens)
- [Fieldsets And Catalogs](#fieldsets-and-catalogs)
- [Inline Decorations](#inline-decorations)
- [Migrations](#migrations)
- [Development](#development)
- [Runtime Validation](#runtime-validation)
- [Repository Layout](#repository-layout)
- [Notes](#notes)
- [License](#license)

## What Konfig Provides

| Area | Details |
|------|---------|
| Typed values | Booleans, ranged integers, ranged longs, ranged doubles, dropdowns, enums, strings, string lists, RGB colors, ARGB colors, and custom codecs |
| Structured values | Fieldsets for ordered, validated collections of typed entries, with simple list and large catalog presentations |
| Side-aware scopes | `CLIENT`, `COMMON`, and `SERVER` configs |
| Files | Commented TOML under `config/<modid>/<name>.toml` |
| Sync | `NONE`, `LOGIN`, and `LOGIN_AND_RELOAD` sync modes, synchronized client views, and server-authoritative remote editing |
| Migrations | Explicit schema versions and step-by-step migration functions |
| Screens | Generated config screens for registered handles, including adaptive catalog screens for large collections |
| Screen content | Value editors, category headers, images, inline text, links, and info panels |
| Loader hooks | Fabric Mod Menu integration plus Forge and NeoForge mod-list config button helpers |

## How It Works

```text
common mod code
    |
    v
Konfig.builder(modid, name)
    |
    +-- typed values, structured Fieldsets, validators, migration steps
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

Published artifacts share a semantic release, and the `+<mc>` suffix identifies the target Minecraft line.

For example, a version shaped like `0.7.0+<minecraft-version>` identifies Konfig `0.7.0` and its target Minecraft line.

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
                .tooltipKey("examplemod.config.enabled.tooltip")
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

Use `ConfigValue#get()` when reading the effective value and `ConfigValue#set(value)` when changing it programmatically.

```java
if (ExampleConfig.ENABLED.get()) {
    int radius = ExampleConfig.RANGE.get();
}
```

Synchronized values return the server overlay from `get()` while connected. Code that owns the authoritative local value, including an integrated server, can read `local()` instead:

```java
int serverRadius = ExampleConfig.RANGE.local();
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
| `tooltipKey(String)` | Translated screen tooltip, resolved when the editor opens |
| `info(Consumer<InfoPanelBuilder>)` | Add richer generated-screen help |
| `restart(RestartRequirement)` | Mark values that need a restart or reload |
| `sync(boolean)` | Include or exclude a value from sync |
| `clientOnly()` | Restrict the value to client-side use |
| `serverOnly()` | Restrict the value to server-side use |
| `remoteScreenView(value, available)` | Supply a derived read-only value for generated screens while connected to a remote authority |
| `validate(Predicate<T>, String)` | Reject invalid values with an error message |
| `build()` | Register the value and return `ConfigValue<T>` |

## Value Types

| Method | Type | Notes |
|--------|------|-------|
| `bool(key, defaultValue)` | `Boolean` | Renders as a toggle |
| `intRange(key, defaultValue, min, max)` | `Integer` | Enforces and displays an integer range |
| `longRange(key, defaultValue, min, max)` | `Long` | Enforces and displays a long range |
| `doubleRange(key, defaultValue, min, max)` | `Double` | Enforces and displays a double range |
| `string(key, defaultValue, minLen, maxLen)` | `String` | Provides length bounds and registry autocomplete |
| `stringList(key, defaultValue)` | `List<String>` | Provides registry autocomplete per entry |
| `dropdown(key, defaultValue, options)` | `String` | Restricts values to a fixed option list and renders as a dropdown |
| `enumValue(key, defaultValue)` | enum | Uses the enum constants as choices |
| `colorRgb(key, defaultValue)` | `Integer` | RGB color value |
| `colorArgb(key, defaultValue)` | `Integer` | ARGB color value |
| `custom(key, defaultValue, codec)` | custom | Uses a `KonfigCodec<T>` |
| `fieldset(key, defaultValue)` | `FieldsetValue` | Stores an ordered collection of typed structured entries |

String and string-list values can be connected to a registry for autocomplete. Use a `ResourceKey<? extends Registry<?>>` where the consumer source has one, or the registry id string overload where it does not.

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

Synchronized `COMMON` and `SERVER` configs remain server-authoritative. After the client and server negotiate remote editing, an operator with permission level 2 can edit those values through Konfig's generated screen. Konfig validates the complete draft, rejects stale revisions, persists the accepted draft on the server, and broadcasts the new snapshot. Other players receive the synchronized read-only view.

Use `remoteScreenView(...)` when the generated screen should display a consumer-owned projection of remote state instead of the value's ordinary synchronized overlay. The supplier does not replace or persist the local value, and Konfig returns to the local screen value after disconnecting.

## Generated Screens

Konfig generates screens from the registered config handles. The screen uses the value metadata from your config declarations, including comments, tooltips, restart requirements, validators, categories, and info panels.

| Editor support | Screen behavior |
|----------------|-----------------|
| Booleans | Toggle editor |
| Numbers | Numeric editor with range metadata |
| Dropdowns | Dropdown menu with fixed string options, option labels, option tooltips, scrolling, and keyboard navigation |
| Enums | Choice editor |
| String lists | List editor |
| Fieldsets | Structured entry editor or adaptive catalog screen |
| RGB and ARGB colors | Color editor |
| Registry-backed strings | Autocomplete when a registry source is bound |

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

## Fieldsets And Catalogs

Fieldsets store ordered entries that share one typed schema. A field can be a boolean, ranged number, string, optional string, dropdown, or registry-backed string. Fieldsets provide per-field validation, whole-entry validation, stable entry identities, read-only built-in entries, editable user entries, and keyed user overrides.

```java
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetBuilder;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetCatalog;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import net.minecraft.core.registries.Registries;

ConfigBuilder builder = Konfig.builder("examplemod", "common")
        .scope(ConfigScope.COMMON)
        .syncMode(SyncMode.LOGIN_AND_RELOAD);

FieldsetField<String> item = FieldsetField.registryString(
        "item",
        "minecraft:iron_sword",
        Registries.ITEM
);
FieldsetField<String> role = FieldsetField.dropdown(
        "role",
        "weapon",
        java.util.Arrays.asList("weapon", "tool", "armor")
);
FieldsetField<Integer> power = FieldsetField.intRange("power", 1, 0, 100);

FieldsetCatalog catalog = FieldsetCatalog.create()
        .editableProfile("User Rules")
        .newEntryLabel("Add Rule")
        .overrideLabel("Override")
        .filter(role)
        .section("Rule", item, role, power)
        .build();

FieldsetValue defaults = FieldsetBuilder.create()
        .field(item)
        .field(role)
        .field(power)
        .key(item)
        .title(item)
        .icon(item)
        .summary(role, power)
        .catalog(catalog)
        .entry(FieldsetEntry.builtin("iron_sword", "Example Mod")
                .with(item, "minecraft:iron_sword")
                .with(role, "weapon")
                .with(power, 4))
        .build();

ConfigValue<FieldsetValue> rules = builder.fieldset("rules", defaults)
        .sync(true)
        .build();
```

Without `.catalog(...)`, Konfig uses the simple Fieldset list and entry screens. A catalog groups built-in entries by source profile and keeps user entries in a dedicated editable profile. It adds search, an optional declared-field filter, compact summaries, grouped detail sections, registry icons, contextual Add, Override, and Delete actions, autosave, and Undo. The layout adapts between side-by-side details and a separate detail page according to the available width.

Declaring a key field gives user entries replacement semantics. A user entry with the same key hides the matching built-in entry in generated views; deleting the override reveals the built-in entry again. Built-in defaults never become writable.

Fieldsets and catalogs are experimental in `api.v1`. They use Konfig's normal persistence, validation, sync, and generated-screen lifecycle, but their source API may still change before `1.0.0`.

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

Konfig provides explicit schema migrations:

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

## Development

Common commands:

| Command | Purpose |
|---------|---------|
| `./gradlew build` | Build the full Gradle graph |
| `just list-nodes` | Print every enabled Minecraft/loader node |
| `just run <mc> forge runClient` | Run the Forge client for one Minecraft line |
| `just run <mc> publish` | Publish all enabled loader artifacts for one Minecraft line |
| `just run downloadTranslations` | Download translations |

`just run` accepts three forms:

| Form | Example |
|------|---------|
| `just run <version> <loader> <task...>` | `just run <mc> fabric build` |
| `just run <version> <aggregate-task...>` | `just run <mc> publishMod` |
| `just run <root-task...>` | `just run downloadTranslations` |

## Runtime Validation

Konfig has three useful runtime validation layers:

| Command | What it checks |
|---------|----------------|
| `just boot-check <mc>-forge 60` | Starts the client and confirms Konfig initializes from logs |
| `just teakit-boot-check <mc>-forge 60` | Enables TeaKit as an optional dev runtime dependency, then confirms Konfig and TeaKit initialize |
| `just teakit-check <mc>-forge 240` | Runs the checked-in TeaKit UI test, opens the title-screen Mods menu, opens Konfig's config screen, and asserts that `Enable Debug Logging` is present |

Matrix-wide helpers:

| Command | Purpose |
|---------|---------|
| `just boot-check-all 60` | Run boot checks across the matrix |
| `just teakit-boot-check-all 60` | Run TeaKit boot checks across the matrix |
| `just teakit-check-all 240` | Run UI tests across the matrix |

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

- Published Konfig artifacts share one semantic release.
- Loader- and version-specific divergence is isolated in `versions/<mc>/` or loader roots rather than split into independent per-version repos.
- The checked-in debug config exists specifically to exercise Konfig's own screen, sync, and editor paths during runtime validation.

## License

Konfig is licensed under [MIT](LICENSE).
