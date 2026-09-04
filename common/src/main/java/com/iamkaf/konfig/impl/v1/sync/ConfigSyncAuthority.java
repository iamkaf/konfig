package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class ConfigSyncAuthority {
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_CONFIG_ID_LENGTH = 256;
    public static final int MAX_JSON_LENGTH = 262_144;
    public static final int MAX_DETAIL_LENGTH = 512;

    private final Map<String, ConfigEditTarget> targets = new ConcurrentHashMap<String, ConfigEditTarget>();

    public void register(ConfigEditTarget target) {
        validateConfigId(target.configId());
        ConfigEditTarget previous = this.targets.putIfAbsent(target.configId(), target);
        if (previous != null && previous != target) {
            throw new IllegalStateException("Remote config target already registered: " + target.configId());
        }
    }

    public void unregister(ConfigEditTarget target) {
        this.targets.remove(target.configId(), target);
    }

    public List<ConfigEditSnapshot> snapshots() {
        List<ConfigEditTarget> sorted = new ArrayList<ConfigEditTarget>(this.targets.values());
        Collections.sort(sorted, Comparator.comparing(ConfigEditTarget::configId));
        List<ConfigEditSnapshot> snapshots = new ArrayList<ConfigEditSnapshot>(sorted.size());
        for (ConfigEditTarget target : sorted) {
            snapshots.add(snapshot(target));
        }
        return Collections.unmodifiableList(snapshots);
    }

    public ConfigEditSnapshot snapshot(String configId) {
        ConfigEditTarget target = this.targets.get(configId);
        return target == null ? null : snapshot(target);
    }

    public ConfigEditResult apply(ConfigEditRequest request, boolean negotiated, boolean permitted) {
        if (!isValidRequestId(request.requestId()) || !isValidRevision(request.baseRevision())) {
            return rejected(request, ConfigEditStatus.INVALID, -1L, "invalid request metadata");
        }
        if (!isValidConfigId(request.configId())) {
            return rejected(request, ConfigEditStatus.INVALID, -1L, "invalid config id");
        }
        if (request.draftJson().length() > MAX_JSON_LENGTH) {
            return rejected(request, ConfigEditStatus.TOO_LARGE, -1L, "draft exceeds payload limit");
        }
        if (!negotiated) {
            return rejected(request, ConfigEditStatus.UNSUPPORTED, -1L, "write protocol was not negotiated");
        }
        if (!permitted) {
            return rejected(request, ConfigEditStatus.UNAUTHORIZED, -1L, "permission level 2 is required");
        }

        ConfigEditTarget target = this.targets.get(request.configId());
        if (target == null) {
            return rejected(request, ConfigEditStatus.UNKNOWN_CONFIG, -1L, "unknown or read-only config");
        }

        ConfigEditTarget.ApplyResult applied;
        try {
            applied = target.applyAtomic(request.baseRevision(), request.draftJson());
        } catch (RuntimeException exception) {
            return rejected(request, ConfigEditStatus.INVALID, target.currentRevision(), "draft could not be applied");
        }
        if (applied == null) {
            return rejected(request, ConfigEditStatus.INVALID, target.currentRevision(), "target returned no result");
        }
        if (applied.revision() < 0L) {
            return rejected(request, ConfigEditStatus.INVALID, target.currentRevision(), "target returned an invalid revision");
        }
        String snapshot;
        try {
            snapshot = boundedSnapshot(applied.snapshotJson());
        } catch (IllegalStateException exception) {
            return rejected(request, ConfigEditStatus.TOO_LARGE, target.currentRevision(), "snapshot exceeds payload limit");
        }
        if ((applied.status() == ConfigEditStatus.ACCEPTED
                || applied.status() == ConfigEditStatus.STALE
                || applied.status() == ConfigEditStatus.NO_OP)
                && snapshot.isEmpty()) {
            return rejected(request, ConfigEditStatus.INVALID, target.currentRevision(), "target returned no authoritative snapshot");
        }
        String detail = boundedDetail(applied.detail());
        return new ConfigEditResult(
                request.requestId(),
                request.configId(),
                applied.status(),
                applied.revision(),
                snapshot,
                detail
        );
    }

    public static boolean canEdit(boolean singleplayerOwner, boolean hasGamemasterPermission) {
        return singleplayerOwner || hasGamemasterPermission;
    }

    public static boolean isValidConfigId(String configId) {
        if (configId == null || configId.length() > MAX_CONFIG_ID_LENGTH) {
            return false;
        }
        int separator = configId.indexOf(':');
        if (separator <= 0 || separator == configId.length() - 1 || separator != configId.lastIndexOf(':')) {
            return false;
        }
        for (int index = 0; index < configId.length(); index++) {
            if (Character.isISOControl(configId.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static ConfigEditSnapshot snapshot(ConfigEditTarget target) {
        return new ConfigEditSnapshot(target.configId(), target.currentRevision(), boundedSnapshot(target.snapshotJson()));
    }

    private static ConfigEditResult rejected(
            ConfigEditRequest request,
            ConfigEditStatus status,
            long revision,
            String detail
    ) {
        return new ConfigEditResult(request.requestId(), request.configId(), status, revision, "", detail);
    }

    private static void validateConfigId(String configId) {
        if (!isValidConfigId(configId)) {
            throw new IllegalArgumentException("Invalid remote config id: " + configId);
        }
    }

    private static boolean isValidRequestId(long requestId) {
        return requestId >= 0L;
    }

    private static boolean isValidRevision(long revision) {
        return revision >= 0L;
    }

    private static String boundedSnapshot(String snapshot) {
        if (snapshot == null) {
            return "";
        }
        if (snapshot.length() > MAX_JSON_LENGTH) {
            throw new IllegalStateException("Config snapshot exceeds payload limit");
        }
        return snapshot;
    }

    private static String boundedDetail(String detail) {
        if (detail == null || detail.length() <= MAX_DETAIL_LENGTH) {
            return detail == null ? "" : detail;
        }
        return detail.substring(0, MAX_DETAIL_LENGTH);
    }
}
