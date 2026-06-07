package top.huliawsl.blockwright.pcg.node;

import com.google.gson.JsonElement;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import top.huliawsl.blockwright.pcg.PcgData;
import top.huliawsl.blockwright.pcg.PcgGraphContext;
import top.huliawsl.blockwright.pcg.PcgNode;
import top.huliawsl.blockwright.pcg.PcgNodeDefinition;
import top.huliawsl.blockwright.pcg.PcgPoint;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.world.BlockResolver;

import java.util.Map;
import java.util.Optional;

public final class BlockPaintNode implements PcgNode {
    @Override
    public PcgData execute(PcgGraphContext context, PcgNodeDefinition node, Map<String, PcgData> inputs) {
        PcgData input = PcgNodeUtil.primaryInput(inputs);
        if (input.getPoints().isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "block_paint requires input points.");
            return input;
        }

        Optional<BlockState> mainBlock = resolveConfiguredBlock(context, node, "blockParam", "block", "minecraft:stone");
        Optional<BlockState> edgeBlock = resolveConfiguredBlock(context, node, "edgeBlockParam", "edgeBlock", "");
        String edgeAttribute = PcgNodeUtil.resolveConfigString(context, node, "edgeAttribute", "edge");
        String blockAttribute = PcgNodeUtil.resolveConfigString(context, node, "blockAttribute", "");
        String blockTemplate = PcgNodeUtil.resolveConfigString(context, node, "blockTemplate", "");
        int yOffset = PcgNodeUtil.resolveConfigInt(context, node, "yOffset", 0);
        if (mainBlock.isEmpty()) {
            context.getPlan().addIssue(PreviewSeverity.ERROR, "block_paint could not resolve its main block.");
            return input;
        }

        boolean warnedAttributeBlock = false;
        for (PcgPoint point : input.getPoints()) {
            BlockState state = mainBlock.get();
            if (!blockTemplate.isBlank()) {
                Optional<BlockState> fromTemplate = BlockResolver.resolve(PcgNodeUtil.interpolatePoint(context, blockTemplate, point));
                if (fromTemplate.isPresent()) {
                    state = fromTemplate.get();
                } else if (!warnedAttributeBlock) {
                    context.getPlan().addIssue(PreviewSeverity.WARNING, "block_paint skipped one or more invalid blockTemplate values.");
                    warnedAttributeBlock = true;
                }
            } else if (!blockAttribute.isBlank()) {
                JsonElement value = point.getAttributes().get(blockAttribute);
                if (value != null && value.isJsonPrimitive()) {
                    Optional<BlockState> fromAttribute = BlockResolver.resolve(value.getAsString());
                    if (fromAttribute.isPresent()) {
                        state = fromAttribute.get();
                    } else if (!warnedAttributeBlock) {
                        context.getPlan().addIssue(PreviewSeverity.WARNING, "block_paint skipped one or more invalid blockAttribute values.");
                        warnedAttributeBlock = true;
                    }
                }
            }
            if (edgeBlock.isPresent() && isTruthy(point.getAttributes().get(edgeAttribute))) {
                state = edgeBlock.get();
            }
            BlockPos pos = new BlockPos(Mth.floor(point.getPosition().x), Mth.floor(point.getPosition().y) + yOffset,
                    Mth.floor(point.getPosition().z));
            context.getPlan().addBlock(pos, state);
        }
        return input;
    }

    private Optional<BlockState> resolveConfiguredBlock(PcgGraphContext context, PcgNodeDefinition node,
                                                        String paramKey, String directKey, String fallback) {
        String param = PcgNodeUtil.resolveConfigString(context, node, paramKey, "");
        if (param != null && !param.isBlank()) {
            return context.resolveBlockParameter(param, fallback);
        }
        String blockId = PcgNodeUtil.resolveConfigString(context, node, directKey, fallback);
        return BlockResolver.resolve(blockId);
    }

    private boolean isTruthy(JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) {
            return false;
        }
        try {
            if (value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean();
            }
            if (value.getAsJsonPrimitive().isNumber()) {
                return value.getAsDouble() != 0.0D;
            }
            String raw = value.getAsString();
            return "true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw) || "1".equals(raw);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
