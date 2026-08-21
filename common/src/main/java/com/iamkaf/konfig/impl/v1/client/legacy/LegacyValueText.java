package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
public final class LegacyValueText {
    private LegacyValueText() {
    }

    public interface Translator<T> {
        T literal(String value);

        T translate(String key, Object... args);

        T translationOrNull(String key);

        String string(T value);
    }

    public static <T> T defaultScreenTitle(String modIdFilter, String screenTitle, Translator<T> translator) {
        if (!isBlank(screenTitle)) {
            return translator.literal(screenTitle);
        }
        if (!isBlank(modIdFilter)) {
            return translatedModTitle(modIdFilter, translator);
        }
        return translator.translate("konfig.screen.title.configurations");
    }

    public static <T> T translatedModTitle(String modId, Translator<T> translator) {
        String titleKey = "konfig.config." + modId + ".title";
        T translated = translator.translate(titleKey);
        if (!titleKey.equals(translator.string(translated))) {
            return translated;
        }

        String legacyTitleKey = modId + ".configuration.title";
        translated = translator.translate(legacyTitleKey);
        if (!legacyTitleKey.equals(translator.string(translated))) {
            return translated;
        }

        return translator.literal(prettySegment(modId));
    }

    public static <T> T booleanText(boolean value, Translator<T> translator) {
        return translator.translate(value ? "options.on" : "options.off");
    }

    public static <T> T enumText(LegacyConfigEntry entry, Enum<?> value, Translator<T> translator) {
        String option = value.name().toLowerCase(Locale.ROOT);
        T translated = translator.translationOrNull(valueTranslationKey(entry, option));
        if (translated != null) {
            return translated;
        }

        translated = translator.translationOrNull(legacyValueTranslationKey(entry, option));
        return translated == null ? translator.literal(prettySegment(value.name())) : translated;
    }

    public static <T> T dropdownText(LegacyConfigEntry entry, String option, Translator<T> translator) {
        DropdownOptionMetadata metadata = entry.value().dropdownOption(option);
        if (metadata != null) {
            return translatedDropdownOption(entry, metadata, translator);
        }
        return dropdownValueText(entry, option, translator);
    }

    public static <T> T dropdownValueText(LegacyConfigEntry entry, String option, Translator<T> translator) {
        T translated = translator.translationOrNull(valueTranslationKey(entry, option));
        if (translated != null) {
            return translated;
        }

        translated = translator.translationOrNull(legacyValueTranslationKey(entry, option));
        return translated == null ? translator.literal(prettySegment(option)) : translated;
    }

    public static <T> T translatedDropdownOption(
            LegacyConfigEntry entry,
            DropdownOptionMetadata option,
            Translator<T> translator
    ) {
        if (option == null) {
            return dropdownValueText(entry, "", translator);
        }
        if (!isBlank(option.label())) {
            if (!option.labelTranslationKey()) {
                return translator.literal(option.label());
            }

            T translated = translator.translationOrNull(option.label());
            return translated == null ? translator.literal(option.label()) : translated;
        }
        return dropdownValueText(entry, option.value(), translator);
    }

    public static String translatedDropdownTooltip(DropdownOptionMetadata option, Translator<?> translator) {
        return option == null ? "" : option.tooltip().resolve(key -> {
            Object translated = translator.translationOrNull(key);
            return translated == null ? null : translatorString(translator, translated);
        });
    }

    public static String translatedTooltip(LegacyConfigEntry entry, Translator<?> translator) {
        if (entry.value().kind() == EntryKind.URL && !isBlank(entry.value().inlineTarget())) {
            return entry.value().inlineTarget();
        }
        return entry.handle().tooltip(entry.value().path(), key -> {
            Object translated = translator.translationOrNull(key);
            return translated == null ? null : translatorString(translator, translated);
        });
    }

    public static String colorText(ConfigValueImpl<?> value, int color) {
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return ColorValueHelper.formatArgb(color);
        }
        return ColorValueHelper.formatRgb(color);
    }

    public static <T> T stringListText(List<String> values, Translator<T> translator) {
        if (values.isEmpty()) {
            return translator.translate("konfig.screen.list.empty");
        }
        if (values.size() == 1) {
            return translator.literal(values.get(0));
        }
        if (values.size() == 2) {
            return translator.literal(values.get(0) + ", " + values.get(1));
        }
        return translator.translate("konfig.screen.list.summary", values.get(0), Integer.valueOf(values.size() - 1));
    }

    public static String contextLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        List<String> parts = new ArrayList<String>();
        parts.add(prettySegment(handle.name()));
        String[] pathParts = value.path().split("\\.");
        for (int i = 0; i < pathParts.length - 1; i++) {
            parts.add(prettySegment(pathParts[i]));
        }
        return String.join(" / ", parts);
    }

    public static String fallbackLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        List<String> parts = new ArrayList<String>();
        parts.add(prettySegment(handle.name()));
        String[] pathParts = value.path().split("\\.");
        for (String pathPart : pathParts) {
            parts.add(prettySegment(pathPart));
        }
        return String.join(" > ", parts);
    }

    public static String lastPathSegment(String path) {
        int lastSeparator = path.lastIndexOf('.');
        if (lastSeparator < 0) {
            return path;
        }
        return path.substring(lastSeparator + 1);
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

    private static String valueTranslationKey(LegacyConfigEntry entry, String option) {
        return "konfig.value."
                + entry.handle().modId()
                + "."
                + entry.handle().name()
                + "."
                + entry.value().path()
                + "."
                + option;
    }

    private static String legacyValueTranslationKey(LegacyConfigEntry entry, String option) {
        return entry.handle().modId()
                + ".config."
                + lastPathSegment(entry.value().path())
                + "."
                + option;
    }

    @SuppressWarnings("unchecked")
    private static <T> String translatorString(Translator<T> translator, Object value) {
        return translator.string((T) value);
    }
}
