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
import top.huliawsl.blockwright.selection.SplineSelection;
import top.huliawsl.blockwright.world.BlockResolver;

import java.util.ArrayList;
import java.util.List;

public final class SplineRoadSkeletonExecutor implements PresetExecutor {
    @Override
    public PreviewPlan execute(PresetExecutionContext context) {
        PreviewPlan plan = new PreviewPlan(context.getPreset().id);
        SplineSelection spline = context.getSession().getSplineSelection();
        if (spline.getPoints().size() < 2) {
            plan.addIssue(PreviewSeverity.ERROR, "Spline requires at least two control points.");
            return plan;
        }

        BlockState roadState = BlockResolver.resolve(getString(context, "roadBlock", "minecraft:stone")).orElse(null);
        BlockState edgeState = BlockResolver.resolve(getString(context, "edgeBlock", "minecraft:cobblestone")).orElse(null);
        if (roadState == null || edgeState == null) {
            plan.addIssue(PreviewSeverity.ERROR, "Road palette blocks could not be resolved.");
            return plan;
        }

        int width = Math.max(1, getInt(context, "roadWidth", 5));
        String style = getString(context, "style", "");
        long seed = getLong(context, "seed", 0L);
        List<BlockPos> rasterized = rasterize(spline.getPoints());
        for (BlockPos center : rasterized) {
            int half = width / 2;
            for (int x = -half; x <= half; x++) {
                BlockPos pos = center.offset(x, 0, 0);
                boolean edge = x == -half || x == half;
                plan.addBlock(pos, edge ? edgeState : roadState);
            }
        }

        addRoadLampModules(context, plan, rasterized, width, style, seed);
        return plan;
    }

    private void addRoadLampModules(PresetExecutionContext context, PreviewPlan plan, List<BlockPos> rasterized,
                                    int width, String style, long seed) {
        String roadLampTag = context.getRule().config.has("roadLampTag") ? context.getRule().config.get("roadLampTag").getAsString() : "";
        if (roadLampTag.isBlank()) {
            return;
        }
        List<ModuleDefinition> roadLampModules = ModuleHelper.findTaggedModules(context.getModules(), roadLampTag, style);
        if (roadLampModules.isEmpty()) {
            plan.addIssue(PreviewSeverity.WARNING, "No static road lamp modules matched tag=" + roadLampTag + ".");
            return;
        }

        RandomSource random = RandomSource.create(seed == 0L ? Mth.getSeed(rasterized.get(0)) : seed);
        int sideOffset = Math.max(2, width / 2 + 1);
        for (int index = 3; index < rasterized.size(); index += 6) {
            BlockPos center = rasterized.get(index);
            ModuleDefinition module = ModuleHelper.pickWeighted(roadLampModules, random);
            Rotation rotation = random.nextBoolean() ? Rotation.NONE : Rotation.CLOCKWISE_180;
            int side = random.nextBoolean() ? 1 : -1;
            ModuleHelper.appendSchematic(plan, module, center.offset(side * sideOffset, 0, 0), rotation, true);
        }
    }

    private List<BlockPos> rasterize(List<BlockPos> points) {
        List<BlockPos> result = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            BlockPos from = points.get(i);
            BlockPos to = points.get(i + 1);
            int dx = to.getX() - from.getX();
            int dy = to.getY() - from.getY();
            int dz = to.getZ() - from.getZ();
            int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            for (int step = 0; step <= steps; step++) {
                double t = steps == 0 ? 0 : (double) step / (double) steps;
                result.add(new BlockPos(
                        (int) Math.round(from.getX() + dx * t),
                        (int) Math.round(from.getY() + dy * t),
                        (int) Math.round(from.getZ() + dz * t)
                ));
            }
        }
        return result;
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
