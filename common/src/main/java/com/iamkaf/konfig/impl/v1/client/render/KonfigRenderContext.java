//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.render;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;

@ApiStatus.Internal
public final class KonfigRenderContext {
//? if >=26.1 {
    private final GuiGraphicsExtractor graphics;

    private KonfigRenderContext(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public static KonfigRenderContext of(GuiGraphicsExtractor graphics) {
        return new KonfigRenderContext(graphics);
    }
//?} elif >=1.20 {
    private final GuiGraphics graphics;

    private KonfigRenderContext(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    public static KonfigRenderContext of(GuiGraphics graphics) {
        return new KonfigRenderContext(graphics);
    }
//?} else {
    private final PoseStack graphics;

    private KonfigRenderContext(PoseStack graphics) {
        this.graphics = graphics;
    }

    public static KonfigRenderContext of(PoseStack graphics) {
        return new KonfigRenderContext(graphics);
    }
//?}

    public void fill(int x1, int y1, int x2, int y2, int color) {
        KonfigUiAdapter.fillRect(this.graphics, x1, y1, x2, y2, color);
    }

    public void drawText(Font font, Component text, int x, int y, int color) {
        KonfigUiAdapter.drawText(this.graphics, font, text, x, y, color);
    }

    public void drawText(Font font, Component text, int x, int y, int color, boolean shadow) {
//? if >=26.1 {
        this.graphics.text(font, text, x, y, color, shadow);
//?} elif >=1.20 {
        this.graphics.drawString(font, text, x, y, color, shadow);
//?} else {
        if (shadow) {
            font.drawShadow(this.graphics, text, (float) x, (float) y, color);
        } else {
            font.draw(this.graphics, text, (float) x, (float) y, color);
        }
//?}
    }

    public void drawText(Font font, FormattedCharSequence text, int x, int y, int color) {
//? if >=26.1 {
        this.graphics.text(font, text, x, y, color);
//?} elif >=1.20 {
        this.graphics.drawString(font, text, x, y, color);
//?} else {
        font.draw(this.graphics, text, (float) x, (float) y, color);
//?}
    }

    public void drawText(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow) {
//? if >=26.1 {
        this.graphics.text(font, text, x, y, color, shadow);
//?} elif >=1.20 {
        this.graphics.drawString(font, text, x, y, color, shadow);
//?} else {
        if (shadow) {
            font.drawShadow(this.graphics, text, (float) x, (float) y, color);
        } else {
            font.draw(this.graphics, text, (float) x, (float) y, color);
        }
//?}
    }

    public void drawCenteredText(Font font, Component text, int x, int y, int color) {
        KonfigUiAdapter.drawCenteredText(this.graphics, font, text, x, y, color);
    }

    public void drawImage(String target, int x, int y, int width, int height) {
        KonfigUiAdapter.drawImage(this.graphics, target, x, y, width, height);
    }

    public void drawImage(String target, int x, int y, int width, int height, int sourceWidth, int sourceHeight) {
        KonfigUiAdapter.drawImage(this.graphics, target, x, y, width, height, sourceWidth, sourceHeight);
    }

    public void drawColorSwatch(int x, int y, int size, int color, EntryKind kind) {
        KonfigUiAdapter.drawColorSwatch(this.graphics, x, y, size, color, kind);
    }

    public void renderWidget(AbstractWidget widget, int mouseX, int mouseY, float partialTick) {
        KonfigUiAdapter.renderWidget(widget, this.graphics, mouseX, mouseY, partialTick);
    }

    public void showTooltip(Screen screen, Font font, String tooltip, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        KonfigUiAdapter.showTooltip(screen, font, this.graphics, tooltip, mouseX, mouseY, left, top, right, bottom);
    }

    public void renderTooltipNow(Screen screen, Font font, String tooltip, int mouseX, int mouseY) {
        KonfigUiAdapter.renderTooltipNow(screen, font, this.graphics, tooltip, mouseX, mouseY);
    }

    public void renderRegistryIcon(ResourceKey<? extends Registry<?>> registryKey, String value, int x, int y) {
        KonfigRegistryAdapter.renderRegistryIcon(this.graphics, registryKey, value, x, y);
    }

    public void renderFloatingLayers(RenderLayer floatingLayer, RenderLayer tooltipLayer) {
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

    public void renderScissored(int left, int top, int right, int bottom, RenderLayer layer) {
//? if >=1.20 {
        this.graphics.enableScissor(left, top, right, bottom);
        try {
            layer.render(this);
        } finally {
            this.graphics.disableScissor();
        }
//?} else {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int scissorX = (int) Math.round(left * scale);
        int scissorY = (int) Math.round(Minecraft.getInstance().getWindow().getHeight() - (bottom * scale));
        int scissorWidth = Math.max(0, (int) Math.round((right - left) * scale));
        int scissorHeight = Math.max(0, (int) Math.round((bottom - top) * scale));
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        try {
            layer.render(this);
        } finally {
            RenderSystem.disableScissor();
        }
//?}
    }

    public interface RenderLayer {
        void render(KonfigRenderContext context);
    }
}
//?}
