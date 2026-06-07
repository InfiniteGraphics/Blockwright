package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShapeExtrudeNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "shape_extrude requires at least one shape/volume input.");
            return input;
        }

        boolean clipToInput = PcgNodeUtil.resolveConfigBoolean(context, node, "clipToInput", true);
        String labelOverride = PcgNodeUtil.resolveConfigString(context, node, "label", "");
        List<PcgVolume> volumes = new ArrayList<>();
        for (PcgVolume volume : input.getVolumes()) {
            int height = Math.max(1, resolveHeight(context, node, volume));
            BlockPos min = volume.getMin();
            int maxY = min.getY() + height - 1;
            if (clipToInput) {
                maxY = Math.min(maxY, volume.getMax().getY());
            }
            if (maxY < min.getY()) {
                continue;
            }
            BlockPos max = new BlockPos(volume.getMax().getX(), maxY, volume.getMax().getZ());
            PcgVolume extruded = volume.withBounds(min, max);
            if (!labelOverride.isBlank()) {
                extruded = extruded.withLabel(labelOverride);
            }
            volumes.add(extruded);
        }
        return new PcgData(input.getPoints(), volumes);
    }

    private int resolveHeight(PcgGraphContext context, PcgNodeDefinition node, PcgVolume volume) {
        String expression = PcgNodeUtil.resolveConfigString(context, node, "heightExpression", "");
        int fallback = PcgNodeUtil.resolveConfigInt(context, node, "height", 8);
        if (!expression.isBlank()) {
            double evaluated = PcgExpressionUtil.evaluateNumber(context, expression, null, volume, fallback);
            return Math.max(1, (int) Math.round(evaluated));
        }
        String heightParam = PcgNodeUtil.resolveConfigString(context, node, "heightParam", "");
        if (heightParam != null && !heightParam.isBlank()) {
            return context.getIntParameter(heightParam, fallback);
        }
        return fallback;
    }
}
