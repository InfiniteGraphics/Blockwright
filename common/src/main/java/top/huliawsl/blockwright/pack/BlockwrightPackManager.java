package top.huliawsl.blockwright.pack;

import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetInputDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.BlockwrightPaths;
import top.huliawsl.blockwright.util.JsonHelper;
import top.huliawsl.blockwright.util.ValidationReport;
import top.huliawsl.blockwright.world.BlockResolver;
import dev.architectury.platform.Platform;

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
            if (metadata == null) {
                metadata = new PackMetadata();
                metadata.id = packRoot.getFileName().toString();
            }
            LoadedPack loadedPack = new LoadedPack(packRoot, metadata);
            validatePackMetadata(loadedPack.getValidationReport(), metadata, packRoot);
            loadModules(loadedPack);
            loadRules(loadedPack);
            loadPresets(loadedPack);
            if (metadata.id != null && loadedPacks.containsKey(metadata.id)) {
                loadedPack.getValidationReport().error(metadata.id, "Duplicate pack id: " + metadata.id);
            }
            loadedPacks.put(metadata.id, loadedPack);
            for (String presetId : loadedPack.getPresets().keySet()) {
                if (presetIndex.containsKey(presetId)) {
                    loadedPack.getValidationReport().error(presetId, "Duplicate preset id across packs: " + presetId);
                }
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
        if (metadata.requiredMods != null) {
            for (String requiredMod : metadata.requiredMods) {
                if (!"minecraft".equals(requiredMod) && !Platform.isModLoaded(requiredMod)) {
                    report.error(location, "Missing required mod: " + requiredMod);
                }
            }
        }
        if (metadata.entrypoints == null) {
            report.error(location, "entrypoints is missing.");
            metadata.entrypoints = new top.huliawsl.blockwright.pack.model.PackEntrypoints();
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
                    if (definition == null) {
                        loadedPack.getValidationReport().error(path.toString(), "Module metadata is empty.");
                        return;
                    }
                    normalizeModule(definition);
                    definition.sourcePath = path;
                    if (definition.id == null || definition.id.isBlank()) {
                        loadedPack.getValidationReport().error(path.toString(), "Module id is missing.");
                        return;
                    }
                    if (loadedPack.getModules().containsKey(definition.id)) {
                        loadedPack.getValidationReport().error(definition.id, "Duplicate module id in pack.");
                        return;
                    }
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
                SpongeSchematicData data = SpongeSchematicReader.read(schematicPath);
                definition.schematicData = data;
                SpongeSchematicMetadata metadata = new SpongeSchematicMetadata(data.getWidth(), data.getHeight(), data.getLength());
                if (definition.size.size() == 3 &&
                        (definition.size.get(0) != metadata.getWidth()
                                || definition.size.get(1) != metadata.getHeight()
                                || definition.size.get(2) != metadata.getLength())) {
                    loadedPack.getValidationReport().warning(definition.id, "Module size does not match schematic size.");
                }
                for (String missingBlockState : data.getMissingBlockStates()) {
                    loadedPack.getValidationReport().error(definition.id, "Missing blockstate in runtime registry: " + missingBlockState);
                }
            } catch (Exception exception) {
                loadedPack.getValidationReport().error(definition.id, "Failed to read schematic: " + exception.getMessage());
            }
        }
        if (definition.size.size() != 3) {
            loadedPack.getValidationReport().warning(definition.id, "Module size should contain exactly 3 values.");
        }
        for (ModuleConnector connector : definition.connectors) {
            validateConnector(loadedPack, definition, connector);
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
                    if (definition == null) {
                        loadedPack.getValidationReport().error(path.toString(), "Rule definition is empty.");
                        return;
                    }
                    normalizeRule(definition);
                    definition.sourcePath = path;
                    String ruleKey = pathRelativeToPack(loadedPack, path);
                    if (loadedPack.getRules().containsKey(ruleKey)) {
                        loadedPack.getValidationReport().error(ruleKey, "Duplicate rule file path.");
                        return;
                    }
                    loadedPack.getRules().put(ruleKey, definition);
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
                    if (definition == null) {
                        loadedPack.getValidationReport().error(path.toString(), "Preset definition is empty.");
                        return;
                    }
                    normalizePreset(definition);
                    definition.sourcePath = path;
                    if (definition.id == null || definition.id.isBlank()) {
                        loadedPack.getValidationReport().error(path.toString(), "Preset id is missing.");
                        return;
                    }
                    if (loadedPack.getPresets().containsKey(definition.id)) {
                        loadedPack.getValidationReport().error(definition.id, "Duplicate preset id in pack.");
                        return;
                    }
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
        for (PresetInputDefinition input : definition.inputs) {
            if (!"box_region".equals(input.type) && !"spline".equals(input.type)) {
                loadedPack.getValidationReport().error(definition.id, "Unsupported input type: " + input.type);
            }
        }
        for (Map.Entry<String, PresetParameterDefinition> entry : definition.parameters.entrySet()) {
            validateParameter(loadedPack, definition, entry.getKey(), entry.getValue());
        }
    }

    private String pathRelativeToPack(LoadedPack loadedPack, Path path) {
        return loadedPack.getRoot().relativize(path).toString().replace('\\', '/');
    }

    private void validateConnector(LoadedPack loadedPack, ModuleDefinition definition, ModuleConnector connector) {
        if (definition.size.size() != 3 || connector.offset.size() != 3 || connector.size.size() != 3) {
            loadedPack.getValidationReport().warning(definition.id, "Connector " + connector.id + " is missing complete offset/size.");
            return;
        }
        for (int i = 0; i < 3; i++) {
            int offset = connector.offset.get(i);
            int size = connector.size.get(i);
            int max = definition.size.get(i);
            if (offset < 0 || size < 1 || offset + size > max) {
                loadedPack.getValidationReport().error(definition.id, "Connector " + connector.id + " is outside module bounds.");
                return;
            }
        }
    }

    private void validateParameter(LoadedPack loadedPack, PresetDefinition preset, String key, PresetParameterDefinition definition) {
        if (definition == null) {
            loadedPack.getValidationReport().error(preset.id, "Parameter " + key + " is null.");
            return;
        }
        if (definition.defaultValue == null) {
            return;
        }
        if ("string".equals(definition.type) || "tag".equals(definition.type)) {
            if (!definition.defaultValue.isJsonPrimitive() || !definition.defaultValue.getAsJsonPrimitive().isString()) {
                loadedPack.getValidationReport().error(preset.id, "Parameter " + key + " default must be a string.");
            }
            return;
        }
        if (!definition.defaultValue.isJsonPrimitive() || !definition.defaultValue.getAsJsonPrimitive().isNumber()) {
            loadedPack.getValidationReport().error(preset.id, "Parameter " + key + " default must be numeric.");
            return;
        }
        double value = definition.defaultValue.getAsDouble();
        if (definition.min != null && value < definition.min) {
            loadedPack.getValidationReport().error(preset.id, "Parameter " + key + " default is below min.");
        }
        if (definition.max != null && value > definition.max) {
            loadedPack.getValidationReport().error(preset.id, "Parameter " + key + " default is above max.");
        }
        if ("block_state".equals(definition.type) && !BlockResolver.exists(definition.defaultValue.getAsString())) {
            loadedPack.getValidationReport().error(preset.id, "Parameter " + key + " default blockstate is invalid.");
        }
    }

    private void normalizeModule(ModuleDefinition definition) {
        if (definition == null) {
            return;
        }
        if (definition.size == null) {
            definition.size = new ArrayList<>();
        }
        if (definition.tags == null) {
            definition.tags = new ArrayList<>();
        }
        if (definition.allowedRotations == null) {
            definition.allowedRotations = new ArrayList<>();
        }
        if (definition.connectors == null) {
            definition.connectors = new ArrayList<>();
        }
        if (definition.constraints == null) {
            definition.constraints = new top.huliawsl.blockwright.module.model.ModuleConstraints();
        }
    }

    private void normalizePreset(PresetDefinition definition) {
        if (definition == null) {
            return;
        }
        if (definition.inputs == null) {
            definition.inputs = new ArrayList<>();
        }
        if (definition.parameters == null) {
            definition.parameters = new LinkedHashMap<>();
        }
    }

    private void normalizeRule(RuleDefinition definition) {
        if (definition != null && definition.config == null) {
            definition.config = new com.google.gson.JsonObject();
        }
    }

    public record PresetLookup(LoadedPack pack, PresetDefinition preset) {
    }
}
