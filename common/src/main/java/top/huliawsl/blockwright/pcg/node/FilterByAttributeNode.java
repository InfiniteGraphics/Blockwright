package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FilterByAttributeNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            return input;
        }
        String attribute = context.getNodeString(node, "attribute", "");
        if (attribute.isBlank()) {
            return input;
        }
        String operation = context.getNodeString(node, "operation", "eq");
        JsonElement expected = PcgNodeUtil.configuredAttributeValue(context, node);

        List<PcgPoint> kept = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            JsonElement actual = PcgNodeUtil.pointValue(point, attribute);
            if (PcgNodeUtil.compare(actual, operation, expected)) {
                kept.add(point);
            }
        }
        return new PcgData(kept, input.getVolumes());
    }
}
