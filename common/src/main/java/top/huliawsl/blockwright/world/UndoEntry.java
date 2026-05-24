package top.huliawsl.blockwright.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class UndoEntry {
    private final BlockPos pos;
    private final BlockState oldState;
    private final CompoundTag oldBlockEntityTag;

    public UndoEntry(BlockPos pos, BlockState oldState, CompoundTag oldBlockEntityTag) {
        this.pos = pos.immutable();
        this.oldState = oldState;
        this.oldBlockEntityTag = oldBlockEntityTag == null ? null : oldBlockEntityTag.copy();
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getOldState() {
        return oldState;
    }

    public CompoundTag getOldBlockEntityTag() {
        return oldBlockEntityTag == null ? null : oldBlockEntityTag.copy();
    }
}
