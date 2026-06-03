package top.huliawsl.blockwright.pcg.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.rule.ModuleHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ConnectorResolver {
    private ConnectorResolver() {
    }

    public static List<ModuleInstance> growChain(PreviewPlan plan, Map<String, ModuleDefinition> moduleMap,
                                                 BlockPos origin, Rotation startRotation, String startTag,
                                                 String allowedTag, int maxModules, long seed, boolean ignoreAir) {
        List<ModuleDefinition> startModules = ModuleHelper.findTaggedModules(moduleMap, startTag, "");
        List<ModuleDefinition> candidates = ModuleHelper.findTaggedModules(moduleMap, allowedTag, "");
        if (startModules.isEmpty() || candidates.isEmpty()) {
            return List.of();
        }

        RandomSource random = RandomSource.create(seed == 0L ? origin.asLong() : seed);
        CollisionGrid collisionGrid = new CollisionGrid();
        List<ModuleInstance> placed = new ArrayList<>();
        ArrayDeque<WorldConnector> frontier = new ArrayDeque<>();

        ModuleDefinition startModule = ModuleHelper.pickWeighted(startModules, random);
        ModuleInstance start = new ModuleInstance(startModule, origin, startRotation);
        collisionGrid.add(start.getBounds());
        placed.add(start);
        ModuleHelper.appendSchematic(plan, start.getModule(), start.getOrigin(), start.getRotation(), ignoreAir);
        frontier.addAll(start.getConnectors());

        while (!frontier.isEmpty() && placed.size() < maxModules) {
            WorldConnector source = frontier.removeFirst();
            if (source.isConsumed()) {
                continue;
            }
            ModuleInstance next = findCandidate(candidates, source, collisionGrid, random);
            if (next == null) {
                continue;
            }
            source.consume();
            consumeMatchedConnector(next, source);
            collisionGrid.add(next.getBounds());
            placed.add(next);
            ModuleHelper.appendSchematic(plan, next.getModule(), next.getOrigin(), next.getRotation(), ignoreAir);
            for (WorldConnector connector : next.getConnectors()) {
                if (!connector.isConsumed()) {
                    frontier.addLast(connector);
                }
            }
        }
        return placed;
    }

    private static ModuleInstance findCandidate(List<ModuleDefinition> candidates, WorldConnector source,
                                                CollisionGrid collisionGrid, RandomSource random) {
        List<ModuleDefinition> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, new java.util.Random(random.nextLong()));
        for (ModuleDefinition module : shuffled) {
            for (Rotation rotation : allowedRotations(module)) {
                if (module.connectors == null) {
                    continue;
                }
                for (ModuleConnector connector : module.connectors) {
                    if (!isCompatible(source, connector, rotation)) {
                        continue;
                    }
                    BlockPos origin = originForMatch(module, connector, rotation, source);
                    ModuleInstance instance = new ModuleInstance(module, origin, rotation);
                    if (!collisionGrid.intersects(instance.getBounds())) {
                        return instance;
                    }
                }
            }
        }
        return null;
    }

    private static void consumeMatchedConnector(ModuleInstance instance, WorldConnector source) {
        for (WorldConnector connector : instance.getConnectors()) {
            if (connector.getType().equals(source.getType())
                    && connector.getDirection() == source.getDirection().getOpposite()
                    && connector.getOrigin().equals(source.getOrigin())) {
                connector.consume();
                return;
            }
        }
    }

    private static boolean isCompatible(WorldConnector source, ModuleConnector target, Rotation rotation) {
        String type = target.type == null ? "" : target.type;
        if (!source.getType().equals(type)) {
            return false;
        }
        Direction targetDirection = ModuleInstance.rotateDirection(ModuleInstance.parseDirection(target.direction), rotation);
        if (targetDirection != source.getDirection().getOpposite()) {
            return false;
        }
        if (!source.getTags().isEmpty() && target.tags != null && !target.tags.isEmpty()) {
            for (String tag : source.getTags()) {
                if (target.tags.contains(tag)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static BlockPos originForMatch(ModuleDefinition module, ModuleConnector connector, Rotation rotation, WorldConnector source) {
        int width = module.schematicData != null ? module.schematicData.getWidth() : module.size.size() > 0 ? module.size.get(0) : 1;
        int depth = module.schematicData != null ? module.schematicData.getLength() : module.size.size() > 2 ? module.size.get(2) : 1;
        BlockPos local = ModuleInstance.rotateLocal(ModuleInstance.readOffset(connector), width, depth, rotation);
        return source.getOrigin().subtract(local);
    }

    private static List<Rotation> allowedRotations(ModuleDefinition module) {
        if (module.allowedRotations == null || module.allowedRotations.isEmpty()) {
            return List.of(Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90);
        }
        List<Rotation> rotations = new ArrayList<>();
        for (Integer degrees : module.allowedRotations) {
            rotations.add(switch (Math.floorMod(degrees == null ? 0 : degrees, 360)) {
                case 90 -> Rotation.CLOCKWISE_90;
                case 180 -> Rotation.CLOCKWISE_180;
                case 270 -> Rotation.COUNTERCLOCKWISE_90;
                default -> Rotation.NONE;
            });
        }
        return rotations;
    }
}
