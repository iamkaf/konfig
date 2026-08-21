package com.iamkaf.konfig.impl.v1.config.builder;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.DropdownOptionBuilder;
import com.iamkaf.konfig.api.v1.InfoPanelBuilder;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.InfoPanelItem;
import com.iamkaf.konfig.impl.v1.config.model.KonfigModels;
import com.iamkaf.konfig.impl.v1.config.model.TooltipText;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@ApiStatus.Internal
final class DropdownOptionBuilderImpl implements DropdownOptionBuilder {
    private final String value;
    private String label = "";
    private boolean labelTranslationKey;
    private TooltipText tooltip = TooltipText.empty();
    private List<InfoPanelItem> info = Collections.emptyList();

    DropdownOptionBuilderImpl(String value) {
        this.value = value;
    }

    @Override
    public DropdownOptionBuilder label(String label) {
        this.label = normalizeOptionalText(label);
        this.labelTranslationKey = false;
        return this;
    }

    @Override
    public DropdownOptionBuilder labelKey(String translationKey) {
        this.label = requireText(translationKey, "translationKey");
        this.labelTranslationKey = true;
        return this;
    }

    @Override
    public DropdownOptionBuilder tooltip(String tooltip) {
        this.tooltip = TooltipText.literal(tooltip);
        return this;
    }

    @Override
    public DropdownOptionBuilder tooltipKey(String translationKey) {
        this.tooltip = TooltipText.translationKey(translationKey);
        return this;
    }

    @Override
    public DropdownOptionBuilder info(Consumer<InfoPanelBuilder> builder) {
        this.info = ConfigBuilderImpl.buildInfo(builder);
        return this;
    }

    DropdownOptionMetadata build() {
        return KonfigModels.dropdownOption(
                this.value,
                this.label,
                this.labelTranslationKey,
                this.tooltip,
                this.info
        );
    }

    private static String normalizeOptionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requireText(String value, String name) {
        String normalized = normalizeOptionalText(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
