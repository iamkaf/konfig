package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigIdentity implements Comparable<ConfigIdentity> {
    private final String modId;
    private final String configId;

    public ConfigIdentity(String modId, String configId) {
        this.modId = requireSegment(modId, "modId");
        this.configId = requireSegment(configId, "configId");
    }

    public static ConfigIdentity parse(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        int separator = normalized.indexOf(':');
        if (separator <= 0 || separator == normalized.length() - 1 || normalized.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Config identity must use <modId>:<configId>: " + value);
        }
        return new ConfigIdentity(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    public String modId() {
        return this.modId;
    }

    public String configId() {
        return this.configId;
    }

    public String value() {
        return this.modId + ':' + this.configId;
    }

    @Override
    public int compareTo(ConfigIdentity other) {
        return value().compareTo(other.value());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigIdentity)) {
            return false;
        }
        ConfigIdentity that = (ConfigIdentity) other;
        return this.modId.equals(that.modId) && this.configId.equals(that.configId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modId, this.configId);
    }

    @Override
    public String toString() {
        return value();
    }

    private static String requireSegment(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        if (normalized.indexOf(':') >= 0 || normalized.indexOf('.') >= 0 || normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + value);
        }
        return normalized;
    }
}
