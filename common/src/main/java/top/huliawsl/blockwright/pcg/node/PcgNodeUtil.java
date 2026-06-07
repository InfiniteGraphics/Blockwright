package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;

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

    static JsonElement resolveConfigValue(PcgGraphContext context, PcgNodeDefinition node, String key, JsonElement fallback) {
        JsonElement raw = node.getConfig().has(key) ? node.getConfig().get(key) : fallback;
        return resolveValue(context, raw);
    }

    static JsonElement resolveValue(PcgGraphContext context, JsonElement raw) {
        if (raw == null || raw.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()) {
            return raw;
        }
        String value = raw.getAsString();
        if (value.startsWith("$preset.")) {
            String key = value.substring("$preset.".length());
            String resolved = context.getStringParameter(key, "");
            return new JsonPrimitive(resolved);
        }
        if (value.startsWith("$param.")) {
            String key = value.substring("$param.".length());
            String resolved = context.getStringParameter(key, "");
            return new JsonPrimitive(resolved);
        }
        if (value.startsWith("$") && value.indexOf('{') < 0) {
            String key = value.substring(1);
            String resolved = context.getStringParameter(key, "");
            return new JsonPrimitive(resolved);
        }
        return raw;
    }

    static String resolveConfigString(PcgGraphContext context, PcgNodeDefinition node, String key, String fallback) {
        JsonElement value = resolveConfigValue(context, node, key, new JsonPrimitive(fallback == null ? "" : fallback));
        if (value == null || value.isJsonNull()) {
            return fallback;
        }
        return value.isJsonPrimitive() ? value.getAsString() : fallback;
    }

    static int resolveConfigInt(PcgGraphContext context, PcgNodeDefinition node, String key, int fallback) {
        JsonElement value = resolveConfigValue(context, node, key, new JsonPrimitive(fallback));
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static long resolveConfigLong(PcgGraphContext context, PcgNodeDefinition node, String key, long fallback) {
        JsonElement value = resolveConfigValue(context, node, key, new JsonPrimitive(fallback));
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static double resolveConfigDouble(PcgGraphContext context, PcgNodeDefinition node, String key, double fallback) {
        JsonElement value = resolveConfigValue(context, node, key, new JsonPrimitive(fallback));
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static boolean resolveConfigBoolean(PcgGraphContext context, PcgNodeDefinition node, String key, boolean fallback) {
        JsonElement value = resolveConfigValue(context, node, key, new JsonPrimitive(fallback));
        try {
            return value != null && value.isJsonPrimitive() ? value.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static String interpolatePoint(PcgGraphContext context, String template, PcgPoint point) {
        return interpolate(template, key -> {
            if (key.startsWith("preset.")) {
                return context.getStringParameter(key.substring("preset.".length()), "");
            }
            if (key.startsWith("param.")) {
                return context.getStringParameter(key.substring("param.".length()), "");
            }
            JsonElement value = pointValue(point, key);
            return value == null || value.isJsonNull() ? "" : value.getAsString();
        });
    }

    static String interpolateVolume(PcgGraphContext context, String template, PcgVolume volume) {
        return interpolate(template, key -> {
            if (key.startsWith("preset.")) {
                return context.getStringParameter(key.substring("preset.".length()), "");
            }
            if (key.startsWith("param.")) {
                return context.getStringParameter(key.substring("param.".length()), "");
            }
            JsonElement value = volumeValue(volume, key);
            return value == null || value.isJsonNull() ? "" : value.getAsString();
        });
    }

    static String interpolatePreset(PcgGraphContext context, String template) {
        return interpolate(template, key -> {
            if (key.startsWith("preset.")) {
                return context.getStringParameter(key.substring("preset.".length()), "");
            }
            if (key.startsWith("param.")) {
                return context.getStringParameter(key.substring("param.".length()), "");
            }
            return context.getStringParameter(key, "");
        });
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

    static JsonElement volumeValue(PcgVolume volume, String key) {
        if (volume == null || key == null || key.isBlank()) {
            return null;
        }
        return switch (key) {
            case "label" -> new JsonPrimitive(volume.getLabel());
            case "minX" -> new JsonPrimitive(volume.getMin().getX());
            case "minY" -> new JsonPrimitive(volume.getMin().getY());
            case "minZ" -> new JsonPrimitive(volume.getMin().getZ());
            case "maxX" -> new JsonPrimitive(volume.getMax().getX());
            case "maxY" -> new JsonPrimitive(volume.getMax().getY());
            case "maxZ" -> new JsonPrimitive(volume.getMax().getZ());
            case "width" -> new JsonPrimitive(volume.getWidth());
            case "height" -> new JsonPrimitive(volume.getHeight());
            case "depth" -> new JsonPrimitive(volume.getDepth());
            default -> volume.getAttributes().get(key);
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

    private static String interpolate(String template, Resolver resolver) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (cursor < template.length()) {
            int start = template.indexOf("${", cursor);
            if (start < 0) {
                result.append(template.substring(cursor));
                break;
            }
            result.append(template, cursor, start);
            int end = template.indexOf('}', start + 2);
            if (end < 0) {
                result.append(template.substring(start));
                break;
            }
            String key = template.substring(start + 2, end).trim();
            result.append(resolver.resolve(key));
            cursor = end + 1;
        }
        return result.toString();
    }

    private interface Resolver {
        String resolve(String key);
    }
}
