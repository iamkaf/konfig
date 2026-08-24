package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigIdentity;
import com.iamkaf.konfig.impl.v1.model.ConfigSectionIdentity;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigInformationContext {
    public enum Kind {
        CONFIG,
        SECTION,
        FIELD
    }

    private final Kind kind;
    private final ConfigIdentity config;
    private final ConfigSectionIdentity section;
    private final ConfigFieldIdentity field;

    private ConfigInformationContext(
            Kind kind,
            ConfigIdentity config,
            ConfigSectionIdentity section,
            ConfigFieldIdentity field
    ) {
        this.kind = kind;
        this.config = config;
        this.section = section;
        this.field = field;
    }

    public static ConfigInformationContext config(ConfigIdentity config) {
        return new ConfigInformationContext(Kind.CONFIG, Objects.requireNonNull(config, "config"), null, null);
    }

    public static ConfigInformationContext section(ConfigSectionIdentity section) {
        Objects.requireNonNull(section, "section");
        return new ConfigInformationContext(Kind.SECTION, section.config(), section, null);
    }

    public static ConfigInformationContext field(ConfigFieldIdentity field) {
        Objects.requireNonNull(field, "field");
        return new ConfigInformationContext(Kind.FIELD, field.config(), null, field);
    }

    public Kind kind() {
        return this.kind;
    }

    public ConfigIdentity config() {
        return this.config;
    }

    public ConfigSectionIdentity section() {
        return this.section;
    }

    public ConfigFieldIdentity field() {
        return this.field;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigInformationContext)) {
            return false;
        }
        ConfigInformationContext that = (ConfigInformationContext) other;
        return this.kind == that.kind
                && this.config.equals(that.config)
                && Objects.equals(this.section, that.section)
                && Objects.equals(this.field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.kind, this.config, this.section, this.field);
    }
}
