package top.huliawsl.blockwright.pcg.node;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ResampleSplineNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        List<PcgPoint> controlPoints = input.getPoints();
        if (controlPoints.size() < 2) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "resample_spline requires at least two input points.");
            return PcgData.empty();
        }

        double spacing = Math.max(0.25D, context.getNodeDouble(node, "spacing", 1.0D));
        long seed = context.getLongParameter(context.getNodeString(node, "seedParam", "seed"), 0L);
        List<PcgPoint> samples = new ArrayList<>();
        Vec3 lastAdded = null;
        int sampleIndex = 0;

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Vec3 from = controlPoints.get(i).getPosition();
            Vec3 to = controlPoints.get(i + 1).getPosition();
            Vec3 delta = to.subtract(from);
            double length = delta.length();
            if (length < 1.0E-6D) {
                continue;
            }
            Vec3 tangent = delta.normalize();
            Vec3 normal = PcgNodeUtil.horizontalNormal(tangent);
            int steps = Math.max(1, Mth.ceil(length / spacing));
            for (int step = 0; step <= steps; step++) {
                if (i > 0 && step == 0) {
                    continue;
                }
                double t = Math.min(1.0D, step * spacing / length);
                Vec3 position = from.add(delta.scale(t));
                if (lastAdded != null && lastAdded.distanceToSqr(position) < 0.25D) {
                    continue;
                }
                long pointSeed = seed == 0L ? PcgNodeUtil.mixSeed(position.hashCode(), sampleIndex) : PcgNodeUtil.mixSeed(seed, sampleIndex);
                samples.add(new PcgPoint(position, tangent, normal, PcgNodeUtil.rotationFromTangent(tangent), 1.0D, pointSeed, Map.of()));
                lastAdded = position;
                sampleIndex++;
            }
        }
        return PcgData.ofPoints(samples);
    }
}
