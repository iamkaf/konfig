package com.iamkaf.konfig.impl.v1;

import com.iamkaf.konfig.api.v1.ImageOptions;

final class InfoPanelItem {
    final EntryKind kind;
    final String label;
    final String target;
    final ImageOptions imageOptions;

    InfoPanelItem(EntryKind kind, String label, String target, ImageOptions imageOptions) {
        this.kind = kind;
        this.label = label == null ? "" : label;
        this.target = target;
        this.imageOptions = imageOptions == null ? ImageOptions.defaults() : imageOptions;
    }
}
