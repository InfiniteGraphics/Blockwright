package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;

import java.util.List;
import java.util.Locale;
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

    static JsonElement configuredAttributeValue(PcgGraphContext context, PcgNodeDefinition node) {
        String valueType = context.getNodeString(node, "valueType", "string").toLowerCase(Locale.ROOT);
        return switch (valueType) {
            case "number" -> new JsonPrimitive(context.getNodeDouble(node, "numberValue", 0.0D));
            case "int", "integer" -> new JsonPrimitive(context.getNodeInt(node, "intValue", 0));
            case "bool", "boolean" -> new JsonPrimitive(context.getNodeBoolean(node, "boolValue", false));
            default -> new JsonPrimitive(context.getNodeString(node, "stringValue", ""));
        };
    }

    static JsonElement pointValue(PcgPoint point, String key) {
        if (point == null || key == null || key.isBlank()) {
            return null;
        }
        return switch (key) {
            case "density" -> new JsonPrimitive(point.getDensity());
            case "seed" -> new JsonPrimitive(point.getSeed());
            case "x" -> new JsonPrimitive(point.getPosition().x);
            case "y" -> new JsonPrimitive(point.getPosition().y);
            case "z" -> new JsonPrimitive(point.getPosition().z);
            default -> point.getAttributes().get(key);
        };
    }

    static boolean compare(JsonElement actual, String operation, JsonElement expected) {
        String op = operation == null ? "eq" : operation.toLowerCase(Locale.ROOT);
        if ("exists".equals(op)) {
            return actual != null;
        }
        if ("missing".equals(op)) {
            return actual == null;
        }
        if (actual == null || expected == null) {
            return false;
        }
        if (isNumeric(actual) && isNumeric(expected)) {
            double left = actual.getAsDouble();
            double right = expected.getAsDouble();
            return switch (op) {
                case "ne", "neq" -> Double.compare(left, right) != 0;
                case "gt" -> left > right;
                case "gte", "ge" -> left >= right;
                case "lt" -> left < right;
                case "lte", "le" -> left <= right;
                default -> Double.compare(left, right) == 0;
            };
        }
        if (actual.isJsonPrimitive() && expected.isJsonPrimitive()
                && actual.getAsJsonPrimitive().isBoolean() && expected.getAsJsonPrimitive().isBoolean()) {
            boolean left = actual.getAsBoolean();
            boolean right = expected.getAsBoolean();
            return "ne".equals(op) || "neq".equals(op) ? left != right : left == right;
        }
        String left = actual.getAsString();
        String right = expected.getAsString();
        return "ne".equals(op) || "neq".equals(op) ? !left.equals(right) : left.equals(right);
    }

    private static boolean isNumeric(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }
}
