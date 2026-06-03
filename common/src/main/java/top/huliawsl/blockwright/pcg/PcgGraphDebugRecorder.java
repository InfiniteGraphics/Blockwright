package top.huliawsl.blockwright.pcg;

import net.minecraft.world.phys.Vec3;

public final class PcgGraphDebugRecorder {
    private static final int NODE_POINT_COLOR = 0xFF68B7FF;
    private static final int NODE_LINE_COLOR = 0xFFD786FF;
    private static final int MAX_POINTS_PER_NODE = 96;
    private static final int MAX_LINES_PER_NODE = 96;

    private PcgGraphDebugRecorder() {
    }

    public static void record(PcgGraphContext context, PcgNodeDefinition node, PcgData data) {
        if (!context.isDebugEnabled() || data == null) {
            return;
        }
        int pointCount = 0;
        for (PcgPoint point : data.getPoints()) {
            if (pointCount++ >= MAX_POINTS_PER_NODE) {
                break;
            }
            context.getPlan().addDebugPoint(point.getPosition(), node.getId(), NODE_POINT_COLOR);
            Vec3 tangentEnd = point.getPosition().add(point.getTangent().scale(0.7D));
            Vec3 normalEnd = point.getPosition().add(point.getNormal().scale(0.45D));
            context.getPlan().addDebugLine(point.getPosition(), tangentEnd, node.getId() + ":t", NODE_LINE_COLOR);
            context.getPlan().addDebugLine(point.getPosition(), normalEnd, node.getId() + ":n", 0xFF74D888);
        }

        int lineCount = 0;
        for (PcgVolume volume : data.getVolumes()) {
            if (lineCount++ >= MAX_LINES_PER_NODE) {
                break;
            }
            Vec3 min = Vec3.atLowerCornerOf(volume.getMin());
            Vec3 max = Vec3.atLowerCornerOf(volume.getMax()).add(1.0D, 1.0D, 1.0D);
            context.getPlan().addDebugLine(min, new Vec3(max.x, min.y, min.z), node.getId(), NODE_LINE_COLOR);
            context.getPlan().addDebugLine(min, new Vec3(min.x, max.y, min.z), node.getId(), NODE_LINE_COLOR);
            context.getPlan().addDebugLine(min, new Vec3(min.x, min.y, max.z), node.getId(), NODE_LINE_COLOR);
            context.getPlan().addDebugLine(max, new Vec3(min.x, max.y, max.z), node.getId(), NODE_LINE_COLOR);
            context.getPlan().addDebugLine(max, new Vec3(max.x, min.y, max.z), node.getId(), NODE_LINE_COLOR);
            context.getPlan().addDebugLine(max, new Vec3(max.x, max.y, min.z), node.getId(), NODE_LINE_COLOR);
        }
    }
}
