package top.huliawsl.blockwright.pcg.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import top.huliawsl.blockwright.module.model.ModuleConnector;

import java.util.List;

public final class WorldConnector {
    private final ModuleInstance owner;
    private final ModuleConnector definition;
    private final BlockPos origin;
    private final Direction direction;
    private boolean consumed;

    public WorldConnector(ModuleInstance owner, ModuleConnector definition, BlockPos origin, Direction direction) {
        this.owner = owner;
        this.definition = definition;
        this.origin = origin.immutable();
        this.direction = direction;
    }

    public ModuleInstance getOwner() {
        return owner;
    }

    public ModuleConnector getDefinition() {
        return definition;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getType() {
        return definition.type == null ? "" : definition.type;
    }

    public List<String> getTags() {
        return definition.tags == null ? List.of() : definition.tags;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void consume() {
        consumed = true;
    }
}
