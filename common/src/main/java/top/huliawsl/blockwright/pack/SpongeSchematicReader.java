package top.huliawsl.blockwright.pack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
