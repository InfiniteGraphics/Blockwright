package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShapeMetricsNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "shape_metrics requires at least one shape/volume input.");
            return input;
        }
        boolean emitCenterPoint = PcgNodeUtil.resolveConfigBoolean(context, node, "emitCenterPoint", false);
        List<PcgVolume> volumes = new ArrayList<>();
        List<PcgPoint> points = new ArrayList<>(input.getPoints());
        for (PcgVolume volume : input.getVolumes()) {
            Map<String, JsonElement> attributes = new LinkedHashMap<>(volume.getAttributes());
            writeMetrics(attributes, volume);
            PcgVolume updated = volume.withAttributes(attributes);
            volumes.add(updated);
            if (emitCenterPoint) {
                points.add(centerPoint(updated, attributes));
            }
        }
        return new PcgData(points, volumes);
    }

    private static void writeMetrics(Map<String, JsonElement> attributes, PcgVolume volume) {
        BlockPos min = volume.getMin();
        BlockPos max = volume.getMax();
        int width = volume.getWidth();
        int height = volume.getHeight();
        int depth = volume.getDepth();
        int longSide = Math.max(width, depth);
        int shortSide = Math.min(width, depth);
        attributes.put("minX", new JsonPrimitive(min.getX()));
        attributes.put("minY", new JsonPrimitive(min.getY()));
        attributes.put("minZ", new JsonPrimitive(min.getZ()));
        attributes.put("maxX", new JsonPrimitive(max.getX()));
        attributes.put("maxY", new JsonPrimitive(max.getY()));
        attributes.put("maxZ", new JsonPrimitive(max.getZ()));
        attributes.put("width", new JsonPrimitive(width));
        attributes.put("height", new JsonPrimitive(height));
        attributes.put("depth", new JsonPrimitive(depth));
        attributes.put("centerX", new JsonPrimitive((min.getX() + max.getX()) * 0.5D + 0.5D));
        attributes.put("centerY", new JsonPrimitive((min.getY() + max.getY()) * 0.5D + 0.5D));
        attributes.put("centerZ", new JsonPrimitive((min.getZ() + max.getZ()) * 0.5D + 0.5D));
        attributes.put("area", new JsonPrimitive(width * depth));
        attributes.put("footprintArea", new JsonPrimitive(width * depth));
        attributes.put("perimeter", new JsonPrimitive(width * 2 + depth * 2));
        attributes.put("longSide", new JsonPrimitive(longSide));
        attributes.put("shortSide", new JsonPrimitive(shortSide));
        attributes.put("longAxis", new JsonPrimitive(depth > width ? "z" : "x"));
        attributes.put("shortAxis", new JsonPrimitive(depth > width ? "x" : "z"));
    }

    private static PcgPoint centerPoint(PcgVolume volume, Map<String, JsonElement> attributes) {
        Vec3 position = new Vec3(
                attributes.get("centerX").getAsDouble(),
                attributes.get("centerY").getAsDouble(),
                attributes.get("centerZ").getAsDouble()
        );
        return new PcgPoint(position, new Vec3(0.0D, 0.0D, 1.0D), new Vec3(0.0D, 1.0D, 0.0D),
                Rotation.NONE, 1.0D, PcgNodeUtil.mixSeed(volume.getMin().asLong(), volume.getMax().hashCode()), attributes);
    }
}
