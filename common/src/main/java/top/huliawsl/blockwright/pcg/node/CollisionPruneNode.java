package top.huliawsl.blockwright.pcg.node;

import net.minecraft.world.phys.AABB;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.module.CollisionGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CollisionPruneNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        double radius = Math.max(0.25D, context.getNodeDouble(node, "radius", 2.0D));
        CollisionGrid grid = new CollisionGrid();
        List<PcgPoint> kept = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            AABB bounds = new AABB(
                    point.getPosition().x - radius, point.getPosition().y - radius, point.getPosition().z - radius,
                    point.getPosition().x + radius, point.getPosition().y + radius, point.getPosition().z + radius
            );
            if (!grid.intersects(bounds)) {
                grid.add(bounds);
                kept.add(point);
            }
        }
        return new PcgData(kept, input.getVolumes());
    }
}
