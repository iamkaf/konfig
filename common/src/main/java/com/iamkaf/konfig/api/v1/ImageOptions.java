package com.iamkaf.konfig.api.v1;

import java.util.Objects;

/**
 * Layout options for image rows in generated config screens and information panels.
 */
public final class ImageOptions {
    /**
     * Horizontal alignment for an image row.
     */
    public enum Align {
        /**
         * Align the image to the left edge of the available content area.
         */
        LEFT,
        /**
         * Center the image in the available content area.
         */
        CENTER,
        /**
         * Align the image to the right edge of the available content area.
         */
        RIGHT
    }

    /**
     * Placement of an image caption.
     */
    public enum CaptionPosition {
        /**
         * Show the caption to the right of the image.
         */
        RIGHT,
        /**
         * Show the caption below the image.
         */
        BELOW,
        /**
         * Do not show a caption.
         */
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

    /**
     * Returns the default image options.
     *
     * @return the default options
     */
    public static ImageOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns options for a small icon-style image.
     *
     * @return icon image options
     */
    public static ImageOptions icon() {
        return builder().size(16, 16).build();
    }

    /**
     * Returns options for a centered banner image with a below-image caption.
     *
     * @param width the banner width
     * @param height the banner height
     * @return banner image options
     */
    public static ImageOptions banner(int width, int height) {
        return builder()
                .size(width, height)
                .align(Align.CENTER)
                .captionPosition(CaptionPosition.BELOW)
                .build();
    }

    /**
     * Starts a new image-options builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the target image width.
     *
     * @return the width in screen pixels
     */
    public int width() {
        return this.width;
    }

    /**
     * Returns the target image height.
     *
     * @return the height in screen pixels
     */
    public int height() {
        return this.height;
    }

    /**
     * Returns the padding around the image.
     *
     * @return the padding in screen pixels
     */
    public int padding() {
        return this.padding;
    }

    /**
     * Returns the horizontal alignment.
     *
     * @return the alignment
     */
    public Align align() {
        return this.align;
    }

    /**
     * Returns the caption position.
     *
     * @return the caption position
     */
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

    /**
     * Mutable builder for {@link ImageOptions}.
     */
    public static final class Builder {
        private int width = 28;
        private int height = 28;
        private int padding = 3;
        private Align align = Align.LEFT;
        private CaptionPosition captionPosition = CaptionPosition.RIGHT;

        private Builder() {
        }

        /**
         * Sets the image size.
         *
         * @param width the target width in screen pixels
         * @param height the target height in screen pixels
         * @return this builder
         */
        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * Sets the image padding.
         *
         * @param padding the padding in screen pixels
         * @return this builder
         */
        public Builder padding(int padding) {
            this.padding = padding;
            return this;
        }

        /**
         * Sets the horizontal alignment.
         *
         * @param align the alignment
         * @return this builder
         */
        public Builder align(Align align) {
            this.align = align;
            return this;
        }

        /**
         * Sets the caption position.
         *
         * @param captionPosition the caption position
         * @return this builder
         */
        public Builder captionPosition(CaptionPosition captionPosition) {
            this.captionPosition = captionPosition;
            return this;
        }

        /**
         * Builds immutable image options.
         *
         * @return the image options
         */
        public ImageOptions build() {
            return new ImageOptions(this);
        }
    }
}
