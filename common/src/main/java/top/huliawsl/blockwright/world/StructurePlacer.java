package top.huliawsl.blockwright.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import top.huliawsl.blockwright.preview.PlannedBlock;
import top.huliawsl.blockwright.preview.PreviewPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StructurePlacer {
    private StructurePlacer() {
    }

    public static List<UndoEntry> bake(ServerLevel level, PreviewPlan plan) {
        Map<BlockPos, PlannedBlock> uniqueBlocks = new LinkedHashMap<>();
        for (PlannedBlock block : plan.getPlannedBlocks()) {
            uniqueBlocks.put(block.getPos(), block);
        }

        List<UndoEntry> undoEntries = new ArrayList<>();
        for (BlockPos pos : uniqueBlocks.keySet()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            CompoundTag tag = blockEntity == null ? null : blockEntity.saveWithFullMetadata();
            undoEntries.add(new UndoEntry(pos, level.getBlockState(pos), tag));
        }

        for (PlannedBlock block : uniqueBlocks.values()) {
            level.setBlock(block.getPos(), block.getState(), 3);
        }

        return undoEntries;
    }

    public static void undo(ServerLevel level, List<UndoEntry> undoEntries) {
        for (UndoEntry entry : undoEntries) {
            level.setBlock(entry.getPos(), entry.getOldState(), 3);
            CompoundTag tag = entry.getOldBlockEntityTag();
            if (tag != null) {
                BlockEntity blockEntity = level.getBlockEntity(entry.getPos());
                if (blockEntity != null) {
                    blockEntity.load(tag);
                    blockEntity.setChanged();
                }
            }
        }
    }
}
