package top.huliawsl.blockwright.preset.model;

import com.google.gson.JsonElement;

public final class PresetParameterDefinition {
    public String type;
    public JsonElement defaultValue;
    public Double min;
    public Double max;
    public boolean exposed;
}
