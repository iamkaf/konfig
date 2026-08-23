package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@ApiStatus.Internal
public final class ConfigAccessInput {
    public enum Kind {
        RUNTIME_SIDE,
        VALUE_SOURCE,
        SERVER_WRITE_PERMISSION,
        FIELD_VALUE
    }

    private final Kind kind;
    private final String subject;
    private final Set<String> acceptedValues;

    public ConfigAccessInput(Kind kind, String subject, Collection<String> acceptedValues) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.subject = normalize(subject);
        if (kind == Kind.FIELD_VALUE && this.subject.isEmpty()) {
            throw new IllegalArgumentException("A field-value input must name its field");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        if (acceptedValues != null) {
            for (String value : acceptedValues) {
                String candidate = normalize(value);
                if (!candidate.isEmpty()) {
                    normalized.add(candidate);
                }
            }
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("An access input must accept at least one value");
        }
        this.acceptedValues = Collections.unmodifiableSet(normalized);
    }

    public Kind kind() {
        return this.kind;
    }

    public String subject() {
        return this.subject;
    }

    public Set<String> acceptedValues() {
        return this.acceptedValues;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
