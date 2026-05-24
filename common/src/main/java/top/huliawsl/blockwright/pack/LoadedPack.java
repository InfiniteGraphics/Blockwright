package top.huliawsl.blockwright.pack;

import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.model.PackMetadata;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.util.ValidationReport;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LoadedPack {
    private final Path root;
    private final PackMetadata metadata;
    private final Map<String, ModuleDefinition> modules = new LinkedHashMap<>();
    private final Map<String, PresetDefinition> presets = new LinkedHashMap<>();
    private final Map<String, RuleDefinition> rules = new LinkedHashMap<>();
    private final ValidationReport validationReport = new ValidationReport();

    public LoadedPack(Path root, PackMetadata metadata) {
        this.root = root;
        this.metadata = metadata;
    }

    public Path getRoot() {
        return root;
    }

    public PackMetadata getMetadata() {
        return metadata;
    }

    public Map<String, ModuleDefinition> getModules() {
        return modules;
    }

    public Map<String, PresetDefinition> getPresets() {
        return presets;
    }

    public Map<String, RuleDefinition> getRules() {
        return rules;
    }

    public ValidationReport getValidationReport() {
        return validationReport;
    }
}
