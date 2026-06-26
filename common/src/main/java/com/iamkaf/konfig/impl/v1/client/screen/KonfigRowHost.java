//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.screen;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.row.DropdownRowHandle;
import com.iamkaf.konfig.impl.v1.client.row.RegistryTextInputRowHandle;
import com.iamkaf.konfig.impl.v1.config.model.ConfigScreenValue;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
public final class KonfigRowHost {
    private final KonfigConfigScreen screen;
    private final KonfigScreenCoordinator coordinator;

    KonfigRowHost(KonfigConfigScreen screen, KonfigScreenCoordinator coordinator) {
        this.screen = screen;
        this.coordinator = coordinator;
    }

    public Screen screen() {
        return this.screen;
    }

    public Font font() {
        return this.screen.rowFont();
    }

    public int rowHeight() {
        return KonfigScreenMetrics.ROW_HEIGHT;
    }

    public int controlHeight() {
        return KonfigScreenMetrics.CONTROL_HEIGHT;
    }

    public int controlMinWidth() {
        return KonfigScreenMetrics.CONTROL_MIN_WIDTH;
    }

    public int controlMaxWidth() {
        return KonfigScreenMetrics.CONTROL_MAX_WIDTH;
    }

    public int validationColor() {
        return KonfigScreenMetrics.VALIDATION_COLOR;
    }

    public int urlButtonWidth() {
        return KonfigScreenMetrics.URL_BUTTON_WIDTH;
    }

    public int suggestionLimit() {
        return KonfigScreenMetrics.SUGGESTION_LIMIT;
    }

    public int suggestionRowHeight() {
        return KonfigScreenMetrics.SUGGESTION_ROW_HEIGHT;
    }

    public int dropdownChevronWidth() {
        return KonfigScreenMetrics.DROPDOWN_CHEVRON_WIDTH;
    }

    public long dropdownTypeSelectResetMs() {
        return KonfigScreenMetrics.DROPDOWN_TYPE_SELECT_RESET_MS;
    }

    public int listTop() {
        return KonfigScreenMetrics.LIST_TOP;
    }

    public int screenHeight() {
        return this.screen.screenHeight();
    }

    public Object draft(ConfigScreenValue<?> value) {
        return this.coordinator.draft(value);
    }

    public boolean readBoolean(ConfigScreenValue<?> value) {
        return this.coordinator.readBoolean(value);
    }

    public Enum<?> currentEnum(ConfigScreenValue<?> value) {
        return this.coordinator.currentEnum(value);
    }

    public Enum<?> cycleEnum(ConfigScreenValue<?> value) {
        return this.coordinator.cycleEnum(value);
    }

    public int currentColor(ConfigScreenValue<?> value) {
        return this.coordinator.currentColor(value);
    }

    public List<String> currentStringList(ConfigScreenValue<?> value) {
        return this.coordinator.currentStringList(value);
    }

    public String currentDropdownValue(ConfigScreenValue<?> value) {
        return this.coordinator.currentDropdownValue(value);
    }

    public String currentStringValue(ConfigScreenValue<?> value) {
        return this.coordinator.currentStringValue(value);
    }

    public int currentInt(ConfigScreenValue<?> value) {
        return this.coordinator.currentInt(value);
    }

    public long currentLong(ConfigScreenValue<?> value) {
        return this.coordinator.currentLong(value);
    }

    public double currentDouble(ConfigScreenValue<?> value) {
        return this.coordinator.currentDouble(value);
    }

    public Component booleanText(ConfigScreenValue<?> value) {
        return this.coordinator.booleanText(value);
    }

    public Component enumText(EntryRef entry, Enum<?> value) {
        return this.coordinator.enumText(entry, value);
    }

    public Component colorText(ConfigScreenValue<?> value) {
        return this.coordinator.colorText(value);
    }

    public Component stringListText(ConfigScreenValue<?> value) {
        return this.coordinator.stringListText(value);
    }

    public Component dropdownText(EntryRef entry, String option) {
        return this.coordinator.dropdownText(entry, option);
    }

    public List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        return this.coordinator.registrySuggestions(registryKey);
    }

    public void queueTooltip(String tooltip, int mouseX, int mouseY) {
        this.coordinator.queueTooltip(tooltip, mouseX, mouseY);
    }

    public void updateHoveredEntry(EntryRef entry, boolean hovered) {
        this.coordinator.updateHoveredEntry(entry, hovered);
    }

    public void setDraft(ConfigScreenValue<?> value, Object draft) {
        this.coordinator.setDraft(value, draft);
    }

    public boolean persistEntry(EntryRef entry) {
        return this.coordinator.persistEntry(entry);
    }

    public void openInlineUrl(EntryRef entry) {
        this.screen.openInlineUrl(entry);
    }

    public void openColorEditor(EntryRef entry) {
        this.screen.openColorEditor(entry);
    }

    public void openStringListEditor(EntryRef entry) {
        this.screen.openStringListEditor(entry);
    }

    public void setActiveRegistryRow(RegistryTextInputRowHandle row) {
        this.coordinator.setActiveRegistryRow(row);
    }

    public boolean isActiveRegistryRow(RegistryTextInputRowHandle row) {
        return this.coordinator.isActiveRegistryRow(row);
    }

    public void clearActiveRegistryRow(RegistryTextInputRowHandle row) {
        this.coordinator.clearActiveRegistryRow(row);
    }

    public void markRenderedRegistryRow(RegistryTextInputRowHandle row) {
        this.coordinator.markRenderedRegistryRow(row);
    }

    public void setActiveDropdownRow(DropdownRowHandle row) {
        this.coordinator.setActiveDropdownRow(row);
    }

    public void clearActiveDropdownRow(DropdownRowHandle row) {
        this.coordinator.clearActiveDropdownRow(row);
    }

    public void clearRenderedDropdownRow(DropdownRowHandle row) {
        this.coordinator.clearRenderedDropdownRow(row);
    }

    public void markRenderedDropdownRow(DropdownRowHandle row) {
        this.coordinator.markRenderedDropdownRow(row);
    }
}
//?}
