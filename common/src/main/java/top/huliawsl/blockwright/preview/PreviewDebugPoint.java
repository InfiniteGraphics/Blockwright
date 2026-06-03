package top.huliawsl.blockwright.preview;

import net.minecraft.world.phys.Vec3;

public final class PreviewDebugPoint {
    private final Vec3 position;
    private final String label;
    private final int color;

    public PreviewDebugPoint(Vec3 position, String label, int color) {
        this.position = position;
        this.label = label == null ? "" : label;
        this.color = color;
    }

    public Vec3 getPosition() {
        return position;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }
}
