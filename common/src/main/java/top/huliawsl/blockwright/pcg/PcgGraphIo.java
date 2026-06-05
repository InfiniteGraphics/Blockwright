package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.JsonHelper;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PcgGraphIo {
    private PcgGraphIo() {
    }

    public static JsonObject loadGraphObject(LoadedPack pack, RuleDefinition rule, PreviewPlan plan) {
        if (rule == null) {
            addIssue(plan, "PCG graph rule is missing.");
            return null;
        }
        JsonObject config = rule.config;
        if (config != null && config.has("graph") && config.get("graph").isJsonObject()) {
            return config.getAsJsonObject("graph").deepCopy();
        }
        if (config != null && config.has("graphFile") && config.get("graphFile").isJsonPrimitive()) {
            if (pack == null) {
                addIssue(plan, "PCG graphFile requires a loaded pack.");
                return null;
            }
            Path graphPath = pack.getRoot().resolve(config.get("graphFile").getAsString()).normalize();
            if (!graphPath.startsWith(pack.getRoot().normalize())) {
                addIssue(plan, "PCG graphFile escapes pack root: " + graphPath);
                return null;
            }
            if (!Files.exists(graphPath)) {
                addIssue(plan, "PCG graphFile was not found: " + config.get("graphFile").getAsString());
                return null;
            }
            try (Reader reader = Files.newBufferedReader(graphPath)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
                addIssue(plan, "PCG graphFile root must be an object: " + config.get("graphFile").getAsString());
            } catch (Exception exception) {
                addIssue(plan, "Failed to read PCG graphFile: " + exception.getMessage());
            }
            return null;
        }
        JsonObject legacyGraph = defaultGraphForExecutor(rule.executor);
        if (legacyGraph != null) {
            return legacyGraph;
        }
        addIssue(plan, "PCG graph rule requires either config.graph or config.graphFile.");
        return null;
    }

    public static JsonObject defaultGraphForExecutor(String executor) {
        String graphJson = switch (executor == null ? "" : executor) {
            case "box_building_skeleton" -> """
                    {
                      \"debug\": false,
                      \"nodes\": [
                        {\"id\":\"region\",\"type\":\"region_input\",\"x\":64,\"y\":80},
                        {\"id\":\"shell\",\"type\":\"box_building_shell\",\"input\":\"region\",\"x\":288,\"y\":80},
                        {\"id\":\"facade\",\"type\":\"facade_modules\",\"input\":\"shell\",\"tagConfig\":\"windowTag\",\"x\":512,\"y\":80}
                      ],
                      \"edges\": [[\"region\",\"shell\"],[\"shell\",\"facade\"]]
                    }
                    """;
            case "spline_road_skeleton" -> """
                    {
                      \"debug\": false,
                      \"nodes\": [
                        {\"id\":\"spline\",\"type\":\"spline_input\",\"x\":64,\"y\":80},
                        {\"id\":\"samples\",\"type\":\"resample_spline\",\"input\":\"spline\",\"spacing\":1.0,\"x\":288,\"y\":80},
                        {\"id\":\"surface\",\"type\":\"road_surface\",\"input\":\"samples\",\"x\":512,\"y\":32},
                        {\"id\":\"lights\",\"type\":\"place_modules_every_n\",\"input\":\"samples\",\"tagConfig\":\"roadLampTag\",\"every\":6,\"sideMode\":\"alternate\",\"x\":512,\"y\":184}
                      ],
                      \"edges\": [[\"spline\",\"samples\"],[\"samples\",\"surface\"],[\"samples\",\"lights\"]]
                    }
                    """;
            default -> null;
        };
        if (graphJson == null) {
            return null;
        }
        return JsonParser.parseString(graphJson).getAsJsonObject();
    }

    public static boolean saveGraphObject(LoadedPack pack, RuleDefinition rule, JsonObject graph) throws Exception {
        if (pack == null || rule == null || graph == null) {
            return false;
        }
        JsonObject config = rule.config == null ? new JsonObject() : rule.config;
        if (config.has("graphFile") && config.get("graphFile").isJsonPrimitive()) {
            Path graphPath = pack.getRoot().resolve(config.get("graphFile").getAsString()).normalize();
            if (!graphPath.startsWith(pack.getRoot().normalize())) {
                throw new IllegalArgumentException("graphFile escapes pack root: " + config.get("graphFile").getAsString());
            }
            Path parent = graphPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(graphPath, JsonHelper.GSON.toJson(graph));
            return true;
        }
        if (rule.sourcePath == null || !Files.exists(rule.sourcePath)) {
            return false;
        }
        JsonObject ruleJson;
        try (Reader reader = Files.newBufferedReader(rule.sourcePath)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                return false;
            }
            ruleJson = element.getAsJsonObject();
        }
        JsonObject ruleConfig = ruleJson.has("config") && ruleJson.get("config").isJsonObject()
                ? ruleJson.getAsJsonObject("config")
                : new JsonObject();
        ruleConfig.add("graph", graph.deepCopy());
        ruleJson.add("config", ruleConfig);
        Files.writeString(rule.sourcePath, JsonHelper.GSON.toJson(ruleJson));
        return true;
    }

    private static void addIssue(PreviewPlan plan, String message) {
        if (plan != null) {
            plan.addIssue(PreviewSeverity.ERROR, message);
        }
    }
}
