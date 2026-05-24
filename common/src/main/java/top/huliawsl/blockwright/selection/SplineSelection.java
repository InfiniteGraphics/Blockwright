package top.huliawsl.blockwright.selection;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SplineSelection {
    private final List<BlockPos> points = new ArrayList<>();

    public void addPoint(BlockPos pos) {
        points.add(pos.immutable());
    }

    public boolean removeIndex(int index) {
        if (index < 0 || index >= points.size()) {
            return false;
        }
        points.remove(index);
        return true;
    }

    public void clear() {
        points.clear();
    }

    public List<BlockPos> getPoints() {
        return Collections.unmodifiableList(points);
    }
}
