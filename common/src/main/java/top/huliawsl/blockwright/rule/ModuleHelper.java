package top.huliawsl.blockwright.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.SpongeSchematicData;
import top.huliawsl.blockwright.preview.PreviewPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ModuleHelper {
    private ModuleHelper() {
    }

    public static List<ModuleDefinition> findTaggedModules(Map<String, ModuleDefinition> modules, String requiredTag, String style) {
        List<ModuleDefinition> matches = new ArrayList<>();
        for (ModuleDefinition module : modules.values()) {
            if (!"static_schem".equals(module.moduleKind) || module.schematicData == null) {
                continue;
            }
            if (requiredTag != null && !requiredTag.isBlank() && !module.tags.contains(requiredTag)) {
                continue;
            }
            if (style != null && !style.isBlank() && module.style != null && !style.equals(module.style)) {
                continue;
            }
            matches.add(module);
        }
        matches.sort(Comparator.comparing(definition -> definition.id));
        return matches;
    }

    public static ModuleDefinition pickWeighted(List<ModuleDefinition> modules, RandomSource random) {
        if (modules.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (ModuleDefinition module : modules) {
            totalWeight += Math.max(1, module.weight);
        }
        int selected = random.nextInt(totalWeight);
        for (ModuleDefinition module : modules) {
            selected -= Math.max(1, module.weight);
            if (selected < 0) {
                return module;
            }
        }
        return modules.get(0);
    }

    public static void appendSchematic(PreviewPlan plan, ModuleDefinition module, BlockPos origin, Rotation rotation, boolean ignoreAir) {
        SpongeSchematicData schematic = module.schematicData;
        if (schematic == null) {
            return;
        }
        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int z = 0; z < schematic.getLength(); z++) {
                for (int x = 0; x < schematic.getWidth(); x++) {
                    BlockState state = schematic.getBlockState(x, y, z);
                    if (state == null || ignoreAir && state.isAir()) {
                        continue;
                    }
                    BlockPos relative = rotate(new BlockPos(x, y, z), schematic, rotation);
                    BlockPos worldPos = origin.offset(relative);
                    CompoundTag blockEntityTag = schematic.getBlockEntities().get(new BlockPos(x, y, z));
                    if (blockEntityTag != null) {
                        CompoundTag placedTag = blockEntityTag.copy();
                        placedTag.putInt("x", worldPos.getX());
                        placedTag.putInt("y", worldPos.getY());
                        placedTag.putInt("z", worldPos.getZ());
                        plan.addBlock(worldPos, state.rotate(rotation), placedTag);
                    } else {
                        plan.addBlock(worldPos, state.rotate(rotation));
                    }
                }
            }
        }
    }

    public static List<String> readStringArray(JsonElement element) {
        List<String> values = new ArrayList<>();
        if (!(element instanceof JsonArray array)) {
            return values;
        }
        for (JsonElement jsonElement : array) {
            if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
                values.add(jsonElement.getAsString());
            }
        }
        return values;
    }

    private static BlockPos rotate(BlockPos pos, SpongeSchematicData schematic, Rotation rotation) {
        int maxX = schematic.getWidth() - 1;
        int maxZ = schematic.getLength() - 1;
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(maxZ - pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> new BlockPos(maxX - pos.getX(), pos.getY(), maxZ - pos.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), maxX - pos.getX());
            default -> pos;
        };
    }
}
