package top.huliawsl.blockwright.session;

import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;
import top.huliawsl.blockwright.world.UndoEntry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PlayerSession {
    private final BoxRegionSelection regionSelection = new BoxRegionSelection();
    private final SplineSelection splineSelection = new SplineSelection();
    private final Deque<List<UndoEntry>> undoHistory = new ArrayDeque<>();
    private PreviewPlan previewPlan;

    public BoxRegionSelection getRegionSelection() {
        return regionSelection;
    }

    public SplineSelection getSplineSelection() {
        return splineSelection;
    }

    public PreviewPlan getPreviewPlan() {
        return previewPlan;
    }

    public void setPreviewPlan(PreviewPlan previewPlan) {
        this.previewPlan = previewPlan;
    }

    public void pushUndo(List<UndoEntry> entries, int maxHistory) {
        undoHistory.push(new ArrayList<>(entries));
        while (undoHistory.size() > maxHistory) {
            undoHistory.removeLast();
        }
    }

    public List<UndoEntry> popUndo() {
        return undoHistory.pollFirst();
    }
}
