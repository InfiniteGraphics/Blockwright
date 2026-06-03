# Preset Library UI

This patch makes Preset Library the primary entry point for the in-game PCG editor.

## Player flow

1. Press `G` to open the Blockwright editor.
2. Use the left toolbar item `Preset Library`, or click the `Preset: ...` button in the top bar.
3. Filter presets by category or search text.
4. Select a preset card.
5. Read the details and validation panel on the right.
6. Click `Use Preset` to jump to the required input tool:
   - `Region` presets switch to `Box Region`.
   - `Spline` presets switch to `Spline`.
   - Graph-only presets switch to `Node Graph`.
7. Press `Preview`, then `Bake` when the result is valid.

## UI changes

- The top bar no longer uses left/right arrows for preset browsing.
- Presets are now browsed through a dedicated visual library.
- `Node Graph` is only an inspector for the currently selected graph preset.
- Preset cards show category, input requirement, executor type, graph flag, and broken state.
- The details panel shows rule/executor metadata, graph counts, next steps, and preset-level validation.

## Categories

The first version derives categories from preset ids, names, types, and executor metadata:

- Buildings
- Roads
- Scatter
- Connectors
- Graph
- Legacy
- Broken

Later, these should move into preset metadata fields such as `category`, `tags`, `description`, and `icon`.
