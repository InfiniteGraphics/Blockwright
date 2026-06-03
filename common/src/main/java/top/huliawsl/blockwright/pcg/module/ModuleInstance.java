package top.huliawsl.blockwright.pcg.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.SpongeSchematicData;

import java.util.ArrayList;
import java.util.List;

public final class ModuleInstance {
    private final ModuleDefinition module;
    private final BlockPos origin;
    private final Rotation rotation;
    private final AABB bounds;
    private final List<WorldConnector> connectors;

    public ModuleInstance(ModuleDefinition module, BlockPos origin, Rotation rotation) {
        this.module = module;
        this.origin = origin.immutable();
        this.rotation = rotation == null ? Rotation.NONE : rotation;
        this.bounds = computeBounds(module, origin, this.rotation);
        this.connectors = computeConnectors();
    }

    public ModuleDefinition getModule() {
        return module;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public AABB getBounds() {
        return bounds;
    }

    public List<WorldConnector> getConnectors() {
        return connectors;
    }

    public WorldConnector firstFreeConnector() {
        for (WorldConnector connector : connectors) {
            if (!connector.isConsumed()) {
                return connector;
            }
        }
        return null;
    }

    public int getWidth() {
        SpongeSchematicData data = module.schematicData;
        if (data != null) {
            return data.getWidth();
        }
        return module.size.size() > 0 ? module.size.get(0) : 1;
    }

    public int getHeight() {
        SpongeSchematicData data = module.schematicData;
        if (data != null) {
            return data.getHeight();
        }
        return module.size.size() > 1 ? module.size.get(1) : 1;
    }

    public int getDepth() {
        SpongeSchematicData data = module.schematicData;
        if (data != null) {
            return data.getLength();
        }
        return module.size.size() > 2 ? module.size.get(2) : 1;
    }

    private List<WorldConnector> computeConnectors() {
        List<WorldConnector> result = new ArrayList<>();
        if (module.connectors == null) {
            return result;
        }
        for (ModuleConnector connector : module.connectors) {
            BlockPos local = readOffset(connector);
            BlockPos rotated = rotateLocal(local, getWidth(), getDepth(), rotation);
            Direction direction = rotateDirection(parseDirection(connector.direction), rotation);
            result.add(new WorldConnector(this, connector, origin.offset(rotated), direction));
        }
        return result;
    }

    private static AABB computeBounds(ModuleDefinition module, BlockPos origin, Rotation rotation) {
        int width = 1;
        int height = 1;
        int depth = 1;
        if (module.schematicData != null) {
            width = module.schematicData.getWidth();
            height = module.schematicData.getHeight();
            depth = module.schematicData.getLength();
        } else if (module.size != null && module.size.size() >= 3) {
            width = Math.max(1, module.size.get(0));
            height = Math.max(1, module.size.get(1));
            depth = Math.max(1, module.size.get(2));
        }
        if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
            int swapped = width;
            width = depth;
            depth = swapped;
        }
        return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + width, origin.getY() + height, origin.getZ() + depth);
    }

    static BlockPos readOffset(ModuleConnector connector) {
        if (connector == null || connector.offset == null || connector.offset.size() < 3) {
            return BlockPos.ZERO;
        }
        return new BlockPos(connector.offset.get(0), connector.offset.get(1), connector.offset.get(2));
    }

    static BlockPos rotateLocal(BlockPos pos, int width, int depth, Rotation rotation) {
        int maxX = Math.max(0, width - 1);
        int maxZ = Math.max(0, depth - 1);
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(maxZ - pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> new BlockPos(maxX - pos.getX(), pos.getY(), maxZ - pos.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), maxX - pos.getX());
            default -> pos;
        };
    }

    static Direction parseDirection(String raw) {
        if (raw == null || raw.isBlank()) {
            return Direction.NORTH;
        }
        Direction direction = Direction.byName(raw.toLowerCase(java.util.Locale.ROOT));
        return direction == null ? Direction.NORTH : direction;
    }

    static Direction rotateDirection(Direction direction, Rotation rotation) {
        return rotation.rotate(direction);
    }
}
