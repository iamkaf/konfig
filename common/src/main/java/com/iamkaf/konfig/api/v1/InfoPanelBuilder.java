package com.iamkaf.konfig.api.v1;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} elif >=1.17 {
import net.minecraft.resources.ResourceLocation;
//?}

/**
 * Builds rich information-panel content for config screens, categories, values, and dropdown options.
 */
public interface InfoPanelBuilder {
    /**
     * Adds a literal header.
     *
     * @param text the header text
     * @return this builder
     */
    InfoPanelBuilder header(String text);

    /**
     * Adds a translated header.
     *
     * @param translationKey the header translation key
     * @return this builder
     */
    InfoPanelBuilder headerKey(String translationKey);

//? if >=1.21.11 {
    /**
     * Adds an image.
     *
     * @param textureId the texture identifier
     * @return this builder
     */
    InfoPanelBuilder image(Identifier textureId);

    /**
     * Adds an image.
     *
     * @param textureId the texture identifier
     * @param options image layout options
     * @return this builder
     */
    InfoPanelBuilder image(Identifier textureId, ImageOptions options);

    /**
     * Adds an image with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @return this builder
     */
    InfoPanelBuilder image(Identifier textureId, String caption);

    /**
     * Adds an image with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @param options image layout options
     * @return this builder
     */
    InfoPanelBuilder image(Identifier textureId, String caption, ImageOptions options);
//?} elif >=1.17 {
    /**
     * Adds an image.
     *
     * @param textureId the texture identifier
     * @return this builder
     */
    InfoPanelBuilder image(ResourceLocation textureId);

    /**
     * Adds an image.
     *
     * @param textureId the texture identifier
     * @param options image layout options
     * @return this builder
     */
    InfoPanelBuilder image(ResourceLocation textureId, ImageOptions options);

    /**
     * Adds an image with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @return this builder
     */
    InfoPanelBuilder image(ResourceLocation textureId, String caption);

    /**
     * Adds an image with a caption.
     *
     * @param textureId the texture identifier
     * @param caption the visible caption
     * @param options image layout options
     * @return this builder
     */
    InfoPanelBuilder image(ResourceLocation textureId, String caption, ImageOptions options);
//?} else {
    /**
     * Adds an image.
     *
     * @param textureId the loader/version texture identifier
     * @return this builder
     */
    InfoPanelBuilder image(Object textureId);

    /**
     * Adds an image.
     *
     * @param textureId the loader/version texture identifier
     * @param options image layout options
     * @return this builder
     */
    InfoPanelBuilder image(Object textureId, ImageOptions options);

    /**
     * Adds an image with a caption.
     *
     * @param textureId the loader/version texture identifier
     * @param caption the visible caption
     * @return this builder
     */
    InfoPanelBuilder image(Object textureId, String caption);

    /**
     * Adds an image with a caption.
     *
     * @param textureId the loader/version texture identifier
     * @param caption the visible caption
     * @param options image layout options
     * @return this builder
     */
    InfoPanelBuilder image(Object textureId, String caption, ImageOptions options);
//?}

    /**
     * Adds literal body text.
     *
     * @param text the body text
     * @return this builder
     */
    InfoPanelBuilder inlineText(String text);

    /**
     * Adds translated body text.
     *
     * @param translationKey the body text translation key
     * @return this builder
     */
    InfoPanelBuilder inlineTextKey(String translationKey);

    /**
     * Adds a URL.
     *
     * @param label the visible label
     * @param url the URL to open
     * @return this builder
     */
    InfoPanelBuilder url(String label, String url);

    /**
     * Adds a URL with a translated label.
     *
     * @param labelTranslationKey the label translation key
     * @param url the URL to open
     * @return this builder
     */
    InfoPanelBuilder urlKey(String labelTranslationKey, String url);
}
