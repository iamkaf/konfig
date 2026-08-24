package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigEditRequest {
    private final long requestId;
    private final String configId;
    private final long baseRevision;
    private final String draftJson;

    public ConfigEditRequest(long requestId, String configId, long baseRevision, String draftJson) {
        this.requestId = requestId;
        this.configId = Objects.requireNonNull(configId, "configId");
        this.baseRevision = baseRevision;
        this.draftJson = Objects.requireNonNull(draftJson, "draftJson");
    }

    public long requestId() {
        return this.requestId;
    }

    public String configId() {
        return this.configId;
    }

    public long baseRevision() {
        return this.baseRevision;
    }

    public String draftJson() {
        return this.draftJson;
    }
}
