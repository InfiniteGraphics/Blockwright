package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ScatterPointsNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "scatter_points requires at least one volume input.");
            return PcgData.empty();
        }
        int count = Math.max(0, context.getNodeInt(node, "count", 64));
        int spacing = Math.max(1, context.getNodeInt(node, "spacing", 1));
        boolean grid = context.getNodeBoolean(node, "grid", false);
        long seed = context.getNodeLong(node, "seed", context.getLongParameter(context.getNodeString(node, "seedParam", "seed"), 0L));
        RandomSource random = RandomSource.create(seed == 0L ? 0xB10C57A11L : seed);
        List<PcgPoint> points = new ArrayList<>();
        for (PcgVolume volume : input.getVolumes()) {
            if (grid) {
                for (int x = volume.getMin().getX(); x <= volume.getMax().getX(); x += spacing) {
                    for (int z = volume.getMin().getZ(); z <= volume.getMax().getZ(); z += spacing) {
                        BlockPos pos = new BlockPos(x, volume.getMin().getY(), z);
                        points.add(PcgPoint.at(pos).withSeed(PcgNodeUtil.mixSeed(Mth.getSeed(pos), points.size())));
                        if (points.size() >= count) {
                            return PcgData.ofPoints(points);
                        }
                    }
                }
            } else {
                for (int i = 0; i < count; i++) {
                    int x = Mth.nextInt(random, volume.getMin().getX(), volume.getMax().getX());
                    int y = Mth.nextInt(random, volume.getMin().getY(), volume.getMax().getY());
                    int z = Mth.nextInt(random, volume.getMin().getZ(), volume.getMax().getZ());
                    Vec3 position = new Vec3(x + 0.5D, y + 0.5D, z + 0.5D);
                    points.add(new PcgPoint(position, new Vec3(0, 0, 1), new Vec3(1, 0, 0), null, 1.0D,
                            PcgNodeUtil.mixSeed(seed, points.size()), Map.of()));
                }
            }
        }
        return PcgData.ofPoints(points);
    }
}
