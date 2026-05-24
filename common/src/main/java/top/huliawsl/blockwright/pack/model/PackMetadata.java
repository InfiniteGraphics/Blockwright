package top.huliawsl.blockwright.pack.model;

import java.util.ArrayList;
import java.util.List;

public final class PackMetadata {
    public String id;
    public String name;
    public String version;
    public String author;
    public String description;
    public List<String> minecraftVersions = new ArrayList<>();
    public List<String> requiredMods = new ArrayList<>();
    public List<String> optionalMods = new ArrayList<>();
    public PackEntrypoints entrypoints = new PackEntrypoints();
}
