package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.pcg.PcgVolume;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PcgExpressionUtil {
    private PcgExpressionUtil() {
    }

    static JsonElement evaluateToJson(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume, JsonElement fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        String trimmed = expression.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return new JsonPrimitive(trimmed.substring(1, trimmed.length() - 1));
        }
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return new JsonPrimitive(Boolean.parseBoolean(trimmed));
        }
        JsonElement directValue = directLookup(context, trimmed, point, volume);
        if (directValue != null) {
            return directValue;
        }
        try {
            boolean booleanResult = evaluateBoolean(context, trimmed, point, volume, false);
            if (looksBoolean(trimmed)) {
                return new JsonPrimitive(booleanResult);
            }
        } catch (RuntimeException ignored) {
        }
        try {
            double value = evaluateNumber(context, trimmed, point, volume, Double.NaN);
            if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                long rounded = Math.round(value);
                if (Math.abs(value - rounded) < 1.0E-9D) {
                    return new JsonPrimitive(rounded);
                }
                return new JsonPrimitive(value);
            }
        } catch (RuntimeException ignored) {
        }
        if (trimmed.contains("${")) {
            if (point != null) {
                return new JsonPrimitive(PcgNodeUtil.interpolatePoint(context, trimmed, point));
            }
            if (volume != null) {
                return new JsonPrimitive(PcgNodeUtil.interpolateVolume(context, trimmed, volume));
            }
            return new JsonPrimitive(PcgNodeUtil.interpolatePreset(context, trimmed));
        }
        return new JsonPrimitive(trimmed);
    }

    private static JsonElement directLookup(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume) {
        String key = expression.startsWith("$") ? expression.substring(1) : expression;
        if (key.isBlank()) {
            return null;
        }
        boolean simple = key.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_' || ch == '.');
        if (!simple) {
            return null;
        }
        if (key.startsWith("preset.")) {
            String value = context.getStringParameter(key.substring("preset.".length()), null);
            return value == null ? null : new JsonPrimitive(value);
        }
        if (key.startsWith("param.")) {
            String value = context.getStringParameter(key.substring("param.".length()), null);
            return value == null ? null : new JsonPrimitive(value);
        }
        JsonElement value = point != null ? PcgNodeUtil.pointValue(point, key) : null;
        if (value == null && volume != null) {
            value = PcgNodeUtil.volumeValue(volume, key);
        }
        return value;
    }

    static double evaluateNumber(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume, double fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        try {
            String resolved = resolveTemplates(context, expression, point, volume);
            return new Parser(context, resolved, point, volume).parse();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    static boolean evaluateBoolean(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume, boolean fallback) {
        if (expression == null || expression.isBlank()) {
            return fallback;
        }
        try {
            return evalBooleanExpression(context, resolveTemplates(context, expression.trim(), point, volume), point, volume);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String resolveTemplates(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume) {
        if (expression == null || expression.indexOf("${") < 0) {
            return expression;
        }
        if (point != null) {
            return PcgNodeUtil.interpolatePoint(context, expression, point);
        }
        if (volume != null) {
            return PcgNodeUtil.interpolateVolume(context, expression, volume);
        }
        return PcgNodeUtil.interpolatePreset(context, expression);
    }

    private static boolean evalBooleanExpression(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume) {
        List<String> orParts = splitTopLevel(expression, "||");
        if (orParts.size() > 1) {
            for (String part : orParts) {
                if (evalBooleanExpression(context, part, point, volume)) {
                    return true;
                }
            }
            return false;
        }
        List<String> andParts = splitTopLevel(expression, "&&");
        if (andParts.size() > 1) {
            for (String part : andParts) {
                if (!evalBooleanExpression(context, part, point, volume)) {
                    return false;
                }
            }
            return true;
        }
        String compact = stripOuterParens(expression.trim());
        String[] operators = new String[] {">=", "<=", "==", "!=", ">", "<"};
        for (String operator : operators) {
            int index = indexOfTopLevel(compact, operator);
            if (index < 0) {
                continue;
            }
            String leftRaw = compact.substring(0, index).trim();
            String rightRaw = compact.substring(index + operator.length()).trim();
            JsonElement leftValue = evaluateToJson(context, leftRaw, point, volume, new JsonPrimitive(0));
            JsonElement rightValue = evaluateToJson(context, rightRaw, point, volume, new JsonPrimitive(0));
            boolean numeric = isNumeric(leftValue) && isNumeric(rightValue);
            if (numeric) {
                double left = leftValue.getAsDouble();
                double right = rightValue.getAsDouble();
                return switch (operator) {
                    case ">=" -> left >= right;
                    case "<=" -> left <= right;
                    case ">" -> left > right;
                    case "<" -> left < right;
                    case "!=" -> Double.compare(left, right) != 0;
                    default -> Double.compare(left, right) == 0;
                };
            }
            String left = leftValue == null || leftValue.isJsonNull() ? "" : leftValue.getAsString();
            String right = rightValue == null || rightValue.isJsonNull() ? "" : rightValue.getAsString();
            return "!=".equals(operator) ? !left.equals(right) : left.equals(right);
        }
        JsonElement value = evaluateToJson(context, compact, point, volume, new JsonPrimitive(false));
        if (value != null && value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean();
            }
            if (value.getAsJsonPrimitive().isNumber()) {
                return value.getAsDouble() != 0.0D;
            }
            String raw = value.getAsString();
            return "true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw) || "1".equals(raw);
        }
        return false;
    }

    private static boolean looksBoolean(String expression) {
        String compact = expression.toLowerCase(Locale.ROOT);
        return compact.contains("==") || compact.contains("!=") || compact.contains(">=") || compact.contains("<=")
                || compact.contains("&&") || compact.contains("||") || compact.contains(">") || compact.contains("<")
                || "true".equals(compact) || "false".equals(compact);
    }

    private static boolean isNumeric(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }

    private static List<String> splitTopLevel(String expression, String delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i <= expression.length() - delimiter.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            }
            if (depth == 0 && expression.startsWith(delimiter, i)) {
                parts.add(expression.substring(start, i).trim());
                start = i + delimiter.length();
                i += delimiter.length() - 1;
            }
        }
        if (start == 0) {
            parts.add(expression.trim());
        } else {
            parts.add(expression.substring(start).trim());
        }
        return parts;
    }

    private static int indexOfTopLevel(String expression, String operator) {
        int depth = 0;
        for (int i = 0; i <= expression.length() - operator.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                depth = Math.max(0, depth - 1);
            }
            if (depth == 0 && expression.startsWith(operator, i)) {
                if ((">".equals(operator) || "<".equals(operator)) && i + 1 < expression.length() && expression.charAt(i + 1) == '=') {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    private static String stripOuterParens(String expression) {
        String value = expression;
        while (value.startsWith("(") && value.endsWith(")")) {
            int depth = 0;
            boolean wraps = true;
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                    if (depth == 0 && i < value.length() - 1) {
                        wraps = false;
                        break;
                    }
                }
            }
            if (!wraps) {
                break;
            }
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static final class Parser {
        private final PcgGraphContext context;
        private final String expression;
        private final PcgPoint point;
        private final PcgVolume volume;
        private int index;

        private Parser(PcgGraphContext context, String expression, PcgPoint point, PcgVolume volume) {
            this.context = context;
            this.expression = expression;
            this.point = point;
            this.volume = volume;
        }

        double parse() {
            double value = parseExpression();
            skipWhitespace();
            if (index < expression.length()) {
                throw new IllegalArgumentException("Unexpected token at " + index);
            }
            return value;
        }

        private double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value *= parseFactor();
                } else if (match('/')) {
                    value /= parseFactor();
                } else {
                    return value;
                }
            }
        }

        private double parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return -parseFactor();
            }
            if (match('(')) {
                double value = parseExpression();
                expect(')');
                return value;
            }
            if (index < expression.length() && (Character.isDigit(expression.charAt(index)) || expression.charAt(index) == '.')) {
                return parseNumber();
            }
            String name = parseIdentifier();
            if (name.isBlank()) {
                throw new IllegalArgumentException("Expected factor at " + index);
            }
            skipWhitespace();
            if (match('(')) {
                List<Double> args = new ArrayList<>();
                skipWhitespace();
                if (!peek(')')) {
                    do {
                        args.add(parseExpression());
                        skipWhitespace();
                    } while (match(','));
                }
                expect(')');
                return call(name, args);
            }
            return resolveVariable(name);
        }

        private double parseNumber() {
            int start = index;
            while (index < expression.length()) {
                char ch = expression.charAt(index);
                if (!Character.isDigit(ch) && ch != '.') {
                    break;
                }
                index++;
            }
            return Double.parseDouble(expression.substring(start, index));
        }

        private String parseIdentifier() {
            int start = index;
            while (index < expression.length()) {
                char ch = expression.charAt(index);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '$') {
                    index++;
                } else {
                    break;
                }
            }
            return expression.substring(start, index);
        }

        private double resolveVariable(String rawName) {
            String name = rawName.startsWith("$") ? rawName.substring(1) : rawName;
            if (name.startsWith("preset.")) {
                return parseParameter(name.substring("preset.".length()), 0.0D);
            }
            if (name.startsWith("param.")) {
                return parseParameter(name.substring("param.".length()), 0.0D);
            }
            JsonElement value = point != null ? PcgNodeUtil.pointValue(point, name) : null;
            if (value == null && volume != null) {
                value = PcgNodeUtil.volumeValue(volume, name);
            }
            if (value != null && value.isJsonPrimitive()) {
                if (value.getAsJsonPrimitive().isNumber()) {
                    return value.getAsDouble();
                }
                if (value.getAsJsonPrimitive().isBoolean()) {
                    return value.getAsBoolean() ? 1.0D : 0.0D;
                }
                try {
                    return Double.parseDouble(value.getAsString());
                } catch (NumberFormatException ignored) {
                    return 0.0D;
                }
            }
            return parseParameter(name, 0.0D);
        }

        private double parseParameter(String key, double fallback) {
            String raw = context.getStringParameter(key, null);
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private double call(String rawName, List<Double> args) {
            String name = rawName.toLowerCase(Locale.ROOT);
            return switch (name) {
                case "min" -> args.stream().mapToDouble(Double::doubleValue).min().orElse(0.0D);
                case "max" -> args.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D);
                case "clamp" -> {
                    double value = args.size() > 0 ? args.get(0) : 0.0D;
                    double min = args.size() > 1 ? args.get(1) : value;
                    double max = args.size() > 2 ? args.get(2) : value;
                    yield Math.max(min, Math.min(max, value));
                }
                case "floor" -> Math.floor(args.isEmpty() ? 0.0D : args.get(0));
                case "ceil" -> Math.ceil(args.isEmpty() ? 0.0D : args.get(0));
                case "round" -> Math.round(args.isEmpty() ? 0.0D : args.get(0));
                case "abs" -> Math.abs(args.isEmpty() ? 0.0D : args.get(0));
                default -> 0.0D;
            };
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (index < expression.length() && expression.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < expression.length() && expression.charAt(index) == expected;
        }

        private void expect(char expected) {
            if (!match(expected)) {
                throw new IllegalArgumentException("Expected " + expected + " at " + index);
            }
        }

        private void skipWhitespace() {
            while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
        }
    }
}
