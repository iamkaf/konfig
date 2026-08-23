package com.iamkaf.konfig.impl.v1.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class ConfigPath implements Comparable<ConfigPath> {
    private static final ConfigPath ROOT = new ConfigPath(Collections.<String>emptyList());

    private final List<String> segments;
    private final String value;

    private ConfigPath(List<String> segments) {
        this.segments = Collections.unmodifiableList(new ArrayList<String>(segments));
        this.value = String.join(".", segments);
    }

    public static ConfigPath root() {
        return ROOT;
    }

    public static ConfigPath parse(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        if (normalized.isEmpty()) {
            return ROOT;
        }
        String[] rawSegments = normalized.split("\\.", -1);
        List<String> segments = new ArrayList<String>(rawSegments.length);
        for (String rawSegment : rawSegments) {
            segments.add(requireSegment(rawSegment));
        }
        return new ConfigPath(segments);
    }

    public ConfigPath child(String segment) {
        List<String> result = new ArrayList<String>(this.segments);
        result.add(requireSegment(segment));
        return new ConfigPath(result);
    }

    public ConfigPath parent() {
        if (this.segments.isEmpty()) {
            return this;
        }
        if (this.segments.size() == 1) {
            return ROOT;
        }
        return new ConfigPath(this.segments.subList(0, this.segments.size() - 1));
    }

    public List<String> segments() {
        return this.segments;
    }

    public String lastSegment() {
        return this.segments.isEmpty() ? "" : this.segments.get(this.segments.size() - 1);
    }

    public boolean isRoot() {
        return this.segments.isEmpty();
    }

    public String value() {
        return this.value;
    }

    @Override
    public int compareTo(ConfigPath other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ConfigPath)) {
            return false;
        }
        return this.value.equals(((ConfigPath) other).value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }

    private static String requireSegment(String value) {
        String normalized = Objects.requireNonNull(value, "segment").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Config path segments cannot be blank");
        }
        if (normalized.indexOf('.') >= 0 || normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) {
            throw new IllegalArgumentException("Config path segment contains unsupported characters: " + value);
        }
        return normalized;
    }
}
