package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AttributeRandomNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        String attribute = PcgNodeUtil.resolveConfigString(context, node, "attribute", "variant");
        if (attribute.isBlank()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "attribute_random requires a non-empty attribute key.");
            return input;
        }
        JsonArray values = node.getConfig().has("values") && node.getConfig().get("values").isJsonArray()
                ? node.getConfig().getAsJsonArray("values")
                : new JsonArray();
        if (values.size() == 0) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "attribute_random has no config.values entries; passing data through.");
            return input;
        }
        long seed = PcgNodeUtil.resolveConfigLong(context, node, "seed",
                context.getLongParameter(PcgNodeUtil.resolveConfigString(context, node, "seedParam", "seed"), 0L));

        List<PcgPoint> points = new ArrayList<>();
        int index = 0;
        for (PcgPoint point : input.getPoints()) {
            JsonElement picked = pickValue(context, values, seed == 0L ? point.getSeed() : seed, index++);
            Map<String, JsonElement> attributes = new LinkedHashMap<>(point.getAttributes());
            attributes.put(attribute, resolveForPoint(context, picked, point));
            points.add(point.withAttributes(attributes));
        }

        List<PcgVolume> volumes = new ArrayList<>();
        for (PcgVolume volume : input.getVolumes()) {
            JsonElement picked = pickValue(context, values, seed == 0L ? volume.hashCode() : seed, index++);
            Map<String, JsonElement> attributes = new LinkedHashMap<>(volume.getAttributes());
            attributes.put(attribute, resolveForVolume(context, picked, volume));
            volumes.add(volume.withAttributes(attributes));
        }
        return new PcgData(points, volumes);
    }

    private JsonElement pickValue(PcgGraphContext context, JsonArray values, long seed, int index) {
        double total = 0.0D;
        List<Entry> entries = new ArrayList<>();
        for (JsonElement value : values) {
            JsonElement actualValue = value;
            double weight = 1.0D;
            if (value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                if (object.has("value")) {
                    actualValue = object.get("value");
                }
                if (object.has("weight") && object.get("weight").isJsonPrimitive()) {
                    try {
                        weight = Math.max(0.0D, object.get("weight").getAsDouble());
                    } catch (RuntimeException ignored) {
                        weight = 1.0D;
                    }
                }
            }
            if (weight <= 0.0D) {
                continue;
            }
            total += weight;
            entries.add(new Entry(actualValue, total));
        }
        if (entries.isEmpty()) {
            return values.get(0);
        }
        RandomSource random = RandomSource.create(PcgNodeUtil.mixSeed(seed, index));
        double pick = random.nextDouble() * total;
        for (Entry entry : entries) {
            if (pick <= entry.cumulativeWeight) {
                return PcgNodeUtil.resolveValue(context, entry.value);
            }
        }
        return PcgNodeUtil.resolveValue(context, entries.get(entries.size() - 1).value);
    }

    private JsonElement resolveForPoint(PcgGraphContext context, JsonElement value, PcgPoint point) {
        JsonElement resolved = PcgNodeUtil.resolveValue(context, value);
        if (resolved != null && resolved.isJsonPrimitive() && resolved.getAsJsonPrimitive().isString()) {
            String raw = resolved.getAsString();
            if (raw.contains("${")) {
                return new com.google.gson.JsonPrimitive(PcgNodeUtil.interpolatePoint(context, raw, point));
            }
        }
        return resolved;
    }

    private JsonElement resolveForVolume(PcgGraphContext context, JsonElement value, PcgVolume volume) {
        JsonElement resolved = PcgNodeUtil.resolveValue(context, value);
        if (resolved != null && resolved.isJsonPrimitive() && resolved.getAsJsonPrimitive().isString()) {
            String raw = resolved.getAsString();
            if (raw.contains("${")) {
                return new com.google.gson.JsonPrimitive(PcgNodeUtil.interpolateVolume(context, raw, volume));
            }
        }
        return resolved;
    }

    private record Entry(JsonElement value, double cumulativeWeight) {
    }
}
