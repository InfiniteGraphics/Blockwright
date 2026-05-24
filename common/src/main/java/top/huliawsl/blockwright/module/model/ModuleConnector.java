package top.huliawsl.blockwright.module.model;

import java.util.ArrayList;
import java.util.List;

public final class ModuleConnector {
    public String id;
    public String type;
    public String direction;
    public List<Integer> offset = new ArrayList<>();
    public List<Integer> size = new ArrayList<>();
    public List<String> tags = new ArrayList<>();
}
