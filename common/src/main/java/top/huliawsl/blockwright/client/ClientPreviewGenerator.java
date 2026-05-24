package top.huliawsl.blockwright.client;

import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.pack.BlockwrightPackManager;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.PresetExecutor;
import top.huliawsl.blockwright.rule.PresetExecutorRegistry;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.session.PlayerSession;

import java.util.Map;

public final class ClientPreviewGenerator {
    private ClientPreviewGenerator() {
    }

    public static PreviewPlan generate(String presetId, Map<String, String> overrides) {
        BlockwrightPackManager.PresetLookup lookup = Blockwright.getPackManager().findPreset(presetId).orElse(null);
        if (lookup == null) {
            return null;
        }

        RuleDefinition rule = lookup.pack().getRules().get(lookup.preset().rule);
        if (rule == null) {
            return null;
        }

        PresetExecutor executor = PresetExecutorRegistry.get(rule.executor).orElse(null);
        if (executor == null) {
            return null;
        }

        PlayerSession session = new PlayerSession();
        session.getRegionSelection().setPos1(ClientSelectionState.getRegionSelection().getPos1());
        session.getRegionSelection().setPos2(ClientSelectionState.getRegionSelection().getPos2());
        ClientSelectionState.getSplineSelection().getPoints().forEach(session.getSplineSelection()::addPoint);

        return executor.execute(new PresetExecutionContext(
                null,
                session,
                lookup.pack(),
                lookup.preset(),
                rule,
                overrides,
                lookup.pack().getModules()
        ));
    }
}
