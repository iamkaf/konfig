# TODO

## Screen Adapters

- Split the remaining large editor/input sections in `common/src/main/java/com/iamkaf/konfig/impl/v1/KonfigConfigScreen.java` into adapters.
- Start with the registry text rows and string-list editor rows.
- Introduce small version-band adapters for:
  - input event handling
  - text input and suggestion dropdown behavior
  - editor/list row rendering
- Keep shared screen state and flow in `KonfigConfigScreen`.
- Prefer adapter seams over new per-version overlays.
