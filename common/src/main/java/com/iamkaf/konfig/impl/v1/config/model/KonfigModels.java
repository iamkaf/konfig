package com.iamkaf.konfig.impl.v1.config.model;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.iamkaf.konfig.api.v1.ConfigMigration;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.SyncMode;
//? if >=1.17 {
// Model construction accepts typed ResourceKey registry bindings on modern
// lines; legacy values are built with string registry ids instead.
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
//?}

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@ApiStatus.Internal
public final class KonfigModels {
    private KonfigModels() {
    }

    public static InfoPanelItem infoPanelItem(
            EntryKind kind,
            String label,
            String target,
            ImageOptions imageOptions,
            boolean labelTranslationKey
    ) {
        return new InfoPanelItem(kind, label, target, imageOptions, labelTranslationKey);
    }

    public static DropdownOptionMetadata dropdownOption(
            String value,
            String label,
            boolean labelTranslationKey,
            String tooltip,
            boolean tooltipTranslationKey,
            List<InfoPanelItem> info
    ) {
        return new DropdownOptionMetadata(value, label, labelTranslationKey, tooltip, tooltipTranslationKey, info);
    }

    public static <T> ConfigValueImpl<T> configValue(
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
//? if <=1.16.5 {
            String boundRegistryId
//?} else {
            ResourceKey<? extends Registry<?>> boundRegistryKey
//?}
    ) {
        return new ConfigValueImpl<T>(
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
//? if <=1.16.5 {
                boundRegistryId
//?} else {
                boundRegistryKey
//?}
        );
    }

    public static ConfigValueImpl<String> inlineDecorationValue(
            String path,
            EntryKind kind,
            String label,
            String target,
            ImageOptions imageOptions,
            boolean labelTranslationKey
    ) {
        return new ConfigValueImpl<String>(
                path,
                label,
                kind,
                JsonElement::getAsString,
                JsonPrimitive::new,
                value -> true,
                "Invalid decoration value",
                UnaryOperator.identity(),
                false,
                false,
                false,
                RestartRequirement.NONE,
                null,
                null,
                Collections.emptyList(),
                false,
                label,
                labelTranslationKey,
                target,
                imageOptions,
//? if <=1.16.5 {
                null
//?} else {
                null
//?}
        );
    }

    public static ConfigHandleImpl configHandle(
            String modId,
            String name,
            ConfigScope scope,
            SyncMode syncMode,
            Path path,
            LinkedHashMap<String, ConfigValueImpl<?>> entries,
            LinkedHashMap<String, String> entryComments,
            LinkedHashMap<String, String> categoryComments,
            LinkedHashMap<String, String> entryTooltips,
            LinkedHashMap<String, String> categoryTooltips,
            List<InfoPanelItem> globalInfo,
            LinkedHashMap<String, List<InfoPanelItem>> categoryInfo,
            LinkedHashMap<String, List<InfoPanelItem>> entryInfo,
            String fileComment,
            int schemaVersion,
            LinkedHashMap<Integer, ConfigMigration> migrations
    ) {
        return new ConfigHandleImpl(
                modId,
                name,
                scope,
                syncMode,
                path,
                entries,
                entryComments,
                categoryComments,
                entryTooltips,
                categoryTooltips,
                globalInfo,
                categoryInfo,
                entryInfo,
                fileComment,
                schemaVersion,
                migrations
        );
    }
}
