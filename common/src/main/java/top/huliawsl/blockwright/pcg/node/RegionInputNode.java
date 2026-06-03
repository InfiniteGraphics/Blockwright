package top.huliawsl.blockwright.pcg.node;

import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgVolume;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.selection.BoxRegionSelection;

import java.util.Map;

public final class RegionInputNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        BoxRegionSelection region = context.getExecutionContext().getSession().getRegionSelection();
        if (!region.isComplete()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "Region input requires a complete box selection.");
            return PcgData.empty();
        }
        return PcgData.ofVolume(new PcgVolume(region.getMin(), region.getMax(), context.getNodeString(node, "label", "region")));
    }
}
