package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PcgVolume {
    private final BlockPos min;
    private final BlockPos max;
    private final String label;
    private final Map<String, JsonElement> attributes;

    public PcgVolume(BlockPos min, BlockPos max, String label) {
        this(min, max, label, Map.of());
    }

    public PcgVolume(BlockPos min, BlockPos max, String label, Map<String, JsonElement> attributes) {
        this.min = min.immutable();
        this.max = max.immutable();
        this.label = label == null ? "" : label;
        this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, JsonElement> getAttributes() {
        return attributes;
    }

    public PcgVolume withBounds(BlockPos newMin, BlockPos newMax) {
        return new PcgVolume(newMin, newMax, label, attributes);
    }

    public PcgVolume withLabel(String newLabel) {
        return new PcgVolume(min, max, newLabel, attributes);
    }

    public PcgVolume withAttributes(Map<String, JsonElement> newAttributes) {
        return new PcgVolume(min, max, label, newAttributes);
    }

    public PcgVolume withAttribute(String key, JsonElement value) {
        Map<String, JsonElement> nextAttributes = new LinkedHashMap<>(attributes);
        nextAttributes.put(key, value);
        return new PcgVolume(min, max, label, nextAttributes);
    }

    public int getWidth() {
        return max.getX() - min.getX() + 1;
    }

    public int getHeight() {
        return max.getY() - min.getY() + 1;
    }

    public int getDepth() {
        return max.getZ() - min.getZ() + 1;
    }

    public AABB toAabb() {
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }
}
