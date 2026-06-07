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
import java.util.Locale;
import java.util.Map;

public final class FacadeGridNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getVolumes().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "facade_grid requires at least one volume input.");
            return input;
        }
        String faces = PcgNodeUtil.resolveConfigString(context, node, "faces", "sides").toLowerCase(Locale.ROOT);
        int cellWidth = Math.max(1, PcgNodeUtil.resolveConfigInt(context, node, "cellWidth", 3));
        int floorHeight = Math.max(1, PcgNodeUtil.resolveConfigInt(context, node, "floorHeight", context.getIntParameter("floorHeight", 4)));
        int inset = Math.max(0, PcgNodeUtil.resolveConfigInt(context, node, "inset", 1));
        int yOffset = PcgNodeUtil.resolveConfigInt(context, node, "yOffset", 1);
        boolean includeGroundFloor = PcgNodeUtil.resolveConfigBoolean(context, node, "includeGroundFloor", true);
        List<PcgPoint> points = new ArrayList<>();
        for (PcgVolume volume : input.getVolumes()) {
            addFace(points, volume, "north", faces, cellWidth, floorHeight, inset, yOffset, includeGroundFloor);
            addFace(points, volume, "south", faces, cellWidth, floorHeight, inset, yOffset, includeGroundFloor);
            addFace(points, volume, "west", faces, cellWidth, floorHeight, inset, yOffset, includeGroundFloor);
            addFace(points, volume, "east", faces, cellWidth, floorHeight, inset, yOffset, includeGroundFloor);
        }
        return new PcgData(points, input.getVolumes());
    }

    private void addFace(List<PcgPoint> points, PcgVolume volume, String face, String faces, int cellWidth,
                         int floorHeight, int inset, int yOffset, boolean includeGroundFloor) {
        if (!includeFace(faces, face)) {
            return;
        }
        BlockPos min = volume.getMin();
        BlockPos max = volume.getMax();
        int start = face.equals("north") || face.equals("south") ? min.getX() + inset : min.getZ() + inset;
        int end = face.equals("north") || face.equals("south") ? max.getX() - inset : max.getZ() - inset;
        if (start > end) {
            return;
        }
        int length = end - start + 1;
        int colCount = Math.max(1, (int) Math.ceil(Math.max(1, length) / (double) cellWidth));
        int floorStartY = min.getY() + yOffset;
        int usableHeight = Math.max(1, max.getY() - floorStartY + 1);
        int floorCount = Math.max(1, usableHeight / floorHeight);
        for (int floor = 0; floor < floorCount; floor++) {
            if (floor == 0 && !includeGroundFloor) {
                continue;
            }
            int y = Math.min(max.getY(), floorStartY + floor * floorHeight + Math.max(0, floorHeight / 2));
            for (int col = 0; col < colCount; col++) {
                double center = start + (col + 0.5D) * length / colCount;
                Vec3 pos = positionForFace(face, volume, center, y);
                Vec3 tangent = tangentForFace(face);
                Vec3 normal = normalForFace(face);
                Map<String, JsonElement> attributes = new LinkedHashMap<>();
                attributes.put("face", new JsonPrimitive(face));
                attributes.put("floorIndex", new JsonPrimitive(floor));
                attributes.put("row", new JsonPrimitive(floor));
                attributes.put("col", new JsonPrimitive(col));
                attributes.put("colCount", new JsonPrimitive(colCount));
                attributes.put("rowCount", new JsonPrimitive(floorCount));
                attributes.put("isCenter", new JsonPrimitive(col == colCount / 2));
                attributes.put("isEdge", new JsonPrimitive(col == 0 || col == colCount - 1));
                attributes.put("isGroundFloor", new JsonPrimitive(floor == 0));
                attributes.put("volumeLabel", new JsonPrimitive(volume.getLabel()));
                points.add(new PcgPoint(pos, tangent, normal, rotationForFace(face), 1.0D,
                        PcgNodeUtil.mixSeed(volume.getMin().asLong() ^ volume.getMax().asLong(), points.size()), attributes));
            }
        }
    }

    private boolean includeFace(String faces, String face) {
        return "all".equals(faces) || "sides".equals(faces) || face.equals(faces);
    }

    private Vec3 positionForFace(String face, PcgVolume volume, double center, int y) {
        return switch (face) {
            case "south" -> new Vec3(center + 0.5D, y + 0.5D, volume.getMax().getZ() + 0.5D);
            case "west" -> new Vec3(volume.getMin().getX() + 0.5D, y + 0.5D, center + 0.5D);
            case "east" -> new Vec3(volume.getMax().getX() + 0.5D, y + 0.5D, center + 0.5D);
            default -> new Vec3(center + 0.5D, y + 0.5D, volume.getMin().getZ() + 0.5D);
        };
    }

    private Vec3 tangentForFace(String face) {
        return switch (face) {
            case "west", "east" -> new Vec3(0.0D, 0.0D, 1.0D);
            default -> new Vec3(1.0D, 0.0D, 0.0D);
        };
    }

    private Vec3 normalForFace(String face) {
        return switch (face) {
            case "south" -> new Vec3(0.0D, 0.0D, 1.0D);
            case "west" -> new Vec3(-1.0D, 0.0D, 0.0D);
            case "east" -> new Vec3(1.0D, 0.0D, 0.0D);
            default -> new Vec3(0.0D, 0.0D, -1.0D);
        };
    }

    private Rotation rotationForFace(String face) {
        return switch (face) {
            case "south" -> Rotation.CLOCKWISE_180;
            case "west" -> Rotation.COUNTERCLOCKWISE_90;
            case "east" -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
