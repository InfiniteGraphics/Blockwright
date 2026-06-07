package top.huliawsl.blockwright.pcg.node;

import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FilterByExpressionNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        String expression = PcgNodeUtil.resolveConfigString(context, node, "expression", "");
        if (expression.isBlank()) {
            return input;
        }
        String target = PcgNodeUtil.resolveConfigString(context, node, "target", "points");
        List<PcgPoint> points = new ArrayList<>();
        if (target.equalsIgnoreCase("points") || target.equalsIgnoreCase("both")) {
            for (PcgPoint point : input.getPoints()) {
                if (PcgExpressionUtil.evaluateBoolean(context, expression, point, null, false)) {
                    points.add(point);
                }
            }
        } else {
            points.addAll(input.getPoints());
        }
        List<PcgVolume> volumes = new ArrayList<>();
        if (target.equalsIgnoreCase("volumes") || target.equalsIgnoreCase("both")) {
            for (PcgVolume volume : input.getVolumes()) {
                if (PcgExpressionUtil.evaluateBoolean(context, expression, null, volume, false)) {
                    volumes.add(volume);
                }
            }
        } else {
            volumes.addAll(input.getVolumes());
        }
        return new PcgData(points, volumes);
    }
}
