package top.huliawsl.blockwright;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.huliawsl.blockwright.command.BlockwrightCommands;
import top.huliawsl.blockwright.pack.BlockwrightPackManager;
import top.huliawsl.blockwright.session.BlockwrightSessionManager;

public final class Blockwright {
    public static final String MOD_ID = "blockwright";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final BlockwrightPackManager PACK_MANAGER = new BlockwrightPackManager();
    private static final BlockwrightSessionManager SESSION_MANAGER = new BlockwrightSessionManager();
    private static boolean initialized;

    private Blockwright() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PACK_MANAGER.reload();
        CommandRegistrationEvent.EVENT.register(BlockwrightCommands::register);
        LOGGER.info("Initialized Blockwright with {} loaded preset pack(s).", PACK_MANAGER.getLoadedPackCount());
    }

    public static BlockwrightPackManager getPackManager() {
        return PACK_MANAGER;
    }

    public static BlockwrightSessionManager getSessionManager() {
        return SESSION_MANAGER;
    }
}
