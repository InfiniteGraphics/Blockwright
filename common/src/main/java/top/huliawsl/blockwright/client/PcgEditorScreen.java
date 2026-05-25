package top.huliawsl.blockwright.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.config.BlockwrightConfig;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.pack.SpongeSchematicData;
import top.huliawsl.blockwright.preview.PreviewIssue;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetInputDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;
import top.huliawsl.blockwright.util.ValidationIssue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PcgEditorScreen extends Screen {
    private static final int PANEL_BG = 0xC8141A20;
    private static final int PANEL_BORDER = 0xFF2D3946;
    private static final int PANEL_HEADER = 0xE0222A33;
    private static final int TEXT_BRIGHT = 0xFFF3F7FB;
    private static final int TEXT_MUTED = 0xFF9BA5B1;
    private static final int TEXT_GREEN = 0xFF73D585;
    private static final int TEXT_YELLOW = 0xFFF2C94C;
    private static final int TEXT_RED = 0xFFF26B6B;
    private static final int TEXT_BLUE = 0xFF5DA9E9;
    private static final int TEXT_MAGENTA = 0xFFD56BF3;
    private static final int TOOL_ACTIVE = 0xCC1E4E7A;
    private static final int TOOL_DISABLED = 0x88454A53;

    private static final int OUTER_PAD = 12;
    private static final int TOP_BAR_HEIGHT = 44;
    private static final int LEFT_BAR_WIDTH = 92;
    private static final int RIGHT_PANEL_WIDTH = 352;
    private static final int BOTTOM_BAR_HEIGHT = 96;
    private static final int GAP = 10;
    private static final int INSET = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_HEIGHT = 18;
    private static final int TOOL_BUTTON_HEIGHT = 78;
    private static final int PARAMETER_ROW_HEIGHT = 28;
    private static final int MODULE_ROW_HEIGHT = 24;
    private static final int LOG_ROW_HEIGHT = 14;

    private final PcgEditorSession session = PcgEditorSession.get();
    private final List<EditBox> parameterBoxes = new ArrayList<>();
    private final List<String> parameterKeys = new ArrayList<>();

    private Button previewButton;
    private Button regenerateButton;
    private Button bakeButton;
    private Button undoButton;
    private Button redoButton;
    private Button reloadButton;
    private Button exitButton;
    private EditBox transformXBox;
    private EditBox transformYBox;
    private EditBox transformZBox;
    private Button transformApplyButton;
    private EditBox moduleSearchBox;
    private EditBox moduleTagBox;
    private EditBox moduleExportIdBox;
    private Button moduleKindButton;
    private Button moduleRotateButton;
    private Button moduleZoomInButton;
    private Button moduleZoomOutButton;
    private Button moduleBoundsButton;
    private Button moduleConnectorsButton;
    private Button moduleAirButton;
    private Button moduleExportButton;

    private int topBarX;
    private int topBarY;
    private int leftBarX;
    private int leftBarY;
    private int rightPanelX;
    private int rightPanelY;
    private int bottomBarX;
    private int bottomBarY;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int rightInnerX;
    private int inspectorContentTop;
    private int inspectorContentBottom;
    private int parameterViewportTop;
    private int parameterViewportBottom;
    private int parameterScroll;
    private int maxParameterScroll;
    private int moduleListTop;
    private int moduleListBottom;
    private int moduleListScroll;
    private int maxModuleListScroll;
    private int logScroll;
    private int maxLogScroll;

    public PcgEditorScreen() {
        super(Component.literal("PCG Editor"));
    }

    @Override
    protected void init() {
        clearWidgets();
        parameterBoxes.clear();
        parameterKeys.clear();
        session.open();
        session.ensureDefaults();
        session.syncSelectionDefaults();
        initLayout();
        initTopToolbar();
        initLeftToolbar();
        rebuildInspectorWidgets();
        updateActionStates();
    }

    private void initLayout() {
        topBarX = OUTER_PAD;
        topBarY = OUTER_PAD;
        leftBarX = OUTER_PAD;
        leftBarY = topBarY + TOP_BAR_HEIGHT + GAP;
        rightPanelX = this.width - RIGHT_PANEL_WIDTH - OUTER_PAD;
        rightPanelY = topBarY + TOP_BAR_HEIGHT + GAP;
        bottomBarX = OUTER_PAD;
        bottomBarY = this.height - BOTTOM_BAR_HEIGHT - OUTER_PAD;
        viewportX = leftBarX + LEFT_BAR_WIDTH + GAP;
        viewportY = topBarY + TOP_BAR_HEIGHT + GAP;
        viewportWidth = rightPanelX - GAP - viewportX;
        viewportHeight = bottomBarY - GAP - viewportY;
        rightInnerX = rightPanelX + INSET;
        inspectorContentTop = rightPanelY + 72;
        inspectorContentBottom = bottomBarY - GAP;
        parameterViewportTop = 0;
        parameterViewportBottom = 0;
        moduleListTop = 0;
        moduleListBottom = 0;
    }

    private void initTopToolbar() {
        int x = topBarX + INSET;
        int y = topBarY + 12;
        int smallWidth = 86;
        int mediumWidth = 152;
        int actionWidth = 92;

        x += 246;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePack(-1))
                .bounds(x, y, 18, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePack(1))
                .bounds(x + mediumWidth - 18, y, 18, BUTTON_HEIGHT).build());

        x += mediumWidth + GAP;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePreset(-1))
                .bounds(x, y, 18, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePreset(1))
                .bounds(x + mediumWidth - 18, y, 18, BUTTON_HEIGHT).build());

        x += mediumWidth + GAP + 6;
        previewButton = addRenderableWidget(Button.builder(Component.literal("Preview"), button -> generatePreview(false))
                .bounds(x, y, actionWidth, BUTTON_HEIGHT).build());
        regenerateButton = addRenderableWidget(Button.builder(Component.literal("Regenerate"), button -> generatePreview(true))
                .bounds(x + actionWidth + GAP, y, actionWidth + 12, BUTTON_HEIGHT).build());
        bakeButton = addRenderableWidget(Button.builder(Component.literal("Bake"), button -> requestBake())
                .bounds(x + (actionWidth + GAP) * 2 + 12, y, smallWidth - 2, BUTTON_HEIGHT).build());
        undoButton = addRenderableWidget(Button.builder(Component.literal("Undo"), button -> runAction("blockwright undo", PcgEditorLogEntry.Severity.INFO, "Undo requested."))
                .bounds(x + (actionWidth + GAP) * 2 + smallWidth + GAP + 10, y, smallWidth - 4, BUTTON_HEIGHT).build());
        redoButton = addRenderableWidget(Button.builder(Component.literal("Redo"), button -> session.log(PcgEditorLogEntry.Severity.DEBUG, "Redo is not implemented in this MVP."))
                .bounds(x + (actionWidth + GAP) * 2 + (smallWidth + GAP) * 2 + 6, y, smallWidth - 4, BUTTON_HEIGHT).build());
        reloadButton = addRenderableWidget(Button.builder(Component.literal("Reload"), button -> reloadPacks())
                .bounds(x + (actionWidth + GAP) * 2 + (smallWidth + GAP) * 3 + 2, y, smallWidth, BUTTON_HEIGHT).build());
        exitButton = addRenderableWidget(Button.builder(Component.literal("Exit"), button -> onClose())
                .bounds(x + (actionWidth + GAP) * 2 + (smallWidth + GAP) * 4 + 2, y, smallWidth - 10, BUTTON_HEIGHT).build());
    }

    private void initLeftToolbar() {
        int x = leftBarX + 8;
        int y = leftBarY + 8;
        for (PcgEditorTool tool : PcgEditorTool.values()) {
            boolean disabled = tool == PcgEditorTool.PAINT_MASK;
            Button button = addRenderableWidget(Button.builder(Component.literal(tool.getTitle()), ignored -> switchTool(tool))
                    .bounds(x, y, LEFT_BAR_WIDTH - 16, TOOL_BUTTON_HEIGHT)
                    .build());
            button.active = !disabled;
            y += TOOL_BUTTON_HEIGHT + 8;
        }
    }

    private void rebuildInspectorWidgets() {
        clearDynamicWidgets();
        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY) {
            initModuleLibraryWidgets();
            return;
        }

        PresetDefinition preset = session.getSelectedPreset();
        initTransformWidgets(computeInspectorContentTop());
        if (preset == null) {
            return;
        }
        int contentY = computeInspectorContentTop();
        parameterViewportTop = contentY + 144;
        parameterViewportBottom = inspectorContentBottom - 188;
        int labelWidth = 102;
        int fieldX = rightInnerX + labelWidth;
        int fieldWidth = RIGHT_PANEL_WIDTH - INSET * 2 - labelWidth - 8;
        for (Map.Entry<String, PresetParameterDefinition> entry : preset.parameters.entrySet()) {
            if (!entry.getValue().exposed) {
                continue;
            }
            parameterKeys.add(entry.getKey());
            EditBox box = new EditBox(this.font, fieldX, parameterViewportTop, fieldWidth, FIELD_HEIGHT, Component.literal(entry.getKey()));
            box.setValue(entry.getValue().defaultValue == null ? "" : entry.getValue().defaultValue.getAsString());
            box.setBordered(true);
            box.setTextColor(TEXT_BRIGHT);
            box.setResponder(value -> {
                session.markDirty("Parameter changed: " + entry.getKey());
                updateActionStates();
            });
            addRenderableWidget(box);
            parameterBoxes.add(box);
        }
        layoutParameterFields();
    }

    private void clearDynamicWidgets() {
        if (transformXBox != null) {
            removeWidget(transformXBox);
            transformXBox = null;
        }
        if (transformYBox != null) {
            removeWidget(transformYBox);
            transformYBox = null;
        }
        if (transformZBox != null) {
            removeWidget(transformZBox);
            transformZBox = null;
        }
        if (transformApplyButton != null) {
            removeWidget(transformApplyButton);
            transformApplyButton = null;
        }
        if (moduleSearchBox != null) {
            removeWidget(moduleSearchBox);
            moduleSearchBox = null;
        }
        if (moduleTagBox != null) {
            removeWidget(moduleTagBox);
            moduleTagBox = null;
        }
        if (moduleExportIdBox != null) {
            removeWidget(moduleExportIdBox);
            moduleExportIdBox = null;
        }
        if (moduleKindButton != null) {
            removeWidget(moduleKindButton);
            moduleKindButton = null;
        }
        if (moduleRotateButton != null) {
            removeWidget(moduleRotateButton);
            moduleRotateButton = null;
        }
        if (moduleZoomInButton != null) {
            removeWidget(moduleZoomInButton);
            moduleZoomInButton = null;
        }
        if (moduleZoomOutButton != null) {
            removeWidget(moduleZoomOutButton);
            moduleZoomOutButton = null;
        }
        if (moduleBoundsButton != null) {
            removeWidget(moduleBoundsButton);
            moduleBoundsButton = null;
        }
        if (moduleConnectorsButton != null) {
            removeWidget(moduleConnectorsButton);
            moduleConnectorsButton = null;
        }
        if (moduleAirButton != null) {
            removeWidget(moduleAirButton);
            moduleAirButton = null;
        }
        if (moduleExportButton != null) {
            removeWidget(moduleExportButton);
            moduleExportButton = null;
        }
        for (EditBox box : parameterBoxes) {
            removeWidget(box);
        }
        parameterBoxes.clear();
        parameterKeys.clear();
    }

    private void initModuleLibraryWidgets() {
        int contentY = computeInspectorContentTop();
        moduleSearchBox = new EditBox(this.font, rightInnerX, contentY + 18, RIGHT_PANEL_WIDTH - INSET * 2, FIELD_HEIGHT, Component.literal("Search"));
        moduleSearchBox.setValue(session.getModuleSearchQuery());
        moduleSearchBox.setBordered(true);
        moduleSearchBox.setTextColor(TEXT_BRIGHT);
        moduleSearchBox.setResponder(value -> session.setModuleSearchQuery(value));
        addRenderableWidget(moduleSearchBox);

        moduleTagBox = new EditBox(this.font, rightInnerX, contentY + 44, RIGHT_PANEL_WIDTH - INSET * 2, FIELD_HEIGHT, Component.literal("Tag filter"));
        moduleTagBox.setValue(session.getModuleTagQuery());
        moduleTagBox.setBordered(true);
        moduleTagBox.setTextColor(TEXT_BRIGHT);
        moduleTagBox.setResponder(value -> session.setModuleTagQuery(value));
        addRenderableWidget(moduleTagBox);

        moduleKindButton = addRenderableWidget(Button.builder(Component.literal(moduleKindLabel()), button -> {
                    session.cycleModuleKindFilter();
                    button.setMessage(Component.literal(moduleKindLabel()));
                })
                .bounds(rightInnerX, contentY + 68, RIGHT_PANEL_WIDTH - INSET * 2, BUTTON_HEIGHT)
                .build());

        moduleExportIdBox = new EditBox(this.font, rightInnerX, contentY + 95, RIGHT_PANEL_WIDTH - INSET * 2, FIELD_HEIGHT, Component.literal("Export module id"));
        moduleExportIdBox.setValue(session.getExportModuleId());
        moduleExportIdBox.setBordered(true);
        moduleExportIdBox.setTextColor(TEXT_BRIGHT);
        moduleExportIdBox.setResponder(session::setExportModuleId);
        addRenderableWidget(moduleExportIdBox);

        moduleExportButton = addRenderableWidget(Button.builder(Component.literal("Export Region"), button -> exportRegionAsModule())
                .bounds(rightInnerX, contentY + 119, RIGHT_PANEL_WIDTH - INSET * 2, BUTTON_HEIGHT)
                .build());

        int previewPanelX = getModulePreviewPanelX();
        int previewPanelY = getModulePreviewPanelY();
        int previewPanelWidth = getModulePreviewPanelWidth();
        int buttonWidth = (previewPanelWidth - 24) / 3;
        int controlTop = previewPanelY + 138;
        moduleRotateButton = addRenderableWidget(Button.builder(Component.literal("Rotate"), button -> session.rotateModulePreview())
                .bounds(previewPanelX + 10, controlTop, buttonWidth, BUTTON_HEIGHT)
                .build());
        moduleZoomOutButton = addRenderableWidget(Button.builder(Component.literal("Zoom-"), button -> session.zoomModulePreview(-25))
                .bounds(previewPanelX + 12 + buttonWidth, controlTop, buttonWidth, BUTTON_HEIGHT)
                .build());
        moduleZoomInButton = addRenderableWidget(Button.builder(Component.literal("Zoom+"), button -> session.zoomModulePreview(25))
                .bounds(previewPanelX + 14 + buttonWidth * 2, controlTop, buttonWidth, BUTTON_HEIGHT)
                .build());
        moduleBoundsButton = addRenderableWidget(Button.builder(Component.literal(toggleLabel("Bounds", session.isShowModuleBounds())),
                        button -> {
                            session.setShowModuleBounds(!session.isShowModuleBounds());
                            button.setMessage(Component.literal(toggleLabel("Bounds", session.isShowModuleBounds())));
                        })
                .bounds(previewPanelX + 10, controlTop + BUTTON_HEIGHT + 6, buttonWidth, BUTTON_HEIGHT)
                .build());
        moduleConnectorsButton = addRenderableWidget(Button.builder(Component.literal(toggleLabel("Conn", session.isShowModuleConnectors())),
                        button -> {
                            session.setShowModuleConnectors(!session.isShowModuleConnectors());
                            button.setMessage(Component.literal(toggleLabel("Conn", session.isShowModuleConnectors())));
                        })
                .bounds(previewPanelX + 12 + buttonWidth, controlTop + BUTTON_HEIGHT + 6, buttonWidth, BUTTON_HEIGHT)
                .build());
        moduleAirButton = addRenderableWidget(Button.builder(Component.literal(toggleLabel("Air", session.isShowModuleAir())),
                        button -> {
                            session.setShowModuleAir(!session.isShowModuleAir());
                            button.setMessage(Component.literal(toggleLabel("Air", session.isShowModuleAir())));
                        })
                .bounds(previewPanelX + 14 + buttonWidth * 2, controlTop + BUTTON_HEIGHT + 6, buttonWidth, BUTTON_HEIGHT)
                .build());

        moduleListTop = contentY + 150;
        moduleListBottom = inspectorContentBottom - 220;
    }

    private void initTransformWidgets(int contentY) {
        if (session.getSelection() != PcgEditorSelection.REGION && session.getSelection() != PcgEditorSelection.SPLINE_POINT) {
            return;
        }

        BlockPos origin = session.getSelection() == PcgEditorSelection.REGION
                ? ClientSelectionState.getRegionSelection().getMin()
                : session.getSelectedSplinePoint();
        if (origin == null) {
            return;
        }

        int x = rightInnerX + 26;
        int y = contentY + 44;
        int width = 56;
        transformXBox = createIntegerField(x, y, width, origin.getX());
        transformYBox = createIntegerField(x + width + 8, y, width, origin.getY());
        transformZBox = createIntegerField(x + (width + 8) * 2, y, width, origin.getZ());
        transformApplyButton = addRenderableWidget(Button.builder(Component.literal("Apply"), button -> applyTransformEdit())
                .bounds(rightInnerX + RIGHT_PANEL_WIDTH - INSET * 2 - 58, y, 58, BUTTON_HEIGHT)
                .build());
    }

    private EditBox createIntegerField(int x, int y, int width, int value) {
        EditBox box = new EditBox(this.font, x, y, width, FIELD_HEIGHT, Component.empty());
        box.setValue(String.valueOf(value));
        box.setBordered(true);
        box.setTextColor(TEXT_BRIGHT);
        addRenderableWidget(box);
        return box;
    }

    private int computeInspectorContentTop() {
        return rightPanelY + 26;
    }

    private void layoutParameterFields() {
        int viewportHeight = Math.max(40, parameterViewportBottom - parameterViewportTop);
        int contentHeight = Math.max(0, parameterBoxes.size() * PARAMETER_ROW_HEIGHT);
        maxParameterScroll = Math.max(0, contentHeight - viewportHeight);
        parameterScroll = clamp(parameterScroll, 0, maxParameterScroll);

        int labelWidth = 102;
        int fieldX = rightInnerX + labelWidth;
        int fieldWidth = RIGHT_PANEL_WIDTH - INSET * 2 - labelWidth - 8;
        int baseY = parameterViewportTop - parameterScroll;
        for (int i = 0; i < parameterBoxes.size(); i++) {
            EditBox box = parameterBoxes.get(i);
            int y = baseY + i * PARAMETER_ROW_HEIGHT + 2;
            boolean visible = y + FIELD_HEIGHT >= parameterViewportTop && y <= parameterViewportBottom;
            box.setX(fieldX);
            box.setY(y);
            box.setWidth(fieldWidth);
            box.setVisible(visible);
            box.active = visible;
            if (!visible) {
                box.setFocused(false);
            }
        }
    }

    @Override
    public void tick() {
        session.syncSelectionDefaults();
        updateActionStates();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawTopToolbar(guiGraphics);
        drawLeftToolbar(guiGraphics);
        drawRightInspector(guiGraphics);
        drawBottomBar(guiGraphics);
        drawViewportHints(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && isInViewport(mouseX, mouseY)) {
            startNavigation();
            return true;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            return true;
        }
        if (button == 0 && isInViewport(mouseX, mouseY) && !session.isNavigating()) {
            return handleViewportClick();
        }
        if (button == 0 && session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY && mouseX >= rightPanelX && mouseX <= rightPanelX + RIGHT_PANEL_WIDTH
                && mouseY >= moduleListTop && mouseY <= moduleListBottom) {
            return selectModuleFromList(mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && session.isNavigating()) {
            stopNavigation();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY
                && mouseX >= rightPanelX && mouseX <= rightPanelX + RIGHT_PANEL_WIDTH
                && mouseY >= moduleListTop && mouseY <= moduleListBottom) {
            moduleListScroll = clamp(moduleListScroll - (int) Math.signum(delta), 0, maxModuleListScroll);
            return true;
        }
        if (parameterBoxes.size() > 0
                && mouseX >= rightPanelX && mouseX <= rightPanelX + RIGHT_PANEL_WIDTH
                && mouseY >= parameterViewportTop && mouseY <= parameterViewportBottom) {
            parameterScroll = clamp(parameterScroll - (int) (delta * 18.0D), 0, maxParameterScroll);
            layoutParameterFields();
            return true;
        }
        if (mouseX >= bottomBarX && mouseX <= bottomBarX + this.width - OUTER_PAD * 2
                && mouseY >= bottomBarY && mouseY <= bottomBarY + BOTTOM_BAR_HEIGHT) {
            logScroll = clamp(logScroll - (int) Math.signum(delta), 0, maxLogScroll);
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
        if (keyCode == 80) {
            generatePreview(false);
            return true;
        }
        if (keyCode == 82) {
            generatePreview(true);
            return true;
        }
        if (keyCode == 66) {
            requestBake();
            return true;
        }
        if (keyCode == 257 && session.getActiveTool() == PcgEditorTool.SPLINE) {
            session.setSelection(PcgEditorSelection.SPLINE);
            session.log(PcgEditorLogEntry.Severity.INFO, "Spline input confirmed.");
            return true;
        }
        if (keyCode == 70) {
            focusSelection();
            return true;
        }
        if (keyCode == 261) {
            deleteSelection();
            return true;
        }
        if (hasControlDown() && keyCode == 90) {
            runAction("blockwright undo", PcgEditorLogEntry.Severity.INFO, "Undo requested.");
            return true;
        }
        if (hasControlDown() && keyCode == 89) {
            session.log(PcgEditorLogEntry.Severity.DEBUG, "Redo is not implemented in this MVP.");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        stopNavigation();
        session.setNavigating(false);
    }

    @Override
    public void onClose() {
        stopNavigation();
        session.close();
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawTopToolbar(GuiGraphics guiGraphics) {
        drawPanel(guiGraphics, topBarX, topBarY, this.width - OUTER_PAD * 2, TOP_BAR_HEIGHT);
        guiGraphics.drawString(this.font, "PCG EDITOR", topBarX + 40, topBarY + 14, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "MODE:", topBarX + 150, topBarY + 14, TEXT_MUTED);
        guiGraphics.drawString(this.font, session.getActiveTool().getTitle(), topBarX + 188, topBarY + 14, TEXT_GREEN);
        guiGraphics.drawString(this.font, "PACK:", topBarX + 258, topBarY + 14, TEXT_MUTED);
        guiGraphics.drawString(this.font,
                trimToWidth(session.getSelectedPack() == null ? "<none>" : session.getSelectedPack().getMetadata().id, 120),
                topBarX + 294, topBarY + 14, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "PRESET:", topBarX + 422, topBarY + 14, TEXT_MUTED);
        guiGraphics.drawString(this.font,
                trimToWidth(session.getSelectedPreset() == null ? "<none>" : session.getSelectedPreset().id, 138),
                topBarX + 470, topBarY + 14, TEXT_BRIGHT);
    }

    private void drawLeftToolbar(GuiGraphics guiGraphics) {
        drawPanel(guiGraphics, leftBarX, leftBarY, LEFT_BAR_WIDTH, viewportHeight);
        int y = leftBarY + 8;
        for (PcgEditorTool tool : PcgEditorTool.values()) {
            if (tool == session.getActiveTool()) {
                guiGraphics.fill(leftBarX + 4, y, leftBarX + 8, y + TOOL_BUTTON_HEIGHT, TEXT_BLUE);
            } else if (tool == PcgEditorTool.PAINT_MASK) {
                guiGraphics.fill(leftBarX + 4, y, leftBarX + 8, y + TOOL_BUTTON_HEIGHT, TOOL_DISABLED);
            }
            y += TOOL_BUTTON_HEIGHT + 8;
        }
    }

    private void drawRightInspector(GuiGraphics guiGraphics) {
        drawPanel(guiGraphics, rightPanelX, rightPanelY, RIGHT_PANEL_WIDTH, inspectorContentBottom - rightPanelY);
        guiGraphics.drawString(this.font, "DETAILS", rightInnerX, rightPanelY + 10, TEXT_BRIGHT);

        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY) {
            drawModuleLibraryPanel(guiGraphics);
        } else {
            drawInspectorSections(guiGraphics);
        }

        drawModulePreviewPanel(guiGraphics);
    }

    private void drawInspectorSections(GuiGraphics guiGraphics) {
        int y = computeInspectorContentTop();
        y = drawInspectorHeader(guiGraphics, y, "OBJECT");
        guiGraphics.drawString(this.font, "Name", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, session.getSelectionLabel(), rightInnerX + 86, y, TEXT_BRIGHT);
        y += 12;
        guiGraphics.drawString(this.font, "Type", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, describeSelectionType(), rightInnerX + 86, y, TEXT_BRIGHT);

        y += 20;
        y = drawInspectorHeader(guiGraphics, y, "TRANSFORM");
        drawTransformSection(guiGraphics, y);
        y += 46;

        y = drawInspectorHeader(guiGraphics, y, "INPUT");
        drawInputSection(guiGraphics, y);
        y += 34;

        y = drawInspectorHeader(guiGraphics, y, "PRESET");
        drawPresetSection(guiGraphics, y);
        y += 34;

        y = drawInspectorHeader(guiGraphics, y, "PARAMETERS");
        drawParameterLabels(guiGraphics, y + 2);

        int validationY = inspectorContentBottom - 172;
        validationY = drawInspectorHeader(guiGraphics, validationY, "VALIDATION");
        drawValidationSection(guiGraphics, validationY);

        int actionsY = inspectorContentBottom - 82;
        actionsY = drawInspectorHeader(guiGraphics, actionsY, "ACTIONS");
        drawActionHints(guiGraphics, actionsY);
    }

    private void drawModuleLibraryPanel(GuiGraphics guiGraphics) {
        int y = computeInspectorContentTop();
        y = drawInspectorHeader(guiGraphics, y, "MODULE LIBRARY");
        LoadedPack pack = session.getSelectedPack();
        guiGraphics.drawString(this.font, "Pack", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, trimToWidth(pack == null ? "<none>" : pack.getMetadata().id, RIGHT_PANEL_WIDTH - INSET * 2 - 54),
                rightInnerX + 54, y, TEXT_BRIGHT);
        y += 66;

        List<ModuleDefinition> modules = getFilteredModules(pack);
        ModuleDefinition selectedModule = session.getSelectedModule();
        updateModuleScroll(modules.size());
        guiGraphics.drawString(this.font, "Modules", rightInnerX, y, TEXT_BRIGHT);
        int rowY = moduleListTop;
        int visibleRows = Math.max(1, (moduleListBottom - moduleListTop) / MODULE_ROW_HEIGHT);
        int end = Math.min(modules.size(), moduleListScroll + visibleRows);
        for (int i = moduleListScroll; i < end; i++) {
            ModuleDefinition module = modules.get(i);
            boolean selected = selectedModule != null && module.id.equals(selectedModule.id);
            if (selected) {
                guiGraphics.fill(rightPanelX + 1, rowY - 1, rightPanelX + RIGHT_PANEL_WIDTH - 1, rowY + MODULE_ROW_HEIGHT - 2, 0x663A4F5E);
            }
            guiGraphics.drawString(this.font, trimToWidth(module.id, RIGHT_PANEL_WIDTH - INSET * 2), rightInnerX, rowY, selected ? TEXT_BRIGHT : TEXT_MUTED);
            guiGraphics.drawString(this.font, trimToWidth(safe(module.moduleKind), RIGHT_PANEL_WIDTH - INSET * 2), rightInnerX, rowY + 10, selected ? TEXT_BLUE : TEXT_MUTED);
            rowY += MODULE_ROW_HEIGHT;
        }

        int detailY = moduleListBottom + 10;
        detailY = drawInspectorHeader(guiGraphics, detailY, "MODULE DETAIL");
        drawModuleDetails(guiGraphics, detailY, selectedModule);
    }

    private void drawModulePreviewPanel(GuiGraphics guiGraphics) {
        int panelX = getModulePreviewPanelX();
        int panelY = getModulePreviewPanelY();
        int panelWidth = getModulePreviewPanelWidth();
        int panelHeight = getModulePreviewPanelHeight();
        drawPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight);
        guiGraphics.drawString(this.font, "MODULE PREVIEW", panelX + INSET, panelY + 10, TEXT_BRIGHT);
        ModuleDefinition module = session.getSelectedModule();
        ModuleSchematicPreviewRenderer.render(guiGraphics, panelX + 10, panelY + 28, panelWidth - 20, 136,
                module,
                session.getModuleRotationQuarterTurns(),
                session.getModulePreviewZoomPercent(),
                session.isShowModuleBounds(),
                session.isShowModuleConnectors(),
                session.isShowModuleAir());
        if (module == null) {
            guiGraphics.drawString(this.font, "No module selected.", panelX + INSET, panelY + 172, TEXT_MUTED);
            return;
        }
        guiGraphics.drawString(this.font, trimToWidth(module.id, panelWidth - 20), panelX + INSET, panelY + 172, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Tags: " + trimToWidth(join(module.tags), panelWidth - 20), panelX + INSET, panelY + 184, TEXT_MUTED);
        SpongeSchematicData data = module.schematicData;
        guiGraphics.drawString(this.font, "Size: " + (data == null ? "<none>" : data.getWidth() + " x " + data.getHeight() + " x " + data.getLength()),
                panelX + INSET, panelY + 196, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Mods: " + (data == null ? "<none>" : join(data.getRequiredMods())),
                panelX + INSET, panelY + 208, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Rot " + (session.getModuleRotationQuarterTurns() * 90) + "° | "
                        + session.getModulePreviewZoomPercent() + "% | "
                        + onOff(session.isShowModuleBounds()) + " B | " + onOff(session.isShowModuleConnectors()) + " C | " + onOff(session.isShowModuleAir()) + " Air",
                panelX + INSET, panelY + 224, TEXT_BLUE);
    }

    private void drawBottomBar(GuiGraphics guiGraphics) {
        drawPanel(guiGraphics, bottomBarX, bottomBarY, this.width - OUTER_PAD * 2, BOTTOM_BAR_HEIGHT);
        int sectionWidth = (this.width - OUTER_PAD * 2 - 20) / 5;
        int x = bottomBarX + 8;
        drawBottomSection(guiGraphics, x, "POSITION", describePosition(), sectionWidth);
        x += sectionWidth + 4;
        drawBottomSection(guiGraphics, x, "SELECTION", describeSelectionStats(), sectionWidth);
        x += sectionWidth + 4;
        drawBottomSection(guiGraphics, x, "STATS", describePreviewStats(), sectionWidth);
        x += sectionWidth + 4;
        drawBottomSection(guiGraphics, x, "WARNINGS", getWarningSummary(), sectionWidth);
        x += sectionWidth + 4;
        drawMessageLog(guiGraphics, x, sectionWidth + 20);
    }

    private void drawBottomSection(GuiGraphics guiGraphics, int x, String title, List<String> lines, int width) {
        guiGraphics.fill(x, bottomBarY + 8, x + width, bottomBarY + BOTTOM_BAR_HEIGHT - 8, PANEL_HEADER);
        guiGraphics.drawString(this.font, title, x + 8, bottomBarY + 14, TEXT_BRIGHT);
        int y = bottomBarY + 32;
        for (String line : lines) {
            guiGraphics.drawString(this.font, trimToWidth(line, width - 16), x + 8, y, TEXT_MUTED);
            y += 14;
        }
    }

    private void drawMessageLog(GuiGraphics guiGraphics, int x, int width) {
        guiGraphics.fill(x, bottomBarY + 8, x + width, bottomBarY + BOTTOM_BAR_HEIGHT - 8, PANEL_HEADER);
        guiGraphics.drawString(this.font, "MESSAGE LOG", x + 8, bottomBarY + 14, TEXT_BRIGHT);
        List<PcgEditorLogEntry> entries = session.getLogEntries();
        maxLogScroll = Math.max(0, entries.size() - 4);
        logScroll = clamp(logScroll, 0, maxLogScroll);
        int y = bottomBarY + 32;
        for (int i = logScroll; i < Math.min(entries.size(), logScroll + 4); i++) {
            PcgEditorLogEntry entry = entries.get(i);
            guiGraphics.drawString(this.font, trimToWidth(entry.getMessage(), width - 16), x + 8, y, logColor(entry), false);
            y += LOG_ROW_HEIGHT;
        }
    }

    private void drawViewportHints(GuiGraphics guiGraphics) {
        guiGraphics.fill(viewportX + 18, viewportY + 18, viewportX + 204, viewportY + 70, 0xA0121820);
        guiGraphics.drawString(this.font, "Hold RMB to navigate,", viewportX + 28, viewportY + 30, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "WASD to fly,", viewportX + 28, viewportY + 42, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "LMB to edit.", viewportX + 28, viewportY + 54, TEXT_BRIGHT);

        guiGraphics.fill(viewportX + 18, viewportY + viewportHeight - 86, viewportX + 146, viewportY + viewportHeight - 18, 0xA0121820);
        guiGraphics.drawString(this.font, "SNAP", viewportX + 28, viewportY + viewportHeight - 78, TEXT_MUTED);
        guiGraphics.drawString(this.font, session.getSnapStep() + " blocks", viewportX + 28, viewportY + viewportHeight - 62, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Selection: " + session.getSelectionLabel(), viewportX + 28, viewportY + viewportHeight - 42, TEXT_MUTED);

        PcgEditorPreviewState previewState = session.getPreviewState();
        if (previewState == PcgEditorPreviewState.DIRTY) {
            guiGraphics.fill(viewportX + viewportWidth - 168, viewportY + 18, viewportX + viewportWidth - 18, viewportY + 46, 0xB0332A16);
            guiGraphics.drawString(this.font, "Outdated Preview", viewportX + viewportWidth - 154, viewportY + 28, TEXT_YELLOW);
        } else if (previewState == PcgEditorPreviewState.ERROR) {
            guiGraphics.fill(viewportX + viewportWidth - 160, viewportY + 18, viewportX + viewportWidth - 18, viewportY + 46, 0xB03A1719);
            guiGraphics.drawString(this.font, "Preview Error", viewportX + viewportWidth - 146, viewportY + 28, TEXT_RED);
        }
    }

    private int drawInspectorHeader(GuiGraphics guiGraphics, int y, String title) {
        guiGraphics.fill(rightPanelX + 1, y, rightPanelX + RIGHT_PANEL_WIDTH - 1, y + 14, PANEL_HEADER);
        guiGraphics.drawString(this.font, title, rightInnerX, y + 3, TEXT_BRIGHT);
        return y + 22;
    }

    private void drawTransformSection(GuiGraphics guiGraphics, int y) {
        guiGraphics.drawString(this.font, "Pos", rightInnerX, y + 5, TEXT_MUTED);
        guiGraphics.drawString(this.font, "X", rightInnerX + 30, y + 5, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Y", rightInnerX + 94, y + 5, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Z", rightInnerX + 158, y + 5, TEXT_MUTED);
        if (session.getSelection() == PcgEditorSelection.REGION) {
            BoxRegionSelection region = ClientSelectionState.getRegionSelection();
            BlockPos min = region.getMin();
            BlockPos max = region.getMax();
            guiGraphics.drawString(this.font, "Max", rightInnerX, y + 26, TEXT_MUTED);
            guiGraphics.drawString(this.font, max == null ? "<unset>" : max.toShortString(), rightInnerX + 86, y + 26, TEXT_BRIGHT);
            guiGraphics.drawString(this.font, "Size", rightInnerX, y + 38, TEXT_MUTED);
            guiGraphics.drawString(this.font, region.isComplete() ? region.getWidth() + " x " + region.getHeight() + " x " + region.getDepth() : "<none>",
                    rightInnerX + 86, y + 38, TEXT_BRIGHT);
            return;
        }
        if (session.getSelection() == PcgEditorSelection.SPLINE || session.getSelection() == PcgEditorSelection.SPLINE_POINT) {
            BlockPos point = session.getSelectedSplinePoint();
            SplineSelection spline = ClientSelectionState.getSplineSelection();
            guiGraphics.drawString(this.font, "Points", rightInnerX, y + 26, TEXT_MUTED);
            guiGraphics.drawString(this.font, String.valueOf(spline.getPoints().size()), rightInnerX + 86, y + 26, TEXT_BRIGHT);
            guiGraphics.drawString(this.font, "Point", rightInnerX, y + 38, TEXT_MUTED);
            guiGraphics.drawString(this.font, point == null ? "<unset>" : point.toShortString(), rightInnerX + 86, y + 38, TEXT_BRIGHT);
            return;
        }
        guiGraphics.drawString(this.font, "Focus", rightInnerX, y + 26, TEXT_MUTED);
        guiGraphics.drawString(this.font, safe(session.getSelectionLabel()), rightInnerX + 86, y + 26, TEXT_BRIGHT);
    }

    private void drawInputSection(GuiGraphics guiGraphics, int y) {
        PresetDefinition preset = session.getSelectedPreset();
        String required = describePresetMode(preset);
        guiGraphics.drawString(this.font, "Required", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, required, rightInnerX + 86, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Current", rightInnerX, y + 12, TEXT_MUTED);
        guiGraphics.drawString(this.font, describeSelectionInput(), rightInnerX + 86, y + 12,
                isInputCompatible(required) ? TEXT_GREEN : TEXT_RED);
    }

    private void drawPresetSection(GuiGraphics guiGraphics, int y) {
        PresetDefinition preset = session.getSelectedPreset();
        LoadedPack pack = session.getSelectedPack();
        guiGraphics.drawString(this.font, "Preset", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, preset == null ? "<none>" : preset.id, rightInnerX + 86, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Pack", rightInnerX, y + 12, TEXT_MUTED);
        guiGraphics.drawString(this.font, pack == null ? "<none>" : pack.getMetadata().id, rightInnerX + 86, y + 12, TEXT_BRIGHT);
    }

    private void drawParameterLabels(GuiGraphics guiGraphics, int y) {
        int baseY = parameterViewportTop - parameterScroll;
        for (int i = 0; i < parameterKeys.size(); i++) {
            int labelY = baseY + i * PARAMETER_ROW_HEIGHT + 7;
            if (labelY < parameterViewportTop - LOG_ROW_HEIGHT || labelY > parameterViewportBottom - LOG_ROW_HEIGHT) {
                continue;
            }
            guiGraphics.drawString(this.font, trimToWidth(parameterKeys.get(i), 94), rightInnerX, labelY, TEXT_MUTED);
        }
        if (parameterBoxes.size() > 0 && maxParameterScroll > 0) {
            guiGraphics.drawString(this.font, "Scroll", rightPanelX + RIGHT_PANEL_WIDTH - 48, y, TEXT_MUTED);
        }
    }

    private void drawValidationSection(GuiGraphics guiGraphics, int y) {
        PcgEditorPreviewState state = session.getPreviewState();
        guiGraphics.drawString(this.font, "State", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, state.name(), rightInnerX + 86, y, colorForPreviewState(state));
        List<String> warnings = getWarningSummary();
        int lineY = y + 12;
        for (String warning : warnings.subList(0, Math.min(3, warnings.size()))) {
            guiGraphics.drawString(this.font, trimToWidth(warning, RIGHT_PANEL_WIDTH - INSET * 2), rightInnerX, lineY, warning.startsWith("ERROR") ? TEXT_RED : TEXT_YELLOW);
            lineY += 12;
        }
        if (warnings.isEmpty()) {
            guiGraphics.drawString(this.font, "No issues found.", rightInnerX, lineY, TEXT_GREEN);
        }
    }

    private void drawActionHints(GuiGraphics guiGraphics, int y) {
        guiGraphics.drawString(this.font, "F Focus  |  Del Delete", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, "P Preview  |  B Bake", rightInnerX, y + 12, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Ctrl+Z Undo  |  Ctrl+Y Redo", rightInnerX, y + 24, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Transform uses the X/Y/Z fields above.", rightInnerX, y + 36, TEXT_MUTED);
    }

    private void drawModuleDetails(GuiGraphics guiGraphics, int y, ModuleDefinition module) {
        if (module == null) {
            guiGraphics.drawString(this.font, "No module selected.", rightInnerX, y, TEXT_MUTED);
            return;
        }
        SpongeSchematicData data = module.schematicData;
        guiGraphics.drawString(this.font, "Kind", rightInnerX, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, safe(module.moduleKind), rightInnerX + 86, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Size", rightInnerX, y + 12, TEXT_MUTED);
        guiGraphics.drawString(this.font, data == null ? joinInts(module.size) : data.getWidth() + " x " + data.getHeight() + " x " + data.getLength(),
                rightInnerX + 86, y + 12, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Tags", rightInnerX, y + 24, TEXT_MUTED);
        guiGraphics.drawString(this.font, trimToWidth(join(module.tags), RIGHT_PANEL_WIDTH - INSET * 2 - 86), rightInnerX + 86, y + 24, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Mods", rightInnerX, y + 36, TEXT_MUTED);
        guiGraphics.drawString(this.font, trimToWidth(data == null ? "<none>" : join(data.getRequiredMods()), RIGHT_PANEL_WIDTH - INSET * 2 - 86),
                rightInnerX + 86, y + 36, TEXT_BRIGHT);
        if (!module.connectors.isEmpty()) {
            int lineY = y + 48;
            guiGraphics.drawString(this.font, "Connectors", rightInnerX, lineY, TEXT_MUTED);
            for (ModuleConnector connector : module.connectors.subList(0, Math.min(3, module.connectors.size()))) {
                lineY += 12;
                guiGraphics.drawString(this.font, trimToWidth(connector.id + " / " + safe(connector.direction), RIGHT_PANEL_WIDTH - INSET * 2),
                        rightInnerX, lineY, TEXT_BLUE);
            }
        }
    }

    private boolean handleViewportClick() {
        switch (session.getActiveTool()) {
            case BOX_REGION -> {
                handleBoxRegionClick();
                return true;
            }
            case SPLINE -> {
                runAction("blockwright spline add", PcgEditorLogEntry.Severity.INFO, "Added spline point.");
                session.selectSplineIfPresent();
                session.setSelection(PcgEditorSelection.SPLINE_POINT);
                session.setSelectedSplinePointIndex(ClientSelectionState.getSplineSelection().getPoints().size() - 1);
                rebuildWidgets();
                return true;
            }
            case SELECT -> {
                selectBestObject();
                return true;
            }
            case TRANSFORM -> {
                focusSelection();
                return true;
            }
            case MODULE_LIBRARY, PAINT_MASK -> {
                return false;
            }
        }
        return false;
    }

    private void handleBoxRegionClick() {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        if (region.getPos1() == null || region.isComplete()) {
            if (region.isComplete()) {
                runAction("blockwright region clear", PcgEditorLogEntry.Severity.INFO, "Started a new box region.");
            }
            runAction("blockwright region pos1", PcgEditorLogEntry.Severity.INFO, "Set region corner P1.");
        } else {
            runAction("blockwright region pos2", PcgEditorLogEntry.Severity.INFO, "Set region corner P2.");
            session.setSelection(PcgEditorSelection.REGION);
        }
        rebuildWidgets();
    }

    private void selectBestObject() {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        PreviewPlan preview = ClientPreviewState.getPreviewPlan();
        if (region.isComplete()) {
            session.setSelection(PcgEditorSelection.REGION);
            session.log(PcgEditorLogEntry.Severity.INFO, "Selected Box Region_01.");
            return;
        }
        if (!spline.getPoints().isEmpty()) {
            session.setSelection(PcgEditorSelection.SPLINE_POINT);
            session.setSelectedSplinePointIndex(spline.getPoints().size() - 1);
            session.log(PcgEditorLogEntry.Severity.INFO, "Selected RoadSpline_01.");
            return;
        }
        if (preview != null) {
            session.setSelection(PcgEditorSelection.PREVIEW);
            session.log(PcgEditorLogEntry.Severity.INFO, "Selected structure preview.");
        }
    }

    private boolean selectModuleFromList(double mouseY) {
        List<ModuleDefinition> modules = getFilteredModules(session.getSelectedPack());
        int index = (int) ((mouseY - moduleListTop) / MODULE_ROW_HEIGHT) + moduleListScroll;
        if (index < 0 || index >= modules.size()) {
            return false;
        }
        session.setSelectedModuleId(modules.get(index).id);
        session.setSelection(PcgEditorSelection.MODULE);
        session.log(PcgEditorLogEntry.Severity.INFO, "Selected module " + modules.get(index).id + ".");
        return true;
    }

    private void startNavigation() {
        Minecraft minecraft = Minecraft.getInstance();
        session.setNavigating(true);
        minecraft.mouseHandler.grabMouse();
        minecraft.mouseHandler.setIgnoreFirstMove();
    }

    private void stopNavigation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (session.isNavigating()) {
            session.setNavigating(false);
            minecraft.mouseHandler.releaseMouse();
        }
    }

    private void updateActionStates() {
        PcgEditorPreviewState previewState = session.getPreviewState();
        PresetDefinition preset = session.getSelectedPreset();
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (previewButton != null) {
            previewButton.active = preset != null && hasValidInputForPreset(preset);
        }
        if (regenerateButton != null) {
            regenerateButton.active = preset != null && hasValidInputForPreset(preset);
        }
        if (bakeButton != null) {
            bakeButton.active = previewState == PcgEditorPreviewState.VALID || previewState == PcgEditorPreviewState.WARNING;
        }
        if (undoButton != null) {
            undoButton.active = true;
        }
        if (redoButton != null) {
            redoButton.active = false;
        }
        if (reloadButton != null) {
            reloadButton.active = true;
        }
        if (previewPlan != null && previewPlan.isStale()) {
            previewButton.setMessage(Component.literal("Preview"));
            regenerateButton.setMessage(Component.literal("Regenerate"));
        }
        ModuleDefinition selectedModule = session.getSelectedModule();
        if (moduleExportButton != null) {
            moduleExportButton.active = ClientSelectionState.getRegionSelection().isComplete();
        }
        if (moduleRotateButton != null) {
            moduleRotateButton.active = selectedModule != null;
        }
        if (moduleZoomInButton != null) {
            moduleZoomInButton.active = selectedModule != null && session.getModulePreviewZoomPercent() < 200;
        }
        if (moduleZoomOutButton != null) {
            moduleZoomOutButton.active = selectedModule != null && session.getModulePreviewZoomPercent() > 50;
        }
        if (moduleBoundsButton != null) {
            moduleBoundsButton.active = selectedModule != null;
        }
        if (moduleConnectorsButton != null) {
            moduleConnectorsButton.active = selectedModule != null;
        }
        if (moduleAirButton != null) {
            moduleAirButton.active = selectedModule != null;
        }
    }

    private void switchTool(PcgEditorTool tool) {
        if (tool == PcgEditorTool.PAINT_MASK) {
            session.log(PcgEditorLogEntry.Severity.DEBUG, "Paint / Mask is reserved for a future milestone.");
            return;
        }
        session.setActiveTool(tool);
        if (tool == PcgEditorTool.MODULE_LIBRARY) {
            session.setSelection(PcgEditorSelection.MODULE);
            if (session.getSelectedModule() != null) {
                session.setSelectedModuleId(session.getSelectedModule().id);
            }
        } else if (tool == PcgEditorTool.BOX_REGION) {
            session.selectRegionIfPresent();
        } else if (tool == PcgEditorTool.SPLINE) {
            session.selectSplineIfPresent();
            if (!ClientSelectionState.getSplineSelection().getPoints().isEmpty()) {
                session.setSelection(PcgEditorSelection.SPLINE_POINT);
                session.setSelectedSplinePointIndex(ClientSelectionState.getSplineSelection().getPoints().size() - 1);
            }
        } else if (tool == PcgEditorTool.SELECT) {
            selectBestObject();
        }
        session.log(PcgEditorLogEntry.Severity.INFO, "Active tool: " + tool.getTitle() + ".");
        Minecraft.getInstance().setScreen(new PcgEditorScreen());
    }

    private void cyclePack(int delta) {
        List<LoadedPack> packs = Blockwright.getPackManager().getLoadedPacks();
        if (packs.isEmpty()) {
            return;
        }
        LoadedPack current = session.getSelectedPack();
        int currentIndex = current == null ? 0 : packs.indexOf(current);
        int nextIndex = Math.floorMod(currentIndex + delta, packs.size());
        session.setSelectedPackId(packs.get(nextIndex).getMetadata().id);
        session.setSelectedPresetId(null);
        session.setSelectedModuleId(null);
        session.markDirty("Pack selection changed.");
        Minecraft.getInstance().setScreen(new PcgEditorScreen());
    }

    private void cyclePreset(int delta) {
        LoadedPack pack = session.getSelectedPack();
        if (pack == null || pack.getPresets().isEmpty()) {
            return;
        }
        List<PresetDefinition> presets = new ArrayList<>(pack.getPresets().values());
        PresetDefinition current = session.getSelectedPreset();
        int currentIndex = current == null ? 0 : presets.indexOf(current);
        int nextIndex = Math.floorMod(currentIndex + delta, presets.size());
        session.setSelectedPresetId(presets.get(nextIndex).id);
        session.markDirty("Preset selection changed.");
        Minecraft.getInstance().setScreen(new PcgEditorScreen());
    }

    private void reloadPacks() {
        Blockwright.getPackManager().reload();
        session.markDirty("Preset packs reloaded.");
        session.log(PcgEditorLogEntry.Severity.SUCCESS, "Reloaded " + Blockwright.getPackManager().getLoadedPackCount() + " pack(s).");
        sendCommand("blockwright reload");
        Minecraft.getInstance().setScreen(new PcgEditorScreen());
    }

    private void generatePreview(boolean regenerate) {
        PresetDefinition preset = session.getSelectedPreset();
        if (preset == null) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "No preset is selected.");
            return;
        }
        if (!hasValidInputForPreset(preset)) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "The selected preset input is not valid.");
            return;
        }
        session.log(PcgEditorLogEntry.Severity.INFO, regenerate ? "Regenerating preview..." : "Generating preview...");
        Map<String, String> overrideMap = new LinkedHashMap<>();
        List<String> overrideParts = new ArrayList<>();
        for (int i = 0; i < parameterKeys.size(); i++) {
            String key = parameterKeys.get(i);
            String value = parameterBoxes.get(i).getValue();
            overrideMap.put(key, value);
            overrideParts.add(key + "=" + value);
        }
        PreviewPlan plan = ClientPreviewGenerator.generate(preset.id, overrideMap);
        if (plan == null) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Preview generation failed.");
            return;
        }
        ClientPreviewState.setPreviewPlan(plan);
        String command = "blockwright preview generate " + preset.id;
        if (!overrideParts.isEmpty()) {
            command += " " + String.join(",", overrideParts);
        }
        sendCommand(command);
        session.setSelection(PcgEditorSelection.PREVIEW);
        session.log(plan.canBake() ? PcgEditorLogEntry.Severity.SUCCESS : PcgEditorLogEntry.Severity.WARNING,
                "Preview generated: " + plan.getPlannedBlocks().size() + " blocks, state=" + session.getPreviewState().name() + ".");
        updateActionStates();
    }

    private void requestBake() {
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (previewPlan == null) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Bake requires a generated preview.");
            return;
        }
        if (!(session.getPreviewState() == PcgEditorPreviewState.VALID || session.getPreviewState() == PcgEditorPreviewState.WARNING)) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Bake requires a valid or warning preview state.");
            return;
        }
        if (!needsBakeConfirmation(previewPlan)) {
            runAction("blockwright bake", PcgEditorLogEntry.Severity.SUCCESS, "Bake requested.");
            return;
        }

        int blockEntities = countPreviewBlockEntities(previewPlan);
        Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                Minecraft.getInstance().setScreen(new PcgEditorScreen());
                runAction("blockwright bake", PcgEditorLogEntry.Severity.SUCCESS, "Bake requested.");
                return;
            }
            Minecraft.getInstance().setScreen(new PcgEditorScreen());
            session.log(PcgEditorLogEntry.Severity.INFO, "Bake cancelled.");
        }, Component.literal("Confirm Bake"),
                Component.literal("Bake " + previewPlan.getPlannedBlocks().size() + " blocks across " + countPreviewChunks(previewPlan)
                        + " chunks" + (blockEntities > 0 ? " with " + blockEntities + " block entities" : "") + "?")));
    }

    private void runAction(String command, PcgEditorLogEntry.Severity severity, String message) {
        sendCommand(command);
        session.log(severity, message);
        if ("blockwright bake".equals(command)) {
            session.setSelection(PcgEditorSelection.NONE);
        }
    }

    private void deleteSelection() {
        switch (session.getSelection()) {
            case REGION -> runAction("blockwright region clear", PcgEditorLogEntry.Severity.INFO, "Deleted Box Region_01.");
            case SPLINE -> runAction("blockwright spline clear", PcgEditorLogEntry.Severity.INFO, "Deleted RoadSpline_01.");
            case SPLINE_POINT -> {
                BlockPos point = session.getSelectedSplinePoint();
                if (point != null) {
                    runAction("blockwright spline remove " + session.getSelectedSplinePointIndex(), PcgEditorLogEntry.Severity.INFO, "Deleted selected spline point.");
                }
            }
            case PREVIEW -> runAction("blockwright preview clear", PcgEditorLogEntry.Severity.INFO, "Cleared preview.");
            default -> session.log(PcgEditorLogEntry.Severity.DEBUG, "Nothing is selected.");
        }
        session.syncSelectionDefaults();
        Minecraft.getInstance().setScreen(new PcgEditorScreen());
    }

    private void focusSelection() {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 focus = session.getSelectionFocus();
        if (minecraft.player == null || focus == null) {
            session.log(PcgEditorLogEntry.Severity.DEBUG, "Nothing to focus.");
            return;
        }
        minecraft.player.moveTo(focus.x, focus.y + 2.0D, focus.z, minecraft.player.getYRot(), minecraft.player.getXRot());
        session.log(PcgEditorLogEntry.Severity.INFO, "Focused " + session.getSelectionLabel() + ".");
    }

    private void applyTransformEdit() {
        if (transformXBox == null || transformYBox == null || transformZBox == null) {
            return;
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(transformXBox.getValue().trim());
            y = Integer.parseInt(transformYBox.getValue().trim());
            z = Integer.parseInt(transformZBox.getValue().trim());
        } catch (NumberFormatException exception) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Transform fields must be integers.");
            return;
        }

        if (session.getSelection() == PcgEditorSelection.REGION) {
            BoxRegionSelection region = ClientSelectionState.getRegionSelection();
            if (!region.isComplete()) {
                session.log(PcgEditorLogEntry.Severity.ERROR, "Region transform requires a complete Box Region.");
                return;
            }
            int x2 = x + region.getWidth() - 1;
            int y2 = y + region.getHeight() - 1;
            int z2 = z + region.getDepth() - 1;
            runAction("blockwright region set " + x + " " + y + " " + z + " " + x2 + " " + y2 + " " + z2,
                    PcgEditorLogEntry.Severity.INFO, "Moved Box Region_01.");
            session.setSelection(PcgEditorSelection.REGION);
            Minecraft.getInstance().setScreen(new PcgEditorScreen());
            return;
        }

        if (session.getSelection() == PcgEditorSelection.SPLINE_POINT) {
            SplineSelection spline = ClientSelectionState.getSplineSelection();
            int pointIndex = session.getSelectedSplinePointIndex();
            if (pointIndex < 0 || pointIndex >= spline.getPoints().size()) {
                session.log(PcgEditorLogEntry.Severity.ERROR, "No spline point is selected.");
                return;
            }
            List<BlockPos> rebuiltPoints = new ArrayList<>(spline.getPoints());
            rebuiltPoints.set(pointIndex, new BlockPos(x, y, z));
            runAction("blockwright spline clear", PcgEditorLogEntry.Severity.INFO, "Rebuilt spline path.");
            for (BlockPos point : rebuiltPoints) {
                sendCommand("blockwright spline addpos " + point.getX() + " " + point.getY() + " " + point.getZ());
            }
            session.setSelection(PcgEditorSelection.SPLINE_POINT);
            session.setSelectedSplinePointIndex(pointIndex);
            session.log(PcgEditorLogEntry.Severity.INFO, "Moved spline point #" + pointIndex + ".");
            Minecraft.getInstance().setScreen(new PcgEditorScreen());
        }
    }

    private void sendCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }
        ClientSelectionState.captureCommand(command, minecraft.player.blockPosition());
        minecraft.player.connection.sendCommand(command);
    }

    private boolean hasValidInputForPreset(PresetDefinition preset) {
        String input = describePresetMode(preset);
        return isInputCompatible(input);
    }

    private boolean isInputCompatible(String required) {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        if ("Region".equals(required)) {
            return region.isComplete();
        }
        if ("Spline".equals(required)) {
            return spline.getPoints().size() >= 2;
        }
        return false;
    }

    private boolean isInViewport(double mouseX, double mouseY) {
        return mouseX >= viewportX && mouseX <= viewportX + viewportWidth
                && mouseY >= viewportY && mouseY <= viewportY + viewportHeight;
    }

    private void exportRegionAsModule() {
        if (!ClientSelectionState.getRegionSelection().isComplete()) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Export requires a complete Box Region.");
            return;
        }
        String moduleId = sanitizeModuleId(moduleExportIdBox == null ? session.getExportModuleId() : moduleExportIdBox.getValue());
        if (moduleId.isBlank()) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Module id cannot be empty.");
            return;
        }
        session.setExportModuleId(moduleId);
        runAction("blockwright export-schem " + moduleId, PcgEditorLogEntry.Severity.SUCCESS, "Exported region as module " + moduleId + ".");
    }

    private List<ModuleDefinition> getFilteredModules(LoadedPack pack) {
        List<ModuleDefinition> modules = new ArrayList<>();
        if (pack == null) {
            return modules;
        }
        String query = normalize(session.getModuleSearchQuery());
        String tag = normalize(session.getModuleTagQuery());
        String kindFilter = normalize(session.getModuleKindFilter());
        for (ModuleDefinition module : pack.getModules().values()) {
            if (!query.isBlank() && !matchesQuery(module, query)) {
                continue;
            }
            if (!tag.isBlank() && module.tags.stream().noneMatch(existing -> normalize(existing).contains(tag))) {
                continue;
            }
            if (!kindFilter.isBlank() && !"all".equals(kindFilter) && !kindFilter.equals(normalize(module.moduleKind))) {
                continue;
            }
            modules.add(module);
        }
        return modules;
    }

    private boolean matchesQuery(ModuleDefinition module, String query) {
        if (normalize(module.id).contains(query)
                || normalize(module.moduleKind).contains(query)
                || normalize(module.category).contains(query)
                || normalize(module.style).contains(query)
                || normalize(module.schematic).contains(query)) {
            return true;
        }
        for (String tag : module.tags) {
            if (normalize(tag).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private void updateModuleScroll(int moduleCount) {
        if (moduleListTop == 0 || moduleListBottom == 0) {
            return;
        }
        int visibleRows = Math.max(1, (moduleListBottom - moduleListTop) / MODULE_ROW_HEIGHT);
        maxModuleListScroll = Math.max(0, moduleCount - visibleRows);
        moduleListScroll = clamp(moduleListScroll, 0, maxModuleListScroll);
    }

    private List<String> describePosition() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return List.of("X 0 | Y 0 | Z 0", "Chunk 0, 0, 0");
        }
        BlockPos pos = minecraft.player.blockPosition();
        return List.of(
                String.format(Locale.ROOT, "X %.2f | Y %.2f | Z %.2f", minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()),
                "Chunk " + pos.getX() / 16 + ", " + pos.getY() / 16 + ", " + pos.getZ() / 16
        );
    }

    private List<String> describeSelectionStats() {
        if (session.getSelection() == PcgEditorSelection.REGION) {
            BoxRegionSelection region = ClientSelectionState.getRegionSelection();
            return List.of(session.getSelectionLabel(), "Size: " + region.getWidth() + " x " + region.getHeight() + " x " + region.getDepth());
        }
        if (session.getSelection() == PcgEditorSelection.SPLINE || session.getSelection() == PcgEditorSelection.SPLINE_POINT) {
            SplineSelection spline = ClientSelectionState.getSplineSelection();
            return List.of(session.getSelectionLabel(), "Points: " + spline.getPoints().size());
        }
        return List.of(session.getSelectionLabel(), "Tool: " + session.getActiveTool().getTitle());
    }

    private List<String> describePreviewStats() {
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (previewPlan == null) {
            return List.of("Blocks: 0", "State: None");
        }
        int blockEntities = 0;
        for (var block : previewPlan.getPlannedBlocks()) {
            if (block.getBlockEntityTag() != null) {
                blockEntities++;
            }
        }
        return List.of(
                "Blocks: " + previewPlan.getPlannedBlocks().size(),
                "Entities: " + blockEntities + " | " + session.getPreviewState().name()
        );
    }

    private List<String> getWarningSummary() {
        List<String> lines = new ArrayList<>();
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (previewPlan != null) {
            for (PreviewIssue issue : previewPlan.getIssues()) {
                lines.add(issue.getSeverity() + ": " + issue.getMessage());
            }
        }
        LoadedPack pack = session.getSelectedPack();
        if (pack != null) {
            for (ValidationIssue issue : pack.getValidationReport().getIssues()) {
                lines.add(issue.getSeverity() + ": " + issue.getMessage());
            }
        }
        return lines;
    }

    private String describeSelectionType() {
        return switch (session.getSelection()) {
            case REGION -> "Box Region";
            case SPLINE -> "Spline";
            case SPLINE_POINT -> "Spline Point";
            case PREVIEW -> "Structure Preview";
            case MODULE -> "Module";
            case NONE -> "None";
        };
    }

    private String describeSelectionInput() {
        return switch (session.getSelection()) {
            case REGION -> "Box Region";
            case SPLINE, SPLINE_POINT -> "Spline";
            default -> "None";
        };
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

    private boolean needsBakeConfirmation(PreviewPlan previewPlan) {
        int blockThreshold = Math.max(2048, BlockwrightConfig.get().maxBakeBlocks / 4);
        return previewPlan.getPlannedBlocks().size() >= blockThreshold
                || countPreviewChunks(previewPlan) >= 8
                || countPreviewBlockEntities(previewPlan) > 0;
    }

    private int countPreviewChunks(PreviewPlan previewPlan) {
        Set<Long> chunkKeys = new HashSet<>();
        for (var block : previewPlan.getPlannedBlocks()) {
            long chunkKey = (((long) block.getPos().getX() >> 4) << 32) ^ (((long) block.getPos().getZ() >> 4) & 0xFFFFFFFFL);
            chunkKeys.add(chunkKey);
        }
        return chunkKeys.size();
    }

    private int countPreviewBlockEntities(PreviewPlan previewPlan) {
        int count = 0;
        for (var block : previewPlan.getPlannedBlocks()) {
            if (block.getBlockEntityTag() != null) {
                count++;
            }
        }
        return count;
    }

    private String moduleKindLabel() {
        return "Kind: " + safe(session.getModuleKindFilter());
    }

    private int getModulePreviewPanelX() {
        return rightPanelX + RIGHT_PANEL_WIDTH - 244;
    }

    private int getModulePreviewPanelY() {
        return viewportY + 248;
    }

    private int getModulePreviewPanelWidth() {
        return 228;
    }

    private int getModulePreviewPanelHeight() {
        return 250;
    }

    private String sanitizeModuleId(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String trimmed = rawValue.trim().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char value = trimmed.charAt(i);
            if ((value >= 'a' && value <= 'z') || (value >= '0' && value <= '9') || value == '_' || value == '.' || value == '-') {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private String toggleLabel(String title, boolean enabled) {
        return title + ": " + (enabled ? "On" : "Off");
    }

    private int colorForPreviewState(PcgEditorPreviewState state) {
        return switch (state) {
            case VALID -> TEXT_GREEN;
            case WARNING -> TEXT_YELLOW;
            case ERROR -> TEXT_RED;
            case DIRTY -> TEXT_MAGENTA;
            case GENERATING -> TEXT_BLUE;
            default -> TEXT_MUTED;
        };
    }

    private int logColor(PcgEditorLogEntry entry) {
        return switch (entry.getSeverity()) {
            case SUCCESS -> TEXT_GREEN;
            case WARNING -> TEXT_YELLOW;
            case ERROR -> TEXT_RED;
            case DEBUG -> TEXT_BLUE;
            default -> TEXT_MUTED;
        };
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(x, y, x + width, y + height, PANEL_BG);
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        guiGraphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String trimToWidth(String text, int width) {
        return this.font.plainSubstrByWidth(text == null ? "" : text, Math.max(20, width));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "<none>";
        }
        return String.join(", ", values);
    }

    private static String joinInts(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "<none>";
        }
        List<String> parts = new ArrayList<>();
        for (Integer value : values) {
            parts.add(String.valueOf(value));
        }
        return String.join(" x ", parts);
    }

    private static String onOff(boolean enabled) {
        return enabled ? "On" : "Off";
    }
}
