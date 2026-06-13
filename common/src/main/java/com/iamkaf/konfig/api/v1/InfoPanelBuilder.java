package com.iamkaf.konfig.api.v1;

//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} elif >=1.17 {
import net.minecraft.resources.ResourceLocation;
//?}

public interface InfoPanelBuilder {
    InfoPanelBuilder header(String text);

    InfoPanelBuilder headerKey(String translationKey);

//? if >=1.21.11 {
    InfoPanelBuilder image(Identifier textureId);

    InfoPanelBuilder image(Identifier textureId, ImageOptions options);

    InfoPanelBuilder image(Identifier textureId, String caption);

    InfoPanelBuilder image(Identifier textureId, String caption, ImageOptions options);
//?} elif >=1.17 {
    InfoPanelBuilder image(ResourceLocation textureId);

    InfoPanelBuilder image(ResourceLocation textureId, ImageOptions options);

    InfoPanelBuilder image(ResourceLocation textureId, String caption);

    InfoPanelBuilder image(ResourceLocation textureId, String caption, ImageOptions options);
//?} else {
    InfoPanelBuilder image(Object textureId);

    InfoPanelBuilder image(Object textureId, ImageOptions options);

    InfoPanelBuilder image(Object textureId, String caption);

    InfoPanelBuilder image(Object textureId, String caption, ImageOptions options);
//?}

    InfoPanelBuilder inlineText(String text);

    InfoPanelBuilder inlineTextKey(String translationKey);

    InfoPanelBuilder url(String label, String url);

    InfoPanelBuilder urlKey(String labelTranslationKey, String url);
}
