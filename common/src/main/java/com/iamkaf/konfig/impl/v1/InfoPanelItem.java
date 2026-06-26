package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ImageOptions;

@ApiStatus.Internal
public final class InfoPanelItem {
    public final EntryKind kind;
    public final String label;
    public final String target;
    public final ImageOptions imageOptions;
    public final boolean labelTranslationKey;

    InfoPanelItem(EntryKind kind, String label, String target, ImageOptions imageOptions) {
        this(kind, label, target, imageOptions, false);
    }

    InfoPanelItem(EntryKind kind, String label, String target, ImageOptions imageOptions, boolean labelTranslationKey) {
        this.kind = kind;
        this.label = label == null ? "" : label;
        this.target = target;
        this.imageOptions = imageOptions == null ? ImageOptions.defaults() : imageOptions;
        this.labelTranslationKey = labelTranslationKey;
    }
}
