package top.huliawsl.blockwright.client;

public enum PcgEditorAxis {
    NONE(0, 0, 0),
    X(1, 0, 0),
    Y(0, 1, 0),
    Z(0, 0, 1);

    private final int stepX;
    private final int stepY;
    private final int stepZ;

    PcgEditorAxis(int stepX, int stepY, int stepZ) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }

    public int stepX() {
        return stepX;
    }

    public int stepY() {
        return stepY;
    }

    public int stepZ() {
        return stepZ;
    }
}
