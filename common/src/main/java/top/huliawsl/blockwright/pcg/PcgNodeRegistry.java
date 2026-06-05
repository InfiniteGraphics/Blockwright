package top.huliawsl.blockwright.pcg;

import top.huliawsl.blockwright.pcg.node.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PcgNodeRegistry {
    private static final Map<String, PcgNode> NODES = new LinkedHashMap<>();

    static {
        register("region_input", new RegionInputNode());
        register("spline_input", new SplineInputNode());
        register("resample_spline", new ResampleSplineNode());
        register("sample_curve", new ResampleSplineNode());
        register("road_surface", new RoadSurfaceNode());
        register("place_modules_every_n", new PlaceModulesEveryNNode());
        register("instance_modules_every_n", new PlaceModulesEveryNNode());
        register("place_modules", new PlaceModulesNode());
        register("instance_modules", new PlaceModulesNode());
        register("box_building_shell", new BoxBuildingShellNode());
        register("facade_modules", new FacadeModulesNode());
        register("scatter_points", new ScatterPointsNode());
        register("scatter_in_volume", new ScatterPointsNode());
        register("merge", new MergeNode());
        register("offset_points", new OffsetPointsNode());
        register("snap_to_grid", new SnapToGridNode());
        register("volume_boundary", new VolumeBoundaryNode());
        register("random_filter", new RandomFilterNode());
        register("filter_random", new RandomFilterNode());
        register("attribute_set", new AttributeSetNode());
        register("set_attribute", new AttributeSetNode());
        register("filter_by_attribute", new FilterByAttributeNode());
        register("terrain_sample", new TerrainSampleNode());
        register("biome_filter", new BiomeFilterNode());
        register("density_mask", new DensityMaskNode());
        register("collision_prune", new CollisionPruneNode());
        register("connector_chain", new ConnectorChainNode());
    }

    private PcgNodeRegistry() {
    }

    public static void register(String type, PcgNode node) {
        NODES.put(type, node);
    }

    public static Optional<PcgNode> get(String type) {
        return Optional.ofNullable(NODES.get(type));
    }

    public static Set<String> getRegisteredTypes() {
        return Set.copyOf(NODES.keySet());
    }
}
