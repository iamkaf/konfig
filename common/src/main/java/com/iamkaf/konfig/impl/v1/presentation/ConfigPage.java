package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigPage {
    private final ConfigPresentationIdentity identity;
    private final long order;
    private final ConfigText label;
    private final ConfigText description;

    ConfigPage(ConfigPresentationIdentity identity, long order, ConfigText label, ConfigText description) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.order = order;
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
    }

    public ConfigPresentationIdentity identity() {
        return this.identity;
    }

    public long order() {
        return this.order;
    }

    public ConfigText label() {
        return this.label;
    }

    public ConfigText description() {
        return this.description;
    }
}
