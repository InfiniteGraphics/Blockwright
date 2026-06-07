package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.world.BlockResolver;

import java.util.Map;
import java.util.Optional;

public final class VoxelizeVolumeNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "voxelize_volume requires at least one volume input.");
            return input;
        }

        String mode = PcgNodeUtil.resolveConfigString(context, node, "mode", "boundary");
        int floorStep = Math.max(0, PcgNodeUtil.resolveConfigInt(context, node, "floorStep", 0));
        Optional<BlockState> wallBlock = resolveConfiguredBlock(context, node, "blockParam", "block", "minecraft:stone_bricks");
        Optional<BlockState> floorBlock = resolveConfiguredBlock(context, node, "floorBlockParam", "floorBlock", "");
        Optional<BlockState> roofBlock = resolveConfiguredBlock(context, node, "roofBlockParam", "roofBlock", "");
        if (wallBlock.isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "voxelize_volume could not resolve its main block.");
            return input;
        }

        for (PcgVolume volume : input.getVolumes()) {
            BlockPos min = volume.getMin();
            BlockPos max = volume.getMax();
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        boolean boundary = x == min.getX() || x == max.getX()
                                || y == min.getY() || y == max.getY()
                                || z == min.getZ() || z == max.getZ();
                        boolean floorLayer = floorStep > 0 && (y - min.getY()) % floorStep == 0;
                        boolean shouldPaint = "solid".equalsIgnoreCase(mode) || boundary || floorLayer;
                        if (!shouldPaint) {
                            continue;
                        }
                        BlockState state = wallBlock.get();
                        if (y == max.getY() && roofBlock.isPresent()) {
                            state = roofBlock.get();
                        } else if ((y == min.getY() || floorLayer) && floorBlock.isPresent()) {
                            state = floorBlock.get();
                        }
                        context.getPlan().addBlock(new BlockPos(x, y, z), state);
                    }
                }
            }
        }
        return input;
    }

    private Optional<BlockState> resolveConfiguredBlock(PcgGraphContext context, PcgNodeDefinition node,
                                                        String paramKey, String directKey, String fallback) {
        String param = PcgNodeUtil.resolveConfigString(context, node, paramKey, "");
        if (param != null && !param.isBlank()) {
            return context.resolveBlockParameter(param, fallback);
        }
        String blockId = PcgNodeUtil.resolveConfigString(context, node, directKey, fallback);
        return BlockResolver.resolve(blockId);
    }
}
