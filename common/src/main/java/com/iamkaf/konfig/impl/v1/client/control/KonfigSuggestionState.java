//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.control;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.filterRegistrySuggestions;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.suggestionSuffix;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.sameValue;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
final class KonfigSuggestionState {
    private final List<String> visibleSuggestions = new ArrayList<String>();
    private boolean suggestionsDismissed;
    private String dismissedValue = "";
    private int selectedIndex;

    boolean isEmpty() {
        return this.visibleSuggestions.isEmpty();
    }

    int size() {
        return this.visibleSuggestions.size();
    }

    String suggestion(int index) {
        return this.visibleSuggestions.get(index);
    }

    int selectedIndex() {
        return this.selectedIndex;
    }

    String selectedSuggestion() {
        return this.visibleSuggestions.get(this.selectedIndex);
    }

    void refresh(List<String> candidates, String currentValue) {
        if (this.suggestionsDismissed) {
            if (sameValue(currentValue, this.dismissedValue)) {
                this.clearVisible();
                return;
            }
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
        }

        this.visibleSuggestions.clear();
        this.visibleSuggestions.addAll(filterRegistrySuggestions(candidates, currentValue));
        if (this.visibleSuggestions.isEmpty()) {
            this.selectedIndex = 0;
            return;
        }

        this.selectedIndex = clamp(this.selectedIndex, 0, this.visibleSuggestions.size() - 1);
    }

    void activate(List<String> candidates, String currentValue) {
        this.suggestionsDismissed = false;
        this.dismissedValue = "";
        this.refresh(candidates, currentValue);
    }

    void dismiss(String currentValue) {
        this.suggestionsDismissed = true;
        this.dismissedValue = currentValue;
        this.clearVisible();
    }

    void close() {
        this.suggestionsDismissed = false;
        this.dismissedValue = "";
        this.clearVisible();
    }

    boolean selectNext() {
        if (this.visibleSuggestions.isEmpty()) {
            return false;
        }
        this.selectedIndex = (this.selectedIndex + 1) % this.visibleSuggestions.size();
        return true;
    }

    boolean selectPrevious() {
        if (this.visibleSuggestions.isEmpty()) {
            return false;
        }
        this.selectedIndex = (this.selectedIndex + this.visibleSuggestions.size() - 1) % this.visibleSuggestions.size();
        return true;
    }

    String inlineSuggestion(String currentValue) {
        if (this.visibleSuggestions.isEmpty()) {
            return "";
        }
        return suggestionSuffix(currentValue, this.selectedSuggestion());
    }

    int hoveredIndex(int mouseX, int mouseY, int left, int top, int width, int height, int rowHeight) {
        if (mouseX < left
                || mouseX > left + width
                || mouseY < top + 2
                || mouseY > top + height - 2) {
            return -1;
        }
        int index = (mouseY - top - 2) / rowHeight;
        return index >= 0 && index < this.visibleSuggestions.size() ? index : -1;
    }

    private void clearVisible() {
        this.visibleSuggestions.clear();
        this.selectedIndex = 0;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
//?}
