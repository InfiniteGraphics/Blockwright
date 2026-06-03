package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.rule.ModuleHelper;

import java.util.List;
import java.util.Map;

public final class PlaceModulesNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "place_modules requires input points.");
            return input;
        }
        String tag = resolveModuleTag(context, node);
        if (tag.isBlank()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "place_modules skipped because module tag is blank.");
            return input;
        }
        String style = context.getStringParameter(context.getNodeString(node, "styleParam", "style"), "");
        List<ModuleDefinition> modules = ModuleHelper.findTaggedModules(context.getModules(), tag, style);
        if (modules.isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "No static modules matched tag=" + tag + ".");
            return input;
        }
        int every = Math.max(1, context.getNodeInt(node, "every", 1));
        int start = Math.max(0, context.getNodeInt(node, "start", 0));
        double normalOffset = context.getNodeDouble(node, "normalOffset", 0.0D);
        int yOffset = context.getNodeInt(node, "yOffset", 0);
        boolean ignoreAir = context.getNodeBoolean(node, "ignoreAir", true);
        boolean followTangent = context.getNodeBoolean(node, "followTangent", true);
        long seed = context.getNodeLong(node, "seed", context.getLongParameter(context.getNodeString(node, "seedParam", "seed"), 0L));
        RandomSource random = RandomSource.create(seed == 0L ? input.getPoints().get(0).getSeed() : seed);
        for (int index = start; index < input.getPoints().size(); index += every) {
            PcgPoint point = input.getPoints().get(index);
            Vec3 position = point.getPosition().add(point.getNormal().scale(normalOffset));
            BlockPos origin = new BlockPos(Mth.floor(position.x), Mth.floor(position.y) + yOffset, Mth.floor(position.z));
            ModuleDefinition module = ModuleHelper.pickWeighted(modules, random);
            Rotation rotation = followTangent ? point.getRotation() : Rotation.NONE;
            ModuleHelper.appendSchematic(context.getPlan(), module, origin, rotation, ignoreAir);
        }
        return input;
    }

    private String resolveModuleTag(PcgGraphContext context, PcgNodeDefinition node) {
        String explicit = context.getNodeString(node, "tag", "");
        if (!explicit.isBlank()) {
            return explicit;
        }
        String ruleKey = context.getNodeString(node, "tagConfig", "");
        return ruleKey.isBlank() ? "" : context.getRuleConfigString(ruleKey, "");
    }
}
