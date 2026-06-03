package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PcgPoint {
    private final Vec3 position;
    private final Vec3 tangent;
    private final Vec3 normal;
    private final Rotation rotation;
    private final double density;
    private final long seed;
    private final Map<String, JsonElement> attributes;

    public PcgPoint(Vec3 position, Vec3 tangent, Vec3 normal, Rotation rotation, double density, long seed,
                    Map<String, JsonElement> attributes) {
        this.position = position;
        this.tangent = normalizeOr(tangent, new Vec3(0.0D, 0.0D, 1.0D));
        this.normal = normalizeOr(normal, new Vec3(1.0D, 0.0D, 0.0D));
        this.rotation = rotation == null ? Rotation.NONE : rotation;
        this.density = density;
        this.seed = seed;
        this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static PcgPoint at(BlockPos pos) {
        return new PcgPoint(Vec3.atCenterOf(pos), new Vec3(0.0D, 0.0D, 1.0D), new Vec3(1.0D, 0.0D, 0.0D),
                Rotation.NONE, 1.0D, 0L, Map.of());
    }

    public Vec3 getPosition() {
        return position;
    }

    public Vec3 getTangent() {
        return tangent;
    }

    public Vec3 getNormal() {
        return normal;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public double getDensity() {
        return density;
    }

    public long getSeed() {
        return seed;
    }

    public Map<String, JsonElement> getAttributes() {
        return attributes;
    }

    public BlockPos blockPos() {
        return new BlockPos(Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z));
    }

    public PcgPoint withFrame(Vec3 newTangent, Vec3 newNormal, Rotation newRotation) {
        return new PcgPoint(position, newTangent, newNormal, newRotation, density, seed, attributes);
    }

    public PcgPoint withPosition(Vec3 newPosition) {
        return new PcgPoint(newPosition, tangent, normal, rotation, density, seed, attributes);
    }

    public PcgPoint withSeed(long newSeed) {
        return new PcgPoint(position, tangent, normal, rotation, density, newSeed, attributes);
    }

    public PcgPoint withDensity(double newDensity) {
        return new PcgPoint(position, tangent, normal, rotation, newDensity, seed, attributes);
    }

    public PcgPoint withAttributes(Map<String, JsonElement> newAttributes) {
        return new PcgPoint(position, tangent, normal, rotation, density, seed, newAttributes);
    }

    public PcgPoint withAttribute(String key, JsonElement value) {
        Map<String, JsonElement> nextAttributes = new LinkedHashMap<>(attributes);
        nextAttributes.put(key, value);
        return new PcgPoint(position, tangent, normal, rotation, density, seed, nextAttributes);
    }

    private static Vec3 normalizeOr(Vec3 value, Vec3 fallback) {
        if (value == null || value.lengthSqr() < 1.0E-8D) {
            return fallback;
        }
        return value.normalize();
    }
}
