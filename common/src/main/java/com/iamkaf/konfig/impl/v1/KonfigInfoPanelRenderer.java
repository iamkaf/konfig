//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ImageOptions;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

@ApiStatus.Internal
final class KonfigInfoPanelRenderer {
    private static final int MIN_WIDTH = 170;
    private static final int MAX_WIDTH = 310;
    private static final int PADDING = 16;
    private static final int GAP = 10;
    private static final int SCROLLBAR_WIDTH = 4;

    int widthFor(int screenWidth) {
        if (screenWidth < 520) {
            return Math.max(120, screenWidth / 3);
        }
        return Mth.clamp(screenWidth / 3, MIN_WIDTH, MAX_WIDTH);
    }

    void render(
            KonfigRenderContext context,
            Font font,
            KonfigInfoPanelState state,
            KonfigInfoPanelBounds bounds,
            KonfigInfoPanelState.DropdownSelectionInfo dropdownSelectionInfo,
            int mouseX,
            int mouseY
    ) {
        context.fill(bounds.left, bounds.top, bounds.right, bounds.bottom, 0x22000000);

        List<InfoPanelItem> items = state.activeItems(dropdownSelectionInfo);
        if (items.isEmpty()) {
            state.clearContent();
            return;
        }

        int x = bounds.left + PADDING;
        int viewportTop = bounds.top + PADDING;
        int viewportBottom = bounds.bottom - PADDING;
        int contentWidth = Math.max(20, bounds.right - bounds.left - (PADDING * 2) - SCROLLBAR_WIDTH - 4);
        int contentHeight = this.measureItems(font, items, contentWidth);
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        state.updateContent(items, contentHeight, viewportHeight);

        int y = viewportTop - (int) Math.round(state.scroll());
        context.renderScissored(
                bounds.left,
                viewportTop,
                bounds.right,
                viewportBottom,
                layer -> this.renderItems(layer, font, state, items, x, y, contentWidth, mouseX, mouseY)
        );

        this.renderScrollbar(context, state, bounds.right, viewportTop, viewportBottom);
    }

    private void renderItems(
            KonfigRenderContext context,
            Font font,
            KonfigInfoPanelState state,
            List<InfoPanelItem> items,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY
    ) {
        int itemY = y;
        for (InfoPanelItem item : items) {
            itemY = this.renderItem(context, font, state, item, x, itemY, width, mouseX, mouseY);
        }
    }

    private int renderItem(
            KonfigRenderContext context,
            Font font,
            KonfigInfoPanelState state,
            InfoPanelItem item,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY
    ) {
        if (item.kind == EntryKind.HEADER) {
            context.drawText(font, infoLabel(item), x, y, 0xFFFFFFFF);
            return y + 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.renderImage(context, font, item, x, y, width);
        }
        if (item.kind == EntryKind.URL) {
            Component label = KonfigScreenSupport.text(infoText(item) + " >");
            int linkWidth = font.width(label);
            state.addLink(x, y, Math.min(width, linkWidth), font.lineHeight, item.target);
            boolean hovered = mouseX >= x
                    && mouseX <= x + Math.min(width, linkWidth)
                    && mouseY >= y
                    && mouseY <= y + font.lineHeight;
            context.drawText(font, label, x, y, hovered ? 0xFFFFFFFF : 0xFF80C8FF);
            if (hovered) {
                context.fill(x, y + font.lineHeight, x + Math.min(width, linkWidth), y + font.lineHeight + 1, 0xFFFFFFFF);
            }
            return y + 16;
        }
        return this.renderParagraph(context, font, infoText(item), x, y, width, 0xFFCFCFCF) + GAP;
    }

    private int renderImage(KonfigRenderContext context, Font font, InfoPanelItem item, int x, int y, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(options.width(), width - (options.padding() * 2)));
        int imageHeight = Math.max(1, (int) Math.round(options.height() * ((double) imageWidth / (double) options.width())));
        int imageX = x + options.padding();
        if (options.align() == ImageOptions.Align.CENTER) {
            imageX = x + Math.max(options.padding(), (width - imageWidth) / 2);
        } else if (options.align() == ImageOptions.Align.RIGHT) {
            imageX = x + Math.max(options.padding(), width - options.padding() - imageWidth);
        }
        context.drawImage(item.target, imageX, y + options.padding(), imageWidth, imageHeight, options.width(), options.height());
        y += imageHeight + (options.padding() * 2);
        if (!KonfigScreenSupport.isBlank(infoText(item)) && options.captionPosition() != ImageOptions.CaptionPosition.NONE) {
            y = this.renderParagraph(context, font, infoText(item), x, y, width, 0xFFCFCFCF);
        }
        return y + GAP;
    }

    private void renderScrollbar(KonfigRenderContext context, KonfigInfoPanelState state, int right, int top, int bottom) {
        if (state.maxScroll() <= 0) {
            return;
        }

        int trackLeft = right - SCROLLBAR_WIDTH - 4;
        int trackRight = right - 4;
        int viewportHeight = Math.max(1, bottom - top);
        int contentHeight = viewportHeight + state.maxScroll();
        int thumbHeight = Mth.clamp((viewportHeight * viewportHeight) / contentHeight, 18, viewportHeight);
        int thumbTop = top + (int) Math.round((viewportHeight - thumbHeight) * (state.scroll() / (double) state.maxScroll()));
        context.fill(trackLeft, top, trackRight, bottom, 0x44000000);
        context.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0xAAFFFFFF);
    }

    private int renderParagraph(KonfigRenderContext context, Font font, String value, int x, int y, int width, int color) {
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                y += 8;
                continue;
            }
            y = this.renderWrappedLines(context, font, paragraph.trim(), x, y, width, color) + 4;
        }
        return y;
    }

    private int renderWrappedLines(KonfigRenderContext context, Font font, String value, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(KonfigScreenSupport.text(value), Math.max(1, width));
        for (FormattedCharSequence line : lines) {
            context.drawText(font, line, x, y, color);
            y += font.lineHeight;
        }
        return y;
    }

    private int measureItems(Font font, List<InfoPanelItem> items, int width) {
        int height = 0;
        for (InfoPanelItem item : items) {
            height += this.measureItem(font, item, width);
        }
        return height;
    }

    private int measureItem(Font font, InfoPanelItem item, int width) {
        if (item.kind == EntryKind.HEADER || item.kind == EntryKind.URL) {
            return 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.measureImage(font, item, width);
        }
        return this.measureParagraph(font, infoText(item), width) + GAP;
    }

    private int measureImage(Font font, InfoPanelItem item, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(options.width(), width - (options.padding() * 2)));
        int imageHeight = Math.max(1, (int) Math.round(options.height() * ((double) imageWidth / (double) options.width())));
        int height = imageHeight + (options.padding() * 2);
        if (!KonfigScreenSupport.isBlank(infoText(item)) && options.captionPosition() != ImageOptions.CaptionPosition.NONE) {
            height += this.measureParagraph(font, infoText(item), width);
        }
        return height + GAP;
    }

    private int measureParagraph(Font font, String value, int width) {
        int height = 0;
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                height += 8;
                continue;
            }
            height += font.split(KonfigScreenSupport.text(paragraph.trim()), Math.max(1, width)).size() * font.lineHeight;
            height += 4;
        }
        return height;
    }

    private static Component infoLabel(InfoPanelItem item) {
        return item.labelTranslationKey ? KonfigScreenSupport.translate(item.label) : KonfigScreenSupport.text(item.label);
    }

    private static String infoText(InfoPanelItem item) {
        return infoLabel(item).getString();
    }
}
//?}
