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

public final class SnapToGridNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            return input;
        }
        double gridX = Math.max(1.0D, context.getNodeDouble(node, "gridX", 1.0D));
        double gridY = Math.max(1.0D, context.getNodeDouble(node, "gridY", 1.0D));
        double gridZ = Math.max(1.0D, context.getNodeDouble(node, "gridZ", 1.0D));
        double originX = context.getNodeDouble(node, "originX", 0.5D);
        double originY = context.getNodeDouble(node, "originY", 0.5D);
        double originZ = context.getNodeDouble(node, "originZ", 0.5D);

        List<PcgPoint> points = new ArrayList<>(input.getPoints().size());
        for (PcgPoint point : input.getPoints()) {
            Vec3 position = point.getPosition();
            Vec3 snapped = new Vec3(
                    snap(position.x, originX, gridX),
                    snap(position.y, originY, gridY),
                    snap(position.z, originZ, gridZ)
            );
            points.add(point.withPosition(snapped));
        }
        return new PcgData(points, input.getVolumes());
    }

    private static double snap(double value, double origin, double grid) {
        return Math.round((value - origin) / grid) * grid + origin;
    }
}
