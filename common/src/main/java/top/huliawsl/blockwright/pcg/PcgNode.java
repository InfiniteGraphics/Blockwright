package top.huliawsl.blockwright.pcg;

import java.util.Map;

public interface PcgNode {
    PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs);
}
