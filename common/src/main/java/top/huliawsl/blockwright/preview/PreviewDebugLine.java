package top.huliawsl.blockwright.preview;

import net.minecraft.world.phys.Vec3;

public final class PreviewDebugLine {
    private final Vec3 from;
    private final Vec3 to;
    private final String label;
    private final int color;

    public PreviewDebugLine(Vec3 from, Vec3 to, String label, int color) {
        this.from = from;
        this.to = to;
        this.label = label == null ? "" : label;
        this.color = color;
    }

    public Vec3 getFrom() {
        return from;
    }

    public Vec3 getTo() {
        return to;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }
}
