package top.huliawsl.blockwright.util;

import dev.architectury.platform.Platform;

import java.nio.file.Files;
import java.nio.file.Path;

public final class BlockwrightPaths {
    public static final String PRESET_FOLDER_NAME = "blockwright_packs";

    private BlockwrightPaths() {
    }

    public static Path getGameDirectory() {
        return Platform.getGameFolder();
    }

    public static Path getPresetRoot() {
        return getGameDirectory().resolve(PRESET_FOLDER_NAME);
    }

    public static Path getConfigDirectory() {
        return getGameDirectory().resolve("config");
    }

    public static Path ensurePresetRoot() {
        Path path = getPresetRoot();
        try {
            Files.createDirectories(path);
        } catch (Exception ignored) {
        }
        return path;
    }
}
