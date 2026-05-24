package top.huliawsl.blockwright.pack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.world.BlockResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class SpongeSchematicReader {
    private SpongeSchematicReader() {
    }

    public static SpongeSchematicMetadata readMetadata(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            CompoundTag root = NbtIo.readCompressed(inputStream);
            int width = Short.toUnsignedInt(root.getShort("Width"));
            int height = Short.toUnsignedInt(root.getShort("Height"));
            int length = Short.toUnsignedInt(root.getShort("Length"));
            return new SpongeSchematicMetadata(width, height, length);
        }
    }

    public static SpongeSchematicData read(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            CompoundTag root = NbtIo.readCompressed(inputStream);
            int version = root.getInt("Version");
            if (version < 2 || version > 3) {
                throw new IOException("Unsupported schematic version: " + version);
            }
            int width = Short.toUnsignedInt(root.getShort("Width"));
            int height = Short.toUnsignedInt(root.getShort("Height"));
            int length = Short.toUnsignedInt(root.getShort("Length"));
            int dataVersion = root.contains("DataVersion") ? root.getInt("DataVersion") : 0;
            int[] offset = root.contains("Offset") ? root.getIntArray("Offset") : new int[] {0, 0, 0};
            SpongeSchematicData data = new SpongeSchematicData(width, height, length, dataVersion, offset);

            CompoundTag paletteTag = root.getCompound("Palette");
            List<Map.Entry<String, Integer>> paletteEntries = new ArrayList<>();
            for (String key : paletteTag.getAllKeys()) {
                paletteEntries.add(Map.entry(key, paletteTag.getInt(key)));
            }
            paletteEntries.sort(Comparator.comparingInt(Map.Entry::getValue));

            int paletteSize = paletteEntries.isEmpty() ? 0 : paletteEntries.get(paletteEntries.size() - 1).getValue() + 1;
            List<BlockState> palette = new ArrayList<>(paletteSize);
            for (int i = 0; i < paletteSize; i++) {
                palette.add(Blocks.AIR.defaultBlockState());
            }
            for (Map.Entry<String, Integer> entry : paletteEntries) {
                int namespaceSplit = entry.getKey().indexOf(':');
                if (namespaceSplit > 0) {
                    data.addRequiredMod(entry.getKey().substring(0, namespaceSplit));
                }
                palette.set(entry.getValue(), BlockResolver.resolve(entry.getKey()).orElseGet(() -> {
                    data.addMissingBlockState(entry.getKey());
                    return Blocks.AIR.defaultBlockState();
                }));
            }

            int[] paletteIndices = decodeVarInts(root.getByteArray("BlockData"), width * height * length);
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    for (int x = 0; x < width; x++) {
                        int paletteIndex = paletteIndices[data.index(x, y, z)];
                        BlockState state = paletteIndex >= 0 && paletteIndex < palette.size()
                                ? palette.get(paletteIndex)
                                : Blocks.AIR.defaultBlockState();
                        data.setBlockState(x, y, z, state);
                    }
                }
            }

            if (root.contains("Metadata")) {
                CompoundTag metadata = root.getCompound("Metadata");
                ListTag requiredMods = metadata.getList("RequiredMods", 8);
                for (int i = 0; i < requiredMods.size(); i++) {
                    data.addRequiredMod(requiredMods.getString(i));
                }
            }

            ListTag blockEntities = root.getList("BlockEntities", 10);
            for (int i = 0; i < blockEntities.size(); i++) {
                CompoundTag blockEntityTag = blockEntities.getCompound(i).copy();
                int[] pos = blockEntityTag.contains("Pos") ? blockEntityTag.getIntArray("Pos") : new int[] {
                        blockEntityTag.getInt("x"),
                        blockEntityTag.getInt("y"),
                        blockEntityTag.getInt("z")
                };
                if (pos.length != 3) {
                    continue;
                }
                if (blockEntityTag.contains("Id")) {
                    blockEntityTag.putString("id", blockEntityTag.getString("Id"));
                }
                blockEntityTag.putInt("x", pos[0]);
                blockEntityTag.putInt("y", pos[1]);
                blockEntityTag.putInt("z", pos[2]);
                data.putBlockEntity(new net.minecraft.core.BlockPos(pos[0], pos[1], pos[2]), blockEntityTag);
            }
            return data;
        }
    }

    private static int[] decodeVarInts(byte[] input, int expectedCount) throws IOException {
        int[] result = new int[expectedCount];
        int readIndex = 0;
        int writeIndex = 0;
        while (readIndex < input.length && writeIndex < expectedCount) {
            int value = 0;
            int position = 0;
            byte current;
            do {
                if (readIndex >= input.length) {
                    throw new IOException("Unexpected end of BlockData");
                }
                current = input[readIndex++];
                value |= (current & 127) << position;
                position += 7;
                if (position > 35) {
                    throw new IOException("VarInt too long in BlockData");
                }
            } while ((current & 128) != 0);
            result[writeIndex++] = value;
        }
        if (writeIndex != expectedCount) {
            throw new IOException("BlockData length mismatch: expected " + expectedCount + " entries but got " + writeIndex);
        }
        return result;
    }
}
