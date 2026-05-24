package top.huliawsl.blockwright.preset.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PresetDefinition {
    public String id;
    public String name;
    public String type;
    public List<PresetInputDefinition> inputs = new ArrayList<>();
    public Map<String, PresetParameterDefinition> parameters = new LinkedHashMap<>();
    public String rule;
    public transient Path sourcePath;
}
