package top.huliawsl.blockwright.pack;

import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.util.JsonHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExportPackBootstrap {
    public static final String PACK_ID = "blockwright_exports";

    private ExportPackBootstrap() {
    }

    public static Path ensureExportPack(Path presetRoot) throws Exception {
        Path packRoot = presetRoot.resolve(PACK_ID);
        Files.createDirectories(packRoot.resolve("modules"));
        Files.createDirectories(packRoot.resolve("presets"));
        Files.createDirectories(packRoot.resolve("rules"));

        Path packJson = packRoot.resolve("pack.json");
        if (!Files.exists(packJson)) {
            PackMetadata metadata = new PackMetadata();
            metadata.id = PACK_ID;
            metadata.name = "Blockwright Exports";
            metadata.version = "0.1.0";
            metadata.author = "Blockwright";
            metadata.description = "Auto-exported Blockwright modules.";
            metadata.minecraftVersions = List.of("1.20.1");
            metadata.requiredMods = List.of("minecraft");
            Files.writeString(packJson, JsonHelper.GSON.toJson(metadata));
        }
        return packRoot;
    }
}
