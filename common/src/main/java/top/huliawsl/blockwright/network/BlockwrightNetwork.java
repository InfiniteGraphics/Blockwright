package top.huliawsl.blockwright.network;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.client.ClientPreviewState;
import top.huliawsl.blockwright.preview.PlannedBlock;
import top.huliawsl.blockwright.preview.PreviewDebugLine;
import top.huliawsl.blockwright.preview.PreviewDebugPoint;
import top.huliawsl.blockwright.preview.PreviewIssue;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.world.BlockResolver;

public final class BlockwrightNetwork {
    public static final ResourceLocation S2C_PREVIEW_SYNC = new ResourceLocation(Blockwright.MOD_ID, "preview_sync");
    public static final ResourceLocation S2C_PREVIEW_CLEAR = new ResourceLocation(Blockwright.MOD_ID, "preview_clear");

    private static boolean clientRegistered;

    private BlockwrightNetwork() {
    }

    public static void initClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2C_PREVIEW_SYNC, (buf, context) -> {
            PreviewPlan plan = readPreview(buf);
            context.queue(() -> ClientPreviewState.setPreviewPlan(plan));
        });
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2C_PREVIEW_CLEAR, (buf, context) ->
                context.queue(ClientPreviewState::clear));
    }

    public static void sendPreview(ServerPlayer player, PreviewPlan plan) {
        if (player == null || plan == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        writePreview(buf, plan);
        NetworkManager.sendToPlayer(player, S2C_PREVIEW_SYNC, buf);
    }

    public static void sendPreviewClear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        NetworkManager.sendToPlayer(player, S2C_PREVIEW_CLEAR, new FriendlyByteBuf(Unpooled.buffer()));
    }

    private static void writePreview(FriendlyByteBuf buf, PreviewPlan plan) {
        buf.writeUtf(plan.getPresetId() == null ? "" : plan.getPresetId());
        buf.writeBoolean(plan.isStale());
        buf.writeVarInt(plan.getPlannedBlocks().size());
        for (PlannedBlock block : plan.getPlannedBlocks()) {
            buf.writeBlockPos(block.getPos());
            buf.writeUtf(BlockResolver.serialize(block.getState()));
        }
        buf.writeVarInt(plan.getIssues().size());
        for (PreviewIssue issue : plan.getIssues()) {
            buf.writeEnum(issue.getSeverity());
            buf.writeUtf(issue.getMessage());
        }
        buf.writeVarInt(plan.getDebugPoints().size());
        for (PreviewDebugPoint point : plan.getDebugPoints()) {
            writeVec3(buf, point.getPosition());
            buf.writeUtf(point.getLabel());
            buf.writeInt(point.getColor());
        }
        buf.writeVarInt(plan.getDebugLines().size());
        for (PreviewDebugLine line : plan.getDebugLines()) {
            writeVec3(buf, line.getFrom());
            writeVec3(buf, line.getTo());
            buf.writeUtf(line.getLabel());
            buf.writeInt(line.getColor());
        }
    }

    private static PreviewPlan readPreview(FriendlyByteBuf buf) {
        PreviewPlan plan = new PreviewPlan(buf.readUtf());
        plan.setStale(buf.readBoolean());
        int blockCount = buf.readVarInt();
        for (int i = 0; i < blockCount; i++) {
            BlockPos pos = buf.readBlockPos();
            String rawState = buf.readUtf();
            BlockState state = BlockResolver.resolve(rawState).orElse(null);
            if (state != null) {
                plan.addBlock(pos, state);
            }
        }
        int issueCount = buf.readVarInt();
        for (int i = 0; i < issueCount; i++) {
            plan.addIssue(buf.readEnum(PreviewSeverity.class), buf.readUtf());
        }
        int pointCount = buf.readVarInt();
        for (int i = 0; i < pointCount; i++) {
            Vec3 position = readVec3(buf);
            String label = buf.readUtf();
            int color = buf.readInt();
            plan.addDebugPoint(position, label, color);
        }
        int lineCount = buf.readVarInt();
        for (int i = 0; i < lineCount; i++) {
            Vec3 from = readVec3(buf);
            Vec3 to = readVec3(buf);
            String label = buf.readUtf();
            int color = buf.readInt();
            plan.addDebugLine(from, to, label, color);
        }
        return plan;
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 value) {
        buf.writeDouble(value.x);
        buf.writeDouble(value.y);
        buf.writeDouble(value.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
