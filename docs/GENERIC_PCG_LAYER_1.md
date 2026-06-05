# Generic PCG Layer 1

This document describes the first generic node layer added on top of the current Blockwright PCG runtime.

The goal of this layer is to move the graph away from hard-coded building semantics and toward a more reusable geometry-and-attribute workflow.

## Design intent

Layer 1 keeps the current runtime data model intact:

- `volume`
- `points`
- `modules`
- `blocks`

It does not introduce full `surface`, `instance`, or `field` data streams yet. Instead, it adds a set of generic nodes that make the current runtime more useful for architectural graphs.

## Generic aliases

The following aliases are now available in the node library:

- `sample_curve` -> existing spline sampler
- `scatter_in_volume` -> existing point scatter
- `instance_modules` -> existing module placement
- `instance_modules_every_n` -> existing interval module placement
- `filter_random` -> existing random point filter
- `set_attribute` -> existing attribute write node with editor-friendly parameters

These aliases let authors build graphs with more neutral names that fit buildings, roads, courtyards, interiors, and non-building procedural content.

## New nodes

### `merge`

Merges all incoming point and volume data into a single stream.

Use it when two parallel branches need to be rejoined before more processing.

### `offset_points`

Offsets points in:

- world space: `dx`, `dy`, `dz`
- tangent space: `tangentOffset`
- normal space: `normalOffset`

This is the basic transform node for point-based architectural placement.

### `snap_to_grid`

Snaps points to a configurable grid with per-axis size and origin.

This is useful for:

- aligning facade anchors
- forcing instance points back to a block grid
- cleaning up sampled or scattered positions before placement

### `volume_boundary`

Samples points from an incoming volume boundary.

Supported modes:

- `sides`
- `top`
- `bottom`
- `all`

Generated points carry at least:

- `face`
- `volumeLabel` when present on the source volume

This node is the first step toward reusable architectural boundary workflows without hard-coding concepts like facade or floor.

### `filter_by_attribute`

Filters incoming points by built-in values or custom attributes.

Supported built-ins:

- `density`
- `seed`
- `x`
- `y`
- `z`

Supported operations:

- `eq`
- `ne`
- `gt`
- `gte`
- `lt`
- `lte`
- `exists`
- `missing`

This is the first general-purpose selection node for attribute-driven graph logic.

## `set_attribute` changes

`set_attribute` and `attribute_set` now support editor-friendly parameterized writes:

- `attribute`
- `valueType`
- `stringValue`
- `numberValue`
- `intValue`
- `boolValue`
- optional `density` override

This avoids requiring a raw JSON object just to write one attribute from the editor UI.

## Example building-oriented flow

This is not a building-specific node graph. It is a generic geometry workflow that can be used for building boundaries.

```json
{
  "debug": false,
  "nodes": [
    { "id": "region", "type": "region_input", "x": 80, "y": 120 },
    { "id": "boundary", "type": "volume_boundary", "faces": "sides", "horizontalStep": 3, "verticalStep": 4, "x": 320, "y": 120 },
    { "id": "mark", "type": "set_attribute", "attribute": "zone", "valueType": "string", "stringValue": "outer_shell", "x": 560, "y": 120 },
    { "id": "grid", "type": "snap_to_grid", "gridX": 1.0, "gridY": 1.0, "gridZ": 1.0, "x": 800, "y": 120 },
    { "id": "windows", "type": "filter_by_attribute", "attribute": "face", "operation": "ne", "valueType": "string", "stringValue": "bottom", "x": 1040, "y": 120 },
    { "id": "modules", "type": "instance_modules", "tagConfig": "windowTag", "followTangent": true, "x": 1280, "y": 120 }
  ],
  "edges": [
    ["region", "boundary"],
    ["boundary", "mark"],
    ["mark", "grid"],
    ["grid", "windows"],
    ["windows", "modules"]
  ]
}
```

Even in the current runtime, this already reads much closer to a generic geometry graph than a special-case building graph.

## What Layer 1 still does not solve

Layer 1 is intentionally narrow. It does not yet provide:

- real surface data streams
- instance data streams separate from immediate placement
- boolean or branch value streams
- group/partition operations
- true subgraph reuse
- length-fitting module selection
- connector-constraint solving

Those belong in later layers.
