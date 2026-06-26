//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

final class KonfigToastContent {
    private Component title;
    private Component message;
    private List<FormattedCharSequence> titleLines = Collections.emptyList();
    private List<FormattedCharSequence> messageLines = Collections.emptyList();
    private int contentWidth;

    void reset(Component title, Component message) {
        this.title = title;
        this.message = message;
        this.titleLines = split(title, 200);
        this.messageLines = split(message, 200);
        this.contentWidth = Stream.concat(this.titleLines.stream(), this.messageLines.stream())
                .mapToInt(Minecraft.getInstance().font::width)
                .max()
                .orElse(130);
    }

    Component title() {
        return this.title;
    }

    Component message() {
        return this.message;
    }

    List<FormattedCharSequence> titleLines() {
        return this.titleLines;
    }

    List<FormattedCharSequence> messageLines() {
        return this.messageLines;
    }

    int width(int minimumWidth) {
        return Math.max(minimumWidth, this.contentWidth + 30);
    }

    int dynamicHeight() {
        int titleHeight = Math.max(1, this.titleLines.size()) * 12;
        int messageHeight = this.messageLines.isEmpty() ? 0 : this.messageLines.size() * 12;
        return 16 + titleHeight + messageHeight;
    }

    List<FormattedCharSequence> legacyMessagePreview(Font font) {
        return this.message == null ? Collections.emptyList() : font.split(this.message, 124);
    }

    private static List<FormattedCharSequence> split(Component text, int maxWidth) {
        if (text == null) {
            return Collections.emptyList();
        }
        return Minecraft.getInstance().font.split(text, maxWidth);
    }
}
//?}
