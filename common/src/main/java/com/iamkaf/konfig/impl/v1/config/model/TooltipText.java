package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Function;

@ApiStatus.Internal
public final class TooltipText {
    private static final TooltipText EMPTY = new TooltipText(Kind.LITERAL, "");

    private final Kind kind;
    private final String value;

    private TooltipText(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public static TooltipText empty() {
        return EMPTY;
    }

    public static TooltipText literal(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? EMPTY : new TooltipText(Kind.LITERAL, normalized);
    }

    public static TooltipText translationKey(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("translationKey cannot be blank");
        }
        return new TooltipText(Kind.TRANSLATION_KEY, normalized);
    }

    public boolean isEmpty() {
        return this.value.isEmpty();
    }

    public String resolve(Function<String, String> translationResolver) {
        Objects.requireNonNull(translationResolver, "translationResolver");
        if (this.kind == Kind.LITERAL) {
            return this.value;
        }

        String translated = translationResolver.apply(this.value);
        return translated == null ? this.value : translated;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private enum Kind {
        LITERAL,
        TRANSLATION_KEY
    }
}
