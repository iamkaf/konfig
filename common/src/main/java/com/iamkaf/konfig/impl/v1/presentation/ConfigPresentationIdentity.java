package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigIdentity;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigPresentationIdentity implements Comparable<ConfigPresentationIdentity> {
    private final ConfigIdentity config;
    private final String key;

    public ConfigPresentationIdentity(ConfigIdentity config, String key) {
        this.config = Objects.requireNonNull(config, "config");
        this.key = normalizeKey(key);
    }

    public ConfigIdentity config() {
        return this.config;
    }

    public String key() {
        return this.key;
    }

    public String value() {
        return this.config.value() + '/' + this.key;
    }

    @Override
    public int compareTo(ConfigPresentationIdentity other) {
        return value().compareTo(other.value());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigPresentationIdentity)) {
            return false;
        }
        ConfigPresentationIdentity that = (ConfigPresentationIdentity) other;
        return this.config.equals(that.config) && this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.config, this.key);
    }

    @Override
    public String toString() {
        return value();
    }

    private static String normalizeKey(String value) {
        String normalized = Objects.requireNonNull(value, "key").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Presentation identity key cannot be blank");
        }
        if (normalized.startsWith("/") || normalized.endsWith("/") || normalized.contains("//") || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Invalid presentation identity key: " + value);
        }
        return normalized;
    }
}
