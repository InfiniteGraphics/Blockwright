package top.huliawsl.blockwright.pack;

import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.BlockwrightPaths;
import top.huliawsl.blockwright.util.JsonHelper;
import top.huliawsl.blockwright.util.ValidationReport;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class BlockwrightPackManager {
    private final Map<String, LoadedPack> loadedPacks = new LinkedHashMap<>();
    private final Map<String, LoadedPack> presetIndex = new LinkedHashMap<>();

    public synchronized void reload() {
        loadedPacks.clear();
        presetIndex.clear();

        Path presetRoot = BlockwrightPaths.ensurePresetRoot();
        DemoPackBootstrap.ensureDemoPack(presetRoot);

        try (Stream<Path> stream = Files.list(presetRoot)) {
            stream.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(this::loadPackDirectory);
        } catch (IOException exception) {
            Blockwright.LOGGER.warn("Failed to scan preset root {}", presetRoot, exception);
        }
    }

    public synchronized int getLoadedPackCount() {
        return loadedPacks.size();
    }

    public synchronized List<LoadedPack> getLoadedPacks() {
        return Collections.unmodifiableList(new ArrayList<>(loadedPacks.values()));
    }

    public synchronized Optional<LoadedPack> findPack(String id) {
        return Optional.ofNullable(loadedPacks.get(id));
    }

    public synchronized Optional<PresetLookup> findPreset(String presetId) {
        LoadedPack pack = presetIndex.get(presetId);
        if (pack == null) {
            return Optional.empty();
        }
        PresetDefinition preset = pack.getPresets().get(presetId);
        if (preset == null) {
            return Optional.empty();
        }
        return Optional.of(new PresetLookup(pack, preset));
    }

    public synchronized List<PresetDefinition> getAllPresets() {
        List<PresetDefinition> presets = new ArrayList<>();
        for (LoadedPack pack : loadedPacks.values()) {
            presets.addAll(pack.getPresets().values());
        }
        return presets;
    }

    private void loadPackDirectory(Path packRoot) {
        Path packJson = packRoot.resolve("pack.json");
        if (!Files.exists(packJson)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(packJson)) {
            PackMetadata metadata = JsonHelper.GSON.fromJson(reader, PackMetadata.class);
            LoadedPack loadedPack = new LoadedPack(packRoot, metadata);
            validatePackMetadata(loadedPack.getValidationReport(), metadata, packRoot);
            loadModules(loadedPack);
            loadRules(loadedPack);
            loadPresets(loadedPack);
            loadedPacks.put(metadata.id, loadedPack);
            for (String presetId : loadedPack.getPresets().keySet()) {
                presetIndex.put(presetId, loadedPack);
            }
        } catch (Exception exception) {
            Blockwright.LOGGER.warn("Failed to load preset pack {}", packRoot, exception);
        }
    }

    private void validatePackMetadata(ValidationReport report, PackMetadata metadata, Path root) {
        String location = root.getFileName().toString();
        if (metadata == null) {
            report.error(location, "pack.json is empty.");
            return;
        }
        if (metadata.id == null || metadata.id.isBlank()) {
            report.error(location, "pack id is missing.");
        }
        if (metadata.minecraftVersions == null || !metadata.minecraftVersions.contains("1.20.1")) {
            report.warning(location, "minecraftVersions does not explicitly include 1.20.1.");
        }
    }

    private void loadModules(LoadedPack loadedPack) {
        Path moduleRoot = loadedPack.getRoot().resolve(loadedPack.getMetadata().entrypoints.modules);
        if (!Files.isDirectory(moduleRoot)) {
            loadedPack.getValidationReport().warning(loadedPack.getMetadata().id, "Module entrypoint directory is missing.");
            return;
        }

        try (Stream<Path> stream = Files.walk(moduleRoot)) {
            stream.filter(path -> path.toString().endsWith(".module.json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    ModuleDefinition definition = JsonHelper.GSON.fromJson(reader, ModuleDefinition.class);
                    definition.sourcePath = path;
                    loadedPack.getModules().put(definition.id, definition);
                    validateModule(loadedPack, definition);
                } catch (Exception exception) {
                    loadedPack.getValidationReport().error(path.toString(), "Failed to parse module metadata: " + exception.getMessage());
                }
            });
        } catch (IOException exception) {
            loadedPack.getValidationReport().error(moduleRoot.toString(), "Failed to scan modules: " + exception.getMessage());
        }
    }

    private void validateModule(LoadedPack loadedPack, ModuleDefinition definition) {
        if (definition.id == null || definition.id.isBlank()) {
            loadedPack.getValidationReport().error(definition.sourcePath.toString(), "Module id is missing.");
        }
        if ("static_schem".equals(definition.moduleKind) && definition.schematic != null) {
            Path schematicPath = loadedPack.getRoot().resolve(definition.schematic);
            if (!Files.exists(schematicPath)) {
                loadedPack.getValidationReport().error(definition.id, "Missing schematic: " + definition.schematic);
                return;
            }
            try {
                SpongeSchematicMetadata metadata = SpongeSchematicReader.readMetadata(schematicPath);
                if (definition.size.size() == 3 &&
                        (definition.size.get(0) != metadata.getWidth()
                                || definition.size.get(1) != metadata.getHeight()
                                || definition.size.get(2) != metadata.getLength())) {
                    loadedPack.getValidationReport().warning(definition.id, "Module size does not match schematic size.");
                }
            } catch (Exception exception) {
                loadedPack.getValidationReport().error(definition.id, "Failed to read schematic: " + exception.getMessage());
            }
        }
    }

    private void loadRules(LoadedPack loadedPack) {
        Path ruleRoot = loadedPack.getRoot().resolve(loadedPack.getMetadata().entrypoints.rules);
        if (!Files.isDirectory(ruleRoot)) {
            loadedPack.getValidationReport().warning(loadedPack.getMetadata().id, "Rule entrypoint directory is missing.");
            return;
        }

        try (Stream<Path> stream = Files.walk(ruleRoot)) {
            stream.filter(path -> path.toString().endsWith(".rule.json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    RuleDefinition definition = JsonHelper.GSON.fromJson(reader, RuleDefinition.class);
                    definition.sourcePath = path;
                    loadedPack.getRules().put(pathRelativeToPack(loadedPack, path), definition);
                } catch (Exception exception) {
                    loadedPack.getValidationReport().error(path.toString(), "Failed to parse rule: " + exception.getMessage());
                }
            });
        } catch (IOException exception) {
            loadedPack.getValidationReport().error(ruleRoot.toString(), "Failed to scan rules: " + exception.getMessage());
        }
    }

    private void loadPresets(LoadedPack loadedPack) {
        Path presetRoot = loadedPack.getRoot().resolve(loadedPack.getMetadata().entrypoints.presets);
        if (!Files.isDirectory(presetRoot)) {
            loadedPack.getValidationReport().warning(loadedPack.getMetadata().id, "Preset entrypoint directory is missing.");
            return;
        }

        try (Stream<Path> stream = Files.walk(presetRoot)) {
            stream.filter(path -> path.toString().endsWith(".preset.json")).forEach(path -> {
                try (Reader reader = Files.newBufferedReader(path)) {
                    PresetDefinition definition = JsonHelper.GSON.fromJson(reader, PresetDefinition.class);
                    definition.sourcePath = path;
                    loadedPack.getPresets().put(definition.id, definition);
                    validatePreset(loadedPack, definition);
                } catch (Exception exception) {
                    loadedPack.getValidationReport().error(path.toString(), "Failed to parse preset: " + exception.getMessage());
                }
            });
        } catch (IOException exception) {
            loadedPack.getValidationReport().error(presetRoot.toString(), "Failed to scan presets: " + exception.getMessage());
        }
    }

    private void validatePreset(LoadedPack loadedPack, PresetDefinition definition) {
        if (definition.id == null || definition.id.isBlank()) {
            loadedPack.getValidationReport().error(definition.sourcePath.toString(), "Preset id is missing.");
            return;
        }
        if (definition.rule == null || definition.rule.isBlank()) {
            loadedPack.getValidationReport().error(definition.id, "Preset rule file is missing.");
            return;
        }
        if (!loadedPack.getRules().containsKey(definition.rule)) {
            loadedPack.getValidationReport().error(definition.id, "Referenced rule file was not loaded: " + definition.rule);
        }
    }

    private String pathRelativeToPack(LoadedPack loadedPack, Path path) {
        return loadedPack.getRoot().relativize(path).toString().replace('\\', '/');
    }

    public record PresetLookup(LoadedPack pack, PresetDefinition preset) {
    }
}
