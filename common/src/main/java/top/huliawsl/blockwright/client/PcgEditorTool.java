package top.huliawsl.blockwright.client;

public enum PcgEditorTool {
    PRESET_LIBRARY("Preset Library"),
    SELECT("Select"),
    BOX_REGION("Box Region"),
    SPLINE("Spline"),
    TRANSFORM("Transform"),
    MODULE_LIBRARY("Module Library"),
    NODE_GRAPH("Node Graph"),
    PAINT_MASK("Paint / Mask");

    private final String title;

    PcgEditorTool(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
