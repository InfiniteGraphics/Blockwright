package top.huliawsl.blockwright.preview;

public final class PreviewNodeSummary {
    private final String nodeId;
    private final String type;
    private final int order;
    private final int inputCount;
    private final int pointCount;
    private final int volumeCount;
    private final int plannedBlockDelta;

    public PreviewNodeSummary(String nodeId, String type, int order, int inputCount, int pointCount, int volumeCount, int plannedBlockDelta) {
        this.nodeId = nodeId == null ? "" : nodeId;
        this.type = type == null ? "" : type;
        this.order = order;
        this.inputCount = Math.max(0, inputCount);
        this.pointCount = Math.max(0, pointCount);
        this.volumeCount = Math.max(0, volumeCount);
        this.plannedBlockDelta = Math.max(0, plannedBlockDelta);
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getType() {
        return type;
    }

    public int getOrder() {
        return order;
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getPointCount() {
        return pointCount;
    }

    public int getVolumeCount() {
        return volumeCount;
    }

    public int getPlannedBlockDelta() {
        return plannedBlockDelta;
    }
}
