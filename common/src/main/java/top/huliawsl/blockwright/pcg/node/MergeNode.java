package top.huliawsl.blockwright.pcg.node;

import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;

import java.util.Map;

public final class MergeNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        return PcgNodeUtil.primaryInput(inputs);
    }
}
