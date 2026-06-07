package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonPrimitive;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TerrainSampleNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (context.getServerLevel().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.WARNING, "terrain_sample needs server world data; keeping input point heights.");
            return input;
        }
        int yOffset = context.getNodeInt(node, "yOffset", 0);
        List<PcgPoint> sampled = new ArrayList<>();
        for (PcgPoint point : input.getPoints()) {
            BlockPos pos = point.blockPos();
            int y = context.getServerLevel().get().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) + yOffset;
            BlockPos groundPos = new BlockPos(pos.getX(), Math.max(context.getServerLevel().get().getMinBuildHeight(), y - 1), pos.getZ());
            BlockState groundState = context.getServerLevel().get().getBlockState(groundPos);
            Map<String, com.google.gson.JsonElement> attributes = new LinkedHashMap<>(point.getAttributes());
            attributes.put("terrainY", new JsonPrimitive(y));
            attributes.put("groundBlock", new JsonPrimitive(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(groundState.getBlock()).toString()));
            attributes.put("biome", new JsonPrimitive(context.getServerLevel().get().getBiome(groundPos).unwrapKey()
                    .map(key -> key.location().toString())
                    .orElse("")));
            sampled.add(point.withPosition(new Vec3(point.getPosition().x, y + 0.5D, point.getPosition().z)).withAttributes(attributes));
        }
        return new PcgData(sampled, input.getVolumes());
    }
}
