package top.huliawsl.blockwright.pack;

import com.google.gson.JsonObject;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetInputDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.JsonHelper;

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
        if (Files.exists(packJson)) {
            return;
        }

        try {
            Files.createDirectories(demoRoot.resolve("modules"));
            Files.createDirectories(demoRoot.resolve("presets"));
            Files.createDirectories(demoRoot.resolve("rules"));

            Files.writeString(packJson, JsonHelper.GSON.toJson(createMetadata()));
            Files.writeString(demoRoot.resolve("presets/basic_box_building.preset.json"), JsonHelper.GSON.toJson(createBoxPreset()));
            Files.writeString(demoRoot.resolve("presets/basic_spline_road.preset.json"), JsonHelper.GSON.toJson(createRoadPreset()));
            Files.writeString(demoRoot.resolve("rules/basic_box_building.rule.json"), JsonHelper.GSON.toJson(createRule("basic_box_building_rule", "box_building_skeleton")));
            Files.writeString(demoRoot.resolve("rules/basic_spline_road.rule.json"), JsonHelper.GSON.toJson(createRule("basic_spline_road_rule", "spline_road_skeleton")));
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
        preset.parameters.put("roadBlock", stringParameter("minecraft:stone"));
        preset.parameters.put("edgeBlock", stringParameter("minecraft:cobblestone"));
        preset.parameters.put("seed", longParameter(0));
        preset.rule = "rules/basic_spline_road.rule.json";
        return preset;
    }

    private static RuleDefinition createRule(String id, String executor) {
        RuleDefinition rule = new RuleDefinition();
        rule.id = id;
        rule.executor = executor;
        rule.config = new JsonObject();
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
}
