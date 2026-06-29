package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.bootstrap.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ReloadCause;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.KonfigManager;
//? if >=1.17 {
// Modern sync tracks ServerPlayer directly; legacy Forge/Fabric player types
// are kept as Object at the runtime facade edge.
import net.minecraft.server.level.ServerPlayer;
//?}

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class KonfigSync {
    private static SyncSender sender;
//? if <=1.16.5 {
    private static final Set<Object> players = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
//?} else {
    private static final Set<ServerPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<ServerPlayer, Boolean>());
//?}

    private KonfigSync() {
    }

    public static void setSender(SyncSender sender) {
        KonfigSync.sender = sender;
    }

//? if <=1.16.5 {
    public static void onPlayerJoin(Object player) {
//?} else {
    public static void onPlayerJoin(ServerPlayer player) {
//?}
        players.add(player);
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
//? if <=1.16.5 {
                        String.valueOf(player),
//?} else {
                        player.getName().getString(),
//?}
                        payload.length()
                );
            }
        }

        if (debug) {
            Constants.LOG.info(
                    "[Konfig/Debug] Player join sync complete for '{}': sent={} totalBytes={}",
//? if <=1.16.5 {
                    String.valueOf(player),
//?} else {
                    player.getName().getString(),
//?}
                    sentCount,
                    totalBytes
            );
        }
    }

//? if <=1.16.5 {
    public static void onPlayerLeave(Object player) {
//?} else {
    public static void onPlayerLeave(ServerPlayer player) {
//?}
        players.remove(player);
    }

    public static void onReload(ConfigHandleImpl handle, ReloadCause cause) {
        if (sender == null || handle.scope() == ConfigScope.CLIENT || handle.syncMode() != SyncMode.LOGIN_AND_RELOAD) {
            return;
        }

        if (players.isEmpty()) {
            if (KonfigDebugConfig.enabled()) {
                Constants.LOG.info(
                        "[Konfig/Debug] Reload sync skipped for '{}' after {}: no connected players.",
                        handle.id(),
                        cause
                );
            }
            return;
        }

        String payload = handle.snapshotJson();
        int sentCount = 0;
//? if <=1.16.5 {
        for (Object player : players) {
//?} else {
        for (ServerPlayer player : players) {
//?}
            sender.send(player, new SyncSnapshot(handle.id(), payload));
            sentCount++;
        }

        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] Reload sync complete for '{}' after {}: sent={} bytes={}",
                    handle.id(),
                    cause,
                    sentCount,
                    payload.length()
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
//? if <=1.16.5 {
        void send(Object player, SyncSnapshot snapshot);
//?} else {
        void send(ServerPlayer player, SyncSnapshot snapshot);
//?}
    }
}
