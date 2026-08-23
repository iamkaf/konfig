package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigSection {
    private final ConfigSectionIdentity identity;
    private final long order;
    private final ConfigText label;
    private final ConfigText description;
    private final List<ConfigFieldIdentity> fields;

    ConfigSection(
            ConfigSectionIdentity identity,
            long order,
            ConfigText label,
            ConfigText description,
            List<ConfigFieldIdentity> fields
    ) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.order = order;
        this.label = label == null ? ConfigText.empty() : label;
        this.description = description == null ? ConfigText.empty() : description;
        this.fields = Collections.unmodifiableList(new ArrayList<ConfigFieldIdentity>(fields));
    }

    public ConfigSectionIdentity identity() {
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

    public List<ConfigFieldIdentity> fields() {
        return this.fields;
    }
}
