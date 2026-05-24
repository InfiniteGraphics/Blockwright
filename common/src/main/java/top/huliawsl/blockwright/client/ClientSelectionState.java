package top.huliawsl.blockwright.client;

import net.minecraft.core.BlockPos;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;

public final class ClientSelectionState {
    private static final String REGION_SET_PREFIX = "blockwright region set ";
    private static final String SPLINE_ADDPOS_PREFIX = "blockwright spline addpos ";
    private static final String SPLINE_REMOVE_PREFIX = "blockwright spline remove ";
    private static final BoxRegionSelection REGION_SELECTION = new BoxRegionSelection();
    private static final SplineSelection SPLINE_SELECTION = new SplineSelection();

    private ClientSelectionState() {
    }

    public static BoxRegionSelection getRegionSelection() {
        return REGION_SELECTION;
    }

    public static SplineSelection getSplineSelection() {
        return SPLINE_SELECTION;
    }

    public static void clearRegion() {
        REGION_SELECTION.clear();
    }

    public static void clearSpline() {
        SPLINE_SELECTION.clear();
    }

    public static void clearAll() {
        clearRegion();
        clearSpline();
    }

    public static void captureCommand(String command, BlockPos playerPos) {
        if (command == null || command.isBlank()) {
            return;
        }
        if ("blockwright region clear".equals(command)) {
            REGION_SELECTION.clear();
            ClientPreviewState.markStale();
            return;
        }
        if ("blockwright spline clear".equals(command)) {
            SPLINE_SELECTION.clear();
            ClientPreviewState.markStale();
            return;
        }
        if ("blockwright preview clear".equals(command)) {
            ClientPreviewState.clear();
            return;
        }
        if ("blockwright bake".equals(command)) {
            ClientPreviewState.clear();
            return;
        }
        if (playerPos != null) {
            if ("blockwright region pos1".equals(command)) {
                REGION_SELECTION.setPos1(playerPos);
                ClientPreviewState.markStale();
                return;
            }
            if ("blockwright region pos2".equals(command)) {
                REGION_SELECTION.setPos2(playerPos);
                ClientPreviewState.markStale();
                return;
            }
            if ("blockwright spline add".equals(command)) {
                SPLINE_SELECTION.addPoint(playerPos);
                ClientPreviewState.markStale();
                return;
            }
        }
        if (command.startsWith(REGION_SET_PREFIX)) {
            captureRegionSet(command);
            ClientPreviewState.markStale();
            return;
        }
        if (command.startsWith(SPLINE_ADDPOS_PREFIX)) {
            captureSplineAddPos(command);
            ClientPreviewState.markStale();
            return;
        }
        if (command.startsWith(SPLINE_REMOVE_PREFIX)) {
            captureSplineRemove(command);
            ClientPreviewState.markStale();
        }
    }

    private static void captureRegionSet(String command) {
        String[] parts = command.substring(REGION_SET_PREFIX.length()).trim().split("\\s+");
        if (parts.length != 6) {
            return;
        }

        try {
            REGION_SELECTION.setPos1(new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            ));
            REGION_SELECTION.setPos2(new BlockPos(
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5])
            ));
        } catch (NumberFormatException ignored) {
        }
    }

    private static void captureSplineAddPos(String command) {
        String[] parts = command.substring(SPLINE_ADDPOS_PREFIX.length()).trim().split("\\s+");
        if (parts.length != 3) {
            return;
        }

        try {
            SPLINE_SELECTION.addPoint(new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            ));
        } catch (NumberFormatException ignored) {
        }
    }

    private static void captureSplineRemove(String command) {
        String rawIndex = command.substring(SPLINE_REMOVE_PREFIX.length()).trim();
        if (rawIndex.isEmpty()) {
            return;
        }

        try {
            SPLINE_SELECTION.removeIndex(Integer.parseInt(rawIndex));
        } catch (NumberFormatException ignored) {
        }
    }
}
