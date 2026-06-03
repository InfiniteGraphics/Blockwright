package top.huliawsl.blockwright.rule;

import top.huliawsl.blockwright.pcg.PcgGraphExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PresetExecutorRegistry {
    private static final Map<String, PresetExecutor> EXECUTORS = new HashMap<>();

    static {
        PcgGraphExecutor graphExecutor = new PcgGraphExecutor();
        register("box_building_skeleton", graphExecutor);
        register("spline_road_skeleton", graphExecutor);
        register("pcg_graph", graphExecutor);
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
