package top.huliawsl.blockwright.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class PlannedBlock {
    private final BlockPos pos;
    private final BlockState state;

    public PlannedBlock(BlockPos pos, BlockState state) {
        this.pos = pos.immutable();
        this.state = state;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getState() {
        return state;
    }
}
