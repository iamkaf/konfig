//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
final class KonfigRowHost {
    private final KonfigConfigScreen screen;
    private final KonfigScreenCoordinator coordinator;

    KonfigRowHost(KonfigConfigScreen screen, KonfigScreenCoordinator coordinator) {
        this.screen = screen;
        this.coordinator = coordinator;
    }

    Screen screen() {
        return this.screen;
    }

    Font font() {
        return this.screen.rowFont();
    }

    int rowHeight() {
        return KonfigScreenMetrics.ROW_HEIGHT;
    }

    int controlHeight() {
        return KonfigScreenMetrics.CONTROL_HEIGHT;
    }

    int controlMinWidth() {
        return KonfigScreenMetrics.CONTROL_MIN_WIDTH;
    }

    int controlMaxWidth() {
        return KonfigScreenMetrics.CONTROL_MAX_WIDTH;
    }

    int validationColor() {
        return KonfigScreenMetrics.VALIDATION_COLOR;
    }

    int urlButtonWidth() {
        return KonfigScreenMetrics.URL_BUTTON_WIDTH;
    }

    int suggestionLimit() {
        return KonfigScreenMetrics.SUGGESTION_LIMIT;
    }

    int suggestionRowHeight() {
        return KonfigScreenMetrics.SUGGESTION_ROW_HEIGHT;
    }

    int dropdownChevronWidth() {
        return KonfigScreenMetrics.DROPDOWN_CHEVRON_WIDTH;
    }

    long dropdownTypeSelectResetMs() {
        return KonfigScreenMetrics.DROPDOWN_TYPE_SELECT_RESET_MS;
    }

    int listTop() {
        return KonfigScreenMetrics.LIST_TOP;
    }

    int screenHeight() {
        return this.screen.screenHeight();
    }

    Object draft(ConfigValueImpl<?> value) {
        return this.coordinator.draft(value);
    }

    boolean readBoolean(ConfigValueImpl<?> value) {
        return this.coordinator.readBoolean(value);
    }

    Enum<?> currentEnum(ConfigValueImpl<?> value) {
        return this.coordinator.currentEnum(value);
    }

    Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        return this.coordinator.cycleEnum(value);
    }

    int currentColor(ConfigValueImpl<?> value) {
        return this.coordinator.currentColor(value);
    }

    List<String> currentStringList(ConfigValueImpl<?> value) {
        return this.coordinator.currentStringList(value);
    }

    String currentDropdownValue(ConfigValueImpl<?> value) {
        return this.coordinator.currentDropdownValue(value);
    }

    String currentStringValue(ConfigValueImpl<?> value) {
        return this.coordinator.currentStringValue(value);
    }

    int currentInt(ConfigValueImpl<?> value) {
        return this.coordinator.currentInt(value);
    }

    long currentLong(ConfigValueImpl<?> value) {
        return this.coordinator.currentLong(value);
    }

    double currentDouble(ConfigValueImpl<?> value) {
        return this.coordinator.currentDouble(value);
    }

    Component booleanText(ConfigValueImpl<?> value) {
        return this.coordinator.booleanText(value);
    }

    Component enumText(EntryRef entry, Enum<?> value) {
        return this.coordinator.enumText(entry, value);
    }

    Component colorText(ConfigValueImpl<?> value) {
        return this.coordinator.colorText(value);
    }

    Component stringListText(ConfigValueImpl<?> value) {
        return this.coordinator.stringListText(value);
    }

    Component dropdownText(EntryRef entry, String option) {
        return this.coordinator.dropdownText(entry, option);
    }

    List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        return this.coordinator.registrySuggestions(registryKey);
    }

    void queueTooltip(String tooltip, int mouseX, int mouseY) {
        this.coordinator.queueTooltip(tooltip, mouseX, mouseY);
    }

    void updateHoveredEntry(EntryRef entry, boolean hovered) {
        this.coordinator.updateHoveredEntry(entry, hovered);
    }

    void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.coordinator.setDraft(value, draft);
    }

    boolean persistEntry(EntryRef entry) {
        return this.coordinator.persistEntry(entry);
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
        this.coordinator.setActiveRegistryRow(row);
    }

    boolean isActiveRegistryRow(RegistryTextInputRow row) {
        return this.coordinator.isActiveRegistryRow(row);
    }

    void clearActiveRegistryRow(RegistryTextInputRow row) {
        this.coordinator.clearActiveRegistryRow(row);
    }

    void markRenderedRegistryRow(RegistryTextInputRow row) {
        this.coordinator.markRenderedRegistryRow(row);
    }

    void setActiveDropdownRow(DropdownRow row) {
        this.coordinator.setActiveDropdownRow(row);
    }

    void clearActiveDropdownRow(DropdownRow row) {
        this.coordinator.clearActiveDropdownRow(row);
    }

    void clearRenderedDropdownRow(DropdownRow row) {
        this.coordinator.clearRenderedDropdownRow(row);
    }

    void markRenderedDropdownRow(DropdownRow row) {
        this.coordinator.markRenderedDropdownRow(row);
    }
}
//?}
