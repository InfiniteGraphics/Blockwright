package top.huliawsl.blockwright.session;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockwrightSessionManager {
    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public PlayerSession getOrCreate(ServerPlayer player) {
        return sessions.computeIfAbsent(player.getUUID(), ignored -> new PlayerSession());
    }
}
