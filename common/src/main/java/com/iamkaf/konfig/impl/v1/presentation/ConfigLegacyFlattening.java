package com.iamkaf.konfig.impl.v1.presentation;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigPath;
import com.iamkaf.konfig.impl.v1.model.ConfigText;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigLegacyFlattening {
    public enum Strategy {
        FIELD,
        HEADER,
        IMAGE,
        TEXT,
        LINK,
        IGNORE
    }

    private final ConfigPath categoryPath;
    private final Strategy strategy;
    private final ConfigText reason;

    public ConfigLegacyFlattening(ConfigPath categoryPath, Strategy strategy, ConfigText reason) {
        this.categoryPath = Objects.requireNonNull(categoryPath, "categoryPath");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.reason = reason == null ? ConfigText.empty() : reason;
    }

    public ConfigPath categoryPath() {
        return this.categoryPath;
    }

    public Strategy strategy() {
        return this.strategy;
    }

    public ConfigText reason() {
        return this.reason;
    }
}
