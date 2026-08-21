package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public final class DropdownOptionMetadata {
    private final String value;
    private final String label;
    private final boolean labelTranslationKey;
    private final TooltipText tooltip;
    private final List<InfoPanelItem> info;

    DropdownOptionMetadata(
            String value,
            String label,
            boolean labelTranslationKey,
            TooltipText tooltip,
            List<InfoPanelItem> info
    ) {
        this.value = value;
        this.label = label == null ? "" : label;
        this.labelTranslationKey = labelTranslationKey;
        this.tooltip = tooltip == null ? TooltipText.empty() : tooltip;
        this.info = info == null || info.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<InfoPanelItem>(info));
    }

    public String value() {
        return this.value;
    }

    public String label() {
        return this.label;
    }

    public boolean labelTranslationKey() {
        return this.labelTranslationKey;
    }

    public TooltipText tooltip() {
        return this.tooltip;
    }

    public List<InfoPanelItem> info() {
        return this.info;
    }
}
