package top.huliawsl.blockwright.pcg.node;

import net.minecraft.util.Mth;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DensityMaskNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        double min = context.getNodeDouble(node, "min", 0.0D);
        double max = context.getNodeDouble(node, "max", 1.0D);
        double scale = Math.max(0.001D, context.getNodeDouble(node, "noiseScale", 32.0D));
        boolean writeOnly = context.getNodeBoolean(node, "writeOnly", false);
        List<PcgPoint> points = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            double noise = pseudoNoise(point.getPosition().x / scale, point.getPosition().z / scale, point.getSeed());
            double density = Mth.clamp(noise, 0.0D, 1.0D);
            PcgPoint updated = point.withDensity(density).withAttribute("density", new com.google.gson.JsonPrimitive(density));
            if (writeOnly || density >= min && density <= max) {
                points.add(updated);
            }
        }
        return new PcgData(points, input.getVolumes());
    }

    private double pseudoNoise(double x, double z, long seed) {
        double value = Math.sin(x * 12.9898D + z * 78.233D + seed * 0.0001D) * 43758.5453D;
        return value - Math.floor(value);
    }
}
