package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.Map;

public final class BoxBuildingShellNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "box_building_shell requires a region volume input.");
            return input;
        }

        String floorsParam = context.getNodeString(node, "floorsParam", "floors");
        String floorHeightParam = context.getNodeString(node, "floorHeightParam", "floorHeight");
        int floors = Math.max(1, context.getIntParameter(floorsParam, context.getNodeInt(node, "floors", 3)));
        int floorHeight = Math.max(3, context.getIntParameter(floorHeightParam, context.getNodeInt(node, "floorHeight", 4)));
        BlockState wallState = context.resolveBlockParameter(context.getNodeString(node, "wallBlockParam", "wallBlock"),
                context.getNodeString(node, "wallBlock", "minecraft:stone_bricks")).orElse(null);
        BlockState floorState = context.resolveBlockParameter(context.getNodeString(node, "floorBlockParam", "floorBlock"),
                context.getNodeString(node, "floorBlock", "minecraft:oak_planks")).orElse(null);
        BlockState roofState = context.resolveBlockParameter(context.getNodeString(node, "roofBlockParam", "roofBlock"),
                context.getNodeString(node, "roofBlock", "minecraft:stone")).orElse(null);
        if (wallState == null || floorState == null || roofState == null) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "box_building_shell could not resolve one or more block states.");
            return input;
        }

        for (PcgVolume volume : input.getVolumes()) {
            BlockPos min = volume.getMin();
            BlockPos max = volume.getMax();
            int requiredHeight = floors * floorHeight;
            if (volume.getHeight() < requiredHeight) {
                context.getPlan().addIssue(PreviewSeverity.WARNING, "Selected height is smaller than floors * floorHeight; generation is clipped by region height.");
            }
            for (int y = min.getY(); y <= max.getY(); y++) {
                boolean boundaryY = y == min.getY() || y == max.getY();
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        boolean boundaryX = x == min.getX() || x == max.getX();
                        boolean boundaryZ = z == min.getZ() || z == max.getZ();
                        if (boundaryY) {
                            context.getPlan().addBlock(new BlockPos(x, y, z), y == max.getY() ? roofState : floorState);
                            continue;
                        }
                        int relativeY = y - min.getY();
                        if (relativeY % floorHeight == 0) {
                            context.getPlan().addBlock(new BlockPos(x, y, z), floorState);
                            continue;
                        }
                        if (boundaryX || boundaryZ) {
                            context.getPlan().addBlock(new BlockPos(x, y, z), wallState);
                        }
                    }
                }
            }
        }
        return input;
    }
}
