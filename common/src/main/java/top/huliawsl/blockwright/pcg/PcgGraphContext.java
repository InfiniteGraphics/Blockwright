package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.world.BlockResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PcgGraphContext {
    private final PresetExecutionContext executionContext;
    private final PreviewPlan plan;
    private final Map<String, PcgData> nodeOutputs = new LinkedHashMap<>();
    private boolean debugEnabled;

    public PcgGraphContext(PresetExecutionContext executionContext, PreviewPlan plan) {
        this.executionContext = executionContext;
        this.plan = plan;
    }

    public PresetExecutionContext getExecutionContext() {
        return executionContext;
    }

    public PreviewPlan getPlan() {
        return plan;
    }

    public Map<String, ModuleDefinition> getModules() {
        return executionContext.getModules();
    }

    public Optional<ServerLevel> getServerLevel() {
        if (executionContext.getPlayer() == null) {
            return Optional.empty();
        }
        return Optional.of(executionContext.getPlayer().serverLevel());
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public void putNodeOutput(String nodeId, PcgData data) {
        nodeOutputs.put(nodeId, data == null ? PcgData.empty() : data);
    }

    public Map<String, PcgData> getNodeOutputs() {
        return Map.copyOf(nodeOutputs);
    }

    public int getIntParameter(String key, int fallback) {
        String value = getStringParameter(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public long getLongParameter(String key, long fallback) {
        String value = getStringParameter(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public double getDoubleParameter(String key, double fallback) {
        String value = getStringParameter(key, null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public String getStringParameter(String key, String fallback) {
        String override = executionContext.getOverrides().get(key);
        if (override != null && !override.isBlank()) {
            return override;
        }
        if (executionContext.getPreset().parameters.containsKey(key)) {
            JsonElement defaultValue = executionContext.getPreset().parameters.get(key).defaultValue;
            if (defaultValue != null && defaultValue.isJsonPrimitive()) {
                return defaultValue.getAsString();
            }
        }
        return fallback;
    }

    public Optional<BlockState> resolveBlockParameter(String key, String fallback) {
        String blockId = getStringParameter(key, fallback);
        return BlockResolver.resolve(blockId);
    }

    public String getRuleConfigString(String key, String fallback) {
        JsonObject config = executionContext.getRule().config;
        if (config != null && config.has(key) && config.get(key).isJsonPrimitive()) {
            return config.get(key).getAsString();
        }
        return fallback;
    }

    public boolean getRuleConfigBoolean(String key, boolean fallback) {
        JsonObject config = executionContext.getRule().config;
        if (config != null && config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return config.get(key).getAsBoolean();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public String getNodeString(PcgNodeDefinition node, String key, String fallback) {
        JsonObject config = node.getConfig();
        if (config.has(key) && config.get(key).isJsonPrimitive()) {
            return resolveParameterReference(config.get(key).getAsString(), fallback);
        }
        return fallback;
    }

    public boolean getNodeBoolean(PcgNodeDefinition node, String key, boolean fallback) {
        JsonObject config = node.getConfig();
        if (config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return Boolean.parseBoolean(resolveParameterReference(config.get(key).getAsString(), Boolean.toString(fallback)));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public int getNodeInt(PcgNodeDefinition node, String key, int fallback) {
        JsonObject config = node.getConfig();
        if (config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return Integer.parseInt(resolveParameterReference(config.get(key).getAsString(), Integer.toString(fallback)));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public long getNodeLong(PcgNodeDefinition node, String key, long fallback) {
        JsonObject config = node.getConfig();
        if (config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return Long.parseLong(resolveParameterReference(config.get(key).getAsString(), Long.toString(fallback)));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public double getNodeDouble(PcgNodeDefinition node, String key, double fallback) {
        JsonObject config = node.getConfig();
        if (config.has(key) && config.get(key).isJsonPrimitive()) {
            try {
                return Double.parseDouble(resolveParameterReference(config.get(key).getAsString(), Double.toString(fallback)));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String resolveParameterReference(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw.startsWith("$preset.")) {
            return getStringParameter(raw.substring("$preset.".length()), fallback);
        }
        if (raw.startsWith("$param.")) {
            return getStringParameter(raw.substring("$param.".length()), fallback);
        }
        if (raw.startsWith("$") && raw.indexOf('{') < 0) {
            return getStringParameter(raw.substring(1), fallback);
        }
        return raw;
    }
}
