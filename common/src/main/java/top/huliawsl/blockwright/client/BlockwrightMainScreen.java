package top.huliawsl.blockwright.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import top.huliawsl.blockwright.Blockwright;
import top.huliawsl.blockwright.pack.LoadedPack;
import top.huliawsl.blockwright.preset.model.PresetDefinition;
import top.huliawsl.blockwright.preset.model.PresetParameterDefinition;

import java.util.ArrayList;
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

        int y = top + 118;
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

        guiGraphics.drawString(this.font, this.title, left, 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Pack: " + (selectedPack == null ? "<none>" : selectedPack.getMetadata().id), left, 96, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Preset: " + (preset == null ? "<none>" : preset.id), left, 108, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Use current player position for Region P1/P2 and Spline Add.", left, this.height - 38, 0xA0A0A0);
        guiGraphics.drawString(this.font, "Preview/Bake flows through /blockwright commands.", left, this.height - 26, 0xA0A0A0);

        int y = 148;
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

        List<String> overrides = new ArrayList<>();
        for (int i = 0; i < parameterKeys.size(); i++) {
            overrides.add(parameterKeys.get(i) + "=" + parameterBoxes.get(i).getValue());
        }
        String command = "blockwright preview generate " + preset.id;
        if (!overrides.isEmpty()) {
            command += " " + String.join(",", overrides);
        }
        sendCommand(command);
    }

    private void sendCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            return;
        }
        minecraft.player.connection.sendCommand(command);
    }
}
