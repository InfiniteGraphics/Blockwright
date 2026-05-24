package top.huliawsl.blockwright.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preview.PreviewIssue;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetInputDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;
import top.huliawsl.blockwright.util.ValidationIssue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockwrightMainScreen extends Screen {
    private static final int PANEL_BG = 0xB0151A21;
    private static final int PANEL_BORDER = 0xFF39424D;
    private static final int TEXT_MUTED = 0xFF99A2AD;
    private static final int TEXT_BRIGHT = 0xFFF4F7FA;
    private static final int STATUS_OK = 0xFF69D18A;
    private static final int STATUS_WARN = 0xFFF2C94C;
    private static final int STATUS_ERROR = 0xFFF25F5C;
    private static final int STATUS_INFO = 0xFF5DA9E9;

    private static int selectedPresetIndex;

    private final List<EditBox> parameterBoxes = new ArrayList<>();
    private final List<String> parameterKeys = new ArrayList<>();

    private int contentLeft;
    private int top;
    private int panelBottom;
    private int leftWidth;
    private int middleWidth;
    private int rightWidth;
    private int middleX;
    private int rightX;
    private int parameterViewportTop;
    private int parameterViewportBottom;
    private int parameterScroll;
    private int maxParameterScroll;

    private Button previewButton;
    private Button bakeButton;
    private Button undoButton;
    private Button clearPreviewButton;

    public BlockwrightMainScreen() {
        super(Component.literal("Blockwright"));
    }

    @Override
    protected void init() {
        clearWidgets();
        parameterBoxes.clear();
        parameterKeys.clear();
        initLayout();

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePreset(-1))
                .bounds(middleX + 12, top + 18, 24, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePreset(1))
                .bounds(middleX + middleWidth - 36, top + 18, 24, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadPacks())
                .bounds(rightX + 12, top + 18, rightWidth - 24, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(rightX + 12, top + 42, rightWidth - 24, 20)
                .build());

        int regionY = top + 90;
        addRenderableWidget(Button.builder(Component.literal("Set P1"), button -> sendCommand("blockwright region pos1"))
                .bounds(contentLeft + 12, regionY + 42, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Set P2"), button -> sendCommand("blockwright region pos2"))
                .bounds(contentLeft + 92, regionY + 42, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), button -> sendCommand("blockwright region clear"))
                .bounds(contentLeft + 12, regionY + 66, 150, 20)
                .build());

        int splineY = top + 212;
        addRenderableWidget(Button.builder(Component.literal("Add"), button -> sendCommand("blockwright spline add"))
                .bounds(contentLeft + 12, splineY + 30, 46, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("List"), button -> sendCommand("blockwright spline list"))
                .bounds(contentLeft + 62, splineY + 30, 46, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), button -> sendCommand("blockwright spline clear"))
                .bounds(contentLeft + 112, splineY + 30, 50, 20)
                .build());

        PresetDefinition preset = getSelectedPreset();
        if (preset != null) {
            for (Map.Entry<String, PresetParameterDefinition> entry : preset.parameters.entrySet()) {
                if (!entry.getValue().exposed) {
                    continue;
                }
                parameterKeys.add(entry.getKey());
                EditBox box = new EditBox(this.font, 0, 0, middleWidth - 108, 18, Component.literal(entry.getKey()));
                box.setValue(entry.getValue().defaultValue == null ? "" : entry.getValue().defaultValue.getAsString());
                box.setResponder(value -> ClientPreviewState.markStale());
                addRenderableWidget(box);
                parameterBoxes.add(box);
            }
        }

        previewButton = addRenderableWidget(Button.builder(Component.literal("Preview"), button -> sendPreviewCommand())
                .bounds(rightX + 12, top + 94, rightWidth - 24, 20)
                .build());
        bakeButton = addRenderableWidget(Button.builder(Component.literal("Bake"), button -> sendCommand("blockwright bake"))
                .bounds(rightX + 12, top + 118, rightWidth - 24, 20)
                .build());
        undoButton = addRenderableWidget(Button.builder(Component.literal("Undo"), button -> sendCommand("blockwright undo"))
                .bounds(rightX + 12, top + 142, rightWidth - 24, 20)
                .build());
        clearPreviewButton = addRenderableWidget(Button.builder(Component.literal("Clear Preview"), button -> sendCommand("blockwright preview clear"))
                .bounds(rightX + 12, top + 166, rightWidth - 24, 20)
                .build());

        layoutParameterFields();
        updateActionStates();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        drawAtmosphere(guiGraphics);

        drawPanel(guiGraphics, contentLeft, top, leftWidth, panelBottom - top);
        drawPanel(guiGraphics, middleX, top, middleWidth, panelBottom - top);
        drawPanel(guiGraphics, rightX, top, rightWidth, panelBottom - top);

        LoadedPack selectedPack = getSelectedPack();
        PresetDefinition preset = getSelectedPreset();
        PreviewPlan previewPlan = getVisiblePreviewPlan();
        BoxRegionSelection regionSelection = ClientSelectionState.getRegionSelection();
        SplineSelection splineSelection = ClientSelectionState.getSplineSelection();

        guiGraphics.drawString(this.font, this.title, contentLeft + 12, top - 12, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Input", contentLeft + 12, top + 8, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Preset", middleX + 12, top + 8, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Status", rightX + 12, top + 8, TEXT_BRIGHT);

        guiGraphics.drawString(this.font, selectedPack == null ? "Pack: <none>" : "Pack: " + selectedPack.getMetadata().id,
                middleX + 44, top + 22, TEXT_MUTED);
        guiGraphics.drawString(this.font, preset == null ? "Preset: <none>" : "Preset: " + preset.id,
                middleX + 44, top + 34, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Mode: " + describePresetMode(preset), middleX + 12, top + 58, TEXT_MUTED);

        drawRegionSection(guiGraphics, contentLeft + 12, top + 90, regionSelection);
        drawSplineSection(guiGraphics, contentLeft + 12, top + 212, splineSelection);
        drawInputHints(guiGraphics, contentLeft + 12, panelBottom - 34);
        drawParameterLabels(guiGraphics, middleX + 12, preset);
        drawStatusPanel(guiGraphics, rightX + 12, top + 204, rightWidth - 24, previewPlan, selectedPack);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        updateActionStates();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= middleX && mouseX <= middleX + middleWidth && mouseY >= parameterViewportTop - 8 && mouseY <= parameterViewportBottom + 8) {
            int nextScroll = parameterScroll - (int) (delta * 16.0D);
            parameterScroll = Math.max(0, Math.min(maxParameterScroll, nextScroll));
            layoutParameterFields();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void initLayout() {
        contentLeft = Math.max(12, this.width / 2 - 286);
        top = 18;
        panelBottom = this.height - 18;
        leftWidth = 176;
        middleWidth = 230;
        rightWidth = 176;
        middleX = contentLeft + leftWidth + 10;
        rightX = middleX + middleWidth + 10;
        parameterViewportTop = top + 112;
        parameterViewportBottom = panelBottom - 14;
    }

    private void layoutParameterFields() {
        int viewportHeight = Math.max(40, parameterViewportBottom - parameterViewportTop);
        int contentHeight = Math.max(0, parameterKeys.size() * 30 - 10);
        maxParameterScroll = Math.max(0, contentHeight - viewportHeight);
        parameterScroll = Math.max(0, Math.min(parameterScroll, maxParameterScroll));

        int baseY = parameterViewportTop - parameterScroll;
        for (int i = 0; i < parameterBoxes.size(); i++) {
            EditBox box = parameterBoxes.get(i);
            int y = baseY + i * 30;
            boolean visible = y >= parameterViewportTop - 6 && y + box.getHeight() <= parameterViewportBottom + 6;
            box.setX(middleX + 88);
            box.setY(y);
            box.setVisible(visible);
            box.active = visible;
            if (!visible) {
                box.setFocused(false);
            }
        }
    }

    private void drawAtmosphere(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xDD0A1017, 0xEE101A25);
        guiGraphics.fillGradient(0, 0, this.width, this.height / 3, 0x441F4C7A, 0x001F4C7A);
        guiGraphics.fillGradient(0, this.height / 2, this.width, this.height, 0x003A6F5B, 0x44213B37);
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, PANEL_BG);
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        guiGraphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    private void drawRegionSection(GuiGraphics guiGraphics, int x, int y, BoxRegionSelection regionSelection) {
        guiGraphics.drawString(this.font, "Region", x, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "P1: " + formatPos(regionSelection.getPos1()), x, y + 16, 0xFFF2C94C);
        guiGraphics.drawString(this.font, "P2: " + formatPos(regionSelection.getPos2()), x, y + 28, 0xFF56CCF2);
        if (regionSelection.isComplete()) {
            guiGraphics.drawString(this.font, trimToWidth("Size: " + regionSelection.getWidth() + " x " + regionSelection.getHeight() + " x " + regionSelection.getDepth(), leftWidth - 24),
                    x, y + 94, STATUS_OK);
            guiGraphics.drawString(this.font, "Volume: " + regionSelection.getVolume(), x, y + 106, TEXT_MUTED);
        } else {
            guiGraphics.drawString(this.font, "Set both corners before preview.", x, y + 94, TEXT_MUTED);
        }
    }

    private void drawSplineSection(GuiGraphics guiGraphics, int x, int y, SplineSelection splineSelection) {
        guiGraphics.drawString(this.font, "Spline", x, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Points: " + splineSelection.getPoints().size(), x, y + 16, TEXT_MUTED);
        if (splineSelection.getPoints().isEmpty()) {
            guiGraphics.drawString(this.font, "No control points.", x, y + 58, TEXT_MUTED);
            return;
        }

        int lineY = y + 58;
        for (int i = 0; i < Math.min(5, splineSelection.getPoints().size()); i++) {
            guiGraphics.drawString(this.font, trimToWidth("#" + i + " " + splineSelection.getPoints().get(i).toShortString(), leftWidth - 24), x, lineY, TEXT_BRIGHT);
            lineY += 12;
        }
        if (splineSelection.getPoints().size() > 5) {
            guiGraphics.drawString(this.font, "...", x, lineY, TEXT_MUTED);
        }
    }

    private void drawInputHints(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.font, "1. 选择 Region 或 Spline", x, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, "2. 调参数后再 Preview", x, y + 12, TEXT_MUTED);
    }

    private void drawParameterLabels(GuiGraphics guiGraphics, int x, PresetDefinition preset) {
        guiGraphics.drawString(this.font, "Parameters", x, top + 88, TEXT_BRIGHT);
        if (preset == null) {
            guiGraphics.drawString(this.font, "No preset loaded.", x, parameterViewportTop, TEXT_MUTED);
            return;
        }
        if (maxParameterScroll > 0) {
            guiGraphics.drawString(this.font, "Scroll", middleX + middleWidth - 44, top + 88, TEXT_MUTED);
        }

        int baseY = parameterViewportTop - parameterScroll;
        for (int i = 0; i < parameterKeys.size(); i++) {
            int labelY = baseY + i * 30 + 5;
            if (labelY < parameterViewportTop - 2 || labelY > parameterViewportBottom - 10) {
                continue;
            }
            guiGraphics.drawString(this.font, parameterKeys.get(i), x, labelY, TEXT_MUTED);
        }
    }

    private void drawStatusPanel(GuiGraphics guiGraphics, int x, int y, int width, PreviewPlan previewPlan, LoadedPack selectedPack) {
        guiGraphics.drawString(this.font, "Preview", x, y, TEXT_BRIGHT);
        if (previewPlan == null) {
            guiGraphics.drawString(this.font, "No preview generated.", x, y + 16, TEXT_MUTED);
        } else {
            PreviewSeverity severity = previewPlan.getOverallSeverity();
            int color = switch (severity) {
                case ERROR -> STATUS_ERROR;
                case WARNING -> STATUS_WARN;
                default -> STATUS_OK;
            };
            guiGraphics.drawString(this.font, "Blocks: " + previewPlan.getPlannedBlocks().size(), x, y + 16, TEXT_BRIGHT);
            guiGraphics.drawString(this.font, "State: " + severity + (previewPlan.isStale() ? " / STALE" : ""), x, y + 28, color);
        }

        guiGraphics.drawString(this.font, "Issues", x, y + 58, TEXT_BRIGHT);
        List<String> issues = collectIssueLines(previewPlan, selectedPack);
        int issueY = y + 74;
        if (issues.isEmpty()) {
            guiGraphics.drawString(this.font, "No warnings.", x, issueY, TEXT_MUTED);
            return;
        }
        for (int i = 0; i < Math.min(12, issues.size()); i++) {
            guiGraphics.drawString(this.font, trimToWidth(issues.get(i), width), x, issueY, issueColor(issues.get(i)));
            issueY += 12;
            if (issueY > panelBottom - 16) {
                break;
            }
        }
    }

    private List<String> collectIssueLines(PreviewPlan previewPlan, LoadedPack selectedPack) {
        List<String> lines = new ArrayList<>();
        if (previewPlan != null) {
            for (PreviewIssue issue : previewPlan.getIssues()) {
                lines.add(issue.getSeverity() + ": " + issue.getMessage());
            }
        }
        if (selectedPack != null) {
            for (ValidationIssue issue : selectedPack.getValidationReport().getIssues()) {
                lines.add(issue.getSeverity() + ": " + issue.getMessage());
            }
        }
        return lines;
    }

    private int issueColor(String issue) {
        if (issue.startsWith("ERROR")) {
            return STATUS_ERROR;
        }
        if (issue.startsWith("WARNING")) {
            return STATUS_WARN;
        }
        if (issue.startsWith("INFO")) {
            return STATUS_INFO;
        }
        return TEXT_MUTED;
    }

    private String trimToWidth(String text, int width) {
        return this.font.plainSubstrByWidth(text, Math.max(40, width - 8));
    }

    private void updateActionStates() {
        if (previewButton == null) {
            return;
        }
        PresetDefinition preset = getSelectedPreset();
        PreviewPlan previewPlan = getVisiblePreviewPlan();
        previewButton.active = preset != null;
        previewButton.setMessage(Component.literal(previewPlan == null ? "Preview" : "Regenerate"));
        bakeButton.active = previewPlan != null && previewPlan.canBake();
        clearPreviewButton.active = previewPlan != null;
        undoButton.active = true;
    }

    private void cyclePreset(int delta) {
        List<PresetDefinition> presets = Blockwright.getPackManager().getAllPresets();
        if (presets.isEmpty()) {
            return;
        }
        selectedPresetIndex = Math.floorMod(selectedPresetIndex + delta, presets.size());
        ClientPreviewState.markStale();
        Minecraft.getInstance().setScreen(new BlockwrightMainScreen());
    }

    private void reloadPacks() {
        Blockwright.getPackManager().reload();
        ClientPreviewState.markStale();
        Minecraft.getInstance().setScreen(new BlockwrightMainScreen());
        sendCommand("blockwright reload");
    }

    private LoadedPack getSelectedPack() {
        PresetDefinition preset = getSelectedPreset();
        if (preset == null) {
            return null;
        }
        return Blockwright.getPackManager().findPreset(preset.id)
                .map(top.huliawsl.blockwright.pack.BlockwrightPackManager.PresetLookup::pack)
                .orElse(null);
    }

    private PresetDefinition getSelectedPreset() {
        List<PresetDefinition> presets = Blockwright.getPackManager().getAllPresets();
        if (presets.isEmpty()) {
            return null;
        }
        if (selectedPresetIndex >= presets.size()) {
            selectedPresetIndex = 0;
        }
        return presets.get(selectedPresetIndex);
    }

    private PreviewPlan getVisiblePreviewPlan() {
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        PresetDefinition preset = getSelectedPreset();
        if (previewPlan != null && preset != null && !previewPlan.getPresetId().equals(preset.id)) {
            previewPlan.setStale(true);
        }
        return previewPlan;
    }

    private void sendPreviewCommand() {
        PresetDefinition preset = getSelectedPreset();
        if (preset == null) {
            return;
        }

        Map<String, String> overrideMap = new LinkedHashMap<>();
        List<String> overrideParts = new ArrayList<>();
        for (int i = 0; i < parameterKeys.size(); i++) {
            EditBox box = parameterBoxes.get(i);
            String key = parameterKeys.get(i);
            String value = box.getValue();
            overrideMap.put(key, value);
            overrideParts.add(key + "=" + value);
        }
        ClientPreviewState.setPreviewPlan(ClientPreviewGenerator.generate(preset.id, overrideMap));
        String command = "blockwright preview generate " + preset.id;
        if (!overrideParts.isEmpty()) {
            command += " " + String.join(",", overrideParts);
        }
        sendCommand(command);
    }

    private void sendCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }
        ClientSelectionState.captureCommand(command, minecraft.player.blockPosition());
        minecraft.player.connection.sendCommand(command);
    }

    private String describePresetMode(PresetDefinition preset) {
        if (preset == null || preset.inputs.isEmpty()) {
            return "None";
        }
        for (PresetInputDefinition input : preset.inputs) {
            if ("box_region".equals(input.type)) {
                return "Region";
            }
            if ("spline".equals(input.type)) {
                return "Spline";
            }
        }
        return preset.inputs.get(0).type;
    }

    private static String formatPos(BlockPos pos) {
        return pos == null ? "<unset>" : pos.toShortString();
    }
}
