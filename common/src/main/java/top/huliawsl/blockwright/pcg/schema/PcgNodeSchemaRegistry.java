package top.huliawsl.blockwright.pcg.schema;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import top.huliawsl.blockwright.pcg.PcgNodeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PcgNodeSchemaRegistry {
    private static final Map<String, JsonObject> SCHEMAS = new LinkedHashMap<>();

    static {
        schema("region_input", "Region Input", "Inputs", "volume", "Current box selection from MC.",
                params(param("label", "string", "region", "Output volume label.")));
        schema("shape_input", "Shape Input", "Inputs", "volume", "Generic alias for the current shape/box selection.",
                params(param("label", "string", "shape", "Output volume label.")));
        schema("spline_input", "Spline Input", "Inputs", "spline", "Current spline selection from MC.");
        schema("curve_input", "Curve Input", "Inputs", "spline", "Generic alias for the current curve/spline selection.");
        schema("resample_spline", "Resample Spline", "Curves", "points", "Samples the input spline into points.",
                params(param("spacing", "number", 1.0D, "Distance between samples."), param("seedParam", "string", "seed", "Preset parameter used as seed.")));
        schema("sample_curve", "Sample Curve", "Curves", "points", "Samples the input curve into points.",
                params(param("spacing", "number", 1.0D, "Distance between samples."), param("seedParam", "string", "seed", "Preset parameter used as seed.")));
        schema("curve_resample", "Curve Resample", "Geometry", "points", "Generic alias for sampling an input curve into points.",
                params(param("spacing", "number", 1.0D, "Distance between samples."), param("seedParam", "string", "seed", "Preset parameter used as seed.")));
        schema("curve_ribbon", "Curve Ribbon", "Geometry", "points", "Expands curve samples into a width-based point ribbon.",
                params(param("width", "int", 5, "Fallback ribbon width."), param("widthParam", "string", "", "Preset width parameter."),
                        param("step", "int", 1, "Offset step across the ribbon."), param("edgeAttribute", "string", "edge", "Boolean edge attribute name."),
                        param("offsetAttribute", "string", "ribbonOffset", "Numeric offset attribute name."), param("yOffset", "number", 0.0D, "Vertical point offset.")));
        schema("shape_metrics", "Shape Metrics", "Geometry", "volume", "Writes width/depth/height/center/area attributes onto shapes.",
                params(param("emitCenterPoint", "bool", false, "Also emit one center point per shape.")));
        schema("measure_shape", "Measure Shape", "Geometry", "volume", "Alias for shape_metrics.",
                params(param("emitCenterPoint", "bool", false, "Also emit one center point per shape.")));
        schema("shape_extrude", "Shape Extrude", "Geometry", "volume", "Extrudes an input shape volume to a configurable height.",
                params(param("height", "int", 8, "Fallback extrusion height."), param("heightParam", "string", "", "Preset height parameter."),
                        param("heightExpression", "string", "", "Expression such as clamp(floor(shortSide / 4) * ${preset.floorHeight}, 4, 16)."),
                        param("clipToInput", "bool", true, "Clip height to the original selection volume."), param("label", "string", "", "Optional output label override.")));
        schema("voxelize_volume", "Voxelize Volume", "Geometry", "blocks", "Materializes volumes as solid or boundary block shells.",
                params(param("mode", "string", "boundary", "solid or boundary."), param("floorStep", "int", 0, "Optional repeated floor layer step."),
                        param("block", "block", "minecraft:stone_bricks", "Main block."), param("floorBlock", "block", "", "Optional floor block."),
                        param("roofBlock", "block", "", "Optional roof/top block."), param("blockParam", "string", "", "Preset main block parameter."),
                        param("floorBlockParam", "string", "", "Preset floor block parameter."), param("roofBlockParam", "string", "", "Preset roof block parameter.")));
        schema("block_paint", "Block Paint", "Placement", "blocks", "Places one block per incoming point.",
                params(param("block", "block", "minecraft:stone", "Main block."), param("edgeBlock", "block", "", "Optional block used when edgeAttribute is true."),
                        param("blockParam", "string", "", "Preset main block parameter."), param("edgeBlockParam", "string", "", "Preset edge block parameter."),
                        param("edgeAttribute", "string", "edge", "Boolean attribute selecting edgeBlock."), param("blockAttribute", "string", "", "Optional point attribute containing a block id."),
                        param("blockTemplate", "string", "", "Optional block-state template using point attributes, e.g. minecraft:oak_stairs[facing=${face}]."),
                        param("yOffset", "int", 0, "Vertical block offset.")));
        schema("voxelize_surface", "Voxelize Surface", "Placement", "blocks", "Alias for block_paint when point ribbons represent surfaces.",
                params(param("block", "block", "minecraft:stone", "Main block."), param("edgeBlock", "block", "", "Optional block used when edgeAttribute is true."),
                        param("blockParam", "string", "", "Preset main block parameter."), param("edgeBlockParam", "string", "", "Preset edge block parameter."),
                        param("edgeAttribute", "string", "edge", "Boolean attribute selecting edgeBlock."), param("blockAttribute", "string", "", "Optional point attribute containing a block id."),
                        param("blockTemplate", "string", "", "Optional block-state template using point attributes."),
                        param("yOffset", "int", 0, "Vertical block offset.")));
        schema("road_surface", "Road Surface", "Roads", "blocks", "Builds a road surface from spline samples.",
                params(param("width", "int", 5, "Fallback road width."), param("widthParam", "string", "roadWidth", "Preset width parameter."),
                        param("roadBlock", "block", "minecraft:stone", "Road block state."), param("edgeBlock", "block", "minecraft:cobblestone", "Edge block state."),
                        param("yOffset", "int", 0, "Vertical offset.")));
        schema("place_modules_every_n", "Place Modules Every N", "Modules", "modules", "Places modules at every Nth point.",
                params(param("tag", "string", "", "Explicit module tag."), param("tagConfig", "string", "", "Rule config key for module tag."),
                        param("every", "int", 6, "Point interval."), param("start", "int", 0, "Start index."),
                        param("sideMode", "string", "random", "left, right, alternate, or random."), param("sideOffset", "int", 3, "Offset from spline."),
                        param("yOffset", "int", 0, "Vertical offset."), param("followTangent", "bool", true, "Rotate with tangent."),
                        param("ignoreAir", "bool", true, "Ignore air blocks inside modules."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("instance_modules_every_n", "Instance Modules Every N", "Instances", "modules", "Places module instances at every Nth point.",
                params(param("tag", "string", "", "Explicit module tag."), param("tagConfig", "string", "", "Rule config key for module tag."),
                        param("every", "int", 6, "Point interval."), param("start", "int", 0, "Start index."),
                        param("sideMode", "string", "random", "left, right, alternate, or random."), param("sideOffset", "int", 3, "Offset from spline."),
                        param("yOffset", "int", 0, "Vertical offset."), param("followTangent", "bool", true, "Rotate with tangent."),
                        param("ignoreAir", "bool", true, "Ignore air blocks inside modules."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("place_modules", "Place Modules", "Modules", "modules", "Places modules at incoming points.",
                params(param("tag", "string", "", "Explicit module tag."), param("tagConfig", "string", "", "Rule config key for module tag."),
                        param("tagAttribute", "string", "", "Point attribute containing the module tag."), param("tagTemplate", "string", "", "Template such as vegetation/${species}."),
                        param("styleAttribute", "string", "", "Point attribute containing the style."), param("styleTemplate", "string", "", "Template for style."),
                        param("every", "int", 1, "Point interval."), param("start", "int", 0, "Start index."),
                        param("normalOffset", "number", 0.0D, "Offset along point normal."), param("yOffset", "int", 0, "Vertical offset."),
                        param("followTangent", "bool", true, "Rotate with tangent."), param("ignoreAir", "bool", true, "Ignore air blocks."),
                        param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("instance_modules", "Instance Modules", "Instances", "modules", "Places module instances at incoming points.",
                params(param("tag", "string", "", "Explicit module tag."), param("tagConfig", "string", "", "Rule config key for module tag."),
                        param("every", "int", 1, "Point interval."), param("start", "int", 0, "Start index."),
                        param("normalOffset", "number", 0.0D, "Offset along point normal."), param("yOffset", "int", 0, "Vertical offset."),
                        param("followTangent", "bool", true, "Rotate with tangent."), param("ignoreAir", "bool", true, "Ignore air blocks."),
                        param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("box_building_shell", "Box Building Shell", "Buildings", "blocks", "Creates a simple building shell from a region.",
                params(param("floors", "int", 3, "Fallback floor count."), param("floorHeight", "int", 4, "Fallback floor height."),
                        param("floorsParam", "string", "floors", "Preset floor-count parameter."), param("floorHeightParam", "string", "floorHeight", "Preset floor-height parameter."),
                        param("wallBlock", "block", "minecraft:stone_bricks", "Wall block."), param("floorBlock", "block", "minecraft:oak_planks", "Floor block."),
                        param("roofBlock", "block", "minecraft:stone", "Roof block.")));
        schema("facade_modules", "Facade Modules", "Buildings", "modules", "Places facade modules along generated shell facades.",
                params(param("tag", "string", "", "Explicit module tag."), param("tagConfig", "string", "windowTag", "Rule config key for facade module tag."),
                        param("floors", "int", 3, "Fallback floors."), param("floorHeight", "int", 4, "Fallback floor height."),
                        param("horizontalStep", "int", 3, "Horizontal placement step."), param("inset", "int", 2, "Facade inset."),
                        param("ignoreAir", "bool", true, "Ignore air in modules."), param("styleParam", "string", "style", "Preset style parameter."),
                        param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("scatter_points", "Scatter Points", "Points", "points", "Scatters points in a volume.",
                params(param("count", "int", 64, "Number of points."), param("spacing", "int", 1, "Grid spacing."),
                        param("grid", "bool", false, "Use grid mode."), param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("scatter_in_volume", "Scatter In Volume", "Geometry", "points", "Scatters points inside the incoming volume.",
                params(param("count", "int", 64, "Number of points."), param("spacing", "int", 1, "Grid spacing."),
                        param("grid", "bool", false, "Use grid mode."), param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("merge", "Merge", "Geometry", "any", "Merges all incoming point and volume data.");
        schema("offset_points", "Offset Points", "Geometry", "points", "Offsets points in world, tangent, and normal space.",
                params(param("dx", "number", 0.0D, "World-space X offset."),
                        param("dy", "number", 0.0D, "World-space Y offset."),
                        param("dz", "number", 0.0D, "World-space Z offset."),
                        param("tangentOffset", "number", 0.0D, "Offset along point tangent."),
                        param("normalOffset", "number", 0.0D, "Offset along point normal.")));
        schema("snap_to_grid", "Snap To Grid", "Geometry", "points", "Snaps incoming points to a configurable grid.",
                params(param("gridX", "number", 1.0D, "Grid size on X."),
                        param("gridY", "number", 1.0D, "Grid size on Y."),
                        param("gridZ", "number", 1.0D, "Grid size on Z."),
                        param("originX", "number", 0.5D, "Grid origin on X."),
                        param("originY", "number", 0.5D, "Grid origin on Y."),
                        param("originZ", "number", 0.5D, "Grid origin on Z.")));
        schema("volume_boundary", "Volume Boundary", "Geometry", "points", "Samples points on top, bottom, or side faces of a volume.",
                params(param("faces", "string", "sides", "all, sides, top, or bottom."),
                        param("horizontalStep", "int", 1, "Horizontal sampling step."),
                        param("verticalStep", "int", 1, "Vertical sampling step for side faces."),
                        param("inset", "int", 0, "Inset applied before sampling.")));
        schema("sample_boundary", "Sample Boundary", "Geometry", "points", "Generic alias for sampling a volume boundary.",
                params(param("faces", "string", "sides", "all, sides, top, or bottom."),
                        param("horizontalStep", "int", 1, "Horizontal sampling step."),
                        param("verticalStep", "int", 1, "Vertical sampling step for side faces."),
                        param("inset", "int", 0, "Inset applied before sampling.")));
        schema("random_filter", "Random Filter", "Filters", "points", "Randomly filters incoming points.",
                params(param("probability", "number", 0.5D, "Keep probability, 0..1."), param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("filter_random", "Filter Random", "Filters", "points", "Randomly keeps incoming points.",
                params(param("probability", "number", 0.5D, "Keep probability, 0..1."), param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("attribute_set", "Attribute Set", "Attributes", "points", "Writes point attributes.",
                params(param("attribute", "string", "", "Attribute key to write."),
                        param("valueType", "string", "string", "Value type: string, number, int, bool."),
                        param("stringValue", "string", "", "String attribute value."),
                        param("numberValue", "number", 0.0D, "Number attribute value."),
                        param("intValue", "int", 0, "Integer attribute value."),
                        param("boolValue", "bool", false, "Boolean attribute value."),
                        param("density", "number", -1.0D, "Optional density override. Values below 0 leave density unchanged.")));
        schema("set_attribute", "Set Attribute", "Attributes", "points", "Writes one attribute and optional density override onto points.",
                params(param("attribute", "string", "", "Attribute key to write."),
                        param("valueType", "string", "string", "Value type: string, number, int, bool."),
                        param("stringValue", "string", "", "String attribute value."),
                        param("numberValue", "number", 0.0D, "Number attribute value."),
                        param("intValue", "int", 0, "Integer attribute value."),
                        param("boolValue", "bool", false, "Boolean attribute value."),
                        param("density", "number", -1.0D, "Optional density override. Values below 0 leave density unchanged.")));
        schema("attribute_random", "Attribute Random", "Attributes", "points", "Writes a random value from config.values into an attribute.",
                params(param("attribute", "string", "variant", "Attribute key."), param("seed", "int", 0, "Local seed."),
                        param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("attribute_expression", "Attribute Expression", "Attributes", "any", "Writes attributes by evaluating math/string expressions against point or volume attributes.",
                params(param("attribute", "string", "", "Single attribute key."), param("expression", "string", "", "Single expression."),
                        param("target", "string", "both", "points, volumes, or both. config.expressions can define multiple attributes.")));
        schema("attribute_math", "Attribute Math", "Attributes", "any", "Alias for attribute_expression.",
                params(param("attribute", "string", "", "Single attribute key."), param("expression", "string", "", "Single expression."),
                        param("target", "string", "both", "points, volumes, or both.")));
        schema("filter_by_attribute", "Filter By Attribute", "Filters", "points", "Keeps points whose built-in or custom attribute matches a comparison.",
                params(param("attribute", "string", "density", "Built-in field or attribute key."),
                        param("operation", "string", "eq", "eq, ne, gt, gte, lt, lte, exists, missing."),
                        param("valueType", "string", "number", "Value type: string, number, int, bool."),
                        param("stringValue", "string", "", "String comparison value."),
                        param("numberValue", "number", 0.0D, "Number comparison value."),
                        param("intValue", "int", 0, "Integer comparison value."),
                        param("boolValue", "bool", false, "Boolean comparison value.")));
        schema("filter_by_expression", "Filter By Expression", "Filters", "any", "Keeps points or volumes matching an expression such as floorIndex == 0 && isCenter.",
                params(param("expression", "string", "", "Boolean expression."), param("target", "string", "points", "points, volumes, or both.")));
        schema("select_point", "Select Point", "Points", "points", "Selects first/last/random/center/center_bottom points, optionally per group.",
                params(param("mode", "string", "center", "first, last, random, center, center_bottom, center_top, or all."),
                        param("count", "int", 1, "Number of points per group."), param("groupAttribute", "string", "", "Optional attribute grouping key."),
                        param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("facade_grid", "Facade Grid", "Geometry", "points", "Samples regular cells on volume side faces and writes face/floorIndex/col/isCenter attributes.",
                params(param("faces", "string", "sides", "sides, all, north, south, west, or east."), param("cellWidth", "int", 3, "Horizontal cell width."),
                        param("floorHeight", "int", 4, "Floor row height."), param("inset", "int", 1, "Horizontal inset from edges."),
                        param("yOffset", "int", 1, "Bottom Y offset from volume min."), param("includeGroundFloor", "bool", true, "Include row 0.")));
        schema("roof_from_footprint", "Roof From Footprint", "Geometry", "blocks", "Builds a simple layered or gable roof from a volume footprint.",
                params(param("roofType", "string", "gable", "gable, pyramid, or layered."), param("height", "int", 3, "Maximum roof height."),
                        param("overhang", "int", 1, "Horizontal overhang."), param("roofBlock", "block", "minecraft:oak_planks", "Roof block."),
                        param("ridgeBlock", "block", "", "Optional ridge block."), param("roofBlockParam", "string", "", "Preset roof block parameter."),
                        param("ridgeBlockParam", "string", "", "Preset ridge block parameter.")));
        schema("terrain_sample", "Terrain Sample", "World", "points", "Samples terrain height for incoming points.",
                params(param("yOffset", "int", 0, "Vertical offset after sampling.")));
        schema("query_terrain", "Query Terrain", "World", "points", "Samples terrain height and writes world attributes such as biome and groundBlock.",
                params(param("yOffset", "int", 0, "Vertical offset after sampling.")));
        schema("project_to_terrain", "Project To Terrain", "World", "points", "Projects incoming points onto the terrain heightmap.",
                params(param("yOffset", "int", 0, "Vertical offset after sampling.")));
        schema("biome_filter", "Biome Filter", "Filters", "points", "Filters points by biome.");
        schema("density_mask", "Density Mask", "Filters", "points", "Applies a noise-based density mask.",
                params(param("min", "number", 0.0D, "Minimum density."), param("max", "number", 1.0D, "Maximum density."),
                        param("noiseScale", "number", 32.0D, "Noise scale."), param("writeOnly", "bool", false, "Write density without filtering.")));
        schema("collision_prune", "Collision Prune", "Filters", "points", "Removes points colliding within a radius.",
                params(param("radius", "number", 2.0D, "Collision radius.")));
        schema("connector_chain", "Connector Chain", "Modules", "modules", "Expands a connector-based module chain.",
                params(param("startTag", "string", "room_start", "Starting module tag."), param("moduleTag", "string", "room", "Module tag."),
                        param("maxModules", "int", 16, "Maximum module count."), param("ignoreAir", "bool", true, "Ignore air blocks."),
                        param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
        schema("connect_by_socket", "Connect By Socket", "Modules", "modules", "Generic alias for connector-based module expansion.",
                params(param("startTag", "string", "room_start", "Starting module tag."), param("moduleTag", "string", "room", "Module tag."),
                        param("maxModules", "int", 16, "Maximum module count."), param("ignoreAir", "bool", true, "Ignore air blocks."),
                        param("seed", "int", 0, "Local seed."), param("seedParam", "string", "seed", "Preset seed parameter.")));
    }

    private PcgNodeSchemaRegistry() {
    }

    public static JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (String type : PcgNodeRegistry.getRegisteredTypes()) {
            JsonObject schema = SCHEMAS.get(type);
            array.add(schema == null ? fallback(type) : schema.deepCopy());
        }
        return array;
    }

    public static JsonObject get(String type) {
        JsonObject schema = SCHEMAS.get(type);
        return schema == null ? fallback(type) : schema.deepCopy();
    }

    private static void schema(String type, String displayName, String category, String outputType, String description, JsonArray parameters) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", type);
        schema.addProperty("displayName", displayName);
        schema.addProperty("category", category);
        schema.addProperty("description", description);
        JsonArray inputs = new JsonArray();
        if (!type.endsWith("_input")) {
            JsonObject input = new JsonObject();
            input.addProperty("id", "input");
            input.addProperty("dataType", "any");
            inputs.add(input);
        }
        schema.add("inputs", inputs);
        JsonArray outputs = new JsonArray();
        JsonObject output = new JsonObject();
        output.addProperty("id", "output");
        output.addProperty("dataType", outputType);
        outputs.add(output);
        schema.add("outputs", outputs);
        schema.add("parameters", parameters == null ? new JsonArray() : parameters);
        SCHEMAS.put(type, schema);
    }

    private static void schema(String type, String displayName, String category, String outputType, String description) {
        schema(type, displayName, category, outputType, description, new JsonArray());
    }

    private static JsonArray params(JsonObject... params) {
        JsonArray array = new JsonArray();
        for (JsonObject param : params) {
            array.add(param);
        }
        return array;
    }

    private static JsonObject param(String id, String type, String defaultValue, String description) {
        JsonObject object = baseParam(id, type, description);
        object.addProperty("default", defaultValue);
        return object;
    }

    private static JsonObject param(String id, String type, int defaultValue, String description) {
        JsonObject object = baseParam(id, type, description);
        object.addProperty("default", defaultValue);
        return object;
    }

    private static JsonObject param(String id, String type, double defaultValue, String description) {
        JsonObject object = baseParam(id, type, description);
        object.addProperty("default", defaultValue);
        return object;
    }

    private static JsonObject param(String id, String type, boolean defaultValue, String description) {
        JsonObject object = baseParam(id, type, description);
        object.addProperty("default", defaultValue);
        return object;
    }

    private static JsonObject baseParam(String id, String type, String description) {
        JsonObject object = new JsonObject();
        object.addProperty("id", id);
        object.addProperty("type", type);
        object.addProperty("description", description);
        return object;
    }

    private static JsonObject fallback(String type) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", type == null ? "unknown" : type);
        schema.addProperty("displayName", humanize(type));
        schema.addProperty("category", "Other");
        schema.addProperty("description", "Registered PCG node.");
        schema.add("inputs", new JsonArray());
        JsonArray outputs = new JsonArray();
        JsonObject output = new JsonObject();
        output.addProperty("id", "output");
        output.addProperty("dataType", "any");
        outputs.add(output);
        schema.add("outputs", outputs);
        schema.add("parameters", new JsonArray());
        return schema;
    }

    private static String humanize(String type) {
        if (type == null || type.isBlank()) {
            return "Unknown";
        }
        StringBuilder builder = new StringBuilder();
        for (String token : type.split("_")) {
            if (token.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
        }
        return builder.toString();
    }
}
