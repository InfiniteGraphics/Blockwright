package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

public final class AttributeSetNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        JsonObject attributes = node.getConfig().has("attributes") && node.getConfig().get("attributes").isJsonObject()
                ? node.getConfig().getAsJsonObject("attributes")
                : new JsonObject();
        String attributeKey = context.getNodeString(node, "attribute", "");
        JsonElement configuredValue = attributeKey.isBlank() ? null : PcgNodeUtil.configuredAttributeValue(context, node);
        double density = context.getNodeDouble(node, "density", -1.0D);
        List<PcgPoint> points = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            Map<String, JsonElement> next = new LinkedHashMap<>(point.getAttributes());
            for (Map.Entry<String, JsonElement> entry : attributes.entrySet()) {
                next.put(entry.getKey(), resolveAttributeValue(context, point, entry.getValue()));
            }
            if (configuredValue != null) {
                next.put(attributeKey, configuredValue);
            }
            PcgPoint updated = point.withAttributes(next);
            if (density >= 0.0D) {
                updated = updated.withDensity(density);
            }
            points.add(updated);
        }
        List<PcgVolume> volumes = new ArrayList<>();
        for (PcgVolume volume : input.getVolumes()) {
            Map<String, JsonElement> next = new LinkedHashMap<>(volume.getAttributes());
            for (Map.Entry<String, JsonElement> entry : attributes.entrySet()) {
                next.put(entry.getKey(), resolveAttributeValue(context, volume, entry.getValue()));
            }
            if (configuredValue != null) {
                next.put(attributeKey, configuredValue);
            }
            volumes.add(volume.withAttributes(next));
        }
        return new PcgData(points, volumes);
    }

    private JsonElement resolveAttributeValue(PcgGraphContext context, PcgPoint point, JsonElement raw) {
        JsonElement resolved = PcgNodeUtil.resolveValue(context, raw);
        if (resolved != null && resolved.isJsonPrimitive() && resolved.getAsJsonPrimitive().isString()) {
            String value = resolved.getAsString();
            if (value.contains("${")) {
                return new com.google.gson.JsonPrimitive(PcgNodeUtil.interpolatePoint(context, value, point));
            }
        }
        return resolved;
    }

    private JsonElement resolveAttributeValue(PcgGraphContext context, PcgVolume volume, JsonElement raw) {
        JsonElement resolved = PcgNodeUtil.resolveValue(context, raw);
        if (resolved != null && resolved.isJsonPrimitive() && resolved.getAsJsonPrimitive().isString()) {
            String value = resolved.getAsString();
            if (value.contains("${")) {
                return new com.google.gson.JsonPrimitive(PcgNodeUtil.interpolateVolume(context, value, volume));
            }
        }
        return resolved;
    }
}
