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

public final class RoofFromFootprintNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "roof_from_footprint requires at least one volume input.");
            return input;
        }
        Optional<BlockState> roofBlock = resolveConfiguredBlock(context, node, "roofBlockParam", "roofBlock", "minecraft:oak_planks");
        Optional<BlockState> ridgeBlock = resolveConfiguredBlock(context, node, "ridgeBlockParam", "ridgeBlock", "");
        if (roofBlock.isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "roof_from_footprint could not resolve roofBlock.");
            return input;
        }
        String type = PcgNodeUtil.resolveConfigString(context, node, "roofType", PcgNodeUtil.resolveConfigString(context, node, "mode", "gable"));
        int overhang = Math.max(0, PcgNodeUtil.resolveConfigInt(context, node, "overhang", 1));
        int height = Math.max(1, PcgNodeUtil.resolveConfigInt(context, node, "height", 3));
        for (PcgVolume volume : input.getVolumes()) {
            if ("pyramid".equalsIgnoreCase(type) || "layered".equalsIgnoreCase(type)) {
                addLayeredRoof(context, volume, roofBlock.get(), ridgeBlock.orElse(roofBlock.get()), overhang, height);
            } else {
                addGableRoof(context, volume, roofBlock.get(), ridgeBlock.orElse(roofBlock.get()), overhang, height);
            }
        }
        return input;
    }

    private void addLayeredRoof(PcgGraphContext context, PcgVolume volume, BlockState roofBlock, BlockState ridgeBlock,
                                int overhang, int height) {
        int minX = volume.getMin().getX() - overhang;
        int maxX = volume.getMax().getX() + overhang;
        int minZ = volume.getMin().getZ() - overhang;
        int maxZ = volume.getMax().getZ() + overhang;
        int y = volume.getMax().getY() + 1;
        for (int layer = 0; layer < height && minX <= maxX && minZ <= maxZ; layer++) {
            BlockState state = layer == height - 1 ? ridgeBlock : roofBlock;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || z == minZ || z == maxZ || layer == height - 1;
                    if (edge) {
                        context.getPlan().addBlock(new BlockPos(x, y + layer, z), state);
                    }
                }
            }
            minX++;
            maxX--;
            minZ++;
            maxZ--;
        }
    }

    private void addGableRoof(PcgGraphContext context, PcgVolume volume, BlockState roofBlock, BlockState ridgeBlock,
                              int overhang, int height) {
        int width = volume.getWidth();
        int depth = volume.getDepth();
        boolean ridgeAlongZ = depth >= width;
        int baseY = volume.getMax().getY() + 1;
        if (ridgeAlongZ) {
            int minX = volume.getMin().getX() - overhang;
            int maxX = volume.getMax().getX() + overhang;
            int minZ = volume.getMin().getZ() - overhang;
            int maxZ = volume.getMax().getZ() + overhang;
            int layers = Math.min(height, Math.max(1, (maxX - minX + 2) / 2));
            for (int layer = 0; layer < layers; layer++) {
                int left = minX + layer;
                int right = maxX - layer;
                BlockState state = left >= right || layer == layers - 1 ? ridgeBlock : roofBlock;
                for (int z = minZ; z <= maxZ; z++) {
                    context.getPlan().addBlock(new BlockPos(left, baseY + layer, z), state);
                    if (left != right) {
                        context.getPlan().addBlock(new BlockPos(right, baseY + layer, z), state);
                    }
                }
            }
        } else {
            int minX = volume.getMin().getX() - overhang;
            int maxX = volume.getMax().getX() + overhang;
            int minZ = volume.getMin().getZ() - overhang;
            int maxZ = volume.getMax().getZ() + overhang;
            int layers = Math.min(height, Math.max(1, (maxZ - minZ + 2) / 2));
            for (int layer = 0; layer < layers; layer++) {
                int near = minZ + layer;
                int far = maxZ - layer;
                BlockState state = near >= far || layer == layers - 1 ? ridgeBlock : roofBlock;
                for (int x = minX; x <= maxX; x++) {
                    context.getPlan().addBlock(new BlockPos(x, baseY + layer, near), state);
                    if (near != far) {
                        context.getPlan().addBlock(new BlockPos(x, baseY + layer, far), state);
                    }
                }
            }
        }
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
