package com.iamkaf.konfig.sync.v1;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.konfig.impl.v1.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.KonfigManager;
import net.minecraft.server.level.ServerPlayer;

public final class KonfigSync {
    private static SyncSender sender;

    private KonfigSync() {
    }

    public static void setSender(SyncSender sender) {
        KonfigSync.sender = sender;
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (sender == null) {
            return;
        }

        boolean debug = KonfigDebugConfig.enabled();
        int sentCount = 0;
        int totalBytes = 0;

        for (ConfigHandleImpl handle : KonfigManager.get().all()) {
            if (handle.scope() == ConfigScope.CLIENT || handle.syncMode() == SyncMode.NONE) {
                continue;
            }

            String payload = handle.snapshotJson();
            sender.send(player, new SyncSnapshot(handle.id(), payload));

            sentCount++;
            totalBytes += payload.length();

            if (debug) {
                Constants.LOG.info(
                        "[Konfig/Debug] Syncing '{}' to player '{}' ({} bytes).",
                        handle.id(),
                        player.getName().getString(),
                        payload.length()
                );
            }
        }

        if (debug) {
            Constants.LOG.info(
                    "[Konfig/Debug] Player join sync complete for '{}': sent={} totalBytes={}",
                    player.getName().getString(),
                    sentCount,
                    totalBytes
            );
        }
    }

    public static void onClientSnapshot(String configId, String jsonPayload) {
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] Applying client snapshot '{}' ({} bytes).",
                    configId,
                    jsonPayload == null ? 0 : jsonPayload.length()
            );
        }
        KonfigManager.get().applySnapshot(configId, jsonPayload);
    }

    public static void onClientDisconnect() {
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info("[Konfig/Debug] Clearing synced config overlays.");
        }
        KonfigManager.get().clearAllSynced();
    }

    @FunctionalInterface
    public interface SyncSender {
        void send(ServerPlayer player, SyncSnapshot snapshot);
    }
}
