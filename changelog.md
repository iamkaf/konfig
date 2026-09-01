# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

See the full changelog at https://github.com/iamkaf/konfig

## 0.7.1

### Fixed

- Config screens with clickable URL decorations no longer crash when opened.
- Fieldset list editors no longer crash when they contain text-entry controls.

## 0.7.0

### Added

- Added an experimental Fieldset API and generated editor for ordered collections of structured config entries on Minecraft `1.21.11` and newer.
- Added negotiated remote editing for synchronized common and server configs. Operators with permission level 2 can apply a complete config draft when both peers support editing; other peers keep the existing read-only synchronized view.

### Fixed

- Numeric sliders can now be adjusted one value at a time with the arrow keys and reset to their configured default with right-click.
- Config saves preserve unknown TOML values and comments, preserve malformed files before restoring defaults, and refuse to overwrite files written with a newer schema.
- Synchronized server values remain authoritative until the server updates them or the client disconnects.

## 0.6.1

### Added

- Added translated config value tooltips with `ValueBuilder.tooltipKey(String)`.

### Changed

- Completely reorganized Konfig's core to give configuration storage, migration, screens, synchronization, runtime setup, and loader integration clear boundaries. This makes behavior easier to keep consistent across every supported Minecraft version and loader.
- Documented the supported `api.v1` addon API and marked implementation types as internal so addon authors can clearly distinguish stable APIs from details that may change between releases.
- See the [Konfig README](https://github.com/iamkaf/konfig#quick-start) for a guide on how to add Konfig support to your mod.

## 0.5.0

### Added

- Added dropdown config values with `ConfigBuilder.dropdown(...)` for settings with a fixed list of string options.
  - Dropdown option labels can be translated with `konfig.value.<modid>.<config>.<path>.<option>` language keys.
  - Dropdown menus support mouse selection, Escape/Enter/Tab, arrow-key navigation, and scrolling for long option lists.

### Fixed

- Legacy Forge config screens now preserve each config's declared setting order instead of sorting settings by path.

## 0.4.0

### Added

- Added translated info-panel builder methods for headers, text, and link labels.
- Added fallback support for mod-owned config label and enum value translations, preserving existing translations when configs move into categorized screens.

### Changed

- Ported to Minecraft 26.2.
- Config screens now keep routine saves quiet and show important failures as Minecraft toasts.

### Fixed

- Server-synced config values now update connected players when a config is saved or reloaded.
- Config screens opened for a specific mod now show that mod's title instead of the generic Konfig title.
- Invalid text and color edits now show feedback next to the setting instead of using the old footer status line.

## 0.3.1

### Fixed

- Fixed registry-backed config screen icons on Minecraft `26.1`, `26.1.1`, and `26.1.2`.

## 0.3.0

### Added

- Support for every Konfig Minecraft line from `1.14.4` through `26.1.2`.
- Inline config-screen decorations for headers, images, descriptive text, and clickable URLs.
- Info-panel builder API for explicit hover details.
- Explicit config-screen tooltip APIs for categories and values.
- Config-screen title helper support across supported versions.

### Changed

- Builder comment APIs now write TOML comments only; config-screen tooltips use explicit tooltip APIs.
- NeoForge coverage now spans every supported line from `1.21.1` through `26.1.2`.

### Fixed

- Config-screen tooltips now render above other screen elements.

## 0.2.0

### Changed

- Added Minecraft 26.1 support.
- Improved generated config screen labels and titles.

## 0.1.0

### Added

- Initial public release of Konfig.
- Multiloader support for Fabric, Forge, and NeoForge across `1.21.11`, `1.21.10`, `1.21.1`, `1.20.1`, `1.19.2`, `1.18.2`, and `1.16.5`.
- Typed config builder API with side-aware scopes for client, server, and common use.
- Built-in server-to-client sync for shared config values.
- Commented TOML config files with generated config screens.
- Fabric Mod Menu integration plus Forge and NeoForge config-screen hooks.
- Schema versioning and forward migration support for persisted TOML configs.

## Types of changes

- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.
