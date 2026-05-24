package top.huliawsl.blockwright.world;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class BlockResolver {
    private BlockResolver() {
    }

    public static Optional<BlockState> resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), raw, false).blockState());
        } catch (Exception exception) {
            String blockId = raw;
            int propertyIndex = raw.indexOf('[');
            if (propertyIndex >= 0) {
                blockId = raw.substring(0, propertyIndex);
            }
            ResourceLocation location = ResourceLocation.tryParse(blockId);
            if (location == null) {
                return Optional.empty();
            }

            Block block = BuiltInRegistries.BLOCK.get(location);
            if (block == Blocks.AIR && !"minecraft:air".equals(blockId)) {
                return Optional.empty();
            }
            return Optional.of(block.defaultBlockState());
        }
    }

    public static boolean exists(String raw) {
        return resolve(raw).isPresent();
    }

    public static String serialize(BlockState state) {
        return BlockStateParser.serialize(state);
    }

    public static ResourceLocation getBlockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }
}
