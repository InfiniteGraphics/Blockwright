package top.huliawsl.blockwright.rule.executor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.PresetExecutor;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.world.BlockResolver;

public final class BoxBuildingSkeletonExecutor implements PresetExecutor {
    @Override
    public PreviewPlan execute(PresetExecutionContext context) {
        PreviewPlan plan = new PreviewPlan(context.getPreset().id);
        BoxRegionSelection region = context.getSession().getRegionSelection();
        if (!region.isComplete()) {
            plan.addIssue(PreviewSeverity.ERROR, "Region selection is incomplete.");
            return plan;
        }

        BlockState wallState = BlockResolver.resolve(getString(context, "wallBlock", "minecraft:stone_bricks")).orElse(null);
        BlockState floorState = BlockResolver.resolve(getString(context, "floorBlock", "minecraft:oak_planks")).orElse(null);
        BlockState roofState = BlockResolver.resolve(getString(context, "roofBlock", "minecraft:stone")).orElse(null);
        if (wallState == null || floorState == null || roofState == null) {
            plan.addIssue(PreviewSeverity.ERROR, "One or more palette blocks could not be resolved.");
            return plan;
        }

        int floors = Math.max(1, getInt(context, "floors", 3));
        int floorHeight = Math.max(3, getInt(context, "floorHeight", 4));

        BlockPos min = region.getMin();
        BlockPos max = region.getMax();
        int requiredHeight = floors * floorHeight;
        if (region.getHeight() < requiredHeight) {
            plan.addIssue(PreviewSeverity.WARNING, "Selected height is smaller than floors * floorHeight; generation is clipped by region height.");
        }

        for (int y = min.getY(); y <= max.getY(); y++) {
            boolean boundaryY = y == min.getY() || y == max.getY();
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean boundaryX = x == min.getX() || x == max.getX();
                    boolean boundaryZ = z == min.getZ() || z == max.getZ();
                    if (boundaryY) {
                        plan.addBlock(new BlockPos(x, y, z), y == max.getY() ? roofState : floorState);
                        continue;
                    }
                    int relativeY = y - min.getY();
                    if (relativeY % floorHeight == 0) {
                        plan.addBlock(new BlockPos(x, y, z), floorState);
                        continue;
                    }
                    if (boundaryX || boundaryZ) {
                        plan.addBlock(new BlockPos(x, y, z), wallState);
                    }
                }
            }
        }

        return plan;
    }

    private int getInt(PresetExecutionContext context, String key, int fallback) {
        String override = context.getOverrides().get(key);
        if (override == null || override.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(override);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String getString(PresetExecutionContext context, String key, String fallback) {
        String override = context.getOverrides().get(key);
        return override == null || override.isBlank() ? fallback : override;
    }
}
