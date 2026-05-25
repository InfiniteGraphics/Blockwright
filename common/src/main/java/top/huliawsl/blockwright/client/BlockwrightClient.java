package top.huliawsl.blockwright.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
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

    private BlockwrightClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        KeyMappingRegistry.register(OPEN_SCREEN);
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.level == null || client.player == null) {
                ClientSelectionState.clearAll();
                ClientPreviewState.clear();
                PcgEditorSession.get().close();
            }
            PcgEditorViewportNavigator.tick(client, PcgEditorSession.get());
            while (OPEN_SCREEN.consumeClick()) {
                if (client.player == null || !client.player.isCreative()) {
                    if (client.player != null) {
                        client.player.displayClientMessage(Component.literal("Blockwright 只允许创造模式玩家打开。"), true);
                    }
                    continue;
                }
                if (client.screen instanceof PcgEditorScreen) {
                    client.setScreen(null);
                    PcgEditorSession.get().close();
                } else {
                    client.setScreen(new PcgEditorScreen());
                }
            }
        });
    }
}
