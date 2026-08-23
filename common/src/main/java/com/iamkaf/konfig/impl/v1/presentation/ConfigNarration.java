package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigText;

@ApiStatus.Internal
public final class ConfigNarration {
    private static final ConfigNarration EMPTY = new ConfigNarration(
            ConfigText.empty(),
            ConfigText.empty(),
            ConfigText.empty(),
            ConfigText.empty()
    );

    private final ConfigText label;
    private final ConfigText help;
    private final ConfigText value;
    private final ConfigText changeAnnouncement;

    public ConfigNarration(ConfigText label, ConfigText help, ConfigText value, ConfigText changeAnnouncement) {
        this.label = label == null ? ConfigText.empty() : label;
        this.help = help == null ? ConfigText.empty() : help;
        this.value = value == null ? ConfigText.empty() : value;
        this.changeAnnouncement = changeAnnouncement == null ? ConfigText.empty() : changeAnnouncement;
    }

    public static ConfigNarration empty() {
        return EMPTY;
    }

    public ConfigText label() {
        return this.label;
    }

    public ConfigText help() {
        return this.help;
    }

    public ConfigText value() {
        return this.value;
    }

    public ConfigText changeAnnouncement() {
        return this.changeAnnouncement;
    }
}
