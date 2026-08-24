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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApiStatus.Internal
public final class KonfigSync {
    private static SyncSender sender;
    private static final ConfigSyncAuthority authority = new ConfigSyncAuthority();
//? if <=1.16.5 {
    private static final Set<Object> players = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());
//?} else {
    private static final Set<ServerPlayer> players = Collections.newSetFromMap(new ConcurrentHashMap<ServerPlayer, Boolean>());
//?}
//? if >=1.21.11 {
    private static final Set<ServerPlayer> remotePeers = Collections.newSetFromMap(new ConcurrentHashMap<ServerPlayer, Boolean>());
    private static final Map<String, Long> clientRevisions = new ConcurrentHashMap<String, Long>();
    private static final CopyOnWriteArrayList<ClientEditListener> clientEditListeners = new CopyOnWriteArrayList<ClientEditListener>();
    private static final AtomicLong requestIds = new AtomicLong();
    private static final ThreadLocal<Boolean> remoteApply = new ThreadLocal<Boolean>();
    private static RemoteSender remoteSender;
    private static ClientRequestSender clientRequestSender;
    private static volatile boolean clientConnected;
    private static volatile boolean clientTransportAvailable;
    private static volatile ConfigEditCapabilities clientCapabilities = readOnlyCapabilities();
//?}

    private KonfigSync() {
    }

    public static void setSender(SyncSender sender) {
        KonfigSync.sender = sender;
    }

    public static ConfigSyncAuthority authority() {
        return authority;
    }

//? if >=1.21.11 {
    public static void setRemoteSender(RemoteSender sender) {
        KonfigSync.remoteSender = sender;
    }

    public static void setClientRequestSender(ClientRequestSender sender) {
        KonfigSync.clientRequestSender = sender;
    }
//?}

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
//? if >=1.21.11
        remotePeers.remove(player);
    }

//? if >=1.21.11 {
    public static void onClientHello(ServerPlayer player, int protocolVersion, boolean permitted) {
        if (remoteSender == null) {
            return;
        }
        if (protocolVersion != ConfigSyncAuthority.PROTOCOL_VERSION) {
            remotePeers.remove(player);
            remoteSender.sendCapabilities(player, readOnlyCapabilities());
            return;
        }

        remotePeers.add(player);
        remoteSender.sendCapabilities(
                player,
                new ConfigEditCapabilities(ConfigSyncAuthority.PROTOCOL_VERSION, permitted)
        );
        for (ConfigEditSnapshot snapshot : authority.snapshots()) {
            remoteSender.sendSnapshot(player, snapshot);
        }
    }

    public static void onRemoteEdit(ServerPlayer player, boolean permitted, ConfigEditRequest request) {
        if (remoteSender == null) {
            return;
        }
        ConfigEditResult result;
        remoteApply.set(Boolean.TRUE);
        try {
            result = authority.apply(request, remotePeers.contains(player), permitted);
        } finally {
            remoteApply.remove();
        }
        remoteSender.sendResult(player, result);
        if (!permitted) {
            remoteSender.sendCapabilities(player, readOnlyCapabilities());
        }
        if (!result.accepted()) {
            return;
        }

        broadcastLegacySnapshot(result.configId(), result.snapshotJson());
        ConfigEditSnapshot snapshot = new ConfigEditSnapshot(result.configId(), result.revision(), result.snapshotJson());
        for (ServerPlayer peer : remotePeers) {
            remoteSender.sendSnapshot(peer, snapshot);
        }
    }

    public static void onClientConnected(boolean remoteSupported) {
        clientConnected = true;
        clientTransportAvailable = remoteSupported;
        clientCapabilities = readOnlyCapabilities();
        clientRevisions.clear();
        if (remoteSupported && clientRequestSender != null) {
            clientRequestSender.sendHello(ConfigSyncAuthority.PROTOCOL_VERSION);
        }
    }

    public static void onClientCapabilities(ConfigEditCapabilities capabilities) {
        if (capabilities.protocolVersion() != ConfigSyncAuthority.PROTOCOL_VERSION) {
            clientCapabilities = readOnlyCapabilities();
        } else {
            clientCapabilities = capabilities;
        }
        for (ClientEditListener listener : clientEditListeners) {
            listener.onCapabilities(clientCapabilities);
        }
    }

    public static void refreshRemoteCapabilities() {
        if (clientTransportAvailable && clientRequestSender != null) {
            clientRequestSender.sendHello(ConfigSyncAuthority.PROTOCOL_VERSION);
        }
    }

    public static void onClientAuthoritySnapshot(ConfigEditSnapshot snapshot) {
        if (!ConfigSyncAuthority.isValidConfigId(snapshot.configId())
                || snapshot.revision() < 0L
                || snapshot.jsonPayload().length() > ConfigSyncAuthority.MAX_JSON_LENGTH) {
            return;
        }
        if (!applyClientAuthoritySnapshot(snapshot.configId(), snapshot.revision(), snapshot.jsonPayload())) {
            return;
        }
        for (ClientEditListener listener : clientEditListeners) {
            listener.onSnapshot(snapshot);
        }
    }

    public static void onClientEditResult(ConfigEditResult result) {
        if (!ConfigSyncAuthority.isValidConfigId(result.configId())) {
            return;
        }
        if (result.revision() >= 0L && !result.snapshotJson().isEmpty()) {
            applyClientAuthoritySnapshot(result.configId(), result.revision(), result.snapshotJson());
        }
        for (ClientEditListener listener : clientEditListeners) {
            listener.onResult(result);
        }
    }

    public static boolean remoteEditsAvailable() {
        return clientCapabilities.protocolVersion() == ConfigSyncAuthority.PROTOCOL_VERSION
                && clientCapabilities.canEdit()
                && clientTransportAvailable
                && clientRequestSender != null;
    }

    public static boolean clientConnected() {
        return clientConnected;
    }

    public static boolean remoteEditsAvailable(String configId) {
        return remoteEditsAvailable()
                && ConfigSyncAuthority.isValidConfigId(configId)
                && clientRevisions.containsKey(configId);
    }

    public static long clientRevision(String configId) {
        Long revision = clientRevisions.get(configId);
        return revision == null ? -1L : revision.longValue();
    }

    public static long submitRemoteDraft(String configId, String completeDraftJson) {
        if (!remoteEditsAvailable(configId)
                || completeDraftJson == null
                || completeDraftJson.length() > ConfigSyncAuthority.MAX_JSON_LENGTH) {
            return -1L;
        }
        Long revision = clientRevisions.get(configId);
        if (revision == null) {
            return -1L;
        }
        long requestId = nextRequestId();
        clientRequestSender.sendEdit(new ConfigEditRequest(requestId, configId, revision.longValue(), completeDraftJson));
        return requestId;
    }

    public static void addClientEditListener(ClientEditListener listener) {
        if (listener != null) {
            clientEditListeners.addIfAbsent(listener);
        }
    }

    public static void removeClientEditListener(ClientEditListener listener) {
        clientEditListeners.remove(listener);
    }
//?}

    public static void onReload(ConfigHandleImpl handle, ReloadCause cause) {
//? if >=1.21.11 {
        if (Boolean.TRUE.equals(remoteApply.get())) {
            return;
        }
//?}
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

//? if >=1.21.11 {
        if (remoteSender != null) {
            ConfigEditSnapshot snapshot = authority.snapshot(handle.id());
            if (snapshot != null) {
                for (ServerPlayer player : remotePeers) {
                    remoteSender.sendSnapshot(player, snapshot);
                }
            }
        }
//?}

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
//? if >=1.21.11 {
        clientCapabilities = readOnlyCapabilities();
        clientConnected = false;
        clientTransportAvailable = false;
        clientRevisions.clear();
        for (ClientEditListener listener : clientEditListeners) {
            listener.onDisconnected();
        }
//?}
    }

//? if >=1.21.11 {
    private static void broadcastLegacySnapshot(String configId, String jsonPayload) {
        if (sender == null) {
            return;
        }
        SyncSnapshot snapshot = new SyncSnapshot(configId, jsonPayload);
        for (ServerPlayer player : players) {
            sender.send(player, snapshot);
        }
    }

    private static ConfigEditCapabilities readOnlyCapabilities() {
        return new ConfigEditCapabilities(ConfigSyncAuthority.PROTOCOL_VERSION, false);
    }

    private static boolean applyClientAuthoritySnapshot(String configId, long revision, String jsonPayload) {
        while (true) {
            Long previous = clientRevisions.get(configId);
            if (previous != null && revision <= previous.longValue()) {
                return false;
            }
            if (previous == null) {
                if (clientRevisions.putIfAbsent(configId, Long.valueOf(revision)) != null) {
                    continue;
                }
            } else if (!clientRevisions.replace(configId, previous, Long.valueOf(revision))) {
                continue;
            }
            onClientSnapshot(configId, jsonPayload);
            return true;
        }
    }

    private static long nextRequestId() {
        long requestId = requestIds.getAndIncrement();
        if (requestId >= 0L) {
            return requestId;
        }
        requestIds.compareAndSet(requestId + 1L, 1L);
        return 0L;
    }
//?}

    @FunctionalInterface
    public interface SyncSender {
//? if <=1.16.5 {
        void send(Object player, SyncSnapshot snapshot);
//?} else {
        void send(ServerPlayer player, SyncSnapshot snapshot);
//?}
    }

//? if >=1.21.11 {
    public interface RemoteSender {
        void sendCapabilities(ServerPlayer player, ConfigEditCapabilities capabilities);

        void sendSnapshot(ServerPlayer player, ConfigEditSnapshot snapshot);

        void sendResult(ServerPlayer player, ConfigEditResult result);
    }

    public interface ClientRequestSender {
        void sendHello(int protocolVersion);

        void sendEdit(ConfigEditRequest request);
    }

    @FunctionalInterface
    public interface ClientEditListener {
        void onResult(ConfigEditResult result);

        default void onCapabilities(ConfigEditCapabilities capabilities) {
        }

        default void onSnapshot(ConfigEditSnapshot snapshot) {
        }

        default void onDisconnected() {
        }
    }
//?}
}
