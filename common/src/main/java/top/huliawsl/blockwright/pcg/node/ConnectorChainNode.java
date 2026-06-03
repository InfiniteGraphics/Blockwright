package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.module.ConnectorResolver;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.Map;

public final class ConnectorChainNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "connector_chain requires one or more seed points.");
            return input;
        }
        String startTag = context.getNodeString(node, "startTag", "room_start");
        String moduleTag = context.getNodeString(node, "moduleTag", context.getNodeString(node, "tag", "room"));
        int maxModules = Math.max(1, context.getNodeInt(node, "maxModules", 16));
        boolean ignoreAir = context.getNodeBoolean(node, "ignoreAir", true);
        long seed = context.getNodeLong(node, "seed", context.getLongParameter(context.getNodeString(node, "seedParam", "seed"), 0L));
        for (PcgPoint point : input.getPoints()) {
            BlockPos origin = new BlockPos(Mth.floor(point.getPosition().x), Mth.floor(point.getPosition().y), Mth.floor(point.getPosition().z));
            ConnectorResolver.growChain(context.getPlan(), context.getModules(), origin, point.getRotation(), startTag, moduleTag,
                    maxModules, seed == 0L ? point.getSeed() : seed, ignoreAir);
        }
        return input;
    }
}
