package top.huliawsl.blockwright.rule;

import net.minecraft.server.level.ServerPlayer;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.session.PlayerSession;

import java.util.Map;

public final class PresetExecutionContext {
    private final ServerPlayer player;
    private final PlayerSession session;
    private final LoadedPack pack;
    private final PresetDefinition preset;
    private final RuleDefinition rule;
    private final Map<String, String> overrides;
    private final Map<String, ModuleDefinition> modules;

    public PresetExecutionContext(ServerPlayer player, PlayerSession session, LoadedPack pack, PresetDefinition preset,
                                  RuleDefinition rule, Map<String, String> overrides, Map<String, ModuleDefinition> modules) {
        this.player = player;
        this.session = session;
        this.pack = pack;
        this.preset = preset;
        this.rule = rule;
        this.overrides = overrides;
        this.modules = modules;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public PlayerSession getSession() {
        return session;
    }

    public LoadedPack getPack() {
        return pack;
    }

    public PresetDefinition getPreset() {
        return preset;
    }

    public RuleDefinition getRule() {
        return rule;
    }

    public Map<String, String> getOverrides() {
        return overrides;
    }

    public Map<String, ModuleDefinition> getModules() {
        return modules;
    }
}
