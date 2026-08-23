package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.RestartRequirement;
//? if >=1.21.11
import com.google.gson.JsonElement;
//? if >=1.17 {
// Screen-facing values expose typed registry keys on modern lines; legacy
// screens read string registry ids from the same conceptual model seam.
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.List;

@ApiStatus.Internal
public interface ConfigScreenValue<T> extends ConfigValue<T> {
    T normalizeAndValidate(T value);

    T copyValue(T value);

//? if >=1.21.11
    JsonElement encodeValue(T value);

    boolean sync();

    boolean synchronizedOverlayActive();

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

//? if <=1.16.5 {
    String boundRegistryId();
//?} else {
    ResourceKey<? extends Registry<?>> boundRegistryKey();
//?}
}
