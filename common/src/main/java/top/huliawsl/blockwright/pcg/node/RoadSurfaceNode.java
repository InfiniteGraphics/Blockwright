package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RoadSurfaceNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "road_surface requires sampled spline points.");
            return input;
        }

        String widthParam = context.getNodeString(node, "widthParam", "roadWidth");
        String roadBlockParam = context.getNodeString(node, "roadBlockParam", "roadBlock");
        String edgeBlockParam = context.getNodeString(node, "edgeBlockParam", "edgeBlock");
        int width = Math.max(1, context.getIntParameter(widthParam, context.getNodeInt(node, "width", 5)));
        BlockState roadState = context.resolveBlockParameter(roadBlockParam, context.getNodeString(node, "roadBlock", "minecraft:stone")).orElse(null);
        BlockState edgeState = context.resolveBlockParameter(edgeBlockParam, context.getNodeString(node, "edgeBlock", "minecraft:cobblestone")).orElse(null);
        if (roadState == null || edgeState == null) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "road_surface could not resolve road or edge block state.");
            return input;
        }

        int yOffset = context.getNodeInt(node, "yOffset", 0);
        int left = -(width / 2);
        int right = width - width / 2 - 1;
        Map<BlockPos, BlockState> dedupedBlocks = new LinkedHashMap<>();
        for (PcgPoint point : input.getPoints()) {
            Vec3 normal = point.getNormal();
            for (int offset = left; offset <= right; offset++) {
                Vec3 position = point.getPosition().add(normal.scale(offset));
                BlockPos blockPos = new BlockPos(Mth.floor(position.x), Mth.floor(position.y) + yOffset, Mth.floor(position.z));
                boolean edge = offset == left || offset == right;
                dedupedBlocks.put(blockPos, edge ? edgeState : roadState);
            }
        }
        for (Map.Entry<BlockPos, BlockState> entry : dedupedBlocks.entrySet()) {
            context.getPlan().addBlock(entry.getKey(), entry.getValue());
        }
        return input;
    }
}
