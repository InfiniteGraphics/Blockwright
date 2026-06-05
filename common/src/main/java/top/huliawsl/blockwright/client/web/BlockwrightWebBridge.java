package top.huliawsl.blockwright.client.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.client.ClientPreviewState;
import top.huliawsl.blockwright.client.ClientSelectionState;
import top.huliawsl.blockwright.client.PcgEditorSession;
import top.huliawsl.blockwright.network.BlockwrightNetwork;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.pcg.PcgGraphIo;
import top.huliawsl.blockwright.pcg.schema.PcgNodeSchemaRegistry;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preview.PlannedBlock;
import top.huliawsl.blockwright.preview.PreviewIssue;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;
import top.huliawsl.blockwright.util.JsonHelper;
import top.huliawsl.blockwright.world.BlockResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class BlockwrightWebBridge {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, CompletableFuture<WebPreviewResult>> PREVIEW_REQUESTS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<WebBakeResult>> BAKE_REQUESTS = new ConcurrentHashMap<>();
    private static HttpServer server;
    private static String token;
    private static int port = -1;

    private BlockwrightWebBridge() {
    }

    public static synchronized URI openEditor() {
        try {
            ensureStarted();
            URI uri = URI.create("http://127.0.0.1:" + port + "/?token=" + token);
            Util.getPlatform().openUri(uri);
            return uri;
        } catch (Exception exception) {
            Blockwright.LOGGER.warn("Failed to open Blockwright web editor", exception);
            return null;
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            port = -1;
        }
        PREVIEW_REQUESTS.clear();
        BAKE_REQUESTS.clear();
    }

    public static void completePreviewRequest(String requestId, boolean ok, String message, PreviewPlan plan) {
        CompletableFuture<WebPreviewResult> future = PREVIEW_REQUESTS.remove(requestId);
        if (future != null) {
            future.complete(new WebPreviewResult(ok, message, plan));
        }
    }

    public static void completeBakeRequest(String requestId, boolean ok, String message) {
        CompletableFuture<WebBakeResult> future = BAKE_REQUESTS.remove(requestId);
        if (future != null) {
            future.complete(new WebBakeResult(ok, message));
        }
    }

    private static void ensureStarted() throws IOException {
        if (server != null) {
            return;
        }
        token = createToken();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 16);
        port = server.getAddress().getPort();
        server.createContext("/", BlockwrightWebBridge::handle);
        server.setExecutor(Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "Blockwright Web Bridge");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        Blockwright.LOGGER.info("Blockwright web editor bridge started on 127.0.0.1:{}", port);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        addBaseHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                if (!isAuthorized(exchange)) {
                    json(exchange, 401, error("Unauthorized web editor request."));
                    return;
                }
                handleApi(exchange, path);
                return;
            }
            serveStatic(exchange, path);
        } catch (Exception exception) {
            JsonObject response = error(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            json(exchange, 500, response);
        } finally {
            exchange.close();
        }
    }

    private static void handleApi(HttpExchange exchange, String path) throws Exception {
        String method = exchange.getRequestMethod();
        if ("GET".equals(method) && "/api/hello".equals(path)) {
            JsonObject response = new JsonObject();
            response.addProperty("ok", true);
            response.addProperty("app", "blockwright");
            response.addProperty("protocolVersion", 1);
            response.addProperty("bridge", "localhost");
            json(exchange, 200, response);
            return;
        }
        if ("GET".equals(method) && "/api/session".equals(path)) {
            json(exchange, 200, sessionJson());
            return;
        }
        if ("POST".equals(method) && "/api/session/select".equals(path)) {
            JsonObject body = readBodyObject(exchange);
            selectSessionPreset(body);
            json(exchange, 200, sessionJson());
            return;
        }
        if ("GET".equals(method) && "/api/node-schemas".equals(path)) {
            JsonObject response = new JsonObject();
            response.addProperty("ok", true);
            response.add("schemas", PcgNodeSchemaRegistry.toJson());
            json(exchange, 200, response);
            return;
        }
        if ("GET".equals(method) && "/api/preview".equals(path)) {
            JsonObject response = new JsonObject();
            response.addProperty("ok", true);
            response.add("preview", previewJson(ClientPreviewState.getPreviewPlan()));
            json(exchange, 200, response);
            return;
        }
        if ("POST".equals(method) && "/api/graph/preview".equals(path)) {
            json(exchange, 200, requestPreview(readBodyObject(exchange)));
            return;
        }
        if ("POST".equals(method) && "/api/graph/save".equals(path)) {
            json(exchange, 200, saveGraph(readBodyObject(exchange)));
            return;
        }
        if ("POST".equals(method) && "/api/graph/bake".equals(path)) {
            json(exchange, 200, requestBake());
            return;
        }
        json(exchange, 404, error("Unknown API endpoint: " + path));
    }

    private static JsonObject sessionJson() {
        PcgEditorSession session = PcgEditorSession.get();
        session.ensureDefaults();
        JsonObject response = new JsonObject();
        response.addProperty("ok", true);
        response.addProperty("sessionId", "local");
        response.addProperty("packId", empty(session.getSelectedPackId()));
        response.addProperty("presetId", empty(session.getSelectedPresetId()));
        response.add("packs", packsJson());
        response.add("selection", selectionJson());
        response.add("preview", previewJson(ClientPreviewState.getPreviewPlan()));
        LoadedPack pack = session.getSelectedPack();
        PresetDefinition preset = session.getSelectedPreset();
        response.add("graph", graphJson(pack, preset));
        response.addProperty("devSaveAvailable", canDevSave());
        return response;
    }

    private static JsonArray packsJson() {
        JsonArray packs = new JsonArray();
        for (LoadedPack pack : Blockwright.getPackManager().getLoadedPacks()) {
            JsonObject packJson = new JsonObject();
            packJson.addProperty("id", pack.getMetadata().id);
            packJson.addProperty("name", pack.getMetadata().name == null ? pack.getMetadata().id : pack.getMetadata().name);
            JsonArray presets = new JsonArray();
            for (PresetDefinition preset : pack.getPresets().values()) {
                JsonObject presetJson = new JsonObject();
                presetJson.addProperty("id", preset.id);
                presetJson.addProperty("name", preset.name == null || preset.name.isBlank() ? preset.id : preset.name);
                presetJson.addProperty("type", preset.type == null ? "" : preset.type);
                presetJson.addProperty("rule", preset.rule == null ? "" : preset.rule);
                RuleDefinition rule = pack.getRules().get(preset.rule);
                presetJson.addProperty("executor", rule == null ? "" : rule.executor);
                presets.add(presetJson);
            }
            packJson.add("presets", presets);
            packs.add(packJson);
        }
        return packs;
    }

    private static JsonObject graphJson(LoadedPack pack, PresetDefinition preset) {
        if (pack == null || preset == null) {
            return defaultGraph();
        }
        RuleDefinition rule = pack.getRules().get(preset.rule);
        JsonObject graph = PcgGraphIo.loadGraphObject(pack, rule, null);
        return graph == null ? defaultGraph() : graph;
    }

    private static JsonObject defaultGraph() {
        JsonObject graph = new JsonObject();
        graph.addProperty("debug", false);
        graph.add("nodes", new JsonArray());
        graph.add("edges", new JsonArray());
        return graph;
    }

    private static JsonObject selectionJson() {
        JsonObject selection = new JsonObject();
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        JsonObject regionJson = new JsonObject();
        regionJson.addProperty("complete", region.isComplete());
        regionJson.add("pos1", posJson(region.getPos1()));
        regionJson.add("pos2", posJson(region.getPos2()));
        if (region.isComplete()) {
            regionJson.add("min", posJson(region.getMin()));
            regionJson.add("max", posJson(region.getMax()));
            regionJson.addProperty("volume", region.getVolume());
        }
        selection.add("region", regionJson);
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        JsonArray points = new JsonArray();
        for (BlockPos point : spline.getPoints()) {
            points.add(posJson(point));
        }
        JsonObject splineJson = new JsonObject();
        splineJson.addProperty("count", spline.getPoints().size());
        splineJson.add("points", points);
        selection.add("spline", splineJson);
        return selection;
    }

    private static JsonElement posJson(BlockPos pos) {
        if (pos == null) {
            return com.google.gson.JsonNull.INSTANCE;
        }
        JsonArray array = new JsonArray();
        array.add(pos.getX());
        array.add(pos.getY());
        array.add(pos.getZ());
        return array;
    }

    private static JsonObject previewJson(PreviewPlan plan) {
        JsonObject preview = new JsonObject();
        preview.addProperty("available", plan != null);
        if (plan == null) {
            return preview;
        }
        preview.addProperty("presetId", plan.getPresetId());
        preview.addProperty("stale", plan.isStale());
        preview.addProperty("blockCount", plan.getPlannedBlocks().size());
        preview.addProperty("severity", plan.getOverallSeverity().name());
        AABB bounds = plan.getBounds();
        JsonObject boundsJson = new JsonObject();
        boundsJson.addProperty("minX", bounds.minX);
        boundsJson.addProperty("minY", bounds.minY);
        boundsJson.addProperty("minZ", bounds.minZ);
        boundsJson.addProperty("maxX", bounds.maxX);
        boundsJson.addProperty("maxY", bounds.maxY);
        boundsJson.addProperty("maxZ", bounds.maxZ);
        preview.add("bounds", boundsJson);
        JsonArray issues = new JsonArray();
        for (PreviewIssue issue : plan.getIssues()) {
            JsonObject issueJson = new JsonObject();
            issueJson.addProperty("severity", issue.getSeverity().name());
            issueJson.addProperty("message", issue.getMessage());
            issues.add(issueJson);
        }
        preview.add("issues", issues);
        JsonArray nodeSummaries = new JsonArray();
        for (var summary : plan.getNodeSummaries()) {
            JsonObject summaryJson = new JsonObject();
            summaryJson.addProperty("nodeId", summary.getNodeId());
            summaryJson.addProperty("type", summary.getType());
            summaryJson.addProperty("order", summary.getOrder());
            summaryJson.addProperty("inputCount", summary.getInputCount());
            summaryJson.addProperty("pointCount", summary.getPointCount());
            summaryJson.addProperty("volumeCount", summary.getVolumeCount());
            summaryJson.addProperty("plannedBlockDelta", summary.getPlannedBlockDelta());
            nodeSummaries.add(summaryJson);
        }
        preview.add("nodeSummaries", nodeSummaries);
        JsonArray sampleBlocks = new JsonArray();
        int limit = Math.min(128, plan.getPlannedBlocks().size());
        for (int i = 0; i < limit; i++) {
            PlannedBlock block = plan.getPlannedBlocks().get(i);
            JsonObject blockJson = new JsonObject();
            blockJson.add("pos", posJson(block.getPos()));
            blockJson.addProperty("state", BlockResolver.serialize(block.getState()));
            sampleBlocks.add(blockJson);
        }
        preview.add("sampleBlocks", sampleBlocks);
        return preview;
    }

    private static void selectSessionPreset(JsonObject body) {
        String packId = string(body, "packId", "");
        String presetId = string(body, "presetId", "");
        PcgEditorSession session = PcgEditorSession.get();
        if (!packId.isBlank()) {
            session.setSelectedPackId(packId);
        }
        if (!presetId.isBlank()) {
            session.setSelectedPresetId(presetId);
        }
        session.markDirty("Web editor selected preset " + presetId + ".");
    }

    private static JsonObject requestPreview(JsonObject body) throws Exception {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) {
            return error("Join a world before requesting preview.");
        }
        String presetId = string(body, "presetId", PcgEditorSession.get().getSelectedPresetId());
        JsonObject graph = body.has("graph") && body.get("graph").isJsonObject() ? body.getAsJsonObject("graph") : graphJson(PcgEditorSession.get().getSelectedPack(), PcgEditorSession.get().getSelectedPreset());
        String rawOverrides = overridesString(body);
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<WebPreviewResult> future = new CompletableFuture<>();
        PREVIEW_REQUESTS.put(requestId, future);
        ClientPreviewState.beginServerPreviewRequest();
        client.execute(() -> BlockwrightNetwork.sendWebGraphPreviewRequest(requestId, presetId, JsonHelper.GSON.toJson(graph), rawOverrides));
        try {
            WebPreviewResult result = future.get(12, TimeUnit.SECONDS);
            JsonObject response = new JsonObject();
            response.addProperty("ok", result.ok());
            response.addProperty("message", result.message());
            response.add("preview", previewJson(result.plan()));
            return response;
        } finally {
            PREVIEW_REQUESTS.remove(requestId);
        }
    }

    private static JsonObject requestBake() throws Exception {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.connection == null) {
            return error("Join a world before baking.");
        }
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<WebBakeResult> future = new CompletableFuture<>();
        BAKE_REQUESTS.put(requestId, future);
        client.execute(() -> BlockwrightNetwork.sendWebBakeRequest(requestId));
        try {
            WebBakeResult result = future.get(12, TimeUnit.SECONDS);
            JsonObject response = new JsonObject();
            response.addProperty("ok", result.ok());
            response.addProperty("message", result.message());
            response.add("preview", previewJson(ClientPreviewState.getPreviewPlan()));
            return response;
        } finally {
            BAKE_REQUESTS.remove(requestId);
        }
    }

    private static JsonObject saveGraph(JsonObject body) throws Exception {
        if (!canDevSave()) {
            return error("Graph save is only enabled in integrated single-player/dev sessions. Use Export JSON on multiplayer servers.");
        }
        PcgEditorSession session = PcgEditorSession.get();
        LoadedPack pack = session.getSelectedPack();
        PresetDefinition preset = session.getSelectedPreset();
        String packId = string(body, "packId", session.getSelectedPackId());
        String presetId = string(body, "presetId", session.getSelectedPresetId());
        if (packId != null && !packId.isBlank()) {
            pack = Blockwright.getPackManager().findPack(packId).orElse(pack);
        }
        if (pack != null && presetId != null && !presetId.isBlank()) {
            preset = pack.getPresets().getOrDefault(presetId, preset);
        }
        if (pack == null || preset == null) {
            return error("No pack/preset is selected.");
        }
        RuleDefinition rule = pack.getRules().get(preset.rule);
        if (rule == null) {
            return error("Selected preset has no rule: " + preset.rule);
        }
        JsonObject graph = body.has("graph") && body.get("graph").isJsonObject() ? body.getAsJsonObject("graph") : null;
        if (graph == null) {
            return error("Request body requires graph object.");
        }
        boolean saved = PcgGraphIo.saveGraphObject(pack, rule, graph);
        if (!saved) {
            return error("Could not save graph because the rule source file is missing.");
        }
        Blockwright.getPackManager().reload();
        session.setSelectedPackId(pack.getMetadata().id);
        session.setSelectedPresetId(preset.id);
        session.markDirty("Graph saved from web editor.");
        JsonObject response = new JsonObject();
        response.addProperty("ok", true);
        response.addProperty("message", "Graph saved and packs reloaded.");
        response.add("session", sessionJson());
        return response;
    }

    private static boolean canDevSave() {
        Minecraft client = Minecraft.getInstance();
        return client.hasSingleplayerServer();
    }

    private static String overridesString(JsonObject body) {
        if (!body.has("overrides") || !body.get("overrides").isJsonObject()) {
            return "";
        }
        JsonObject overrides = body.getAsJsonObject("overrides");
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : entry.getValue().toString());
        }
        return builder.toString();
    }

    private static JsonObject readBodyObject(HttpExchange exchange) throws IOException {
        String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            return new JsonObject();
        }
        JsonElement element = JsonParser.parseString(raw);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Request body must be a JSON object.");
        }
        return element.getAsJsonObject();
    }

    private static void serveStatic(HttpExchange exchange, String path) throws IOException {
        String normalized = path == null || path.equals("/") ? "/index.html" : path;
        if (normalized.contains("..")) {
            text(exchange, 400, "Bad path", "text/plain; charset=utf-8");
            return;
        }
        String resourcePath = "/assets/blockwright/web" + normalized;
        try (InputStream stream = BlockwrightWebBridge.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                try (InputStream fallback = BlockwrightWebBridge.class.getResourceAsStream("/assets/blockwright/web/index.html")) {
                    if (fallback == null) {
                        text(exchange, 404, "Blockwright web editor assets are missing.", "text/plain; charset=utf-8");
                        return;
                    }
                    bytes(exchange, 200, fallback.readAllBytes(), "text/html; charset=utf-8");
                }
                return;
            }
            bytes(exchange, 200, stream.readAllBytes(), contentType(normalized));
        }
    }

    private static boolean isAuthorized(HttpExchange exchange) {
        String supplied = queryParam(exchange.getRequestURI(), "token");
        if (supplied == null || supplied.isBlank()) {
            supplied = exchange.getRequestHeaders().getFirst("X-Blockwright-Token");
        }
        if (supplied == null || supplied.isBlank()) {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                supplied = authorization.substring("Bearer ".length());
            }
        }
        return token != null && token.equals(supplied);
    }

    private static String queryParam(URI uri, String key) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String part : query.split("&")) {
            int index = part.indexOf('=');
            String name = index < 0 ? part : part.substring(0, index);
            if (key.equals(name)) {
                return index < 0 ? "" : java.net.URLDecoder.decode(part.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static void addBaseHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://127.0.0.1:" + port);
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,X-Blockwright-Token,Authorization");
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
    }

    private static void json(HttpExchange exchange, int status, JsonObject body) throws IOException {
        bytes(exchange, status, JsonHelper.GSON.toJson(body).getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8");
    }

    private static void text(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        bytes(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void bytes(HttpExchange exchange, int status, byte[] body, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (path.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (path.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static JsonObject error(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("ok", false);
        response.addProperty("message", message == null ? "Unknown error." : message);
        return response;
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsString();
        }
        return fallback == null ? "" : fallback;
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    private static String createToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record WebPreviewResult(boolean ok, String message, PreviewPlan plan) {
    }

    private record WebBakeResult(boolean ok, String message) {
    }
}
