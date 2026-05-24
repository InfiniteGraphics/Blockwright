package top.huliawsl.blockwright.selection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class BoxRegionSelection {
    private BlockPos pos1;
    private BlockPos pos2;

    public void setPos1(BlockPos pos1) {
        this.pos1 = pos1 == null ? null : pos1.immutable();
    }

    public void setPos2(BlockPos pos2) {
        this.pos2 = pos2 == null ? null : pos2.immutable();
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public BlockPos getMin() {
        if (!isComplete()) {
            return null;
        }
        return new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    public BlockPos getMax() {
        if (!isComplete()) {
            return null;
        }
        return new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public int getWidth() {
        return isComplete() ? getMax().getX() - getMin().getX() + 1 : 0;
    }

    public int getHeight() {
        return isComplete() ? getMax().getY() - getMin().getY() + 1 : 0;
    }

    public int getDepth() {
        return isComplete() ? getMax().getZ() - getMin().getZ() + 1 : 0;
    }

    public int getVolume() {
        return getWidth() * getHeight() * getDepth();
    }

    public AABB toAabb() {
        if (!isComplete()) {
            return null;
        }
        BlockPos min = getMin();
        BlockPos max = getMax();
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
    }
}
