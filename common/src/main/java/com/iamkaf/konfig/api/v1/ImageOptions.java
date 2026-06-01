package com.iamkaf.konfig.api.v1;

import java.util.Objects;

public final class ImageOptions {
    public enum Align {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum CaptionPosition {
        RIGHT,
        BELOW,
        NONE
    }

    private static final ImageOptions DEFAULTS = builder().build();

    private final int width;
    private final int height;
    private final int padding;
    private final Align align;
    private final CaptionPosition captionPosition;

    private ImageOptions(Builder builder) {
        this.width = requirePositive(builder.width, "width");
        this.height = requirePositive(builder.height, "height");
        this.padding = requireNonNegative(builder.padding, "padding");
        this.align = Objects.requireNonNull(builder.align, "align");
        this.captionPosition = Objects.requireNonNull(builder.captionPosition, "captionPosition");
    }

    public static ImageOptions defaults() {
        return DEFAULTS;
    }

    public static ImageOptions icon() {
        return builder().size(16, 16).build();
    }

    public static ImageOptions banner(int width, int height) {
        return builder()
                .size(width, height)
                .align(Align.CENTER)
                .captionPosition(CaptionPosition.BELOW)
                .build();
    }

    public static Builder builder() {
        return new Builder();
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

    public Align align() {
        return this.align;
    }

    public CaptionPosition captionPosition() {
        return this.captionPosition;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static int requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return value;
    }

    public static final class Builder {
        private int width = 28;
        private int height = 28;
        private int padding = 3;
        private Align align = Align.LEFT;
        private CaptionPosition captionPosition = CaptionPosition.RIGHT;

        private Builder() {
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder padding(int padding) {
            this.padding = padding;
            return this;
        }

        public Builder align(Align align) {
            this.align = align;
            return this;
        }

        public Builder captionPosition(CaptionPosition captionPosition) {
            this.captionPosition = captionPosition;
            return this;
        }

        public ImageOptions build() {
            return new ImageOptions(this);
        }
    }
}
