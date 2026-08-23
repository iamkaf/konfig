package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigFieldIdentity implements Comparable<ConfigFieldIdentity> {
    private final ConfigIdentity config;
    private final ConfigPath path;

    public ConfigFieldIdentity(ConfigIdentity config, ConfigPath path) {
        this.config = Objects.requireNonNull(config, "config");
        this.path = Objects.requireNonNull(path, "path");
        if (path.isRoot()) {
            throw new IllegalArgumentException("Config field path cannot be empty");
        }
    }

    public ConfigIdentity config() {
        return this.config;
    }

    public ConfigPath path() {
        return this.path;
    }

    public String value() {
        return this.config.value() + '/' + this.path.value();
    }

    @Override
    public int compareTo(ConfigFieldIdentity other) {
        return value().compareTo(other.value());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigFieldIdentity)) {
            return false;
        }
        ConfigFieldIdentity that = (ConfigFieldIdentity) other;
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
