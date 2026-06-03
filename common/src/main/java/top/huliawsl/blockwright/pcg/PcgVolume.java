package top.huliawsl.blockwright.pcg;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class PcgVolume {
    private final BlockPos min;
    private final BlockPos max;
    private final String label;

    public PcgVolume(BlockPos min, BlockPos max, String label) {
        this.min = min.immutable();
        this.max = max.immutable();
        this.label = label == null ? "" : label;
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
