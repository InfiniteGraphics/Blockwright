package top.huliawsl.blockwright.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public final class PlannedBlock {
    private final BlockPos pos;
    private final BlockState state;
    private final CompoundTag blockEntityTag;

    public PlannedBlock(BlockPos pos, BlockState state) {
        this(pos, state, null);
    }

    public PlannedBlock(BlockPos pos, BlockState state, CompoundTag blockEntityTag) {
        this.pos = pos.immutable();
        this.state = state;
        this.blockEntityTag = blockEntityTag == null ? null : blockEntityTag.copy();
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }

    public CompoundTag getBlockEntityTag() {
        return blockEntityTag == null ? null : blockEntityTag.copy();
    }
}
