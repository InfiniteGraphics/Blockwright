package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.PresetExecutor;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PcgGraphExecutor implements PresetExecutor {
    @Override
    public PreviewPlan execute(PresetExecutionContext context) {
        PreviewPlan plan = new PreviewPlan(context.getPreset().id);
        JsonObject graphObject = loadGraphObject(context, plan);
        if (graphObject == null) {
            return plan;
        }

        PcgGraphDefinition graph = PcgGraphDefinition.fromJson(graphObject);
        if (graph.getNodes().isEmpty()) {
            plan.addIssue(PreviewSeverity.ERROR, "PCG graph has no nodes.");
            return plan;
        }

        PcgGraphContext graphContext = new PcgGraphContext(context, plan);
        graphContext.setDebugEnabled(resolveDebugFlag(context, graphObject));
        executeGraph(graphContext, graph);
        return plan;
    }

    private JsonObject loadGraphObject(PresetExecutionContext context, PreviewPlan plan) {
        JsonObject config = context.getRule().config;
        if (config == null) {
            JsonObject legacyGraph = defaultGraphForExecutor(context.getRule().executor);
            if (legacyGraph != null) {
                return legacyGraph;
            }
            plan.addIssue(PreviewSeverity.ERROR, "PCG graph rule config is missing.");
            return null;
        }
        if (config.has("graph") && config.get("graph").isJsonObject()) {
            return config.getAsJsonObject("graph");
        }
        if (config.has("graphFile") && config.get("graphFile").isJsonPrimitive()) {
            Path graphPath = context.getPack().getRoot().resolve(config.get("graphFile").getAsString()).normalize();
            if (!graphPath.startsWith(context.getPack().getRoot().normalize())) {
                plan.addIssue(PreviewSeverity.ERROR, "PCG graphFile escapes pack root: " + graphPath);
                return null;
            }
            if (!Files.exists(graphPath)) {
                plan.addIssue(PreviewSeverity.ERROR, "PCG graphFile was not found: " + config.get("graphFile").getAsString());
                return null;
            }
            try (Reader reader = Files.newBufferedReader(graphPath)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
                plan.addIssue(PreviewSeverity.ERROR, "PCG graphFile root must be an object: " + config.get("graphFile").getAsString());
            } catch (Exception exception) {
                plan.addIssue(PreviewSeverity.ERROR, "Failed to read PCG graphFile: " + exception.getMessage());
            }
            return null;
        }
        JsonObject legacyGraph = defaultGraphForExecutor(context.getRule().executor);
        if (legacyGraph != null) {
            return legacyGraph;
        }
        plan.addIssue(PreviewSeverity.ERROR, "PCG graph rule requires either config.graph or config.graphFile.");
        return null;
    }

    private JsonObject defaultGraphForExecutor(String executor) {
        String graphJson = switch (executor == null ? "" : executor) {
            case "box_building_skeleton" -> """
                    {
                      \"debug\": false,
                      \"nodes\": [
                        {\"id\":\"region\",\"type\":\"region_input\"},
                        {\"id\":\"shell\",\"type\":\"box_building_shell\",\"input\":\"region\"},
                        {\"id\":\"facade\",\"type\":\"facade_modules\",\"input\":\"shell\",\"tagConfig\":\"windowTag\"}
                      ],
                      \"edges\": [[\"region\",\"shell\"],[\"shell\",\"facade\"]]
                    }
                    """;
            case "spline_road_skeleton" -> """
                    {
                      \"debug\": false,
                      \"nodes\": [
                        {\"id\":\"spline\",\"type\":\"spline_input\"},
                        {\"id\":\"samples\",\"type\":\"resample_spline\",\"input\":\"spline\",\"spacing\":1.0},
                        {\"id\":\"surface\",\"type\":\"road_surface\",\"input\":\"samples\"},
                        {\"id\":\"lights\",\"type\":\"place_modules_every_n\",\"input\":\"samples\",\"tagConfig\":\"roadLampTag\",\"every\":6,\"sideMode\":\"alternate\"}
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

    private boolean resolveDebugFlag(PresetExecutionContext context, JsonObject graphObject) {
        if (context.getOverrides().containsKey("debug")) {
            return Boolean.parseBoolean(context.getOverrides().get("debug"));
        }
        JsonObject config = context.getRule().config;
        if (config != null && config.has("debug") && config.get("debug").isJsonPrimitive()) {
            try {
                return config.get("debug").getAsBoolean();
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        if (graphObject != null && graphObject.has("debug") && graphObject.get("debug").isJsonPrimitive()) {
            try {
                return graphObject.get("debug").getAsBoolean();
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private void executeGraph(PcgGraphContext context, PcgGraphDefinition graph) {
        Map<String, PcgNodeDefinition> nodesById = new LinkedHashMap<>();
        for (PcgNodeDefinition node : graph.getNodes()) {
            if (node.getId().isBlank()) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG graph contains a node without id.");
                return;
            }
            if (nodesById.containsKey(node.getId())) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG graph contains duplicate node id: " + node.getId());
                return;
            }
            nodesById.put(node.getId(), node);
        }

        Map<String, List<String>> incoming = buildIncoming(nodesById, graph.getEdges(), context);
        if (context.getPlan().getOverallSeverity() == PreviewSeverity.ERROR) {
            return;
        }

        List<PcgNodeDefinition> orderedNodes = topologicalOrder(nodesById, graph.getEdges(), context);
        if (orderedNodes.isEmpty()) {
            return;
        }

        Map<String, PcgData> outputs = new LinkedHashMap<>();
        for (PcgNodeDefinition nodeDefinition : orderedNodes) {
            PcgNode node = PcgNodeRegistry.get(nodeDefinition.getType()).orElse(null);
            if (node == null) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "Unknown PCG node type: " + nodeDefinition.getType());
                return;
            }
            Map<String, PcgData> nodeInputs = new LinkedHashMap<>();
            for (String sourceId : incoming.getOrDefault(nodeDefinition.getId(), List.of())) {
                nodeInputs.put(sourceId, outputs.getOrDefault(sourceId, PcgData.empty()));
            }
            String explicitInput = context.getNodeString(nodeDefinition, "input", "");
            if (!explicitInput.isBlank()) {
                nodeInputs.put(explicitInput, outputs.getOrDefault(explicitInput, PcgData.empty()));
            }
            PcgData output = node.execute(context, nodeDefinition, nodeInputs);
            output = output == null ? PcgData.empty() : output;
            outputs.put(nodeDefinition.getId(), output);
            context.putNodeOutput(nodeDefinition.getId(), output);
            PcgGraphDebugRecorder.record(context, nodeDefinition, output);
            if (context.getPlan().getOverallSeverity() == PreviewSeverity.ERROR) {
                return;
            }
        }
    }

    private Map<String, List<String>> buildIncoming(Map<String, PcgNodeDefinition> nodesById, List<PcgEdge> edges,
                                                    PcgGraphContext context) {
        Map<String, List<String>> incoming = new HashMap<>();
        for (PcgEdge edge : edges) {
            if (!nodesById.containsKey(edge.getFrom())) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG edge references unknown source node: " + edge.getFrom());
                return incoming;
            }
            if (!nodesById.containsKey(edge.getTo())) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG edge references unknown target node: " + edge.getTo());
                return incoming;
            }
            incoming.computeIfAbsent(edge.getTo(), ignored -> new ArrayList<>()).add(edge.getFrom());
        }
        return incoming;
    }

    private List<PcgNodeDefinition> topologicalOrder(Map<String, PcgNodeDefinition> nodesById, List<PcgEdge> edges,
                                                     PcgGraphContext context) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        Map<String, List<String>> outgoing = new LinkedHashMap<>();
        for (String id : nodesById.keySet()) {
            indegree.put(id, 0);
        }
        for (PcgEdge edge : edges) {
            outgoing.computeIfAbsent(edge.getFrom(), ignored -> new ArrayList<>()).add(edge.getTo());
            indegree.put(edge.getTo(), indegree.getOrDefault(edge.getTo(), 0) + 1);
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<PcgNodeDefinition> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (!visited.add(id)) {
                continue;
            }
            result.add(nodesById.get(id));
            for (String target : outgoing.getOrDefault(id, List.of())) {
                int nextIndegree = indegree.get(target) - 1;
                indegree.put(target, nextIndegree);
                if (nextIndegree == 0) {
                    queue.add(target);
                }
            }
        }

        if (result.size() != nodesById.size()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG graph contains a cycle.");
            return List.of();
        }
        return result;
    }
}
