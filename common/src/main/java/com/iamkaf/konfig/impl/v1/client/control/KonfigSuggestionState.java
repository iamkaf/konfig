//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.control;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues.sameValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
final class KonfigSuggestionState {
    private static final int SUGGESTION_LIMIT = 7;
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

    static List<String> filterRegistrySuggestions(List<String> candidates, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean tagQuery = normalized.startsWith("#");
        String search = tagQuery ? normalized.substring(1) : normalized;
        List<String> exact = new ArrayList<>();
        List<String> prefix = new ArrayList<>();
        List<String> contains = new ArrayList<>();

        for (String candidate : candidates) {
            boolean tagCandidate = candidate.startsWith("#");
            if (tagQuery != tagCandidate) {
                continue;
            }
            String id = (tagCandidate ? candidate.substring(1) : candidate).toLowerCase(Locale.ROOT);
            String path = id.substring(id.indexOf(':') + 1);
            if (search.isEmpty()) {
                prefix.add(candidate);
            } else if (id.equals(search) || path.equals(search)) {
                exact.add(candidate);
            } else if (id.startsWith(search) || path.startsWith(search)) {
                prefix.add(candidate);
            } else if (id.contains(search) || path.contains(search)) {
                contains.add(candidate);
            }
        }

        List<String> result = new ArrayList<>(SUGGESTION_LIMIT);
        appendSuggestions(result, exact);
        appendSuggestions(result, prefix);
        appendSuggestions(result, contains);
        return result;
    }

    private static void appendSuggestions(List<String> target, List<String> source) {
        for (String value : source) {
            if (target.size() >= SUGGESTION_LIMIT) {
                return;
            }
            target.add(value);
        }
    }

    private static String suggestionSuffix(String currentValue, String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return "";
        }
        String current = currentValue == null ? "" : currentValue;
        if (current.isEmpty()) {
            return suggestion;
        }
        if (suggestion.regionMatches(true, 0, current, 0, current.length())) {
            return suggestion.substring(current.length());
        }
        return "";
    }
}
//?}
