package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigEditResult {
    private final long requestId;
    private final String configId;
    private final ConfigEditStatus status;
    private final long revision;
    private final String snapshotJson;
    private final String detail;

    public ConfigEditResult(
            long requestId,
            String configId,
            ConfigEditStatus status,
            long revision,
            String snapshotJson,
            String detail
    ) {
        this.requestId = requestId;
        this.configId = Objects.requireNonNull(configId, "configId");
        this.status = Objects.requireNonNull(status, "status");
        this.revision = revision;
        this.snapshotJson = snapshotJson == null ? "" : snapshotJson;
        this.detail = detail == null ? "" : detail;
    }

    public long requestId() {
        return this.requestId;
    }

    public String configId() {
        return this.configId;
    }

    public ConfigEditStatus status() {
        return this.status;
    }

    public long revision() {
        return this.revision;
    }

    public String snapshotJson() {
        return this.snapshotJson;
    }

    public String detail() {
        return this.detail;
    }

    public boolean accepted() {
        return this.status == ConfigEditStatus.ACCEPTED;
    }
}
