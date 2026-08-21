package com.iamkaf.konfig.impl.v1.client.screen;

import org.jetbrains.annotations.ApiStatus;

//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so shared parsing, labels, and visibility helpers target the 1.17+ UI path.
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenHandle;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.KonfigManager;
import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import net.minecraft.network.chat.Component;
//? if <=1.18.2 {
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
public final class KonfigScreenSupport {
    private KonfigScreenSupport() {
    }

    public static List<EntryRef> collectEntries(String modIdFilter) {
        List<EntryRef> result = new ArrayList<EntryRef>();

        for (ConfigScreenHandle handle : KonfigManager.get().screenHandles()) {
            if (modIdFilter != null && !modIdFilter.equals(handle.modId())) {
                continue;
            }
            for (ConfigScreenValue<?> impl : handle.screenEntries()) {
                if (!isVisibleOnThisSide(impl)) {
                    continue;
                }

                boolean editable = !impl.isDecoration() && impl.kind() != EntryKind.CUSTOM;
                result.add(new EntryRef(handle, impl, editable));
            }
        }

        Collections.sort(result, Comparator.comparing(entry -> entry.handle.id()));
        return result;
    }

    private static boolean isVisibleOnThisSide(ConfigScreenValue<?> value) {
        if (value.clientOnly() && !KonfigRuntime.isClient()) {
            return false;
        }
        if (value.serverOnly() && KonfigRuntime.isClient()) {
            return false;
        }
        return true;
    }

    public static Component translate(String key, Object... args) {
//? if >=1.19 {
        return Component.translatable(key, args);
//?} else {
        return new TranslatableComponent(key, args);
//?}
    }

    public static Component text(String value) {
//? if >=1.19 {
        return Component.nullToEmpty(value);
//?} else {
        return new TextComponent(value == null ? "" : value);
//?}
    }

    public static Component translatedLabel(ConfigScreenHandle handle, ConfigScreenValue<?> value) {
        String key = "konfig.config." + handle.modId() + "." + handle.name() + "." + value.path();
        Component translated = translationOrNull(key);
        if (translated != null) {
            return translated;
        }

        String legacyKey = handle.modId() + ".config." + lastPathSegment(value.path());
        translated = translationOrNull(legacyKey);
        return translated == null ? text(fallbackLabel(handle, value)) : translated;
    }

    public static Component translatedEnumValue(EntryRef entry, Enum<?> value) {
        String valueName = value.name().toLowerCase(Locale.ROOT);
        String key = "konfig.value."
                + entry.handle.modId() + "."
                + entry.handle.name() + "."
                + entry.value.path() + "."
                + valueName;
        Component translated = translationOrNull(key);
        if (translated != null) {
            return translated;
        }

        String legacyKey = entry.handle.modId() + ".config." + lastPathSegment(entry.value.path()) + "." + valueName;
        translated = translationOrNull(legacyKey);
        return translated == null ? text(prettySegment(value.name())) : translated;
    }

    public static Component translatedDropdownValue(EntryRef entry, String option) {
        String valueName = option == null ? "" : option;
        String key = "konfig.value."
                + entry.handle.modId() + "."
                + entry.handle.name() + "."
                + entry.value.path() + "."
                + valueName;
        Component translated = translationOrNull(key);
        if (translated != null) {
            return translated;
        }

        String legacyKey = entry.handle.modId() + ".config." + lastPathSegment(entry.value.path()) + "." + valueName;
        translated = translationOrNull(legacyKey);
        return translated == null ? text(prettySegment(valueName)) : translated;
    }

    public static Component translatedDropdownOption(EntryRef entry, DropdownOptionMetadata option) {
        if (option == null) {
            return translatedDropdownValue(entry, "");
        }
        if (!isBlank(option.label())) {
            if (!option.labelTranslationKey()) {
                return text(option.label());
            }

            Component translated = translationOrNull(option.label());
            return translated == null ? text(option.label()) : translated;
        }
        return translatedDropdownValue(entry, option.value());
    }

    public static String translatedDropdownTooltip(DropdownOptionMetadata option) {
        return option == null ? "" : option.tooltip().resolve(key -> {
            Component translated = translationOrNull(key);
            return translated == null ? null : translated.getString();
        });
    }

    public static String translatedTooltip(ConfigScreenHandle handle, String path) {
        return handle.tooltip(path, key -> {
            Component translated = translationOrNull(key);
            return translated == null ? null : translated.getString();
        });
    }

    public static Component decorationLabel(ConfigScreenValue<?> value) {
        if (!value.inlineLabelTranslationKey()) {
            return text(value.inlineLabel());
        }

        Component translated = translationOrNull(value.inlineLabel());
        return translated == null ? text(value.inlineLabel()) : translated;
    }

    private static Component translationOrNull(String key) {
        Component translated = translate(key);
        return key.equals(translated.getString()) ? null : translated;
    }

    private static String lastPathSegment(String path) {
        int lastSeparator = path.lastIndexOf('.');
        if (lastSeparator < 0) {
            return path;
        }
        return path.substring(lastSeparator + 1);
    }

    public static Component contextLabel(ConfigScreenHandle handle, ConfigScreenValue<?> value) {
        List<String> parts = new ArrayList<String>();
        parts.add(prettySegment(handle.name()));
        String[] pathParts = value.path().split("\\.");
        for (int i = 0; i < pathParts.length - 1; i++) {
            parts.add(prettySegment(pathParts[i]));
        }
        return text(String.join(" / ", parts));
    }

    public static String fallbackLabel(ConfigScreenHandle handle, ConfigScreenValue<?> value) {
//? if >=26.1 {
        String[] pathParts = value.path().split("\\.");
        return prettySegment(pathParts[pathParts.length - 1]);
//?} else {
        List<String> parts = new ArrayList<String>();
        parts.add(prettySegment(handle.name()));
        String[] pathParts = value.path().split("\\.");
        for (String pathPart : pathParts) {
            parts.add(prettySegment(pathPart));
        }
        return String.join(" > ", parts);
//?}
    }

    public static String prettySegment(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (character == '_' || character == '-' || character == '.') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
                    builder.append(' ');
                }
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext) {
                builder.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else if (Character.isUpperCase(character) && i > 0 && Character.isLowerCase(raw.charAt(i - 1))) {
                builder.append(' ').append(character);
            } else {
                builder.append(Character.toLowerCase(character));
            }
        }
        if (builder.length() > 0) {
            builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        }
        return builder.toString().trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
//?}
