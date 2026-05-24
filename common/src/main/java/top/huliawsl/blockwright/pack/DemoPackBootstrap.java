package top.huliawsl.blockwright.pack;

import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Blocks;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
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
            writeIfMissing(demoRoot.resolve("rules/basic_box_building.rule.json"), JsonHelper.GSON.toJson(createBoxRule()));
            writeIfMissing(demoRoot.resolve("rules/basic_spline_road.rule.json"), JsonHelper.GSON.toJson(createRoadRule()));
            writeIfMissing(demoRoot.resolve("modules/demo/window_basic.module.json"), JsonHelper.GSON.toJson(createWindowModule()));
            writeIfMissing(demoRoot.resolve("modules/demo/road_lamp_basic.module.json"), JsonHelper.GSON.toJson(createRoadLampModule()));
            writeDemoSchematic(demoRoot.resolve("modules/demo/window_basic.schem"), createWindowSchematic());
            writeDemoSchematic(demoRoot.resolve("modules/demo/road_lamp_basic.schem"), createRoadLampSchematic());
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
