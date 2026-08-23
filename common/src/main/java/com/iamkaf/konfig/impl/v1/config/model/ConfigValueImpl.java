package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonElement;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.RestartRequirement;
//? if >=1.17 {
// Runtime model stores typed ResourceKey registry bindings on modern lines;
// <=1.16.5 stores the equivalent binding as a string registry id.
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
//? if >=1.21.11 {
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
//?}

@ApiStatus.Internal
public final class ConfigValueImpl<T> implements ConfigScreenValue<T> {
    private final String path;
    private final T defaultValue;
    private final EntryKind kind;
    private final Function<JsonElement, T> decoder;
    private final Function<T, JsonElement> encoder;
    private final Predicate<T> validator;
    private final String validationMessage;
    private final UnaryOperator<T> canonicalizer;
    private final boolean sync;
    private final boolean clientOnly;
    private final boolean serverOnly;
//? if >=1.21.11 {
    private final Supplier<T> remoteScreenValue;
    private final BooleanSupplier remoteScreenViewAvailable;
//?}
    private final RestartRequirement restartRequirement;
    private final Number rangeMin;
    private final Number rangeMax;
    private final List<DropdownOptionMetadata> dropdownOptions;
    private final List<String> dropdownOptionValues;
    private final boolean persistent;
    private final String inlineLabel;
    private final boolean inlineLabelTranslationKey;
    private final String inlineTarget;
    private final ImageOptions imageOptions;
//? if <=1.16.5 {
    private final String boundRegistryId;
//?} else {
    private final ResourceKey<? extends Registry<?>> boundRegistryKey;
//?}

    private volatile T localValue;
    private volatile T syncedValue;

    ConfigValueImpl(
            String path,
            T defaultValue,
            EntryKind kind,
            Function<JsonElement, T> decoder,
            Function<T, JsonElement> encoder,
            Predicate<T> validator,
            String validationMessage,
            UnaryOperator<T> canonicalizer,
            boolean sync,
            boolean clientOnly,
            boolean serverOnly,
            RestartRequirement restartRequirement,
            Number rangeMin,
            Number rangeMax,
            List<DropdownOptionMetadata> dropdownOptions,
            boolean persistent,
            String inlineLabel,
            boolean inlineLabelTranslationKey,
            String inlineTarget,
            ImageOptions imageOptions,
//? if >=1.21.11 {
            Supplier<T> remoteScreenValue,
            BooleanSupplier remoteScreenViewAvailable,
//?}
//? if <=1.16.5 {
            String boundRegistryId
//?} else {
            ResourceKey<? extends Registry<?>> boundRegistryKey
//?}
    ) {
        this.path = path;
        this.canonicalizer = canonicalizer == null ? UnaryOperator.identity() : canonicalizer;
        this.defaultValue = canonicalize(defaultValue);
        this.kind = kind;
        this.decoder = decoder;
        this.encoder = encoder;
        this.validator = validator;
        this.validationMessage = validationMessage;
        this.sync = sync;
        this.clientOnly = clientOnly;
        this.serverOnly = serverOnly;
//? if >=1.21.11 {
        this.remoteScreenValue = remoteScreenValue;
        this.remoteScreenViewAvailable = remoteScreenViewAvailable == null ? () -> false : remoteScreenViewAvailable;
//?}
        this.restartRequirement = restartRequirement;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.dropdownOptions = dropdownOptions == null || dropdownOptions.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<DropdownOptionMetadata>(dropdownOptions));
        this.dropdownOptionValues = dropdownOptionValues(this.dropdownOptions);
        this.persistent = persistent;
        this.inlineLabel = inlineLabel;
        this.inlineLabelTranslationKey = inlineLabelTranslationKey;
        this.inlineTarget = inlineTarget;
        this.imageOptions = imageOptions == null ? ImageOptions.defaults() : imageOptions;
//? if <=1.16.5 {
        this.boundRegistryId = boundRegistryId;
//?} else {
        this.boundRegistryKey = boundRegistryKey;
//?}
        this.localValue = this.defaultValue;
    }

    ConfigValueImpl(
            String path,
            T defaultValue,
            EntryKind kind,
            Function<JsonElement, T> decoder,
            Function<T, JsonElement> encoder,
            Predicate<T> validator,
            String validationMessage,
            UnaryOperator<T> canonicalizer,
            boolean sync,
            boolean clientOnly,
            boolean serverOnly,
            RestartRequirement restartRequirement,
            Number rangeMin,
            Number rangeMax,
            List<DropdownOptionMetadata> dropdownOptions,
//? if >=1.21.11 {
            Supplier<T> remoteScreenValue,
            BooleanSupplier remoteScreenViewAvailable,
//?}
//? if <=1.16.5 {
            String boundRegistryId
//?} else {
            ResourceKey<? extends Registry<?>> boundRegistryKey
//?}
    ) {
        this(
                path,
                defaultValue,
                kind,
                decoder,
                encoder,
                validator,
                validationMessage,
                canonicalizer,
                sync,
                clientOnly,
                serverOnly,
                restartRequirement,
                rangeMin,
                rangeMax,
                dropdownOptions,
                true,
                null,
                false,
                null,
                null,
//? if >=1.21.11 {
                remoteScreenValue,
                remoteScreenViewAvailable,
//?}
//? if <=1.16.5 {
                boundRegistryId
//?} else {
                boundRegistryKey
//?}
        );
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public T defaultValue() {
        return this.defaultValue;
    }

    @Override
    public T get() {
        T overlay = this.syncedValue;
        return overlay != null ? overlay : this.localValue;
    }

    @Override
    public void set(T value) {
        setLocal(value);
    }

    void setLocal(T value) {
        this.localValue = validateOrThrow(value);
    }

    void setSynced(T value) {
        this.syncedValue = validateOrFallback(value);
    }

    void clearSynced() {
        this.syncedValue = null;
    }

    T decodeOrFallback(JsonElement element) {
        try {
            if (element == null) {
                return this.defaultValue;
            }
            return decodeStrict(element);
        } catch (Exception ignored) {
            return this.defaultValue;
        }
    }

    public T decodeStrict(JsonElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Missing value for '" + this.path + "'.");
        }
        return validateOrThrow(this.decoder.apply(element));
    }

    @Override
    public T normalizeAndValidate(T value) {
        return validateOrThrow(value);
    }

    @Override
    public T copyValue(T value) {
        return decodeStrict(encodeValue(value));
    }

    JsonElement encodeCurrent() {
        return this.encoder.apply(this.localValue);
    }

//? if >=1.21.11
    @Override
    public JsonElement encodeValue(T value) {
        return this.encoder.apply(validateOrThrow(value));
    }

    public T localValue() {
        return this.localValue;
    }

    public String validationMessage() {
        return this.validationMessage;
    }

    @Override
    public boolean sync() {
        return this.sync;
    }

    @Override
    public boolean synchronizedOverlayActive() {
        return this.syncedValue != null;
    }

//? if >=1.21.11 {
    @Override
    public boolean remoteScreenViewAvailable() {
        return this.remoteScreenValue != null && this.remoteScreenViewAvailable.getAsBoolean();
    }

    @Override
    public T remoteScreenValue() {
        if (!remoteScreenViewAvailable()) {
            return get();
        }
        return validateOrFallback(this.remoteScreenValue.get());
    }
//?}

    public boolean clientOnly() {
        return this.clientOnly;
    }

    public boolean serverOnly() {
        return this.serverOnly;
    }

    public RestartRequirement restartRequirement() {
        return this.restartRequirement;
    }

    public boolean hasNumericRange() {
        return this.rangeMin != null && this.rangeMax != null;
    }

    public Number rangeMin() {
        return this.rangeMin;
    }

    public Number rangeMax() {
        return this.rangeMax;
    }

    public List<String> dropdownOptions() {
        return this.dropdownOptionValues;
    }

    public List<DropdownOptionMetadata> dropdownOptionMetadata() {
        return this.dropdownOptions;
    }

    public DropdownOptionMetadata dropdownOption(String value) {
        for (DropdownOptionMetadata option : this.dropdownOptions) {
            if (Objects.equals(option.value(), value)) {
                return option;
            }
        }
        return null;
    }

    public EntryKind kind() {
        return this.kind;
    }

    public boolean persistent() {
        return this.persistent;
    }

    public boolean isDecoration() {
        return this.kind == EntryKind.HEADER || this.kind == EntryKind.IMAGE || this.kind == EntryKind.INLINE_TEXT || this.kind == EntryKind.URL;
    }

    public String inlineLabel() {
        return this.inlineLabel;
    }

    public boolean inlineLabelTranslationKey() {
        return this.inlineLabelTranslationKey;
    }

    public String inlineUrl() {
        return this.inlineTarget;
    }

    public String inlineTarget() {
        return this.inlineTarget;
    }

    public ImageOptions imageOptions() {
        return this.imageOptions;
    }

    public boolean hasBoundRegistry() {
//? if <=1.16.5 {
        return this.boundRegistryId != null && !this.boundRegistryId.isEmpty();
//?} else {
        return this.boundRegistryKey != null;
//?}
    }

    private static List<String> dropdownOptionValues(List<DropdownOptionMetadata> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<String>();
        for (DropdownOptionMetadata option : options) {
            values.add(option.value());
        }
        return Collections.unmodifiableList(values);
    }

//? if <=1.16.5 {
    public String boundRegistryId() {
        return this.boundRegistryId;
    }
//?} else {
    public ResourceKey<? extends Registry<?>> boundRegistryKey() {
        return this.boundRegistryKey;
    }
//?}

    private T validateOrFallback(T value) {
        if (value == null) {
            return this.defaultValue;
        }
        T canonical = canonicalize(value);
        return this.validator.test(canonical) ? canonical : this.defaultValue;
    }

    private T validateOrThrow(T value) {
        Objects.requireNonNull(value, "Config value cannot be null for " + this.path);
        T canonical = canonicalize(value);
        if (!this.validator.test(canonical)) {
            throw new IllegalArgumentException(this.validationMessage + " (" + this.path + ")");
        }
        return canonical;
    }

    private T canonicalize(T value) {
        return this.canonicalizer.apply(value);
    }
}
