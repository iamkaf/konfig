package com.iamkaf.konfig.api.v1;

//? if >=1.21.11
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} elif >=1.17 {
import net.minecraft.resources.ResourceLocation;
//?}

import java.util.List;
import java.util.function.Consumer;

/**
 * Builds and registers a Konfig configuration file.
 *
 * <p>A builder owns the schema, file metadata, visible screen layout, and values for one
 * configuration handle. Builders are mutable and are intended to be used during mod initialization.</p>
 */
public interface ConfigBuilder {
    /**
     * Sets where this configuration is logically owned.
     *
     * @param scope the config scope
     * @return this builder
     */
    ConfigBuilder scope(ConfigScope scope);

    /**
     * Sets when values from this configuration should synchronize.
     *
     * @param mode the synchronization mode
     * @return this builder
     */
    ConfigBuilder syncMode(SyncMode mode);

    /**
     * Sets the file name used under the config directory.
     *
     * @param fileName the file name, including extension
     * @return this builder
     */
    ConfigBuilder fileName(String fileName);

    /**
     * Sets the schema version stored with this configuration.
     *
     * @param version the current schema version
     * @return this builder
     */
    ConfigBuilder schemaVersion(int version);

    /**
     * Registers a migration from an older schema version.
     *
     * @param fromVersion the schema version this migration reads
     * @param migration the migration callback
     * @return this builder
     */
    ConfigBuilder migrate(int fromVersion, ConfigMigration migration);

    /**
     * Enters a nested category path for following entries.
     *
     * @param category the category segment to push
     * @return this builder
     */
    ConfigBuilder push(String category);

    /**
     * Leaves the current category path.
     *
     * @return this builder
     */
    ConfigBuilder pop();

    /**
     * Adds a file comment for the next value or section.
     *
     * @param comment the comment text
     * @return this builder
     */
    ConfigBuilder comment(String comment);

    /**
     * Adds a file comment for the current category.
     *
     * @param comment the category comment text
     * @return this builder
     */
    ConfigBuilder categoryComment(String comment);

    /**
     * Adds screen tooltip text for the current category.
     *
     * @param tooltip the tooltip text
     * @return this builder
     */
    ConfigBuilder categoryTooltip(String tooltip);

    /**
     * Adds information-panel content for the whole config screen.
     *
     * @param builder the information-panel builder callback
     * @return this builder
     */
    ConfigBuilder info(Consumer<InfoPanelBuilder> builder);

    /**
     * Adds information-panel content for the current category.
     *
     * @param builder the information-panel builder callback
     * @return this builder
     */
    ConfigBuilder categoryInfo(Consumer<InfoPanelBuilder> builder);

    /**
     * Adds a visible header row to the generated screen.
     *
     * @param text the literal header text
     * @return this builder
     */
    ConfigBuilder header(String text);

    /**
     * Adds a visible translated header row to the generated screen.
     *
     * @param translationKey the translation key for the header
     * @return this builder
     */
    ConfigBuilder headerKey(String translationKey);

//? if >=1.21.11 {
    /**
     * Adds an image decoration row.
     *
     * @param textureId the texture identifier
     * @return this builder
     */
    ConfigBuilder image(Identifier textureId);

    /**
     * Adds an image decoration row.
     *
     * @param textureId the texture identifier
     * @param options image layout options
     * @return this builder
     */
    ConfigBuilder image(Identifier textureId, ImageOptions options);

    /**
     * Adds an image decoration row with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @return this builder
     */
    ConfigBuilder image(Identifier textureId, String caption);

    /**
     * Adds an image decoration row with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @param options image layout options
     * @return this builder
     */
    ConfigBuilder image(Identifier textureId, String caption, ImageOptions options);
//?} elif >=1.17 {
    /**
     * Adds an image decoration row.
     *
     * @param textureId the texture identifier
     * @return this builder
     */
    ConfigBuilder image(ResourceLocation textureId);

    /**
     * Adds an image decoration row.
     *
     * @param textureId the texture identifier
     * @param options image layout options
     * @return this builder
     */
    ConfigBuilder image(ResourceLocation textureId, ImageOptions options);

    /**
     * Adds an image decoration row with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @return this builder
     */
    ConfigBuilder image(ResourceLocation textureId, String caption);

    /**
     * Adds an image decoration row with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @param options image layout options
     * @return this builder
     */
    ConfigBuilder image(ResourceLocation textureId, String caption, ImageOptions options);
//?} else {
    /**
     * Adds an image decoration row.
     *
     * @param textureId the loader/version texture identifier
     * @return this builder
     */
    ConfigBuilder image(Object textureId);

    /**
     * Adds an image decoration row.
     *
     * @param textureId the loader/version texture identifier
     * @param options image layout options
     * @return this builder
     */
    ConfigBuilder image(Object textureId, ImageOptions options);

    /**
     * Adds an image decoration row with a caption.
     *
     * @param textureId the loader/version texture identifier
     * @param caption the visible caption
     * @return this builder
     */
    ConfigBuilder image(Object textureId, String caption);

    /**
     * Adds an image decoration row with a caption.
     *
     * @param textureId the loader/version texture identifier
     * @param caption the visible caption
     * @param options image layout options
     * @return this builder
     */
    ConfigBuilder image(Object textureId, String caption, ImageOptions options);
//?}

    /**
     * Adds a literal informational text row.
     *
     * @param text the text to show
     * @return this builder
     */
    ConfigBuilder inlineText(String text);

    /**
     * Adds a URL row.
     *
     * @param label the visible link label
     * @param url the URL to open
     * @return this builder
     */
    ConfigBuilder url(String label, String url);

    /**
     * Adds a boolean config value.
     *
     * @param key the config key
     * @param defaultValue the default value
     * @return a value builder for the new entry
     */
    ValueBuilder<Boolean> bool(String key, boolean defaultValue);

    /**
     * Adds an integer value with a bounded range.
     *
     * @param key the config key
     * @param defaultValue the default value
     * @param min the inclusive minimum
     * @param max the inclusive maximum
     * @return a value builder for the new entry
     */
    ValueBuilder<Integer> intRange(String key, int defaultValue, int min, int max);

    /**
     * Adds a long value with a bounded range.
     *
     * @param key the config key
     * @param defaultValue the default value
     * @param min the inclusive minimum
     * @param max the inclusive maximum
     * @return a value builder for the new entry
     */
    ValueBuilder<Long> longRange(String key, long defaultValue, long min, long max);

    /**
     * Adds a double value with a bounded range.
     *
     * @param key the config key
     * @param defaultValue the default value
     * @param min the inclusive minimum
     * @param max the inclusive maximum
     * @return a value builder for the new entry
     */
    ValueBuilder<Double> doubleRange(String key, double defaultValue, double min, double max);

    /**
     * Adds a string value with length bounds.
     *
     * @param key the config key
     * @param defaultValue the default value
     * @param minLen the inclusive minimum length
     * @param maxLen the inclusive maximum length
     * @return a string value builder for the new entry
     */
    StringValueBuilder string(String key, String defaultValue, int minLen, int maxLen);

    /**
     * Adds a list of strings.
     *
     * @param key the config key
     * @param defaultValue the default list value
     * @return a string-list value builder for the new entry
     */
    StringListValueBuilder stringList(String key, List<String> defaultValue);

    /**
     * Adds a string dropdown from fixed option values.
     *
     * @param key the config key
     * @param defaultValue the default option value
     * @param options the allowed option values
     * @return a value builder for the new entry
     */
    ValueBuilder<String> dropdown(String key, String defaultValue, List<String> options);

    /**
     * Adds a string dropdown with per-option metadata.
     *
     * @param key the config key
     * @param defaultValue the default option value
     * @param options the dropdown options callback
     * @return a value builder for the new entry
     */
    ValueBuilder<String> dropdown(String key, String defaultValue, Consumer<DropdownOptionsBuilder> options);

    /**
     * Adds an enum value.
     *
     * @param key the config key
     * @param defaultValue the default enum constant
     * @param <E> the enum type
     * @return a value builder for the new entry
     */
    <E extends Enum<E>> ValueBuilder<E> enumValue(String key, E defaultValue);

    /**
     * Adds an RGB color value encoded as an integer.
     *
     * @param key the config key
     * @param defaultValue the default RGB value
     * @return a value builder for the new entry
     */
    ValueBuilder<Integer> colorRgb(String key, int defaultValue);

    /**
     * Adds an ARGB color value encoded as an integer.
     *
     * @param key the config key
     * @param defaultValue the default ARGB value
     * @return a value builder for the new entry
     */
    ValueBuilder<Integer> colorArgb(String key, int defaultValue);

    /**
     * Adds a custom encoded value.
     *
     * @param key the config key
     * @param defaultValue the default value
     * @param codec the codec used to read and write the value
     * @param <T> the value type
     * @return a value builder for the new entry
     */
    <T> ValueBuilder<T> custom(String key, T defaultValue, KonfigCodec<T> codec);

//? if >=1.21.11 {
    /**
     * Adds an editable flat collection of structured entries.
     *
     * @param key the config key
     * @param defaultValue the schema and builtin entries for the fieldset
     * @return a value builder for the new entry
     */
    @org.jetbrains.annotations.ApiStatus.Experimental
    ValueBuilder<FieldsetValue> fieldset(String key, FieldsetValue defaultValue);
//?}

    /**
     * Finalizes this builder and registers the config handle.
     *
     * @return the registered config handle
     */
    ConfigHandle build();
}
