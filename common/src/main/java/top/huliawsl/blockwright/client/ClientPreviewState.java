package top.huliawsl.blockwright.client;

import top.huliawsl.blockwright.preview.PreviewPlan;

public final class ClientPreviewState {
    private static PreviewPlan previewPlan;

    private ClientPreviewState() {
    }

    public static PreviewPlan getPreviewPlan() {
        return previewPlan;
    }

    public static void setPreviewPlan(PreviewPlan previewPlan) {
        ClientPreviewState.previewPlan = previewPlan;
    }

    public static void markStale() {
        if (previewPlan != null) {
            previewPlan.setStale(true);
        }
    }

    public static void clear() {
        previewPlan = null;
    }
}
