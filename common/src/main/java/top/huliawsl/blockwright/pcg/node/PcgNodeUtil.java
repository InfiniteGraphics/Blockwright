package top.huliawsl.blockwright.pcg.node;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;

import java.util.List;
import java.util.Map;

final class PcgNodeUtil {
    private PcgNodeUtil() {
    }

    static PcgData primaryInput(Map<String, PcgData> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return PcgData.empty();
        }
        return PcgData.merge(List.copyOf(inputs.values()));
    }

    static Vec3 horizontalNormal(Vec3 tangent) {
        if (tangent == null) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 horizontal = new Vec3(tangent.x, 0.0D, tangent.z);
        if (horizontal.lengthSqr() < 1.0E-8D) {
            return new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 normalized = horizontal.normalize();
        return new Vec3(-normalized.z, 0.0D, normalized.x);
    }

    static Rotation rotationFromTangent(Vec3 tangent) {
        if (tangent == null) {
            return Rotation.NONE;
        }
        double absX = Math.abs(tangent.x);
        double absZ = Math.abs(tangent.z);
        if (absX > absZ) {
            return tangent.x >= 0.0D ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        }
        return tangent.z >= 0.0D ? Rotation.NONE : Rotation.CLOCKWISE_180;
    }

    static long mixSeed(long seed, int salt) {
        long value = seed ^ (long) salt * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
