package top.huliawsl.blockwright.client;

import net.minecraft.core.BlockPos;
import top.huliawsl.blockwright.selection.BoxRegionSelection;

public final class ClientSelectionState {
    private static final String REGION_SET_PREFIX = "blockwright region set ";
    private static final BoxRegionSelection REGION_SELECTION = new BoxRegionSelection();

    private ClientSelectionState() {
    }

    public static BoxRegionSelection getRegionSelection() {
        return REGION_SELECTION;
    }

    public static void clearRegion() {
        REGION_SELECTION.clear();
    }

    public static void captureCommand(String command, BlockPos playerPos) {
        if (command == null || command.isBlank()) {
            return;
        }
        if ("blockwright region clear".equals(command)) {
            REGION_SELECTION.clear();
            return;
        }
        if (playerPos != null) {
            if ("blockwright region pos1".equals(command)) {
                REGION_SELECTION.setPos1(playerPos);
                return;
            }
            if ("blockwright region pos2".equals(command)) {
                REGION_SELECTION.setPos2(playerPos);
                return;
            }
        }
        if (!command.startsWith(REGION_SET_PREFIX)) {
            return;
        }

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
}
