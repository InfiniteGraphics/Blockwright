package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.ModuleHelper;

import java.util.List;
import java.util.Map;

public final class FacadeModulesNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "facade_modules requires a region volume input.");
            return input;
        }

        String tag = resolveModuleTag(context, node);
        if (tag.isBlank()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "facade_modules skipped because module tag is blank.");
            return input;
        }
        String style = context.getStringParameter(context.getNodeString(node, "styleParam", "style"), "");
        List<ModuleDefinition> modules = ModuleHelper.findTaggedModules(context.getModules(), tag, style);
        if (modules.isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "No static facade modules matched tag=" + tag + ".");
            return input;
        }

        int floors = Math.max(1, context.getIntParameter(context.getNodeString(node, "floorsParam", "floors"), context.getNodeInt(node, "floors", 3)));
        int floorHeight = Math.max(3, context.getIntParameter(context.getNodeString(node, "floorHeightParam", "floorHeight"), context.getNodeInt(node, "floorHeight", 4)));
        int horizontalStep = Math.max(1, context.getNodeInt(node, "horizontalStep", 3));
        int inset = Math.max(0, context.getNodeInt(node, "inset", 2));
        boolean ignoreAir = context.getNodeInt(node, "ignoreAir", 1) != 0;
        long seed = context.getLongParameter(context.getNodeString(node, "seedParam", "seed"), 0L);

        for (PcgVolume volume : input.getVolumes()) {
            BlockPos min = volume.getMin();
            BlockPos max = volume.getMax();
            RandomSource random = RandomSource.create(seed == 0L ? Mth.getSeed(min) : seed);
            for (int floor = 0; floor < floors; floor++) {
                int baseY = min.getY() + 1 + floor * floorHeight;
                if (baseY + 1 > max.getY() - 1) {
                    continue;
                }
                for (int x = min.getX() + inset; x <= max.getX() - inset; x += horizontalStep) {
                    ModuleDefinition module = ModuleHelper.pickWeighted(modules, random);
                    ModuleHelper.appendSchematic(context.getPlan(), module, new BlockPos(x, baseY, min.getZ()), Rotation.NONE, ignoreAir);
                    module = ModuleHelper.pickWeighted(modules, random);
                    ModuleHelper.appendSchematic(context.getPlan(), module, new BlockPos(x, baseY, max.getZ()), Rotation.CLOCKWISE_180, ignoreAir);
                }
                for (int z = min.getZ() + inset; z <= max.getZ() - inset; z += horizontalStep) {
                    ModuleDefinition module = ModuleHelper.pickWeighted(modules, random);
                    ModuleHelper.appendSchematic(context.getPlan(), module, new BlockPos(min.getX(), baseY, z), Rotation.COUNTERCLOCKWISE_90, ignoreAir);
                    module = ModuleHelper.pickWeighted(modules, random);
                    ModuleHelper.appendSchematic(context.getPlan(), module, new BlockPos(max.getX(), baseY, z), Rotation.CLOCKWISE_90, ignoreAir);
                }
            }
        }
        return input;
    }

    private String resolveModuleTag(PcgGraphContext context, PcgNodeDefinition node) {
        String explicit = context.getNodeString(node, "tag", "");
        if (!explicit.isBlank()) {
            return explicit;
        }
        String ruleKey = context.getNodeString(node, "tagConfig", "windowTag");
        return context.getRuleConfigString(ruleKey, "");
    }
}
