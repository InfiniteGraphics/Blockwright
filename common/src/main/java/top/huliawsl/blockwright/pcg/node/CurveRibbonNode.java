package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CurveRibbonNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "curve_ribbon requires sampled curve points.");
            return input;
        }

        int width = resolveWidth(context, node);
        int step = Math.max(1, PcgNodeUtil.resolveConfigInt(context, node, "step", 1));
        double yOffset = PcgNodeUtil.resolveConfigDouble(context, node, "yOffset", 0.0D);
        String edgeAttribute = PcgNodeUtil.resolveConfigString(context, node, "edgeAttribute", "edge");
        String offsetAttribute = PcgNodeUtil.resolveConfigString(context, node, "offsetAttribute", "ribbonOffset");
        String sideAttribute = PcgNodeUtil.resolveConfigString(context, node, "sideAttribute", "side");
        boolean includeCenter = PcgNodeUtil.resolveConfigBoolean(context, node, "includeCenter", true);

        int radius = Math.max(0, width / 2);
        List<PcgPoint> ribbon = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            Vec3 normal = point.getNormal();
            if (normal.lengthSqr() < 1.0E-8D) {
                normal = PcgNodeUtil.horizontalNormal(point.getTangent());
            }
            for (int offset = -radius; offset <= radius; offset += step) {
                if (!includeCenter && offset == 0) {
                    continue;
                }
                boolean edge = Math.abs(offset) >= radius && radius > 0;
                Vec3 position = point.getPosition().add(normal.scale(offset)).add(0.0D, yOffset, 0.0D);
                Map<String, JsonElement> attributes = new LinkedHashMap<>(point.getAttributes());
                if (!edgeAttribute.isBlank()) {
                    attributes.put(edgeAttribute, new JsonPrimitive(edge));
                }
                if (!offsetAttribute.isBlank()) {
                    attributes.put(offsetAttribute, new JsonPrimitive(offset));
                }
                if (!sideAttribute.isBlank()) {
                    attributes.put(sideAttribute, new JsonPrimitive(offset < 0 ? "left" : offset > 0 ? "right" : "center"));
                }
                ribbon.add(new PcgPoint(position, point.getTangent(), normal, point.getRotation(), point.getDensity(),
                        PcgNodeUtil.mixSeed(point.getSeed(), ribbon.size()), attributes));
            }
        }
        return new PcgData(ribbon, input.getVolumes());
    }

    private int resolveWidth(PcgGraphContext context, PcgNodeDefinition node) {
        String widthParam = PcgNodeUtil.resolveConfigString(context, node, "widthParam", "");
        int fallback = Math.max(1, PcgNodeUtil.resolveConfigInt(context, node, "width", 5));
        if (widthParam != null && !widthParam.isBlank()) {
            return Math.max(1, context.getIntParameter(widthParam, fallback));
        }
        return fallback;
    }
}
