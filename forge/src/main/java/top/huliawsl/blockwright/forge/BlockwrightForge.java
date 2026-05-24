package top.huliawsl.blockwright.forge;

import top.huliawsl.blockwright.Blockwright;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Blockwright.MOD_ID)
public final class BlockwrightForge {
    public BlockwrightForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Blockwright.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        Blockwright.init();
    }
}
