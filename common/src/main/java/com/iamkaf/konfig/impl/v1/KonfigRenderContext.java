//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;

final class KonfigRenderContext {
//? if >=26.1 {
    private final GuiGraphicsExtractor graphics;

    private KonfigRenderContext(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    static KonfigRenderContext of(GuiGraphicsExtractor graphics) {
        return new KonfigRenderContext(graphics);
    }
//?} elif >=1.20 {
    private final GuiGraphics graphics;

    private KonfigRenderContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    static KonfigRenderContext of(GuiGraphics graphics) {
        return new KonfigRenderContext(graphics);
    }
//?} else {
    private final PoseStack graphics;

    private KonfigRenderContext(PoseStack graphics) {
        this.graphics = graphics;
    }

    static KonfigRenderContext of(PoseStack graphics) {
        return new KonfigRenderContext(graphics);
    }
//?}

    void fill(int x1, int y1, int x2, int y2, int color) {
        KonfigUiAdapter.fillRect(this.graphics, x1, y1, x2, y2, color);
    }

    void drawText(Font font, Component text, int x, int y, int color) {
        KonfigUiAdapter.drawText(this.graphics, font, text, x, y, color);
    }

    void drawText(Font font, FormattedCharSequence text, int x, int y, int color) {
//? if >=26.1 {
        this.graphics.text(font, text, x, y, color);
//?} elif >=1.20 {
        this.graphics.drawString(font, text, x, y, color);
//?} else {
        font.draw(this.graphics, text, (float) x, (float) y, color);
//?}
    }

    void drawCenteredText(Font font, Component text, int x, int y, int color) {
        KonfigUiAdapter.drawCenteredText(this.graphics, font, text, x, y, color);
    }

    void drawImage(String target, int x, int y, int width, int height) {
        KonfigUiAdapter.drawImage(this.graphics, target, x, y, width, height);
    }

    void drawImage(String target, int x, int y, int width, int height, int sourceWidth, int sourceHeight) {
        KonfigUiAdapter.drawImage(this.graphics, target, x, y, width, height, sourceWidth, sourceHeight);
    }

    void drawColorSwatch(int x, int y, int size, int color, EntryKind kind) {
        KonfigUiAdapter.drawColorSwatch(this.graphics, x, y, size, color, kind);
    }

    void renderWidget(AbstractWidget widget, int mouseX, int mouseY, float partialTick) {
        KonfigUiAdapter.renderWidget(widget, this.graphics, mouseX, mouseY, partialTick);
    }

    void showTooltip(Screen screen, Font font, String tooltip, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        KonfigUiAdapter.showTooltip(screen, font, this.graphics, tooltip, mouseX, mouseY, left, top, right, bottom);
    }

    void renderTooltipNow(Screen screen, Font font, String tooltip, int mouseX, int mouseY) {
        KonfigUiAdapter.renderTooltipNow(screen, font, this.graphics, tooltip, mouseX, mouseY);
    }

    void renderRegistryIcon(ResourceKey<? extends Registry<?>> registryKey, String value, int x, int y) {
        KonfigRegistryAdapter.renderRegistryIcon(this.graphics, registryKey, value, x, y);
    }

    void renderFloatingLayers(RenderLayer floatingLayer, RenderLayer tooltipLayer) {
//? if >=1.21.6 {
        this.graphics.nextStratum();
        floatingLayer.render(this);
        this.graphics.nextStratum();
        tooltipLayer.render(this);
//?} elif >=1.20 {
        this.graphics.pose().pushPose();
        this.graphics.pose().translate(0.0F, 0.0F, 300.0F);
        try {
            floatingLayer.render(this);
            this.graphics.pose().translate(0.0F, 0.0F, 100.0F);
            tooltipLayer.render(this);
        } finally {
            this.graphics.pose().popPose();
        }
//?} else {
        this.graphics.pushPose();
        this.graphics.translate(0.0D, 0.0D, 300.0D);
        try {
            floatingLayer.render(this);
            this.graphics.translate(0.0D, 0.0D, 100.0D);
            tooltipLayer.render(this);
        } finally {
            this.graphics.popPose();
        }
//?}
    }

    interface RenderLayer {
        void render(KonfigRenderContext context);
    }
}
//?}
