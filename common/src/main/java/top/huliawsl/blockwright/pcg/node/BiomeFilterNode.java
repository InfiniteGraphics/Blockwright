package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BiomeFilterNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (context.getServerLevel().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "biome_filter needs server world data; keeping all points.");
            return input;
        }
        Set<String> allowed = readStringSet(node, "allow");
        Set<String> denied = readStringSet(node, "deny");
        List<PcgPoint> kept = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            String biome = context.getServerLevel().get().getBiome(point.blockPos()).unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("");
            if (!allowed.isEmpty() && !allowed.contains(biome)) {
                continue;
            }
            if (denied.contains(biome)) {
                continue;
            }
            kept.add(point.withAttribute("biome", new com.google.gson.JsonPrimitive(biome)));
        }
        return new PcgData(kept, input.getVolumes());
    }

    private Set<String> readStringSet(PcgNodeDefinition node, String key) {
        Set<String> values = new HashSet<>();
        if (node.getConfig().has(key) && node.getConfig().get(key).isJsonArray()) {
            JsonArray array = node.getConfig().getAsJsonArray(key);
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    ResourceLocation location = ResourceLocation.tryParse(element.getAsString());
                    if (location != null) {
                        values.add(location.toString());
                    }
                }
            }
        }
        return values;
    }
}
