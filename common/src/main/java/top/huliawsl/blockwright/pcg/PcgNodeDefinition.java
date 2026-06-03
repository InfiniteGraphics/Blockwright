package top.huliawsl.blockwright.pcg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public final class PcgNodeDefinition {
    private final String id;
    private final String type;
    private final JsonObject config;

    public PcgNodeDefinition(String id, String type, JsonObject config) {
        this.id = id;
        this.type = type;
        this.config = config == null ? new JsonObject() : config;
    }

    public static PcgNodeDefinition fromJson(JsonObject object) {
        String id = object.has("id") ? object.get("id").getAsString() : "";
        String type = object.has("type") ? object.get("type").getAsString() : "";
        JsonObject config = new JsonObject();
        if (object.has("config") && object.get("config").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("config").entrySet()) {
                config.add(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (!"id".equals(key) && !"type".equals(key) && !"config".equals(key)) {
                config.add(key, entry.getValue());
            }
        }
        return new PcgNodeDefinition(id, type, config);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public JsonObject getConfig() {
        return config;
    }
}
