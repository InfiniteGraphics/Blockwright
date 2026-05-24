package top.huliawsl.blockwright.rule.executor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.ModuleHelper;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.PresetExecutor;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.world.BlockResolver;

import java.util.List;

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
        long seed = getLong(context, "seed", 0L);
        String style = getString(context, "style", "");

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

        addWindowModules(context, plan, min, max, floorHeight, floors, seed, style);
        return plan;
    }

    private void addWindowModules(PresetExecutionContext context, PreviewPlan plan, BlockPos min, BlockPos max,
                                  int floorHeight, int floors, long seed, String style) {
        String windowTag = context.getRule().config.has("windowTag") ? context.getRule().config.get("windowTag").getAsString() : "";
        if (windowTag.isBlank()) {
            return;
        }
        List<ModuleDefinition> windowModules = ModuleHelper.findTaggedModules(context.getModules(), windowTag, style);
        if (windowModules.isEmpty()) {
            plan.addIssue(PreviewSeverity.WARNING, "No static window modules matched tag=" + windowTag + ".");
            return;
        }

        RandomSource random = RandomSource.create(seed == 0L ? Mth.getSeed(min) : seed);
        for (int floor = 0; floor < floors; floor++) {
            int baseY = min.getY() + 1 + floor * floorHeight;
            if (baseY + 1 > max.getY() - 1) {
                continue;
            }
            for (int x = min.getX() + 2; x <= max.getX() - 2; x += 3) {
                ModuleDefinition module = ModuleHelper.pickWeighted(windowModules, random);
                ModuleHelper.appendSchematic(plan, module, new BlockPos(x, baseY, min.getZ()), Rotation.NONE, true);
                ModuleHelper.appendSchematic(plan, module, new BlockPos(x, baseY, max.getZ()), Rotation.CLOCKWISE_180, true);
            }
            for (int z = min.getZ() + 2; z <= max.getZ() - 2; z += 3) {
                ModuleDefinition module = ModuleHelper.pickWeighted(windowModules, random);
                ModuleHelper.appendSchematic(plan, module, new BlockPos(min.getX(), baseY, z), Rotation.COUNTERCLOCKWISE_90, true);
                ModuleHelper.appendSchematic(plan, module, new BlockPos(max.getX(), baseY, z), Rotation.CLOCKWISE_90, true);
            }
        }
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

    private long getLong(PresetExecutionContext context, String key, long fallback) {
        String override = context.getOverrides().get(key);
        if (override == null || override.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(override);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
