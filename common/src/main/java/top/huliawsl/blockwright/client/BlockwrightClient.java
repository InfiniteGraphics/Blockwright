package top.huliawsl.blockwright.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.tree.CommandNode;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class BlockwrightClient {
    private static final KeyMapping OPEN_SCREEN = new KeyMapping(
            "key.blockwright.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.misc"
    );

    private static boolean initialized;
    private static int editorToggleCooldownTicks;

    private BlockwrightClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        KeyMappingRegistry.register(OPEN_SCREEN);
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (editorToggleCooldownTicks > 0) {
                editorToggleCooldownTicks--;
            }
            if (client.level == null || client.player == null) {
                ClientSelectionState.clearAll();
                ClientPreviewState.clear();
                PcgEditorSession.get().exitCameraMode(client);
                PcgEditorSession.get().close();
            }
            PcgEditorViewportNavigator.tick(client, PcgEditorSession.get());
            while (OPEN_SCREEN.consumeClick()) {
                if (editorToggleCooldownTicks > 0) {
                    continue;
                }
                if (PcgEditorSession.get().isOpen()) {
                    if (client.screen instanceof PcgEditorScreen) {
                        client.screen.onClose();
                    } else {
                        sendCommand(client, "blockwright editor exit");
                        PcgEditorSession.get().exitCameraMode(client);
                        PcgEditorSession.get().close();
                        client.setScreen(null);
                    }
                    continue;
                }
                if (client.player == null || !client.player.isCreative()) {
                    if (client.player != null) {
                        client.player.displayClientMessage(Component.literal("Blockwright 只允许创造模式玩家打开。"), true);
                    }
                    continue;
                }
                if (!canUseEditorSpectatorMode(client)) {
                    client.player.displayClientMessage(Component.literal("需要游戏模式权限才能进入 Blockwright 编辑视角。"), true);
                    continue;
                }
                sendCommand(client, "blockwright editor enter");
                client.setScreen(new PcgEditorScreen());
            }
        });
    }

    public static void suppressNextEditorToggle() {
        editorToggleCooldownTicks = 2;
    }

    private static boolean canUseEditorSpectatorMode(Minecraft client) {
        if (client == null || client.player == null || client.player.connection == null) {
            return false;
        }
        if (client.hasSingleplayerServer()) {
            return true;
        }
        CommandNode<?> blockwright = client.player.connection.getCommands().getRoot().getChild("blockwright");
        if (blockwright == null) {
            return false;
        }
        CommandNode<?> editor = blockwright.getChild("editor");
        return editor != null && editor.getChild("enter") != null && editor.getChild("exit") != null;
    }

    private static void sendCommand(Minecraft client, String command) {
        if (client == null || client.player == null || client.player.connection == null) {
            return;
        }
        client.player.connection.sendCommand(command);
    }
}
