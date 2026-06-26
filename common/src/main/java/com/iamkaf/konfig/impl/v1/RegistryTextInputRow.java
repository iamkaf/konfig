//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigRegistryAdapter.supportsRegistryIcon;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.text;

import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

final class RegistryTextInputRow extends KonfigConfigRow {
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 6;

    private final EditBox input;
    private final KonfigSuggestionState suggestions = new KonfigSuggestionState();
    private boolean suppressResponder;
    private int lastInputX;
    private int lastInputY;
    private int lastInputWidth;
    private int lastDropdownX;
    private int lastDropdownY;
    private int lastDropdownWidth;
    private int lastDropdownHeight;

    RegistryTextInputRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.input = new EditBox(host.font(), 0, 0, host.controlMinWidth(), host.controlHeight(), entry.label);
        this.input.setMaxLength(256);
        this.input.setValue(host.currentStringValue(entry.value));
        this.input.setResponder(value -> {
            if (this.suppressResponder) {
                return;
            }
            this.host.setDraft(entry.value, value);
            this.host.persistEntry(entry);
            this.refreshSuggestions();
        });
    }

    @Override
    protected AbstractWidget control() {
        return this.input;
    }

    @Override
    protected void tick() {
        if (this.input.isFocused()) {
            this.host.setActiveRegistryRow(this);
            this.refreshSuggestions();
        }
    }

    @Override
    protected void syncFromDraft() {
        this.suppressResponder = true;
        this.input.setValue(this.host.currentStringValue(this.entry.value));
        this.suppressResponder = false;
        this.activateSuggestions();
    }

//? if >=26.1 {
    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        int x = this.getContentX();
        int y = this.getContentY();
        int width = this.getContentWidth();
        int height = this.getContentHeight();
        int controlWidth = Math.min(this.host.controlMaxWidth(), Math.max(this.host.controlMinWidth(), width / 2));
        int labelRight = x + width - controlWidth - 8;
        this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, labelRight, y + height, false);
    }
//?} elif >=1.21.9 {
    @Override
    public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
        int x = this.getContentX();
        int y = this.getContentY();
        int width = this.getContentWidth();
        int height = this.getContentHeight();
        this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), true);
    }
//?} elif >=1.20 {
    @Override
    protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, true);
    }
//?} else {
    @Override
    protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
        this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, true);
    }
//?}

    private void renderRegistryTextInputRow(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom, boolean renderIcon) {
        this.host.updateHoveredEntry(this.entry, hovered);
        if (hovered) {
            context.fill(x, y, x + width, y + height, 0x22000000);
        }

        context.showTooltip(this.host.screen(), this.host.font(), this.entry.tooltip, mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);

        int controlWidth = Math.min(this.host.controlMaxWidth(), Math.max(this.host.controlMinWidth(), width / 2));
        int controlX = x + width - controlWidth;
        int controlY = y + (height - this.host.controlHeight()) / 2;
        layoutControl(this.control(), controlX, controlY, controlWidth);
        this.lastInputX = controlX;
        this.lastInputY = controlY;
        this.lastInputWidth = controlWidth;

        context.drawText(this.host.font(), this.entry.contextLabel, x + 4, y + 1, 0xFFA0A0A0);
        context.drawText(this.host.font(), this.entry.displayLabel(), x + 4, y + 12, 0xFFFFFFFF);
        if (renderIcon && this.entry.value.boundRegistryKey() != null && supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
            context.renderRegistryIcon(
                    this.entry.value.boundRegistryKey(),
                    this.host.currentStringValue(this.entry.value),
                    controlX - ICON_GAP - ICON_SIZE,
                    y + (height - ICON_SIZE) / 2
            );
        }
        context.renderWidget(this.input, mouseX, mouseY, partialTick);

        if (this.input.isFocused()) {
            this.host.setActiveRegistryRow(this);
            this.refreshSuggestions();
        }
        if (this.host.isActiveRegistryRow(this) && !this.suggestions.isEmpty()) {
            this.host.markRenderedRegistryRow(this);
        }
    }

    public boolean isFocused() {
        return this.input.isFocused();
    }

    boolean isPointInsideInput(double mouseX, double mouseY) {
        return mouseX >= this.lastInputX
                && mouseX <= this.lastInputX + this.lastInputWidth
                && mouseY >= this.lastInputY
                && mouseY <= this.lastInputY + this.host.controlHeight();
    }

    void refreshSuggestions() {
        if (this.entry.value.boundRegistryKey() == null) {
            this.closeSuggestions();
            return;
        }

        this.suggestions.refresh(
                this.host.registrySuggestions(this.entry.value.boundRegistryKey()),
                this.input.getValue()
        );
        this.updateInlineSuggestion();
    }

    void activateSuggestions() {
        if (this.entry.value.boundRegistryKey() == null) {
            this.closeSuggestions();
            return;
        }

        this.suggestions.activate(
                this.host.registrySuggestions(this.entry.value.boundRegistryKey()),
                this.input.getValue()
        );
        this.updateInlineSuggestion();
    }

    private void dismissSuggestions() {
        this.suggestions.dismiss(this.input.getValue());
        this.updateInlineSuggestion();
    }

    void closeSuggestions() {
        this.suggestions.close();
        this.updateInlineSuggestion();
        this.host.clearActiveRegistryRow(this);
    }

    void renderSuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
        if (!this.host.isActiveRegistryRow(this) || this.suggestions.isEmpty()) {
            return;
        }

        this.layoutSuggestionBox();
        context.fill(this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
        context.fill(this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

        for (int index = 0; index < this.suggestions.size(); index++) {
            int rowY = this.lastDropdownY + 2 + (index * this.host.suggestionRowHeight());
            int rowBottom = rowY + this.host.suggestionRowHeight();
            boolean hovered = index == this.hoveredSuggestionIndex(mouseX, mouseY);
            if (hovered || index == this.suggestions.selectedIndex()) {
                context.fill(this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, hovered ? 0x80406080 : 0x50303030);
            }
            int textX = this.lastDropdownX + 4;
            if (this.entry.value.boundRegistryKey() != null && supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                context.renderRegistryIcon(this.entry.value.boundRegistryKey(), this.suggestions.suggestion(index), this.lastDropdownX + 2, rowY - 1);
                textX += 18;
            }
            context.drawText(this.host.font(), text(this.suggestions.suggestion(index)), textX, rowY + 3, 0xFFFFFFFF);
        }
    }

//? if >=1.21.9 {
    boolean handleSuggestionClick(MouseButtonEvent event) {
        if (!this.host.isActiveRegistryRow(this) || this.suggestions.isEmpty()) {
            return false;
        }

        int hovered = this.hoveredSuggestionIndex((int) event.x(), (int) event.y());
        if (hovered < 0) {
            return false;
        }

        this.acceptSuggestion(this.suggestions.suggestion(hovered));
        return true;
    }

    boolean handleSuggestionKey(KeyEvent event) {
        if (!this.host.isActiveRegistryRow(this)) {
            return false;
        }

        int keyCode = event.key();
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.dismissSuggestions();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            this.dismissSuggestions();
            return true;
        }
        if (this.suggestions.isEmpty()) {
            return false;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            this.suggestions.selectNext();
            this.updateInlineSuggestion();
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            this.suggestions.selectPrevious();
            this.updateInlineSuggestion();
            return true;
        }
        if (keyCode == InputConstants.KEY_TAB) {
            this.acceptSuggestion(this.suggestions.selectedSuggestion());
            return true;
        }
        return false;
    }
//?} else {
    boolean handleSuggestionClick(double mouseX, double mouseY) {
        if (!this.host.isActiveRegistryRow(this) || this.suggestions.isEmpty()) {
            return false;
        }

        int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
        if (hovered < 0) {
            return false;
        }

        this.acceptSuggestion(this.suggestions.suggestion(hovered));
        return true;
    }

    boolean handleSuggestionKey(int keyCode) {
        if (!this.host.isActiveRegistryRow(this)) {
            return false;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.dismissSuggestions();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            this.dismissSuggestions();
            return true;
        }
        if (this.suggestions.isEmpty()) {
            return false;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            this.suggestions.selectNext();
            this.updateInlineSuggestion();
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            this.suggestions.selectPrevious();
            this.updateInlineSuggestion();
            return true;
        }
        if (keyCode == InputConstants.KEY_TAB) {
            this.acceptSuggestion(this.suggestions.selectedSuggestion());
            return true;
        }
        return false;
    }
//?}

    private void acceptSuggestion(String suggestion) {
        this.suppressResponder = true;
        this.input.setValue(suggestion);
        this.suppressResponder = false;
        this.host.setDraft(this.entry.value, suggestion);
        this.host.persistEntry(this.entry);
        this.dismissSuggestions();
//? if >=1.19.4 {
        this.input.setFocused(true);
//?} else {
        this.input.setFocus(true);
//?}
    }

    private void updateInlineSuggestion() {
        this.input.setSuggestion(this.suggestions.inlineSuggestion(this.input.getValue()));
    }

    private void layoutSuggestionBox() {
        this.lastDropdownX = this.lastInputX;
        this.lastDropdownWidth = this.lastInputWidth;
        this.lastDropdownHeight = (this.suggestions.size() * this.host.suggestionRowHeight()) + 4;

        int belowY = this.lastInputY + this.host.controlHeight() + 2;
        int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
        boolean openAbove = belowY + this.lastDropdownHeight > this.host.screenHeight() - 32 && aboveY >= this.host.listTop();
        this.lastDropdownY = openAbove ? aboveY : belowY;
    }

    private int hoveredSuggestionIndex(int mouseX, int mouseY) {
        return this.suggestions.hoveredIndex(mouseX, mouseY, this.lastDropdownX, this.lastDropdownY, this.lastDropdownWidth, this.lastDropdownHeight, this.host.suggestionRowHeight());
    }
}
//?}
