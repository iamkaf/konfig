package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

//? if >=1.17 {
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.sameValue;

import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
final class KonfigDropdownState {
    interface OptionSearch {
        boolean matches(int index, String query);
    }

    private boolean open;
    private int selectedIndex;
    private int scrollOffset;
    private int dropdownX;
    private int dropdownY;
    private int dropdownWidth;
    private int dropdownHeight;
    private final StringBuilder typeSelectBuffer = new StringBuilder();
    private long lastTypeSelectMillis;

    boolean isOpen() {
        return this.open;
    }

    int selectedIndex() {
        return this.selectedIndex;
    }

    int scrollOffset() {
        return this.scrollOffset;
    }

    int dropdownX() {
        return this.dropdownX;
    }

    int dropdownY() {
        return this.dropdownY;
    }

    int dropdownWidth() {
        return this.dropdownWidth;
    }

    int dropdownHeight() {
        return this.dropdownHeight;
    }

    boolean open(List<String> options, String currentValue, int maxVisibleOptions) {
        if (options.isEmpty()) {
            return false;
        }

        this.open = true;
        this.selectedIndex = this.optionIndex(options, currentValue);
        this.ensureSelectedVisible(options.size(), maxVisibleOptions);
        this.clearTypeSelect();
        return true;
    }

    void close() {
        this.open = false;
        this.clearTypeSelect();
    }

    int optionIndex(List<String> options, String option) {
        for (int index = 0; index < options.size(); index++) {
            if (sameValue(options.get(index), option)) {
                return index;
            }
        }
        return 0;
    }

    int visibleOptionCount(int optionCount, int maxVisibleOptions) {
        return Math.min(maxVisibleOptions, optionCount);
    }

    int maxScrollOffset(int optionCount, int maxVisibleOptions) {
        return Math.max(0, optionCount - this.visibleOptionCount(optionCount, maxVisibleOptions));
    }

    boolean selectNext(int optionCount, int maxVisibleOptions) {
        if (!this.open || optionCount <= 0) {
            return false;
        }
        this.selectedIndex = (this.selectedIndex + 1) % optionCount;
        this.ensureSelectedVisible(optionCount, maxVisibleOptions);
        return true;
    }

    boolean selectPrevious(int optionCount, int maxVisibleOptions) {
        if (!this.open || optionCount <= 0) {
            return false;
        }
        this.selectedIndex = (this.selectedIndex + optionCount - 1) % optionCount;
        this.ensureSelectedVisible(optionCount, maxVisibleOptions);
        return true;
    }

    boolean handleTypeSelect(int codePoint, List<String> options, int maxVisibleOptions, long resetMillis, OptionSearch search) {
        if (!this.open
                || options.isEmpty()
                || !Character.isValidCodePoint(codePoint)
                || Character.isISOControl(codePoint)) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastTypeSelectMillis > resetMillis) {
            this.typeSelectBuffer.setLength(0);
        }
        this.lastTypeSelectMillis = now;

        int normalizedCodePoint = Character.toLowerCase(codePoint);
        this.typeSelectBuffer.appendCodePoint(normalizedCodePoint);
        if (!this.focusFirstTypeMatch(this.typeSelectBuffer.toString(), options.size(), maxVisibleOptions, search)) {
            this.typeSelectBuffer.setLength(0);
            this.typeSelectBuffer.appendCodePoint(normalizedCodePoint);
            this.focusFirstTypeMatch(this.typeSelectBuffer.toString(), options.size(), maxVisibleOptions, search);
        }
        return true;
    }

    boolean scroll(double scrollY, int optionCount, int maxVisibleOptions) {
        if (!this.open) {
            return false;
        }

        int previousOffset = this.scrollOffset;
        if (scrollY > 0.0D) {
            this.scrollOffset--;
        } else if (scrollY < 0.0D) {
            this.scrollOffset++;
        }
        this.scrollOffset = clamp(this.scrollOffset, 0, this.maxScrollOffset(optionCount, maxVisibleOptions));
        if (this.scrollOffset != previousOffset && this.visibleOptionCount(optionCount, maxVisibleOptions) > 0) {
            this.selectedIndex = clamp(this.selectedIndex, this.scrollOffset, this.scrollOffset + this.visibleOptionCount(optionCount, maxVisibleOptions) - 1);
        }
        return true;
    }

    void layout(
            int buttonX,
            int buttonY,
            int buttonWidth,
            int minWidth,
            int controlHeight,
            int rowHeight,
            int optionCount,
            int maxVisibleOptions,
            int screenBottomY,
            int listTop
    ) {
        int visibleCount = this.visibleOptionCount(optionCount, maxVisibleOptions);
        this.dropdownWidth = Math.max(minWidth, buttonWidth);
        this.dropdownHeight = (visibleCount * rowHeight) + 4;
        this.dropdownX = buttonX;

        int belowY = buttonY + controlHeight + 2;
        int aboveY = buttonY - this.dropdownHeight - 2;
        boolean openAbove = belowY + this.dropdownHeight > screenBottomY && aboveY >= listTop;
        this.dropdownY = openAbove ? aboveY : belowY;
        this.scrollOffset = clamp(this.scrollOffset, 0, this.maxScrollOffset(optionCount, maxVisibleOptions));
    }

    boolean contains(double mouseX, double mouseY) {
        return this.open
                && mouseX >= this.dropdownX
                && mouseX <= this.dropdownX + this.dropdownWidth
                && mouseY >= this.dropdownY
                && mouseY <= this.dropdownY + this.dropdownHeight;
    }

    int hoveredIndex(int mouseX, int mouseY, int optionCount, int maxVisibleOptions, int rowHeight) {
        if (mouseX < this.dropdownX
                || mouseX > this.dropdownX + this.dropdownWidth
                || mouseY < this.dropdownY + 2
                || mouseY > this.dropdownY + this.dropdownHeight - 2) {
            return -1;
        }

        int visibleIndex = (mouseY - this.dropdownY - 2) / rowHeight;
        int index = this.scrollOffset + visibleIndex;
        return index >= 0
                && index < optionCount
                && visibleIndex < this.visibleOptionCount(optionCount, maxVisibleOptions)
                ? index
                : -1;
    }

    private void ensureSelectedVisible(int optionCount, int maxVisibleOptions) {
        int visibleCount = this.visibleOptionCount(optionCount, maxVisibleOptions);
        if (visibleCount <= 0) {
            this.scrollOffset = 0;
            return;
        }

        this.selectedIndex = clamp(this.selectedIndex, 0, optionCount - 1);
        if (this.selectedIndex < this.scrollOffset) {
            this.scrollOffset = this.selectedIndex;
        } else if (this.selectedIndex >= this.scrollOffset + visibleCount) {
            this.scrollOffset = this.selectedIndex - visibleCount + 1;
        }
        this.scrollOffset = clamp(this.scrollOffset, 0, this.maxScrollOffset(optionCount, maxVisibleOptions));
    }

    private boolean focusFirstTypeMatch(String query, int optionCount, int maxVisibleOptions, OptionSearch search) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        int start = Math.max(0, this.selectedIndex + 1);
        for (int offset = 0; offset < optionCount; offset++) {
            int index = (start + offset) % optionCount;
            if (search.matches(index, normalizedQuery)) {
                this.selectedIndex = index;
                this.ensureSelectedVisible(optionCount, maxVisibleOptions);
                return true;
            }
        }
        return false;
    }

    private void clearTypeSelect() {
        this.typeSelectBuffer.setLength(0);
        this.lastTypeSelectMillis = 0L;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
//?}
