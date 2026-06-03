package top.huliawsl.blockwright.pcg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PcgData {
    private static final PcgData EMPTY = new PcgData(List.of(), List.of());

    private final List<PcgPoint> points;
    private final List<PcgVolume> volumes;

    public PcgData(List<PcgPoint> points, List<PcgVolume> volumes) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points == null ? List.of() : points));
        this.volumes = Collections.unmodifiableList(new ArrayList<>(volumes == null ? List.of() : volumes));
    }

    public static PcgData empty() {
        return EMPTY;
    }

    public static PcgData ofPoints(List<PcgPoint> points) {
        return new PcgData(points, List.of());
    }

    public static PcgData ofVolume(PcgVolume volume) {
        return new PcgData(List.of(), List.of(volume));
    }

    public static PcgData ofVolumes(List<PcgVolume> volumes) {
        return new PcgData(List.of(), volumes);
    }

    public static PcgData merge(List<PcgData> dataSets) {
        if (dataSets == null || dataSets.isEmpty()) {
            return empty();
        }
        List<PcgPoint> mergedPoints = new ArrayList<>();
        List<PcgVolume> mergedVolumes = new ArrayList<>();
        for (PcgData data : dataSets) {
            if (data == null) {
                continue;
            }
            mergedPoints.addAll(data.getPoints());
            mergedVolumes.addAll(data.getVolumes());
        }
        return new PcgData(mergedPoints, mergedVolumes);
    }

    public List<PcgPoint> getPoints() {
        return points;
    }

    public List<PcgVolume> getVolumes() {
        return volumes;
    }

    public boolean isEmpty() {
        return points.isEmpty() && volumes.isEmpty();
    }
}
