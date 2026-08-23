package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigSectionIdentity implements Comparable<ConfigSectionIdentity> {
    private final ConfigIdentity config;
    private final ConfigPath path;

    public ConfigSectionIdentity(ConfigIdentity config, ConfigPath path) {
        this.config = Objects.requireNonNull(config, "config");
        this.path = Objects.requireNonNull(path, "path");
    }

    public ConfigIdentity config() {
        return this.config;
    }

    public ConfigPath path() {
        return this.path;
    }

    public String value() {
        return this.path.isRoot() ? this.config.value() : this.config.value() + '/' + this.path.value();
    }

    @Override
    public int compareTo(ConfigSectionIdentity other) {
        return value().compareTo(other.value());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigSectionIdentity)) {
            return false;
        }
        ConfigSectionIdentity that = (ConfigSectionIdentity) other;
        return this.config.equals(that.config) && this.path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.config, this.path);
    }

    @Override
    public String toString() {
        return value();
    }
}
