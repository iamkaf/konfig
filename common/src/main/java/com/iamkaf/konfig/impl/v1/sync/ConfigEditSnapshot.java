package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigEditSnapshot {
    private final String configId;
    private final long revision;
    private final String jsonPayload;

    public ConfigEditSnapshot(String configId, long revision, String jsonPayload) {
        this.configId = Objects.requireNonNull(configId, "configId");
        this.revision = revision;
        this.jsonPayload = Objects.requireNonNull(jsonPayload, "jsonPayload");
    }

    public String configId() {
        return this.configId;
    }

    public long revision() {
        return this.revision;
    }

    public String jsonPayload() {
        return this.jsonPayload;
    }
}
