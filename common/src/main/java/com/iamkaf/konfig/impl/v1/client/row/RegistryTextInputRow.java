//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.supportsRegistryIcon;

import com.iamkaf.konfig.impl.v1.client.control.KonfigRegistrySuggestionController;
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigRowHost;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
final class RegistryTextInputRow extends KonfigConfigRow implements RegistryTextInputRowHandle {
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 6;

    private final EditBox input;
    private final KonfigRegistrySuggestionController suggestions;
    private boolean suppressResponder;

    RegistryTextInputRow(KonfigRowHost host, EntryRef entry) {
        super(host, entry);
        this.input = new EditBox(host.font(), 0, 0, host.controlMinWidth(), host.controlHeight(), entry.label);
        this.input.setMaxLength(256);
        this.input.setValue(this.field().stringValue());
        this.input.setResponder(value -> {
            if (this.suppressResponder) {
                return;
            }
            this.field().setDraft(value);
            this.host.persistEntry(entry);
            this.refreshSuggestions();
        });
        this.suggestions = new KonfigRegistrySuggestionController(new KonfigRegistrySuggestionController.Owner() {
            @Override
            public boolean hasRegistryBinding() {
                return RegistryTextInputRow.this.entry.value.boundRegistryKey() != null;
            }

            @Override
            public ResourceKey<? extends Registry<?>> registryKey() {
                return RegistryTextInputRow.this.entry.value.boundRegistryKey();
            }

            @Override
            public List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
                return RegistryTextInputRow.this.host.registrySuggestions(registryKey);
            }

            @Override
            public String inputValue() {
                return RegistryTextInputRow.this.input.getValue();
            }

            @Override
            public void setInlineSuggestion(String suggestion) {
                RegistryTextInputRow.this.input.setSuggestion(suggestion);
            }

            @Override
            public boolean applySuggestion(String suggestion) {
                RegistryTextInputRow.this.suppressResponder = true;
                RegistryTextInputRow.this.input.setValue(suggestion);
                RegistryTextInputRow.this.suppressResponder = false;
                RegistryTextInputRow.this.field().setDraft(suggestion);
                RegistryTextInputRow.this.host.persistEntry(RegistryTextInputRow.this.entry);
                return true;
            }

            @Override
            public void focusInput() {
//? if >=1.19.4 {
                RegistryTextInputRow.this.input.setFocused(true);
//?} else {
                RegistryTextInputRow.this.input.setFocus(true);
//?}
            }

            @Override
            public Font font() {
                return RegistryTextInputRow.this.host.font();
            }

            @Override
            public int controlHeight() {
                return RegistryTextInputRow.this.host.controlHeight();
            }

            @Override
            public int suggestionRowHeight() {
                return RegistryTextInputRow.this.host.suggestionRowHeight();
            }

            @Override
            public int screenHeight() {
                return RegistryTextInputRow.this.host.screenHeight();
            }

            @Override
            public int listTop() {
                return RegistryTextInputRow.this.host.listTop();
            }
        });
    }

    @Override
    protected AbstractWidget control() {
        return this.input;
    }

    @Override
    public void tick() {
        if (this.input.isFocused()) {
            this.host.setActiveRegistryRow(this);
            this.refreshSuggestions();
        }
    }

    @Override
    protected void syncFromDraft() {
        this.suppressResponder = true;
        this.input.setValue(this.field().stringValue());
        this.suppressResponder = false;
        this.activateSuggestions();
    }

    @Override
    protected void renderRowContent(KonfigRenderContext context, KonfigRowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom) {
        boolean renderIcon = true;
//? if >=26.1 {
        tooltipRight = layout.controlX - 8;
        renderIcon = false;
//?}
        this.renderRegistryTextInputRow(context, layout.x, layout.y, layout.width, layout.height, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom, renderIcon);
    }

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
        this.suggestions.updateInputBounds(controlX, controlY, controlWidth);

        context.drawText(this.host.font(), this.entry.contextLabel, x + 4, y + 1, 0xFFA0A0A0);
        context.drawText(this.host.font(), this.entry.displayLabel(), x + 4, y + 12, 0xFFFFFFFF);
        if (renderIcon && this.entry.value.boundRegistryKey() != null && supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
            context.renderRegistryIcon(
                    this.entry.value.boundRegistryKey(),
                    this.field().stringValue(),
                    controlX - ICON_GAP - ICON_SIZE,
                    y + (height - ICON_SIZE) / 2
            );
        }
        context.renderWidget(this.input, mouseX, mouseY, partialTick);

        if (this.input.isFocused()) {
            this.host.setActiveRegistryRow(this);
            this.refreshSuggestions();
        }
        if (this.host.isActiveRegistryRow(this) && this.suggestions.hasVisibleSuggestions()) {
            this.host.markRenderedRegistryRow(this);
        }
    }

    public boolean isFocused() {
        return this.input.isFocused();
    }

    public boolean isPointInsideInput(double mouseX, double mouseY) {
        return this.suggestions.isPointInsideInput(mouseX, mouseY);
    }

    public void refreshSuggestions() {
        this.suggestions.refresh();
    }

    public void activateSuggestions() {
        this.suggestions.activate();
    }

    public void closeSuggestions() {
        this.suggestions.close();
        this.host.clearActiveRegistryRow(this);
    }

    public void renderSuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
        if (!this.host.isActiveRegistryRow(this)) {
            return;
        }
        this.suggestions.render(context, mouseX, mouseY);
    }

//? if >=1.21.9 {
    public boolean handleSuggestionClick(MouseButtonEvent event) {
        if (!this.host.isActiveRegistryRow(this)) {
            return false;
        }
        return this.suggestions.handleClick(event.x(), event.y());
    }

    public boolean handleSuggestionKey(KeyEvent event) {
        if (!this.host.isActiveRegistryRow(this)) {
            return false;
        }
        return this.suggestions.handleKey(event.key());
    }
//?} else {
    public boolean handleSuggestionClick(double mouseX, double mouseY) {
        if (!this.host.isActiveRegistryRow(this)) {
            return false;
        }
        return this.suggestions.handleClick(mouseX, mouseY);
    }

    public boolean handleSuggestionKey(int keyCode) {
        if (!this.host.isActiveRegistryRow(this)) {
            return false;
        }
        return this.suggestions.handleKey(keyCode);
    }
//?}
}
//?}
