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
        register("shape_input", new RegionInputNode());
        register("spline_input", new SplineInputNode());
        register("curve_input", new SplineInputNode());
        register("resample_spline", new ResampleSplineNode());
        register("sample_curve", new ResampleSplineNode());
        register("curve_resample", new ResampleSplineNode());
        register("curve_ribbon", new CurveRibbonNode());
        register("shape_metrics", new ShapeMetricsNode());
        register("measure_shape", new ShapeMetricsNode());
        register("shape_extrude", new ShapeExtrudeNode());
        register("voxelize_volume", new VoxelizeVolumeNode());
        register("block_paint", new BlockPaintNode());
        register("voxelize_surface", new BlockPaintNode());
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
        register("sample_boundary", new VolumeBoundaryNode());
        register("random_filter", new RandomFilterNode());
        register("filter_random", new RandomFilterNode());
        register("attribute_set", new AttributeSetNode());
        register("set_attribute", new AttributeSetNode());
        register("attribute_random", new AttributeRandomNode());
        register("attribute_expression", new AttributeExpressionNode());
        register("attribute_math", new AttributeExpressionNode());
        register("filter_by_attribute", new FilterByAttributeNode());
        register("filter_by_expression", new FilterByExpressionNode());
        register("select_point", new SelectPointNode());
        register("facade_grid", new FacadeGridNode());
        register("roof_from_footprint", new RoofFromFootprintNode());
        register("terrain_sample", new TerrainSampleNode());
        register("query_terrain", new TerrainSampleNode());
        register("project_to_terrain", new TerrainSampleNode());
        register("biome_filter", new BiomeFilterNode());
        register("density_mask", new DensityMaskNode());
        register("collision_prune", new CollisionPruneNode());
        register("connector_chain", new ConnectorChainNode());
        register("connect_by_socket", new ConnectorChainNode());
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
