package top.huliawsl.blockwright.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.pack.BlockwrightPackManager;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preview.PreviewIssue;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.rule.PresetExecutionContext;
import top.huliawsl.blockwright.rule.PresetExecutor;
import top.huliawsl.blockwright.rule.PresetExecutorRegistry;
import top.huliawsl.blockwright.rule.model.RuleDefinition;
import top.huliawsl.blockwright.session.PlayerSession;
import top.huliawsl.blockwright.util.ValidationIssue;
import top.huliawsl.blockwright.world.StructurePlacer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockwrightCommands {
    private static final int MAX_UNDO_HISTORY = 8;

    private BlockwrightCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        LiteralArgumentBuilder<CommandSourceStack> regionCommand = Commands.literal("region")
                .requires(BlockwrightCommands::hasBuildPermission)
                .then(Commands.literal("pos1")
                        .executes(command -> setRegionCorner(command.getSource(), true)))
                .then(Commands.literal("pos2")
                        .executes(command -> setRegionCorner(command.getSource(), false)))
                .then(Commands.literal("clear")
                        .executes(command -> clearRegion(command.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1", IntegerArgumentType.integer())
                                        .then(Commands.argument("z1", IntegerArgumentType.integer())
                                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                                        .then(Commands.argument("y2", IntegerArgumentType.integer())
                                                                .then(Commands.argument("z2", IntegerArgumentType.integer())
                                                                        .executes(BlockwrightCommands::setRegionCoordinates))))))));

        LiteralArgumentBuilder<CommandSourceStack> splineCommand = Commands.literal("spline")
                .requires(BlockwrightCommands::hasBuildPermission)
                .then(Commands.literal("add")
                        .executes(command -> addSplinePoint(command.getSource())))
                .then(Commands.literal("addpos")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(BlockwrightCommands::addSplineCoordinate)))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(command -> removeSplinePoint(command.getSource(), IntegerArgumentType.getInteger(command, "index")))))
                .then(Commands.literal("clear")
                        .executes(command -> clearSpline(command.getSource())))
                .then(Commands.literal("list")
                        .executes(command -> listSpline(command.getSource())));

        dispatcher.register(Commands.literal("blockwright")
                .then(Commands.literal("open")
                        .executes(command -> {
                            send(command, "Use the Blockwright hotkey (default G) to open the GUI.");
                            return 1;
                        }))
                .then(Commands.literal("reload")
                        .requires(BlockwrightCommands::hasBuildPermission)
                        .executes(command -> reloadPacks(command.getSource())))
                .then(Commands.literal("presets")
                        .then(Commands.literal("list")
                                .executes(command -> listPresets(command.getSource()))))
                .then(Commands.literal("modules")
                        .then(Commands.literal("validate")
                                .executes(command -> validateModules(command.getSource()))))
                .then(Commands.literal("preset")
                        .then(Commands.literal("validate")
                                .then(Commands.argument("preset_id", StringArgumentType.word())
                                        .executes(command -> validatePreset(command.getSource(), StringArgumentType.getString(command, "preset_id"))))))
                .then(Commands.literal("preview")
                        .requires(BlockwrightCommands::hasBuildPermission)
                        .then(Commands.literal("generate")
                                .then(Commands.argument("preset_id", StringArgumentType.word())
                                        .executes(command -> previewGenerate(command, StringArgumentType.getString(command, "preset_id"), ""))
                                        .then(Commands.argument("overrides", StringArgumentType.greedyString())
                                                .executes(command -> previewGenerate(command, StringArgumentType.getString(command, "preset_id"),
                                                        StringArgumentType.getString(command, "overrides"))))))
                        .then(Commands.literal("clear")
                                .executes(command -> previewClear(command.getSource()))))
                .then(Commands.literal("bake")
                        .requires(BlockwrightCommands::hasBuildPermission)
                        .executes(command -> bake(command.getSource())))
                .then(Commands.literal("undo")
                        .requires(BlockwrightCommands::hasBuildPermission)
                        .executes(command -> undo(command.getSource())))
                .then(regionCommand)
                .then(splineCommand));
    }

    private static boolean hasBuildPermission(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !player.isCreative()) {
            return false;
        }
        return !source.getServer().isDedicatedServer() || source.hasPermission(2);
    }

    private static int reloadPacks(CommandSourceStack source) {
        Blockwright.getPackManager().reload();
        send(source, "Reloaded " + Blockwright.getPackManager().getLoadedPackCount() + " preset pack(s).");
        return 1;
    }

    private static int listPresets(CommandSourceStack source) {
        List<String> presetIds = Blockwright.getPackManager().getAllPresets().stream().map(preset -> preset.id).toList();
        if (presetIds.isEmpty()) {
            send(source, "No presets loaded.");
            return 0;
        }
        send(source, "Loaded presets: " + String.join(", ", presetIds));
        return presetIds.size();
    }

    private static int validateModules(CommandSourceStack source) {
        int issueCount = 0;
        for (LoadedPack pack : Blockwright.getPackManager().getLoadedPacks()) {
            issueCount += printValidationIssues(source, pack);
        }
        if (issueCount == 0) {
            send(source, "No validation issues.");
        }
        return issueCount;
    }

    private static int validatePreset(CommandSourceStack source, String presetId) {
        BlockwrightPackManager.PresetLookup lookup = Blockwright.getPackManager().findPreset(presetId).orElse(null);
        if (lookup == null) {
            send(source, "Preset not found: " + presetId);
            return 0;
        }
        int count = printValidationIssues(source, lookup.pack());
        if (count == 0) {
            send(source, "Preset " + presetId + " has no pack-level validation issues.");
        }
        return count;
    }

    private static int previewGenerate(CommandContext<CommandSourceStack> command, String presetId, String rawOverrides) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        BlockwrightPackManager.PresetLookup lookup = Blockwright.getPackManager().findPreset(presetId).orElse(null);
        if (lookup == null) {
            send(source, "Preset not found: " + presetId);
            return 0;
        }
        RuleDefinition rule = lookup.pack().getRules().get(lookup.preset().rule);
        if (rule == null) {
            send(source, "Preset rule not found: " + lookup.preset().rule);
            return 0;
        }
        PresetExecutor executor = PresetExecutorRegistry.get(rule.executor).orElse(null);
        if (executor == null) {
            send(source, "No executor registered for rule type: " + rule.executor);
            return 0;
        }

        PreviewPlan plan = executor.execute(new PresetExecutionContext(
                player,
                session,
                lookup.pack(),
                lookup.preset(),
                rule,
                parseOverrides(rawOverrides),
                lookup.pack().getModules()
        ));
        session.setPreviewPlan(plan);
        send(source, "Preview generated for " + presetId + ": " + plan.getPlannedBlocks().size() + " block(s), severity=" + plan.getOverallSeverity());
        for (PreviewIssue issue : plan.getIssues()) {
            send(source, issue.getSeverity() + ": " + issue.getMessage());
        }
        return plan.getPlannedBlocks().size();
    }

    private static int previewClear(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        Blockwright.getSessionManager().getOrCreate(player).setPreviewPlan(null);
        send(source, "Preview cleared.");
        return 1;
    }

    private static int bake(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        PreviewPlan plan = session.getPreviewPlan();
        if (plan == null) {
            send(source, "No preview is available.");
            return 0;
        }
        if (!plan.canBake()) {
            send(source, "Preview has errors and cannot be baked.");
            return 0;
        }
        List<top.huliawsl.blockwright.world.UndoEntry> undoEntries = StructurePlacer.bake(player.serverLevel(), plan);
        session.pushUndo(undoEntries, MAX_UNDO_HISTORY);
        session.setPreviewPlan(null);
        send(source, "Bake complete: " + undoEntries.size() + " block(s) captured for undo.");
        return undoEntries.size();
    }

    private static int undo(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        List<top.huliawsl.blockwright.world.UndoEntry> undoEntries = session.popUndo();
        if (undoEntries == null || undoEntries.isEmpty()) {
            send(source, "Nothing to undo.");
            return 0;
        }
        StructurePlacer.undo(player.serverLevel(), undoEntries);
        send(source, "Undo complete: restored " + undoEntries.size() + " block(s).");
        return undoEntries.size();
    }

    private static int setRegionCorner(CommandSourceStack source, boolean firstCorner) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        if (firstCorner) {
            session.getRegionSelection().setPos1(player.blockPosition());
        } else {
            session.getRegionSelection().setPos2(player.blockPosition());
        }
        send(source, "Region " + (firstCorner ? "pos1" : "pos2") + " set to " + player.blockPosition().toShortString() + ".");
        return 1;
    }

    private static int setRegionCoordinates(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        PlayerSession session = Blockwright.getSessionManager().getOrCreate(player);
        session.getRegionSelection().setPos1(new net.minecraft.core.BlockPos(
                IntegerArgumentType.getInteger(command, "x1"),
                IntegerArgumentType.getInteger(command, "y1"),
                IntegerArgumentType.getInteger(command, "z1")
        ));
        session.getRegionSelection().setPos2(new net.minecraft.core.BlockPos(
                IntegerArgumentType.getInteger(command, "x2"),
                IntegerArgumentType.getInteger(command, "y2"),
                IntegerArgumentType.getInteger(command, "z2")
        ));
        send(source, "Region set: "
                + session.getRegionSelection().getMin().toShortString()
                + " -> " + session.getRegionSelection().getMax().toShortString());
        return 1;
    }

    private static int clearRegion(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        Blockwright.getSessionManager().getOrCreate(player).getRegionSelection().clear();
        send(source, "Region selection cleared.");
        return 1;
    }

    private static int addSplinePoint(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        Blockwright.getSessionManager().getOrCreate(player).getSplineSelection().addPoint(player.blockPosition());
        send(source, "Added spline point " + player.blockPosition().toShortString() + ".");
        return 1;
    }

    private static int addSplineCoordinate(CommandContext<CommandSourceStack> command) {
        CommandSourceStack source = command.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        net.minecraft.core.BlockPos point = new net.minecraft.core.BlockPos(
                IntegerArgumentType.getInteger(command, "x"),
                IntegerArgumentType.getInteger(command, "y"),
                IntegerArgumentType.getInteger(command, "z")
        );
        Blockwright.getSessionManager().getOrCreate(player).getSplineSelection().addPoint(point);
        send(source, "Added spline point " + point.toShortString() + ".");
        return 1;
    }

    private static int removeSplinePoint(CommandSourceStack source, int index) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        boolean removed = Blockwright.getSessionManager().getOrCreate(player).getSplineSelection().removeIndex(index);
        send(source, removed ? "Removed spline point #" + index + "." : "Spline point index out of range.");
        return removed ? 1 : 0;
    }

    private static int clearSpline(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        Blockwright.getSessionManager().getOrCreate(player).getSplineSelection().clear();
        send(source, "Spline cleared.");
        return 1;
    }

    private static int listSpline(CommandSourceStack source) {
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            return 0;
        }
        List<String> points = Blockwright.getSessionManager().getOrCreate(player).getSplineSelection().getPoints().stream()
                .map(net.minecraft.core.BlockPos::toShortString)
                .toList();
        send(source, points.isEmpty() ? "Spline has no control points." : "Spline points: " + String.join(" | ", points));
        return points.size();
    }

    private static int printValidationIssues(CommandSourceStack source, LoadedPack pack) {
        int count = 0;
        for (ValidationIssue issue : pack.getValidationReport().getIssues()) {
            send(source, "[" + pack.getMetadata().id + "] " + issue.getSeverity() + " " + issue.getLocation() + " - " + issue.getMessage());
            count++;
        }
        return count;
    }

    private static Map<String, String> parseOverrides(String rawOverrides) {
        Map<String, String> overrides = new LinkedHashMap<>();
        if (rawOverrides == null || rawOverrides.isBlank()) {
            return overrides;
        }
        Arrays.stream(rawOverrides.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank() && token.contains("="))
                .forEach(token -> {
                    int index = token.indexOf('=');
                    overrides.put(token.substring(0, index).trim(), token.substring(index + 1).trim());
                });
        return overrides;
    }

    private static void send(CommandContext<CommandSourceStack> command, String message) {
        send(command.getSource(), message);
    }

    private static void send(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            send(source, "This command can only be used by a player.");
        }
        return player;
    }
}
