package top.huliawsl.blockwright.pack;

import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Blocks;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetInputDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.JsonHelper;
import top.huliawsl.blockwright.world.BlockResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DemoPackBootstrap {
    private static final String DEMO_PACK_ID = "blockwright_demo";

    private DemoPackBootstrap() {
    }

    public static void ensureDemoPack(Path presetRoot) {
        Path demoRoot = presetRoot.resolve(DEMO_PACK_ID);
        Path packJson = demoRoot.resolve("pack.json");

        try {
            Files.createDirectories(demoRoot.resolve("modules"));
            Files.createDirectories(demoRoot.resolve("presets"));
            Files.createDirectories(demoRoot.resolve("rules"));

            if (!Files.exists(packJson)) {
                Files.writeString(packJson, JsonHelper.GSON.toJson(createMetadata()));
            }
            writeIfMissing(demoRoot.resolve("presets/basic_box_building.preset.json"), JsonHelper.GSON.toJson(createBoxPreset()));
            writeIfMissing(demoRoot.resolve("presets/basic_spline_road.preset.json"), JsonHelper.GSON.toJson(createRoadPreset()));
            writeIfMissing(demoRoot.resolve("presets/graph_box_building.preset.json"), JsonHelper.GSON.toJson(createGraphBoxPreset()));
            writeIfMissing(demoRoot.resolve("presets/graph_spline_road.preset.json"), JsonHelper.GSON.toJson(createGraphRoadPreset()));
            writeIfMissing(demoRoot.resolve("presets/graph_scatter_lamps.preset.json"), JsonHelper.GSON.toJson(createGraphScatterPreset()));
            writeIfMissing(demoRoot.resolve("presets/graph_connector_rooms.preset.json"), JsonHelper.GSON.toJson(createGraphConnectorPreset()));
            writeIfMissing(demoRoot.resolve("rules/basic_box_building.rule.json"), JsonHelper.GSON.toJson(createBoxRule()));
            writeIfMissing(demoRoot.resolve("rules/basic_spline_road.rule.json"), JsonHelper.GSON.toJson(createRoadRule()));
            writeIfMissing(demoRoot.resolve("rules/graph_box_building.rule.json"), JsonHelper.GSON.toJson(createGraphBoxRule()));
            writeIfMissing(demoRoot.resolve("rules/graph_spline_road.rule.json"), JsonHelper.GSON.toJson(createGraphRoadRule()));
            writeIfMissing(demoRoot.resolve("rules/graph_scatter_lamps.rule.json"), JsonHelper.GSON.toJson(createGraphScatterRule()));
            writeIfMissing(demoRoot.resolve("rules/graph_connector_rooms.rule.json"), JsonHelper.GSON.toJson(createGraphConnectorRule()));
            writeIfMissing(demoRoot.resolve("modules/demo/window_basic.module.json"), JsonHelper.GSON.toJson(createWindowModule()));
            writeIfMissing(demoRoot.resolve("modules/demo/road_lamp_basic.module.json"), JsonHelper.GSON.toJson(createRoadLampModule()));
            writeIfMissing(demoRoot.resolve("modules/demo/room_start.module.json"), JsonHelper.GSON.toJson(createRoomStartModule()));
            writeIfMissing(demoRoot.resolve("modules/demo/corridor_basic.module.json"), JsonHelper.GSON.toJson(createCorridorModule()));
            writeDemoSchematic(demoRoot.resolve("modules/demo/window_basic.schem"), createWindowSchematic());
            writeDemoSchematic(demoRoot.resolve("modules/demo/road_lamp_basic.schem"), createRoadLampSchematic());
            writeDemoSchematic(demoRoot.resolve("modules/demo/room_start.schem"), createRoomSchematic());
            writeDemoSchematic(demoRoot.resolve("modules/demo/corridor_basic.schem"), createCorridorSchematic());
        } catch (IOException ignored) {
        }
    }

    private static PackMetadata createMetadata() {
        PackMetadata metadata = new PackMetadata();
        metadata.id = DEMO_PACK_ID;
        metadata.name = "Blockwright Demo";
        metadata.version = "0.1.0";
        metadata.author = "Blockwright";
        metadata.description = "Bootstrap demo presets for Blockwright.";
        metadata.minecraftVersions = List.of("1.20.1");
        metadata.requiredMods = List.of("minecraft");
        return metadata;
    }

    private static PresetDefinition createBoxPreset() {
        PresetDefinition preset = new PresetDefinition();
        preset.id = "basic_box_building";
        preset.name = "Basic Box Building";
        preset.type = "region_building";
        PresetInputDefinition regionInput = new PresetInputDefinition();
        regionInput.name = "region";
        regionInput.type = "box_region";
        regionInput.required = true;
        preset.inputs.add(regionInput);
        preset.parameters.put("floors", intParameter(3, 1, 20));
        preset.parameters.put("floorHeight", intParameter(4, 3, 8));
        preset.parameters.put("style", stringParameter("demo"));
        preset.parameters.put("wallBlock", stringParameter("minecraft:stone_bricks"));
        preset.parameters.put("floorBlock", stringParameter("minecraft:oak_planks"));
        preset.parameters.put("roofBlock", stringParameter("minecraft:stone"));
        preset.parameters.put("seed", longParameter(0));
        preset.rule = "rules/basic_box_building.rule.json";
        return preset;
    }

    private static PresetDefinition createRoadPreset() {
        PresetDefinition preset = new PresetDefinition();
        preset.id = "basic_spline_road";
        preset.name = "Basic Spline Road";
        preset.type = "spline_road";
        PresetInputDefinition splineInput = new PresetInputDefinition();
        splineInput.name = "spline";
        splineInput.type = "spline";
        splineInput.required = true;
        preset.inputs.add(splineInput);
        preset.parameters.put("roadWidth", intParameter(5, 1, 15));
        preset.parameters.put("style", stringParameter("demo"));
        preset.parameters.put("roadBlock", stringParameter("minecraft:stone"));
        preset.parameters.put("edgeBlock", stringParameter("minecraft:cobblestone"));
        preset.parameters.put("seed", longParameter(0));
        preset.rule = "rules/basic_spline_road.rule.json";
        return preset;
    }

    private static PresetDefinition createGraphBoxPreset() {
        PresetDefinition preset = createBoxPreset();
        preset.id = "graph_box_building";
        preset.name = "Graph Box Building";
        preset.rule = "rules/graph_box_building.rule.json";
        return preset;
    }

    private static PresetDefinition createGraphRoadPreset() {
        PresetDefinition preset = createRoadPreset();
        preset.id = "graph_spline_road";
        preset.name = "Graph Spline Road";
        preset.rule = "rules/graph_spline_road.rule.json";
        return preset;
    }

    private static PresetDefinition createGraphScatterPreset() {
        PresetDefinition preset = new PresetDefinition();
        preset.id = "graph_scatter_lamps";
        preset.name = "Graph Scatter Lamps";
        preset.type = "region_scatter";
        PresetInputDefinition regionInput = new PresetInputDefinition();
        regionInput.name = "region";
        regionInput.type = "box_region";
        regionInput.required = true;
        preset.inputs.add(regionInput);
        preset.parameters.put("style", stringParameter("demo"));
        preset.parameters.put("seed", longParameter(0));
        preset.rule = "rules/graph_scatter_lamps.rule.json";
        return preset;
    }

    private static PresetDefinition createGraphConnectorPreset() {
        PresetDefinition preset = new PresetDefinition();
        preset.id = "graph_connector_rooms";
        preset.name = "Graph Connector Rooms";
        preset.type = "point_connector";
        PresetInputDefinition splineInput = new PresetInputDefinition();
        splineInput.name = "origin";
        splineInput.type = "spline";
        splineInput.required = true;
        preset.inputs.add(splineInput);
        preset.parameters.put("style", stringParameter("demo"));
        preset.parameters.put("seed", longParameter(0));
        preset.rule = "rules/graph_connector_rooms.rule.json";
        return preset;
    }


    private static RuleDefinition createBoxRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "basic_box_building_rule";
        rule.executor = "box_building_skeleton";
        rule.config = new JsonObject();
        rule.config.addProperty("windowTag", "window");
        return rule;
    }

    private static RuleDefinition createRoadRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "basic_spline_road_rule";
        rule.executor = "spline_road_skeleton";
        rule.config = new JsonObject();
        rule.config.addProperty("roadLampTag", "road_lamp");
        return rule;
    }

    private static RuleDefinition createGraphBoxRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "graph_box_building_rule";
        rule.executor = "pcg_graph";
        rule.config = new JsonObject();
        rule.config.addProperty("windowTag", "window");
        JsonObject graph = new JsonObject();
        com.google.gson.JsonArray nodes = new com.google.gson.JsonArray();
        nodes.add(node("region", "region_input"));
        nodes.add(node("shell", "box_building_shell"));
        JsonObject windows = node("windows", "facade_modules");
        windows.addProperty("tagConfig", "windowTag");
        nodes.add(windows);
        graph.add("nodes", nodes);
        com.google.gson.JsonArray edges = new com.google.gson.JsonArray();
        edges.add(edge("region", "shell"));
        edges.add(edge("region", "windows"));
        graph.add("edges", edges);
        rule.config.add("graph", graph);
        return rule;
    }

    private static RuleDefinition createGraphRoadRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "graph_spline_road_rule";
        rule.executor = "pcg_graph";
        rule.config = new JsonObject();
        rule.config.addProperty("roadLampTag", "road_lamp");
        JsonObject graph = new JsonObject();
        com.google.gson.JsonArray nodes = new com.google.gson.JsonArray();
        nodes.add(node("spline", "spline_input"));
        JsonObject samples = node("samples", "resample_spline");
        samples.addProperty("spacing", 1.0D);
        nodes.add(samples);
        nodes.add(node("surface", "road_surface"));
        JsonObject lamps = node("lamps", "place_modules_every_n");
        lamps.addProperty("tagConfig", "roadLampTag");
        lamps.addProperty("start", 3);
        lamps.addProperty("every", 6);
        lamps.addProperty("sideMode", "random");
        nodes.add(lamps);
        graph.add("nodes", nodes);
        com.google.gson.JsonArray edges = new com.google.gson.JsonArray();
        edges.add(edge("spline", "samples"));
        edges.add(edge("samples", "surface"));
        edges.add(edge("samples", "lamps"));
        graph.add("edges", edges);
        rule.config.add("graph", graph);
        return rule;
    }

    private static RuleDefinition createGraphScatterRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "graph_scatter_lamps_rule";
        rule.executor = "pcg_graph";
        rule.config = new JsonObject();
        rule.config.addProperty("roadLampTag", "road_lamp");
        JsonObject graph = new JsonObject();
        graph.addProperty("debug", true);
        com.google.gson.JsonArray nodes = new com.google.gson.JsonArray();
        nodes.add(node("region", "region_input"));
        JsonObject scatter = node("scatter", "scatter_points");
        scatter.addProperty("count", 48);
        scatter.addProperty("spacing", 4);
        scatter.addProperty("grid", true);
        nodes.add(scatter);
        nodes.add(node("terrain", "terrain_sample"));
        JsonObject density = node("density", "density_mask");
        density.addProperty("min", 0.35D);
        density.addProperty("noiseScale", 18.0D);
        nodes.add(density);
        JsonObject prune = node("prune", "collision_prune");
        prune.addProperty("radius", 3.0D);
        nodes.add(prune);
        JsonObject place = node("place", "place_modules");
        place.addProperty("tagConfig", "roadLampTag");
        place.addProperty("followTangent", false);
        nodes.add(place);
        graph.add("nodes", nodes);
        com.google.gson.JsonArray edges = new com.google.gson.JsonArray();
        edges.add(edge("region", "scatter"));
        edges.add(edge("scatter", "terrain"));
        edges.add(edge("terrain", "density"));
        edges.add(edge("density", "prune"));
        edges.add(edge("prune", "place"));
        graph.add("edges", edges);
        rule.config.add("graph", graph);
        return rule;
    }

    private static RuleDefinition createGraphConnectorRule() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "graph_connector_rooms_rule";
        rule.executor = "pcg_graph";
        rule.config = new JsonObject();
        JsonObject graph = new JsonObject();
        graph.addProperty("debug", true);
        com.google.gson.JsonArray nodes = new com.google.gson.JsonArray();
        nodes.add(node("spline", "spline_input"));
        JsonObject chain = node("chain", "connector_chain");
        chain.addProperty("startTag", "room_start");
        chain.addProperty("moduleTag", "room_piece");
        chain.addProperty("maxModules", 8);
        nodes.add(chain);
        graph.add("nodes", nodes);
        com.google.gson.JsonArray edges = new com.google.gson.JsonArray();
        edges.add(edge("spline", "chain"));
        graph.add("edges", edges);
        rule.config.add("graph", graph);
        return rule;
    }

    private static JsonObject node(String id, String type) {
        JsonObject node = new JsonObject();
        node.addProperty("id", id);
        node.addProperty("type", type);
        return node;
    }

    private static com.google.gson.JsonArray edge(String from, String to) {
        com.google.gson.JsonArray edge = new com.google.gson.JsonArray();
        edge.add(from);
        edge.add(to);
        return edge;
    }


    private static PresetParameterDefinition intParameter(int defaultValue, int min, int max) {
        PresetParameterDefinition parameter = new PresetParameterDefinition();
        parameter.type = "int";
        parameter.defaultValue = JsonHelper.GSON.toJsonTree(defaultValue);
        parameter.min = (double) min;
        parameter.max = (double) max;
        parameter.exposed = true;
        return parameter;
    }

    private static PresetParameterDefinition longParameter(long defaultValue) {
        PresetParameterDefinition parameter = new PresetParameterDefinition();
        parameter.type = "long";
        parameter.defaultValue = JsonHelper.GSON.toJsonTree(defaultValue);
        parameter.exposed = true;
        return parameter;
    }

    private static PresetParameterDefinition stringParameter(String defaultValue) {
        PresetParameterDefinition parameter = new PresetParameterDefinition();
        parameter.type = "string";
        parameter.defaultValue = JsonHelper.GSON.toJsonTree(defaultValue);
        parameter.exposed = true;
        return parameter;
    }

    private static ModuleDefinition createWindowModule() {
        ModuleDefinition module = new ModuleDefinition();
        module.id = "demo.window_basic";
        module.moduleKind = "static_schem";
        module.sizePolicy = "fixed";
        module.placementRole = "detail";
        module.schematic = "modules/demo/window_basic.schem";
        module.category = "facade_piece";
        module.size = List.of(1, 2, 1);
        module.tags = List.of("window", "facade");
        module.style = "demo";
        module.allowedRotations = List.of(0, 90, 180, 270);
        return module;
    }

    private static ModuleDefinition createRoadLampModule() {
        ModuleDefinition module = new ModuleDefinition();
        module.id = "demo.road_lamp_basic";
        module.moduleKind = "static_schem";
        module.sizePolicy = "fixed";
        module.placementRole = "detail";
        module.schematic = "modules/demo/road_lamp_basic.schem";
        module.category = "road_detail";
        module.size = List.of(1, 3, 1);
        module.tags = List.of("road_lamp", "road");
        module.style = "demo";
        module.allowedRotations = List.of(0, 90, 180, 270);
        return module;
    }

    private static ModuleDefinition createRoomStartModule() {
        ModuleDefinition module = new ModuleDefinition();
        module.id = "demo.room_start";
        module.moduleKind = "static_schem";
        module.sizePolicy = "fixed";
        module.placementRole = "room";
        module.schematic = "modules/demo/room_start.schem";
        module.category = "connector_room";
        module.size = List.of(5, 4, 5);
        module.tags = List.of("room_start", "room_piece");
        module.style = "demo";
        module.allowedRotations = List.of(0, 90, 180, 270);
        module.connectors = List.of(
                connector("north", "door", "north", 2, 1, -1),
                connector("south", "door", "south", 2, 1, 5),
                connector("east", "door", "east", 5, 1, 2),
                connector("west", "door", "west", -1, 1, 2)
        );
        return module;
    }

    private static ModuleDefinition createCorridorModule() {
        ModuleDefinition module = new ModuleDefinition();
        module.id = "demo.corridor_basic";
        module.moduleKind = "static_schem";
        module.sizePolicy = "fixed";
        module.placementRole = "corridor";
        module.schematic = "modules/demo/corridor_basic.schem";
        module.category = "connector_corridor";
        module.size = List.of(3, 3, 5);
        module.tags = List.of("room_piece", "corridor");
        module.style = "demo";
        module.allowedRotations = List.of(0, 90, 180, 270);
        module.connectors = List.of(
                connector("north", "door", "north", 1, 1, -1),
                connector("south", "door", "south", 1, 1, 5)
        );
        return module;
    }

    private static ModuleConnector connector(String id, String type, String direction, int x, int y, int z) {
        ModuleConnector connector = new ModuleConnector();
        connector.id = id;
        connector.type = type;
        connector.direction = direction;
        connector.offset = List.of(x, y, z);
        connector.size = List.of(1, 2, 1);
        connector.tags = List.of("door");
        return connector;
    }

    private static SpongeSchematicData createWindowSchematic() {
        SpongeSchematicData data = new SpongeSchematicData(1, 2, 1, 3465, new int[] {0, 0, 0});
        data.setBlockState(0, 0, 0, BlockResolver.resolve("minecraft:glass").orElse(Blocks.GLASS.defaultBlockState()));
        data.setBlockState(0, 1, 0, BlockResolver.resolve("minecraft:glass").orElse(Blocks.GLASS.defaultBlockState()));
        data.addRequiredMod("minecraft");
        return data;
    }

    private static SpongeSchematicData createRoadLampSchematic() {
        SpongeSchematicData data = new SpongeSchematicData(1, 3, 1, 3465, new int[] {0, 0, 0});
        data.setBlockState(0, 0, 0, BlockResolver.resolve("minecraft:cobblestone_wall").orElse(Blocks.COBBLESTONE_WALL.defaultBlockState()));
        data.setBlockState(0, 1, 0, BlockResolver.resolve("minecraft:cobblestone_wall").orElse(Blocks.COBBLESTONE_WALL.defaultBlockState()));
        data.setBlockState(0, 2, 0, BlockResolver.resolve("minecraft:glowstone").orElse(Blocks.GLOWSTONE.defaultBlockState()));
        data.addRequiredMod("minecraft");
        return data;
    }

    private static SpongeSchematicData createRoomSchematic() {
        SpongeSchematicData data = new SpongeSchematicData(5, 4, 5, 3465, new int[] {0, 0, 0});
        var floor = BlockResolver.resolve("minecraft:spruce_planks").orElse(Blocks.SPRUCE_PLANKS.defaultBlockState());
        var wall = BlockResolver.resolve("minecraft:stone_bricks").orElse(Blocks.STONE_BRICKS.defaultBlockState());
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                data.setBlockState(x, 0, z, floor);
                if (x == 0 || x == 4 || z == 0 || z == 4) {
                    data.setBlockState(x, 1, z, wall);
                    data.setBlockState(x, 2, z, wall);
                }
            }
        }
        data.addRequiredMod("minecraft");
        return data;
    }

    private static SpongeSchematicData createCorridorSchematic() {
        SpongeSchematicData data = new SpongeSchematicData(3, 3, 5, 3465, new int[] {0, 0, 0});
        var floor = BlockResolver.resolve("minecraft:oak_planks").orElse(Blocks.OAK_PLANKS.defaultBlockState());
        var wall = BlockResolver.resolve("minecraft:cobblestone").orElse(Blocks.COBBLESTONE.defaultBlockState());
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 5; z++) {
                data.setBlockState(x, 0, z, floor);
                if (x == 0 || x == 2) {
                    data.setBlockState(x, 1, z, wall);
                }
            }
        }
        data.addRequiredMod("minecraft");
        return data;
    }

    private static void writeIfMissing(Path path, String contents) throws IOException {
        if (Files.exists(path)) {
            return;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
    }

    private static void writeDemoSchematic(Path path, SpongeSchematicData data) {
        if (Files.exists(path)) {
            return;
        }
        try {
            SpongeSchematicWriter.write(path, data, path.getFileName().toString(), "Blockwright");
        } catch (Exception ignored) {
        }
    }
}
