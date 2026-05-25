package top.huliawsl.blockwright.client;

final class PcgInspectorLayout {
    private PcgInspectorLayout() {
    }

    static int transformSectionHeight(PcgUiMetrics metrics, PcgEditorSelection selection) {
        if (selection == PcgEditorSelection.REGION || selection == PcgEditorSelection.SPLINE_POINT) {
            return metrics.unit * 8 - 4;
        }
        if (selection == PcgEditorSelection.SPLINE) {
            return metrics.unit * 4 + 4;
        }
        return metrics.unit * 4 - 4;
    }

    static int parameterRowsHeight(PcgUiMetrics metrics, int rows) {
        return Math.max(metrics.topButtonHeight, rows == 0 ? metrics.topButtonHeight : rows * metrics.rowHeight);
    }

    static int validationContentHeight(PcgUiMetrics metrics, int warningCount) {
        return warningCount == 0 ? metrics.unit * 3 - 2 : metrics.unit * 2 + Math.min(3, warningCount) * (metrics.unit + 1);
    }

    static int measureContentHeight(PcgUiMetrics metrics, PcgEditorSelection selection, int parameterRows, int warningCount) {
        return metrics.inset
                + metrics.unit * 5
                + metrics.unit * 3 + transformSectionHeight(metrics, selection)
                + metrics.unit * 3 + metrics.unit * 3
                + metrics.unit * 3 + metrics.unit * 3
                + metrics.unit * 3 + parameterRowsHeight(metrics, parameterRows)
                + metrics.unit * 3 + validationContentHeight(metrics, warningCount)
                + metrics.unit * 3 + metrics.topButtonHeight + metrics.gap * 4;
    }
}
