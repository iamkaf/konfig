package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigTab {
    private final ConfigPresentationIdentity identity;
    private final ConfigPresentationIdentity page;
    private final long order;
    private final ConfigText label;
    private final ConfigText description;

    ConfigTab(
            ConfigPresentationIdentity identity,
            ConfigPresentationIdentity page,
            long order,
            ConfigText label,
            ConfigText description
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.page = Objects.requireNonNull(page, "page");
        this.order = order;
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
    }

    public ConfigPresentationIdentity identity() {
        return this.identity;
    }

    public ConfigPresentationIdentity page() {
        return this.page;
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
