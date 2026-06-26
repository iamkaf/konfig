//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
final class KonfigInfoPanelState {
    static final int SCROLL_STEP = 18;

    private final List<EntryRef> entries;
    private final List<Link> links = new ArrayList<Link>();
    private final Map<List<InfoPanelItem>, Double> scrollPositions = new IdentityHashMap<List<InfoPanelItem>, Double>();
    private EntryRef hoveredEntry;
    private EntryRef activeInfoEntry;
    private EntryRef activeDropdownOptionEntry;
    private List<InfoPanelItem> activeDropdownOptionInfo = Collections.emptyList();
    private boolean mouseOverPanel;
    private boolean mouseOverBridge;
    private List<InfoPanelItem> renderedItems = Collections.emptyList();
    private double scroll;
    private int maxScroll;

    KonfigInfoPanelState(List<EntryRef> entries) {
        this.entries = entries;
    }

    void beginFrame(KonfigInfoPanelBounds bounds, int mouseX, int mouseY) {
        this.hoveredEntry = null;
        this.mouseOverPanel = bounds.containsPanel(mouseX, mouseY);
        this.mouseOverBridge = bounds.containsBridge(mouseX, mouseY);
        this.links.clear();
    }

    void updateHoveredEntry(EntryRef entry, boolean hovered) {
        if (hovered) {
            this.hoveredEntry = entry;
            this.activeInfoEntry = entry;
        }
    }

    void setActiveDropdownOptionInfo(EntryRef entry, List<InfoPanelItem> info) {
        if (entry == null || info == null || info.isEmpty()) {
            this.activeDropdownOptionEntry = null;
            this.activeDropdownOptionInfo = Collections.emptyList();
            return;
        }
        this.activeDropdownOptionEntry = entry;
        this.activeDropdownOptionInfo = info;
    }

    List<InfoPanelItem> activeItems(DropdownSelectionInfo dropdownSelectionInfo) {
        if (this.activeDropdownOptionEntry != null && !this.activeDropdownOptionInfo.isEmpty()) {
            return this.activeDropdownOptionInfo;
        }

        EntryRef hovered = this.hoveredEntry;
        if (hovered == null && (this.mouseOverPanel || this.mouseOverBridge)) {
            hovered = this.activeInfoEntry;
        }
        if (hovered != null) {
            List<InfoPanelItem> selectedDropdownInfo = dropdownSelectionInfo.selectedInfo(hovered);
            if (!selectedDropdownInfo.isEmpty()) {
                return selectedDropdownInfo;
            }
            List<InfoPanelItem> entryInfo = hovered.handle.entryInfo(hovered.value.path());
            if (!entryInfo.isEmpty()) {
                return entryInfo;
            }
            if (!KonfigScreenSupport.isBlank(hovered.categoryPath)) {
                List<InfoPanelItem> categoryInfo = hovered.handle.categoryInfo(hovered.categoryPath);
                if (!categoryInfo.isEmpty()) {
                    return categoryInfo;
                }
            }
            List<InfoPanelItem> globalInfo = hovered.handle.globalInfo();
            if (!globalInfo.isEmpty()) {
                return globalInfo;
            }
        }

        for (EntryRef entry : this.entries) {
            List<InfoPanelItem> globalInfo = entry.handle.globalInfo();
            if (!globalInfo.isEmpty()) {
                return globalInfo;
            }
        }
        return Collections.emptyList();
    }

    void clearContent() {
        this.setRenderedItems(Collections.emptyList());
        this.scroll = 0.0D;
        this.maxScroll = 0;
    }

    void updateContent(List<InfoPanelItem> items, int contentHeight, int viewportHeight) {
        this.setRenderedItems(items);
        this.maxScroll = Math.max(0, contentHeight - Math.max(1, viewportHeight));
        this.scroll = Mth.clamp(this.scroll, 0.0D, (double) this.maxScroll);
        this.rememberScroll();
    }

    double scroll() {
        return this.scroll;
    }

    int maxScroll() {
        return this.maxScroll;
    }

    boolean handleScroll(KonfigInfoPanelBounds bounds, double mouseX, double mouseY, double scrollY) {
        if (!bounds.containsPanel(mouseX, mouseY) || this.maxScroll <= 0) {
            return false;
        }
        this.scroll = Mth.clamp(this.scroll - (scrollY * SCROLL_STEP), 0.0D, (double) this.maxScroll);
        this.rememberScroll();
        return true;
    }

    String clickedLink(KonfigInfoPanelBounds bounds, double mouseX, double mouseY) {
        if (!bounds.containsPanel(mouseX, mouseY)) {
            return null;
        }
        for (Link link : this.links) {
            if (link.contains(mouseX, mouseY)) {
                return link.target;
            }
        }
        return null;
    }

    void addLink(int x, int y, int width, int height, String target) {
        this.links.add(new Link(x, y, width, height, target));
    }

    private void setRenderedItems(List<InfoPanelItem> items) {
        if (items == this.renderedItems) {
            return;
        }
        this.rememberScroll();
        this.renderedItems = items;
        Double rememberedScroll = this.scrollPositions.get(items);
        this.scroll = rememberedScroll == null ? 0.0D : rememberedScroll.doubleValue();
    }

    private void rememberScroll() {
        if (!this.renderedItems.isEmpty()) {
            this.scrollPositions.put(this.renderedItems, this.scroll);
        }
    }

    interface DropdownSelectionInfo {
        List<InfoPanelItem> selectedInfo(EntryRef entry);
    }

    private static final class Link {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String target;

        private Link(int x, int y, int width, int height, String target) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.target = target;
        }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= this.x
                    && mouseX <= this.x + this.width
                    && mouseY >= this.y
                    && mouseY <= this.y + this.height;
        }
    }
}
//?}
