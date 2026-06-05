package top.huliawsl.blockwright.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetInputDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.JsonHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class PackAuthoringTemplates {
    public enum PresetTemplateKind {
        REGION_GRAPH("Region Graph"),
        SPLINE_GRAPH("Spline Graph");

        private final String label;

        PresetTemplateKind(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private PackAuthoringTemplates() {
    }

    public static void createPack(Path packRoot, String packId, String packName) throws IOException {
        Path packDirectory = packRoot.resolve(packId);
        Path packJson = packDirectory.resolve("pack.json");
        if (Files.exists(packJson)) {
            throw new IOException("Pack already exists: " + packId);
        }
        Files.createDirectories(packDirectory.resolve("modules"));
        Files.createDirectories(packDirectory.resolve("presets"));
        Files.createDirectories(packDirectory.resolve("rules"));
        writeJson(packJson, createMetadata(packId, packName));
    }

    public static void createPreset(Path packRoot, String packId, String presetId, String presetName, PresetTemplateKind templateKind) throws IOException {
        Path packDirectory = packRoot.resolve(packId);
        Path packJson = packDirectory.resolve("pack.json");
        if (!Files.exists(packJson)) {
            throw new IOException("Pack does not exist: " + packId);
        }
        Files.createDirectories(packDirectory.resolve("presets"));
        Files.createDirectories(packDirectory.resolve("rules"));

        Path presetPath = packDirectory.resolve("presets").resolve(presetId + ".preset.json");
        Path rulePath = packDirectory.resolve("rules").resolve(presetId + ".rule.json");
        if (Files.exists(presetPath) || Files.exists(rulePath)) {
            throw new IOException("Preset already exists: " + presetId);
        }

        writeJson(presetPath, createPresetDefinition(presetId, presetName, templateKind));
        writeJson(rulePath, createRuleDefinition(presetId, templateKind));
    }

    private static PackMetadata createMetadata(String packId, String packName) {
        PackMetadata metadata = new PackMetadata();
        metadata.id = packId;
        metadata.name = packName == null || packName.isBlank() ? humanizeId(packId) : packName.trim();
        metadata.version = "0.1.0";
        metadata.author = "Player";
        metadata.description = "Player-created Blockwright pack.";
        metadata.minecraftVersions = List.of("1.20.1");
        metadata.requiredMods = List.of("minecraft");
        return metadata;
    }

    private static PresetDefinition createPresetDefinition(String presetId, String presetName, PresetTemplateKind templateKind) {
        PresetDefinition preset = new PresetDefinition();
        preset.id = presetId;
        preset.name = presetName == null || presetName.isBlank() ? humanizeId(presetId) : presetName.trim();
        preset.type = templateKind == PresetTemplateKind.SPLINE_GRAPH ? "spline_graph" : "region_graph";
        preset.inputs.add(input(templateKind == PresetTemplateKind.SPLINE_GRAPH ? "spline" : "region",
                templateKind == PresetTemplateKind.SPLINE_GRAPH ? "spline" : "box_region"));
        preset.parameters.put("style", stringParameter("custom"));
        preset.parameters.put("seed", longParameter(0L));
        preset.rule = "rules/" + presetId + ".rule.json";
        return preset;
    }

    private static RuleDefinition createRuleDefinition(String presetId, PresetTemplateKind templateKind) {
        RuleDefinition rule = new RuleDefinition();
        rule.id = presetId + "_rule";
        rule.executor = "pcg_graph";
        rule.config = new JsonObject();
        rule.config.add("graph", templateKind == PresetTemplateKind.SPLINE_GRAPH ? createSplineGraph() : createRegionGraph());
        return rule;
    }

    private static JsonObject createRegionGraph() {
        JsonObject graph = new JsonObject();
        JsonArray nodes = new JsonArray();
        nodes.add(node("region", "region_input", 80, 120));
        JsonObject boundary = node("boundary", "volume_boundary", 320, 120);
        boundary.addProperty("faces", "sides");
        boundary.addProperty("horizontalStep", 4);
        boundary.addProperty("verticalStep", 4);
        nodes.add(boundary);
        graph.add("nodes", nodes);
        JsonArray edges = new JsonArray();
        edges.add(edge("region", "boundary"));
        graph.add("edges", edges);
        return graph;
    }

    private static JsonObject createSplineGraph() {
        JsonObject graph = new JsonObject();
        JsonArray nodes = new JsonArray();
        nodes.add(node("spline", "spline_input", 80, 120));
        JsonObject samples = node("samples", "sample_curve", 320, 120);
        samples.addProperty("spacing", 4.0D);
        nodes.add(samples);
        graph.add("nodes", nodes);
        JsonArray edges = new JsonArray();
        edges.add(edge("spline", "samples"));
        graph.add("edges", edges);
        return graph;
    }

    private static JsonObject node(String id, String type, int x, int y) {
        JsonObject node = new JsonObject();
        node.addProperty("id", id);
        node.addProperty("type", type);
        node.addProperty("x", x);
        node.addProperty("y", y);
        return node;
    }

    private static JsonArray edge(String from, String to) {
        JsonArray edge = new JsonArray();
        edge.add(from);
        edge.add(to);
        return edge;
    }

    private static PresetInputDefinition input(String name, String type) {
        PresetInputDefinition input = new PresetInputDefinition();
        input.name = name;
        input.type = type;
        input.required = true;
        return input;
    }

    private static PresetParameterDefinition stringParameter(String defaultValue) {
        PresetParameterDefinition parameter = new PresetParameterDefinition();
        parameter.type = "string";
        parameter.defaultValue = new JsonPrimitive(defaultValue);
        parameter.exposed = true;
        return parameter;
    }

    private static PresetParameterDefinition longParameter(long defaultValue) {
        PresetParameterDefinition parameter = new PresetParameterDefinition();
        parameter.type = "long";
        parameter.defaultValue = new JsonPrimitive(defaultValue);
        parameter.exposed = true;
        return parameter;
    }

    private static String humanizeId(String id) {
        if (id == null || id.isBlank()) {
            return "Untitled";
        }
        String[] parts = id.replace('.', '_').replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder(id.length());
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.length() == 0 ? id : builder.toString();
    }

    private static void writeJson(Path path, Object value) throws IOException {
        Files.writeString(path, JsonHelper.GSON.toJson(value), StandardOpenOption.CREATE_NEW);
    }
}
