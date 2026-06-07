package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SelectPointNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        String mode = PcgNodeUtil.resolveConfigString(context, node, "mode", "center");
        String groupAttribute = PcgNodeUtil.resolveConfigString(context, node, "groupAttribute", "");
        int count = Math.max(1, PcgNodeUtil.resolveConfigInt(context, node, "count", 1));
        long seed = PcgNodeUtil.resolveConfigLong(context, node, "seed", context.getLongParameter(PcgNodeUtil.resolveConfigString(context, node, "seedParam", "seed"), 0L));
        Map<String, List<PcgPoint>> groups = new LinkedHashMap<>();
        for (PcgPoint point : input.getPoints()) {
            String key = "__all";
            if (!groupAttribute.isBlank()) {
                JsonElement value = PcgNodeUtil.pointValue(point, groupAttribute);
                key = value != null && value.isJsonPrimitive() ? value.getAsString() : "";
            }
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(point);
        }
        List<PcgPoint> selected = new ArrayList<>();
        int groupIndex = 0;
        for (List<PcgPoint> group : groups.values()) {
            selected.addAll(selectGroup(group, mode, count, PcgNodeUtil.mixSeed(seed, groupIndex++)));
        }
        return new PcgData(selected, input.getVolumes());
    }

    private List<PcgPoint> selectGroup(List<PcgPoint> points, String mode, int count, long seed) {
        if (points.isEmpty()) {
            return List.of();
        }
        String normalized = mode == null ? "center" : mode.toLowerCase(java.util.Locale.ROOT);
        if ("all".equals(normalized)) {
            return points;
        }
        if ("first".equals(normalized)) {
            return points.subList(0, Math.min(count, points.size()));
        }
        if ("last".equals(normalized)) {
            int from = Math.max(0, points.size() - count);
            return points.subList(from, points.size());
        }
        if ("random".equals(normalized)) {
            List<PcgPoint> shuffled = new ArrayList<>(points);
            RandomSource random = RandomSource.create(seed);
            for (int i = shuffled.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                PcgPoint tmp = shuffled.get(i);
                shuffled.set(i, shuffled.get(j));
                shuffled.set(j, tmp);
            }
            return shuffled.subList(0, Math.min(count, shuffled.size()));
        }
        Vec3 target = target(points, normalized);
        return points.stream()
                .sorted(Comparator.comparingDouble(point -> point.getPosition().distanceToSqr(target)))
                .limit(count)
                .toList();
    }

    private Vec3 target(List<PcgPoint> points, String mode) {
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (PcgPoint point : points) {
            Vec3 pos = point.getPosition();
            x += pos.x;
            y += pos.y;
            z += pos.z;
            minY = Math.min(minY, pos.y);
            maxY = Math.max(maxY, pos.y);
        }
        int size = Math.max(1, points.size());
        double targetY = y / size;
        if ("center_bottom".equals(mode) || "bottom_center".equals(mode)) {
            targetY = minY;
        } else if ("center_top".equals(mode) || "top_center".equals(mode)) {
            targetY = maxY;
        }
        return new Vec3(x / size, targetY, z / size);
    }
}
