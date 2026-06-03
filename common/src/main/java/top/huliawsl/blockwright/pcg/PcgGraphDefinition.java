package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PcgGraphDefinition {
    private final List<PcgNodeDefinition> nodes;
    private final List<PcgEdge> edges;

    public PcgGraphDefinition(List<PcgNodeDefinition> nodes, List<PcgEdge> edges) {
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes == null ? List.of() : nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges == null ? List.of() : edges));
    }

    public static PcgGraphDefinition fromJson(JsonObject object) {
        List<PcgNodeDefinition> nodes = new ArrayList<>();
        if (object.has("nodes") && object.get("nodes").isJsonArray()) {
            JsonArray nodeArray = object.getAsJsonArray("nodes");
            for (JsonElement nodeElement : nodeArray) {
                if (nodeElement.isJsonObject()) {
                    nodes.add(PcgNodeDefinition.fromJson(nodeElement.getAsJsonObject()));
                }
            }
        }

        List<PcgEdge> edges = new ArrayList<>();
        if (object.has("edges") && object.get("edges").isJsonArray()) {
            JsonArray edgeArray = object.getAsJsonArray("edges");
            for (JsonElement edgeElement : edgeArray) {
                if (edgeElement.isJsonArray()) {
                    edges.add(PcgEdge.fromArray(edgeElement.getAsJsonArray()));
                } else if (edgeElement.isJsonObject()) {
                    edges.add(PcgEdge.fromObject(edgeElement.getAsJsonObject()));
                }
            }
        }
        return new PcgGraphDefinition(nodes, edges);
    }

    public List<PcgNodeDefinition> getNodes() {
        return nodes;
    }

    public List<PcgEdge> getEdges() {
        return edges;
    }
}
