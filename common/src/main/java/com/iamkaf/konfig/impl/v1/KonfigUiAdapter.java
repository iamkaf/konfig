//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class KonfigUiAdapter {
    private KonfigUiAdapter() {
    }

//? if >=26.1 {
    static void drawColorSwatch(GuiGraphicsExtractor guiGraphics, int x, int y, int size, int color, EntryKind kind) {
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF202020);
//?} elif >=1.20 {
    static void drawColorSwatch(GuiGraphics guiGraphics, int x, int y, int size, int color, EntryKind kind) {
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF202020);
//?} else {
    static void drawColorSwatch(PoseStack guiGraphics, int x, int y, int size, int color, EntryKind kind) {
        GuiComponent.fill(guiGraphics, x - 1, y - 1, x + size + 1, y + size + 1, 0xFF202020);
//?}
        if (kind == EntryKind.COLOR_ARGB && ColorValueHelper.alpha(color) < 255) {
            int cell = Math.max(2, size / 4);
            for (int row = 0; row < size; row += cell) {
                for (int column = 0; column < size; column += cell) {
                    boolean dark = ((row / cell) + (column / cell)) % 2 == 0;
//? if >=26.1 {
                    guiGraphics.fill(
//?} elif >=1.20 {
                    guiGraphics.fill(
//?} else {
                    GuiComponent.fill(guiGraphics,
//?}
                        x + column,
                        y + row,
                        x + Math.min(size, column + cell),
                        y + Math.min(size, row + cell),
                        dark ? 0xFF707070 : 0xFFC0C0C0
                    );
                }
            }
        } else {
//? if >=26.1 {
            guiGraphics.fill(x, y, x + size, y + size, 0xFFFFFFFF);
//?} elif >=1.20 {
            guiGraphics.fill(x, y, x + size, y + size, 0xFFFFFFFF);
//?} else {
            GuiComponent.fill(guiGraphics, x, y, x + size, y + size, 0xFFFFFFFF);
//?}
        }
//? if >=26.1 {
        guiGraphics.fill(x, y, x + size, y + size, ColorValueHelper.toRenderColor(kind, color));
//?} elif >=1.20 {
        guiGraphics.fill(x, y, x + size, y + size, ColorValueHelper.toRenderColor(kind, color));
//?} else {
        GuiComponent.fill(guiGraphics, x, y, x + size, y + size, ColorValueHelper.toRenderColor(kind, color));
//?}
    }

    static Button button(int x, int y, int width, int height, Component label, Button.OnPress onPress) {
//? if >=1.19.3 {
        return Button.builder(label, onPress).bounds(x, y, width, height).build();
//?} else {
        return new Button(x, y, width, height, label, onPress);
//?}
    }

    static List<Component> tooltipLines(String tooltip) {
        List<Component> lines = new ArrayList<Component>();
        String normalized = tooltip.replace('\r', '\n');
        for (String line : normalized.split("\\n")) {
            lines.add(KonfigScreenSupport.text(line));
        }
        return lines;
    }

//? if >=26.1 {
    static void fillRect(GuiGraphicsExtractor guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y2, color);
    }

    static void drawCenteredText(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int color) {
        guiGraphics.centeredText(font, text, x, y, color);
    }

    static void drawText(GuiGraphicsExtractor guiGraphics, Font font, Component text, int x, int y, int color) {
        guiGraphics.text(font, text, x, y, color);
    }

    static void renderWidget(AbstractWidget widget, GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    static void showTooltip(Screen screen, Font font, GuiGraphicsExtractor guiGraphics, String tooltip, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        if (!KonfigScreenSupport.isBlank(tooltip) && mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
            guiGraphics.setComponentTooltipForNextFrame(font, tooltipLines(tooltip), mouseX, mouseY);
        }
    }
//?} elif >=1.20 {
    static void fillRect(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y2, color);
    }

    static void drawCenteredText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color) {
        guiGraphics.drawCenteredString(font, text, x, y, color);
    }

    static void drawText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color);
    }

    static void renderWidget(AbstractWidget widget, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        widget.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    static void showTooltip(Screen screen, Font font, GuiGraphics guiGraphics, String tooltip, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        if (!KonfigScreenSupport.isBlank(tooltip) && mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
//? if >=1.21.6 {
            guiGraphics.setTooltipForNextFrame(font, font.split(KonfigScreenSupport.text(tooltip), Math.max(screen.width / 2, 200)), mouseX, mouseY);
//?} else {
            guiGraphics.renderComponentTooltip(font, tooltipLines(tooltip), mouseX, mouseY);
//?}
        }
    }
//?} else {
    static void fillRect(PoseStack guiGraphics, int x1, int y1, int x2, int y2, int color) {
        GuiComponent.fill(guiGraphics, x1, y1, x2, y2, color);
    }

    static void drawCenteredText(PoseStack guiGraphics, Font font, Component text, int x, int y, int color) {
        Screen.drawCenteredString(guiGraphics, font, text, x, y, color);
    }

    static void drawText(PoseStack guiGraphics, Font font, Component text, int x, int y, int color) {
        font.draw(guiGraphics, text, (float) x, (float) y, color);
    }

    static void renderWidget(AbstractWidget widget, PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        widget.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    static void showTooltip(Screen screen, Font font, PoseStack guiGraphics, String tooltip, int mouseX, int mouseY, int left, int top, int right, int bottom) {
        if (KonfigScreenSupport.isBlank(tooltip) || mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) {
            return;
        }
//? if <=1.16.1 {
        screen.renderTooltip(guiGraphics, tooltipLines(tooltip), mouseX, mouseY);
//?} elif <=1.16.3 {
        screen.renderTooltip(guiGraphics, font.split(KonfigScreenSupport.text(tooltip), Math.max(screen.width / 2, 200)), mouseX, mouseY);
//?} else {
        screen.renderComponentTooltip(guiGraphics, tooltipLines(tooltip), mouseX, mouseY);
//?}
    }
//?}
}
//?}
