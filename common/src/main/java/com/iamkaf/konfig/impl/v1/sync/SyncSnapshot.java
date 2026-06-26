package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class SyncSnapshot {
    private final String configId;
    private final String jsonPayload;

    public SyncSnapshot(String configId, String jsonPayload) {
        this.configId = configId;
        this.jsonPayload = jsonPayload;
    }

    public String configId() {
        return this.configId;
    }

    public String jsonPayload() {
        return this.jsonPayload;
    }
}
