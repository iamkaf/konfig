//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.row;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.supportsRegistryIcon;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;

import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
public final class KonfigRegistrySuggestionController {
    private final Owner owner;
    private final KonfigSuggestionState suggestions = new KonfigSuggestionState();
    private int inputX;
    private int inputY;
    private int inputWidth;
    private int dropdownX;
    private int dropdownY;
    private int dropdownWidth;
    private int dropdownHeight;

    public KonfigRegistrySuggestionController(Owner owner) {
        this.owner = owner;
    }

    public void updateInputBounds(int x, int y, int width) {
        this.inputX = x;
        this.inputY = y;
        this.inputWidth = width;
    }

    public boolean isPointInsideInput(double mouseX, double mouseY) {
        return mouseX >= this.inputX
                && mouseX <= this.inputX + this.inputWidth
                && mouseY >= this.inputY
                && mouseY <= this.inputY + this.owner.controlHeight();
    }

    public boolean hasVisibleSuggestions() {
        return !this.suggestions.isEmpty();
    }

    public void refresh() {
        if (!this.owner.hasRegistryBinding()) {
            this.close();
            return;
        }

        this.suggestions.refresh(this.owner.registrySuggestions(this.owner.registryKey()), this.owner.inputValue());
        this.updateInlineSuggestion();
    }

    public void activate() {
        if (!this.owner.hasRegistryBinding()) {
            this.close();
            return;
        }

        this.suggestions.activate(this.owner.registrySuggestions(this.owner.registryKey()), this.owner.inputValue());
        this.updateInlineSuggestion();
    }

    public void close() {
        this.suggestions.close();
        this.updateInlineSuggestion();
    }

    public void dismiss() {
        this.suggestions.dismiss(this.owner.inputValue());
        this.updateInlineSuggestion();
    }

    public void render(KonfigRenderContext context, int mouseX, int mouseY) {
        if (this.suggestions.isEmpty()) {
            return;
        }

        this.layoutSuggestionBox();
        context.fill(this.dropdownX - 1, this.dropdownY - 1, this.dropdownX + this.dropdownWidth + 1, this.dropdownY + this.dropdownHeight + 1, 0xFF202020);
        context.fill(this.dropdownX, this.dropdownY, this.dropdownX + this.dropdownWidth, this.dropdownY + this.dropdownHeight, 0xFF101010);

        ResourceKey<? extends Registry<?>> registryKey = this.owner.registryKey();
        boolean renderIcons = registryKey != null && supportsRegistryIcon(registryKey);
        for (int index = 0; index < this.suggestions.size(); index++) {
            int rowY = this.dropdownY + 2 + (index * this.owner.suggestionRowHeight());
            int rowBottom = rowY + this.owner.suggestionRowHeight();
            boolean hovered = index == this.hoveredSuggestionIndex(mouseX, mouseY);
            if (hovered || index == this.suggestions.selectedIndex()) {
                context.fill(this.dropdownX + 1, rowY, this.dropdownX + this.dropdownWidth - 1, rowBottom, hovered ? 0x80406080 : 0x50303030);
            }

            int textX = this.dropdownX + 4;
            if (renderIcons) {
                context.renderRegistryIcon(registryKey, this.suggestions.suggestion(index), this.dropdownX + 2, rowY - 1);
                textX += 18;
            }
            context.drawText(this.owner.font(), text(this.suggestions.suggestion(index)), textX, rowY + 3, 0xFFFFFFFF);
        }
    }

    public boolean handleClick(double mouseX, double mouseY) {
        if (this.suggestions.isEmpty()) {
            return false;
        }

        int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
        if (hovered < 0) {
            return false;
        }

        return this.acceptSuggestion(this.suggestions.suggestion(hovered));
    }

    public boolean handleKey(int keyCode) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.dismiss();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            this.dismiss();
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
            return this.acceptSuggestion(this.suggestions.selectedSuggestion());
        }
        return false;
    }

    private boolean acceptSuggestion(String suggestion) {
        if (this.owner.applySuggestion(suggestion)) {
            this.dismiss();
            this.owner.focusInput();
        }
        return true;
    }

    private void updateInlineSuggestion() {
        this.owner.setInlineSuggestion(this.suggestions.inlineSuggestion(this.owner.inputValue()));
    }

    private void layoutSuggestionBox() {
        this.dropdownX = this.inputX;
        this.dropdownWidth = this.inputWidth;
        this.dropdownHeight = (this.suggestions.size() * this.owner.suggestionRowHeight()) + 4;

        int belowY = this.inputY + this.owner.controlHeight() + 2;
        int aboveY = this.inputY - this.dropdownHeight - 2;
        boolean openAbove = belowY + this.dropdownHeight > this.owner.screenHeight() - 32 && aboveY >= this.owner.listTop();
        this.dropdownY = openAbove ? aboveY : belowY;
    }

    private int hoveredSuggestionIndex(int mouseX, int mouseY) {
        return this.suggestions.hoveredIndex(mouseX, mouseY, this.dropdownX, this.dropdownY, this.dropdownWidth, this.dropdownHeight, this.owner.suggestionRowHeight());
    }

    public interface Owner {
        boolean hasRegistryBinding();

        ResourceKey<? extends Registry<?>> registryKey();

        List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey);

        String inputValue();

        void setInlineSuggestion(String suggestion);

        boolean applySuggestion(String suggestion);

        void focusInput();

        Font font();

        int controlHeight();

        int suggestionRowHeight();

        int screenHeight();

        int listTop();
    }
}
//?}
