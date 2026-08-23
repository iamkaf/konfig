package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigGroup {
    private final ConfigPresentationIdentity identity;
    private final ConfigPlacement placement;
    private final long order;
    private final ConfigText label;
    private final ConfigText description;
    private final boolean collapsible;
    private final boolean expandedByDefault;

    ConfigGroup(
            ConfigPresentationIdentity identity,
            ConfigPlacement placement,
            long order,
            ConfigText label,
            ConfigText description,
            boolean collapsible,
            boolean expandedByDefault
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.placement = Objects.requireNonNull(placement, "placement");
        this.order = order;
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
        this.collapsible = collapsible;
        this.expandedByDefault = expandedByDefault;
        if (!collapsible && !expandedByDefault) {
            throw new IllegalArgumentException("A non-collapsible group must be expanded: " + identity);
        }
    }

    public ConfigPresentationIdentity identity() {
        return this.identity;
    }

    public ConfigPlacement placement() {
        return this.placement;
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

    public boolean collapsible() {
        return this.collapsible;
    }

    public boolean expandedByDefault() {
        return this.expandedByDefault;
    }
}
