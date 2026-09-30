package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.google.gson.JsonElement;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
public interface ConfigScreenValue<T> extends ConfigValue<T> {
    T normalizeAndValidate(T value);

    T copyValue(T value);

    JsonElement encodeValue(T value);

    boolean sync();

    boolean synchronizedOverlayActive();

    boolean remoteScreenViewAvailable();

    T remoteScreenValue();

    boolean clientOnly();

    boolean serverOnly();

    RestartRequirement restartRequirement();

    boolean hasNumericRange();

    Number rangeMin();

    Number rangeMax();

    List<String> dropdownOptions();

    List<DropdownOptionMetadata> dropdownOptionMetadata();

    DropdownOptionMetadata dropdownOption(String value);

    EntryKind kind();

    boolean persistent();

    boolean isDecoration();

    String inlineLabel();

    boolean inlineLabelTranslationKey();

    String inlineUrl();

    String inlineTarget();

    ImageOptions imageOptions();

    boolean hasBoundRegistry();

    ResourceKey<? extends Registry<?>> boundRegistryKey();
}
