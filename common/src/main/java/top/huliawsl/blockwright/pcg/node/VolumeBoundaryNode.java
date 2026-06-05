package top.huliawsl.blockwright.pcg.node;

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

public final class VolumeBoundaryNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "volume_boundary requires at least one volume input.");
            return PcgData.empty();
        }
        String faces = context.getNodeString(node, "faces", "sides");
        int horizontalStep = Math.max(1, context.getNodeInt(node, "horizontalStep", 1));
        int verticalStep = Math.max(1, context.getNodeInt(node, "verticalStep", 1));
        int inset = Math.max(0, context.getNodeInt(node, "inset", 0));

        List<PcgPoint> points = new ArrayList<>();
        for (PcgVolume volume : input.getVolumes()) {
            BlockPos min = volume.getMin().offset(inset, inset, inset);
            BlockPos max = volume.getMax().offset(-inset, -inset, -inset);
            if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
                continue;
            }
            if (includes(faces, "top", "all")) {
                for (int x = min.getX(); x <= max.getX(); x += horizontalStep) {
                    for (int z = min.getZ(); z <= max.getZ(); z += horizontalStep) {
                        addPoint(points, x, max.getY(), z, "top", new Vec3(1, 0, 0), new Vec3(0, 1, 0), volume.getLabel());
                    }
                }
            }
            if (includes(faces, "bottom", "all")) {
                for (int x = min.getX(); x <= max.getX(); x += horizontalStep) {
                    for (int z = min.getZ(); z <= max.getZ(); z += horizontalStep) {
                        addPoint(points, x, min.getY(), z, "bottom", new Vec3(1, 0, 0), new Vec3(0, -1, 0), volume.getLabel());
                    }
                }
            }
            if (includes(faces, "sides", "all")) {
                for (int y = min.getY(); y <= max.getY(); y += verticalStep) {
                    for (int x = min.getX(); x <= max.getX(); x += horizontalStep) {
                        addPoint(points, x, y, min.getZ(), "north", new Vec3(1, 0, 0), new Vec3(0, 0, -1), volume.getLabel());
                        if (max.getZ() != min.getZ()) {
                            addPoint(points, x, y, max.getZ(), "south", new Vec3(1, 0, 0), new Vec3(0, 0, 1), volume.getLabel());
                        }
                    }
                    for (int z = min.getZ() + horizontalStep; z < max.getZ(); z += horizontalStep) {
                        addPoint(points, min.getX(), y, z, "west", new Vec3(0, 0, 1), new Vec3(-1, 0, 0), volume.getLabel());
                        if (max.getX() != min.getX()) {
                            addPoint(points, max.getX(), y, z, "east", new Vec3(0, 0, 1), new Vec3(1, 0, 0), volume.getLabel());
                        }
                    }
                }
            }
        }
        return PcgData.ofPoints(points);
    }

    private static boolean includes(String faces, String value, String wildcard) {
        return wildcard.equalsIgnoreCase(faces) || value.equalsIgnoreCase(faces);
    }

    private static void addPoint(List<PcgPoint> points, int x, int y, int z, String face, Vec3 tangent, Vec3 normal, String label) {
        Map<String, com.google.gson.JsonElement> attributes = new LinkedHashMap<>();
        attributes.put("face", new JsonPrimitive(face));
        if (label != null && !label.isBlank()) {
            attributes.put("volumeLabel", new JsonPrimitive(label));
        }
        points.add(new PcgPoint(
                new Vec3(x + 0.5D, y + 0.5D, z + 0.5D),
                tangent,
                normal,
                Rotation.NONE,
                1.0D,
                PcgNodeUtil.mixSeed((((long) x) << 32) ^ (((long) y) << 16) ^ (z & 0xFFFFL), points.size()),
                attributes
        ));
    }
}
