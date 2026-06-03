package top.huliawsl.blockwright.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreviewPlan {
    private final String presetId;
    private final List<PlannedBlock> plannedBlocks = new ArrayList<>();
    private final List<PreviewIssue> issues = new ArrayList<>();
    private final List<PreviewDebugPoint> debugPoints = new ArrayList<>();
    private final List<PreviewDebugLine> debugLines = new ArrayList<>();
    private boolean stale;

    public PreviewPlan(String presetId) {
        this.presetId = presetId;
    }

    public String getPresetId() {
        return presetId;
    }

    public void addBlock(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        plannedBlocks.add(new PlannedBlock(pos, state));
    }

    public void addBlock(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, CompoundTag blockEntityTag) {
        plannedBlocks.add(new PlannedBlock(pos, state, blockEntityTag));
    }

    public void addIssue(PreviewSeverity severity, String message) {
        issues.add(new PreviewIssue(severity, message));
    }

    public List<PlannedBlock> getPlannedBlocks() {
        return Collections.unmodifiableList(plannedBlocks);
    }

    public List<PreviewIssue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void addDebugPoint(net.minecraft.world.phys.Vec3 position, String label, int color) {
        debugPoints.add(new PreviewDebugPoint(position, label, color));
    }

    public void addDebugLine(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, String label, int color) {
        debugLines.add(new PreviewDebugLine(from, to, label, color));
    }

    public List<PreviewDebugPoint> getDebugPoints() {
        return Collections.unmodifiableList(debugPoints);
    }

    public List<PreviewDebugLine> getDebugLines() {
        return Collections.unmodifiableList(debugLines);
    }

    public boolean canBake() {
        return !stale && issues.stream().noneMatch(issue -> issue.getSeverity() == PreviewSeverity.ERROR);
    }

    public PreviewSeverity getOverallSeverity() {
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == PreviewSeverity.ERROR)) {
            return PreviewSeverity.ERROR;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == PreviewSeverity.WARNING)) {
            return PreviewSeverity.WARNING;
        }
        return PreviewSeverity.OK;
    }

    public AABB getBounds() {
        if (plannedBlocks.isEmpty()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }
        BlockPos first = plannedBlocks.get(0).getPos();
        int minX = first.getX();
        int minY = first.getY();
        int minZ = first.getZ();
        int maxX = first.getX();
        int maxY = first.getY();
        int maxZ = first.getZ();
        for (PlannedBlock block : plannedBlocks) {
            BlockPos pos = block.getPos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }
}
