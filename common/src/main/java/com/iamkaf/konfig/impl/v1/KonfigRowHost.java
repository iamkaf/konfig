//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

final class KonfigRowHost {
    private final KonfigConfigScreen screen;

    KonfigRowHost(KonfigConfigScreen screen) {
        this.screen = screen;
    }

    Screen screen() {
        return this.screen;
    }

    Font font() {
        return this.screen.rowFont();
    }

    int rowHeight() {
        return KonfigConfigScreen.ROW_HEIGHT;
    }

    int controlHeight() {
        return KonfigConfigScreen.CONTROL_HEIGHT;
    }

    int controlMinWidth() {
        return KonfigConfigScreen.CONTROL_MIN_WIDTH;
    }

    int controlMaxWidth() {
        return KonfigConfigScreen.CONTROL_MAX_WIDTH;
    }

    int validationColor() {
        return KonfigConfigScreen.VALIDATION_COLOR;
    }

    int currentColor(ConfigValueImpl<?> value) {
        return this.screen.currentColor(value);
    }

    void updateHoveredEntry(EntryRef entry, boolean hovered) {
        this.screen.updateHoveredEntry(entry, hovered);
    }

    void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.screen.setDraft(value, draft);
    }

    boolean persistEntry(EntryRef entry) {
        return this.screen.persistEntry(entry);
    }
}
//?}
