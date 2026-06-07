package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AttributeExpressionNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        JsonObject expressions = node.getConfig().has("expressions") && node.getConfig().get("expressions").isJsonObject()
                ? node.getConfig().getAsJsonObject("expressions")
                : new JsonObject();
        String attribute = PcgNodeUtil.resolveConfigString(context, node, "attribute", "");
        String expression = PcgNodeUtil.resolveConfigString(context, node, "expression", "");
        String target = PcgNodeUtil.resolveConfigString(context, node, "target", "both");
        List<PcgPoint> points = new ArrayList<>();
        if (target.equalsIgnoreCase("points") || target.equalsIgnoreCase("both")) {
            for (PcgPoint point : input.getPoints()) {
                Map<String, JsonElement> next = new LinkedHashMap<>(point.getAttributes());
                applyExpressions(context, expressions, next, point, null);
                if (!attribute.isBlank() && !expression.isBlank()) {
                    next.put(attribute, PcgExpressionUtil.evaluateToJson(context, expression, point, null, new JsonPrimitive(0)));
                }
                points.add(point.withAttributes(next));
            }
        } else {
            points.addAll(input.getPoints());
        }
        List<PcgVolume> volumes = new ArrayList<>();
        if (target.equalsIgnoreCase("volumes") || target.equalsIgnoreCase("both")) {
            for (PcgVolume volume : input.getVolumes()) {
                Map<String, JsonElement> next = new LinkedHashMap<>(volume.getAttributes());
                applyExpressions(context, expressions, next, null, volume);
                if (!attribute.isBlank() && !expression.isBlank()) {
                    next.put(attribute, PcgExpressionUtil.evaluateToJson(context, expression, null, volume, new JsonPrimitive(0)));
                }
                volumes.add(volume.withAttributes(next));
            }
        } else {
            volumes.addAll(input.getVolumes());
        }
        return new PcgData(points, volumes);
    }

    private void applyExpressions(PcgGraphContext context, JsonObject expressions, Map<String, JsonElement> attributes,
                                  PcgPoint point, PcgVolume volume) {
        for (Map.Entry<String, JsonElement> entry : expressions.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }
            attributes.put(entry.getKey(), PcgExpressionUtil.evaluateToJson(context, entry.getValue().getAsString(), point, volume, new JsonPrimitive(0)));
        }
    }
}
