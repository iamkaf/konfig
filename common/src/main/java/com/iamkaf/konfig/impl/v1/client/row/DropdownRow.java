//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.*;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigRowHost;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
final class DropdownRow extends KonfigConfigRow implements DropdownRowHandle {
    private final Button button;
    private final KonfigDropdownState dropdown = new KonfigDropdownState();
    private int lastButtonX;
    private int lastButtonY;
    private int lastButtonWidth;

    DropdownRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.lastButtonWidth = host.controlMinWidth();
        this.button = button(
                0,
                0,
                host.controlMinWidth(),
                host.controlHeight(),
                text(""),
                ignored -> this.toggleDropdown()
        );
    }

    @Override
    public EntryRef entry() {
        return this.entry;
    }

    @Override
    protected AbstractWidget control() {
        return this.button;
    }

    @Override
    protected void syncFromDraft() {
        this.button.setMessage(text(""));
    }

    @Override
    protected String rowTooltip() {
        if (this.dropdown.isOpen()) {
            return this.entry.tooltip;
        }
        String optionTooltip = translatedDropdownTooltip(this.currentOption());
        return isBlank(optionTooltip) ? this.entry.tooltip : optionTooltip;
    }

    @Override
    protected void renderRowContent(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        super.renderRowContent(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
        this.captureButtonBounds(layout);
        this.renderButtonLabel(context);
        if (this.dropdown.isOpen()) {
            this.host.markRenderedDropdownRow(this);
        }
    }

    private void toggleDropdown() {
        if (this.dropdown.isOpen()) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    }

    private void openDropdown() {
        if (!this.dropdown.open(this.options(), this.field().dropdownValue(), this.host.suggestionLimit())) {
            return;
        }
        this.host.setActiveDropdownRow(this);
    }

    public void closeDropdown() {
        this.dropdown.close();
        this.host.clearActiveDropdownRow(this);
        this.host.clearRenderedDropdownRow(this);
    }

    private List<String> options() {
        return this.entry.value.dropdownOptions();
    }

    private DropdownOptionMetadata option(int index) {
        return this.field().dropdownOption(index);
    }

    private DropdownOptionMetadata currentOption() {
        return this.field().currentDropdownOption();
    }

    public boolean isButtonFocused() {
        return this.button.isFocused();
    }

    public DropdownOptionMetadata activeInfoOption(int mouseX, int mouseY) {
        if (!this.dropdown.isOpen()) {
            return null;
        }

        this.layoutDropdown();
        int hovered = this.hoveredOptionIndex(mouseX, mouseY);
        if (hovered >= 0) {
            return this.option(hovered);
        }
        return this.option(this.dropdown.selectedIndex());
    }

    private int visibleOptionCount() {
        return this.dropdown.visibleOptionCount(this.options().size(), this.host.suggestionLimit());
    }

    private int maxScrollOffset() {
        return this.dropdown.maxScrollOffset(this.options().size(), this.host.suggestionLimit());
    }

    private void selectOption(int optionIndex) {
        List<String> options = this.options();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            return;
        }

        Object previousDraft = this.field().draft();
        this.field().setDropdownValue(options.get(optionIndex));
        this.commitOrRevert(previousDraft);
        this.syncFromDraft();
        this.closeDropdown();
    }

//? if >=1.21.9 {
    public boolean handleDropdownClick(MouseButtonEvent event) {
        return this.handleDropdownClick(event.x(), event.y());
    }

    public boolean handleDropdownKey(KeyEvent event) {
        return this.handleDropdownKey(event.key());
    }
//?}

    public boolean handleDropdownClick(double mouseX, double mouseY) {
        if (!this.dropdown.isOpen()) {
            return false;
        }
        this.layoutDropdown();
        if (!this.isPointInsideDropdown(mouseX, mouseY)) {
            return false;
        }

        int hovered = this.hoveredOptionIndex((int) mouseX, (int) mouseY);
        if (hovered >= 0) {
            this.selectOption(hovered);
        }
        return true;
    }

    public boolean handleDropdownKey(int keyCode) {
        List<String> options = this.options();
        if (!this.dropdown.isOpen() || options.isEmpty()) {
            return false;
        }

        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.closeDropdown();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER
                || keyCode == InputConstants.KEY_SPACE
                || keyCode == InputConstants.KEY_TAB) {
            this.selectOption(this.dropdown.selectedIndex());
            return true;
        }
        if (keyCode == InputConstants.KEY_DOWN) {
            this.dropdown.selectNext(options.size(), this.host.suggestionLimit());
            return true;
        }
        if (keyCode == InputConstants.KEY_UP) {
            this.dropdown.selectPrevious(options.size(), this.host.suggestionLimit());
            return true;
        }
        return false;
    }

    public boolean handleClosedDropdownKey(int keyCode) {
        if (this.dropdown.isOpen() || this.options().isEmpty()) {
            return false;
        }
        if (keyCode == InputConstants.KEY_RETURN
                || keyCode == InputConstants.KEY_NUMPADENTER
                || keyCode == InputConstants.KEY_SPACE) {
            this.openDropdown();
            return true;
        }
        return false;
    }

    public boolean handleDropdownChar(int codePoint) {
        return this.dropdown.handleTypeSelect(codePoint, this.options(), this.host.suggestionLimit(), this.host.dropdownTypeSelectResetMs(), (index, query) -> this.optionSearchText(index).startsWith(query));
    }

    private String optionSearchText(int index) {
        DropdownOptionMetadata option = this.option(index);
        if (option == null) {
            return "";
        }

        String label = this.field().dropdownText(option.value()).getString();
        return (label + " " + option.value()).toLowerCase(Locale.ROOT);
    }

    public boolean handleDropdownScroll(double mouseX, double mouseY, double scrollY) {
        if (!this.dropdown.isOpen()) {
            return false;
        }
        this.layoutDropdown();
        if (!this.isPointInsideDropdown(mouseX, mouseY)) {
            return false;
        }

        return this.dropdown.scroll(scrollY, this.options().size(), this.host.suggestionLimit());
    }

    public boolean isPointInsideButton(double mouseX, double mouseY) {
        return mouseX >= this.lastButtonX
                && mouseX <= this.lastButtonX + this.lastButtonWidth
                && mouseY >= this.lastButtonY
                && mouseY <= this.lastButtonY + this.host.controlHeight();
    }

    public boolean isPointInsideDropdown(double mouseX, double mouseY) {
        if (!this.dropdown.isOpen()) {
            return false;
        }
        this.layoutDropdown();
        return this.dropdown.contains(mouseX, mouseY);
    }

    private void layoutDropdown() {
        this.dropdown.layout(
                this.lastButtonX,
                this.lastButtonY,
                this.lastButtonWidth,
                this.host.controlMinWidth(),
                this.host.controlHeight(),
                this.host.suggestionRowHeight(),
                this.options().size(),
                this.host.suggestionLimit(),
                this.host.screenHeight() - 32,
                this.host.listTop()
        );
    }

//? if >=1.19.3 {
    private void captureButtonBounds() {
        this.lastButtonX = this.button.getX();
        this.lastButtonY = this.button.getY();
        this.lastButtonWidth = this.button.getWidth();
    }
//?}

    private void captureButtonBounds(KonfigRowLayout layout) {
//? if >=1.19.3 {
        this.captureButtonBounds();
//?} else {
        this.lastButtonX = this.button.x;
        this.lastButtonY = this.button.y;
        this.lastButtonWidth = layout.controlWidth;
//?}
    }

    private void renderButtonLabel(KonfigRenderContext context) {
        int textX = this.lastButtonX + 6;
        int chevronLeft = this.lastButtonX + this.lastButtonWidth - this.host.dropdownChevronWidth();
        int textMaxWidth = Math.max(0, chevronLeft - textX - 4);
        int textY = this.lastButtonY + ((this.host.controlHeight() - this.host.font().lineHeight) / 2) + 1;
        Component valueText = this.fitDropdownText(this.field().dropdownText(this.field().dropdownValue()), textMaxWidth);
        context.drawText(this.host.font(), valueText, textX, textY, 0xFFFFFFFF);

        Component chevron = text(this.dropdown.isOpen() ? "\u25B4" : "\u25BE");
        int chevronX = chevronLeft + Math.max(0, (this.host.dropdownChevronWidth() - this.host.font().width(chevron)) / 2);
        context.drawText(this.host.font(), chevron, chevronX, textY, this.dropdown.isOpen() ? 0xFFF8E38F : 0xFFCFCFCF);
    }

    private Component fitDropdownText(Component value, int maxWidth) {
        if (maxWidth <= 0) {
            return text("");
        }
        if (this.host.font().width(value) <= maxWidth) {
            return value;
        }

        String ellipsis = "...";
        int available = Math.max(0, maxWidth - this.host.font().width(ellipsis));
        String trimmed = this.host.font().plainSubstrByWidth(value.getString(), available).trim();
        return text(trimmed + ellipsis);
    }

    private int hoveredOptionIndex(int mouseX, int mouseY) {
        return this.dropdown.hoveredIndex(mouseX, mouseY, this.options().size(), this.host.suggestionLimit(), this.host.suggestionRowHeight());
    }

    public void renderDropdown(KonfigRenderContext context, int mouseX, int mouseY) {
        List<String> options = this.options();
        if (!this.dropdown.isOpen() || options.isEmpty()) {
            return;
        }

        this.layoutDropdown();
        int dropdownX = this.dropdown.dropdownX();
        int dropdownY = this.dropdown.dropdownY();
        int dropdownWidth = this.dropdown.dropdownWidth();
        int dropdownHeight = this.dropdown.dropdownHeight();
        context.fill(dropdownX - 1, dropdownY - 1, dropdownX + dropdownWidth + 1, dropdownY + dropdownHeight + 1, 0xFF202020);
        context.fill(dropdownX, dropdownY, dropdownX + dropdownWidth, dropdownY + dropdownHeight, 0xFF101010);

        int hovered = this.hoveredOptionIndex(mouseX, mouseY);
        DropdownOptionMetadata tooltipOption = this.option(hovered >= 0 ? hovered : this.dropdown.selectedIndex());
        String tooltip = translatedDropdownTooltip(tooltipOption);
        if (!isBlank(tooltip)) {
            this.host.queueTooltip(tooltip, mouseX, mouseY);
        }
        int visibleCount = this.visibleOptionCount();
        int currentIndex = this.dropdown.optionIndex(options, this.field().dropdownValue());
        for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
            int optionIndex = this.dropdown.scrollOffset() + visibleIndex;
            if (optionIndex >= options.size()) {
                break;
            }

            int rowY = dropdownY + 2 + (visibleIndex * this.host.suggestionRowHeight());
            int rowBottom = rowY + this.host.suggestionRowHeight();
            boolean rowHovered = optionIndex == hovered;
            boolean focused = optionIndex == this.dropdown.selectedIndex();
            boolean current = optionIndex == currentIndex;
            if (rowHovered || focused || current) {
                int color = rowHovered ? 0x805C6FA8 : focused ? 0x60406080 : 0x50303030;
                context.fill(dropdownX + 1, rowY, dropdownX + dropdownWidth - 1, rowBottom, color);
            }
            if (current) {
                context.fill(dropdownX + 2, rowY + 2, dropdownX + 4, rowBottom - 2, 0xFFF8E38F);
            }
            int textX = dropdownX + 8;
            int textRight = this.maxScrollOffset() > 0 ? dropdownX + dropdownWidth - 8 : dropdownX + dropdownWidth - 4;
            context.drawText(this.host.font(), this.fitDropdownText(this.field().dropdownText(options.get(optionIndex)), Math.max(0, textRight - textX)), textX, rowY + 3, 0xFFFFFFFF);
        }

        if (this.maxScrollOffset() > 0) {
            int trackTop = dropdownY + 2;
            int trackBottom = dropdownY + dropdownHeight - 2;
            int trackHeight = Math.max(1, trackBottom - trackTop);
            int thumbHeight = Mth.clamp((trackHeight * visibleCount) / options.size(), 10, trackHeight);
            int thumbTop = trackTop + ((trackHeight - thumbHeight) * this.dropdown.scrollOffset() / this.maxScrollOffset());
            context.fill(dropdownX + dropdownWidth - 4, trackTop, dropdownX + dropdownWidth - 2, trackBottom, 0x44000000);
            context.fill(dropdownX + dropdownWidth - 4, thumbTop, dropdownX + dropdownWidth - 2, thumbTop + thumbHeight, 0xAAFFFFFF);
        }
    }
}
//?}
