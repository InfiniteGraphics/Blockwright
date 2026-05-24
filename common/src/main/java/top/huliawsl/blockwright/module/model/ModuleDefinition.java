package top.huliawsl.blockwright.module.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModuleDefinition {
    public String id;
    public String moduleKind;
    public String sizePolicy;
    public String placementRole;
    public String schematic;
    public String category;
    public List<Integer> size = new ArrayList<>();
    public List<String> tags = new ArrayList<>();
    public String style;
    public int weight = 1;
    public List<Integer> allowedRotations = new ArrayList<>();
    public String scaling = "none";
    public List<ModuleConnector> connectors = new ArrayList<>();
    public ModuleConstraints constraints = new ModuleConstraints();
    public String generator;
    public transient Path sourcePath;
}
