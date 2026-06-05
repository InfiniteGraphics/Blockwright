package top.huliawsl.blockwright.client;

final class PcgUiMetrics {
    final int unit;
    final int gap;
    final int inset;
    final int topBarHeight;
    final int defaultBottomBarHeight;
    final int topButtonHeight;
    final int fieldHeight;
    final int rowHeight;
    final int moduleRowHeight;
    final int logRowHeight;
    final int splitterSize;
    final int leftBarMinWidth;
    final int leftBarMaxWidth;
    final int minDetailsWidth;
    final int minPreviewWidth;
    final int maxPreviewWidth;
    final int minBottomBarHeight;
    final int maxBottomBarHeight;

    private PcgUiMetrics(int unit, int gap, int inset, int topBarHeight, int defaultBottomBarHeight, int topButtonHeight,
                         int fieldHeight, int rowHeight, int moduleRowHeight, int logRowHeight, int splitterSize,
                         int leftBarMinWidth, int leftBarMaxWidth, int minDetailsWidth, int minPreviewWidth,
                         int maxPreviewWidth, int minBottomBarHeight, int maxBottomBarHeight) {
        this.unit = unit;
        this.gap = gap;
        this.inset = inset;
        this.topBarHeight = topBarHeight;
        this.defaultBottomBarHeight = defaultBottomBarHeight;
        this.topButtonHeight = topButtonHeight;
        this.fieldHeight = fieldHeight;
        this.rowHeight = rowHeight;
        this.moduleRowHeight = moduleRowHeight;
        this.logRowHeight = logRowHeight;
        this.splitterSize = splitterSize;
        this.leftBarMinWidth = leftBarMinWidth;
        this.leftBarMaxWidth = leftBarMaxWidth;
        this.minDetailsWidth = minDetailsWidth;
        this.minPreviewWidth = minPreviewWidth;
        this.maxPreviewWidth = maxPreviewWidth;
        this.minBottomBarHeight = minBottomBarHeight;
        this.maxBottomBarHeight = maxBottomBarHeight;
    }

    static PcgUiMetrics from(int screenWidth, int screenHeight, int lineHeight) {
        int unit = Math.max(10, lineHeight + 3);
        int gap = Math.max(1, unit / 6);
        int inset = unit - 2;
        int topButtonHeight = unit * 2;
        int topBarHeight = unit * 4;
        int defaultBottomBarHeight = unit * 5;
        int fieldHeight = unit * 2;
        int rowHeight = unit * 2 + unit / 2;
        int moduleRowHeight = rowHeight;
        int logRowHeight = lineHeight + unit / 2;
        int splitterSize = Math.max(6, unit / 2);
        int leftBarMinWidth = Math.max(unit * 8, screenWidth / 15);
        int leftBarMaxWidth = Math.max(leftBarMinWidth + unit, screenWidth / 10);
        int minDetailsWidth = Math.max(unit * 18, screenWidth / 8);
        int minPreviewWidth = Math.max(unit * 14, screenWidth / 12);
        int maxPreviewWidth = Math.max(minPreviewWidth + unit * 2, screenWidth / 6);
        int minBottomBarHeight = Math.max(unit * 4, screenHeight / 16);
        int maxBottomBarHeight = Math.max(minBottomBarHeight + unit * 3, screenHeight / 5);
        return new PcgUiMetrics(unit, gap, inset, topBarHeight, defaultBottomBarHeight, topButtonHeight,
                fieldHeight, rowHeight, moduleRowHeight, logRowHeight, splitterSize,
                leftBarMinWidth, leftBarMaxWidth, minDetailsWidth, minPreviewWidth,
                maxPreviewWidth, minBottomBarHeight, maxBottomBarHeight);
    }
}
