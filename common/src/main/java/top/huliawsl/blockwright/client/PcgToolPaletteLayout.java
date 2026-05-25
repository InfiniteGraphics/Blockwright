package top.huliawsl.blockwright.client;

final class PcgToolPaletteLayout {
    private PcgToolPaletteLayout() {
    }

    static int computeButtonHeight(PcgUiMetrics metrics, int leftBarHeight, int toolCount) {
        int available = leftBarHeight - metrics.inset * 2 - (toolCount - 1) * (metrics.gap * 3);
        return clamp(available / Math.max(1, toolCount), metrics.unit * 4, metrics.unit * 5 + 2);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
