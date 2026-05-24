package top.huliawsl.blockwright.config;

import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.util.BlockwrightPaths;
import top.huliawsl.blockwright.util.JsonHelper;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BlockwrightConfig {
    private static final String FILE_NAME = "blockwright.json";

    private static ConfigValues values = ConfigValues.defaults();

    private BlockwrightConfig() {
    }

    public static synchronized void reload() {
        Path path = BlockwrightPaths.getConfigDirectory().resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.writeString(path, JsonHelper.GSON.toJson(ConfigValues.defaults()));
                values = ConfigValues.defaults();
                return;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                ConfigValues loaded = JsonHelper.GSON.fromJson(reader, ConfigValues.class);
                values = loaded == null ? ConfigValues.defaults() : loaded.sanitized();
            }
        } catch (Exception exception) {
            values = ConfigValues.defaults();
            Blockwright.LOGGER.warn("Failed to load Blockwright config {}", path, exception);
        }
    }

    public static synchronized ConfigValues get() {
        return values;
    }

    public static final class ConfigValues {
        public int maxSelectionVolume = 131072;
        public int maxPreviewBlocks = 65536;
        public int maxBakeBlocks = 65536;
        public int maxSplinePoints = 64;
        public int maxUndoHistory = 8;

        public static ConfigValues defaults() {
            return new ConfigValues();
        }

        public ConfigValues sanitized() {
            maxSelectionVolume = Math.max(1, maxSelectionVolume);
            maxPreviewBlocks = Math.max(1, maxPreviewBlocks);
            maxBakeBlocks = Math.max(1, maxBakeBlocks);
            maxSplinePoints = Math.max(2, maxSplinePoints);
            maxUndoHistory = Math.max(1, maxUndoHistory);
            return this;
        }
    }
}
