package com.iamkaf.konfig.impl.v1;

import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.InfoPanelBuilder;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} elif >=1.17 {
import net.minecraft.resources.ResourceLocation;
//?}

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class InfoPanelBuilderImpl implements InfoPanelBuilder {
    private final List<InfoPanelItem> items = new ArrayList<InfoPanelItem>();

    @Override
    public InfoPanelBuilder header(String text) {
        this.add(EntryKind.HEADER, requireText(text, "text"), null, null);
        return this;
    }

    @Override
    public InfoPanelBuilder headerKey(String translationKey) {
        this.add(EntryKind.HEADER, requireText(translationKey, "translationKey"), null, null, true);
        return this;
    }

//? if >=1.21.11 {
    @Override
    public InfoPanelBuilder image(Identifier textureId) {
//?} elif >=1.17 {
    @Override
    public InfoPanelBuilder image(ResourceLocation textureId) {
//?} else {
    @Override
    public InfoPanelBuilder image(Object textureId) {
//?}
        return image(textureId, "", ImageOptions.defaults());
    }

//? if >=1.21.11 {
    @Override
    public InfoPanelBuilder image(Identifier textureId, ImageOptions options) {
//?} elif >=1.17 {
    @Override
    public InfoPanelBuilder image(ResourceLocation textureId, ImageOptions options) {
//?} else {
    @Override
    public InfoPanelBuilder image(Object textureId, ImageOptions options) {
//?}
        return image(textureId, "", options);
    }

//? if >=1.21.11 {
    @Override
    public InfoPanelBuilder image(Identifier textureId, String caption) {
//?} elif >=1.17 {
    @Override
    public InfoPanelBuilder image(ResourceLocation textureId, String caption) {
//?} else {
    @Override
    public InfoPanelBuilder image(Object textureId, String caption) {
//?}
        return image(textureId, caption, ImageOptions.defaults());
    }

//? if >=1.21.11 {
    @Override
    public InfoPanelBuilder image(Identifier textureId, String caption, ImageOptions options) {
//?} elif >=1.17 {
    @Override
    public InfoPanelBuilder image(ResourceLocation textureId, String caption, ImageOptions options) {
//?} else {
    @Override
    public InfoPanelBuilder image(Object textureId, String caption, ImageOptions options) {
//?}
        Objects.requireNonNull(textureId, "textureId");
        this.add(EntryKind.IMAGE, caption == null ? "" : caption.trim(), textureId.toString(), options);
        return this;
    }

    @Override
    public InfoPanelBuilder inlineText(String text) {
        this.add(EntryKind.INLINE_TEXT, requireText(text, "text"), null, null);
        return this;
    }

    @Override
    public InfoPanelBuilder inlineTextKey(String translationKey) {
        this.add(EntryKind.INLINE_TEXT, requireText(translationKey, "translationKey"), null, null, true);
        return this;
    }

    @Override
    public InfoPanelBuilder url(String label, String url) {
        this.add(EntryKind.URL, requireText(label, "label"), requireText(url, "url"), null);
        return this;
    }

    @Override
    public InfoPanelBuilder urlKey(String labelTranslationKey, String url) {
        this.add(EntryKind.URL, requireText(labelTranslationKey, "labelTranslationKey"), requireText(url, "url"), null, true);
        return this;
    }

    List<InfoPanelItem> build() {
        return Collections.unmodifiableList(new ArrayList<InfoPanelItem>(this.items));
    }

    private void add(EntryKind kind, String label, String target, ImageOptions imageOptions) {
        this.add(kind, label, target, imageOptions, false);
    }

    private void add(EntryKind kind, String label, String target, ImageOptions imageOptions, boolean labelTranslationKey) {
        this.items.add(new InfoPanelItem(kind, label, target, imageOptions, labelTranslationKey));
    }

    private static String requireText(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
