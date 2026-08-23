package com.iamkaf.konfig.impl.v1.sync;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class ConfigEditCapabilities {
    private final int protocolVersion;
    private final boolean canEdit;

    public ConfigEditCapabilities(int protocolVersion, boolean canEdit) {
        this.protocolVersion = protocolVersion;
        this.canEdit = canEdit;
    }

    public int protocolVersion() {
        return this.protocolVersion;
    }

    public boolean canEdit() {
        return this.canEdit;
    }
}
