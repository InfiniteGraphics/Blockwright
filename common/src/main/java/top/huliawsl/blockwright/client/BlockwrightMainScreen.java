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

    private static final int OUTER_PADDING = 12;
    private static final int PANEL_GAP = 10;
    private static final int PANEL_INSET = 12;
    private static final int HEADER_HEIGHT = 32;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_HEIGHT = 18;
    private static final int ROW_GAP = 4;
    private static final int SECTION_GAP = 14;
    private static final int PARAMETER_ROW_HEIGHT = 28;
    private static final int ISSUE_ROW_HEIGHT = 12;

    private static int selectedPackIndex;
    private static String selectedPresetId;

    private final List<EditBox> parameterBoxes = new ArrayList<>();
    private final List<String> parameterKeys = new ArrayList<>();

    private int contentLeft;
    private int topY;
    private int panelBottom;
    private int leftWidth;
    private int middleWidth;
    private int rightWidth;
    private int middleX;
    private int rightX;
    private int leftInnerX;
    private int middleInnerX;
    private int rightInnerX;
    private int regionSectionTop;
    private int regionButtonsY;
    private int regionInfoY;
    private int splineSectionTop;
    private int splineButtonsY;
    private int inputHintY;
    private int maxSplinePreviewLines;
    private int presetInfoTop;
    private int parameterHeaderY;
    private int parameterLabelWidth;
    private int parameterFieldX;
    private int parameterFieldWidth;
    private int statusPreviewTop;
    private int actionButtonsY;
    private int issuesHeaderY;
    private int parameterViewportTop;
    private int parameterViewportBottom;
    private int parameterScroll;
    private int maxParameterScroll;
    private int issueViewportTop;
    private int issueViewportBottom;
    private int issueScroll;
    private int maxIssueScroll;

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

        int buttonRowY = topY + PANEL_INSET + 14;
        int packArrowY = presetInfoTop - 4;
        int presetArrowY = presetInfoTop + 8;
        int actionRowWidth = (rightWidth - PANEL_INSET * 2 - PANEL_GAP * 2) / 3;

        addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePack(-1))
                .bounds(middleInnerX, packArrowY, 20, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePack(1))
                .bounds(middleX + middleWidth - PANEL_INSET - 20, packArrowY, 20, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePreset(-1))
                .bounds(middleInnerX, presetArrowY, 20, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePreset(1))
                .bounds(middleX + middleWidth - PANEL_INSET - 20, presetArrowY, 20, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadPacks())
                .bounds(rightInnerX, buttonRowY, actionRowWidth, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Library"), button -> openModuleLibrary())
                .bounds(rightInnerX + actionRowWidth + PANEL_GAP, buttonRowY, actionRowWidth, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(rightInnerX + (actionRowWidth + PANEL_GAP) * 2, buttonRowY, actionRowWidth, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Set P1"), button -> sendCommand("blockwright region pos1"))
                .bounds(leftInnerX, regionButtonsY, (leftWidth - PANEL_INSET * 2 - PANEL_GAP) / 2, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Set P2"), button -> sendCommand("blockwright region pos2"))
                .bounds(leftInnerX + (leftWidth - PANEL_INSET * 2 - PANEL_GAP) / 2 + PANEL_GAP, regionButtonsY,
                        (leftWidth - PANEL_INSET * 2 - PANEL_GAP) / 2, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), button -> sendCommand("blockwright region clear"))
                .bounds(leftInnerX, regionButtonsY + BUTTON_HEIGHT + ROW_GAP, leftWidth - PANEL_INSET * 2, BUTTON_HEIGHT)
                .build());

        int splineButtonWidth = (leftWidth - PANEL_INSET * 2 - PANEL_GAP * 2) / 3;
        addRenderableWidget(Button.builder(Component.literal("Add"), button -> sendCommand("blockwright spline add"))
                .bounds(leftInnerX, splineButtonsY, splineButtonWidth, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("List"), button -> sendCommand("blockwright spline list"))
                .bounds(leftInnerX + splineButtonWidth + PANEL_GAP, splineButtonsY, splineButtonWidth, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), button -> sendCommand("blockwright spline clear"))
                .bounds(leftInnerX + (splineButtonWidth + PANEL_GAP) * 2, splineButtonsY, splineButtonWidth, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Drop Last"), button -> removeLastSplinePoint())
                .bounds(leftInnerX, splineButtonsY + BUTTON_HEIGHT + ROW_GAP, leftWidth - PANEL_INSET * 2, BUTTON_HEIGHT)
                .build());

        PresetDefinition preset = getSelectedPreset();
        if (preset != null) {
            for (Map.Entry<String, PresetParameterDefinition> entry : preset.parameters.entrySet()) {
                if (!entry.getValue().exposed) {
                    continue;
                }
                parameterKeys.add(entry.getKey());
                EditBox box = new EditBox(this.font, 0, 0, 10, FIELD_HEIGHT, Component.literal(entry.getKey()));
                box.setValue(entry.getValue().defaultValue == null ? "" : entry.getValue().defaultValue.getAsString());
                box.setBordered(true);
                box.setEditable(true);
                box.setTextColor(TEXT_BRIGHT);
                box.setTextColorUneditable(TEXT_MUTED);
                box.setResponder(value -> ClientPreviewState.markStale());
                addRenderableWidget(box);
                parameterBoxes.add(box);
            }
        }

        int actionButtonWidth = (rightWidth - PANEL_INSET * 2 - PANEL_GAP) / 2;
        previewButton = addRenderableWidget(Button.builder(Component.literal("Preview"), button -> sendPreviewCommand())
                .bounds(rightInnerX, actionButtonsY, actionButtonWidth, BUTTON_HEIGHT)
                .build());
        bakeButton = addRenderableWidget(Button.builder(Component.literal("Bake"), button -> sendCommand("blockwright bake"))
                .bounds(rightInnerX + actionButtonWidth + PANEL_GAP, actionButtonsY, actionButtonWidth, BUTTON_HEIGHT)
                .build());
        undoButton = addRenderableWidget(Button.builder(Component.literal("Undo"), button -> sendCommand("blockwright undo"))
                .bounds(rightInnerX, actionButtonsY + BUTTON_HEIGHT + ROW_GAP, actionButtonWidth, BUTTON_HEIGHT)
                .build());
        clearPreviewButton = addRenderableWidget(Button.builder(Component.literal("Clear"), button -> sendCommand("blockwright preview clear"))
                .bounds(rightInnerX + actionButtonWidth + PANEL_GAP, actionButtonsY + BUTTON_HEIGHT + ROW_GAP,
                        actionButtonWidth, BUTTON_HEIGHT)
                .build());

        layoutParameterFields();
        updateActionStates();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        drawAtmosphere(guiGraphics);

        drawPanel(guiGraphics, contentLeft, topY, leftWidth, panelBottom - topY);
        drawPanel(guiGraphics, middleX, topY, middleWidth, panelBottom - topY);
        drawPanel(guiGraphics, rightX, topY, rightWidth, panelBottom - topY);

        LoadedPack selectedPack = getSelectedPack();
        PresetDefinition preset = getSelectedPreset();
        PreviewPlan previewPlan = getVisiblePreviewPlan();
        BoxRegionSelection regionSelection = ClientSelectionState.getRegionSelection();
        SplineSelection splineSelection = ClientSelectionState.getSplineSelection();
        List<String> issues = collectIssueLines(previewPlan, selectedPack);
        updateIssueScrollBounds(issues.size());

        guiGraphics.drawString(this.font, this.title, contentLeft + PANEL_INSET, topY - 12, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Input", contentLeft + PANEL_INSET, topY + 8, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Preset", middleX + PANEL_INSET, topY + 8, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Status", rightX + PANEL_INSET, topY + 8, TEXT_BRIGHT);

        guiGraphics.drawString(this.font,
                trimToWidth(selectedPack == null ? "Pack: <none>" : "Pack: " + selectedPack.getMetadata().id, middleWidth - PANEL_INSET * 2 - 52),
                middleInnerX + 26, presetInfoTop, TEXT_MUTED);
        guiGraphics.drawString(this.font,
                trimToWidth(preset == null ? "Preset: <none>" : "Preset: " + preset.id, middleWidth - PANEL_INSET * 2 - 52),
                middleInnerX + 26, presetInfoTop + 12, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Mode: " + describePresetMode(preset), middleInnerX, presetInfoTop + 28, TEXT_MUTED);

        drawRegionSection(guiGraphics, leftInnerX, regionSectionTop, regionSelection);
        drawSplineSection(guiGraphics, leftInnerX, splineSectionTop, splineSelection);
        drawInputHints(guiGraphics, leftInnerX, inputHintY);
        drawParameterLabels(guiGraphics, middleInnerX, preset);
        drawStatusPanel(guiGraphics, rightInnerX, previewPlan, issues);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        updateActionStates();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= middleX && mouseX <= middleX + middleWidth
                && mouseY >= parameterViewportTop - 8 && mouseY <= parameterViewportBottom + 8) {
            int nextScroll = parameterScroll - (int) (delta * 16.0D);
            parameterScroll = clamp(nextScroll, 0, maxParameterScroll);
            layoutParameterFields();
            return true;
        }
        if (mouseX >= rightX && mouseX <= rightX + rightWidth
                && mouseY >= issueViewportTop - 8 && mouseY <= issueViewportBottom + 8) {
            int nextScroll = issueScroll - (int) (delta * ISSUE_ROW_HEIGHT);
            issueScroll = clamp(nextScroll, 0, maxIssueScroll);
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
        int layoutWidth = Math.min(this.width - OUTER_PADDING * 2, 1280);
        contentLeft = (this.width - layoutWidth) / 2;
        topY = 18;
        panelBottom = this.height - 18;

        int availableWidth = layoutWidth - PANEL_GAP * 2;
        leftWidth = clamp(availableWidth * 23 / 100, 164, 240);
        rightWidth = clamp(availableWidth * 21 / 100, 176, 236);
        middleWidth = availableWidth - leftWidth - rightWidth;
        if (middleWidth < 260) {
            int shortage = 260 - middleWidth;
            int trimLeft = Math.min(shortage / 2 + shortage % 2, leftWidth - 152);
            int trimRight = Math.min(shortage / 2, rightWidth - 164);
            leftWidth -= Math.max(0, trimLeft);
            rightWidth -= Math.max(0, trimRight);
            middleWidth = availableWidth - leftWidth - rightWidth;
        }

        middleX = contentLeft + leftWidth + PANEL_GAP;
        rightX = middleX + middleWidth + PANEL_GAP;
        leftInnerX = contentLeft + PANEL_INSET;
        middleInnerX = middleX + PANEL_INSET;
        rightInnerX = rightX + PANEL_INSET;

        int contentTop = topY + HEADER_HEIGHT + 18;
        int contentBottom = panelBottom - PANEL_INSET;

        regionSectionTop = contentTop;
        regionButtonsY = regionSectionTop + 44;
        regionInfoY = regionButtonsY + BUTTON_HEIGHT * 2 + ROW_GAP + 8;
        splineSectionTop = regionInfoY + 36;
        inputHintY = contentBottom - 24;
        splineButtonsY = Math.max(splineSectionTop + 68, inputHintY - BUTTON_HEIGHT * 2 - ROW_GAP - 10);
        maxSplinePreviewLines = Math.max(1, (splineButtonsY - (splineSectionTop + 40)) / 12);

        presetInfoTop = topY + PANEL_INSET + 20;
        parameterHeaderY = presetInfoTop + 42;
        parameterLabelWidth = clamp(middleWidth / 3, 74, 112);
        parameterFieldX = middleInnerX + parameterLabelWidth + 10;
        parameterFieldWidth = Math.max(96, middleWidth - PANEL_INSET * 2 - parameterLabelWidth - 10);
        parameterViewportTop = parameterHeaderY + 22;
        parameterViewportBottom = contentBottom;

        statusPreviewTop = contentTop;
        actionButtonsY = statusPreviewTop + 46;
        issuesHeaderY = actionButtonsY + BUTTON_HEIGHT * 2 + ROW_GAP + SECTION_GAP + 4;
        issueViewportTop = issuesHeaderY + 16;
        issueViewportBottom = contentBottom;
    }

    private void layoutParameterFields() {
        int viewportHeight = Math.max(40, parameterViewportBottom - parameterViewportTop);
        int contentHeight = Math.max(0, parameterKeys.size() * PARAMETER_ROW_HEIGHT);
        maxParameterScroll = Math.max(0, contentHeight - viewportHeight);
        parameterScroll = clamp(parameterScroll, 0, maxParameterScroll);

        int baseY = parameterViewportTop - parameterScroll;
        for (int i = 0; i < parameterBoxes.size(); i++) {
            EditBox box = parameterBoxes.get(i);
            int y = baseY + i * PARAMETER_ROW_HEIGHT + 2;
            boolean visible = y + FIELD_HEIGHT >= parameterViewportTop && y <= parameterViewportBottom;
            box.setX(parameterFieldX);
            box.setY(y);
            box.setWidth(parameterFieldWidth);
            box.setVisible(visible);
            box.active = visible;
            if (!visible) {
                box.setFocused(false);
            }
        }
    }

    private void updateIssueScrollBounds(int issueCount) {
        int viewportHeight = Math.max(0, issueViewportBottom - issueViewportTop);
        int contentHeight = issueCount * ISSUE_ROW_HEIGHT;
        maxIssueScroll = Math.max(0, contentHeight - viewportHeight);
        issueScroll = clamp(issueScroll, 0, maxIssueScroll);
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
            guiGraphics.drawString(this.font,
                    trimToWidth("Size: " + regionSelection.getWidth() + " x " + regionSelection.getHeight() + " x " + regionSelection.getDepth(),
                            leftWidth - PANEL_INSET * 2),
                    x, regionInfoY, STATUS_OK);
            guiGraphics.drawString(this.font, "Volume: " + regionSelection.getVolume(), x, regionInfoY + 12, TEXT_MUTED);
        } else {
            guiGraphics.drawString(this.font, "Set both corners before preview.", x, regionInfoY, TEXT_MUTED);
        }
    }

    private void drawSplineSection(GuiGraphics guiGraphics, int x, int y, SplineSelection splineSelection) {
        guiGraphics.drawString(this.font, "Spline", x, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Points: " + splineSelection.getPoints().size(), x, y + 16, TEXT_MUTED);
        int lineY = y + 32;
        if (splineSelection.getPoints().isEmpty()) {
            guiGraphics.drawString(this.font, "No control points.", x, lineY + 12, TEXT_MUTED);
            return;
        }

        int visibleLines = Math.max(1, Math.min(maxSplinePreviewLines, splineSelection.getPoints().size()));
        for (int i = 0; i < visibleLines; i++) {
            guiGraphics.drawString(this.font,
                    trimToWidth("#" + i + " " + splineSelection.getPoints().get(i).toShortString(), leftWidth - PANEL_INSET * 2),
                    x, lineY, TEXT_BRIGHT);
            lineY += 12;
        }
        if (splineSelection.getPoints().size() > visibleLines) {
            guiGraphics.drawString(this.font, "...", x, lineY, TEXT_MUTED);
        }
    }

    private void drawInputHints(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.font, "1. Select input, then pick a preset.", x, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, "2. Preview first, bake only after review.", x, y + 12, TEXT_MUTED);
    }

    private void drawParameterLabels(GuiGraphics guiGraphics, int x, PresetDefinition preset) {
        guiGraphics.drawString(this.font, "Parameters", x, parameterHeaderY, TEXT_BRIGHT);
        if (preset == null) {
            guiGraphics.drawString(this.font, "No preset loaded.", x, parameterViewportTop, TEXT_MUTED);
            return;
        }
        if (maxParameterScroll > 0) {
            guiGraphics.drawString(this.font, "Scroll", middleX + middleWidth - PANEL_INSET - 34, parameterHeaderY, TEXT_MUTED);
        }

        int baseY = parameterViewportTop - parameterScroll;
        for (int i = 0; i < parameterKeys.size(); i++) {
            int labelY = baseY + i * PARAMETER_ROW_HEIGHT + 7;
            if (labelY < parameterViewportTop - ISSUE_ROW_HEIGHT || labelY > parameterViewportBottom - ISSUE_ROW_HEIGHT) {
                continue;
            }
            guiGraphics.drawString(this.font, trimToWidth(parameterKeys.get(i), parameterLabelWidth), x, labelY, TEXT_MUTED);
        }
    }

    private void drawStatusPanel(GuiGraphics guiGraphics, int x, PreviewPlan previewPlan, List<String> issues) {
        int statusY = statusPreviewTop;
        guiGraphics.drawString(this.font, "Preview", x, statusY, TEXT_BRIGHT);
        if (previewPlan == null) {
            guiGraphics.drawString(this.font, "No preview generated.", x, statusY + 16, TEXT_MUTED);
        } else {
            PreviewSeverity severity = previewPlan.getOverallSeverity();
            int color = switch (severity) {
                case ERROR -> STATUS_ERROR;
                case WARNING -> STATUS_WARN;
                default -> STATUS_OK;
            };
            guiGraphics.drawString(this.font, "Blocks: " + previewPlan.getPlannedBlocks().size(), x, statusY + 16, TEXT_BRIGHT);
            guiGraphics.drawString(this.font, trimToWidth("State: " + severity + (previewPlan.isStale() ? " / STALE" : ""), rightWidth - PANEL_INSET * 2),
                    x, statusY + 28, color);
        }

        guiGraphics.drawString(this.font, "Issues", x, issuesHeaderY, TEXT_BRIGHT);
        if (maxIssueScroll > 0) {
            guiGraphics.drawString(this.font, "Scroll", rightX + rightWidth - PANEL_INSET - 34, issuesHeaderY, TEXT_MUTED);
        }
        if (issues.isEmpty()) {
            guiGraphics.drawString(this.font, "No warnings.", x, issueViewportTop, TEXT_MUTED);
            return;
        }

        int baseY = issueViewportTop - issueScroll;
        for (int i = 0; i < issues.size(); i++) {
            int y = baseY + i * ISSUE_ROW_HEIGHT;
            if (y < issueViewportTop - ISSUE_ROW_HEIGHT || y > issueViewportBottom - 2) {
                continue;
            }
            guiGraphics.drawString(this.font, trimToWidth(issues.get(i), rightWidth - PANEL_INSET * 2), x, y, issueColor(issues.get(i)));
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
        return this.font.plainSubstrByWidth(text, Math.max(20, width));
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
        LoadedPack pack = getSelectedPack();
        List<PresetDefinition> presets = getPackPresets(pack);
        if (presets.isEmpty()) {
            return;
        }
        int currentIndex = selectedPresetId == null ? 0 : findPresetIndex(presets, selectedPresetId);
        int nextIndex = Math.floorMod(currentIndex + delta, presets.size());
        selectedPresetId = presets.get(nextIndex).id;
        ClientPreviewState.markStale();
        Minecraft.getInstance().setScreen(new BlockwrightMainScreen());
    }

    private void cyclePack(int delta) {
        List<LoadedPack> packs = Blockwright.getPackManager().getLoadedPacks();
        if (packs.isEmpty()) {
            return;
        }
        selectedPackIndex = Math.floorMod(selectedPackIndex + delta, packs.size());
        selectedPresetId = null;
        ClientPreviewState.markStale();
        Minecraft.getInstance().setScreen(new BlockwrightMainScreen());
    }

    private void reloadPacks() {
        Blockwright.getPackManager().reload();
        ClientPreviewState.markStale();
        Minecraft.getInstance().setScreen(new BlockwrightMainScreen());
        sendCommand("blockwright reload");
    }

    private void openModuleLibrary() {
        LoadedPack selectedPack = getSelectedPack();
        Minecraft.getInstance().setScreen(new ModuleLibraryScreen(this, selectedPack == null ? null : selectedPack.getMetadata().id));
    }

    private LoadedPack getSelectedPack() {
        List<LoadedPack> packs = Blockwright.getPackManager().getLoadedPacks();
        if (packs.isEmpty()) {
            return null;
        }
        if (selectedPackIndex >= packs.size()) {
            selectedPackIndex = 0;
        }
        return packs.get(selectedPackIndex);
    }

    private PresetDefinition getSelectedPreset() {
        List<PresetDefinition> presets = getPackPresets(getSelectedPack());
        if (presets.isEmpty()) {
            selectedPresetId = null;
            return null;
        }
        if (selectedPresetId != null) {
            for (PresetDefinition preset : presets) {
                if (selectedPresetId.equals(preset.id)) {
                    return preset;
                }
            }
        }
        selectedPresetId = presets.get(0).id;
        return presets.get(0);
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<PresetDefinition> getPackPresets(LoadedPack pack) {
        if (pack == null) {
            return List.of();
        }
        return new ArrayList<>(pack.getPresets().values());
    }

    private int findPresetIndex(List<PresetDefinition> presets, String presetId) {
        for (int i = 0; i < presets.size(); i++) {
            if (presetId.equals(presets.get(i).id)) {
                return i;
            }
        }
        return 0;
    }

    private void removeLastSplinePoint() {
        int lastIndex = ClientSelectionState.getSplineSelection().getPoints().size() - 1;
        if (lastIndex < 0) {
            return;
        }
        sendCommand("blockwright spline remove " + lastIndex);
    }

    private static String formatPos(BlockPos pos) {
        return pos == null ? "<unset>" : pos.toShortString();
    }
}
