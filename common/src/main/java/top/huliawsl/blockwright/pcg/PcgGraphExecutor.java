package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonObject;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewNodeSummary;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.PresetExecutor;

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
        JsonObject graphObject = PcgGraphIo.loadGraphObject(context.getPack(), context.getRule(), plan);
        if (graphObject == null) {
            return plan;
        }
        return execute(context, graphObject, plan);
    }

    public PreviewPlan execute(PresetExecutionContext context, JsonObject graphObject) {
        PreviewPlan plan = new PreviewPlan(context.getPreset().id);
        if (graphObject == null) {
            plan.addIssue(PreviewSeverity.ERROR, "PCG graph payload is missing.");
            return plan;
        }
        return execute(context, graphObject, plan);
    }

    private PreviewPlan execute(PresetExecutionContext context, JsonObject graphObject, PreviewPlan plan) {
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

        Map<String, List<PcgEdge>> incoming = buildIncoming(nodesById, graph.getEdges(), context);
        if (context.getPlan().getOverallSeverity() == PreviewSeverity.ERROR) {
            return;
        }

        List<PcgNodeDefinition> orderedNodes = topologicalOrder(nodesById, graph.getEdges(), context);
        if (orderedNodes.isEmpty()) {
            return;
        }

        Map<String, PcgData> outputs = new LinkedHashMap<>();
        for (int order = 0; order < orderedNodes.size(); order++) {
            PcgNodeDefinition nodeDefinition = orderedNodes.get(order);
            PcgNode node = PcgNodeRegistry.get(nodeDefinition.getType()).orElse(null);
            if (node == null) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "Unknown PCG node type: " + nodeDefinition.getType());
                return;
            }
            Map<String, PcgData> nodeInputs = new LinkedHashMap<>();
            for (PcgEdge edge : incoming.getOrDefault(nodeDefinition.getId(), List.of())) {
                String inputKey = edge.getToPort();
                if (nodeInputs.containsKey(inputKey)) {
                    inputKey = edge.getFrom() + "." + edge.getFromPort() + "->" + edge.getToPort();
                }
                nodeInputs.put(inputKey, outputs.getOrDefault(edge.getFrom(), PcgData.empty()));
            }
            String explicitInput = context.getNodeString(nodeDefinition, "input", "");
            if (!explicitInput.isBlank()) {
                nodeInputs.put(explicitInput, outputs.getOrDefault(explicitInput, PcgData.empty()));
            }
            int blockCountBefore = context.getPlan().getPlannedBlocks().size();
            PcgData output = node.execute(context, nodeDefinition, nodeInputs);
            output = output == null ? PcgData.empty() : output;
            outputs.put(nodeDefinition.getId(), output);
            context.putNodeOutput(nodeDefinition.getId(), output);
            int plannedBlockDelta = context.getPlan().getPlannedBlocks().size() - blockCountBefore;
            context.getPlan().addNodeSummary(new PreviewNodeSummary(
                    nodeDefinition.getId(),
                    nodeDefinition.getType(),
                    order,
                    nodeInputs.size(),
                    output.getPoints().size(),
                    output.getVolumes().size(),
                    plannedBlockDelta
            ));
            PcgGraphDebugRecorder.record(context, nodeDefinition, output);
            if (context.getPlan().getOverallSeverity() == PreviewSeverity.ERROR) {
                return;
            }
        }
    }

    private Map<String, List<PcgEdge>> buildIncoming(Map<String, PcgNodeDefinition> nodesById, List<PcgEdge> edges,
                                                     PcgGraphContext context) {
        Map<String, List<PcgEdge>> incoming = new HashMap<>();
        for (PcgEdge edge : edges) {
            if (!nodesById.containsKey(edge.getFrom())) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG edge references unknown source node: " + edge.getFrom());
                return incoming;
            }
            if (!nodesById.containsKey(edge.getTo())) {
                context.getPlan().addIssue(PreviewSeverity.ERROR, "PCG edge references unknown target node: " + edge.getTo());
                return incoming;
            }
            incoming.computeIfAbsent(edge.getTo(), ignored -> new ArrayList<>()).add(edge);
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
