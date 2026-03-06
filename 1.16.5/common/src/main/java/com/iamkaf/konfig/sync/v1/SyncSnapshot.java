package com.iamkaf.konfig.sync.v1;

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
