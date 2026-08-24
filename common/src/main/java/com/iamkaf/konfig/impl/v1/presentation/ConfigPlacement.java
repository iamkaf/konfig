package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigPlacement {
    public enum Region {
        BODY,
        HEADER_ACTIONS,
        FOOTER_ACTIONS,
        INFO_PANEL
    }

    private final ConfigPresentationIdentity page;
    private final ConfigPresentationIdentity tab;
    private final ConfigPresentationIdentity group;
    private final Region region;

    public ConfigPlacement(
            ConfigPresentationIdentity page,
            ConfigPresentationIdentity tab,
            ConfigPresentationIdentity group,
            Region region
    ) {
        this.page = Objects.requireNonNull(page, "page");
        this.tab = tab;
        this.group = group;
        this.region = region == null ? Region.BODY : region;
        if (tab != null && !page.config().equals(tab.config())) {
            throw new IllegalArgumentException("Page and tab belong to different configs");
        }
        if (group != null && !page.config().equals(group.config())) {
            throw new IllegalArgumentException("Page and group belong to different configs");
        }
    }

    public static ConfigPlacement body(ConfigPresentationIdentity page, ConfigPresentationIdentity tab) {
        return new ConfigPlacement(page, tab, null, Region.BODY);
    }

    public ConfigPresentationIdentity page() {
        return this.page;
    }

    public ConfigPresentationIdentity tab() {
        return this.tab;
    }

    public ConfigPresentationIdentity group() {
        return this.group;
    }

    public Region region() {
        return this.region;
    }
}
