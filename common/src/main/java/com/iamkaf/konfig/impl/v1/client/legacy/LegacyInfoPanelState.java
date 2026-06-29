package com.iamkaf.konfig.impl.v1.client.legacy;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.config.model.InfoPanelItem;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public final class LegacyInfoPanelState {
    private LegacyConfigEntry activeEntry;
    private LegacyConfigEntry dropdownOptionEntry;
    private List<InfoPanelItem> dropdownOptionInfo = Collections.emptyList();
    private final Map<List<InfoPanelItem>, Double> scrollPositions = new IdentityHashMap<List<InfoPanelItem>, Double>();
    private List<InfoPanelItem> renderedItems = Collections.emptyList();
    private double scroll;
    private int maxScroll;

    public interface DropdownInfoProvider {
        List<InfoPanelItem> selectedDropdownInfo(LegacyConfigEntry entry);
    }

    public void hover(LegacyConfigEntry entry) {
        this.activeEntry = entry;
    }

    public void dropdownOption(LegacyConfigEntry entry, List<InfoPanelItem> info) {
        this.dropdownOptionEntry = entry;
        this.dropdownOptionInfo = info == null ? Collections.<InfoPanelItem>emptyList() : info;
    }

    public void clearDropdownOption() {
        this.dropdownOptionEntry = null;
        this.dropdownOptionInfo = Collections.emptyList();
    }

    public List<InfoPanelItem> activeItems(
            List<LegacyConfigEntry> entries,
            LegacyConfigEntry hoveredEntry,
            boolean preserveActiveEntry,
            DropdownInfoProvider dropdownInfoProvider
    ) {
        if (this.dropdownOptionEntry != null && !this.dropdownOptionInfo.isEmpty()) {
            return this.dropdownOptionInfo;
        }

        LegacyConfigEntry selected = hoveredEntry;
        if (selected == null && preserveActiveEntry) {
            selected = this.activeEntry;
        }

        if (selected != null) {
            List<InfoPanelItem> selectedDropdownInfo = dropdownInfoProvider.selectedDropdownInfo(selected);
            if (!selectedDropdownInfo.isEmpty()) {
                return selectedDropdownInfo;
            }

            List<InfoPanelItem> entryInfo = selected.handle().entryInfo(selected.value().path());
            if (!entryInfo.isEmpty()) {
                return entryInfo;
            }

            if (!LegacyValueText.isBlank(selected.categoryPath())) {
                List<InfoPanelItem> categoryInfo = selected.handle().categoryInfo(selected.categoryPath());
                if (!categoryInfo.isEmpty()) {
                    return categoryInfo;
                }
            }

            List<InfoPanelItem> globalInfo = selected.handle().globalInfo();
            if (!globalInfo.isEmpty()) {
                return globalInfo;
            }
        }

        for (LegacyConfigEntry entry : entries) {
            List<InfoPanelItem> globalInfo = entry.handle().globalInfo();
            if (!globalInfo.isEmpty()) {
                return globalInfo;
            }
        }
        return Collections.emptyList();
    }

    public void setRenderedItems(List<InfoPanelItem> items) {
        if (items == this.renderedItems) {
            return;
        }
        rememberScroll();
        this.renderedItems = items;
        Double rememberedScroll = this.scrollPositions.get(items);
        this.scroll = rememberedScroll == null ? 0.0D : rememberedScroll.doubleValue();
    }

    public void clearRenderedItems() {
        setRenderedItems(Collections.<InfoPanelItem>emptyList());
        this.scroll = 0.0D;
        this.maxScroll = 0;
    }

    public double scroll() {
        return this.scroll;
    }

    public int maxScroll() {
        return this.maxScroll;
    }

    public void maxScroll(int maxScroll) {
        this.maxScroll = Math.max(0, maxScroll);
        this.scroll = clamp(this.scroll, 0.0D, (double) this.maxScroll);
        rememberScroll();
    }

    public boolean scrollBy(double scrollY, int step) {
        if (this.maxScroll <= 0) {
            return false;
        }
        this.scroll = clamp(this.scroll - (scrollY * step), 0.0D, (double) this.maxScroll);
        rememberScroll();
        return true;
    }

    private void rememberScroll() {
        if (!this.renderedItems.isEmpty()) {
            this.scrollPositions.put(this.renderedItems, Double.valueOf(this.scroll));
        }
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
