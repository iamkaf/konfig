//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.List;

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

    int urlButtonWidth() {
        return KonfigConfigScreen.URL_BUTTON_WIDTH;
    }

    int suggestionLimit() {
        return KonfigConfigScreen.SUGGESTION_LIMIT;
    }

    int suggestionRowHeight() {
        return KonfigConfigScreen.SUGGESTION_ROW_HEIGHT;
    }

    int dropdownChevronWidth() {
        return KonfigConfigScreen.DROPDOWN_CHEVRON_WIDTH;
    }

    long dropdownTypeSelectResetMs() {
        return KonfigConfigScreen.DROPDOWN_TYPE_SELECT_RESET_MS;
    }

    int listTop() {
        return KonfigConfigScreen.LIST_TOP;
    }

    int screenHeight() {
        return this.screen.screenHeight();
    }

    Object draft(ConfigValueImpl<?> value) {
        return this.screen.draft(value);
    }

    boolean readBoolean(ConfigValueImpl<?> value) {
        return this.screen.readBoolean(value);
    }

    Enum<?> currentEnum(ConfigValueImpl<?> value) {
        return this.screen.currentEnum(value);
    }

    Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        return this.screen.cycleEnum(value);
    }

    int currentColor(ConfigValueImpl<?> value) {
        return this.screen.currentColor(value);
    }

    List<String> currentStringList(ConfigValueImpl<?> value) {
        return this.screen.currentStringList(value);
    }

    String currentDropdownValue(ConfigValueImpl<?> value) {
        return this.screen.currentDropdownValue(value);
    }

    String currentStringValue(ConfigValueImpl<?> value) {
        return this.screen.currentStringValue(value);
    }

    int currentInt(ConfigValueImpl<?> value) {
        return this.screen.currentInt(value);
    }

    long currentLong(ConfigValueImpl<?> value) {
        return this.screen.currentLong(value);
    }

    double currentDouble(ConfigValueImpl<?> value) {
        return this.screen.currentDouble(value);
    }

    Component booleanText(ConfigValueImpl<?> value) {
        return this.screen.booleanText(value);
    }

    Component enumText(EntryRef entry, Enum<?> value) {
        return this.screen.enumText(entry, value);
    }

    Component colorText(ConfigValueImpl<?> value) {
        return this.screen.colorText(value);
    }

    Component stringListText(ConfigValueImpl<?> value) {
        return this.screen.stringListText(value);
    }

    Component dropdownText(EntryRef entry, String option) {
        return this.screen.dropdownText(entry, option);
    }

    List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        return this.screen.registrySuggestions(registryKey);
    }

    void queueTooltip(String tooltip, int mouseX, int mouseY) {
        this.screen.queueTooltip(tooltip, mouseX, mouseY);
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

    void openInlineUrl(EntryRef entry) {
        this.screen.openInlineUrl(entry);
    }

    void openColorEditor(EntryRef entry) {
        this.screen.openColorEditor(entry);
    }

    void openStringListEditor(EntryRef entry) {
        this.screen.openStringListEditor(entry);
    }

    void setActiveRegistryRow(RegistryTextInputRow row) {
        this.screen.setActiveRegistryRow(row);
    }

    boolean isActiveRegistryRow(RegistryTextInputRow row) {
        return this.screen.isActiveRegistryRow(row);
    }

    void clearActiveRegistryRow(RegistryTextInputRow row) {
        this.screen.clearActiveRegistryRow(row);
    }

    void markRenderedRegistryRow(RegistryTextInputRow row) {
        this.screen.markRenderedRegistryRow(row);
    }

    void setActiveDropdownRow(DropdownRow row) {
        this.screen.setActiveDropdownRow(row);
    }

    void clearActiveDropdownRow(DropdownRow row) {
        this.screen.clearActiveDropdownRow(row);
    }

    void clearRenderedDropdownRow(DropdownRow row) {
        this.screen.clearRenderedDropdownRow(row);
    }

    void markRenderedDropdownRow(DropdownRow row) {
        this.screen.markRenderedDropdownRow(row);
    }
}
//?}
