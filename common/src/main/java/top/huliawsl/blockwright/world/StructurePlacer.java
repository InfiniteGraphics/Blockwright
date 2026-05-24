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

        List<UndoEntry> appliedEntries = new ArrayList<>();
        try {
            for (PlannedBlock block : uniqueBlocks.values()) {
                if (!level.setBlock(block.getPos(), block.getState(), 3)) {
                    throw new IllegalStateException("Failed to place block at " + block.getPos().toShortString());
                }
                applyBlockEntity(level, block);
                UndoEntry undoEntry = findUndoEntry(undoEntries, block.getPos());
                if (undoEntry != null) {
                    appliedEntries.add(undoEntry);
                }
            }
        } catch (Exception exception) {
            undo(level, appliedEntries);
            throw exception;
        }

        return undoEntries;
    }

    public static void undo(ServerLevel level, List<UndoEntry> undoEntries) {
        for (UndoEntry entry : undoEntries) {
            level.setBlock(entry.getPos(), entry.getOldState(), 3);
            applyBlockEntity(level, entry.getPos(), entry.getOldState(), entry.getOldBlockEntityTag());
        }
    }

    private static void applyBlockEntity(ServerLevel level, PlannedBlock block) {
        applyBlockEntity(level, block.getPos(), block.getState(), block.getBlockEntityTag());
    }

    private static void applyBlockEntity(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, CompoundTag tag) {
        if (tag == null) {
            BlockEntity existing = level.getBlockEntity(pos);
            if (existing != null) {
                existing.setRemoved();
                level.removeBlockEntity(pos);
            }
            return;
        }
        CompoundTag copy = tag.copy();
        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());
        BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, copy);
        if (blockEntity == null) {
            return;
        }
        blockEntity.setLevel(level);
        level.setBlockEntity(blockEntity);
        blockEntity.setChanged();
    }

    private static UndoEntry findUndoEntry(List<UndoEntry> undoEntries, BlockPos pos) {
        for (UndoEntry undoEntry : undoEntries) {
            if (undoEntry.getPos().equals(pos)) {
                return undoEntry;
            }
        }
        return null;
    }
}
