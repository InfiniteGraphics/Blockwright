package top.huliawsl.blockwright.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PcgGraphEditorState {
    private final Map<String, NodeView> nodes = new LinkedHashMap<>();
    private final List<EdgeView> edges = new ArrayList<>();
    private String signature = "";
    private String selectedNodeId = "";

    public void sync(LoadedPack pack, PresetDefinition preset) {
        String nextSignature = pack == null || preset == null ? "<none>" : pack.getMetadata().id + "|" + preset.id + "|" + preset.rule;
        if (nextSignature.equals(signature)) {
            return;
        }
        signature = nextSignature;
        selectedNodeId = "";
        nodes.clear();
        edges.clear();
        RuleDefinition rule = pack == null || preset == null ? null : pack.getRules().get(preset.rule);
        JsonObject graph = extractGraph(rule);
        if (graph == null) {
            return;
        }
        readNodes(graph);
        readEdges(graph);
        layoutMissingPositions();
    }

    public List<NodeView> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    public List<EdgeView> getEdges() {
        return new ArrayList<>(edges);
    }

    public NodeView getNode(String id) {
        return nodes.get(id);
    }

    public String getSelectedNodeId() {
        return selectedNodeId;
    }

    public void setSelectedNodeId(String selectedNodeId) {
        this.selectedNodeId = selectedNodeId == null ? "" : selectedNodeId;
    }

    public NodeView hitNode(double x, double y) {
        List<NodeView> list = getNodes();
        for (int i = list.size() - 1; i >= 0; i--) {
            NodeView node = list.get(i);
            if (x >= node.x && x <= node.x + node.width && y >= node.y && y <= node.y + node.height) {
                return node;
            }
        }
        return null;
    }

    private JsonObject extractGraph(RuleDefinition rule) {
        if (rule == null) {
            return null;
        }
        if (rule.config != null && rule.config.has("graph") && rule.config.get("graph").isJsonObject()) {
            return rule.config.getAsJsonObject("graph");
        }
        String legacy = switch (rule.executor == null ? "" : rule.executor) {
            case "box_building_skeleton" -> """
                    {"nodes":[{"id":"region","type":"region_input"},{"id":"shell","type":"box_building_shell"},{"id":"facade","type":"facade_modules"}],"edges":[["region","shell"],["shell","facade"]]}
                    """;
            case "spline_road_skeleton" -> """
                    {"nodes":[{"id":"spline","type":"spline_input"},{"id":"samples","type":"resample_spline"},{"id":"surface","type":"road_surface"},{"id":"lights","type":"place_modules_every_n"}],"edges":[["spline","samples"],["samples","surface"],["samples","lights"]]}
                    """;
            default -> null;
        };
        return legacy == null ? null : JsonParser.parseString(legacy).getAsJsonObject();
    }

    private void readNodes(JsonObject graph) {
        if (!graph.has("nodes") || !graph.get("nodes").isJsonArray()) {
            return;
        }
        int index = 0;
        for (JsonElement element : graph.getAsJsonArray("nodes")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "node_" + index;
            String type = object.has("type") ? object.get("type").getAsString() : "unknown";
            int x = object.has("x") ? object.get("x").getAsInt() : Integer.MIN_VALUE;
            int y = object.has("y") ? object.get("y").getAsInt() : Integer.MIN_VALUE;
            nodes.put(id, new NodeView(id, type, x, y));
            index++;
        }
    }

    private void readEdges(JsonObject graph) {
        if (!graph.has("edges") || !graph.get("edges").isJsonArray()) {
            return;
        }
        JsonArray array = graph.getAsJsonArray("edges");
        for (JsonElement element : array) {
            if (element.isJsonArray() && element.getAsJsonArray().size() >= 2) {
                edges.add(new EdgeView(element.getAsJsonArray().get(0).getAsString(), element.getAsJsonArray().get(1).getAsString()));
            } else if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has("from") && object.has("to")) {
                    edges.add(new EdgeView(object.get("from").getAsString(), object.get("to").getAsString()));
                }
            }
        }
    }

    private void layoutMissingPositions() {
        Map<String, Integer> depth = new LinkedHashMap<>();
        for (String id : nodes.keySet()) {
            depth.put(id, 0);
        }
        for (int i = 0; i < nodes.size(); i++) {
            for (EdgeView edge : edges) {
                if (depth.containsKey(edge.from) && depth.containsKey(edge.to)) {
                    depth.put(edge.to, Math.max(depth.get(edge.to), depth.get(edge.from) + 1));
                }
            }
        }
        Map<Integer, Integer> rows = new LinkedHashMap<>();
        for (NodeView node : nodes.values()) {
            if (node.x != Integer.MIN_VALUE && node.y != Integer.MIN_VALUE) {
                continue;
            }
            int column = depth.getOrDefault(node.id, 0);
            int row = rows.getOrDefault(column, 0);
            rows.put(column, row + 1);
            node.x = 48 + column * 168;
            node.y = 56 + row * 78;
        }
    }

    public static final class NodeView {
        public final String id;
        public final String type;
        public int x;
        public int y;
        public int width = 132;
        public int height = 48;

        NodeView(String id, String type, int x, int y) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    public record EdgeView(String from, String to) {
    }
}
