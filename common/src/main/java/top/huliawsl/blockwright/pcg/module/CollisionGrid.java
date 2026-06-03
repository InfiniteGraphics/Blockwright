package top.huliawsl.blockwright.pcg.module;

import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollisionGrid {
    private final List<AABB> occupied = new ArrayList<>();

    public boolean intersects(AABB bounds) {
        for (AABB existing : occupied) {
            if (existing.intersects(bounds)) {
                return true;
            }
        }
        return false;
    }

    public boolean addIfFree(AABB bounds) {
        if (intersects(bounds)) {
            return false;
        }
        occupied.add(bounds);
        return true;
    }

    public void add(AABB bounds) {
        occupied.add(bounds);
    }

    public List<AABB> getOccupied() {
        return Collections.unmodifiableList(occupied);
    }
}
