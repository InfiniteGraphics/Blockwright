package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TerrainSampleNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (context.getServerLevel().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "terrain_sample needs server world data; keeping input point heights.");
            return input;
        }
        int yOffset = context.getNodeInt(node, "yOffset", 0);
        List<PcgPoint> sampled = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            BlockPos pos = point.blockPos();
            int y = context.getServerLevel().get().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) + yOffset;
            sampled.add(point.withPosition(new Vec3(point.getPosition().x, y + 0.5D, point.getPosition().z)));
        }
        return new PcgData(sampled, input.getVolumes());
    }
}
