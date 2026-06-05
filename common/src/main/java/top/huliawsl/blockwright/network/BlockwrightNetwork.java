package top.huliawsl.blockwright.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.client.web.BlockwrightWebBridge;
import top.huliawsl.blockwright.config.BlockwrightConfig;
import top.huliawsl.blockwright.client.ClientPreviewState;
import top.huliawsl.blockwright.preview.PlannedBlock;
import top.huliawsl.blockwright.preview.PreviewDebugLine;
import top.huliawsl.blockwright.preview.PreviewDebugPoint;
import top.huliawsl.blockwright.preview.PreviewIssue;
import top.huliawsl.blockwright.preview.PreviewNodeSummary;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.pcg.PcgGraphExecutor;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.session.PlayerSession;
import top.huliawsl.blockwright.world.BlockResolver;
import top.huliawsl.blockwright.world.StructurePlacer;

public final class BlockwrightNetwork {
    public static final ResourceLocation S2C_PREVIEW_SYNC = new ResourceLocation(Blockwright.MOD_ID, "preview_sync");
    public static final ResourceLocation S2C_PREVIEW_CLEAR = new ResourceLocation(Blockwright.MOD_ID, "preview_clear");
    public static final ResourceLocation C2S_WEB_GRAPH_PREVIEW = new ResourceLocation(Blockwright.MOD_ID, "web_graph_preview");
    public static final ResourceLocation C2S_WEB_BAKE = new ResourceLocation(Blockwright.MOD_ID, "web_bake");
    public static final ResourceLocation S2C_WEB_PREVIEW_RESULT = new ResourceLocation(Blockwright.MOD_ID, "web_preview_result");
    public static final ResourceLocation S2C_WEB_BAKE_RESULT = new ResourceLocation(Blockwright.MOD_ID, "web_bake_result");

    private static final int MAX_GRAPH_JSON_CHARS = 1_000_000;

    private static boolean commonRegistered;
    private static boolean clientRegistered;

    private BlockwrightNetwork() {
    }


    public static void initCommon() {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2S_WEB_GRAPH_PREVIEW, (buf, context) -> {
            String requestId = buf.readUtf(128);
            String presetId = buf.readUtf(256);
            String graphJson = buf.readUtf(MAX_GRAPH_JSON_CHARS);
            String rawOverrides = buf.readUtf(MAX_GRAPH_JSON_CHARS);
            context.queue(() -> handleWebGraphPreview((ServerPlayer) context.getPlayer(), requestId, presetId, graphJson, rawOverrides));
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, C2S_WEB_BAKE, (buf, context) -> {
            String requestId = buf.readUtf(128);
            context.queue(() -> handleWebBake((ServerPlayer) context.getPlayer(), requestId));
        });
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
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2C_WEB_PREVIEW_RESULT, (buf, context) -> {
            String requestId = buf.readUtf(128);
            boolean ok = buf.readBoolean();
            String message = buf.readUtf(MAX_GRAPH_JSON_CHARS);
            PreviewPlan plan = readPreview(buf);
            context.queue(() -> {
                if (ok) {
                    ClientPreviewState.setPreviewPlan(plan);
                } else {
                    ClientPreviewState.clear();
                }
                BlockwrightWebBridge.completePreviewRequest(requestId, ok, message, plan);
            });
        });
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, S2C_WEB_BAKE_RESULT, (buf, context) -> {
            String requestId = buf.readUtf(128);
            boolean ok = buf.readBoolean();
            String message = buf.readUtf(MAX_GRAPH_JSON_CHARS);
            context.queue(() -> BlockwrightWebBridge.completeBakeRequest(requestId, ok, message));
        });
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


    public static void sendWebGraphPreviewRequest(String requestId, String presetId, String graphJson, String rawOverrides) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(requestId == null ? "" : requestId, 128);
        buf.writeUtf(presetId == null ? "" : presetId, 256);
        buf.writeUtf(graphJson == null ? "{}" : graphJson, MAX_GRAPH_JSON_CHARS);
        buf.writeUtf(rawOverrides == null ? "" : rawOverrides, MAX_GRAPH_JSON_CHARS);
        NetworkManager.sendToServer(C2S_WEB_GRAPH_PREVIEW, buf);
    }

    public static void sendWebBakeRequest(String requestId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(requestId == null ? "" : requestId, 128);
        NetworkManager.sendToServer(C2S_WEB_BAKE, buf);
    }

    private static void handleWebGraphPreview(ServerPlayer player, String requestId, String presetId, String graphJson, String rawOverrides) {
        PreviewPlan plan = new PreviewPlan(presetId == null ? "" : presetId);
        if (player == null) {
            plan.addIssue(PreviewSeverity.ERROR, "Web preview requires a server player.");
            sendWebPreviewResult(player, requestId, false, "No server player.", plan);
            return;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        if (!hasWebBuildPermission(player, session)) {
            plan.addIssue(PreviewSeverity.ERROR, "Player is not allowed to run Blockwright web previews.");
            sendWebPreviewResult(player, requestId, false, "Missing Blockwright build permission.", plan);
            return;
        }
        var lookup = Blockwright.getPackManager().findPreset(presetId).orElse(null);
        if (lookup == null) {
            plan.addIssue(PreviewSeverity.ERROR, "Preset not found: " + presetId);
            sendWebPreviewResult(player, requestId, false, "Preset not found: " + presetId, plan);
            return;
        }
        RuleDefinition rule = lookup.pack().getRules().get(lookup.preset().rule);
        if (rule == null) {
            plan.addIssue(PreviewSeverity.ERROR, "Preset rule not found: " + lookup.preset().rule);
            sendWebPreviewResult(player, requestId, false, "Preset rule not found.", plan);
            return;
        }
        if (!"pcg_graph".equals(rule.executor) && !"box_building_skeleton".equals(rule.executor) && !"spline_road_skeleton".equals(rule.executor)) {
            plan.addIssue(PreviewSeverity.ERROR, "Web editor can only preview PCG graph-compatible rules, got: " + rule.executor);
            sendWebPreviewResult(player, requestId, false, "Rule is not PCG graph-compatible.", plan);
            return;
        }
        if (!validateSelectionLimits(session, plan)) {
            sendWebPreviewResult(player, requestId, false, "Selection exceeds configured limits.", plan);
            return;
        }
        JsonObject graphObject;
        try {
            JsonElement element = JsonParser.parseString(graphJson == null || graphJson.isBlank() ? "{}" : graphJson);
            if (element == null || !element.isJsonObject()) {
                plan.addIssue(PreviewSeverity.ERROR, "Web graph payload root must be a JSON object.");
                sendWebPreviewResult(player, requestId, false, "Graph root must be an object.", plan);
                return;
            }
            graphObject = element.getAsJsonObject();
        } catch (RuntimeException exception) {
            plan.addIssue(PreviewSeverity.ERROR, "Failed to parse web graph: " + exception.getMessage());
            sendWebPreviewResult(player, requestId, false, "Invalid graph JSON.", plan);
            return;
        }

        PcgGraphExecutor executor = new PcgGraphExecutor();
        plan = executor.execute(new PresetExecutionContext(
                player,
                session,
                lookup.pack(),
                lookup.preset(),
                rule,
                parseOverrides(rawOverrides),
                lookup.pack().getModules()
        ), graphObject);
        int maxPreviewBlocks = BlockwrightConfig.get().maxPreviewBlocks;
        if (plan.getPlannedBlocks().size() > maxPreviewBlocks) {
            plan.addIssue(PreviewSeverity.ERROR, "Preview exceeds maxPreviewBlocks=" + maxPreviewBlocks + ".");
        }
        session.setPreviewPlan(plan);
        sendPreview(player, plan);
        boolean ok = plan.getOverallSeverity() != PreviewSeverity.ERROR;
        sendWebPreviewResult(player, requestId, ok, "Preview generated: " + plan.getPlannedBlocks().size() + " block(s), severity=" + plan.getOverallSeverity(), plan);
    }

    private static void handleWebBake(ServerPlayer player, String requestId) {
        if (player == null) {
            return;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        if (!hasWebBuildPermission(player, session)) {
            sendWebBakeResult(player, requestId, false, "Missing Blockwright build permission.");
            return;
        }
        PreviewPlan plan = session.getPreviewPlan();
        if (plan == null) {
            sendWebBakeResult(player, requestId, false, "No preview is available.");
            return;
        }
        if (!plan.canBake()) {
            sendWebBakeResult(player, requestId, false, plan.isStale() ? "Preview is stale and must be regenerated." : "Preview has errors and cannot be baked.");
            return;
        }
        int maxBakeBlocks = BlockwrightConfig.get().maxBakeBlocks;
        if (plan.getPlannedBlocks().size() > maxBakeBlocks) {
            sendWebBakeResult(player, requestId, false, "Preview exceeds maxBakeBlocks=" + maxBakeBlocks + ".");
            return;
        }
        try {
            var undoEntries = StructurePlacer.bake(player.serverLevel(), plan);
            session.pushUndo(undoEntries, BlockwrightConfig.get().maxUndoHistory);
            session.setPreviewPlan(null);
            sendPreviewClear(player);
            sendWebBakeResult(player, requestId, true, "Bake complete: " + undoEntries.size() + " block(s) captured for undo.");
        } catch (Exception exception) {
            sendWebBakeResult(player, requestId, false, "Bake failed: " + exception.getMessage());
        }
    }

    private static boolean hasWebBuildPermission(ServerPlayer player, PlayerSession session) {
        if (player == null || session == null) {
            return false;
        }
        boolean editorPermitted = !player.getServer().isDedicatedServer() || player.hasPermissions(2);
        return editorPermitted && (player.isCreative() || session.isEditorSpectatorMode());
    }

    private static boolean validateSelectionLimits(PlayerSession session, PreviewPlan plan) {
        if (session.getRegionSelection().isComplete() && session.getRegionSelection().getVolume() > BlockwrightConfig.get().maxSelectionVolume) {
            plan.addIssue(PreviewSeverity.ERROR, "Selection exceeds maxSelectionVolume=" + BlockwrightConfig.get().maxSelectionVolume + ".");
            return false;
        }
        if (session.getSplineSelection().getPoints().size() > BlockwrightConfig.get().maxSplinePoints) {
            plan.addIssue(PreviewSeverity.ERROR, "Spline exceeds maxSplinePoints=" + BlockwrightConfig.get().maxSplinePoints + ".");
            return false;
        }
        return true;
    }

    private static void sendWebPreviewResult(ServerPlayer player, String requestId, boolean ok, String message, PreviewPlan plan) {
        if (player == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(requestId == null ? "" : requestId, 128);
        buf.writeBoolean(ok);
        buf.writeUtf(message == null ? "" : message, MAX_GRAPH_JSON_CHARS);
        writePreview(buf, plan == null ? new PreviewPlan("") : plan);
        NetworkManager.sendToPlayer(player, S2C_WEB_PREVIEW_RESULT, buf);
    }

    private static void sendWebBakeResult(ServerPlayer player, String requestId, boolean ok, String message) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(requestId == null ? "" : requestId, 128);
        buf.writeBoolean(ok);
        buf.writeUtf(message == null ? "" : message, MAX_GRAPH_JSON_CHARS);
        NetworkManager.sendToPlayer(player, S2C_WEB_BAKE_RESULT, buf);
    }

    private static java.util.Map<String, String> parseOverrides(String rawOverrides) {
        java.util.Map<String, String> overrides = new java.util.LinkedHashMap<>();
        if (rawOverrides == null || rawOverrides.isBlank()) {
            return overrides;
        }
        for (String token : rawOverrides.split(",")) {
            String trimmed = token.trim();
            int index = trimmed.indexOf('=');
            if (index <= 0) {
                continue;
            }
            overrides.put(trimmed.substring(0, index).trim(), trimmed.substring(index + 1).trim());
        }
        return overrides;
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
        buf.writeVarInt(plan.getNodeSummaries().size());
        for (PreviewNodeSummary summary : plan.getNodeSummaries()) {
            buf.writeUtf(summary.getNodeId());
            buf.writeUtf(summary.getType());
            buf.writeVarInt(summary.getOrder());
            buf.writeVarInt(summary.getInputCount());
            buf.writeVarInt(summary.getPointCount());
            buf.writeVarInt(summary.getVolumeCount());
            buf.writeVarInt(summary.getPlannedBlockDelta());
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
        int summaryCount = buf.readVarInt();
        for (int i = 0; i < summaryCount; i++) {
            plan.addNodeSummary(new PreviewNodeSummary(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            ));
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
