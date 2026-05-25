package top.huliawsl.blockwright.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preview.PreviewSeverity;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.selection.BoxRegionSelection;
import top.huliawsl.blockwright.selection.SplineSelection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PcgEditorSession {
    private static final int MAX_LOG_ENTRIES = 6;
    private static final String[] MODULE_KIND_FILTERS = {"all", "static_schem", "procedural", "compound"};
    private static final String[] MODULE_MOD_STATUS_FILTERS = {"all", "available", "missing"};
    private static final PcgEditorSession INSTANCE = new PcgEditorSession();

    private final Deque<PcgEditorLogEntry> logEntries = new ArrayDeque<>();
    private PcgEditorTool activeTool = PcgEditorTool.BOX_REGION;
    private PcgEditorSelection selection = PcgEditorSelection.NONE;
    private boolean navigating;
    private boolean open;
    private boolean cameraModeActive;
    private int selectedSplinePointIndex = -1;
    private String selectedPackId;
    private String selectedPresetId;
    private String selectedModuleId;
    private String moduleSearchQuery = "";
    private String moduleTagQuery = "";
    private String moduleStyleQuery = "";
    private String moduleCategoryQuery = "";
    private String moduleKindFilter = MODULE_KIND_FILTERS[0];
    private String moduleModStatusFilter = MODULE_MOD_STATUS_FILTERS[0];
    private String exportModuleId = "exported_module";
    private int moduleRotationQuarterTurns;
    private int modulePreviewZoomPercent = 100;
    private boolean showModuleBounds = true;
    private boolean showModuleConnectors = true;
    private boolean showModuleAir;
    private int snapStep = 1;
    private Vec3 cameraOrigin = Vec3.ZERO;
    private float cameraOriginYRot;
    private float cameraOriginXRot;
    private boolean originalNoPhysics;
    private boolean originalNoGravity;
    private boolean originalFlying;
    private boolean originalMayFly;
    private boolean originalMayBuild = true;
    private boolean originalInstabuild;
    private float originalFlyingSpeed = 0.05F;

    private PcgEditorSession() {
    }

    public static PcgEditorSession get() {
        return INSTANCE;
    }

    public void open() {
        if (open) {
            ensureDefaults();
            return;
        }
        open = true;
        ensureDefaults();
        log(PcgEditorLogEntry.Severity.INFO, "Editor opened.");
    }

    public void close() {
        if (!open) {
            return;
        }
        navigating = false;
        open = false;
        log(PcgEditorLogEntry.Severity.INFO, "Editor closed.");
    }

    public boolean isOpen() {
        return open;
    }

    public PcgEditorTool getActiveTool() {
        return activeTool;
    }

    public void setActiveTool(PcgEditorTool activeTool) {
        this.activeTool = activeTool;
    }

    public PcgEditorSelection getSelection() {
        return selection;
    }

    public void setSelection(PcgEditorSelection selection) {
        this.selection = selection;
    }

    public boolean isNavigating() {
        return navigating;
    }

    public void setNavigating(boolean navigating) {
        this.navigating = navigating;
    }

    public boolean isCameraModeActive() {
        return cameraModeActive;
    }

    public void enterCameraMode(Minecraft minecraft) {
        if (cameraModeActive || minecraft == null || minecraft.player == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        Abilities abilities = player.getAbilities();
        cameraOrigin = player.position();
        cameraOriginYRot = player.getYRot();
        cameraOriginXRot = player.getXRot();
        originalNoPhysics = player.noPhysics;
        originalNoGravity = player.isNoGravity();
        originalFlying = abilities.flying;
        originalMayFly = abilities.mayfly;
        originalMayBuild = abilities.mayBuild;
        originalInstabuild = abilities.instabuild;
        originalFlyingSpeed = abilities.getFlyingSpeed();

        cameraModeActive = true;
        player.noPhysics = true;
        player.setNoGravity(true);
        abilities.mayfly = true;
        abilities.flying = true;
        abilities.mayBuild = false;
        abilities.setFlyingSpeed(Math.max(0.08F, originalFlyingSpeed));
        player.onUpdateAbilities();
    }

    public void exitCameraMode(Minecraft minecraft) {
        cameraModeActive = false;
        navigating = false;
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        Abilities abilities = player.getAbilities();
        player.noPhysics = originalNoPhysics;
        player.setNoGravity(originalNoGravity);
        abilities.flying = originalFlying;
        abilities.mayfly = originalMayFly;
        abilities.mayBuild = originalMayBuild;
        abilities.instabuild = originalInstabuild;
        abilities.setFlyingSpeed(originalFlyingSpeed);
        player.onUpdateAbilities();
        player.setDeltaMovement(Vec3.ZERO);
        player.moveTo(cameraOrigin.x, cameraOrigin.y, cameraOrigin.z, cameraOriginYRot, cameraOriginXRot);
    }

    public int getSelectedSplinePointIndex() {
        return selectedSplinePointIndex;
    }

    public void setSelectedSplinePointIndex(int selectedSplinePointIndex) {
        this.selectedSplinePointIndex = selectedSplinePointIndex;
    }

    public String getSelectedPackId() {
        return selectedPackId;
    }

    public void setSelectedPackId(String selectedPackId) {
        this.selectedPackId = selectedPackId;
    }

    public String getSelectedPresetId() {
        return selectedPresetId;
    }

    public void setSelectedPresetId(String selectedPresetId) {
        this.selectedPresetId = selectedPresetId;
    }

    public String getSelectedModuleId() {
        return selectedModuleId;
    }

    public void setSelectedModuleId(String selectedModuleId) {
        this.selectedModuleId = selectedModuleId;
    }

    public String getModuleSearchQuery() {
        return moduleSearchQuery;
    }

    public void setModuleSearchQuery(String moduleSearchQuery) {
        this.moduleSearchQuery = moduleSearchQuery == null ? "" : moduleSearchQuery;
    }

    public String getModuleTagQuery() {
        return moduleTagQuery;
    }

    public void setModuleTagQuery(String moduleTagQuery) {
        this.moduleTagQuery = moduleTagQuery == null ? "" : moduleTagQuery;
    }

    public String getModuleKindFilter() {
        return moduleKindFilter;
    }

    public void cycleModuleKindFilter() {
        for (int i = 0; i < MODULE_KIND_FILTERS.length; i++) {
            if (MODULE_KIND_FILTERS[i].equals(moduleKindFilter)) {
                moduleKindFilter = MODULE_KIND_FILTERS[(i + 1) % MODULE_KIND_FILTERS.length];
                return;
            }
        }
        moduleKindFilter = MODULE_KIND_FILTERS[0];
    }

    public String getModuleStyleQuery() {
        return moduleStyleQuery;
    }

    public void setModuleStyleQuery(String moduleStyleQuery) {
        this.moduleStyleQuery = moduleStyleQuery == null ? "" : moduleStyleQuery;
    }

    public String getModuleCategoryQuery() {
        return moduleCategoryQuery;
    }

    public void setModuleCategoryQuery(String moduleCategoryQuery) {
        this.moduleCategoryQuery = moduleCategoryQuery == null ? "" : moduleCategoryQuery;
    }

    public String getModuleModStatusFilter() {
        return moduleModStatusFilter;
    }

    public void cycleModuleModStatusFilter() {
        for (int i = 0; i < MODULE_MOD_STATUS_FILTERS.length; i++) {
            if (MODULE_MOD_STATUS_FILTERS[i].equals(moduleModStatusFilter)) {
                moduleModStatusFilter = MODULE_MOD_STATUS_FILTERS[(i + 1) % MODULE_MOD_STATUS_FILTERS.length];
                return;
            }
        }
        moduleModStatusFilter = MODULE_MOD_STATUS_FILTERS[0];
    }

    public String getExportModuleId() {
        return exportModuleId;
    }

    public void setExportModuleId(String exportModuleId) {
        this.exportModuleId = exportModuleId == null || exportModuleId.isBlank() ? "exported_module" : exportModuleId.trim();
    }

    public int getModuleRotationQuarterTurns() {
        return moduleRotationQuarterTurns;
    }

    public void rotateModulePreview() {
        moduleRotationQuarterTurns = (moduleRotationQuarterTurns + 1) & 3;
    }

    public int getModulePreviewZoomPercent() {
        return modulePreviewZoomPercent;
    }

    public void zoomModulePreview(int deltaPercent) {
        modulePreviewZoomPercent = Math.max(50, Math.min(200, modulePreviewZoomPercent + deltaPercent));
    }

    public boolean isShowModuleBounds() {
        return showModuleBounds;
    }

    public void setShowModuleBounds(boolean showModuleBounds) {
        this.showModuleBounds = showModuleBounds;
    }

    public boolean isShowModuleConnectors() {
        return showModuleConnectors;
    }

    public void setShowModuleConnectors(boolean showModuleConnectors) {
        this.showModuleConnectors = showModuleConnectors;
    }

    public boolean isShowModuleAir() {
        return showModuleAir;
    }

    public void setShowModuleAir(boolean showModuleAir) {
        this.showModuleAir = showModuleAir;
    }

    public int getSnapStep() {
        return snapStep;
    }

    public void cycleSnapStep() {
        snapStep = switch (snapStep) {
            case 1 -> 4;
            case 4 -> 16;
            default -> 1;
        };
    }

    public List<PcgEditorLogEntry> getLogEntries() {
        return new ArrayList<>(logEntries);
    }

    public void log(PcgEditorLogEntry.Severity severity, String message) {
        logEntries.addFirst(new PcgEditorLogEntry(severity, message, System.currentTimeMillis()));
        while (logEntries.size() > MAX_LOG_ENTRIES) {
            logEntries.removeLast();
        }
    }

    public void ensureDefaults() {
        if (selectedPackId == null) {
            LoadedPack pack = getSelectedPack();
            if (pack != null) {
                selectedPackId = pack.getMetadata().id;
            }
        }
        if (selectedPresetId == null) {
            PresetDefinition preset = getSelectedPreset();
            if (preset != null) {
                selectedPresetId = preset.id;
            }
        }
        if (selectedModuleId == null) {
            ModuleDefinition module = getSelectedModule();
            if (module != null) {
                selectedModuleId = module.id;
            }
        }
        if (exportModuleId == null || exportModuleId.isBlank()) {
            exportModuleId = "exported_module";
        }
    }

    public LoadedPack getSelectedPack() {
        List<LoadedPack> packs = Blockwright.getPackManager().getLoadedPacks();
        if (packs.isEmpty()) {
            return null;
        }
        if (selectedPackId != null) {
            for (LoadedPack pack : packs) {
                if (selectedPackId.equals(pack.getMetadata().id)) {
                    return pack;
                }
            }
        }
        LoadedPack first = packs.get(0);
        selectedPackId = first.getMetadata().id;
        return first;
    }

    public PresetDefinition getSelectedPreset() {
        LoadedPack pack = getSelectedPack();
        if (pack == null || pack.getPresets().isEmpty()) {
            return null;
        }
        if (selectedPresetId != null && pack.getPresets().containsKey(selectedPresetId)) {
            return pack.getPresets().get(selectedPresetId);
        }
        PresetDefinition preset = pack.getPresets().values().iterator().next();
        selectedPresetId = preset.id;
        return preset;
    }

    public ModuleDefinition getSelectedModule() {
        LoadedPack pack = getSelectedPack();
        if (pack == null || pack.getModules().isEmpty()) {
            return null;
        }
        if (selectedModuleId != null && pack.getModules().containsKey(selectedModuleId)) {
            return pack.getModules().get(selectedModuleId);
        }
        ModuleDefinition module = pack.getModules().values().iterator().next();
        selectedModuleId = module.id;
        return module;
    }

    public PcgEditorPreviewState getPreviewState() {
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
        if (previewPlan == null) {
            return PcgEditorPreviewState.NONE;
        }
        if (previewPlan.isStale()) {
            return PcgEditorPreviewState.DIRTY;
        }
        return switch (previewPlan.getOverallSeverity()) {
            case WARNING -> PcgEditorPreviewState.WARNING;
            case ERROR -> PcgEditorPreviewState.ERROR;
            default -> PcgEditorPreviewState.VALID;
        };
    }

    public String getSelectionLabel() {
        return switch (selection) {
            case REGION -> "Box Region_01";
            case SPLINE -> "RoadSpline_01";
            case SPLINE_POINT -> "Spline Point #" + selectedSplinePointIndex;
            case PREVIEW -> "Structure Preview";
            case MODULE -> selectedModuleId == null ? "Module" : selectedModuleId;
            case NONE -> "None";
        };
    }

    public Vec3 getSelectionFocus() {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        return switch (selection) {
            case REGION -> region.isComplete() ? region.toAabb().getCenter() : null;
            case SPLINE_POINT -> {
                if (selectedSplinePointIndex >= 0 && selectedSplinePointIndex < spline.getPoints().size()) {
                    yield Vec3.atCenterOf(spline.getPoints().get(selectedSplinePointIndex));
                }
                yield null;
            }
            case SPLINE -> spline.getPoints().isEmpty() ? null : averageSplineCenter(spline);
            case PREVIEW -> {
                PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();
                yield previewPlan == null ? null : previewPlan.getBounds().getCenter();
            }
            default -> null;
        };
    }

    public AABB getSelectedBounds() {
        BoxRegionSelection region = ClientSelectionState.getRegionSelection();
        return selection == PcgEditorSelection.REGION && region.isComplete() ? region.toAabb() : null;
    }

    public BlockPos getSelectedSplinePoint() {
        SplineSelection spline = ClientSelectionState.getSplineSelection();
        if (selectedSplinePointIndex < 0 || selectedSplinePointIndex >= spline.getPoints().size()) {
            return null;
        }
        return spline.getPoints().get(selectedSplinePointIndex);
    }

    public void selectRegionIfPresent() {
        if (ClientSelectionState.getRegionSelection().isComplete()) {
            selection = PcgEditorSelection.REGION;
        }
    }

    public void selectSplineIfPresent() {
        if (!ClientSelectionState.getSplineSelection().getPoints().isEmpty()) {
            selection = PcgEditorSelection.SPLINE;
            if (selectedSplinePointIndex >= ClientSelectionState.getSplineSelection().getPoints().size()) {
                selectedSplinePointIndex = ClientSelectionState.getSplineSelection().getPoints().size() - 1;
            }
        }
    }

    public void markDirty(String reason) {
        ClientPreviewState.markStale();
        if (reason != null && !reason.isBlank()) {
            log(PcgEditorLogEntry.Severity.DEBUG, reason);
        }
    }

    public void syncSelectionDefaults() {
        if (selection == PcgEditorSelection.REGION && !ClientSelectionState.getRegionSelection().isComplete()) {
            selection = PcgEditorSelection.NONE;
        }
        if ((selection == PcgEditorSelection.SPLINE || selection == PcgEditorSelection.SPLINE_POINT)
                && ClientSelectionState.getSplineSelection().getPoints().isEmpty()) {
            selection = PcgEditorSelection.NONE;
            selectedSplinePointIndex = -1;
        }
        if (selection == PcgEditorSelection.NONE) {
            if (ClientSelectionState.getRegionSelection().isComplete()) {
                selection = PcgEditorSelection.REGION;
            } else if (!ClientSelectionState.getSplineSelection().getPoints().isEmpty()) {
                selection = PcgEditorSelection.SPLINE;
            }
        }
        if (selection == PcgEditorSelection.SPLINE && selectedSplinePointIndex < 0
                && !ClientSelectionState.getSplineSelection().getPoints().isEmpty()) {
            selectedSplinePointIndex = ClientSelectionState.getSplineSelection().getPoints().size() - 1;
        }
    }

    public int getUndoCount() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }
        return 0;
    }

    private static Vec3 averageSplineCenter(SplineSelection spline) {
        double sumX = 0.0D;
        double sumY = 0.0D;
        double sumZ = 0.0D;
        for (BlockPos point : spline.getPoints()) {
            sumX += point.getX() + 0.5D;
            sumY += point.getY() + 0.5D;
            sumZ += point.getZ() + 0.5D;
        }
        double size = spline.getPoints().size();
        return new Vec3(sumX / size, sumY / size, sumZ / size);
    }
}
