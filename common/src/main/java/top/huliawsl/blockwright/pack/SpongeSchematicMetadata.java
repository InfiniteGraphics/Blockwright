package top.huliawsl.blockwright.pack;

public final class SpongeSchematicMetadata {
    private final int width;
    private final int height;
    private final int length;

    public SpongeSchematicMetadata(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }
}
