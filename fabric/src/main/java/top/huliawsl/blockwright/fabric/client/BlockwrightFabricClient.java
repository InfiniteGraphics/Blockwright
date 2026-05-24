package top.huliawsl.blockwright.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import top.huliawsl.blockwright.client.BlockwrightClient;

public final class BlockwrightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockwrightClient.init();
    }
}
