package top.huliawsl.blockwright.session;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockwrightSessionManager {
    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public PlayerSession getOrCreate(ServerPlayer player) {
        return sessions.computeIfAbsent(player.getUUID(), ignored -> new PlayerSession());
    }

    public void markAllPreviewsStale() {
        sessions.values().forEach(PlayerSession::markPreviewStale);
    }

    public boolean restoreEditorSpectator(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        PlayerSession session = sessions.get(player.getUUID());
        if (session == null || !session.isEditorSpectatorMode()) {
            return false;
        }
        GameType restoreGameType = session.consumeEditorRestoreGameType();
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setGameMode(restoreGameType);
        }
        return true;
    }

    public void cleanupPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        restoreEditorSpectator(player);
        sessions.remove(player.getUUID());
    }
}
