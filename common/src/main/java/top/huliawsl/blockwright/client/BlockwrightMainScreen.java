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
import top.huliawsl.blockwright.preview.PreviewPlan;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;
import top.huliawsl.blockwright.selection.BoxRegionSelection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockwrightMainScreen extends Screen {
    private static int selectedPresetIndex;

    private final List<EditBox> parameterBoxes = new ArrayList<>();
    private final List<String> parameterKeys = new ArrayList<>();

    public BlockwrightMainScreen() {
        super(Component.literal("Blockwright"));
    }

    @Override
    protected void init() {
        parameterBoxes.clear();
        parameterKeys.clear();

        int left = this.width / 2 - 150;
        int top = 30;

        addRenderableWidget(Button.builder(Component.literal("< Preset"), button -> cyclePreset(-1))
                .bounds(left, top, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Preset >"), button -> cyclePreset(1))
                .bounds(left + 80, top, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reload"), button -> sendCommand("blockwright reload"))
                .bounds(left + 160, top, 60, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(left + 230, top, 60, 20)
                .build());

        int actionTop = top + 28;
        addRenderableWidget(Button.builder(Component.literal("Preview"), button -> sendPreviewCommand())
                .bounds(left, actionTop, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Bake"), button -> sendCommand("blockwright bake"))
                .bounds(left + 80, actionTop, 60, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Undo"), button -> sendCommand("blockwright undo"))
                .bounds(left + 150, actionTop, 60, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), button -> sendCommand("blockwright preview clear"))
                .bounds(left + 220, actionTop, 70, 20)
                .build());

        int toolTop = top + 56;
        addRenderableWidget(Button.builder(Component.literal("Region P1"), button -> sendCommand("blockwright region pos1"))
                .bounds(left, toolTop, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Region P2"), button -> sendCommand("blockwright region pos2"))
                .bounds(left + 80, toolTop, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Region Clear"), button -> sendCommand("blockwright region clear"))
                .bounds(left + 160, toolTop, 90, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Spline Add"), button -> sendCommand("blockwright spline add"))
                .bounds(left, toolTop + 24, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Spline List"), button -> sendCommand("blockwright spline list"))
                .bounds(left + 80, toolTop + 24, 70, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Spline Clear"), button -> sendCommand("blockwright spline clear"))
                .bounds(left + 160, toolTop + 24, 90, 20)
                .build());

        PresetDefinition preset = getSelectedPreset();
        if (preset == null) {
            return;
        }

        int y = top + 150;
        for (Map.Entry<String, PresetParameterDefinition> entry : preset.parameters.entrySet()) {
            if (!entry.getValue().exposed) {
                continue;
            }
            parameterKeys.add(entry.getKey());
            EditBox box = new EditBox(this.font, left + 110, y, 180, 18, Component.literal(entry.getKey()));
            box.setValue(entry.getValue().defaultValue == null ? "" : entry.getValue().defaultValue.getAsString());
            addRenderableWidget(box);
            parameterBoxes.add(box);
            y += 24;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 150;
        LoadedPack selectedPack = getSelectedPack();
        PresetDefinition preset = getSelectedPreset();
        BoxRegionSelection regionSelection = ClientSelectionState.getRegionSelection();
        PreviewPlan previewPlan = ClientPreviewState.getPreviewPlan();

        guiGraphics.drawString(this.font, this.title, left, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Pack: " + (selectedPack == null ? "<none>" : selectedPack.getMetadata().id), left, 118, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Preset: " + (preset == null ? "<none>" : preset.id), left, 130, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Region P1: " + formatPos(regionSelection.getPos1()), left, 142, 0xF2C93D);
        guiGraphics.drawString(this.font, "Region P2: " + formatPos(regionSelection.getPos2()), left, 154, 0x3DC9F2);
        guiGraphics.drawString(this.font, "1. 站到第一个角点，点 Region P1。 2. 站到对角点，点 Region P2。", left, this.height - 50, 0xA0A0A0);
        guiGraphics.drawString(this.font, "3. 选 preset 和参数后点 Preview。 4. 确认没问题再点 Bake。", left, this.height - 38, 0xA0A0A0);
        guiGraphics.drawString(this.font, "当前版本的世界内 outline 只跟随 GUI 按钮同步。", left, this.height - 26, 0xA0A0A0);

        if (regionSelection.isComplete()) {
            guiGraphics.drawString(
                    this.font,
                    "Region Size: " + regionSelection.getWidth() + " x " + regionSelection.getHeight() + " x " + regionSelection.getDepth()
                            + " (" + regionSelection.getVolume() + " blocks)",
                    left,
                    166,
                    0x7CFF92
            );
        }
        if (previewPlan != null) {
            guiGraphics.drawString(
                    this.font,
                    "Preview: " + previewPlan.getPlannedBlocks().size() + " blocks, " + previewPlan.getOverallSeverity(),
                    left,
                    178,
                    0x8FE6FF
            );
        }

        int y = 192;
        for (int i = 0; i < parameterKeys.size(); i++) {
            guiGraphics.drawString(this.font, parameterKeys.get(i), left, y + 5, 0xFFFFFF);
            y += 24;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void cyclePreset(int delta) {
        List<PresetDefinition> presets = Blockwright.getPackManager().getAllPresets();
        if (presets.isEmpty()) {
            return;
        }
        selectedPresetIndex = Math.floorMod(selectedPresetIndex + delta, presets.size());
        Minecraft.getInstance().setScreen(new BlockwrightMainScreen());
    }

    private LoadedPack getSelectedPack() {
        PresetDefinition preset = getSelectedPreset();
        if (preset == null) {
            return null;
        }
        return Blockwright.getPackManager().findPreset(preset.id).map(top.huliawsl.blockwright.pack.BlockwrightPackManager.PresetLookup::pack).orElse(null);
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

    private void sendPreviewCommand() {
        PresetDefinition preset = getSelectedPreset();
        if (preset == null) {
            return;
        }

        Map<String, String> overrideMap = new LinkedHashMap<>();
        List<String> overrideParts = new ArrayList<>();
        for (int i = 0; i < parameterKeys.size(); i++) {
            String key = parameterKeys.get(i);
            String value = parameterBoxes.get(i).getValue();
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

    private static String formatPos(BlockPos pos) {
        return pos == null ? "<unset>" : pos.toShortString();
    }
}
