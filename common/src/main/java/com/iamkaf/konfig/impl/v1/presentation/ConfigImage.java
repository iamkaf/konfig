package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigImage {
    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum CaptionPosition {
        RIGHT,
        BELOW,
        NONE
    }

    private final String resource;
    private final int width;
    private final int height;
    private final int padding;
    private final Alignment alignment;
    private final CaptionPosition captionPosition;

    public ConfigImage(
            String resource,
            int width,
            int height,
            int padding,
            Alignment alignment,
            CaptionPosition captionPosition
    ) {
        this.resource = Objects.requireNonNull(resource, "resource").trim();
        if (this.resource.isEmpty()) {
            throw new IllegalArgumentException("Image resource cannot be blank");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        if (padding < 0) {
            throw new IllegalArgumentException("Image padding cannot be negative");
        }
        this.width = width;
        this.height = height;
        this.padding = padding;
        this.alignment = Objects.requireNonNull(alignment, "alignment");
        this.captionPosition = Objects.requireNonNull(captionPosition, "captionPosition");
    }

    public String resource() {
        return this.resource;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int padding() {
        return this.padding;
    }

    public Alignment alignment() {
        return this.alignment;
    }

    public CaptionPosition captionPosition() {
        return this.captionPosition;
    }
}
