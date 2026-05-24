package top.huliawsl.blockwright.rule;

import top.huliawsl.blockwright.preview.PreviewPlan;

public interface PresetExecutor {
    PreviewPlan execute(PresetExecutionContext context);
}
