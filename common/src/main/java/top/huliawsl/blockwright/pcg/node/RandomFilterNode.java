package top.huliawsl.blockwright.pcg.node;

import net.minecraft.util.RandomSource;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RandomFilterNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        double probability = Math.max(0.0D, Math.min(1.0D, context.getNodeDouble(node, "probability", 0.5D)));
        long seed = context.getNodeLong(node, "seed", context.getLongParameter(context.getNodeString(node, "seedParam", "seed"), 0L));
        List<PcgPoint> kept = new ArrayList<>();
        int index = 0;
        for (PcgPoint point : input.getPoints()) {
            RandomSource random = RandomSource.create(PcgNodeUtil.mixSeed(seed == 0L ? point.getSeed() : seed, index++));
            if (random.nextDouble() <= probability) {
                kept.add(point);
            }
        }
        return new PcgData(kept, input.getVolumes());
    }
}
