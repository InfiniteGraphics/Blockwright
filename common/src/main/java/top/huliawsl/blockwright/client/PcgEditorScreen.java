package top.huliawsl.blockwright.client;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.config.BlockwrightConfig;
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
import java.util.function.Consumer;

public final class PcgEditorScreen extends Screen {
    private static final double GIZMO_AXIS_LENGTH = 2.0D;
    private static final double GIZMO_PICK_RADIUS = 12.0D;
    private static final double GIZMO_MIN_AXIS_SCREEN_LENGTH = 10.0D;
    private static long horizontalResizeCursor;
    private static long verticalResizeCursor;
    private static final int ROOT_BG = 0x00000000;
    private static final int PANEL_BG = 0xD610141B;
    private static final int PANEL_BORDER = 0xFF2E3640;
    private static final int PANEL_HEADER = 0xEA1D232C;
    private static final int PANEL_ACCENT = 0xEE273B52;
    private static final int PANEL_ACCENT_SOFT = 0x88263C57;
    private static final int BUTTON_BG = 0xFF2C3138;
    private static final int BUTTON_HOVER = 0xFF3A434E;
    private static final int BUTTON_ACTIVE = 0xFF27527F;
    private static final int BUTTON_DANGER = 0xFF6B3535;
    private static final int FIELD_BG = 0xFF0B0E12;
    private static final int FIELD_BORDER = 0xFF6A737E;
    private static final int TEXT_BRIGHT = 0xFFF3F6FA;
    private static final int TEXT_MUTED = 0xFF95A0AE;
    private static final int TEXT_GREEN = 0xFF74D888;
    private static final int TEXT_YELLOW = 0xFFF3C654;
    private static final int TEXT_RED = 0xFFF07171;
    private static final int TEXT_BLUE = 0xFF68B7FF;
    private static final int TEXT_MAGENTA = 0xFFD786FF;

    private static final int OUTER_PAD = 0;

    private final PcgEditorSession session = PcgEditorSession.get();
    private final List<EditorButton> buttons = new ArrayList<>();
    private final List<EditorField> fields = new ArrayList<>();
    private final List<ParameterField> parameterFields = new ArrayList<>();
    private final Map<String, String> parameterOverrides = new LinkedHashMap<>();
    private final List<ModuleDefinition> visibleModules = new ArrayList<>();

    private LayoutRect topBar;
    private LayoutRect leftBar;
    private LayoutRect viewport;
    private LayoutRect workspace;
    private LayoutRect rightPanel;
    private LayoutRect detailsPanel;
    private LayoutRect previewDockPanel;
    private LayoutRect bottomBar;
    private LayoutRect parameterViewport;
    private LayoutRect moduleListViewport;
    private LayoutRect modulePreviewPanel;
    private LayoutRect messageLogRect;
    private LayoutRect inspectorBodyRect;
    private LayoutRect detailsSplitter;
    private LayoutRect previewSplitter;
    private LayoutRect bottomSplitter;
    private EditorButton previewButton;
    private EditorButton regenerateButton;
    private EditorButton bakeButton;
    private EditorButton undoButton;
    private EditorButton redoButton;
    private EditorButton reloadButton;
    private EditorButton exitButton;
    private EditorButton moduleKindButton;
    private EditorButton moduleRotateButton;
    private EditorButton moduleZoomOutButton;
    private EditorButton moduleZoomInButton;
    private EditorButton moduleBoundsButton;
    private EditorButton moduleConnectorsButton;
    private EditorButton moduleAirButton;
    private EditorButton moduleExportButton;
    private EditorButton moduleModStatusButton;
    private EditorButton focusButton;
    private EditorButton deleteButton;
    private EditorButton clearPreviewButton;
    private EditorButton transformApplyButton;

    private EditorField focusedField;
    private EditorField transformXField;
    private EditorField transformYField;
    private EditorField transformZField;
    private EditorField moduleStyleField;
    private EditorField moduleCategoryField;
    private EditorField moduleExportField;

    private int inspectorScroll;
    private int maxInspectorScroll;
    private int moduleListScroll;
    private int maxModuleListScroll;
    private int logScroll;
    private int maxLogScroll;
    private int uiWidth;
    private int uiHeight;
    private PcgUiMetrics uiMetrics;
    private double uiScale = 1.0D;
    private double uiOffsetX;
    private double uiOffsetY;
    private double navigationMouseX;
    private double navigationMouseY;
    private Vec3 navigationVelocity = Vec3.ZERO;
    private long navigationLastNanos;
    private boolean navForward;
    private boolean navBack;
    private boolean navLeft;
    private boolean navRight;
    private boolean navUp;
    private boolean navDown;
    private boolean navFast;
    private int bottomBarHeight = -1;
    private int detailsPanelWidth = -1;
    private int previewPanelWidth = -1;
    private DragHandle activeDragHandle = DragHandle.NONE;
    private CursorType activeCursorType = CursorType.DEFAULT;
    private PcgEditorAxis activeGizmoAxis = PcgEditorAxis.NONE;
    private BlockPos gizmoDragStartPoint;
    private BlockPos gizmoDragAppliedPoint;
    private double gizmoDragStartMouseX;
    private double gizmoDragStartMouseY;
    private double gizmoDragAxisUnitX;
    private double gizmoDragAxisUnitY;
    private double gizmoDragWorldPerPixel;

    private boolean showBakeConfirm;
    private String uiSignature = "";

    public PcgEditorScreen() {
        super(Component.literal("PCG Editor"));
    }

    @Override
    protected void init() {
        session.open();
        session.ensureDefaults();
        session.syncSelectionDefaults();
        rebuildUi();
    }

    @Override
    public void added() {
        session.enterCameraMode(Minecraft.getInstance());
    }

    @Override
    public void tick() {
        session.syncSelectionDefaults();
        String signature = buildUiSignature();
        if (!signature.equals(uiSignature)) {
            rebuildUi();
        } else {
            updateActionStates();
            layoutParameterFields();
            clampLogScroll();
            refreshVisibleModules();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (session.isNavigating()) {
            tickNavigationMovement();
        }
        updateHoverPlacement(mouseX, mouseY);
        guiGraphics.fill(0, 0, this.width, this.height, ROOT_BG);
        if (showBakeConfirm) {
            guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);
        }
        int uiMouseX = (int) Math.round(toUiX(mouseX));
        int uiMouseY = (int) Math.round(toUiY(mouseY));
        beginUi(guiGraphics);
        drawTopBar(guiGraphics, uiMouseX, uiMouseY);
        drawLeftBar(guiGraphics, uiMouseX, uiMouseY);
        drawViewport(guiGraphics);
        drawRightPanel(guiGraphics, uiMouseX, uiMouseY);
        drawBottomBar(guiGraphics);
        drawInteractiveElements(guiGraphics, uiMouseX, uiMouseY);
        if (showBakeConfirm) {
            drawBakeConfirm(guiGraphics, uiMouseX, uiMouseY);
        }
        endUi(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double uiMouseX = toUiX(mouseX);
        double uiMouseY = toUiY(mouseY);
        if (showBakeConfirm) {
            return handleConfirmClick(uiMouseX, uiMouseY, button);
        }
        if (button == 0 && beginLayoutDrag(uiMouseX, uiMouseY)) {
            return true;
        }
        if (button == 1 && viewport.contains(uiMouseX, uiMouseY)) {
            startNavigation(mouseX, mouseY);
            return true;
        }
        if (button != 0) {
            return false;
        }

        if (handleButtonClick(uiMouseX, uiMouseY)) {
            return true;
        }
        if (handleFieldClick(uiMouseX, uiMouseY)) {
            return true;
        }
        clearFocus();
        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY && moduleListViewport != null && moduleListViewport.contains(uiMouseX, uiMouseY)) {
            return selectModuleFromList(uiMouseY);
        }
        if (viewport.contains(uiMouseX, uiMouseY) && !session.isNavigating()) {
            if (beginGizmoDrag(mouseX, mouseY)) {
                return true;
            }
            return handleViewportClick(mouseX, mouseY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && activeGizmoAxis != PcgEditorAxis.NONE) {
            finishGizmoDrag();
            return true;
        }
        if (button == 0 && activeDragHandle != DragHandle.NONE) {
            activeDragHandle = DragHandle.NONE;
            updateCursorForPosition(toUiX(mouseX), toUiY(mouseY));
            return true;
        }
        if (button == 1 && session.isNavigating()) {
            stopNavigation();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && activeGizmoAxis != PcgEditorAxis.NONE) {
            updateGizmoDrag(mouseX, mouseY);
            return true;
        }
        if (button == 0 && activeDragHandle != DragHandle.NONE) {
            handleLayoutDrag(toUiX(mouseX), toUiY(mouseY));
            return true;
        }
        if (button == 1 && session.isNavigating()) {
            updateNavigationLook(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (session.isNavigating()) {
            updateNavigationLook(mouseX, mouseY);
        } else {
            updateCursorForPosition(toUiX(mouseX), toUiY(mouseY));
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double uiMouseX = toUiX(mouseX);
        double uiMouseY = toUiY(mouseY);
        if (showBakeConfirm) {
            return true;
        }
        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY && moduleListViewport != null && moduleListViewport.contains(uiMouseX, uiMouseY)) {
            moduleListScroll = clamp(moduleListScroll - (int) Math.signum(delta), 0, maxModuleListScroll);
            return true;
        }
        if (session.getActiveTool() != PcgEditorTool.MODULE_LIBRARY && inspectorBodyRect != null && inspectorBodyRect.contains(uiMouseX, uiMouseY)) {
            inspectorScroll = clamp(inspectorScroll - (int) (delta * 18.0D), 0, maxInspectorScroll);
            rebuildUi();
            return true;
        }
        if (messageLogRect != null && messageLogRect.contains(uiMouseX, uiMouseY)) {
            logScroll = clamp(logScroll - (int) Math.signum(delta), 0, maxLogScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showBakeConfirm) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                showBakeConfirm = false;
                session.log(PcgEditorLogEntry.Severity.INFO, "Bake cancelled.");
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirmBake();
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_G) {
            onClose();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (session.isNavigating()) {
                stopNavigation();
            } else if (focusedField != null) {
                clearFocus();
            } else {
                onClose();
            }
            return true;
        }

        if (handleNavigationKey(keyCode, scanCode, true)) {
            return true;
        }

        if (focusedField != null && focusedField.keyPressed(keyCode, modifiers)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_P) {
            generatePreview(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            generatePreview(true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_B) {
            requestBake();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (session.getActiveTool() == PcgEditorTool.SPLINE) {
                session.setSelection(PcgEditorSelection.SPLINE);
                session.log(PcgEditorLogEntry.Severity.INFO, "Spline input confirmed.");
                return true;
            }
            if (transformApplyButton != null && isTransformSelection()) {
                applyTransformEdit();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_F) {
            focusSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            deleteSelection();
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_D) {
            clearAllSelections();
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            runAction("blockwright undo", PcgEditorLogEntry.Severity.INFO, "Undo requested.");
            return true;
        }
        if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
            session.log(PcgEditorLogEntry.Severity.DEBUG, "Redo is not implemented in this MVP.");
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (handleNavigationKey(keyCode, scanCode, false)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (showBakeConfirm) {
            return true;
        }
        return focusedField != null && focusedField.charTyped(codePoint);
    }

    @Override
    public void removed() {
        cancelGizmoDrag();
        stopNavigation();
        BlockwrightClient.closeEditor(Minecraft.getInstance(), true);
        setCursor(CursorType.DEFAULT);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        cancelGizmoDrag();
        stopNavigation();
        BlockwrightClient.suppressNextEditorToggle();
        BlockwrightClient.closeEditor(Minecraft.getInstance(), true);
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildUi() {
        String focusedId = focusedField == null ? null : focusedField.id;
        buttons.clear();
        fields.clear();
        parameterFields.clear();
        previewButton = null;
        regenerateButton = null;
        bakeButton = null;
        undoButton = null;
        redoButton = null;
        reloadButton = null;
        exitButton = null;
        moduleKindButton = null;
        moduleRotateButton = null;
        moduleZoomOutButton = null;
        moduleZoomInButton = null;
        moduleBoundsButton = null;
        moduleConnectorsButton = null;
        moduleAirButton = null;
        moduleExportButton = null;
        moduleModStatusButton = null;
        focusButton = null;
        deleteButton = null;
        clearPreviewButton = null;
        transformApplyButton = null;
        transformXField = null;
        transformYField = null;
        transformZField = null;
        moduleStyleField = null;
        moduleCategoryField = null;
        moduleExportField = null;
        focusedField = null;

        updateUiMetrics();
        layoutRoot();
        buildTopToolbar();
        buildToolPalette();
        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY) {
            buildModuleLibraryControls();
        } else {
            buildInspectorControls();
        }
        buildModulePreviewControls();
        restoreFocus(focusedId);
        updateActionStates();
        refreshVisibleModules();
        layoutParameterFields();
        clampLogScroll();
        uiSignature = buildUiSignature();
    }

    private void layoutRoot() {
        int rootWidth = uiWidth - OUTER_PAD * 2;
        topBar = new LayoutRect(OUTER_PAD, OUTER_PAD, rootWidth, uiMetrics.topBarHeight);
        if (bottomBarHeight < 0) {
            bottomBarHeight = uiMetrics.defaultBottomBarHeight;
        }
        bottomBarHeight = clamp(bottomBarHeight, uiMetrics.minBottomBarHeight, uiMetrics.maxBottomBarHeight);
        bottomBar = new LayoutRect(OUTER_PAD, uiHeight - OUTER_PAD - bottomBarHeight, rootWidth, bottomBarHeight);
        int actualLeftWidth = clamp(uiWidth / 14, uiMetrics.leftBarMinWidth, uiMetrics.leftBarMaxWidth);
        leftBar = new LayoutRect(OUTER_PAD, topBar.bottom() + uiMetrics.gap, actualLeftWidth, bottomBar.y - uiMetrics.gap - (topBar.bottom() + uiMetrics.gap));
        int contentTop = topBar.bottom() + uiMetrics.gap;
        int contentHeight = bottomBar.y - uiMetrics.gap - contentTop;
        int defaultPreviewWidth = clamp(uiWidth / 7, uiMetrics.minPreviewWidth, Math.min(uiMetrics.maxPreviewWidth, uiMetrics.unit * 20));
        int previewWidth = previewPanelWidth > 0 ? previewPanelWidth : defaultPreviewWidth;
        previewWidth = clamp(previewWidth, uiMetrics.minPreviewWidth, uiMetrics.maxPreviewWidth);
        previewDockPanel = new LayoutRect(uiWidth - OUTER_PAD - previewWidth, contentTop, previewWidth, contentHeight);
        int workspaceLeft = leftBar.right() + uiMetrics.gap;
        int workspaceRight = previewDockPanel.x - uiMetrics.gap;
        workspace = new LayoutRect(workspaceLeft, contentTop, workspaceRight - workspaceLeft, contentHeight);
        viewport = workspace;
        int defaultDetailsWidth = clamp(workspace.width / 4, uiMetrics.minDetailsWidth, Math.max(uiMetrics.minDetailsWidth, workspace.width - uiMetrics.unit * 18));
        int overlayDetailsWidth = detailsPanelWidth > 0 ? detailsPanelWidth : defaultDetailsWidth;
        int maxDetailsWidth = Math.max(uiMetrics.minDetailsWidth, workspace.width - uiMetrics.unit * 12);
        overlayDetailsWidth = clamp(overlayDetailsWidth, uiMetrics.minDetailsWidth, maxDetailsWidth);
        detailsPanel = new LayoutRect(workspace.right() - overlayDetailsWidth, workspace.y, overlayDetailsWidth, workspace.height);
        rightPanel = new LayoutRect(detailsPanel.x, detailsPanel.y, previewDockPanel.right() - detailsPanel.x, detailsPanel.height);
        modulePreviewPanel = new LayoutRect(previewDockPanel.x, previewDockPanel.y, previewDockPanel.width, previewDockPanel.height);
        inspectorBodyRect = new LayoutRect(detailsPanel.x + uiMetrics.inset, detailsPanel.y + uiMetrics.unit * 2 + uiMetrics.gap,
                detailsPanel.width - uiMetrics.inset * 2,
                Math.max(120, detailsPanel.height - 44));
        detailsSplitter = new LayoutRect(detailsPanel.x - uiMetrics.splitterSize / 2, workspace.y, uiMetrics.splitterSize, workspace.height);
        previewSplitter = new LayoutRect(previewDockPanel.x - uiMetrics.splitterSize / 2, workspace.y, uiMetrics.splitterSize, workspace.height);
        bottomSplitter = new LayoutRect(leftBar.right() + uiMetrics.gap, bottomBar.y - uiMetrics.splitterSize / 2,
                uiWidth - (leftBar.right() + uiMetrics.gap), uiMetrics.splitterSize);
        parameterViewport = null;
        moduleListViewport = null;
        messageLogRect = null;
    }

    private void buildTopToolbar() {
        PcgTopBarLayoutSpec layout = computeTopBarLayout();
        int x = layout.actionStart();
        previewButton = addButton("preview", x, layout.buttonY(), layout.previewWidth(), uiMetrics.topButtonHeight, layout.previewLabel(), false, false, () -> generatePreview(false));
        regenerateButton = addButton("regenerate", x + layout.previewWidth() + layout.actionGap(), layout.buttonY(), layout.regenerateWidth(), uiMetrics.topButtonHeight,
                layout.regenerateLabel(), false, false, () -> generatePreview(true));
        bakeButton = addButton("bake", x + layout.previewWidth() + layout.regenerateWidth() + layout.actionGap() * 2, layout.buttonY(), layout.smallWidth(),
                uiMetrics.topButtonHeight, "Bake", false, false, this::requestBake);
        undoButton = addButton("undo", x + layout.previewWidth() + layout.regenerateWidth() + layout.smallWidth() + layout.actionGap() * 3,
                layout.buttonY(), layout.smallWidth(), uiMetrics.topButtonHeight, "Undo", false, false,
                () -> runAction("blockwright undo", PcgEditorLogEntry.Severity.INFO, "Undo requested."));
        redoButton = addButton("redo", x + layout.previewWidth() + layout.regenerateWidth() + layout.smallWidth() * 2 + layout.actionGap() * 4,
                layout.buttonY(), layout.smallWidth(), uiMetrics.topButtonHeight, "Redo", false, false,
                () -> session.log(PcgEditorLogEntry.Severity.DEBUG, "Redo is not implemented in this MVP."));
        reloadButton = addButton("reload", x + layout.previewWidth() + layout.regenerateWidth() + layout.smallWidth() * 3 + layout.actionGap() * 5,
                layout.buttonY(), layout.smallWidth(), uiMetrics.topButtonHeight, "Reload", false, false, this::reloadPacks);
        exitButton = addButton("exit", x + layout.previewWidth() + layout.regenerateWidth() + layout.smallWidth() * 4 + layout.actionGap() * 6,
                layout.buttonY(), layout.exitWidth(), uiMetrics.topButtonHeight, "Exit", false, false, this::onClose);

        addButton("pack_prev", layout.packX(), layout.buttonY(), uiMetrics.unit * 2, uiMetrics.topButtonHeight, "<", false, false, () -> cyclePack(-1));
        addButton("pack_next", layout.packX() + layout.clusterWidth() - uiMetrics.unit * 2, layout.buttonY(), uiMetrics.unit * 2, uiMetrics.topButtonHeight, ">", false, false, () -> cyclePack(1));
        addButton("preset_prev", layout.presetX(), layout.buttonY(), uiMetrics.unit * 2, uiMetrics.topButtonHeight, "<", false, false, () -> cyclePreset(-1));
        addButton("preset_next", layout.presetX() + layout.clusterWidth() - uiMetrics.unit * 2, layout.buttonY(), uiMetrics.unit * 2, uiMetrics.topButtonHeight, ">", false, false, () -> cyclePreset(1));
    }

    private void buildToolPalette() {
        int buttonWidth = leftBar.width - uiMetrics.inset * 2;
        int x = leftBar.x + uiMetrics.inset;
        int y = leftBar.y + uiMetrics.inset;
        int gap = uiMetrics.gap * 3;
        int toolHeight = computeToolButtonHeight();
        for (PcgEditorTool tool : PcgEditorTool.values()) {
            if (!shouldShowToolButton(tool)) {
                continue;
            }
            boolean disabled = tool == PcgEditorTool.PAINT_MASK;
            boolean selected = tool == session.getActiveTool();
            addButton("tool_" + tool.name().toLowerCase(Locale.ROOT), x, y, buttonWidth, toolHeight,
                    toolLabel(tool), selected, disabled, () -> switchTool(tool));
            y += toolHeight + gap;
        }
    }

    private void buildInspectorControls() {
        int innerX = detailsPanel.x + uiMetrics.inset;
        int innerWidth = detailsPanel.width - uiMetrics.inset * 2;
        maxInspectorScroll = Math.max(0, measureInspectorContentHeight() - (inspectorBodyRect.height - 12));
        inspectorScroll = clamp(inspectorScroll, 0, maxInspectorScroll);
        int y = inspectorBodyRect.y + uiMetrics.inset - inspectorScroll;

        y += uiMetrics.unit * 2 + uiMetrics.rowHeight;
        y += uiMetrics.unit * 2;
        if (isTransformSelection()) {
            int fieldGap = uiMetrics.gap * 4;
            int fieldWidth = clamp((innerWidth - 42 - fieldGap * 2) / 3, uiMetrics.unit * 5, uiMetrics.unit * 7);
            int fieldY = y + uiMetrics.unit + 2;
            transformXField = addField("transform_x", innerX + 42, fieldY, fieldWidth, uiMetrics.fieldHeight,
                    String.valueOf(currentTransformOrigin().getX()), true, text -> {
            });
            transformYField = addField("transform_y", innerX + 42 + fieldWidth + fieldGap, fieldY, fieldWidth, uiMetrics.fieldHeight,
                    String.valueOf(currentTransformOrigin().getY()), true, text -> {
            });
            transformZField = addField("transform_z", innerX + 42 + (fieldWidth + fieldGap) * 2, fieldY, fieldWidth, uiMetrics.fieldHeight,
                    String.valueOf(currentTransformOrigin().getZ()), true, text -> {
            });
            transformApplyButton = addButton("transform_apply", innerX + innerWidth - uiMetrics.unit * 7, fieldY, uiMetrics.unit * 7, uiMetrics.topButtonHeight,
                    "Apply", false, false, this::applyTransformEdit);
            applyClipVisibility(transformXField, inspectorBodyRect);
            applyClipVisibility(transformYField, inspectorBodyRect);
            applyClipVisibility(transformZField, inspectorBodyRect);
            applyClipVisibility(transformApplyButton, inspectorBodyRect);
        }

        y += transformSectionHeight();
        y += uiMetrics.unit * 2 + uiMetrics.rowHeight;
        y += uiMetrics.unit * 2 + uiMetrics.rowHeight;
        y += uiMetrics.unit * 2;
        PresetDefinition preset = session.getSelectedPreset();
        parameterViewport = new LayoutRect(innerX, y, innerWidth, parameterRowsHeight());
        if (preset != null) {
            int labelWidth = Math.min(uiMetrics.unit * 10, Math.max(uiMetrics.unit * 7, innerWidth / 3));
            int fieldX = innerX + labelWidth + uiMetrics.gap * 4;
            int fieldWidth = innerWidth - labelWidth - uiMetrics.gap * 4;
            int rowIndex = 0;
            for (Map.Entry<String, PresetParameterDefinition> entry : preset.parameters.entrySet()) {
                if (!entry.getValue().exposed) {
                    continue;
                }
                String value = parameterOverrides.containsKey(entry.getKey())
                        ? parameterOverrides.get(entry.getKey())
                        : entry.getValue().defaultValue == null ? "" : entry.getValue().defaultValue.getAsString();
                parameterOverrides.putIfAbsent(entry.getKey(), value);
                EditorField field = addField("param_" + entry.getKey(), fieldX, 0, fieldWidth, uiMetrics.fieldHeight, value, false, text -> {
                    parameterOverrides.put(entry.getKey(), text);
                    session.markDirty("Parameter changed: " + entry.getKey());
                });
                parameterFields.add(new ParameterField(entry.getKey(), rowIndex, field));
                rowIndex++;
            }
        }

        y += parameterRowsHeight();
        y += uiMetrics.unit * 2 + validationContentHeight();
        y += uiMetrics.unit * 2;
        int actionsTop = y;
        int actionGap = uiMetrics.gap * 4;
        int focusWidth = Math.max(uiMetrics.unit * 6, (innerWidth - actionGap * 2 - uiMetrics.unit * 9) / 2);
        int deleteWidth = focusWidth;
        int clearWidth = innerWidth - focusWidth - deleteWidth - actionGap * 2;
        focusButton = addButton("focus", innerX, actionsTop, focusWidth, uiMetrics.topButtonHeight, "Focus", false, false, this::focusSelection);
        deleteButton = addButton("delete", innerX + focusWidth + actionGap, actionsTop, deleteWidth, uiMetrics.topButtonHeight, "Delete", false, true, this::deleteSelection);
        clearPreviewButton = addButton("clear_preview", innerX + focusWidth + deleteWidth + actionGap * 2, actionsTop, clearWidth, uiMetrics.topButtonHeight, "Clear Preview", false, false,
                () -> runAction("blockwright preview clear", PcgEditorLogEntry.Severity.INFO, "Cleared preview."));
        applyClipVisibility(focusButton, inspectorBodyRect);
        applyClipVisibility(deleteButton, inspectorBodyRect);
        applyClipVisibility(clearPreviewButton, inspectorBodyRect);
    }

    private void buildModuleLibraryControls() {
        int innerX = detailsPanel.x + uiMetrics.inset;
        int innerWidth = detailsPanel.width - uiMetrics.inset * 2;
        int y = inspectorBodyRect.y + uiMetrics.inset;
        addField("module_search", innerX, y + uiMetrics.unit + 2, innerWidth, uiMetrics.fieldHeight,
                session.getModuleSearchQuery(), false, value -> {
                    session.setModuleSearchQuery(value);
                    refreshVisibleModules();
                });
        addField("module_tag", innerX, y + uiMetrics.rowHeight + uiMetrics.unit, innerWidth, uiMetrics.fieldHeight,
                session.getModuleTagQuery(), false, value -> {
                    session.setModuleTagQuery(value);
                    refreshVisibleModules();
                });
        moduleKindButton = addButton("module_kind", innerX, y + uiMetrics.rowHeight * 2, innerWidth, uiMetrics.topButtonHeight, moduleKindLabel(),
                false, false, () -> {
                    session.cycleModuleKindFilter();
                    refreshVisibleModules();
                    rebuildUi();
                });
        moduleStyleField = addField("module_style", innerX, y + uiMetrics.rowHeight * 3, innerWidth, uiMetrics.fieldHeight,
                session.getModuleStyleQuery(), false, value -> {
                    session.setModuleStyleQuery(value);
                    refreshVisibleModules();
                });
        moduleCategoryField = addField("module_category", innerX, y + uiMetrics.rowHeight * 4, innerWidth, uiMetrics.fieldHeight,
                session.getModuleCategoryQuery(), false, value -> {
                    session.setModuleCategoryQuery(value);
                    refreshVisibleModules();
                });
        moduleModStatusButton = addButton("module_mod_status", innerX, y + uiMetrics.rowHeight * 5, innerWidth, uiMetrics.topButtonHeight, moduleModStatusLabel(),
                false, false, () -> {
                    session.cycleModuleModStatusFilter();
                    refreshVisibleModules();
                    rebuildUi();
                });
        moduleExportField = addField("module_export", innerX, y + uiMetrics.rowHeight * 6, innerWidth, uiMetrics.fieldHeight,
                session.getExportModuleId(), false, session::setExportModuleId);
        moduleExportButton = addButton("module_export_button", innerX, y + uiMetrics.rowHeight * 7, innerWidth, uiMetrics.topButtonHeight, "Export Region",
                false, false, this::exportRegionAsModule);

        int listTop = y + uiMetrics.rowHeight * 8 + uiMetrics.gap * 2;
        int listBottom = inspectorBodyRect.bottom() - uiMetrics.inset;
        moduleListViewport = new LayoutRect(innerX, listTop, innerWidth, Math.max(96, listBottom - listTop));
        refreshVisibleModules();
    }

    private void buildModulePreviewControls() {
        int x = modulePreviewPanel.x + uiMetrics.inset;
        int width = modulePreviewPanel.width - uiMetrics.inset * 2;
        int buttonY = modulePreviewPanel.bottom() - uiMetrics.unit * 5;
        int buttonWidth = (width - uiMetrics.gap * 8) / 3;
        moduleRotateButton = addButton("module_rotate", x, buttonY, buttonWidth, uiMetrics.topButtonHeight, "Rotate", false, false,
                () -> {
                    session.rotateModulePreview();
                    rebuildUi();
                });
        moduleZoomOutButton = addButton("module_zoom_out", x + buttonWidth + uiMetrics.gap * 4, buttonY, buttonWidth, uiMetrics.topButtonHeight, "Zoom-", false, false,
                () -> {
                    session.zoomModulePreview(-25);
                    rebuildUi();
                });
        moduleZoomInButton = addButton("module_zoom_in", x + (buttonWidth + uiMetrics.gap * 4) * 2, buttonY, buttonWidth, uiMetrics.topButtonHeight, "Zoom+", false, false,
                () -> {
                    session.zoomModulePreview(25);
                    rebuildUi();
                });
        int toggleY = modulePreviewPanel.bottom() - uiMetrics.topButtonHeight - uiMetrics.gap * 2;
        moduleBoundsButton = addButton("module_bounds", x, toggleY, buttonWidth, uiMetrics.topButtonHeight, toggleLabel("Bounds", session.isShowModuleBounds()),
                false, false, () -> {
                    session.setShowModuleBounds(!session.isShowModuleBounds());
                    rebuildUi();
                });
        moduleConnectorsButton = addButton("module_connectors", x + buttonWidth + uiMetrics.gap * 4, toggleY, buttonWidth, uiMetrics.topButtonHeight,
                toggleLabel("Conn", session.isShowModuleConnectors()), false, false, () -> {
                    session.setShowModuleConnectors(!session.isShowModuleConnectors());
                    rebuildUi();
                });
        moduleAirButton = addButton("module_air", x + (buttonWidth + uiMetrics.gap * 4) * 2, toggleY, buttonWidth, uiMetrics.topButtonHeight, toggleLabel("Air", session.isShowModuleAir()),
                false, false, () -> {
                    session.setShowModuleAir(!session.isShowModuleAir());
                    rebuildUi();
                });
    }

    private void drawTopBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawPanel(guiGraphics, topBar, true);
        guiGraphics.fill(topBar.x + uiMetrics.inset - 2, topBar.y + uiMetrics.gap * 4, topBar.x + uiMetrics.inset + uiMetrics.unit * 2, topBar.y + topBar.height - uiMetrics.gap * 4, PANEL_ACCENT);
        int textY = topBar.y + (topBar.height - 8) / 2;
        guiGraphics.drawString(this.font, "BW", topBar.x + 15, textY, TEXT_BRIGHT);
        PcgTopBarLayoutSpec layout = computeTopBarLayout();
        guiGraphics.drawString(this.font, layout.title(), topBar.x + 46, textY, TEXT_BRIGHT);
        int chipY = topBar.y + (topBar.height - (uiMetrics.topButtonHeight + 4)) / 2;
        LayoutRect packRect = new LayoutRect(layout.packX() + uiMetrics.unit * 2, chipY, layout.clusterWidth() - uiMetrics.unit * 4, uiMetrics.topButtonHeight + 4);
        LayoutRect modeRect = new LayoutRect(layout.modeX(), chipY, layout.modeWidth(), uiMetrics.topButtonHeight + 4);
        LayoutRect presetRect = new LayoutRect(layout.presetX() + uiMetrics.unit * 2, chipY, layout.clusterWidth() - uiMetrics.unit * 4, uiMetrics.topButtonHeight + 4);
        drawToolbarChip(guiGraphics, modeRect, "MODE", "Build", TEXT_GREEN);
        drawToolbarChip(guiGraphics, packRect, "PACK", session.getSelectedPack() == null ? "<none>" : session.getSelectedPack().getMetadata().id, TEXT_BRIGHT);
        drawToolbarChip(guiGraphics, presetRect, "PRESET", session.getSelectedPreset() == null ? "<none>" : session.getSelectedPreset().id, TEXT_BRIGHT);
    }

    private void drawLeftBar(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawPanel(guiGraphics, leftBar, false);
    }

    private void drawViewport(GuiGraphics guiGraphics) {
        drawViewportFrame(guiGraphics);
        guiGraphics.fill(viewport.x + 18, viewport.y + 18, viewport.x + 210, viewport.y + 80, 0xB1161C24);
        guiGraphics.drawString(this.font, "Hold RMB to navigate,", viewport.x + 30, viewport.y + 30, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "WASD to fly,", viewport.x + 30, viewport.y + 44, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "LMB to edit.", viewport.x + 30, viewport.y + 58, TEXT_BRIGHT);

        guiGraphics.fill(viewport.x + 18, viewport.bottom() - 100, viewport.x + 190, viewport.bottom() - 18, 0xB1161C24);
        guiGraphics.drawString(this.font, "VIEW", viewport.x + 30, viewport.bottom() - 88, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Editor Overlay", viewport.x + 30, viewport.bottom() - 74, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "SNAP", viewport.x + 30, viewport.bottom() - 54, TEXT_MUTED);
        guiGraphics.drawString(this.font, session.getSnapStep() + " blocks", viewport.x + 30, viewport.bottom() - 40, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Tool: " + session.getActiveTool().getTitle(), viewport.x + 30, viewport.bottom() - 22, TEXT_MUTED);

        PcgEditorPreviewState previewState = session.getPreviewState();
        if (previewState == PcgEditorPreviewState.DIRTY || previewState == PcgEditorPreviewState.ERROR) {
            int badgeColor = previewState == PcgEditorPreviewState.ERROR ? 0xB03B161B : 0xB03B3015;
            int textColor = previewState == PcgEditorPreviewState.ERROR ? TEXT_RED : TEXT_YELLOW;
            String label = previewState == PcgEditorPreviewState.ERROR ? "Preview Error" : "Outdated Preview";
            guiGraphics.fill(viewport.right() - 164, viewport.y + 18, viewport.right() - 18, viewport.y + 44, badgeColor);
            guiGraphics.drawString(this.font, label, viewport.right() - 150, viewport.y + 27, textColor);
        }
    }

    private void drawRightPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawPanel(guiGraphics, detailsPanel, false);
        drawPanel(guiGraphics, previewDockPanel, false);
        if (detailsSplitter != null) {
            drawLayoutHandle(guiGraphics, detailsSplitter, true);
        }
        if (previewSplitter != null) {
            drawLayoutHandle(guiGraphics, previewSplitter, true);
        }
        guiGraphics.drawString(this.font, "DETAILS", detailsPanel.x + uiMetrics.inset, detailsPanel.y + uiMetrics.inset, TEXT_BRIGHT);
        if (session.getActiveTool() == PcgEditorTool.MODULE_LIBRARY) {
            drawModuleLibrary(guiGraphics);
        } else {
            drawInspector(guiGraphics);
        }
        drawModulePreview(guiGraphics);
    }

    private void drawInspector(GuiGraphics guiGraphics) {
        int innerX = inspectorBodyRect.x;
        int innerWidth = inspectorBodyRect.width;
        int y = inspectorBodyRect.y + 12 - inspectorScroll;

        enableUiScissor(guiGraphics, inspectorBodyRect);

        y = drawSectionHeader(guiGraphics, y, "OBJECT");
        drawLabelValue(guiGraphics, innerX, y, innerWidth, "Name", session.getSelectionLabel(), TEXT_BRIGHT);
        drawLabelValue(guiGraphics, innerX, y + 16, innerWidth, "Type", describeSelectionType(), TEXT_BRIGHT);

        y += 40;
        y = drawSectionHeader(guiGraphics, y, "TRANSFORM");
        drawTransformSection(guiGraphics, innerX, y);

        y += transformSectionHeight();
        y = drawSectionHeader(guiGraphics, y, "INPUT");
        drawLabelValue(guiGraphics, innerX, y, innerWidth, "Required", describePresetMode(session.getSelectedPreset()), TEXT_BRIGHT);
        String currentInput = describeSelectionInput();
        int inputColor = isInputCompatible(describePresetMode(session.getSelectedPreset())) ? TEXT_GREEN : TEXT_RED;
        drawLabelValue(guiGraphics, innerX, y + 16, innerWidth, "Current", currentInput, inputColor);

        y += 36;
        y = drawSectionHeader(guiGraphics, y, "PRESET");
        LoadedPack pack = session.getSelectedPack();
        PresetDefinition preset = session.getSelectedPreset();
        drawLabelValue(guiGraphics, innerX, y, innerWidth, "Pack", pack == null ? "<none>" : pack.getMetadata().id, TEXT_BRIGHT);
        drawLabelValue(guiGraphics, innerX, y + 16, innerWidth, "Preset", preset == null ? "<none>" : preset.id, TEXT_BRIGHT);

        y += 36;
        y = drawSectionHeader(guiGraphics, y, "PARAMETERS");
        drawParameterRows(guiGraphics, innerX, y);
        y += parameterRowsHeight();

        y = drawSectionHeader(guiGraphics, y, "VALIDATION");
        drawValidationSection(guiGraphics, innerX, y, innerWidth);
        y += validationContentHeight();

        y = drawSectionHeader(guiGraphics, y, "ACTIONS");
        guiGraphics.drawString(this.font, trimToWidth("Toolbar actions stay live while the world viewport is active.", innerWidth), innerX, y + 2, TEXT_MUTED);
        guiGraphics.disableScissor();
    }

    private void drawModuleLibrary(GuiGraphics guiGraphics) {
        int innerX = inspectorBodyRect.x;
        int innerWidth = inspectorBodyRect.width;
        int y = inspectorBodyRect.y;
        enableUiScissor(guiGraphics, inspectorBodyRect);
        y = drawSectionHeader(guiGraphics, y, "MODULE LIBRARY");
        LoadedPack pack = session.getSelectedPack();
        drawLabelValue(guiGraphics, innerX, y, innerWidth, "Pack", pack == null ? "<none>" : pack.getMetadata().id, TEXT_BRIGHT);
        drawLabelValue(guiGraphics, innerX, y + 16, innerWidth, "Kind", session.getModuleKindFilter(), TEXT_BLUE);
        drawLabelValue(guiGraphics, innerX, y + 32, innerWidth, "Mods", session.getModuleModStatusFilter(), TEXT_BLUE);
        if (moduleListViewport != null) {
            int headerY = moduleListViewport.y - 18;
            guiGraphics.drawString(this.font, "Modules", innerX, headerY, TEXT_BRIGHT);
            drawModuleList(guiGraphics);
        }
        guiGraphics.disableScissor();
    }

    private void drawModulePreview(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, "MODULE PREVIEW", modulePreviewPanel.x + 10, modulePreviewPanel.y + 10, TEXT_BRIGHT);
        ModuleDefinition module = session.getSelectedModule();
        int controlsHeight = 58;
        int summaryHeight = 66;
        int previewHeight = Math.max(72, modulePreviewPanel.height - 28 - controlsHeight - summaryHeight);
        ModuleSchematicPreviewRenderer.render(guiGraphics, modulePreviewPanel.x + 10, modulePreviewPanel.y + 28,
                modulePreviewPanel.width - 20, previewHeight, module,
                session.getModuleRotationQuarterTurns(), session.getModulePreviewZoomPercent(),
                session.isShowModuleBounds(), session.isShowModuleConnectors(), session.isShowModuleAir());
        int textY = modulePreviewPanel.y + previewHeight + uiMetrics.unit * 3;
        if (module == null) {
            guiGraphics.drawString(this.font, "No module selected.", modulePreviewPanel.x + 10, textY, TEXT_MUTED);
            return;
        }
        SpongeSchematicData data = module.schematicData;
        enableUiScissor(guiGraphics, new LayoutRect(modulePreviewPanel.x + 2, textY - 2,
                modulePreviewPanel.width - 4, modulePreviewPanel.bottom() - 62 - (textY - 2)));
        guiGraphics.drawString(this.font, trimToWidth(module.id, modulePreviewPanel.width - 20), modulePreviewPanel.x + 10, textY, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Size " + (data == null ? "<none>" : data.getWidth() + " x " + data.getHeight() + " x " + data.getLength()),
                modulePreviewPanel.x + 10, textY + 12, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Footprint " + (data == null ? "<none>" : data.getWidth() + " x " + data.getLength()),
                modulePreviewPanel.x + 10, textY + 24, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Style " + trimToWidth(safe(module.style), modulePreviewPanel.width - 70),
                modulePreviewPanel.x + 10, textY + 36, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Category " + trimToWidth(safe(module.category), modulePreviewPanel.width - 88),
                modulePreviewPanel.x + 10, textY + 48, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Tags " + trimToWidth(join(module.tags), modulePreviewPanel.width - 70),
                modulePreviewPanel.x + 10, textY + 60, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Mods " + trimToWidth(data == null ? "<none>" : join(data.getRequiredMods()), modulePreviewPanel.width - 70),
                modulePreviewPanel.x + 10, textY + 72, moduleHasMissingMods(module) ? TEXT_RED : TEXT_MUTED);
        List<ValidationIssue> issues = moduleValidationIssues(session.getSelectedPack(), module);
        String validationSummary = issues.isEmpty() ? "Validation OK" : "Validation " + issues.size() + " issue(s)";
        guiGraphics.drawString(this.font, validationSummary, modulePreviewPanel.x + 10, textY + 84, issues.isEmpty() ? TEXT_GREEN : TEXT_YELLOW);
        guiGraphics.disableScissor();
    }

    private void drawBottomBar(GuiGraphics guiGraphics) {
        drawPanel(guiGraphics, bottomBar, false);
        if (bottomSplitter != null) {
            drawLayoutHandle(guiGraphics, bottomSplitter, false);
        }
        int contentX = bottomBar.x + uiMetrics.inset;
        int contentY = bottomBar.y + uiMetrics.inset;
        int contentHeight = bottomBar.height - uiMetrics.inset * 2;
        int totalWidth = bottomBar.width - uiMetrics.inset * 2;
        int sectionWidth = clamp(totalWidth / 7, 150, 196);
        int statsWidth = clamp(totalWidth / 7, 160, 210);
        int warningWidth = clamp(totalWidth / 5, 210, 320);
        int logWidth = totalWidth - sectionWidth * 2 - statsWidth - warningWidth - 12;

        drawBottomSection(guiGraphics, new LayoutRect(contentX, contentY, sectionWidth, contentHeight), "POSITION", describePosition());
        contentX += sectionWidth + 4;
        drawBottomSection(guiGraphics, new LayoutRect(contentX, contentY, sectionWidth, contentHeight), "SELECTION", describeSelectionStats());
        contentX += sectionWidth + 4;
        drawBottomSection(guiGraphics, new LayoutRect(contentX, contentY, statsWidth, contentHeight), "STATS", describePreviewStats());
        contentX += statsWidth + 4;
        LayoutRect warningRect = new LayoutRect(contentX, contentY, warningWidth, contentHeight);
        drawBottomSection(guiGraphics, warningRect, "WARNINGS", getWarningSummary());
        contentX += warningWidth + 4;
        messageLogRect = new LayoutRect(contentX, contentY, logWidth, contentHeight);
        drawMessageLog(guiGraphics, messageLogRect);
    }

    private void drawInteractiveElements(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (EditorButton button : buttons) {
            if (!button.visible) {
                continue;
            }
            button.draw(guiGraphics, mouseX, mouseY);
        }
        for (EditorField field : fields) {
            if (!field.visible) {
                continue;
            }
            field.draw(guiGraphics);
        }
    }

    private void drawBakeConfirm(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        LayoutRect dialog = getConfirmDialogRect();
        drawPanel(guiGraphics, dialog, true);
        guiGraphics.drawString(this.font, "Confirm Bake", dialog.x + 14, dialog.y + 14, TEXT_BRIGHT);
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        int blockEntities = previewPlan == null ? 0 : countPreviewBlockEntities(previewPlan);
        String line = previewPlan == null
                ? "Preview missing."
                : "Bake " + previewPlan.getPlannedBlocks().size() + " blocks across " + countPreviewChunks(previewPlan)
                + " chunks" + (blockEntities > 0 ? " with " + blockEntities + " block entities" : "") + "?";
        int lineY = dialog.y + 40;
        for (String confirmLine : wrapText(line, dialog.width - 28)) {
            guiGraphics.drawString(this.font, confirmLine, dialog.x + 14, lineY, TEXT_MUTED);
            lineY += 12;
        }

        for (EditorButton button : confirmButtons(dialog)) {
            button.draw(guiGraphics, mouseX, mouseY);
        }
    }

    private boolean handleConfirmClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        LayoutRect dialog = getConfirmDialogRect();
        for (EditorButton confirmButton : confirmButtons(dialog)) {
            if (confirmButton.contains(mouseX, mouseY)) {
                confirmButton.press();
                return true;
            }
        }
        return dialog.contains(mouseX, mouseY);
    }

    private List<EditorButton> confirmButtons(LayoutRect dialog) {
        int buttonWidth = 108;
        int y = dialog.bottom() - 36;
        EditorButton cancel = new EditorButton("confirm_cancel",
                new LayoutRect(dialog.right() - buttonWidth * 2 - 20, y, buttonWidth, uiMetrics.topButtonHeight),
                "Cancel", false, false, () -> {
            showBakeConfirm = false;
            session.log(PcgEditorLogEntry.Severity.INFO, "Bake cancelled.");
        });
        EditorButton confirm = new EditorButton("confirm_bake",
                new LayoutRect(dialog.right() - buttonWidth - 10, y, buttonWidth, uiMetrics.topButtonHeight),
                "Bake", false, true, this::confirmBake);
        return List.of(cancel, confirm);
    }

    private LayoutRect getConfirmDialogRect() {
        int width = 360;
        int height = 146;
        return new LayoutRect((uiWidth - width) / 2, (uiHeight - height) / 2, width, height);
    }

    private void confirmBake() {
        showBakeConfirm = false;
        runAction("blockwright bake", PcgEditorLogEntry.Severity.SUCCESS, "Bake requested.");
    }

    private void drawParameterRows(GuiGraphics guiGraphics, int innerX, int startY) {
        if (parameterFields.isEmpty()) {
            guiGraphics.drawString(this.font, "No exposed parameters.", innerX, startY + 4, TEXT_MUTED);
            return;
        }
        for (ParameterField parameterField : parameterFields) {
            EditorField field = parameterField.field;
            if (!field.visible) {
                continue;
            }
            guiGraphics.drawString(this.font, trimToWidth(parameterField.key, 116), innerX, field.bounds.y + 4, TEXT_MUTED);
        }
    }

    private void drawModuleList(GuiGraphics guiGraphics) {
        if (moduleListViewport == null) {
            return;
        }
        guiGraphics.fill(moduleListViewport.x, moduleListViewport.y, moduleListViewport.right(), moduleListViewport.bottom(), 0x8010141A);
        guiGraphics.fill(moduleListViewport.x, moduleListViewport.y, moduleListViewport.right(), moduleListViewport.y + 1, PANEL_BORDER);
        guiGraphics.fill(moduleListViewport.x, moduleListViewport.bottom() - 1, moduleListViewport.right(), moduleListViewport.bottom(), PANEL_BORDER);
        enableUiScissor(guiGraphics, moduleListViewport);
        int rowY = moduleListViewport.y;
        int maxRows = Math.max(1, moduleListViewport.height / uiMetrics.moduleRowHeight);
        int end = Math.min(visibleModules.size(), moduleListScroll + maxRows);
        ModuleDefinition selectedModule = session.getSelectedModule();
        for (int i = moduleListScroll; i < end; i++) {
            ModuleDefinition module = visibleModules.get(i);
            boolean selected = selectedModule != null && module.id.equals(selectedModule.id);
            int rowBottom = rowY + uiMetrics.moduleRowHeight - 2;
            guiGraphics.fill(moduleListViewport.x + 1, rowY + 1, moduleListViewport.right() - 1, rowBottom,
                    selected ? PANEL_ACCENT_SOFT : 0x00000000);
            guiGraphics.drawString(this.font, trimToWidth(module.id, moduleListViewport.width - 18), moduleListViewport.x + 10, rowY + 5,
                    selected ? TEXT_BRIGHT : TEXT_MUTED);
            guiGraphics.drawString(this.font, trimToWidth(safe(module.moduleKind), moduleListViewport.width - 18), moduleListViewport.x + 10, rowY + 16,
                    selected ? TEXT_BLUE : TEXT_MUTED);
            rowY += uiMetrics.moduleRowHeight;
        }
        guiGraphics.disableScissor();
    }

    private void drawBottomSection(GuiGraphics guiGraphics, LayoutRect rect, String title, List<String> lines) {
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), PANEL_HEADER);
        guiGraphics.drawString(this.font, title, rect.x + 8, rect.y + 10, TEXT_BRIGHT);
        int y = rect.y + 30;
        for (String line : lines) {
            guiGraphics.drawString(this.font, trimToWidth(line, rect.width - 16), rect.x + 8, y, TEXT_MUTED);
            y += 15;
        }
    }

    private void drawMessageLog(GuiGraphics guiGraphics, LayoutRect rect) {
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), PANEL_HEADER);
        guiGraphics.drawString(this.font, "MESSAGE LOG", rect.x + 8, rect.y + 10, TEXT_BRIGHT);
        List<PcgEditorLogEntry> entries = session.getLogEntries();
        maxLogScroll = Math.max(0, entries.size() - 4);
        logScroll = clamp(logScroll, 0, maxLogScroll);
        int y = rect.y + 30;
        for (int i = logScroll; i < Math.min(entries.size(), logScroll + 4); i++) {
            PcgEditorLogEntry entry = entries.get(i);
            guiGraphics.drawString(this.font, trimToWidth(entry.getMessage(), rect.width - 16), rect.x + 8, y, logColor(entry));
            y += uiMetrics.logRowHeight;
        }
    }

    private int drawSectionHeader(GuiGraphics guiGraphics, int y, String title) {
        guiGraphics.fill(detailsPanel.x + 1, y, detailsPanel.right() - 1, y + 16, PANEL_HEADER);
        guiGraphics.drawString(this.font, title, detailsPanel.x + uiMetrics.inset, y + 4, TEXT_BRIGHT);
        return y + 24;
    }

    private void drawTransformSection(GuiGraphics guiGraphics, int innerX, int y) {
        guiGraphics.drawString(this.font, "Pos", innerX, y + 4, TEXT_MUTED);
        guiGraphics.drawString(this.font, "X", innerX + 44, y + 4, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Y", innerX + 116, y + 4, TEXT_MUTED);
        guiGraphics.drawString(this.font, "Z", innerX + 188, y + 4, TEXT_MUTED);
        if (session.getSelection() == PcgEditorSelection.REGION) {
            BoxRegionSelection region = ClientSelectionState.getRegionSelection();
            BlockPos max = region.getMax();
            guiGraphics.drawString(this.font, "Max", innerX, y + 46, TEXT_MUTED);
            guiGraphics.drawString(this.font, max == null ? "<unset>" : max.toShortString(), innerX + 82, y + 46, TEXT_BRIGHT);
            guiGraphics.drawString(this.font, "Size", innerX, y + 60, TEXT_MUTED);
            guiGraphics.drawString(this.font,
                    region.isComplete() ? region.getWidth() + " x " + region.getHeight() + " x " + region.getDepth() : "<none>",
                    innerX + 82, y + 60, TEXT_BRIGHT);
            return;
        }
        if (session.getSelection() == PcgEditorSelection.SPLINE || session.getSelection() == PcgEditorSelection.SPLINE_POINT) {
            SplineSelection spline = ClientSelectionState.getSplineSelection();
            BlockPos point = session.getSelectedSplinePoint();
            int infoY = session.getSelection() == PcgEditorSelection.SPLINE_POINT ? y + 46 : y + 30;
            guiGraphics.drawString(this.font, "Points", innerX, infoY, TEXT_MUTED);
            guiGraphics.drawString(this.font, String.valueOf(spline.getPoints().size()), innerX + 82, infoY, TEXT_BRIGHT);
            guiGraphics.drawString(this.font, "Point", innerX, infoY + 14, TEXT_MUTED);
            guiGraphics.drawString(this.font, point == null ? "<unset>" : point.toShortString(), innerX + 82, infoY + 14, TEXT_BRIGHT);
            return;
        }
        guiGraphics.drawString(this.font, "Focus", innerX, y + 30, TEXT_MUTED);
        guiGraphics.drawString(this.font, safe(session.getSelectionLabel()), innerX + 82, y + 30, TEXT_BRIGHT);
    }

    private void drawValidationSection(GuiGraphics guiGraphics, int innerX, int y, int innerWidth) {
        PcgEditorPreviewState state = session.getPreviewState();
        drawLabelValue(guiGraphics, innerX, y, innerWidth, "State", state.name(), colorForPreviewState(state));
        List<String> warnings = getWarningSummary();
        int lineY = y + 18;
        if (warnings.isEmpty()) {
            guiGraphics.drawString(this.font, "No issues found.", innerX, lineY, TEXT_GREEN);
            return;
        }
        for (String warning : warnings.subList(0, Math.min(3, warnings.size()))) {
            int color = warning.startsWith("ERROR") ? TEXT_RED : TEXT_YELLOW;
            guiGraphics.drawString(this.font, trimToWidth(warning, innerWidth), innerX, lineY, color);
            lineY += 12;
        }
    }

    private void drawLabelValue(GuiGraphics guiGraphics, int x, int y, int width, String label, String value, int valueColor) {
        guiGraphics.drawString(this.font, label, x, y, TEXT_MUTED);
        guiGraphics.drawString(this.font, trimToWidth(value, Math.max(32, width - 92)), x + 86, y, valueColor);
    }

    private void drawViewportFrame(GuiGraphics guiGraphics) {
        guiGraphics.fill(viewport.x, viewport.y, viewport.right(), viewport.y + 1, PANEL_BORDER);
        guiGraphics.fill(viewport.x, viewport.bottom() - 1, viewport.right(), viewport.bottom(), PANEL_BORDER);
        guiGraphics.fill(viewport.x, viewport.y, viewport.x + 1, viewport.bottom(), PANEL_BORDER);
        guiGraphics.fill(viewport.right() - 1, viewport.y, viewport.right(), viewport.bottom(), PANEL_BORDER);
    }

    private void drawLayoutHandle(GuiGraphics guiGraphics, LayoutRect rect, boolean vertical) {
        int fill = 0x663E4956;
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), fill);
        if (vertical) {
            int center = rect.x + rect.width / 2;
            guiGraphics.fill(center, rect.y + 8, center + 1, rect.bottom() - 8, PANEL_BORDER);
        } else {
            int center = rect.y + rect.height / 2;
            guiGraphics.fill(rect.x + 8, center, rect.right() - 8, center + 1, PANEL_BORDER);
        }
    }

    private void drawPanel(GuiGraphics guiGraphics, LayoutRect rect, boolean topAccent) {
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), PANEL_BG);
        if (topAccent) {
            guiGraphics.fill(rect.x, rect.y, rect.right(), rect.y + 2, PANEL_ACCENT);
        }
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.y + 1, PANEL_BORDER);
        guiGraphics.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.x, rect.y, rect.x + 1, rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.right() - 1, rect.y, rect.right(), rect.bottom(), PANEL_BORDER);
    }

    private boolean beginLayoutDrag(double mouseX, double mouseY) {
        if (detailsSplitter != null && detailsSplitter.contains(mouseX, mouseY)) {
            activeDragHandle = DragHandle.DETAILS;
            setCursor(CursorType.HORIZONTAL_RESIZE);
            return true;
        }
        if (previewSplitter != null && previewSplitter.contains(mouseX, mouseY)) {
            activeDragHandle = DragHandle.PREVIEW;
            setCursor(CursorType.HORIZONTAL_RESIZE);
            return true;
        }
        if (bottomSplitter != null && bottomSplitter.contains(mouseX, mouseY)) {
            activeDragHandle = DragHandle.BOTTOM;
            setCursor(CursorType.VERTICAL_RESIZE);
            return true;
        }
        return false;
    }

    private void handleLayoutDrag(double mouseX, double mouseY) {
        switch (activeDragHandle) {
            case DETAILS -> {
                int newWidth = workspace.right() - (int) Math.round(mouseX);
                detailsPanelWidth = clamp(newWidth, uiMetrics.minDetailsWidth, Math.max(uiMetrics.minDetailsWidth, workspace.width - uiMetrics.unit * 12));
                rebuildUi();
            }
            case PREVIEW -> {
                int newWidth = uiWidth - (int) Math.round(mouseX);
                previewPanelWidth = clamp(newWidth, uiMetrics.minPreviewWidth, uiMetrics.maxPreviewWidth);
                rebuildUi();
            }
            case BOTTOM -> {
                int newHeight = uiHeight - (int) Math.round(mouseY);
                bottomBarHeight = clamp(newHeight, uiMetrics.minBottomBarHeight, uiMetrics.maxBottomBarHeight);
                rebuildUi();
            }
            case NONE -> {
            }
        }
    }

    private void updateCursorForPosition(double mouseX, double mouseY) {
        CursorType target = CursorType.DEFAULT;
        if (detailsSplitter != null && detailsSplitter.contains(mouseX, mouseY)) {
            target = CursorType.HORIZONTAL_RESIZE;
        } else if (previewSplitter != null && previewSplitter.contains(mouseX, mouseY)) {
            target = CursorType.HORIZONTAL_RESIZE;
        } else if (bottomSplitter != null && bottomSplitter.contains(mouseX, mouseY)) {
            target = CursorType.VERTICAL_RESIZE;
        }
        setCursor(target);
    }

    private void setCursor(CursorType target) {
        if (activeCursorType == target) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        long window = minecraft.getWindow().getWindow();
        if (target == CursorType.HORIZONTAL_RESIZE) {
            if (horizontalResizeCursor == 0L) {
                horizontalResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
            }
            GLFW.glfwSetCursor(window, horizontalResizeCursor);
        } else if (target == CursorType.VERTICAL_RESIZE) {
            if (verticalResizeCursor == 0L) {
                verticalResizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR);
            }
            GLFW.glfwSetCursor(window, verticalResizeCursor);
        } else {
            GLFW.glfwSetCursor(window, 0L);
        }
        activeCursorType = target;
    }

    private EditorButton addButton(String id, int x, int y, int width, int height, String label, boolean selected, boolean danger, Runnable onPress) {
        EditorButton button = new EditorButton(id, new LayoutRect(x, y, width, height), label, selected, danger, onPress);
        buttons.add(button);
        return button;
    }

    private EditorField addField(String id, int x, int y, int width, int height, String value, boolean integerOnly, Consumer<String> onChange) {
        EditorField field = new EditorField(id, new LayoutRect(x, y, width, height), value == null ? "" : value, integerOnly, onChange);
        fields.add(field);
        return field;
    }

    private void restoreFocus(String focusedId) {
        if (focusedId == null) {
            return;
        }
        for (EditorField field : fields) {
            if (field.id.equals(focusedId)) {
                focusedField = field;
                field.focused = true;
                field.caret = field.value.length();
                return;
            }
        }
    }

    private boolean handleButtonClick(double mouseX, double mouseY) {
        for (EditorButton button : buttons) {
            if (button.visible && button.contains(mouseX, mouseY)) {
                button.press();
                return true;
            }
        }
        return false;
    }

    private boolean handleFieldClick(double mouseX, double mouseY) {
        for (EditorField field : fields) {
            if (!field.visible) {
                continue;
            }
            if (field.contains(mouseX, mouseY)) {
                focusField(field);
                return true;
            }
        }
        return false;
    }

    private void focusField(EditorField field) {
        if (focusedField != null) {
            focusedField.focused = false;
        }
        focusedField = field;
        focusedField.focused = true;
        focusedField.caret = focusedField.value.length();
    }

    private void clearFocus() {
        if (focusedField != null) {
            focusedField.focused = false;
        }
        focusedField = null;
    }

    private void layoutParameterFields() {
        if (parameterViewport == null) {
            return;
        }
        for (ParameterField parameterField : parameterFields) {
            int y = parameterViewport.y + parameterField.rowIndex * uiMetrics.rowHeight + 4;
            parameterField.field.bounds = new LayoutRect(parameterField.field.bounds.x, y, parameterField.field.bounds.width, parameterField.field.bounds.height);
            parameterField.field.visible = rectIntersects(parameterField.field.bounds, inspectorBodyRect);
            if (!parameterField.field.visible && parameterField.field.focused) {
                clearFocus();
            }
        }
    }

    private void clampLogScroll() {
        List<PcgEditorLogEntry> entries = session.getLogEntries();
        maxLogScroll = Math.max(0, entries.size() - 4);
        logScroll = clamp(logScroll, 0, maxLogScroll);
    }

    private void refreshVisibleModules() {
        visibleModules.clear();
        visibleModules.addAll(getFilteredModules(session.getSelectedPack()));
        if (moduleListViewport != null) {
            int maxRows = Math.max(1, moduleListViewport.height / uiMetrics.moduleRowHeight);
            maxModuleListScroll = Math.max(0, visibleModules.size() - maxRows);
            moduleListScroll = clamp(moduleListScroll, 0, maxModuleListScroll);
        }
    }

    private String buildUiSignature() {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        ModuleDefinition module = session.getSelectedModule();
        return session.getActiveTool().name()
                + "|" + session.getSelection().name()
                + "|" + safe(session.getSelectedPackId())
                + "|" + safe(session.getSelectedPresetId())
                + "|" + safe(module == null ? session.getSelectedModuleId() : module.id)
                + "|" + region.isComplete()
                + "|" + posKey(region.getPos1())
                + "|" + posKey(region.getPos2())
                + "|" + region.getWidth() + "x" + region.getHeight() + "x" + region.getDepth()
                + "|" + spline.getPoints().size()
                + "|" + posKey(getSelectedControlPoint())
                + "|" + session.getSelectedRegionCornerIndex()
                + "|" + session.getSelectedSplinePointIndex()
                + "|" + session.getPreviewState().name()
                + "|" + session.getModuleKindFilter()
                + "|" + session.getModuleModStatusFilter()
                + "|" + session.getModuleStyleQuery()
                + "|" + session.getModuleCategoryQuery()
                + "|" + session.getModuleSearchQuery()
                + "|" + session.getModuleTagQuery()
                + "|" + this.width + "x" + this.height;
    }

    private String posKey(BlockPos pos) {
        return pos == null ? "<none>" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private boolean handleViewportClick(double mouseX, double mouseY) {
        boolean selectedExistingTarget = selectViewportTarget(mouseX, mouseY);
        switch (session.getActiveTool()) {
            case BOX_REGION -> {
                if (selectedExistingTarget) {
                    rebuildUi();
                    return true;
                }
                handleBoxRegionClick(mouseX, mouseY);
                return true;
            }
            case SPLINE -> {
                if (selectedExistingTarget) {
                    rebuildUi();
                    return true;
                }
                BlockPos target = pickViewportBlock(mouseX, mouseY);
                if (target == null) {
                    session.log(PcgEditorLogEntry.Severity.WARNING, "No block under the editor view.");
                    return true;
                }
                runAction("blockwright spline addpos " + target.getX() + " " + target.getY() + " " + target.getZ(),
                        PcgEditorLogEntry.Severity.INFO, "Added spline point.");
                session.selectSplineIfPresent();
                session.setSelection(PcgEditorSelection.SPLINE_POINT);
                session.setSelectedSplinePointIndex(ClientSelectionState.getSplineSelection().getPoints().size() - 1);
                rebuildUi();
                return true;
            }
            case SELECT -> {
                if (selectedExistingTarget) {
                    rebuildUi();
                    return true;
                }
                selectBestObject();
                rebuildUi();
                return true;
            }
            case TRANSFORM -> {
                if (selectedExistingTarget) {
                    rebuildUi();
                    return true;
                }
                focusSelection();
                return true;
            }
            case MODULE_LIBRARY, PAINT_MASK -> {
                return false;
            }
        }
        return false;
    }

    private void handleBoxRegionClick(double mouseX, double mouseY) {
        BlockPos target = pickViewportBlock(mouseX, mouseY);
        if (target == null) {
            session.log(PcgEditorLogEntry.Severity.WARNING, "No block under the editor view.");
            return;
        }
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        if (region.getPos1() == null) {
            setRegionCorner(true, target, "Set region corner P1.");
            session.setSelection(PcgEditorSelection.REGION);
            session.setSelectedRegionCornerIndex(0);
        } else if (region.getPos2() == null) {
            setRegionCorner(false, target, "Set region corner P2.");
            session.setSelection(PcgEditorSelection.REGION);
            session.setSelectedRegionCornerIndex(1);
        } else {
            runAction("blockwright region clear", PcgEditorLogEntry.Severity.INFO, "Started a new box region.");
            setRegionCorner(true, target, "Set region corner P1.");
            session.setSelection(PcgEditorSelection.REGION);
            session.setSelectedRegionCornerIndex(0);
        }
        rebuildUi();
    }

    private void setRegion(BlockPos p1, BlockPos p2, String logMessage) {
        runAction("blockwright region set "
                        + p1.getX() + " " + p1.getY() + " " + p1.getZ() + " "
                        + p2.getX() + " " + p2.getY() + " " + p2.getZ(),
                PcgEditorLogEntry.Severity.INFO, logMessage);
    }

    private void setRegionCorner(boolean firstCorner, BlockPos pos, String logMessage) {
        runAction("blockwright region " + (firstCorner ? "setpos1 " : "setpos2 ")
                        + pos.getX() + " " + pos.getY() + " " + pos.getZ(),
                PcgEditorLogEntry.Severity.INFO, logMessage);
    }

    private BlockPos pickViewportBlock(double mouseX, double mouseY) {
        BlockHitResult blockHit = pickViewportHit(mouseX, mouseY);
        return blockHit == null ? null : blockHit.getBlockPos().relative(blockHit.getDirection());
    }

    private BlockPos pickViewportSurfaceBlock(double mouseX, double mouseY) {
        BlockHitResult blockHit = pickViewportHit(mouseX, mouseY);
        return blockHit == null ? null : blockHit.getBlockPos();
    }

    private BlockHitResult pickViewportHit(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return null;
        }
        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        Vec3 direction = screenRayDirection(mouseX, mouseY);
        if (direction == null) {
            return null;
        }
        HitResult hit = minecraft.level.clip(new ClipContext(
                eye,
                eye.add(direction.scale(160.0D)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        ));
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }
        return null;
    }

    private boolean beginGizmoDrag(double mouseX, double mouseY) {
        BlockPos controlPoint = getSelectedControlPoint();
        if (controlPoint == null) {
            return false;
        }
        PcgEditorAxis axis = pickGizmoAxis(mouseX, mouseY, controlPoint);
        if (axis == PcgEditorAxis.NONE) {
            return false;
        }
        ScreenProjection originProjection = projectWorldToScreen(Vec3.atCenterOf(controlPoint));
        ScreenProjection axisProjection = projectWorldToScreen(Vec3.atCenterOf(controlPoint).add(axis.stepX() * GIZMO_AXIS_LENGTH,
                axis.stepY() * GIZMO_AXIS_LENGTH, axis.stepZ() * GIZMO_AXIS_LENGTH));
        if (originProjection == null || axisProjection == null) {
            return false;
        }
        double axisScreenX = axisProjection.screenX - originProjection.screenX;
        double axisScreenY = axisProjection.screenY - originProjection.screenY;
        double axisScreenLength = Math.hypot(axisScreenX, axisScreenY);
        if (axisScreenLength < GIZMO_MIN_AXIS_SCREEN_LENGTH) {
            return false;
        }
        activeGizmoAxis = axis;
        gizmoDragStartPoint = controlPoint;
        gizmoDragAppliedPoint = controlPoint;
        gizmoDragStartMouseX = mouseX;
        gizmoDragStartMouseY = mouseY;
        gizmoDragAxisUnitX = axisScreenX / axisScreenLength;
        gizmoDragAxisUnitY = axisScreenY / axisScreenLength;
        gizmoDragWorldPerPixel = GIZMO_AXIS_LENGTH / axisScreenLength;
        session.setActiveGizmoAxis(axis);
        session.setHoveredGizmoAxis(axis);
        return true;
    }

    private void updateGizmoDrag(double mouseX, double mouseY) {
        if (activeGizmoAxis == PcgEditorAxis.NONE || gizmoDragStartPoint == null) {
            return;
        }
        double projectedPixels = (mouseX - gizmoDragStartMouseX) * gizmoDragAxisUnitX
                + (mouseY - gizmoDragStartMouseY) * gizmoDragAxisUnitY;
        int snappedOffset = snapAxisOffset(projectedPixels * gizmoDragWorldPerPixel);
        BlockPos nextPoint = gizmoDragStartPoint.offset(
                activeGizmoAxis.stepX() * snappedOffset,
                activeGizmoAxis.stepY() * snappedOffset,
                activeGizmoAxis.stepZ() * snappedOffset
        );
        if (nextPoint.equals(gizmoDragAppliedPoint) || !moveSelectedControlPoint(nextPoint, false)) {
            return;
        }
        gizmoDragAppliedPoint = nextPoint;
        rebuildUi();
    }

    private void finishGizmoDrag() {
        if (activeGizmoAxis == PcgEditorAxis.NONE) {
            return;
        }
        if (gizmoDragStartPoint != null && gizmoDragAppliedPoint != null && !gizmoDragStartPoint.equals(gizmoDragAppliedPoint)) {
            logControlPointMove();
        }
        cancelGizmoDrag();
        rebuildUi();
    }

    private void cancelGizmoDrag() {
        activeGizmoAxis = PcgEditorAxis.NONE;
        gizmoDragStartPoint = null;
        gizmoDragAppliedPoint = null;
        gizmoDragStartMouseX = 0.0D;
        gizmoDragStartMouseY = 0.0D;
        gizmoDragAxisUnitX = 0.0D;
        gizmoDragAxisUnitY = 0.0D;
        gizmoDragWorldPerPixel = 0.0D;
        session.setActiveGizmoAxis(PcgEditorAxis.NONE);
    }

    private PcgEditorAxis pickGizmoAxis(double mouseX, double mouseY, BlockPos controlPoint) {
        Vec3 origin = Vec3.atCenterOf(controlPoint);
        ScreenProjection originProjection = projectWorldToScreen(origin);
        if (originProjection == null) {
            return PcgEditorAxis.NONE;
        }
        PcgEditorAxis bestAxis = PcgEditorAxis.NONE;
        double bestDistance = GIZMO_PICK_RADIUS;
        for (PcgEditorAxis axis : PcgEditorAxis.values()) {
            if (axis == PcgEditorAxis.NONE) {
                continue;
            }
            ScreenProjection axisProjection = projectWorldToScreen(origin.add(axis.stepX() * GIZMO_AXIS_LENGTH,
                    axis.stepY() * GIZMO_AXIS_LENGTH, axis.stepZ() * GIZMO_AXIS_LENGTH));
            if (axisProjection == null) {
                continue;
            }
            double distance = distanceToSegment(mouseX, mouseY,
                    originProjection.screenX, originProjection.screenY,
                    axisProjection.screenX, axisProjection.screenY);
            if (distance <= bestDistance) {
                bestDistance = distance;
                bestAxis = axis;
            }
        }
        return bestAxis;
    }

    private Vec3 screenRayDirection(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        double normalizedX = mouseX / Math.max(1.0D, this.width) * 2.0D - 1.0D;
        double normalizedY = 1.0D - mouseY / Math.max(1.0D, this.height) * 2.0D;
        double aspect = Math.max(1.0D, (double) this.width / Math.max(1, this.height));
        double fovDegrees = minecraft.options.fov().get();
        double tanHalfFov = Math.tan(Math.toRadians(fovDegrees) * 0.5D);
        Vec3 forward = minecraft.player.getViewVector(1.0F).normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward);
        if (up.lengthSqr() < 1.0E-6D) {
            up = worldUp;
        } else {
            up = up.normalize();
        }
        return forward
                .add(right.scale(normalizedX * tanHalfFov * aspect))
                .add(up.scale(normalizedY * tanHalfFov))
                .normalize();
    }

    private ScreenProjection projectWorldToScreen(Vec3 worldPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        Vec3 forward = minecraft.player.getViewVector(1.0F).normalize();
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward);
        if (up.lengthSqr() < 1.0E-6D) {
            up = worldUp;
        } else {
            up = up.normalize();
        }
        Vec3 relative = worldPos.subtract(eye);
        double depth = relative.dot(forward);
        if (depth <= 0.05D) {
            return null;
        }
        double aspect = Math.max(1.0D, (double) this.width / Math.max(1, this.height));
        double tanHalfFov = Math.tan(Math.toRadians(minecraft.options.fov().get()) * 0.5D);
        double normalizedX = relative.dot(right) / (depth * tanHalfFov * aspect);
        double normalizedY = relative.dot(up) / (depth * tanHalfFov);
        double screenX = (normalizedX + 1.0D) * 0.5D * this.width;
        double screenY = (1.0D - normalizedY) * 0.5D * this.height;
        return new ScreenProjection(screenX, screenY, depth);
    }

    private void updateHoverPlacement(double mouseX, double mouseY) {
        if (session.isNavigating() || showBakeConfirm || activeGizmoAxis != PcgEditorAxis.NONE) {
            session.setHoveredGizmoAxis(PcgEditorAxis.NONE);
            ClientSelectionState.clearHoverPlacement();
            return;
        }
        double uiMouseX = toUiX(mouseX);
        double uiMouseY = toUiY(mouseY);
        if (!viewport.contains(uiMouseX, uiMouseY)) {
            session.setHoveredGizmoAxis(PcgEditorAxis.NONE);
            ClientSelectionState.clearHoverPlacement();
            return;
        }
        updateHoveredGizmoAxis(mouseX, mouseY);
        if (session.getHoveredGizmoAxis() != PcgEditorAxis.NONE) {
            ClientSelectionState.clearHoverPlacement();
            return;
        }
        if (session.getActiveTool() != PcgEditorTool.BOX_REGION && session.getActiveTool() != PcgEditorTool.SPLINE) {
            ClientSelectionState.clearHoverPlacement();
            return;
        }
        if (findViewportSelectionTarget(mouseX, mouseY) != null) {
            ClientSelectionState.clearHoverPlacement();
            return;
        }
        ClientSelectionState.setHoverPlacement(pickViewportBlock(mouseX, mouseY));
    }

    private void updateHoveredGizmoAxis(double mouseX, double mouseY) {
        BlockPos controlPoint = getSelectedControlPoint();
        if (controlPoint == null) {
            session.setHoveredGizmoAxis(PcgEditorAxis.NONE);
            return;
        }
        session.setHoveredGizmoAxis(pickGizmoAxis(mouseX, mouseY, controlPoint));
    }

    private boolean selectViewportTarget(double mouseX, double mouseY) {
        ViewportSelectionTarget target = findViewportSelectionTarget(mouseX, mouseY);
        if (target == null) {
            return false;
        }
        session.setSelection(target.selection());
        session.setSelectedRegionCornerIndex(target.regionCornerIndex());
        if (target.selection() == PcgEditorSelection.SPLINE_POINT) {
            session.setSelectedSplinePointIndex(target.splinePointIndex());
        } else {
            session.setSelectedSplinePointIndex(-1);
        }
        session.log(PcgEditorLogEntry.Severity.INFO, target.logMessage());
        return true;
    }

    private ViewportSelectionTarget findViewportSelectionTarget(double mouseX, double mouseY) {
        BlockPos surfaceHit = pickViewportSurfaceBlock(mouseX, mouseY);
        BlockPos placementHit = pickViewportBlock(mouseX, mouseY);
        if (surfaceHit == null && placementHit == null) {
            return null;
        }
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        for (int i = 0; i < spline.getPoints().size(); i++) {
            BlockPos point = spline.getPoints().get(i);
            if (point.equals(surfaceHit) || point.equals(placementHit)) {
                return new ViewportSelectionTarget(PcgEditorSelection.SPLINE_POINT, -1, i, "Selected spline point #" + i + ".");
            }
        }
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        if ((region.getPos1() != null && region.getPos1().equals(surfaceHit))
                || (region.getPos1() != null && region.getPos1().equals(placementHit))) {
            return new ViewportSelectionTarget(PcgEditorSelection.REGION, 0, -1, "Selected Box Region_01 corner P1.");
        }
        if ((region.getPos2() != null && region.getPos2().equals(surfaceHit))
                || (region.getPos2() != null && region.getPos2().equals(placementHit))) {
            return new ViewportSelectionTarget(PcgEditorSelection.REGION, 1, -1, "Selected Box Region_01 corner P2.");
        }
        return null;
    }

    private void clearAllSelections() {
        runAction("blockwright region clear", PcgEditorLogEntry.Severity.INFO, "Cleared all selections.");
        runAction("blockwright spline clear", PcgEditorLogEntry.Severity.INFO, "Cleared all selections.");
        session.setSelection(PcgEditorSelection.NONE);
        ClientSelectionState.clearHoverPlacement();
        rebuildUi();
    }

    private void selectBestObject() {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        PreviewPlan preview = ClientPreviewState.getPreviewPlan();
        if (region.getPos1() != null || region.getPos2() != null) {
            session.setSelection(PcgEditorSelection.REGION);
            session.setSelectedRegionCornerIndex(region.getPos2() != null ? 1 : 0);
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
        int index = (int) ((mouseY - moduleListViewport.y) / uiMetrics.moduleRowHeight) + moduleListScroll;
        if (index < 0 || index >= visibleModules.size()) {
            return false;
        }
        ModuleDefinition module = visibleModules.get(index);
        session.setSelectedModuleId(module.id);
        session.setSelection(PcgEditorSelection.MODULE);
        session.log(PcgEditorLogEntry.Severity.INFO, "Selected module " + module.id + ".");
        rebuildUi();
        return true;
    }

    private void startNavigation(double mouseX, double mouseY) {
        session.setNavigating(true);
        clearFocus();
        navigationMouseX = mouseX;
        navigationMouseY = mouseY;
        navigationLastNanos = System.nanoTime();
    }

    private void stopNavigation() {
        if (session.isNavigating()) {
            session.setNavigating(false);
            navForward = false;
            navBack = false;
            navLeft = false;
            navRight = false;
            navUp = false;
            navDown = false;
            navFast = false;
            navigationVelocity = Vec3.ZERO;
            navigationLastNanos = 0L;
            resetNavigationInput();
        }
    }

    private void updateNavigationLook(double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!session.isNavigating() || minecraft.player == null) {
            return;
        }
        double deltaX = mouseX - navigationMouseX;
        double deltaY = mouseY - navigationMouseY;
        navigationMouseX = mouseX;
        navigationMouseY = mouseY;
        if (deltaX == 0.0D && deltaY == 0.0D) {
            return;
        }
        float sensitivity = (float) (minecraft.options.sensitivity().get() * 0.45D + 0.1D) * session.getEditorLookSensitivityMultiplier();
        double scaleCompensation = uiScale <= 0.0D ? 1.0D : 1.0D / uiScale;
        float yawStep = (float) (deltaX * sensitivity * scaleCompensation * 0.75D);
        float pitchStep = (float) (deltaY * sensitivity * scaleCompensation * 0.75D);
        minecraft.player.setYRot(minecraft.player.getYRot() + yawStep);
        minecraft.player.setXRot(clampPitch(minecraft.player.getXRot() + pitchStep));
        minecraft.player.yRotO = minecraft.player.getYRot();
        minecraft.player.xRotO = minecraft.player.getXRot();
    }

    private boolean handleNavigationKey(int keyCode, int scanCode, boolean pressed) {
        if (!session.isNavigating()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.keyUp.matches(keyCode, scanCode)) {
            navForward = pressed;
        } else if (minecraft.options.keyDown.matches(keyCode, scanCode)) {
            navBack = pressed;
        } else if (minecraft.options.keyLeft.matches(keyCode, scanCode)) {
            navLeft = pressed;
        } else if (minecraft.options.keyRight.matches(keyCode, scanCode)) {
            navRight = pressed;
        } else if (minecraft.options.keyJump.matches(keyCode, scanCode)) {
            navUp = pressed;
        } else if (minecraft.options.keyShift.matches(keyCode, scanCode)) {
            navDown = pressed;
        } else if (minecraft.options.keySprint.matches(keyCode, scanCode)) {
            navFast = pressed;
        } else {
            return false;
        }
        return true;
    }

    private void tickNavigationMovement() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        boolean forwardPressed = navForward || minecraft.options.keyUp.isDown();
        boolean backPressed = navBack || minecraft.options.keyDown.isDown();
        boolean leftPressed = navLeft || minecraft.options.keyLeft.isDown();
        boolean rightPressed = navRight || minecraft.options.keyRight.isDown();
        boolean upPressed = navUp || minecraft.options.keyJump.isDown();
        boolean downPressed = navDown || minecraft.options.keyShift.isDown();
        boolean fastPressed = navFast || minecraft.options.keySprint.isDown();
        float forwardImpulse = 0.0F;
        float strafeImpulse = 0.0F;
        float verticalImpulse = 0.0F;
        if (forwardPressed) {
            forwardImpulse += 1.0F;
        }
        if (backPressed) {
            forwardImpulse -= 1.0F;
        }
        if (leftPressed) {
            strafeImpulse += 1.0F;
        }
        if (rightPressed) {
            strafeImpulse -= 1.0F;
        }
        if (upPressed) {
            verticalImpulse += 1.0F;
        }
        if (downPressed) {
            verticalImpulse -= 1.0F;
        }
        player.input.leftImpulse = strafeImpulse;
        player.input.forwardImpulse = forwardImpulse;
        player.input.jumping = upPressed;
        player.input.shiftKeyDown = downPressed;
        player.setSprinting(fastPressed);
        float targetFlyingSpeed = fastPressed ? session.getEditorNavigationFastFlySpeed() : session.getEditorNavigationFlySpeed();
        if (player.getAbilities().getFlyingSpeed() != targetFlyingSpeed) {
            player.getAbilities().setFlyingSpeed(targetFlyingSpeed);
            player.onUpdateAbilities();
        }
        double elapsedSeconds = navigationElapsedSeconds();
        updateNavigationCameraVelocity(player, forwardImpulse, strafeImpulse, verticalImpulse, fastPressed, elapsedSeconds);
    }

    private double navigationElapsedSeconds() {
        long now = System.nanoTime();
        if (navigationLastNanos == 0L) {
            navigationLastNanos = now;
            return 0.0D;
        }
        double elapsedSeconds = (now - navigationLastNanos) / 1_000_000_000.0D;
        navigationLastNanos = now;
        return Math.min(0.1D, Math.max(0.0D, elapsedSeconds));
    }

    private void updateNavigationCameraVelocity(LocalPlayer player, float forwardImpulse, float strafeImpulse, float verticalImpulse,
                                                boolean fastPressed, double elapsedSeconds) {
        Vec3 forward = player.getLookAngle();
        Vec3 flatForward = new Vec3(forward.x, 0.0D, forward.z);
        if (flatForward.lengthSqr() < 1.0E-6D) {
            flatForward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            flatForward = flatForward.normalize();
        }
        Vec3 right = new Vec3(-flatForward.z, 0.0D, flatForward.x);
        Vec3 input = flatForward.scale(forwardImpulse)
                .subtract(right.scale(strafeImpulse))
                .add(0.0D, verticalImpulse, 0.0D);
        if (input.lengthSqr() > 1.0D) {
            input = input.normalize();
        }
        double maxSpeed = fastPressed ? 28.0D : 11.0D;
        double acceleration = fastPressed ? 72.0D : 36.0D;
        if (input.lengthSqr() > 0.0D) {
            navigationVelocity = navigationVelocity.add(input.scale(acceleration * elapsedSeconds));
        } else {
            navigationVelocity = navigationVelocity.scale(Math.pow(0.001D, elapsedSeconds));
            if (navigationVelocity.lengthSqr() < 0.0004D) {
                navigationVelocity = Vec3.ZERO;
            }
        }
        if (navigationVelocity.lengthSqr() > maxSpeed * maxSpeed) {
            navigationVelocity = navigationVelocity.normalize().scale(maxSpeed);
        }
        if (navigationVelocity.lengthSqr() == 0.0D) {
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 destination = player.position().add(navigationVelocity.scale(elapsedSeconds));
        player.moveTo(destination.x, destination.y, destination.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
    }

    private void resetNavigationInput() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        player.input.leftImpulse = 0.0F;
        player.input.forwardImpulse = 0.0F;
        player.input.jumping = false;
        player.input.shiftKeyDown = false;
        player.setSprinting(false);
        navigationVelocity = Vec3.ZERO;
        if (player.getAbilities().getFlyingSpeed() != session.getEditorNavigationFlySpeed()) {
            player.getAbilities().setFlyingSpeed(session.getEditorNavigationFlySpeed());
            player.onUpdateAbilities();
        }
    }

    private void updateActionStates() {
        PresetDefinition preset = session.getSelectedPreset();
        PcgEditorPreviewState previewState = session.getPreviewState();
        ModuleDefinition selectedModule = session.getSelectedModule();
        if (previewButton != null) {
            previewButton.enabled = preset != null && hasValidInputForPreset(preset);
        }
        if (regenerateButton != null) {
            regenerateButton.enabled = preset != null && hasValidInputForPreset(preset);
        }
        if (bakeButton != null) {
            bakeButton.enabled = previewState == PcgEditorPreviewState.VALID || previewState == PcgEditorPreviewState.WARNING;
        }
        if (undoButton != null) {
            undoButton.enabled = true;
        }
        if (redoButton != null) {
            redoButton.enabled = false;
        }
        if (reloadButton != null) {
            reloadButton.enabled = true;
        }
        if (moduleExportButton != null) {
            moduleExportButton.enabled = ClientSelectionState.getRegionSelection().isComplete();
        }
        if (moduleRotateButton != null) {
            moduleRotateButton.enabled = selectedModule != null;
        }
        if (moduleZoomOutButton != null) {
            moduleZoomOutButton.enabled = selectedModule != null && session.getModulePreviewZoomPercent() > 50;
        }
        if (moduleZoomInButton != null) {
            moduleZoomInButton.enabled = selectedModule != null && session.getModulePreviewZoomPercent() < 200;
        }
        if (moduleBoundsButton != null) {
            moduleBoundsButton.enabled = selectedModule != null;
        }
        if (moduleConnectorsButton != null) {
            moduleConnectorsButton.enabled = selectedModule != null;
        }
        if (moduleAirButton != null) {
            moduleAirButton.enabled = selectedModule != null;
        }
        if (focusButton != null) {
            focusButton.enabled = session.getSelectionFocus() != null;
        }
        if (deleteButton != null) {
            deleteButton.enabled = session.getSelection() != PcgEditorSelection.NONE;
        }
        if (clearPreviewButton != null) {
            clearPreviewButton.enabled = ClientPreviewState.getPreviewPlan() != null;
        }
        if (transformApplyButton != null) {
            transformApplyButton.enabled = isTransformSelection();
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
        rebuildUi();
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
        parameterOverrides.clear();
        session.markDirty("Pack selection changed.");
        rebuildUi();
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
        parameterOverrides.clear();
        session.markDirty("Preset selection changed.");
        rebuildUi();
    }

    private void reloadPacks() {
        Blockwright.getPackManager().reload();
        parameterOverrides.clear();
        session.markDirty("Preset packs reloaded.");
        session.log(PcgEditorLogEntry.Severity.SUCCESS, "Reloaded " + Blockwright.getPackManager().getLoadedPackCount() + " pack(s).");
        sendCommand("blockwright reload");
        rebuildUi();
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
        for (ParameterField field : parameterFields) {
            String value = field.field.value;
            overrideMap.put(field.key, value);
            overrideParts.add(field.key + "=" + value);
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
        showBakeConfirm = true;
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
        rebuildUi();
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
        if (transformXField == null || transformYField == null || transformZField == null) {
            return;
        }
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(transformXField.value.trim());
            y = Integer.parseInt(transformYField.value.trim());
            z = Integer.parseInt(transformZField.value.trim());
        } catch (NumberFormatException exception) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Transform fields must be integers.");
            return;
        }

        if (session.getSelection() == PcgEditorSelection.REGION) {
            if (!moveSelectedControlPoint(new BlockPos(x, y, z), true)) {
                session.log(PcgEditorLogEntry.Severity.ERROR, "No box region corner is selected.");
            }
            rebuildUi();
            return;
        }

        if (session.getSelection() == PcgEditorSelection.SPLINE_POINT) {
            if (!moveSelectedControlPoint(new BlockPos(x, y, z), true)) {
                session.log(PcgEditorLogEntry.Severity.ERROR, "No spline point is selected.");
            }
            rebuildUi();
        }
    }

    private boolean moveSelectedControlPoint(BlockPos newPos, boolean logChange) {
        return switch (session.getSelection()) {
            case REGION -> moveSelectedRegionCorner(newPos, logChange);
            case SPLINE_POINT -> moveSelectedSplinePoint(newPos, logChange);
            default -> false;
        };
    }

    private boolean moveSelectedRegionCorner(BlockPos newPos, boolean logChange) {
        BlockPos currentCorner = session.getSelectedRegionCorner();
        int cornerIndex = session.getSelectedRegionCornerIndex();
        if (currentCorner == null || cornerIndex < 0 || currentCorner.equals(newPos)) {
            return false;
        }
        sendCommand("blockwright region " + (cornerIndex == 0 ? "setpos1 " : "setpos2 ")
                + newPos.getX() + " " + newPos.getY() + " " + newPos.getZ());
        session.setSelection(PcgEditorSelection.REGION);
        session.setSelectedRegionCornerIndex(cornerIndex);
        if (logChange) {
            session.log(PcgEditorLogEntry.Severity.INFO, "Moved Box Region_01 corner P" + (cornerIndex + 1) + ".");
        }
        return true;
    }

    private boolean moveSelectedSplinePoint(BlockPos newPos, boolean logChange) {
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        int pointIndex = session.getSelectedSplinePointIndex();
        if (pointIndex < 0 || pointIndex >= spline.getPoints().size() || newPos.equals(spline.getPoints().get(pointIndex))) {
            return false;
        }
        List<BlockPos> rebuiltPoints = new ArrayList<>(spline.getPoints());
        rebuiltPoints.set(pointIndex, newPos);
        sendCommand("blockwright spline clear");
        for (BlockPos point : rebuiltPoints) {
            sendCommand("blockwright spline addpos " + point.getX() + " " + point.getY() + " " + point.getZ());
        }
        session.setSelection(PcgEditorSelection.SPLINE_POINT);
        session.setSelectedSplinePointIndex(pointIndex);
        if (logChange) {
            session.log(PcgEditorLogEntry.Severity.INFO, "Moved spline point #" + pointIndex + ".");
        }
        return true;
    }

    private void logControlPointMove() {
        if (session.getSelection() == PcgEditorSelection.REGION) {
            int cornerIndex = session.getSelectedRegionCornerIndex();
            if (cornerIndex >= 0) {
                session.log(PcgEditorLogEntry.Severity.INFO, "Moved Box Region_01 corner P" + (cornerIndex + 1) + ".");
            }
            return;
        }
        if (session.getSelection() == PcgEditorSelection.SPLINE_POINT && session.getSelectedSplinePointIndex() >= 0) {
            session.log(PcgEditorLogEntry.Severity.INFO, "Moved spline point #" + session.getSelectedSplinePointIndex() + ".");
        }
    }

    private BlockPos getSelectedControlPoint() {
        return switch (session.getSelection()) {
            case REGION -> session.getSelectedRegionCorner();
            case SPLINE_POINT -> session.getSelectedSplinePoint();
            default -> null;
        };
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
        return isInputCompatible(describePresetMode(preset));
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

    private int snapAxisOffset(double worldOffset) {
        int snap = Math.max(1, session.getSnapStep());
        return Math.round((float) (worldOffset / snap)) * snap;
    }

    private double distanceToSegment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= 1.0E-6D) {
            return Math.hypot(px - ax, py - ay);
        }
        double t = ((px - ax) * dx + (py - ay) * dy) / lengthSquared;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double nearestX = ax + dx * t;
        double nearestY = ay + dy * t;
        return Math.hypot(px - nearestX, py - nearestY);
    }

    private void exportRegionAsModule() {
        if (!ClientSelectionState.getRegionSelection().isComplete()) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Export requires a complete Box Region.");
            return;
        }
        String raw = moduleExportField == null ? session.getExportModuleId() : moduleExportField.value;
        String moduleId = sanitizeModuleId(raw);
        if (moduleId.isBlank()) {
            session.log(PcgEditorLogEntry.Severity.ERROR, "Module id cannot be empty.");
            return;
        }
        session.setExportModuleId(moduleId);
        runAction("blockwright export-schem " + moduleId, PcgEditorLogEntry.Severity.SUCCESS, "Exported region as module " + moduleId + ".");
        rebuildUi();
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
            if (!moduleMatchesStyle(module)) {
                continue;
            }
            if (!moduleMatchesCategory(module)) {
                continue;
            }
            if (!moduleMatchesModStatus(module)) {
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

    private boolean moduleMatchesStyle(ModuleDefinition module) {
        String styleQuery = normalize(session.getModuleStyleQuery());
        return styleQuery.isBlank() || normalize(module.style).contains(styleQuery);
    }

    private boolean moduleMatchesCategory(ModuleDefinition module) {
        String categoryQuery = normalize(session.getModuleCategoryQuery());
        return categoryQuery.isBlank() || normalize(module.category).contains(categoryQuery);
    }

    private boolean moduleMatchesModStatus(ModuleDefinition module) {
        String filter = normalize(session.getModuleModStatusFilter());
        if (filter.isBlank() || "all".equals(filter)) {
            return true;
        }
        boolean missingMods = moduleHasMissingMods(module);
        if ("missing".equals(filter)) {
            return missingMods;
        }
        if ("available".equals(filter)) {
            return !missingMods;
        }
        return true;
    }

    private boolean moduleHasMissingMods(ModuleDefinition module) {
        if (module == null || module.schematicData == null) {
            return false;
        }
        for (String modId : module.schematicData.getRequiredMods()) {
            if (!modId.isBlank() && !"minecraft".equals(modId) && !Platform.isModLoaded(modId)) {
                return true;
            }
        }
        return false;
    }

    private List<ValidationIssue> moduleValidationIssues(LoadedPack pack, ModuleDefinition module) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (pack == null || module == null) {
            return issues;
        }
        for (ValidationIssue issue : pack.getValidationReport().getIssues()) {
            if (issue.getLocation().equals(module.id)) {
                issues.add(issue);
                continue;
            }
            if (module.sourcePath != null && issue.getLocation().contains(module.sourcePath.getFileName().toString())) {
                issues.add(issue);
                continue;
            }
            if (module.schematic != null && issue.getMessage().contains(module.schematic)) {
                issues.add(issue);
            }
        }
        return issues;
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
                "Entities: " + blockEntities,
                "State: " + session.getPreviewState().name()
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

    private String moduleModStatusLabel() {
        return "Mods: " + safe(session.getModuleModStatusFilter());
    }

    private boolean isTransformSelection() {
        return session.getSelection() == PcgEditorSelection.REGION || session.getSelection() == PcgEditorSelection.SPLINE_POINT;
    }

    private BlockPos currentTransformOrigin() {
        if (session.getSelection() == PcgEditorSelection.REGION) {
            BlockPos selectedCorner = session.getSelectedRegionCorner();
            return selectedCorner == null ? BlockPos.ZERO : selectedCorner;
        }
        if (session.getSelection() == PcgEditorSelection.SPLINE_POINT) {
            return session.getSelectedSplinePoint();
        }
        return BlockPos.ZERO;
    }

    private String toolLabel(PcgEditorTool tool) {
        return switch (tool) {
            case BOX_REGION -> "BOX\nREGION";
            case MODULE_LIBRARY -> "MODULE\nLIBRARY";
            case PAINT_MASK -> "PAINT /\nMASK";
            default -> tool.getTitle().toUpperCase(Locale.ROOT);
        };
    }

    private int computeToolButtonHeight() {
        return PcgToolPaletteLayout.computeButtonHeight(uiMetrics, leftBar.height, visibleToolButtonCount());
    }

    private int visibleToolButtonCount() {
        int count = 0;
        for (PcgEditorTool tool : PcgEditorTool.values()) {
            if (shouldShowToolButton(tool)) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private boolean shouldShowToolButton(PcgEditorTool tool) {
        return tool != PcgEditorTool.SELECT && tool != PcgEditorTool.TRANSFORM;
    }

    private PcgTopBarLayoutSpec computeTopBarLayout() {
        return PcgTopBarLayoutSpec.compute(uiMetrics, uiWidth, topBar.x, topBar.right(), this.font);
    }

    private void updateUiMetrics() {
        int availableWidth = Math.max(1, this.width);
        int availableHeight = Math.max(1, this.height);
        int minimumWidth = minimumLayoutWidth();
        int minimumHeight = minimumLayoutHeight();
        uiScale = Math.min(1.0D, Math.min((double) availableWidth / minimumWidth, (double) availableHeight / minimumHeight));
        uiWidth = Math.max(minimumWidth, (int) Math.floor(availableWidth / uiScale));
        uiHeight = Math.max(minimumHeight, (int) Math.floor(availableHeight / uiScale));
        uiOffsetX = (availableWidth - uiWidth * uiScale) / 2.0D;
        uiOffsetY = (availableHeight - uiHeight * uiScale) / 2.0D;
        uiMetrics = PcgUiMetrics.from(uiWidth, uiHeight, this.font.lineHeight);
    }

    private int minimumLayoutWidth() {
        int unit = metricUnit();
        int gap = metricGap(unit);
        int leftPanel = unit * 8;
        int viewportArea = unit * 28;
        int detailsPanel = unit * 18;
        int previewPanel = unit * 14;
        return leftPanel + viewportArea + detailsPanel + previewPanel + gap * 4 + OUTER_PAD * 2;
    }

    private int minimumLayoutHeight() {
        int unit = metricUnit();
        int gap = metricGap(unit);
        int topBarHeight = unit * 4;
        int contentHeight = unit * 18;
        int bottomBarHeight = unit * 8;
        return topBarHeight + contentHeight + bottomBarHeight + gap * 2 + OUTER_PAD * 2;
    }

    private int metricUnit() {
        return Math.max(10, this.font.lineHeight + 3);
    }

    private int metricGap(int unit) {
        return Math.max(1, unit / 6);
    }

    private void beginUi(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate((float) uiOffsetX, (float) uiOffsetY, 0.0F);
        guiGraphics.pose().scale((float) uiScale, (float) uiScale, 1.0F);
    }

    private void endUi(GuiGraphics guiGraphics) {
        guiGraphics.pose().popPose();
    }

    private double toUiX(double mouseX) {
        return (mouseX - uiOffsetX) / uiScale;
    }

    private double toUiY(double mouseY) {
        return (mouseY - uiOffsetY) / uiScale;
    }

    private void enableUiScissor(GuiGraphics guiGraphics, LayoutRect rect) {
        int left = clamp((int) Math.floor(uiOffsetX + rect.x * uiScale), 0, this.width);
        int top = clamp((int) Math.floor(uiOffsetY + rect.y * uiScale), 0, this.height);
        int right = clamp((int) Math.ceil(uiOffsetX + rect.right() * uiScale), 0, this.width);
        int bottom = clamp((int) Math.ceil(uiOffsetY + rect.bottom() * uiScale), 0, this.height);
        guiGraphics.enableScissor(left, top, right, bottom);
    }

    private void drawToolbarChip(GuiGraphics guiGraphics, LayoutRect rect, String label, String value, int valueColor) {
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), PANEL_HEADER);
        guiGraphics.fill(rect.x, rect.y, rect.right(), rect.y + 1, PANEL_BORDER);
        guiGraphics.fill(rect.x, rect.bottom() - 1, rect.right(), rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.x, rect.y, rect.x + 1, rect.bottom(), PANEL_BORDER);
        guiGraphics.fill(rect.right() - 1, rect.y, rect.right(), rect.bottom(), PANEL_BORDER);
        String prefix = label + ":";
        guiGraphics.drawString(this.font, prefix, rect.x + 8, rect.y + 8, TEXT_MUTED);
        int prefixWidth = this.font.width(prefix);
        guiGraphics.drawString(this.font, trimToWidth(value, rect.width - prefixWidth - 18), rect.x + 12 + prefixWidth, rect.y + 8, valueColor);
    }

    private int transformSectionHeight() {
        return PcgInspectorLayout.transformSectionHeight(uiMetrics, session.getSelection());
    }

    private int parameterRowsHeight() {
        return PcgInspectorLayout.parameterRowsHeight(uiMetrics, countExposedParameters());
    }

    private int validationContentHeight() {
        return PcgInspectorLayout.validationContentHeight(uiMetrics, getWarningSummary().size());
    }

    private int measureInspectorContentHeight() {
        return PcgInspectorLayout.measureContentHeight(uiMetrics, session.getSelection(),
                countExposedParameters(), getWarningSummary().size());
    }

    private int countExposedParameters() {
        PresetDefinition preset = session.getSelectedPreset();
        if (preset == null) {
            return 0;
        }
        int count = 0;
        for (PresetParameterDefinition parameter : preset.parameters.values()) {
            if (parameter.exposed) {
                count++;
            }
        }
        return count;
    }

    private void applyClipVisibility(EditorField field, LayoutRect clip) {
        field.visible = rectIntersects(field.bounds, clip);
        if (!field.visible && field.focused) {
            clearFocus();
        }
    }

    private void applyClipVisibility(EditorButton button, LayoutRect clip) {
        button.visible = rectIntersects(button.bounds, clip);
    }

    private boolean rectIntersects(LayoutRect target, LayoutRect clip) {
        return target != null && clip != null
                && target.x < clip.right()
                && target.right() > clip.x
                && target.y < clip.bottom()
                && target.bottom() > clip.y;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampPitch(float value) {
        return Math.max(-89.9F, Math.min(89.9F, value));
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

    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text;
        while (!remaining.isEmpty()) {
            String line = this.font.plainSubstrByWidth(remaining, width);
            if (line.isEmpty()) {
                break;
            }
            lines.add(line);
            remaining = remaining.substring(line.length()).stripLeading();
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private final class EditorButton {
        private final String id;
        private final LayoutRect bounds;
        private final Runnable onPress;
        private String label;
        private boolean selected;
        private boolean danger;
        private boolean enabled = true;
        private boolean visible = true;

        private EditorButton(String id, LayoutRect bounds, String label, boolean selected, boolean danger, Runnable onPress) {
            this.id = id;
            this.bounds = bounds;
            this.label = label;
            this.selected = selected;
            this.danger = danger;
            this.onPress = onPress;
        }

        private boolean contains(double mouseX, double mouseY) {
            return bounds.contains(mouseX, mouseY);
        }

        private void press() {
            if (!enabled || onPress == null) {
                return;
            }
            onPress.run();
        }

        private void draw(GuiGraphics guiGraphics, int mouseX, int mouseY) {
            int fill = !enabled ? 0x9930363F : selected ? BUTTON_ACTIVE : danger ? BUTTON_DANGER : BUTTON_BG;
            if (enabled && contains(mouseX, mouseY) && !selected) {
                fill = danger ? 0xFF814444 : BUTTON_HOVER;
            }
            guiGraphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), fill);
            guiGraphics.fill(bounds.x, bounds.y, bounds.right(), bounds.y + 1, PANEL_BORDER);
            guiGraphics.fill(bounds.x, bounds.bottom() - 1, bounds.right(), bounds.bottom(), PANEL_BORDER);
            guiGraphics.fill(bounds.x, bounds.y, bounds.x + 1, bounds.bottom(), PANEL_BORDER);
            guiGraphics.fill(bounds.right() - 1, bounds.y, bounds.right(), bounds.bottom(), PANEL_BORDER);
            int textColor = enabled ? TEXT_BRIGHT : TEXT_MUTED;
            String[] lines = label.split("\\n");
            int lineHeight = font.lineHeight + 1;
            int totalHeight = lines.length * lineHeight - 1;
            int textY = bounds.y + Math.max(6, (bounds.height - totalHeight) / 2);
            for (String line : lines) {
                int textWidth = font.width(line);
                guiGraphics.drawString(font, line, bounds.x + Math.max(6, (bounds.width - textWidth) / 2), textY, textColor);
                textY += lineHeight;
            }
        }
    }

    private final class EditorField {
        private final String id;
        private final Consumer<String> onChange;
        private final boolean integerOnly;
        private LayoutRect bounds;
        private String value;
        private boolean focused;
        private boolean visible = true;
        private int caret;

        private EditorField(String id, LayoutRect bounds, String value, boolean integerOnly, Consumer<String> onChange) {
            this.id = id;
            this.bounds = bounds;
            this.value = value == null ? "" : value;
            this.integerOnly = integerOnly;
            this.onChange = onChange;
            this.caret = this.value.length();
        }

        private boolean contains(double mouseX, double mouseY) {
            return bounds.contains(mouseX, mouseY);
        }

        private void draw(GuiGraphics guiGraphics) {
            guiGraphics.fill(bounds.x, bounds.y, bounds.right(), bounds.bottom(), FIELD_BG);
            int border = focused ? TEXT_BLUE : FIELD_BORDER;
            guiGraphics.fill(bounds.x, bounds.y, bounds.right(), bounds.y + 1, border);
            guiGraphics.fill(bounds.x, bounds.bottom() - 1, bounds.right(), bounds.bottom(), border);
            guiGraphics.fill(bounds.x, bounds.y, bounds.x + 1, bounds.bottom(), border);
            guiGraphics.fill(bounds.right() - 1, bounds.y, bounds.right(), bounds.bottom(), border);
            String visibleText = font.plainSubstrByWidth(value, bounds.width - 12);
            guiGraphics.drawString(font, visibleText, bounds.x + 6, bounds.y + 6, TEXT_BRIGHT);
            if (focused && ((System.currentTimeMillis() / 400L) & 1L) == 0L) {
                int caretWidth = font.width(font.plainSubstrByWidth(value.substring(0, Math.min(caret, value.length())), bounds.width - 12));
                int caretX = Math.min(bounds.right() - 6, bounds.x + 6 + caretWidth);
                guiGraphics.fill(caretX, bounds.y + 4, caretX + 1, bounds.bottom() - 4, TEXT_BRIGHT);
            }
        }

        private boolean keyPressed(int keyCode, int modifiers) {
            if (hasControlDown() && keyCode == GLFW.GLFW_KEY_V) {
                insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (caret > 0 && !value.isEmpty()) {
                    value = value.substring(0, caret - 1) + value.substring(caret);
                    caret--;
                    onChange.accept(value);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (caret < value.length()) {
                    value = value.substring(0, caret) + value.substring(caret + 1);
                    onChange.accept(value);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                caret = Math.max(0, caret - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                caret = Math.min(value.length(), caret + 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_HOME) {
                caret = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_END) {
                caret = value.length();
                return true;
            }
            return false;
        }

        private boolean charTyped(char codePoint) {
            if (codePoint < 32 || codePoint == 127) {
                return false;
            }
            insertText(String.valueOf(codePoint));
            return true;
        }

        private void insertText(String rawText) {
            if (rawText == null || rawText.isEmpty()) {
                return;
            }
            StringBuilder accepted = new StringBuilder(rawText.length());
            for (int i = 0; i < rawText.length(); i++) {
                char value = rawText.charAt(i);
                if (integerOnly) {
                    if ((value >= '0' && value <= '9') || (value == '-' && caret == 0 && !this.value.startsWith("-"))) {
                        accepted.append(value);
                    }
                } else if (value >= 32 && value != 127) {
                    accepted.append(value);
                }
            }
            if (accepted.isEmpty()) {
                return;
            }
            this.value = this.value.substring(0, caret) + accepted + this.value.substring(caret);
            caret += accepted.length();
            onChange.accept(this.value);
        }
    }

    private record ParameterField(String key, int rowIndex, EditorField field) {
    }

    private record ScreenProjection(double screenX, double screenY, double depth) {
    }

    private record ViewportSelectionTarget(PcgEditorSelection selection, int regionCornerIndex, int splinePointIndex, String logMessage) {
    }

    private enum DragHandle {
        NONE,
        DETAILS,
        PREVIEW,
        BOTTOM
    }

    private enum CursorType {
        DEFAULT,
        HORIZONTAL_RESIZE,
        VERTICAL_RESIZE
    }

    private static final class LayoutRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private LayoutRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private int right() {
            return x + width;
        }

        private int bottom() {
            return y + height;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= right() && mouseY >= y && mouseY <= bottom();
        }
    }
}
