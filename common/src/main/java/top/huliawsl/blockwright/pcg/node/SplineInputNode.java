package top.huliawsl.blockwright.pcg.node;

import net.minecraft.core.BlockPos;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.selection.SplineSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SplineInputNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        SplineSelection spline = context.getExecutionContext().getSession().getSplineSelection();
        if (spline.getPoints().size() < 2) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "Spline input requires at least two control points.");
            return PcgData.empty();
        }
        List<PcgPoint> points = new ArrayList<>();
        for (BlockPos point : spline.getPoints()) {
            points.add(PcgPoint.at(point));
        }
        return PcgData.ofPoints(points);
    }
}
