package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@ApiStatus.Internal
public final class ConfigText {
    private static final ConfigText EMPTY = new ConfigText(Collections.<String>emptyList(), "");

    private final List<String> translationKeys;
    private final String fallback;

    private ConfigText(List<String> translationKeys, String fallback) {
        this.translationKeys = Collections.unmodifiableList(new ArrayList<String>(translationKeys));
        this.fallback = fallback;
    }

    public static ConfigText empty() {
        return EMPTY;
    }

    public static ConfigText literal(String text) {
        String normalized = normalize(text);
        return normalized.isEmpty() ? EMPTY : new ConfigText(Collections.<String>emptyList(), normalized);
    }

    public static ConfigText translated(String translationKey, String fallback) {
        String normalizedKey = normalize(translationKey);
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("translationKey cannot be blank");
        }
        return new ConfigText(Collections.singletonList(normalizedKey), normalize(fallback));
    }

    public static ConfigText translated(List<String> translationKeys, String fallback) {
        Objects.requireNonNull(translationKeys, "translationKeys");
        List<String> normalizedKeys = new ArrayList<String>(translationKeys.size());
        for (String translationKey : translationKeys) {
            String normalized = normalize(translationKey);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("translationKeys cannot contain a blank key");
            }
            if (!normalizedKeys.contains(normalized)) {
                normalizedKeys.add(normalized);
            }
        }
        if (normalizedKeys.isEmpty()) {
            throw new IllegalArgumentException("translationKeys cannot be empty");
        }
        return new ConfigText(normalizedKeys, normalize(fallback));
    }

    public String translationKey() {
        return this.translationKeys.isEmpty() ? "" : this.translationKeys.get(0);
    }

    public List<String> translationKeys() {
        return this.translationKeys;
    }

    public String fallback() {
        return this.fallback;
    }

    public boolean isEmpty() {
        return this.translationKeys.isEmpty() && this.fallback.isEmpty();
    }

    public String resolve(Function<String, String> translations) {
        Objects.requireNonNull(translations, "translations");
        for (String translationKey : this.translationKeys) {
            String translated = translations.apply(translationKey);
            if (translated != null && !translated.equals(translationKey)) {
                return translated;
            }
        }
        return this.fallback.isEmpty() ? translationKey() : this.fallback;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigText)) {
            return false;
        }
        ConfigText that = (ConfigText) other;
        return this.translationKeys.equals(that.translationKeys) && this.fallback.equals(that.fallback);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.translationKeys, this.fallback);
    }

    @Override
    public String toString() {
        return this.translationKeys.isEmpty() ? this.fallback : this.translationKeys.get(0);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
