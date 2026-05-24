package top.huliawsl.blockwright.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
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
            while (OPEN_SCREEN.consumeClick()) {
                client.setScreen(new BlockwrightMainScreen());
            }
        });
    }
}
