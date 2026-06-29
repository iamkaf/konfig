//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.toast;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
//?} elif >=1.21.11 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
//?} elif >=1.21.6 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
//?} elif >=1.21.2 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
//?} elif >=1.21 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.resources.ResourceLocation;
//?} elif >=1.20.2 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.resources.ResourceLocation;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
//?} else {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

@ApiStatus.Internal
final class KonfigToastRenderer {
    private static final int TITLE_COLOR = 0xFFFFFF00;
    private static final int MESSAGE_COLOR = 0xFFFFFFFF;

    private KonfigToastRenderer() {
    }

//? if >=1.21.2 {
//? if >=26.1 {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/system");

    static void render(KonfigToastContent content, int width, int height, GuiGraphicsExtractor graphics, Font font) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width, height);
        renderDynamicText(content, KonfigRenderContext.of(graphics), font);
    }
//?} elif >=1.21.11 {
/*    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/system");

    static void render(KonfigToastContent content, int width, int height, GuiGraphics graphics, Font font) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width, height);
        renderDynamicText(content, KonfigRenderContext.of(graphics), font);
    }*/
//?} elif >=1.21.6 {
/*    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");

    static void render(KonfigToastContent content, int width, int height, GuiGraphics graphics, Font font) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width, height);
        renderDynamicText(content, KonfigRenderContext.of(graphics), font);
    }*/
//?} else {
/*    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");

    static void render(KonfigToastContent content, int width, int height, GuiGraphics graphics, Font font) {
        graphics.blitSprite(RenderType::guiTextured, BACKGROUND_SPRITE, 0, 0, width, height);
        renderDynamicText(content, KonfigRenderContext.of(graphics), font);
    }*/
//?}

    private static void renderDynamicText(KonfigToastContent content, KonfigRenderContext context, Font font) {
        int y = content.messageLines().isEmpty() ? 12 : 7;
        for (FormattedCharSequence line : content.titleLines()) {
            context.drawText(font, line, 18, y, TITLE_COLOR, false);
            y += 12;
        }
        for (FormattedCharSequence line : content.messageLines()) {
            context.drawText(font, line, 18, y, MESSAGE_COLOR, false);
            y += 12;
        }
    }
//?} elif >=1.20 {
//? if >=1.21 {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");
//?} elif >=1.20.2 {
    private static final ResourceLocation BACKGROUND_SPRITE = new ResourceLocation("toast/system");
//?}

    static void render(KonfigToastContent content, int width, int height, GuiGraphics graphics, ToastComponent toastComponent) {
//? if >=1.20.2 {
        graphics.blitSprite(BACKGROUND_SPRITE, 0, 0, width, height);
//?} else {
        graphics.blit(Toast.TEXTURE, 0, 0, 0, 64, width, height);
//?}
        Font font = toastComponent.getMinecraft().font;
        renderLegacyText(content, font, KonfigRenderContext.of(graphics));
    }
//?} else {
    static void render(KonfigToastContent content, int width, int height, PoseStack graphics, ToastComponent toastComponent) {
        RenderSystem.setShaderTexture(0, Toast.TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        toastComponent.blit(graphics, 0, 0, 0, 64, width, height);
        Font font = toastComponent.getMinecraft().font;
        renderLegacyText(content, font, KonfigRenderContext.of(graphics));
    }
//?}

//? if <1.21.2 {
    private static void renderLegacyText(KonfigToastContent content, Font font, KonfigRenderContext context) {
        List<FormattedCharSequence> detailLines = content.legacyMessagePreview(font);
        if (detailLines.isEmpty()) {
            context.drawText(font, content.title(), 18, 12, TITLE_COLOR, false);
        } else {
            context.drawText(font, content.title(), 18, 7, TITLE_COLOR, false);
            context.drawText(font, detailLines.get(0), 18, 18, MESSAGE_COLOR, false);
        }
    }
//?}
}
//?}
