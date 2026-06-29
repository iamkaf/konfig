package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
public final class LegacyDropdownState {
    private boolean open;
    private int selectedIndex;
    private int scrollOffset;
    private final StringBuilder typeSelectBuffer = new StringBuilder();
    private long lastTypeSelectMillis;

    public interface OptionSearchText {
        String searchText(int index);
    }

    public boolean open() {
        return this.open;
    }

    public int selectedIndex() {
        return this.selectedIndex;
    }

    public int scrollOffset() {
        return this.scrollOffset;
    }

    public void open(List<String> options, String currentValue, int visibleLimit) {
        if (options.isEmpty()) {
            return;
        }

        this.open = true;
        this.selectedIndex = optionIndex(options, currentValue);
        ensureSelectedVisible(options, visibleLimit);
        this.typeSelectBuffer.setLength(0);
        this.lastTypeSelectMillis = 0L;
    }

    public void close() {
        this.open = false;
        this.typeSelectBuffer.setLength(0);
    }

    public int visibleOptionCount(List<String> options, int visibleLimit) {
        return Math.min(visibleLimit, options.size());
    }

    public int maxScrollOffset(List<String> options, int visibleLimit) {
        return Math.max(0, options.size() - visibleOptionCount(options, visibleLimit));
    }

    public void ensureSelectedVisible(List<String> options, int visibleLimit) {
        int visibleCount = visibleOptionCount(options, visibleLimit);
        if (visibleCount <= 0) {
            this.scrollOffset = 0;
            return;
        }
        if (this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        } else if (this.selectedIndex >= this.scrollOffset + visibleCount) {
            this.scrollOffset = this.selectedIndex - visibleCount + 1;
        }
        this.scrollOffset = clamp(this.scrollOffset, 0, maxScrollOffset(options, visibleLimit));
    }

    public void selectNext(List<String> options, int visibleLimit) {
        if (options.isEmpty()) {
            return;
        }
        this.selectedIndex = (this.selectedIndex + 1) % options.size();
        ensureSelectedVisible(options, visibleLimit);
    }

    public void selectPrevious(List<String> options, int visibleLimit) {
        if (options.isEmpty()) {
            return;
        }
        this.selectedIndex = (this.selectedIndex + options.size() - 1) % options.size();
        ensureSelectedVisible(options, visibleLimit);
    }

    public void selectIndex(int index, List<String> options, int visibleLimit) {
        if (index < 0 || index >= options.size()) {
            return;
        }
        this.selectedIndex = index;
        ensureSelectedVisible(options, visibleLimit);
    }

    public boolean scroll(List<String> options, int visibleLimit, double scrollY) {
        int previousOffset = this.scrollOffset;
        if (scrollY > 0.0D) {
            this.scrollOffset--;
        } else if (scrollY < 0.0D) {
            this.scrollOffset++;
        }

        this.scrollOffset = clamp(this.scrollOffset, 0, maxScrollOffset(options, visibleLimit));
        if (this.scrollOffset != previousOffset && visibleOptionCount(options, visibleLimit) > 0) {
            this.selectedIndex = clamp(
                    this.selectedIndex,
                    this.scrollOffset,
                    this.scrollOffset + visibleOptionCount(options, visibleLimit) - 1
            );
        }
        return this.scrollOffset != previousOffset;
    }

    public boolean typeSelect(
            List<String> options,
            int visibleLimit,
            int codePoint,
            long nowMillis,
            long resetMillis,
            OptionSearchText searchText
    ) {
        if (options.isEmpty()
                || !Character.isValidCodePoint(codePoint)
                || Character.isISOControl(codePoint)) {
            return false;
        }

        if (nowMillis - this.lastTypeSelectMillis > resetMillis) {
            this.typeSelectBuffer.setLength(0);
        }
        this.lastTypeSelectMillis = nowMillis;
        int normalizedCodePoint = Character.toLowerCase(codePoint);
        this.typeSelectBuffer.appendCodePoint(normalizedCodePoint);

        if (!focusFirstTypeMatch(options, visibleLimit, this.typeSelectBuffer.toString(), searchText)) {
            this.typeSelectBuffer.setLength(0);
            this.typeSelectBuffer.appendCodePoint(normalizedCodePoint);
            focusFirstTypeMatch(options, visibleLimit, this.typeSelectBuffer.toString(), searchText);
        }
        return true;
    }

    public int hoveredOptionIndex(
            int mouseX,
            int mouseY,
            int dropdownX,
            int dropdownY,
            int dropdownWidth,
            int dropdownHeight,
            int rowHeight,
            List<String> options,
            int visibleLimit
    ) {
        if (mouseX < dropdownX
                || mouseX > dropdownX + dropdownWidth
                || mouseY < dropdownY + 2
                || mouseY > dropdownY + dropdownHeight - 2) {
            return -1;
        }

        int visibleIndex = (mouseY - dropdownY - 2) / rowHeight;
        int index = this.scrollOffset + visibleIndex;
        return index >= 0 && index < options.size() && visibleIndex < visibleOptionCount(options, visibleLimit) ? index : -1;
    }

    public int optionIndex(List<String> options, String option) {
        for (int index = 0; index < options.size(); index++) {
            if (LegacyDraftSession.sameValue(options.get(index), option)) {
                return index;
            }
        }
        return 0;
    }

    private boolean focusFirstTypeMatch(
            List<String> options,
            int visibleLimit,
            String query,
            OptionSearchText searchText
    ) {
        if (LegacyValueText.isBlank(query)) {
            return false;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        int start = Math.max(0, this.selectedIndex + 1);
        for (int offset = 0; offset < options.size(); offset++) {
            int index = (start + offset) % options.size();
            if (searchText.searchText(index).startsWith(normalizedQuery)) {
                this.selectedIndex = index;
                ensureSelectedVisible(options, visibleLimit);
                return true;
            }
        }
        return false;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
