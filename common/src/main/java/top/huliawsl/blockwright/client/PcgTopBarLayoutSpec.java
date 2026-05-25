package top.huliawsl.blockwright.client;

import net.minecraft.client.gui.Font;

record PcgTopBarLayoutSpec(int buttonY, int actionGap, int smallWidth, int previewWidth, int regenerateWidth,
                           int exitWidth, int actionStart, int clusterWidth, int packX, int modeX, int modeWidth,
                           int presetX, String title, String previewLabel, String regenerateLabel) {

    static PcgTopBarLayoutSpec compute(PcgUiMetrics metrics, int screenWidth, int topBarX, int topBarRight, Font font) {
        int buttonY = metrics.gap + (metrics.topBarHeight - metrics.topButtonHeight) / 2;
        boolean compact = screenWidth < 1500;
        String title = compact ? "PCG" : "PCG EDITOR";
        int actionGap = screenWidth < 1180 ? metrics.gap + 1 : compact ? metrics.gap + 2 : metrics.gap + 4;
        int exitWidth = clamp(screenWidth / 18, metrics.unit * 5, metrics.unit * 6 + 4);
        int smallWidth = clamp(screenWidth / 16, metrics.unit * 5, metrics.unit * 7);
        int previewWidth = clamp(screenWidth / 13, metrics.unit * 6, metrics.unit * 8 + 8);
        int regenerateWidth = clamp(screenWidth / 11, metrics.unit * 7, metrics.unit * 10);
        String previewLabel = compact ? "Prev" : "Preview";
        String regenerateLabel = compact ? "Regen" : "Regenerate";
        int actionsWidth = previewWidth + regenerateWidth + exitWidth + smallWidth * 4 + actionGap * 6;
        int actionStart = topBarRight - metrics.inset - actionsWidth;
        int titleEnd = topBarX + metrics.inset + font.width("BW") + metrics.gap + font.width(title) + metrics.gap * 3;
        if (actionStart - titleEnd < metrics.unit * 28) {
            title = "BW";
            titleEnd = topBarX + metrics.inset + font.width("BW") + metrics.gap * 3 + font.width(title);
        }
        int chipGap = metrics.gap * 4;
        int chipStart = titleEnd + metrics.gap * 4;
        int chipAvailable = Math.max(metrics.unit * 18, actionStart - chipStart - metrics.gap * 4);
        int modeWidth = clamp(chipAvailable / 4, metrics.unit * 6, metrics.unit * 10);
        int clusterWidth = Math.max(metrics.unit * 6, (chipAvailable - modeWidth - chipGap * 2) / 2);
        int packX = chipStart;
        int modeX = packX + clusterWidth + chipGap;
        int presetX = modeX + modeWidth + chipGap;
        return new PcgTopBarLayoutSpec(buttonY, actionGap, smallWidth, previewWidth, regenerateWidth,
                exitWidth, actionStart, clusterWidth, packX, modeX, modeWidth, presetX,
                title, previewLabel, regenerateLabel);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
