package top.huliawsl.blockwright.client;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.module.model.ModuleConnector;
import top.huliawsl.blockwright.module.model.ModuleDefinition;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.pack.SpongeSchematicData;
import top.huliawsl.blockwright.util.ValidationIssue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModuleLibraryScreen extends Screen {
    private static final int PANEL_BG = 0xB0151A21;
    private static final int PANEL_BORDER = 0xFF39424D;
    private static final int TEXT_MUTED = 0xFF99A2AD;
    private static final int TEXT_BRIGHT = 0xFFF4F7FA;
    private static final int STATUS_OK = 0xFF69D18A;
    private static final int STATUS_WARN = 0xFFF2C94C;
    private static final int STATUS_ERROR = 0xFFF25F5C;
    private static final int STATUS_INFO = 0xFF5DA9E9;

    private static final int OUTER_PADDING = 14;
    private static final int PANEL_GAP = 10;
    private static final int PANEL_INSET = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_HEIGHT = 18;
    private static final int LIST_ROW_HEIGHT = 24;
    private static final int DETAIL_ROW_HEIGHT = 12;

    private static int selectedPackIndex;
    private static String selectedModuleId;
    private static KindFilter kindFilter = KindFilter.ALL;
    private static int rotationQuarterTurns;
    private static boolean showBounds = true;
    private static boolean showConnectors = true;
    private static boolean showAir;
    private static String searchQuery = "";
    private static String tagQuery = "";

    private final Screen parent;

    private EditBox searchBox;
    private EditBox tagFilterBox;
    private Button kindButton;
    private Button rotateButton;
    private Button boundsButton;
    private Button connectorsButton;
    private Button airButton;

    private int contentLeft;
    private int topY;
    private int panelBottom;
    private int leftWidth;
    private int rightWidth;
    private int rightX;
    private int leftInnerX;
    private int rightInnerX;
    private int listTop;
    private int listBottom;
    private int previewTop;
    private int previewBottom;
    private int previewHeight;
    private int detailTop;
    private int detailBottom;
    private int detailScroll;
    private int maxDetailScroll;
    private int listScroll;
    private int maxListScroll;

    public ModuleLibraryScreen(Screen parent, String initialPackId) {
        super(Component.literal("Module Library"));
        this.parent = parent;
        syncPackSelection(initialPackId);
    }

    @Override
    protected void init() {
        clearWidgets();
        initLayout();

        int headerButtonWidth = (leftWidth - PANEL_INSET * 2 - PANEL_GAP * 3) / 4;
        int searchY = topY + 46;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cyclePack(-1))
                .bounds(leftInnerX, searchY, 20, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cyclePack(1))
                .bounds(leftInnerX + 24, searchY, 20, BUTTON_HEIGHT)
                .build());

        searchBox = new EditBox(this.font, leftInnerX + 50, searchY + 1, leftWidth - PANEL_INSET * 2 - 50, FIELD_HEIGHT, Component.literal("Search"));
        searchBox.setValue(searchQuery);
        searchBox.setBordered(true);
        searchBox.setTextColor(TEXT_BRIGHT);
        searchBox.setResponder(value -> {
            searchQuery = value;
            resetListScroll();
        });
        addRenderableWidget(searchBox);

        int filterY = searchY + BUTTON_HEIGHT + 8;
        kindButton = addRenderableWidget(Button.builder(Component.literal("Kind: " + kindFilter.label), button -> cycleKindFilter())
                .bounds(leftInnerX, filterY, headerButtonWidth + 18, BUTTON_HEIGHT)
                .build());
        tagFilterBox = new EditBox(this.font, leftInnerX + headerButtonWidth + 26, filterY + 1,
                leftWidth - PANEL_INSET * 2 - headerButtonWidth - 26, FIELD_HEIGHT, Component.literal("Tag filter"));
        tagFilterBox.setBordered(true);
        tagFilterBox.setTextColor(TEXT_BRIGHT);
        tagFilterBox.setValue(tagQuery);
        tagFilterBox.setResponder(value -> {
            tagQuery = value;
            resetListScroll();
        });
        addRenderableWidget(tagFilterBox);

        int detailButtonWidth = (rightWidth - PANEL_INSET * 2 - PANEL_GAP) / 2;
        int actionY = topY + 24;
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> closeToParent())
                .bounds(rightInnerX, actionY, detailButtonWidth, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Open Main"), button -> Minecraft.getInstance().setScreen(new BlockwrightMainScreen()))
                .bounds(rightInnerX + detailButtonWidth + PANEL_GAP, actionY, detailButtonWidth, BUTTON_HEIGHT)
                .build());

        int toggleY = actionY + BUTTON_HEIGHT + 8;
        int toggleWidth = (rightWidth - PANEL_INSET * 2 - PANEL_GAP * 3) / 4;
        rotateButton = addRenderableWidget(Button.builder(Component.literal("Rotate"), button -> rotatePreview())
                .bounds(rightInnerX, toggleY, toggleWidth, BUTTON_HEIGHT)
                .build());
        boundsButton = addRenderableWidget(Button.builder(Component.literal("Bounds"), button -> toggleBounds())
                .bounds(rightInnerX + toggleWidth + PANEL_GAP, toggleY, toggleWidth, BUTTON_HEIGHT)
                .build());
        connectorsButton = addRenderableWidget(Button.builder(Component.literal("Connectors"), button -> toggleConnectors())
                .bounds(rightInnerX + (toggleWidth + PANEL_GAP) * 2, toggleY, toggleWidth, BUTTON_HEIGHT)
                .build());
        airButton = addRenderableWidget(Button.builder(Component.literal("Air"), button -> toggleAir())
                .bounds(rightInnerX + (toggleWidth + PANEL_GAP) * 3, toggleY, toggleWidth, BUTTON_HEIGHT)
                .build());

        updateToggleLabels();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        drawAtmosphere(guiGraphics);
        drawPanel(guiGraphics, contentLeft, topY, leftWidth, panelBottom - topY);
        drawPanel(guiGraphics, rightX, topY, rightWidth, panelBottom - topY);

        LoadedPack pack = getSelectedPack();
        List<ModuleDefinition> modules = getFilteredModules(pack);
        ModuleDefinition selectedModule = getSelectedModule(modules);
        List<String> detailLines = buildDetailLines(pack, selectedModule);
        updateScrollBounds(modules.size(), detailLines.size());

        guiGraphics.drawString(this.font, this.title, contentLeft + PANEL_INSET, topY - 12, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Modules", leftInnerX, topY + 8, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Preview", rightInnerX, topY + 8, TEXT_BRIGHT);

        guiGraphics.drawString(this.font,
                trimToWidth(pack == null ? "Pack: <none>" : "Pack: " + safePackName(pack), leftWidth - PANEL_INSET * 2),
                leftInnerX, topY + 28, TEXT_BRIGHT);
        guiGraphics.drawString(this.font,
                trimToWidth(pack == null ? "0 modules" : modules.size() + " visible / " + pack.getModules().size() + " total", leftWidth - PANEL_INSET * 2),
                leftInnerX, topY + 40, TEXT_MUTED);

        drawModuleList(guiGraphics, modules, selectedModule);
        ModuleSchematicPreviewRenderer.render(guiGraphics, rightInnerX, previewTop, rightWidth - PANEL_INSET * 2, previewHeight,
                selectedModule, rotationQuarterTurns, 100, showBounds, showConnectors, showAir);
        drawPreviewCaption(guiGraphics, selectedModule, rightInnerX, previewBottom + 6);
        drawDetailLines(guiGraphics, detailLines);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= contentLeft + 1 && mouseX <= contentLeft + leftWidth - 1
                && mouseY >= listTop && mouseY <= listBottom) {
            List<ModuleDefinition> modules = getFilteredModules(getSelectedPack());
            int index = (int) ((mouseY - listTop) / LIST_ROW_HEIGHT) + listScroll;
            if (index >= 0 && index < modules.size()) {
                selectedModuleId = modules.get(index).id;
                detailScroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= contentLeft && mouseX <= contentLeft + leftWidth && mouseY >= listTop && mouseY <= listBottom) {
            listScroll = clamp(listScroll - (int) Math.signum(delta), 0, maxListScroll);
            return true;
        }
        if (mouseX >= rightX && mouseX <= rightX + rightWidth && mouseY >= detailTop && mouseY <= detailBottom) {
            detailScroll = clamp(detailScroll - (int) Math.signum(delta), 0, maxDetailScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            closeToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void initLayout() {
        int layoutWidth = Math.min(this.width - OUTER_PADDING * 2, 1320);
        contentLeft = (this.width - layoutWidth) / 2;
        topY = 18;
        panelBottom = this.height - 18;

        int availableWidth = layoutWidth - PANEL_GAP;
        leftWidth = clamp(availableWidth * 34 / 100, 240, 360);
        rightWidth = availableWidth - leftWidth;
        rightX = contentLeft + leftWidth + PANEL_GAP;
        leftInnerX = contentLeft + PANEL_INSET;
        rightInnerX = rightX + PANEL_INSET;

        listTop = topY + 106;
        listBottom = panelBottom - 14;
        previewTop = topY + 74;
        previewHeight = Math.max(140, (panelBottom - topY) / 2 - 16);
        previewBottom = previewTop + previewHeight;
        detailTop = previewBottom + 40;
        detailBottom = panelBottom - 14;
    }

    private void drawModuleList(GuiGraphics guiGraphics, List<ModuleDefinition> modules, ModuleDefinition selectedModule) {
        int visibleRows = Math.max(1, (listBottom - listTop) / LIST_ROW_HEIGHT);
        int end = Math.min(modules.size(), listScroll + visibleRows);
        if (modules.isEmpty()) {
            guiGraphics.drawString(this.font, "No modules matched the current filters.", leftInnerX, listTop + 8, TEXT_MUTED);
            return;
        }

        int rowY = listTop;
        for (int i = listScroll; i < end; i++) {
            ModuleDefinition module = modules.get(i);
            boolean selected = selectedModule != null && module.id.equals(selectedModule.id);
            if (selected) {
                guiGraphics.fill(contentLeft + 1, rowY - 1, contentLeft + leftWidth - 1, rowY + LIST_ROW_HEIGHT - 1, 0x70313C4A);
            }
            guiGraphics.drawString(this.font, trimToWidth(module.id, leftWidth - PANEL_INSET * 2), leftInnerX, rowY, selected ? TEXT_BRIGHT : TEXT_MUTED);
            String suffix = module.moduleKind == null ? "unknown" : module.moduleKind;
            guiGraphics.drawString(this.font, trimToWidth(suffix, leftWidth - PANEL_INSET * 2), leftInnerX, rowY + 10, selected ? STATUS_INFO : TEXT_MUTED);
            rowY += LIST_ROW_HEIGHT;
        }

        if (maxListScroll > 0) {
            guiGraphics.drawString(this.font, "Scroll " + (listScroll + 1) + "/" + (maxListScroll + 1),
                    leftInnerX, panelBottom - 10, TEXT_MUTED);
        }
    }

    private void drawPreviewCaption(GuiGraphics guiGraphics, ModuleDefinition module, int x, int y) {
        if (module == null) {
            guiGraphics.drawString(this.font, "No module selected.", x, y, TEXT_MUTED);
            return;
        }
        guiGraphics.drawString(this.font, trimToWidth("Selected: " + module.id, rightWidth - PANEL_INSET * 2), x, y, TEXT_BRIGHT);
        guiGraphics.drawString(this.font, "Rot " + ((rotationQuarterTurns * 90) % 360) + "°", x, y + 12, TEXT_MUTED);
    }

    private void drawDetailLines(GuiGraphics guiGraphics, List<String> lines) {
        guiGraphics.drawString(this.font, "Details", rightInnerX, detailTop - 16, TEXT_BRIGHT);
        if (lines.isEmpty()) {
            guiGraphics.drawString(this.font, "No details available.", rightInnerX, detailTop, TEXT_MUTED);
            return;
        }

        int visibleRows = Math.max(1, (detailBottom - detailTop) / DETAIL_ROW_HEIGHT);
        int end = Math.min(lines.size(), detailScroll + visibleRows);
        int y = detailTop;
        for (int i = detailScroll; i < end; i++) {
            String line = lines.get(i);
            guiGraphics.drawString(this.font, trimToWidth(line, rightWidth - PANEL_INSET * 2), rightInnerX, y, detailColor(line));
            y += DETAIL_ROW_HEIGHT;
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

    private void cyclePack(int delta) {
        List<LoadedPack> packs = Blockwright.getPackManager().getLoadedPacks();
        if (packs.isEmpty()) {
            return;
        }
        selectedPackIndex = Math.floorMod(selectedPackIndex + delta, packs.size());
        selectedModuleId = null;
        resetListScroll();
    }

    private void cycleKindFilter() {
        kindFilter = kindFilter.next();
        kindButton.setMessage(Component.literal("Kind: " + kindFilter.label));
        resetListScroll();
    }

    private void rotatePreview() {
        rotationQuarterTurns = (rotationQuarterTurns + 1) & 3;
        updateToggleLabels();
    }

    private void toggleBounds() {
        showBounds = !showBounds;
        updateToggleLabels();
    }

    private void toggleConnectors() {
        showConnectors = !showConnectors;
        updateToggleLabels();
    }

    private void toggleAir() {
        showAir = !showAir;
        updateToggleLabels();
    }

    private void updateToggleLabels() {
        if (rotateButton != null) {
            rotateButton.setMessage(Component.literal("Rot " + ((rotationQuarterTurns * 90) % 360) + "°"));
        }
        if (boundsButton != null) {
            boundsButton.setMessage(Component.literal("Bounds " + onOff(showBounds)));
        }
        if (connectorsButton != null) {
            connectorsButton.setMessage(Component.literal("Conn " + onOff(showConnectors)));
        }
        if (airButton != null) {
            airButton.setMessage(Component.literal("Air " + onOff(showAir)));
        }
    }

    private List<ModuleDefinition> getFilteredModules(LoadedPack pack) {
        List<ModuleDefinition> modules = new ArrayList<>();
        if (pack == null) {
            return modules;
        }
        String query = normalize(searchBox == null ? "" : searchBox.getValue());
        String tag = normalize(tagFilterBox == null ? "" : tagFilterBox.getValue());
        for (ModuleDefinition module : pack.getModules().values()) {
            if (!kindFilter.matches(module)) {
                continue;
            }
            if (!tag.isBlank() && module.tags.stream().noneMatch(existing -> normalize(existing).contains(tag))) {
                continue;
            }
            if (!query.isBlank() && !matchesQuery(module, query)) {
                continue;
            }
            modules.add(module);
        }
        modules.sort(Comparator.comparing(module -> module.id));
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

    private ModuleDefinition getSelectedModule(List<ModuleDefinition> modules) {
        if (modules.isEmpty()) {
            selectedModuleId = null;
            return null;
        }
        if (selectedModuleId != null) {
            for (ModuleDefinition module : modules) {
                if (selectedModuleId.equals(module.id)) {
                    return module;
                }
            }
        }
        selectedModuleId = modules.get(0).id;
        return modules.get(0);
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

    private List<String> buildDetailLines(LoadedPack pack, ModuleDefinition module) {
        List<String> lines = new ArrayList<>();
        if (pack == null) {
            lines.add("ERROR: No preset pack is loaded.");
            return lines;
        }
        if (module == null) {
            lines.add("INFO: Choose a module from the list.");
            return lines;
        }

        lines.add("ID: " + safe(module.id));
        lines.add("Kind: " + safe(module.moduleKind));
        lines.add("Category: " + safe(module.category));
        lines.add("Style: " + safe(module.style));
        lines.add("Tags: " + joinList(module.tags));
        lines.add("Size: " + joinInts(module.size));
        lines.add("Weight: " + module.weight);
        lines.add("Rotations: " + joinInts(module.allowedRotations));
        lines.add("Schematic: " + safe(module.schematic));
        if (module.sourcePath != null) {
            lines.add("Source: " + pack.getRoot().relativize(module.sourcePath).toString().replace('\\', '/'));
        }

        SpongeSchematicData schematic = module.schematicData;
        if (schematic != null) {
            lines.add("Volume: " + schematic.getVolume());
            lines.add("Block Entities: " + schematic.getBlockEntities().size());
            lines.add("Required Mods: " + joinList(schematic.getRequiredMods()));
            List<String> missingMods = new ArrayList<>();
            for (String modId : schematic.getRequiredMods()) {
                if (!modId.isBlank() && !"minecraft".equals(modId) && !Platform.isModLoaded(modId)) {
                    missingMods.add(modId);
                }
            }
            lines.add((missingMods.isEmpty() ? "INFO" : "ERROR") + ": Missing runtime mods: " + joinList(missingMods));
            if (!schematic.getMissingBlockStates().isEmpty()) {
                lines.add("ERROR: Missing blockstates: " + String.join(", ", schematic.getMissingBlockStates()));
            }
        } else if ("static_schem".equals(module.moduleKind)) {
            lines.add("ERROR: Schematic data could not be loaded.");
        }

        if (module.connectors.isEmpty()) {
            lines.add("INFO: No connectors defined.");
        } else {
            lines.add("Connectors:");
            for (ModuleConnector connector : module.connectors) {
                lines.add(" - " + safe(connector.id) + " / " + safe(connector.direction)
                        + " / off " + joinInts(connector.offset) + " / size " + joinInts(connector.size));
            }
        }

        for (ValidationIssue issue : pack.getValidationReport().getIssues()) {
            if (matchesIssue(module, issue)) {
                lines.add(issue.getSeverity() + ": " + issue.getLocation() + " - " + issue.getMessage());
            }
        }
        return lines;
    }

    private boolean matchesIssue(ModuleDefinition module, ValidationIssue issue) {
        if (issue.getLocation().equals(module.id)) {
            return true;
        }
        if (module.sourcePath != null && issue.getLocation().contains(module.sourcePath.getFileName().toString())) {
            return true;
        }
        return module.schematic != null && issue.getMessage().contains(module.schematic);
    }

    private void updateScrollBounds(int moduleCount, int detailLineCount) {
        int visibleRows = Math.max(1, (listBottom - listTop) / LIST_ROW_HEIGHT);
        maxListScroll = Math.max(0, moduleCount - visibleRows);
        listScroll = clamp(listScroll, 0, maxListScroll);

        int detailRows = Math.max(1, (detailBottom - detailTop) / DETAIL_ROW_HEIGHT);
        maxDetailScroll = Math.max(0, detailLineCount - detailRows);
        detailScroll = clamp(detailScroll, 0, maxDetailScroll);
    }

    private void resetListScroll() {
        listScroll = 0;
        detailScroll = 0;
    }

    private void syncPackSelection(String initialPackId) {
        List<LoadedPack> packs = Blockwright.getPackManager().getLoadedPacks();
        if (packs.isEmpty()) {
            selectedPackIndex = 0;
            return;
        }
        if (initialPackId == null || initialPackId.isBlank()) {
            selectedPackIndex = clamp(selectedPackIndex, 0, packs.size() - 1);
            return;
        }
        for (int i = 0; i < packs.size(); i++) {
            if (initialPackId.equals(packs.get(i).getMetadata().id)) {
                selectedPackIndex = i;
                return;
            }
        }
        selectedPackIndex = 0;
    }

    private String safePackName(LoadedPack pack) {
        String name = safe(pack.getMetadata().name);
        if (!name.isBlank() && !"<none>".equals(name)) {
            return pack.getMetadata().id + " / " + name;
        }
        return pack.getMetadata().id;
    }

    private void closeToParent() {
        Minecraft.getInstance().setScreen(parent == null ? new BlockwrightMainScreen() : parent);
    }

    private int detailColor(String line) {
        if (line.startsWith("ERROR")) {
            return STATUS_ERROR;
        }
        if (line.startsWith("WARNING")) {
            return STATUS_WARN;
        }
        if (line.startsWith("INFO")) {
            return STATUS_INFO;
        }
        return TEXT_MUTED;
    }

    private String trimToWidth(String text, int width) {
        return this.font.plainSubstrByWidth(text, Math.max(20, width));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    private static String joinList(List<String> values) {
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

    private enum KindFilter {
        ALL("All"),
        STATIC("Static"),
        PROCEDURAL("Procedural");

        private final String label;

        KindFilter(String label) {
            this.label = label;
        }

        private KindFilter next() {
            return switch (this) {
                case ALL -> STATIC;
                case STATIC -> PROCEDURAL;
                case PROCEDURAL -> ALL;
            };
        }

        private boolean matches(ModuleDefinition module) {
            return switch (this) {
                case ALL -> true;
                case STATIC -> "static_schem".equals(module.moduleKind);
                case PROCEDURAL -> "static_schem".equals(module.moduleKind) ? false : module.moduleKind != null;
            };
        }
    }
}
