package top.huliawsl.blockwright.rule;

import top.huliawsl.blockwright.rule.executor.BoxBuildingSkeletonExecutor;
import top.huliawsl.blockwright.rule.executor.SplineRoadSkeletonExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PresetExecutorRegistry {
    private static final Map<String, PresetExecutor> EXECUTORS = new HashMap<>();

    static {
        register("box_building_skeleton", new BoxBuildingSkeletonExecutor());
        register("spline_road_skeleton", new SplineRoadSkeletonExecutor());
    }

    private PresetExecutorRegistry() {
    }

    public static void register(String id, PresetExecutor executor) {
        EXECUTORS.put(id, executor);
    }

    public static Optional<PresetExecutor> get(String id) {
        return Optional.ofNullable(EXECUTORS.get(id));
    }
}
