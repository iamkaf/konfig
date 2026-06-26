//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.*;
import static com.iamkaf.konfig.impl.v1.KonfigUiAdapter.button;

import com.iamkaf.konfig.api.v1.ImageOptions;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.util.FormattedCharSequence;

import java.util.Collections;
import java.util.List;

final class UnsupportedRow extends KonfigConfigRow {
    private final Button button;

    UnsupportedRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.controlMinWidth(), host.controlHeight(), translate("konfig.screen.unsupported"), ignored -> {
        });
        this.button.active = false;
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }
}

abstract class DecorationRow extends KonfigConfigRow {
    private final Button spacer;

    DecorationRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.spacer = button(0, 0, 0, 0, text(""), ignored -> {});
        this.spacer.visible = false;
        this.spacer.active = false;
    }

    @Override
    protected final AbstractWidget control() {
        return this.spacer;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return Collections.emptyList();
    }
}

final class HeaderRow extends DecorationRow {
    HeaderRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderHeaderRow(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderHeaderRow(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderHeaderRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, x, y, x + width, y + height);
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderHeaderRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, x, y, x + width, y + height);
    }
//?}

    private void renderHeaderRow(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        this.host.updateHoveredEntry(this.entry, hovered);
        context.showTooltip(this.host.screen(), this.host.font(), this.entry.tooltip, mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        context.fill(x, y + 4, x + width, y + height - 4, 0x552B3550);
        context.drawCenteredText(this.host.font(), this.entry.displayLabel(), x + (width / 2), y + 10, 0xFFF8E38F);
    }
}

final class ImageRow extends DecorationRow {
    ImageRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
    }

    private boolean hasCaption() {
        return !KonfigScreenSupport.isBlank(this.entry.value.inlineLabel()) && this.entry.value.imageOptions().captionPosition() != ImageOptions.CaptionPosition.NONE;
    }

    private int captionWidth() {
        return this.hasCaption() ? this.host.font().width(this.entry.displayLabel()) : 0;
    }

    private int[] imageSize(int rowWidth, int rowHeight) {
        ImageOptions options = this.entry.value.imageOptions();
        int captionReserve = this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.RIGHT ? this.captionWidth() + 8 : 0;
        int maxWidth = Math.max(1, rowWidth - (options.padding() * 2) - captionReserve);
        int maxHeight = Math.max(1, rowHeight - (options.padding() * 2) - (this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.BELOW ? 10 : 0));
        double scale = Math.min(1.0D, Math.min((double) maxWidth / (double) options.width(), (double) maxHeight / (double) options.height()));
        return new int[] {
                Math.max(1, (int) Math.round(options.width() * scale)),
                Math.max(1, (int) Math.round(options.height() * scale))
        };
    }

    private int contentWidth(int imageWidth) {
        ImageOptions options = this.entry.value.imageOptions();
        if (this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
            return imageWidth + 8 + this.captionWidth();
        }
        return imageWidth;
    }

    private int contentHeight(int imageHeight) {
        ImageOptions options = this.entry.value.imageOptions();
        if (this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
            return imageHeight + 12;
        }
        return imageHeight;
    }

    private int imageX(int x, int width, int contentWidth) {
        ImageOptions options = this.entry.value.imageOptions();
        if (options.align() == ImageOptions.Align.CENTER) {
            return x + Math.max(options.padding(), (width - contentWidth) / 2);
        }
        if (options.align() == ImageOptions.Align.RIGHT) {
            return x + Math.max(options.padding(), width - options.padding() - contentWidth);
        }
        return x + options.padding();
    }

    private int imageY(int y, int height, int contentHeight) {
        return y + Math.max(0, (height - contentHeight) / 2);
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderImageRow(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderImageRow(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderImageRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, x, y, x + width, y + height);
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderImageRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, x, y, x + width, y + height);
    }
//?}

    private void renderImageRow(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        this.host.updateHoveredEntry(this.entry, hovered);
        if (hovered) {
            context.fill(x, y, x + width, y + height, 0x16000000);
        }
        context.showTooltip(this.host.screen(), this.host.font(), this.entry.tooltip, mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        int[] imageSize = imageSize(width, height);
        int contentWidth = contentWidth(imageSize[0]);
        int contentHeight = contentHeight(imageSize[1]);
        int imageX = imageX(x, width, contentWidth);
        int imageY = imageY(y, height, contentHeight);
        context.drawImage(this.entry.value.inlineTarget(), imageX, imageY, imageSize[0], imageSize[1], this.entry.value.imageOptions().width(), this.entry.value.imageOptions().height());
        if (this.hasCaption()) {
            ImageOptions options = this.entry.value.imageOptions();
            if (options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                context.drawText(this.host.font(), this.entry.displayLabel(), imageX + imageSize[0] + 8, imageY + Math.max(0, (imageSize[1] - 8) / 2), 0xFFCFCFCF);
            } else if (options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                context.drawText(this.host.font(), this.entry.displayLabel(), imageX + Math.max(0, (imageSize[0] - this.captionWidth()) / 2), imageY + imageSize[1] + 2, 0xFFCFCFCF);
            }
        }
    }
}

final class InlineTextRow extends DecorationRow {
    InlineTextRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
    }

    @Override
    protected int preferredHeight(int rowWidth) {
        int textWidth = Math.max(1, rowWidth - 20);
        int lineCount = this.host.font().split(this.entry.displayLabel(), textWidth).size();
        return Math.max(this.host.rowHeight(), (lineCount * this.host.font().lineHeight) + 16);
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderInlineTextRow(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderInlineTextRow(KonfigRenderContext.of(guiGraphics), this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight(), mouseX, mouseY, hovered, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderInlineTextRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, x, y, x + width, y + height);
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderInlineTextRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, x, y, x + width, y + height);
    }
//?}

    private void renderInlineTextRow(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        this.host.updateHoveredEntry(this.entry, hovered);
        if (hovered) {
            context.fill(x, y, x + width, y + height, 0x16000000);
        }
        context.showTooltip(this.host.screen(), this.host.font(), this.entry.tooltip, mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        List<FormattedCharSequence> lines = this.host.font().split(this.entry.displayLabel(), Math.max(1, width - 16));
        int textY = y + Math.max(4, (height - (lines.size() * this.host.font().lineHeight)) / 2);
        for (FormattedCharSequence line : lines) {
            context.drawText(this.host.font(), line, x + 8, textY, 0xFFCFCFCF);
            textY += this.host.font().lineHeight;
        }
    }
}

final class UrlRow extends KonfigConfigRow {
    private final Button button;

    UrlRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.button = button(0, 0, host.urlButtonWidth(), host.controlHeight(), text("Open"), ignored -> this.host.openInlineUrl(this.entry));
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
        this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF80C8FF);
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
        this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF80C8FF);
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(x, y, width, height);
        this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, 0xFF80C8FF);
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        KonfigRowLayout layout = this.rowLayout(x, y, width, height);
        this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, 0xFF80C8FF);
    }
//?}
}
//?}
