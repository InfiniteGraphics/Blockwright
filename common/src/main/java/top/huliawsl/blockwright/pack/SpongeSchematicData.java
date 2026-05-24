package top.huliawsl.blockwright.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpongeSchematicData {
    private final int width;
    private final int height;
    private final int length;
    private final int dataVersion;
    private final int[] offset;
    private final BlockState[] blockStates;
    private final Map<BlockPos, CompoundTag> blockEntities = new LinkedHashMap<>();
    private final Set<String> missingBlockStates = new LinkedHashSet<>();
    private final List<String> requiredMods = new ArrayList<>();

    public SpongeSchematicData(int width, int height, int length, int dataVersion, int[] offset) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.dataVersion = dataVersion;
        this.offset = offset == null || offset.length != 3 ? new int[] {0, 0, 0} : offset.clone();
        this.blockStates = new BlockState[width * height * length];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getLength() {
        return length;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public int[] getOffset() {
        return offset.clone();
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        blockStates[index(x, y, z)] = state;
    }

    public BlockState getBlockState(int x, int y, int z) {
        return blockStates[index(x, y, z)];
    }

    public void putBlockEntity(BlockPos pos, CompoundTag tag) {
        blockEntities.put(pos.immutable(), tag.copy());
    }

    public Map<BlockPos, CompoundTag> getBlockEntities() {
        return blockEntities;
    }

    public void addMissingBlockState(String stateId) {
        missingBlockStates.add(stateId);
    }

    public Set<String> getMissingBlockStates() {
        return missingBlockStates;
    }

    public List<String> getRequiredMods() {
        return requiredMods;
    }

    public void addRequiredMod(String modId) {
        if (modId != null && !modId.isBlank() && !requiredMods.contains(modId)) {
            requiredMods.add(modId);
        }
    }

    public int getVolume() {
        return blockStates.length;
    }

    public int index(int x, int y, int z) {
        return x + z * width + y * width * length;
    }
}
