package top.huliawsl.blockwright.pcg.node;

import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OffsetPointsNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            return input;
        }
        double dx = context.getNodeDouble(node, "dx", 0.0D);
        double dy = context.getNodeDouble(node, "dy", 0.0D);
        double dz = context.getNodeDouble(node, "dz", 0.0D);
        double tangentOffset = context.getNodeDouble(node, "tangentOffset", 0.0D);
        double normalOffset = context.getNodeDouble(node, "normalOffset", 0.0D);

        List<PcgPoint> points = new ArrayList<>(input.getPoints().size());
        for (PcgPoint point : input.getPoints()) {
            Vec3 position = point.getPosition()
                    .add(dx, dy, dz)
                    .add(point.getTangent().scale(tangentOffset))
                    .add(point.getNormal().scale(normalOffset));
            points.add(point.withPosition(position));
        }
        return new PcgData(points, input.getVolumes());
    }
}
