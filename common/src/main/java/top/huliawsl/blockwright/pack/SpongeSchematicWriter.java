package top.huliawsl.blockwright.pack;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.world.BlockResolver;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpongeSchematicWriter {
    private static final int CURRENT_VERSION = 2;

    private SpongeSchematicWriter() {
    }

    public static SpongeSchematicData capture(Level level, BoxRegionSelection region) {
        BlockPos min = region.getMin();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getDepth();
        int dataVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        SpongeSchematicData data = new SpongeSchematicData(width, height, length, dataVersion, new int[] {0, 0, 0});
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos worldPos = min.offset(x, y, z);
                    BlockState state = level.getBlockState(worldPos);
                    data.setBlockState(x, y, z, state);
                    ResourceLocation blockId = BlockResolver.getBlockId(state);
                    if (blockId != null) {
                        data.addRequiredMod(blockId.getNamespace());
                    }
                    BlockEntity blockEntity = level.getBlockEntity(worldPos);
                    if (blockEntity != null) {
                        CompoundTag tag = blockEntity.saveWithFullMetadata();
                        tag.putInt("x", x);
                        tag.putInt("y", y);
                        tag.putInt("z", z);
                        data.putBlockEntity(new BlockPos(x, y, z), tag);
                    }
                }
            }
        }
        return data;
    }

    public static void write(Path path, SpongeSchematicData data, String name, String author) throws Exception {
        Files.createDirectories(path.getParent());

        CompoundTag root = new CompoundTag();
        root.putInt("Version", CURRENT_VERSION);
        root.putInt("DataVersion", data.getDataVersion());
        root.putShort("Width", (short) data.getWidth());
        root.putShort("Height", (short) data.getHeight());
        root.putShort("Length", (short) data.getLength());
        root.put("Offset", new IntArrayTag(data.getOffset()));
        root.put("Metadata", createMetadata(name, author, data));

        Map<String, Integer> palette = new LinkedHashMap<>();
        List<Integer> indices = new ArrayList<>(data.getVolume());
        for (int y = 0; y < data.getHeight(); y++) {
            for (int z = 0; z < data.getLength(); z++) {
                for (int x = 0; x < data.getWidth(); x++) {
                    BlockState state = data.getBlockState(x, y, z);
                    String serialized = BlockResolver.serialize(state);
                    Integer paletteIndex = palette.get(serialized);
                    if (paletteIndex == null) {
                        paletteIndex = palette.size();
                        palette.put(serialized, paletteIndex);
                    }
                    indices.add(paletteIndex);
                }
            }
        }
        root.putInt("PaletteMax", Math.max(1, palette.size()));
        CompoundTag paletteTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : palette.entrySet()) {
            paletteTag.putInt(entry.getKey(), entry.getValue());
        }
        root.put("Palette", paletteTag);
        root.putByteArray("BlockData", encodeVarInts(indices));

        ListTag blockEntities = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> entry : data.getBlockEntities().entrySet()) {
            CompoundTag tag = entry.getValue().copy();
            BlockPos pos = entry.getKey();
            tag.put("Pos", new IntArrayTag(new int[] {pos.getX(), pos.getY(), pos.getZ()}));
            if (tag.contains("id")) {
                tag.putString("Id", tag.getString("id"));
            }
            blockEntities.add(tag);
        }
        root.put("BlockEntities", blockEntities);
        root.put("Entities", new ListTag());

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(root, outputStream);
        }
    }

    private static CompoundTag createMetadata(String name, String author, SpongeSchematicData data) {
        CompoundTag metadata = new CompoundTag();
        metadata.putString("Name", name);
        metadata.putString("Author", author);
        metadata.putLong("Date", System.currentTimeMillis());
        ListTag requiredMods = new ListTag();
        for (String modId : data.getRequiredMods()) {
            requiredMods.add(net.minecraft.nbt.StringTag.valueOf(modId));
        }
        metadata.put("RequiredMods", requiredMods);
        return metadata;
    }

    private static byte[] encodeVarInts(List<Integer> values) {
        byte[] buffer = new byte[Math.max(1, values.size() * 5)];
        int offset = 0;
        for (int value : values) {
            int current = value;
            while ((current & -128) != 0) {
                buffer[offset++] = (byte) (current & 127 | 128);
                current >>>= 7;
            }
            buffer[offset++] = (byte) current;
        }
        byte[] result = new byte[offset];
        System.arraycopy(buffer, 0, result, 0, offset);
        return result;
    }
}
