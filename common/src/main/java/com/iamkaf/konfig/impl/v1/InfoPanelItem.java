package com.iamkaf.konfig.impl.v1;

import com.iamkaf.konfig.api.v1.ImageOptions;

public final class InfoPanelItem {
    public final EntryKind kind;
    public final String label;
    public final String target;
    public final ImageOptions imageOptions;

    InfoPanelItem(EntryKind kind, String label, String target, ImageOptions imageOptions) {
        this.kind = kind;
        this.label = label == null ? "" : label;
        this.target = target;
        this.imageOptions = imageOptions == null ? ImageOptions.defaults() : imageOptions;
    }
}
