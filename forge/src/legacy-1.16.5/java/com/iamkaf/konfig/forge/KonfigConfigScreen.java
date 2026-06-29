package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.bootstrap.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyConfigEntries;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyConfigEntry;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyDraftSession;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyDropdownState;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyInfoPanelState;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyRegistrySuggestions;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyStringListState;
import com.iamkaf.konfig.impl.v1.client.legacy.LegacyValueText;
import com.iamkaf.konfig.impl.v1.config.model.ColorValueHelper;
import com.iamkaf.konfig.impl.v1.config.model.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import com.iamkaf.konfig.impl.v1.config.model.DropdownOptionMetadata;
import com.iamkaf.konfig.impl.v1.config.model.EntryKind;
import com.iamkaf.konfig.impl.v1.config.model.InfoPanelItem;
import com.iamkaf.konfig.impl.v1.client.toast.KonfigToastSupport;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Block;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.net.URI;

@ApiStatus.Internal
// Forge 1.16.5 legacy config screen. This shell adapts MCP names, MatrixStack,
// and Forge widgets to the shared common client.legacy state modules.
public final class KonfigConfigScreen extends Screen {
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_TAB = 258;
    private static final int KEY_SPACE = 32;
    private static final int KEY_DOWN = 264;
    private static final int KEY_UP = 265;
    private static final int KEY_KP_ENTER = 335;

    private static final int LIST_TOP = 28;
    private static final int LIST_BOTTOM_MARGIN = 52;
    private static final int ROW_HEIGHT = 34;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_MIN_WIDTH = 132;
    private static final int CONTROL_MAX_WIDTH = 200;
    private static final int VALIDATION_COLOR = 0xFFFF8080;
    private static final int URL_BUTTON_WIDTH = 60;
    private static final int SUGGESTION_LIMIT = 7;
    private static final int SUGGESTION_ROW_HEIGHT = 14;
    private static final int DROPDOWN_CHEVRON_WIDTH = 16;
    private static final long DROPDOWN_TYPE_SELECT_RESET_MS = 1000L;
    private static final int INFO_PANEL_MIN_WIDTH = 140;
    private static final int INFO_PANEL_MAX_WIDTH = 210;
    private static final int INFO_PANEL_PADDING = 16;
    private static final int INFO_PANEL_GAP = 10;
    private static final int INFO_PANEL_IMAGE_MAX_WIDTH = 168;
    private static final int INFO_PANEL_SCROLLBAR_WIDTH = 4;
    private static final int INFO_PANEL_SCROLL_STEP = 18;

    private static final LegacyValueText.Translator<ITextComponent> VALUE_TEXT_TRANSLATOR = new LegacyValueText.Translator<ITextComponent>() {
        @Override
        public ITextComponent literal(String value) {
            return text(value);
        }

        @Override
        public ITextComponent translate(String key, Object... args) {
            return KonfigConfigScreen.translate(key, args);
        }

        @Override
        public ITextComponent translationOrNull(String key) {
            return KonfigConfigScreen.translationOrNull(key);
        }

        @Override
        public String string(ITextComponent value) {
            return value.getString();
        }
    };

    private final Screen parent;
    private final String modIdFilter;
    private final String screenTitle;
    private final List<LegacyConfigEntry> legacyEntries;
    private final List<EntryRef> entries;
    private final LegacyDraftSession draftSession;
    private final Map<String, List<String>> registrySuggestionCache = new LinkedHashMap<String, List<String>>();

    private EntryList list;
    private RegistryTextInputRow activeRegistryRow;
    private RegistryTextInputRow renderedRegistryRow;
    private DropdownRow activeDropdownRow;
    private DropdownRow renderedDropdownRow;
    private EntryRef hoveredEntry;
    private final LegacyInfoPanelState infoPanelState = new LegacyInfoPanelState();
    private boolean mouseOverInfoPanel;
    private boolean mouseOverInfoPanelBridge;
    private final List<InfoPanelLink> infoPanelLinks = new ArrayList<InfoPanelLink>();
    private List pendingTooltipLines;
    private int pendingTooltipMouseX;
    private int pendingTooltipMouseY;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        this(parent, modIdFilter, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter, String screenTitle) {
        super(defaultScreenTitle(modIdFilter, screenTitle));
        this.parent = parent;
        this.modIdFilter = modIdFilter;
        this.screenTitle = screenTitle;
        this.legacyEntries = LegacyConfigEntries.collect(modIdFilter);
        this.entries = collectEntries(this.legacyEntries);
        this.draftSession = new LegacyDraftSession(this.legacyEntries);
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] creating screen parent={} modFilter={} entries={}",
                    parent == null ? "null" : parent.getClass().getName(),
                    modIdFilter == null ? "<all>" : modIdFilter,
                    this.entries.size()
            );
        }
    }

    @Override
    protected void init() {
        this.rebuildScreenWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.list == null) {
            return;
        }
        for (ConfigRow row : this.list.children()) {
            row.tick();
        }
    }

    @Override
    public void onClose() {
        this.closeScreen();
    }

    private void closeScreen() {
        this.minecraft.setScreen(this.parent);
    }

    private void openInlineUrl(EntryRef entry) {
        String target = entry.value.inlineUrl();
        if (isBlank(target)) {
            KonfigToastSupport.missingUrl();
            return;
        }

        try {
            Util.getPlatform().openUri(URI.create(target));
        } catch (Exception exception) {
            Constants.LOG.warn("Failed to open inline URL {}", target, exception);
            KonfigToastSupport.openFailed(target);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && this.handleInfoPanelClick(mouseX, mouseY)) {
            return true;
        }
        if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionClick(mouseX, mouseY)) {
            return true;
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (this.activeDropdownRow != null
                && !this.activeDropdownRow.isPointInsideButton(mouseX, mouseY)
                && !this.activeDropdownRow.isPointInsideDropdown(mouseX, mouseY)) {
            this.activeDropdownRow.closeDropdown();
        }
        RegistryTextInputRow focusedRow = this.findFocusedRegistryRow();
        if (focusedRow != null) {
            this.setActiveRegistryRow(focusedRow);
            focusedRow.activateSuggestions();
        } else if (this.activeRegistryRow != null && !this.activeRegistryRow.isPointInsideInput(mouseX, mouseY)) {
            this.activeRegistryRow.closeSuggestions();
        }

        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownKey(keyCode)) {
            return true;
        }
        DropdownRow focusedDropdown = this.findFocusedDropdownRow();
        if (focusedDropdown != null && focusedDropdown.handleClosedDropdownKey(keyCode)) {
            return true;
        }
        if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionKey(keyCode)) {
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (this.activeRegistryRow != null && this.activeRegistryRow.isFocused()) {
            this.activeRegistryRow.refreshSuggestions();
        }
        return handled;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownChar(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownScroll(mouseX, mouseY, delta)) {
            return true;
        }
        if (this.handleInfoPanelScroll(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderedRegistryRow = null;
        this.renderedDropdownRow = null;
        this.hoveredEntry = null;
        this.pendingTooltipLines = null;
        this.mouseOverInfoPanel = this.isPointInInfoPanel(mouseX, mouseY);
        this.mouseOverInfoPanelBridge = this.isPointInInfoPanelBridge(mouseX, mouseY);
        this.infoPanelLinks.clear();
        AbstractGui.fill(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
        if (this.list != null) {
            this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        AbstractGui.fill(guiGraphics, 0, this.height - LIST_BOTTOM_MARGIN, this.mainPanelRight(), this.height, 0xC0101010);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawCenteredString(guiGraphics, this.font, screenTitle(), this.width / 2, 8, 0xFFFFFFFF);
        AbstractGui.fill(guiGraphics, this.mainPanelRight(), LIST_TOP, this.mainPanelRight() + 1, this.height, 0xFF202020);
        this.renderInfoPanel(guiGraphics, mouseX, mouseY);

        if (this.entries.isEmpty()) {
            drawCenteredString(guiGraphics, this.font, translate("konfig.screen.empty"), this.mainPanelRight() / 2, this.height / 2 - 10, 0xFFC0C0C0);
        }

        // Painter order matters here: side panels first, floating controls next, queued tooltips last.
        guiGraphics.pushPose();
        guiGraphics.translate(0.0D, 0.0D, 300.0D);
        try {
            if (this.renderedRegistryRow != null) {
                this.renderedRegistryRow.renderSuggestions(guiGraphics, mouseX, mouseY);
            }
            if (this.renderedDropdownRow != null) {
                this.renderedDropdownRow.renderDropdown(guiGraphics, mouseX, mouseY);
            }
            guiGraphics.translate(0.0D, 0.0D, 100.0D);
            this.renderPendingTooltip(guiGraphics);
        } finally {
            guiGraphics.popPose();
        }
    }

    private void queueTooltip(MatrixStack guiGraphics, List lines, int mouseX, int mouseY) {
        this.pendingTooltipLines = lines;
        this.pendingTooltipMouseX = mouseX;
        this.pendingTooltipMouseY = mouseY;
    }

    private void queueTooltip(MatrixStack guiGraphics, String tooltip, int mouseX, int mouseY) {
        if (isBlank(tooltip)) {
            return;
        }
        this.queueTooltip(
                guiGraphics,
                this.font.split(text(tooltip), Math.max(this.width / 2, 200)),
                mouseX,
                mouseY
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void renderPendingTooltip(MatrixStack guiGraphics) {
        if (this.pendingTooltipLines == null || this.pendingTooltipLines.isEmpty()) {
            return;
        }
        super.renderTooltip(guiGraphics, this.pendingTooltipLines, this.pendingTooltipMouseX, this.pendingTooltipMouseY);
    }

    private void rebuildScreenWidgets() {
        this.buttons.clear();
        this.children.clear();

        int listHeight = Math.max(48, this.height - LIST_TOP - LIST_BOTTOM_MARGIN);
        this.list = new EntryList(this.minecraft, this.mainPanelRight(), listHeight, LIST_TOP);
        this.children.add(this.list);
        for (EntryRef entry : this.entries) {
            this.list.addKonfigEntry(createRow(entry));
        }

        int footerY = this.height - 26;
        int footerCenter = this.mainPanelRight() / 2;
        this.addButton(new Button(footerCenter - 82, footerY, 80, 20, translate("konfig.screen.reset"), button -> this.resetEntries()));
        this.addButton(new Button(footerCenter + 2, footerY, 80, 20, translate("konfig.screen.done"), button -> this.onClose()));
    }

    private ConfigRow createRow(EntryRef entry) {
        if (entry.value.kind() == EntryKind.HEADER) {
            return new HeaderRow(entry);
        }
        if (entry.value.kind() == EntryKind.IMAGE) {
            return new ImageRow(entry);
        }
        if (entry.value.kind() == EntryKind.INLINE_TEXT) {
            return new InlineTextRow(entry);
        }
        if (entry.value.kind() == EntryKind.URL) {
            return new UrlRow(entry);
        }
        if (!entry.editable) {
            return new UnsupportedRow(entry);
        }
        if (entry.value.kind() == EntryKind.BOOLEAN) {
            return new BooleanRow(entry);
        }
        if (entry.value.kind() == EntryKind.ENUM) {
            return new EnumRow(entry);
        }
        if (entry.value.kind() == EntryKind.COLOR_RGB || entry.value.kind() == EntryKind.COLOR_ARGB) {
            return new ColorRow(entry);
        }
        if (entry.value.kind() == EntryKind.STRING_LIST) {
            return new StringListRow(entry);
        }
        if (entry.value.kind() == EntryKind.DROPDOWN) {
            return new DropdownRow(entry);
        }
        if (entry.value.kind() == EntryKind.INTEGER && entry.value.hasNumericRange()) {
            return new IntegerSliderRow(entry);
        }
        if (entry.value.kind() == EntryKind.LONG && entry.value.hasNumericRange()) {
            return new LongSliderRow(entry);
        }
        if (entry.value.kind() == EntryKind.DOUBLE && entry.value.hasNumericRange()) {
            return new DoubleSliderRow(entry);
        }
        if (entry.value.kind() == EntryKind.STRING && entry.value.hasBoundRegistry()) {
            return new RegistryTextInputRow(entry);
        }
        return new TextInputRow(entry);
    }

    private boolean persistEntry(EntryRef entry) {
        return this.draftSession.persist(entry.legacyEntry);
    }

    private void resetEntries() {
        this.draftSession.reset(this.legacyEntries);
        this.rebuildScreenWidgets();
    }

    private static ITextComponent defaultScreenTitle(String modIdFilter, String screenTitle) {
        return LegacyValueText.defaultScreenTitle(modIdFilter, screenTitle, VALUE_TEXT_TRANSLATOR);
    }

    private static ITextComponent translatedModTitle(String modId) {
        return LegacyValueText.translatedModTitle(modId, VALUE_TEXT_TRANSLATOR);
    }

    private ITextComponent screenTitle() {
        if (!isBlank(this.screenTitle)) {
            return text(this.screenTitle);
        }
        return this.title;
    }

    private int mainPanelRight() {
        int infoPanelWidth = MathHelper.clamp(this.width / 3, INFO_PANEL_MIN_WIDTH, INFO_PANEL_MAX_WIDTH);
        int mainRight = this.width - infoPanelWidth;
        return Math.max(220, mainRight);
    }

    private void updateHoveredEntry(EntryRef entry, boolean hovered) {
        if (hovered) {
            this.hoveredEntry = entry;
            this.infoPanelState.hover(entry.legacyEntry);
        }
    }

    private List<InfoPanelItem> activeInfoItems() {
        return this.infoPanelState.activeItems(
                this.legacyEntries,
                this.hoveredEntry == null ? null : this.hoveredEntry.legacyEntry,
                this.mouseOverInfoPanel || this.mouseOverInfoPanelBridge,
                new LegacyInfoPanelState.DropdownInfoProvider() {
                    @Override
                    public List<InfoPanelItem> selectedDropdownInfo(LegacyConfigEntry entry) {
                        return KonfigConfigScreen.this.selectedDropdownOptionInfo(entry);
                    }
                }
        );
    }

    private void updateActiveDropdownOptionInfo(int mouseX, int mouseY) {
        this.infoPanelState.clearDropdownOption();
        if (this.activeDropdownRow == null) {
            return;
        }

        DropdownOptionMetadata option = this.activeDropdownRow.activeInfoOption(mouseX, mouseY);
        if (option == null || option.info().isEmpty()) {
            return;
        }

        this.infoPanelState.dropdownOption(this.activeDropdownRow.entry.legacyEntry, option.info());
    }

    private List<InfoPanelItem> selectedDropdownOptionInfo(LegacyConfigEntry entry) {
        if (entry.value().kind() != EntryKind.DROPDOWN) {
            return Collections.emptyList();
        }
        if (this.activeDropdownRow != null && this.activeDropdownRow.entry.legacyEntry == entry) {
            return Collections.emptyList();
        }

        DropdownOptionMetadata option = entry.value().dropdownOption(this.currentDropdownValue(entry.value()));
        return option == null ? Collections.emptyList() : option.info();
    }

    private boolean isPointInInfoPanel(double mouseX, double mouseY) {
        return mouseX >= this.mainPanelRight() + 1
                && mouseX <= this.width
                && mouseY >= LIST_TOP
                && mouseY <= this.height;
    }

    private boolean isPointInInfoPanelBridge(double mouseX, double mouseY) {
        return mouseX >= this.mainPanelRight() - 24
                && mouseX <= this.mainPanelRight() + 1
                && mouseY >= LIST_TOP
                && mouseY <= this.height - LIST_BOTTOM_MARGIN;
    }

    private boolean handleInfoPanelClick(double mouseX, double mouseY) {
        if (!this.isPointInInfoPanel(mouseX, mouseY)) {
            return false;
        }
        for (InfoPanelLink link : this.infoPanelLinks) {
            if (link.contains(mouseX, mouseY)) {
                try {
                    Util.getPlatform().openUri(URI.create(link.target));
                } catch (Exception exception) {
                    Constants.LOG.warn("Failed to open info panel URL {}", link.target, exception);
                    KonfigToastSupport.openFailed(link.target);
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleInfoPanelScroll(double mouseX, double mouseY, double scrollY) {
        if (!this.isPointInInfoPanel(mouseX, mouseY)) {
            return false;
        }
        return this.infoPanelState.scrollBy(scrollY, INFO_PANEL_SCROLL_STEP);
    }

    private void renderInfoPanel(MatrixStack guiGraphics, int mouseX, int mouseY) {
        int left = this.mainPanelRight() + 1;
        int right = this.width;
        int top = LIST_TOP;
        int bottom = this.height;
        AbstractGui.fill(guiGraphics, left, top, right, bottom, 0x22000000);

        this.updateActiveDropdownOptionInfo(mouseX, mouseY);
        List<InfoPanelItem> items = this.activeInfoItems();
        if (items.isEmpty()) {
            this.infoPanelState.clearRenderedItems();
            return;
        }

        this.infoPanelState.setRenderedItems(items);

        int x = left + INFO_PANEL_PADDING;
        int viewportTop = top + INFO_PANEL_PADDING;
        int viewportBottom = bottom - INFO_PANEL_PADDING;
        int contentWidth = Math.max(20, right - left - (INFO_PANEL_PADDING * 2) - INFO_PANEL_SCROLLBAR_WIDTH - 4);
        int contentHeight = this.measureInfoPanelItems(items, contentWidth);
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        this.infoPanelState.maxScroll(contentHeight - viewportHeight);

        int y = viewportTop - (int) Math.round(this.infoPanelState.scroll());
        this.enableInfoPanelScissor(left, viewportTop, right, viewportBottom);
        for (InfoPanelItem item : items) {
            y = this.renderInfoPanelItem(guiGraphics, item, x, y, contentWidth, mouseX, mouseY);
        }
        this.disableInfoPanelScissor();

        this.renderInfoPanelScrollbar(guiGraphics, right, viewportTop, viewportBottom);
    }

    private int renderInfoPanelItem(MatrixStack guiGraphics, InfoPanelItem item, int x, int y, int width, int mouseX, int mouseY) {
        if (item.kind == EntryKind.HEADER) {
            this.font.draw(guiGraphics, infoLabel(item), x, y, 0xFFFFFFFF);
            return y + 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.renderInfoImage(guiGraphics, item, x, y, width);
        }
        if (item.kind == EntryKind.URL) {
            ITextComponent label = text(infoText(item) + " >");
            int linkWidth = this.font.width(label);
            InfoPanelLink link = new InfoPanelLink(x, y, Math.min(width, linkWidth), this.font.lineHeight, item.target);
            this.infoPanelLinks.add(link);
            boolean hovered = link.contains(mouseX, mouseY);
            this.font.draw(guiGraphics, label, x, y, hovered ? 0xFFFFFFFF : 0xFF80C8FF);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y + this.font.lineHeight, x + Math.min(width, linkWidth), y + this.font.lineHeight + 1, 0xFFFFFFFF);
            }
            return y + 16;
        }
        return this.renderInfoParagraph(guiGraphics, infoText(item), x, y, width, 0xFFCFCFCF) + INFO_PANEL_GAP;
    }

    private int renderInfoImage(MatrixStack guiGraphics, InfoPanelItem item, int x, int y, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(Math.min(options.width(), INFO_PANEL_IMAGE_MAX_WIDTH), width - (options.padding() * 2)));
        int imageHeight = Math.max(1, (int) Math.round(options.height() * ((double) imageWidth / (double) options.width())));
        int imageX = x + options.padding();
        if (options.align() == ImageOptions.Align.CENTER) {
            imageX = x + Math.max(options.padding(), (width - imageWidth) / 2);
        } else if (options.align() == ImageOptions.Align.RIGHT) {
            imageX = x + Math.max(options.padding(), width - options.padding() - imageWidth);
        }
        drawImage(guiGraphics, item.target, imageX, y + options.padding(), imageWidth, imageHeight, options.width(), options.height());
        y += imageHeight + (options.padding() * 2);
        if (!isBlank(infoText(item)) && options.captionPosition() != ImageOptions.CaptionPosition.NONE) {
            y = this.renderInfoParagraph(guiGraphics, infoText(item), x, y, width, 0xFFCFCFCF);
        }
        return y + INFO_PANEL_GAP;
    }

    private static ITextComponent infoLabel(InfoPanelItem item) {
        return item.labelTranslationKey ? translate(item.label) : text(item.label);
    }

    private static String infoText(InfoPanelItem item) {
        return infoLabel(item).getString();
    }

    private int measureInfoPanelItems(List<InfoPanelItem> items, int width) {
        int height = 0;
        for (InfoPanelItem item : items) {
            height += this.measureInfoPanelItem(item, width);
        }
        return height;
    }

    private int measureInfoPanelItem(InfoPanelItem item, int width) {
        if (item.kind == EntryKind.HEADER || item.kind == EntryKind.URL) {
            return 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.measureInfoImage(item, width);
        }
        return this.measureInfoParagraph(infoText(item), width) + INFO_PANEL_GAP;
    }

    private int measureInfoImage(InfoPanelItem item, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(Math.min(options.width(), INFO_PANEL_IMAGE_MAX_WIDTH), width - (options.padding() * 2)));
        int imageHeight = Math.max(1, (int) Math.round(options.height() * ((double) imageWidth / (double) options.width())));
        int height = imageHeight + (options.padding() * 2);
        if (!isBlank(infoText(item)) && options.captionPosition() != ImageOptions.CaptionPosition.NONE) {
            height += this.measureInfoParagraph(infoText(item), width);
        }
        return height + INFO_PANEL_GAP;
    }

    private int measureInfoParagraph(String value, int width) {
        int height = 0;
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                height += 8;
                continue;
            }
            height += this.wrapLines(paragraph.trim(), width).size() * this.font.lineHeight;
            height += 4;
        }
        return height;
    }

    private void renderInfoPanelScrollbar(MatrixStack guiGraphics, int right, int top, int bottom) {
        if (this.infoPanelState.maxScroll() <= 0) {
            return;
        }

        int trackLeft = right - INFO_PANEL_SCROLLBAR_WIDTH - 4;
        int trackRight = right - 4;
        int viewportHeight = Math.max(1, bottom - top);
        int contentHeight = viewportHeight + this.infoPanelState.maxScroll();
        int thumbHeight = MathHelper.clamp((viewportHeight * viewportHeight) / contentHeight, 18, viewportHeight);
        int thumbTop = top + (int) Math.round((viewportHeight - thumbHeight) * (this.infoPanelState.scroll() / (double) this.infoPanelState.maxScroll()));
        AbstractGui.fill(guiGraphics, trackLeft, top, trackRight, bottom, 0x44000000);
        AbstractGui.fill(guiGraphics, trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0xAAFFFFFF);
    }

    private void enableInfoPanelScissor(int left, int top, int right, int bottom) {
        double scale = this.minecraft.getWindow().getGuiScale();
        int scissorX = (int) Math.round(left * scale);
        int scissorY = (int) Math.round(this.minecraft.getWindow().getHeight() - (bottom * scale));
        int scissorWidth = Math.max(0, (int) Math.round((right - left) * scale));
        int scissorHeight = Math.max(0, (int) Math.round((bottom - top) * scale));
        RenderSystem.enableScissor(scissorX, scissorY, scissorWidth, scissorHeight);
    }

    private void disableInfoPanelScissor() {
        RenderSystem.disableScissor();
    }

    private int renderInfoParagraph(MatrixStack guiGraphics, String value, int x, int y, int width, int color) {
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                y += 8;
                continue;
            }
            y = this.renderWrappedLines(guiGraphics, paragraph.trim(), x, y, width, color) + 4;
        }
        return y;
    }

    private int renderWrappedLines(MatrixStack guiGraphics, String value, int x, int y, int width, int color) {
        for (String line : this.wrapLines(value, width)) {
            this.font.draw(guiGraphics, line, (float) x, (float) y, color);
            y += this.font.lineHeight;
        }
        return y;
    }

    private List<String> wrapLines(String value, int width) {
        List<String> lines = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String word : value.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (this.font.width(candidate) <= width) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }
            while (this.font.width(word) > width && word.length() > 1) {
                int split = word.length();
                while (split > 1 && this.font.width(word.substring(0, split)) > width) {
                    split--;
                }
                lines.add(word.substring(0, split));
                word = word.substring(split);
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static final class InfoPanelLink {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final String target;

        private InfoPanelLink(int x, int y, int width, int height, String target) {
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

    private static List<EntryRef> collectEntries(List<LegacyConfigEntry> legacyEntries) {
        List<EntryRef> result = new ArrayList<EntryRef>();

        for (LegacyConfigEntry entry : legacyEntries) {
            result.add(new EntryRef(entry));
        }

        return result;
    }

    private static String stringValue(Object value) {
        return LegacyDraftSession.stringValue(value);
    }

    private boolean readBoolean(ConfigValueImpl<?> value) {
        return this.draftSession.readBoolean(value);
    }

    private Enum<?> currentEnum(ConfigValueImpl<?> value) {
        return this.draftSession.currentEnum(value);
    }

    private Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        return this.draftSession.cycleEnum(value);
    }

    private int currentColor(ConfigValueImpl<?> value) {
        return this.draftSession.currentColor(value);
    }

    private List<String> currentStringList(ConfigValueImpl<?> value) {
        return LegacyStringListState.current(this.draftSession, value);
    }

    private String currentDropdownValue(ConfigValueImpl<?> value) {
        return this.draftSession.currentDropdownValue(value);
    }

    private ITextComponent booleanText(ConfigValueImpl<?> value) {
        return LegacyValueText.booleanText(readBoolean(value), VALUE_TEXT_TRANSLATOR);
    }

    private ITextComponent enumText(EntryRef entry, Enum<?> value) {
        return LegacyValueText.enumText(entry.legacyEntry, value, VALUE_TEXT_TRANSLATOR);
    }

    private ITextComponent dropdownText(EntryRef entry, String option) {
        return LegacyValueText.dropdownText(entry.legacyEntry, option, VALUE_TEXT_TRANSLATOR);
    }

    private ITextComponent dropdownValueText(EntryRef entry, String option) {
        return LegacyValueText.dropdownValueText(entry.legacyEntry, option, VALUE_TEXT_TRANSLATOR);
    }

    private ITextComponent translatedDropdownOption(EntryRef entry, DropdownOptionMetadata option) {
        return LegacyValueText.translatedDropdownOption(entry.legacyEntry, option, VALUE_TEXT_TRANSLATOR);
    }

    private String translatedDropdownTooltip(DropdownOptionMetadata option) {
        return LegacyValueText.translatedDropdownTooltip(option, VALUE_TEXT_TRANSLATOR);
    }

    private ITextComponent colorText(ConfigValueImpl<?> value) {
        return text(LegacyValueText.colorText(value, currentColor(value)));
    }

    private ITextComponent stringListText(ConfigValueImpl<?> value) {
        return LegacyValueText.stringListText(currentStringList(value), VALUE_TEXT_TRANSLATOR);
    }

    private String currentStringValue(ConfigValueImpl<?> value) {
        Object current = this.draftSession.draft(value);
        if (current instanceof String) {
            return (String) current;
        }
        return stringValue(value.get());
    }

    private static double progressFor(double current, double min, double max) {
        double span = max - min;
        if (span <= 0.0D) {
            return 0.0D;
        }
        return MathHelper.clamp((current - min) / span, 0.0D, 1.0D);
    }

    private static int intFromProgress(double progress, int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + (int) Math.round((max - min) * progress);
    }

    private static long longFromProgress(double progress, long min, long max) {
        if (max <= min) {
            return min;
        }
        return min + Math.round((max - min) * progress);
    }

    private static double doubleFromProgress(double progress, double min, double max) {
        if (max <= min) {
            return min;
        }
        return min + (max - min) * progress;
    }

    private static String formatDouble(double value) {
        String formatted = String.format(Locale.ROOT, "%.3f", value);
        while (formatted.contains(".") && (formatted.endsWith("0") || formatted.endsWith("."))) {
            formatted = formatted.substring(0, formatted.length() - 1);
        }
        return formatted;
    }

    private static void drawColorSwatch(MatrixStack guiGraphics, int x, int y, int size, int color, EntryKind kind) {
        AbstractGui.fill(guiGraphics, x - 1, y - 1, x + size + 1, y + size + 1, 0xFF202020);
        if (kind == EntryKind.COLOR_ARGB && ColorValueHelper.alpha(color) < 255) {
            int cell = Math.max(2, size / 4);
            for (int row = 0; row < size; row += cell) {
                for (int column = 0; column < size; column += cell) {
                    boolean dark = ((row / cell) + (column / cell)) % 2 == 0;
                    AbstractGui.fill(guiGraphics, 
                            x + column,
                            y + row,
                            x + Math.min(size, column + cell),
                            y + Math.min(size, row + cell),
                            dark ? 0xFF707070 : 0xFFC0C0C0
                    );
                }
            }
        } else {
            AbstractGui.fill(guiGraphics, x, y, x + size, y + size, 0xFFFFFFFF);
        }
        AbstractGui.fill(guiGraphics, x, y, x + size, y + size, ColorValueHelper.toRenderColor(kind, color));
    }

    private static ResourceLocation parseIdentifier(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return ResourceLocation.tryParse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ResourceLocation textureIdentifier(String value) {
        ResourceLocation identifier = parseIdentifier(value);
        if (identifier == null) {
            return parseIdentifier("minecraft:textures/missingno.png");
        }

        String path = identifier.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return new ResourceLocation(identifier.getNamespace(), path);
    }

    private static void drawImage(MatrixStack guiGraphics, String target, int x, int y, int width, int height) {
        drawImage(guiGraphics, target, x, y, width, height, width, height);
    }

    private static void drawImage(MatrixStack guiGraphics, String target, int x, int y, int width, int height, int sourceWidth, int sourceHeight) {
        Minecraft.getInstance().getTextureManager().bind(textureIdentifier(target));
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        AbstractGui.blit(guiGraphics, x, y, width, height, 0.0F, 0.0F, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
    }

    private static boolean supportsRegistryIcon(String registryId) {
        return "minecraft:item".equals(registryId) || "minecraft:block".equals(registryId);
    }

    private static ItemStack registryIconStack(String registryId, String value) {
        if (!supportsRegistryIcon(registryId)) {
            return ItemStack.EMPTY;
        }

        ResourceLocation identifier = parseIdentifier(value);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }

        if ("minecraft:item".equals(registryId)) {
            Item item = Registry.ITEM.get(identifier);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
            return ItemStack.EMPTY;
        }

        Block block = Registry.BLOCK.get(identifier);
        if (block == null) {
            return ItemStack.EMPTY;
        }

        Item item = block.asItem();
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static void renderRegistryIcon(MatrixStack guiGraphics, String registryId, String value, int x, int y) {
        ItemStack stack = registryIconStack(registryId, value);
        if (!stack.isEmpty()) {
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
        }
    }

    private static String normalizeHexInput(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean isHexPrefix(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static ITextComponent translate(String key, Object... args) {
        return new TranslationTextComponent(key, args);
    }

    private static ITextComponent text(String value) {
        return new StringTextComponent(value == null ? "" : value);
    }

    private static ITextComponent translatedLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        String key = "konfig.config." + handle.modId() + "." + handle.name() + "." + value.path();
        ITextComponent translated = translationOrNull(key);
        if (translated != null) {
            return translated;
        }

        String legacyKey = handle.modId() + ".config." + LegacyValueText.lastPathSegment(value.path());
        translated = translationOrNull(legacyKey);
        return translated == null ? text(LegacyValueText.fallbackLabel(handle, value)) : translated;
    }

    private static ITextComponent translationOrNull(String key) {
        ITextComponent translated = translate(key);
        return key.equals(translated.getString()) ? null : translated;
    }

    private static String lastPathSegment(String path) {
        return LegacyValueText.lastPathSegment(path);
    }

    private static ITextComponent contextLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        return text(LegacyValueText.contextLabel(handle, value));
    }

    private static String fallbackLabel(ConfigHandleImpl handle, ConfigValueImpl<?> value) {
        return LegacyValueText.fallbackLabel(handle, value);
    }

    private static String prettySegment(String raw) {
        return LegacyValueText.prettySegment(raw);
    }

    private static List<ITextComponent> tooltipLines(String tooltip) {
        List<ITextComponent> lines = new ArrayList<ITextComponent>();
        String normalized = tooltip.replace('\r', '\n');
        for (String line : normalized.split("\\n")) {
            lines.add(text(line));
        }
        return lines;
    }

    private static boolean isBlank(String value) {
        return LegacyValueText.isBlank(value);
    }

    private RegistryTextInputRow findFocusedRegistryRow() {
        if (this.list == null) {
            return null;
        }
        for (ConfigRow row : this.list.children()) {
            if (row instanceof RegistryTextInputRow) {
                RegistryTextInputRow registryRow = (RegistryTextInputRow) row;
                if (registryRow.isFocused()) {
                    return registryRow;
                }
            }
        }
        return null;
    }

    private DropdownRow findFocusedDropdownRow() {
        if (this.list == null || this.activeDropdownRow != null) {
            return null;
        }
        for (ConfigRow row : this.list.children()) {
            if (row instanceof DropdownRow) {
                DropdownRow dropdownRow = (DropdownRow) row;
                if (dropdownRow.isButtonFocused()) {
                    return dropdownRow;
                }
            }
        }
        return null;
    }

    private void setActiveRegistryRow(RegistryTextInputRow row) {
        if (this.activeDropdownRow != null) {
            this.activeDropdownRow.closeDropdown();
        }
        if (this.activeRegistryRow == row) {
            return;
        }
        if (this.activeRegistryRow != null) {
            this.activeRegistryRow.closeSuggestions();
        }
        this.activeRegistryRow = row;
    }

    private void setActiveDropdownRow(DropdownRow row) {
        if (this.activeDropdownRow == row) {
            return;
        }
        if (this.activeRegistryRow != null) {
            this.activeRegistryRow.closeSuggestions();
        }
        if (this.activeDropdownRow != null) {
            this.activeDropdownRow.closeDropdown();
        }
        this.activeDropdownRow = row;
    }

    private List<String> registrySuggestions(String registryId) {
        if (isBlank(registryId)) {
            return Collections.emptyList();
        }
        List<String> cached = this.registrySuggestionCache.get(registryId);
        if (cached != null) {
            return cached;
        }

        List<String> values = new ArrayList<String>();
        Registry<?> registry = builtInRegistry(registryId);
        if (registry != null) {
            for (Object key : registry.keySet()) {
                values.add(String.valueOf(key));
            }
            Collections.sort(values);
        }

        List<String> immutable = Collections.unmodifiableList(values);
        this.registrySuggestionCache.put(registryId, immutable);
        return immutable;
    }

    @SuppressWarnings("unchecked")
    private static Registry<?> builtInRegistry(String registryId) {
        ResourceLocation identifier = parseIdentifier(registryId);
        if (identifier == null || !Registry.REGISTRY.containsKey(identifier)) {
            return null;
        }
        return (Registry<?>) Registry.REGISTRY.get(identifier);
    }

    private static List<String> filterRegistrySuggestions(List<String> allSuggestions, String query) {
        return LegacyRegistrySuggestions.filter(allSuggestions, query, SUGGESTION_LIMIT);
    }

    private static String suggestionSuffix(String currentValue, String suggestion) {
        return LegacyRegistrySuggestions.suffix(currentValue, suggestion);
    }

    private final class EntryList extends ExtendedList<ConfigRow> {
        private EntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, KonfigConfigScreen.this.height, y, y + height, ROW_HEIGHT);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
        }

        private void addKonfigEntry(ConfigRow row) {
            super.addEntry(row);
        }

        @Override
        public int getRowWidth() {
            return KonfigConfigScreen.this.mainPanelRight() - 28;
        }

        @Override
        protected int getScrollbarPosition() {
            return KonfigConfigScreen.this.mainPanelRight() - 6;
        }

        @Override
        protected void renderBackground(MatrixStack guiGraphics) {
            AbstractGui.fill(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
        }
    }

    private abstract class ConfigRow extends ExtendedList.AbstractListEntry<ConfigRow> implements INestedGuiEventHandler {
        protected final EntryRef entry;
        private IGuiEventListener focused;
        private boolean dragging;

        private ConfigRow(EntryRef entry) {
            this.entry = entry;
        }

        protected abstract Widget control();

        protected void tick() {
        }

        protected String validationMessage() {
            return "";
        }

        protected String rowTooltip() {
            return this.entry.tooltip;
        }

        @Override
        public void render(MatrixStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
        }

        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }

            String rowTooltip = this.rowTooltip();
            if (!isBlank(rowTooltip)) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    KonfigConfigScreen.this.queueTooltip(guiGraphics, rowTooltip, mouseX, mouseY);
                }
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int controlX = x + width - controlWidth;
            int controlY = y + (height - CONTROL_HEIGHT) / 2;
            layoutControl(this.control(), controlX, controlY, controlWidth);

            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.contextLabel, x + 4.0F, y + 1.0F, 0xFFA0A0A0);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
            this.control().render(guiGraphics, mouseX, mouseY, partialTick);
            if (!this.validationMessage().isEmpty()) {
                KonfigConfigScreen.this.font.draw(guiGraphics, text(this.validationMessage()), controlX, controlY + CONTROL_HEIGHT + 2, VALIDATION_COLOR);
            }
        }

        @Override
        public List<? extends IGuiEventListener> children() {
            return Collections.singletonList(this.control());
        }

        @Override
        public IGuiEventListener getFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(IGuiEventListener listener) {
            this.focused = listener;
        }

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        protected final void layoutControl(Widget control, int x, int y, int width) {
            control.x = x;
            control.y = y;
            control.setWidth(width);
        }

        protected void revertDraft(Object previousValue) {
            KonfigConfigScreen.this.draftSession.draft(this.entry.value, LegacyDraftSession.copyDraftValue(this.entry.value, previousValue));
        }

        protected void commitOrRevert(Object previousValue) {
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                this.revertDraft(previousValue);
                this.syncFromDraft();
            }
        }

        protected void syncFromDraft() {
        }
    }

    private final class UnsupportedRow extends ConfigRow {
        private final Button button;

        private UnsupportedRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, translate("konfig.screen.unsupported"), ignored -> {
            });
            this.button.active = false;
        }

        @Override
        protected Widget control() {
            return this.button;
        }
    }

    private abstract class DecorationRow extends ConfigRow {
        private final Button spacer;

        private DecorationRow(EntryRef entry) {
            super(entry);
            this.spacer = new Button(0, 0, 0, 0, new StringTextComponent(""), ignored -> {});
            this.spacer.visible = false;
            this.spacer.active = false;
        }

        @Override
        protected final Widget control() {
            return this.spacer;
        }

        @Override
        public List<? extends IGuiEventListener> children() {
            return Collections.emptyList();
        }
    }

    private final class HeaderRow extends DecorationRow {
        private HeaderRow(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (!isBlank(this.entry.tooltip) && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                KonfigConfigScreen.this.queueTooltip(
                        guiGraphics,
                        KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                        mouseX,
                        mouseY
                );
            }
            AbstractGui.fill(guiGraphics, x, y + 4, x + width, y + height - 4, 0x552B3550);
            drawCenteredString(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), x + (width / 2), y + 10, 0xFFF8E38F);
        }
    }

    private final class ImageRow extends DecorationRow {
        private ImageRow(EntryRef entry) {
            super(entry);
        }

        private boolean hasCaption() {
            return !isBlank(this.entry.value.inlineLabel()) && this.entry.value.imageOptions().captionPosition() != ImageOptions.CaptionPosition.NONE;
        }

        private int captionWidth() {
            return this.hasCaption() ? KonfigConfigScreen.this.font.width(this.entry.displayLabel()) : 0;
        }

        private int[] imageSize(int rowWidth, int rowHeight) {
            ImageOptions options = this.entry.value.imageOptions();
            int captionReserve = this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.RIGHT ? this.captionWidth() + 8 : 0;
            int maxWidth = Math.max(1, rowWidth - (options.padding() * 2) - captionReserve);
            int maxHeight = Math.max(1, rowHeight - (options.padding() * 2) - (this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.BELOW ? 10 : 0));
            double scale = Math.min(1.0D, Math.min((double) maxWidth / (double) options.width(), (double) maxHeight / (double) options.height()));
            return new int[] { Math.max(1, (int) Math.round(options.width() * scale)), Math.max(1, (int) Math.round(options.height() * scale)) };
        }

        private int contentWidth(int imageWidth) {
            ImageOptions options = this.entry.value.imageOptions();
            return this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.RIGHT ? imageWidth + 8 + this.captionWidth() : imageWidth;
        }

        private int contentHeight(int imageHeight) {
            ImageOptions options = this.entry.value.imageOptions();
            return this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.BELOW ? imageHeight + 12 : imageHeight;
        }

        private int imageX(int x, int width, int contentWidth) {
            ImageOptions options = this.entry.value.imageOptions();
            if (options.align() == ImageOptions.Align.CENTER) {
                return x + Math.max(options.padding(), (width - contentWidth) / 2);
            }
            if (options.align() == ImageOptions.Align.RIGHT) {
                return x + Math.max(options.padding(), width - options.padding() - contentWidth);
            }
            return x + options.padding();
        }

        private int imageY(int y, int height, int contentHeight) {
            return y + Math.max(0, (height - contentHeight) / 2);
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            if (!isBlank(this.entry.tooltip) && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                KonfigConfigScreen.this.queueTooltip(
                        guiGraphics,
                        KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                        mouseX,
                        mouseY
                );
            }
            int[] imageSize = imageSize(width, height);
            int contentWidth = contentWidth(imageSize[0]);
            int contentHeight = contentHeight(imageSize[1]);
            int imageX = imageX(x, width, contentWidth);
            int imageY = imageY(y, height, contentHeight);
            drawImage(guiGraphics, this.entry.value.inlineTarget(), imageX, imageY, imageSize[0], imageSize[1], this.entry.value.imageOptions().width(), this.entry.value.imageOptions().height());
            if (this.hasCaption()) {
                ImageOptions options = this.entry.value.imageOptions();
                if (options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                    KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), imageX + imageSize[0] + 8.0F, imageY + Math.max(0, (imageSize[1] - 8) / 2), 0xFFCFCFCF);
                } else if (options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                    KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), imageX + Math.max(0, (imageSize[0] - this.captionWidth()) / 2), imageY + imageSize[1] + 2.0F, 0xFFCFCFCF);
                }
            }
        }
    }

    private final class InlineTextRow extends DecorationRow {
        private InlineTextRow(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            if (!isBlank(this.entry.tooltip) && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                KonfigConfigScreen.this.queueTooltip(
                        guiGraphics,
                        KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                        mouseX,
                        mouseY
                );
            }
            int lineCount = KonfigConfigScreen.this.wrapLines(this.entry.displayLabel().getString(), Math.max(1, width - 16)).size();
            int textY = y + Math.max(4, (height - (lineCount * KonfigConfigScreen.this.font.lineHeight)) / 2);
            KonfigConfigScreen.this.renderWrappedLines(guiGraphics, this.entry.displayLabel().getString(), x + 8, textY, Math.max(1, width - 16), 0xFFCFCFCF);
        }
    }

    private final class UrlRow extends ConfigRow {
        private final Button button;

        private UrlRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, URL_BUTTON_WIDTH, CONTROL_HEIGHT, text("Open"), ignored -> KonfigConfigScreen.this.openInlineUrl(this.entry));
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }
            if (!isBlank(this.entry.tooltip) && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                KonfigConfigScreen.this.queueTooltip(
                        guiGraphics,
                        KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                        mouseX,
                        mouseY
                );
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int controlX = x + width - controlWidth;
            int controlY = y + (height - CONTROL_HEIGHT) / 2;
            layoutControl(this.control(), controlX, controlY, controlWidth);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, 0xFF80C8FF);
            this.control().render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private final class BooleanRow extends ConfigRow {
        private final Button button;

        private BooleanRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, booleanText(entry.value), button -> {
                Object previousDraft = KonfigConfigScreen.this.draftSession.draft(entry.value);
                KonfigConfigScreen.this.draftSession.draft(entry.value, Boolean.valueOf(!KonfigConfigScreen.this.readBoolean(entry.value)));
                this.commitOrRevert(previousDraft);
                this.syncFromDraft();
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(booleanText(this.entry.value));
        }
    }

    private final class EnumRow extends ConfigRow {
        private final Button button;

        private EnumRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, enumText(entry, KonfigConfigScreen.this.currentEnum(entry.value)), button -> {
                Object previousDraft = KonfigConfigScreen.this.draftSession.draft(entry.value);
                KonfigConfigScreen.this.draftSession.draft(entry.value, KonfigConfigScreen.this.cycleEnum(entry.value));
                this.commitOrRevert(previousDraft);
                this.syncFromDraft();
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(enumText(this.entry, KonfigConfigScreen.this.currentEnum(this.entry.value)));
        }
    }

    private final class DropdownRow extends ConfigRow {
        private final Button button;
        private final LegacyDropdownState dropdownState = new LegacyDropdownState();
        private int lastButtonX;
        private int lastButtonY;
        private int lastButtonWidth = CONTROL_MIN_WIDTH;
        private int lastDropdownX;
        private int lastDropdownY;
        private int lastDropdownWidth;
        private int lastDropdownHeight;

        private DropdownRow(EntryRef entry) {
            super(entry);
            this.button = new Button(
                    0,
                    0,
                    CONTROL_MIN_WIDTH,
                    CONTROL_HEIGHT,
                    text(""),
                    ignored -> this.toggleDropdown()
            );
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(text(""));
        }

        @Override
        protected String rowTooltip() {
            if (this.dropdownState.open()) {
                return this.entry.tooltip;
            }
            String optionTooltip = translatedDropdownTooltip(this.currentOption());
            return isBlank(optionTooltip) ? this.entry.tooltip : optionTooltip;
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
            this.lastButtonX = this.button.x;
            this.lastButtonY = this.button.y;
            this.lastButtonWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            this.renderButtonLabel(guiGraphics);
            if (this.dropdownState.open()) {
                KonfigConfigScreen.this.renderedDropdownRow = this;
            }
        }

        private void toggleDropdown() {
            if (this.dropdownState.open()) {
                this.closeDropdown();
            } else {
                this.openDropdown();
            }
        }

        private void openDropdown() {
            if (this.options().isEmpty()) {
                return;
            }
            this.dropdownState.open(this.options(), KonfigConfigScreen.this.currentDropdownValue(this.entry.value), SUGGESTION_LIMIT);
            KonfigConfigScreen.this.setActiveDropdownRow(this);
        }

        private void closeDropdown() {
            this.dropdownState.close();
            if (KonfigConfigScreen.this.activeDropdownRow == this) {
                KonfigConfigScreen.this.activeDropdownRow = null;
            }
            if (KonfigConfigScreen.this.renderedDropdownRow == this) {
                KonfigConfigScreen.this.renderedDropdownRow = null;
            }
        }

        private List<String> options() {
            return this.entry.value.dropdownOptions();
        }

        private DropdownOptionMetadata option(int index) {
            List<DropdownOptionMetadata> options = this.entry.value.dropdownOptionMetadata();
            return index >= 0 && index < options.size() ? options.get(index) : null;
        }

        private DropdownOptionMetadata currentOption() {
            return this.entry.value.dropdownOption(KonfigConfigScreen.this.currentDropdownValue(this.entry.value));
        }

        private boolean isButtonFocused() {
            return this.button.isFocused();
        }

        private DropdownOptionMetadata activeInfoOption(int mouseX, int mouseY) {
            if (!this.dropdownState.open()) {
                return null;
            }

            this.layoutDropdown();
            int hovered = this.hoveredOptionIndex(mouseX, mouseY);
            if (hovered >= 0) {
                return this.option(hovered);
            }
            return this.option(this.dropdownState.selectedIndex());
        }

        private int optionIndex(String option) {
            return this.dropdownState.optionIndex(this.options(), option);
        }

        private int visibleOptionCount() {
            return this.dropdownState.visibleOptionCount(this.options(), SUGGESTION_LIMIT);
        }

        private int maxScrollOffset() {
            return this.dropdownState.maxScrollOffset(this.options(), SUGGESTION_LIMIT);
        }

        private void ensureSelectedVisible() {
            this.dropdownState.ensureSelectedVisible(this.options(), SUGGESTION_LIMIT);
        }

        private void selectOption(int optionIndex) {
            List<String> options = this.options();
            if (optionIndex < 0 || optionIndex >= options.size()) {
                return;
            }

            Object previousDraft = KonfigConfigScreen.this.draftSession.draft(this.entry.value);
            KonfigConfigScreen.this.draftSession.draft(this.entry.value, options.get(optionIndex));
            this.commitOrRevert(previousDraft);
            this.syncFromDraft();
            this.closeDropdown();
        }

        private boolean handleDropdownClick(double mouseX, double mouseY) {
            if (!this.dropdownState.open()) {
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

        private boolean handleDropdownKey(int keyCode) {
            List<String> options = this.options();
            if (!this.dropdownState.open() || options.isEmpty()) {
                return false;
            }

            if (keyCode == KEY_ESCAPE) {
                this.closeDropdown();
                return true;
            }
            if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER || keyCode == KEY_SPACE || keyCode == KEY_TAB) {
                this.selectOption(this.dropdownState.selectedIndex());
                return true;
            }
            if (keyCode == KEY_DOWN) {
                this.dropdownState.selectNext(options, SUGGESTION_LIMIT);
                return true;
            }
            if (keyCode == KEY_UP) {
                this.dropdownState.selectPrevious(options, SUGGESTION_LIMIT);
                return true;
            }
            return false;
        }

        private boolean handleClosedDropdownKey(int keyCode) {
            if (this.dropdownState.open() || this.options().isEmpty()) {
                return false;
            }
            if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER || keyCode == KEY_SPACE) {
                this.openDropdown();
                return true;
            }
            return false;
        }

        private boolean handleDropdownChar(int codePoint) {
            if (!this.dropdownState.open()) {
                return false;
            }

            return this.dropdownState.typeSelect(
                    this.options(),
                    SUGGESTION_LIMIT,
                    codePoint,
                    System.currentTimeMillis(),
                    DROPDOWN_TYPE_SELECT_RESET_MS,
                    new LegacyDropdownState.OptionSearchText() {
                        @Override
                        public String searchText(int index) {
                            return DropdownRow.this.optionSearchText(index);
                        }
                    }
            );
        }

        private String optionSearchText(int index) {
            DropdownOptionMetadata option = this.option(index);
            if (option == null) {
                return "";
            }

            String label = translatedDropdownOption(this.entry, option).getString();
            return (label + " " + option.value()).toLowerCase(Locale.ROOT);
        }

        private boolean handleDropdownScroll(double mouseX, double mouseY, double scrollY) {
            if (!this.dropdownState.open()) {
                return false;
            }
            this.layoutDropdown();
            if (!this.isPointInsideDropdown(mouseX, mouseY)) {
                return false;
            }

            this.dropdownState.scroll(this.options(), SUGGESTION_LIMIT, scrollY);
            return true;
        }

        private boolean isPointInsideButton(double mouseX, double mouseY) {
            return mouseX >= this.lastButtonX
                    && mouseX <= this.lastButtonX + this.lastButtonWidth
                    && mouseY >= this.lastButtonY
                    && mouseY <= this.lastButtonY + CONTROL_HEIGHT;
        }

        private boolean isPointInsideDropdown(double mouseX, double mouseY) {
            if (!this.dropdownState.open()) {
                return false;
            }
            this.layoutDropdown();
            return mouseX >= this.lastDropdownX
                    && mouseX <= this.lastDropdownX + this.lastDropdownWidth
                    && mouseY >= this.lastDropdownY
                    && mouseY <= this.lastDropdownY + this.lastDropdownHeight;
        }

        private void layoutDropdown() {
            int visibleCount = this.visibleOptionCount();
            this.lastDropdownWidth = Math.max(CONTROL_MIN_WIDTH, this.lastButtonWidth);
            this.lastDropdownHeight = (visibleCount * SUGGESTION_ROW_HEIGHT) + 4;
            this.lastDropdownX = this.lastButtonX;

            int belowY = this.lastButtonY + CONTROL_HEIGHT + 2;
            int aboveY = this.lastButtonY - this.lastDropdownHeight - 2;
            boolean openAbove = belowY + this.lastDropdownHeight > KonfigConfigScreen.this.height - 32 && aboveY >= LIST_TOP;
            this.lastDropdownY = openAbove ? aboveY : belowY;
            this.ensureSelectedVisible();
        }

        private void renderButtonLabel(MatrixStack guiGraphics) {
            int textX = this.lastButtonX + 6;
            int chevronLeft = this.lastButtonX + this.lastButtonWidth - DROPDOWN_CHEVRON_WIDTH;
            int textMaxWidth = Math.max(0, chevronLeft - textX - 4);
            int textY = this.lastButtonY + ((CONTROL_HEIGHT - KonfigConfigScreen.this.font.lineHeight) / 2) + 1;
            ITextComponent valueText = this.fitDropdownText(dropdownText(this.entry, KonfigConfigScreen.this.currentDropdownValue(this.entry.value)), textMaxWidth);
            KonfigConfigScreen.this.font.draw(guiGraphics, valueText, (float) textX, (float) textY, 0xFFFFFFFF);

            ITextComponent chevron = text(this.dropdownState.open() ? "\u25B4" : "\u25BE");
            int chevronX = chevronLeft + Math.max(0, (DROPDOWN_CHEVRON_WIDTH - KonfigConfigScreen.this.font.width(chevron)) / 2);
            KonfigConfigScreen.this.font.draw(guiGraphics, chevron, (float) chevronX, (float) textY, this.dropdownState.open() ? 0xFFF8E38F : 0xFFCFCFCF);
        }

        private ITextComponent fitDropdownText(ITextComponent value, int maxWidth) {
            if (maxWidth <= 0) {
                return text("");
            }
            if (KonfigConfigScreen.this.font.width(value) <= maxWidth) {
                return value;
            }

            String ellipsis = "...";
            int available = Math.max(0, maxWidth - KonfigConfigScreen.this.font.width(ellipsis));
            String raw = value.getString();
            int end = raw.length();
            while (end > 0 && KonfigConfigScreen.this.font.width(raw.substring(0, end)) > available) {
                end--;
            }
            return text(raw.substring(0, end).trim() + ellipsis);
        }

        private int hoveredOptionIndex(int mouseX, int mouseY) {
            return this.dropdownState.hoveredOptionIndex(
                    mouseX,
                    mouseY,
                    this.lastDropdownX,
                    this.lastDropdownY,
                    this.lastDropdownWidth,
                    this.lastDropdownHeight,
                    SUGGESTION_ROW_HEIGHT,
                    this.options(),
                    SUGGESTION_LIMIT
            );
        }

        private void renderDropdown(MatrixStack guiGraphics, int mouseX, int mouseY) {
            List<String> options = this.options();
            if (!this.dropdownState.open() || options.isEmpty()) {
                return;
            }

            this.layoutDropdown();
            AbstractGui.fill(guiGraphics, this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
            AbstractGui.fill(guiGraphics, this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

            int hovered = this.hoveredOptionIndex(mouseX, mouseY);
            DropdownOptionMetadata tooltipOption = this.option(hovered >= 0 ? hovered : this.dropdownState.selectedIndex());
            String tooltip = translatedDropdownTooltip(tooltipOption);
            if (!isBlank(tooltip)) {
                KonfigConfigScreen.this.queueTooltip(guiGraphics, tooltip, mouseX, mouseY);
            }
            int visibleCount = this.visibleOptionCount();
            int currentIndex = this.optionIndex(KonfigConfigScreen.this.currentDropdownValue(this.entry.value));
            for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
                int optionIndex = this.dropdownState.scrollOffset() + visibleIndex;
                if (optionIndex >= options.size()) {
                    break;
                }

                int rowY = this.lastDropdownY + 2 + (visibleIndex * SUGGESTION_ROW_HEIGHT);
                int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                boolean rowHovered = optionIndex == hovered;
                boolean focused = optionIndex == this.dropdownState.selectedIndex();
                boolean current = optionIndex == currentIndex;
                if (rowHovered || focused || current) {
                    int color = rowHovered ? 0x805C6FA8 : focused ? 0x60406080 : 0x50303030;
                    AbstractGui.fill(guiGraphics, this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, color);
                }
                if (current) {
                    AbstractGui.fill(guiGraphics, this.lastDropdownX + 2, rowY + 2, this.lastDropdownX + 4, rowBottom - 2, 0xFFF8E38F);
                }
                int textX = this.lastDropdownX + 8;
                int textRight = this.maxScrollOffset() > 0 ? this.lastDropdownX + this.lastDropdownWidth - 8 : this.lastDropdownX + this.lastDropdownWidth - 4;
                KonfigConfigScreen.this.font.draw(guiGraphics, this.fitDropdownText(dropdownText(this.entry, options.get(optionIndex)), Math.max(0, textRight - textX)), (float) textX, (float) (rowY + 3), 0xFFFFFFFF);
            }

            if (this.maxScrollOffset() > 0) {
                int trackTop = this.lastDropdownY + 2;
                int trackBottom = this.lastDropdownY + this.lastDropdownHeight - 2;
                int trackHeight = Math.max(1, trackBottom - trackTop);
                int thumbHeight = MathHelper.clamp((trackHeight * visibleCount) / options.size(), 10, trackHeight);
                int thumbTop = trackTop + ((trackHeight - thumbHeight) * this.dropdownState.scrollOffset() / this.maxScrollOffset());
                AbstractGui.fill(guiGraphics, this.lastDropdownX + this.lastDropdownWidth - 4, trackTop, this.lastDropdownX + this.lastDropdownWidth - 2, trackBottom, 0x44000000);
                AbstractGui.fill(guiGraphics, this.lastDropdownX + this.lastDropdownWidth - 4, thumbTop, this.lastDropdownX + this.lastDropdownWidth - 2, thumbTop + thumbHeight, 0xAAFFFFFF);
            }
        }
    }

    private final class ColorRow extends ConfigRow {
        private static final int PREVIEW_SIZE = 16;
        private static final int PREVIEW_GAP = 6;

        private final Button button;

        private ColorRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, colorText(entry.value), ignored -> {
                KonfigConfigScreen.this.minecraft.setScreen(new ColorEditorScreen(entry));
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(colorText(this.entry.value));
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }

            if (!isBlank(this.entry.tooltip)) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    KonfigConfigScreen.this.queueTooltip(
                            guiGraphics,
                            KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                            mouseX,
                            mouseY
                    );
                }
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int previewX = x + width - controlWidth - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = y + (height - PREVIEW_SIZE) / 2;
            int buttonWidth = controlWidth;
            layoutControl(this.control(), x + width - buttonWidth, y + (height - CONTROL_HEIGHT) / 2, buttonWidth);

            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.contextLabel, x + 4.0F, y + 1.0F, 0xFFA0A0A0);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, 0xFFFFFFFF);
            drawColorSwatch(guiGraphics, previewX, previewY, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
            this.control().render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private final class StringListRow extends ConfigRow {
        private static final int PREVIEW_SIZE = 16;
        private static final int PREVIEW_GAP = 6;

        private final Button button;

        private StringListRow(EntryRef entry) {
            super(entry);
            this.button = new Button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, stringListText(entry.value), ignored -> {
                KonfigConfigScreen.this.minecraft.setScreen(new StringListEditorScreen(entry));
            });
        }

        @Override
        protected Widget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(stringListText(this.entry.value));
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
            if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryId())) {
                return;
            }

            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (values.isEmpty()) {
                return;
            }

            int previewX = this.button.x - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.y + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
            renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryId(), values.get(0), previewX, previewY);
        }
    }

    private abstract class BaseSliderWidget extends AbstractSlider {
        private BaseSliderWidget(double initialProgress) {
            super(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, StringTextComponent.EMPTY, initialProgress);
        }

        protected final void syncToProgress(double progress) {
            this.value = MathHelper.clamp(progress, 0.0D, 1.0D);
            this.updateMessage();
        }
    }

    private final class IntegerSliderRow extends ConfigRow {
        private final int min;
        private final int max;
        private final SliderWidget slider;

        private IntegerSliderRow(EntryRef entry) {
            super(entry);
            this.min = entry.value.rangeMin().intValue();
            this.max = entry.value.rangeMax().intValue();
            this.slider = new SliderWidget();
        }

        @Override
        protected Widget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private int currentValue() {
            Object draft = KonfigConfigScreen.this.draftSession.draft(this.entry.value);
            if (draft instanceof Number) {
                return ((Number) draft).intValue();
            }
            return ((Number) this.entry.value.get()).intValue();
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.draftSession.draft(this.entry.value, Integer.valueOf(intFromProgress(progress, this.min, this.max)));
        }

        private final class SliderWidget extends BaseSliderWidget {
            private SliderWidget() {
                super(progressFor(IntegerSliderRow.this.currentValue(), IntegerSliderRow.this.min, IntegerSliderRow.this.max));
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(text(Integer.toString(IntegerSliderRow.this.currentValue())));
            }

            @Override
            protected void applyValue() {
                IntegerSliderRow.this.updateDraftFromSlider(this.value);
            }

            @Override
            public void onRelease(double mouseX, double mouseY) {
                Object previousValue = IntegerSliderRow.this.entry.value.get();
                super.onRelease(mouseX, mouseY);
                IntegerSliderRow.this.commitOrRevert(previousValue);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                int previousValue = IntegerSliderRow.this.currentValue();
                boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
                if (handled && previousValue != IntegerSliderRow.this.currentValue()) {
                    IntegerSliderRow.this.commitOrRevert(Integer.valueOf(previousValue));
                }
                return handled;
            }
        }
    }

    private final class LongSliderRow extends ConfigRow {
        private final long min;
        private final long max;
        private final SliderWidget slider;

        private LongSliderRow(EntryRef entry) {
            super(entry);
            this.min = entry.value.rangeMin().longValue();
            this.max = entry.value.rangeMax().longValue();
            this.slider = new SliderWidget();
        }

        @Override
        protected Widget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private long currentValue() {
            Object draft = KonfigConfigScreen.this.draftSession.draft(this.entry.value);
            if (draft instanceof Number) {
                return ((Number) draft).longValue();
            }
            return ((Number) this.entry.value.get()).longValue();
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.draftSession.draft(this.entry.value, Long.valueOf(longFromProgress(progress, this.min, this.max)));
        }

        private final class SliderWidget extends BaseSliderWidget {
            private SliderWidget() {
                super(progressFor(LongSliderRow.this.currentValue(), LongSliderRow.this.min, LongSliderRow.this.max));
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(text(Long.toString(LongSliderRow.this.currentValue())));
            }

            @Override
            protected void applyValue() {
                LongSliderRow.this.updateDraftFromSlider(this.value);
            }

            @Override
            public void onRelease(double mouseX, double mouseY) {
                Object previousValue = LongSliderRow.this.entry.value.get();
                super.onRelease(mouseX, mouseY);
                LongSliderRow.this.commitOrRevert(previousValue);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                long previousValue = LongSliderRow.this.currentValue();
                boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
                if (handled && previousValue != LongSliderRow.this.currentValue()) {
                    LongSliderRow.this.commitOrRevert(Long.valueOf(previousValue));
                }
                return handled;
            }
        }
    }

    private final class DoubleSliderRow extends ConfigRow {
        private final double min;
        private final double max;
        private final SliderWidget slider;

        private DoubleSliderRow(EntryRef entry) {
            super(entry);
            this.min = entry.value.rangeMin().doubleValue();
            this.max = entry.value.rangeMax().doubleValue();
            this.slider = new SliderWidget();
        }

        @Override
        protected Widget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private double currentValue() {
            Object draft = KonfigConfigScreen.this.draftSession.draft(this.entry.value);
            if (draft instanceof Number) {
                return ((Number) draft).doubleValue();
            }
            return ((Number) this.entry.value.get()).doubleValue();
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.draftSession.draft(this.entry.value, Double.valueOf(doubleFromProgress(progress, this.min, this.max)));
        }

        private final class SliderWidget extends BaseSliderWidget {
            private SliderWidget() {
                super(progressFor(DoubleSliderRow.this.currentValue(), DoubleSliderRow.this.min, DoubleSliderRow.this.max));
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                this.setMessage(text(formatDouble(DoubleSliderRow.this.currentValue())));
            }

            @Override
            protected void applyValue() {
                DoubleSliderRow.this.updateDraftFromSlider(this.value);
            }

            @Override
            public void onRelease(double mouseX, double mouseY) {
                Object previousValue = DoubleSliderRow.this.entry.value.get();
                super.onRelease(mouseX, mouseY);
                DoubleSliderRow.this.commitOrRevert(previousValue);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                double previousValue = DoubleSliderRow.this.currentValue();
                boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
                if (handled && !LegacyDraftSession.sameValue(Double.valueOf(previousValue), Double.valueOf(DoubleSliderRow.this.currentValue()))) {
                    DoubleSliderRow.this.commitOrRevert(Double.valueOf(previousValue));
                }
                return handled;
            }
        }
    }

    private final class RegistryTextInputRow extends ConfigRow {
        private static final int ICON_SIZE = 16;
        private static final int ICON_GAP = 6;

        private final TextFieldWidget input;
        private final List<String> visibleSuggestions = new ArrayList<String>();
        private boolean suppressResponder;
        private boolean suggestionsDismissed;
        private String dismissedValue = "";
        private int selectedSuggestionIndex;
        private int lastInputX;
        private int lastInputY;
        private int lastInputWidth;
        private int lastDropdownX;
        private int lastDropdownY;
        private int lastDropdownWidth;
        private int lastDropdownHeight;

        private RegistryTextInputRow(EntryRef entry) {
            super(entry);
            this.input = new TextFieldWidget(KonfigConfigScreen.this.font, 0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, entry.label);
            this.input.setMaxLength(256);
            this.input.setValue(stringValue(KonfigConfigScreen.this.draftSession.draft(entry.value)));
            this.input.setResponder(value -> {
                if (this.suppressResponder) {
                    return;
                }
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                KonfigConfigScreen.this.draftSession.draft(entry.value, value);
                KonfigConfigScreen.this.persistEntry(entry);
                this.refreshSuggestions();
            });
        }

        @Override
        protected Widget control() {
            return this.input;
        }

        @Override
        protected void tick() {
            if (this.input.isFocused()) {
                KonfigConfigScreen.this.setActiveRegistryRow(this);
                this.refreshSuggestions();
            }
        }

        @Override
        protected void syncFromDraft() {
            this.suppressResponder = true;
            this.input.setValue(stringValue(KonfigConfigScreen.this.draftSession.draft(this.entry.value)));
            this.suppressResponder = false;
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
            this.refreshSuggestions();
        }

        @Override
        protected void renderRow(MatrixStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                AbstractGui.fill(guiGraphics, x, y, x + width, y + height, 0x22000000);
            }

            if (!isBlank(this.entry.tooltip)) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    KonfigConfigScreen.this.queueTooltip(
                            guiGraphics,
                            KonfigConfigScreen.this.font.split(text(this.entry.tooltip), Math.max(KonfigConfigScreen.this.width / 2, 200)),
                            mouseX,
                            mouseY
                    );
                }
            }

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int controlX = x + width - controlWidth;
            int controlY = y + (height - CONTROL_HEIGHT) / 2;
            layoutControl(this.control(), controlX, controlY, controlWidth);
            this.lastInputX = controlX;
            this.lastInputY = controlY;
            this.lastInputWidth = controlWidth;

            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.contextLabel, x + 4.0F, y + 1.0F, 0xFFA0A0A0);
            KonfigConfigScreen.this.font.draw(guiGraphics, this.entry.displayLabel(), x + 4.0F, y + 12.0F, 0xFFFFFFFF);
            if (this.entry.value.hasBoundRegistry() && supportsRegistryIcon(this.entry.value.boundRegistryId())) {
                renderRegistryIcon(
                        guiGraphics,
                        this.entry.value.boundRegistryId(),
                        KonfigConfigScreen.this.currentStringValue(this.entry.value),
                        controlX - ICON_GAP - ICON_SIZE,
                        y + (height - ICON_SIZE) / 2
                );
            }
            this.input.render(guiGraphics, mouseX, mouseY, partialTick);

            if (this.input.isFocused()) {
                KonfigConfigScreen.this.setActiveRegistryRow(this);
                this.refreshSuggestions();
            }
            if (KonfigConfigScreen.this.activeRegistryRow == this && !this.visibleSuggestions.isEmpty()) {
                KonfigConfigScreen.this.renderedRegistryRow = this;
            }
        }

        public boolean isFocused() {
            return this.input.isFocused();
        }

        private boolean isPointInsideInput(double mouseX, double mouseY) {
            return mouseX >= this.lastInputX
                    && mouseX <= this.lastInputX + this.lastInputWidth
                    && mouseY >= this.lastInputY
                    && mouseY <= this.lastInputY + CONTROL_HEIGHT;
        }

        private void refreshSuggestions() {
            if (!this.entry.value.hasBoundRegistry()) {
                this.closeSuggestions();
                return;
            }

            if (this.suggestionsDismissed) {
                if (LegacyDraftSession.sameValue(this.input.getValue(), this.dismissedValue)) {
                    this.visibleSuggestions.clear();
                    this.selectedSuggestionIndex = 0;
                    this.input.setSuggestion("");
                    return;
                }
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
            }

            this.visibleSuggestions.clear();
            this.visibleSuggestions.addAll(filterRegistrySuggestions(
                    KonfigConfigScreen.this.registrySuggestions(this.entry.value.boundRegistryId()),
                    this.input.getValue()
            ));

            if (this.visibleSuggestions.isEmpty()) {
                this.selectedSuggestionIndex = 0;
                this.input.setSuggestion("");
                return;
            }

            this.selectedSuggestionIndex = MathHelper.clamp(this.selectedSuggestionIndex, 0, this.visibleSuggestions.size() - 1);
            this.updateInlineSuggestion();
        }

        private void activateSuggestions() {
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
            this.refreshSuggestions();
        }

        private void dismissSuggestions() {
            this.suggestionsDismissed = true;
            this.dismissedValue = this.input.getValue();
            this.visibleSuggestions.clear();
            this.selectedSuggestionIndex = 0;
            this.input.setSuggestion("");
        }

        private void closeSuggestions() {
            this.suggestionsDismissed = false;
            this.dismissedValue = "";
            this.visibleSuggestions.clear();
            this.selectedSuggestionIndex = 0;
            this.input.setSuggestion("");
            if (KonfigConfigScreen.this.activeRegistryRow == this) {
                KonfigConfigScreen.this.activeRegistryRow = null;
            }
        }

        private void renderSuggestions(MatrixStack guiGraphics, int mouseX, int mouseY) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                return;
            }

            this.layoutSuggestionBox();
            AbstractGui.fill(guiGraphics, this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
            AbstractGui.fill(guiGraphics, this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

            for (int index = 0; index < this.visibleSuggestions.size(); index++) {
                int rowY = this.lastDropdownY + 2 + (index * SUGGESTION_ROW_HEIGHT);
                int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                boolean hovered = index == this.hoveredSuggestionIndex(mouseX, mouseY);
                if (hovered || index == this.selectedSuggestionIndex) {
                    AbstractGui.fill(guiGraphics, this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, hovered ? 0x80406080 : 0x50303030);
                }
                int textX = this.lastDropdownX + 4;
                if (this.entry.value.hasBoundRegistry() && supportsRegistryIcon(this.entry.value.boundRegistryId())) {
                    renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryId(), this.visibleSuggestions.get(index), this.lastDropdownX + 2, rowY - 1);
                    textX += 18;
                }
                KonfigConfigScreen.this.font.draw(
                        guiGraphics,
                        text(this.visibleSuggestions.get(index)),
                        (float) textX,
                        (float) (rowY + 3),
                        0xFFFFFFFF
                );
            }
        }

        private boolean handleSuggestionClick(double mouseX, double mouseY) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                return false;
            }

            int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
            if (hovered < 0) {
                return false;
            }

            this.acceptSuggestion(this.visibleSuggestions.get(hovered));
            return true;
        }

        private boolean handleSuggestionKey(int keyCode) {
            if (KonfigConfigScreen.this.activeRegistryRow != this) {
                return false;
            }

            if (keyCode == KEY_ESCAPE) {
                this.dismissSuggestions();
                return true;
            }
            if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER) {
                this.dismissSuggestions();
                return true;
            }
            if (this.visibleSuggestions.isEmpty()) {
                return false;
            }
            if (keyCode == KEY_DOWN) {
                this.selectedSuggestionIndex = (this.selectedSuggestionIndex + 1) % this.visibleSuggestions.size();
                this.updateInlineSuggestion();
                return true;
            }
            if (keyCode == KEY_UP) {
                this.selectedSuggestionIndex = (this.selectedSuggestionIndex + this.visibleSuggestions.size() - 1) % this.visibleSuggestions.size();
                this.updateInlineSuggestion();
                return true;
            }
            if (keyCode == KEY_TAB) {
                this.acceptSuggestion(this.visibleSuggestions.get(this.selectedSuggestionIndex));
                return true;
            }
            return false;
        }

        private void acceptSuggestion(String suggestion) {
            this.suppressResponder = true;
            this.input.setValue(suggestion);
            this.suppressResponder = false;
            KonfigConfigScreen.this.draftSession.draft(this.entry.value, suggestion);
            KonfigConfigScreen.this.persistEntry(this.entry);
            this.dismissSuggestions();
            this.input.setFocus(true);
        }

        private void updateInlineSuggestion() {
            if (this.visibleSuggestions.isEmpty()) {
                this.input.setSuggestion("");
                return;
            }
            this.input.setSuggestion(suggestionSuffix(this.input.getValue(), this.visibleSuggestions.get(this.selectedSuggestionIndex)));
        }

        private void layoutSuggestionBox() {
            this.lastDropdownX = this.lastInputX;
            this.lastDropdownWidth = this.lastInputWidth;
            this.lastDropdownHeight = (this.visibleSuggestions.size() * SUGGESTION_ROW_HEIGHT) + 4;

            int belowY = this.lastInputY + CONTROL_HEIGHT + 2;
            int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
            boolean openAbove = belowY + this.lastDropdownHeight > KonfigConfigScreen.this.height - 32 && aboveY >= LIST_TOP;
            this.lastDropdownY = openAbove ? aboveY : belowY;
        }

        private int hoveredSuggestionIndex(int mouseX, int mouseY) {
            if (mouseX < this.lastDropdownX
                    || mouseX > this.lastDropdownX + this.lastDropdownWidth
                    || mouseY < this.lastDropdownY + 2
                    || mouseY > this.lastDropdownY + this.lastDropdownHeight - 2) {
                return -1;
            }

            int index = (mouseY - this.lastDropdownY - 2) / SUGGESTION_ROW_HEIGHT;
            return index >= 0 && index < this.visibleSuggestions.size() ? index : -1;
        }
    }

    private final class TextInputRow extends ConfigRow {
        private final TextFieldWidget input;
        private String validationMessage = "";

        private TextInputRow(EntryRef entry) {
            super(entry);
            this.input = new TextFieldWidget(KonfigConfigScreen.this.font, 0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, entry.label);
            this.input.setMaxLength(256);
            this.input.setValue(stringValue(KonfigConfigScreen.this.draftSession.draft(entry.value)));
            this.input.setResponder(value -> {
                KonfigConfigScreen.this.draftSession.draft(entry.value, value);
                try {
                    LegacyDraftSession.parseDraft(entry.value, value);
                    this.validationMessage = "";
                    KonfigConfigScreen.this.persistEntry(entry);
                } catch (Exception exception) {
                    this.validationMessage = LegacyDraftSession.exceptionMessage(exception);
                }
            });
        }

        @Override
        protected Widget control() {
            return this.input;
        }

        @Override
        protected String validationMessage() {
            return this.validationMessage;
        }

        @Override
        protected void tick() {
        }

        @Override
        protected void syncFromDraft() {
            this.input.setValue(stringValue(KonfigConfigScreen.this.draftSession.draft(this.entry.value)));
        }
    }

    private abstract class EntryEditorScreen extends Screen {
        protected static final int EDITOR_TITLE_Y = 8;
        protected static final int EDITOR_CONTEXT_Y = 24;
        protected static final int EDITOR_CONTENT_TOP = 42;

        protected final EntryRef entry;

        private EntryEditorScreen(EntryRef entry) {
            super(entry.label);
            this.entry = entry;
        }

        @Override
        public void onClose() {
            this.returnToParent();
        }

        protected final void returnToParent() {
            KonfigConfigScreen.this.rebuildScreenWidgets();
            this.minecraft.setScreen(KonfigConfigScreen.this);
        }

        protected final boolean persistEditedValue(Object previousValue) {
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                KonfigConfigScreen.this.draftSession.draft(this.entry.value, LegacyDraftSession.copyDraftValue(this.entry.value, previousValue));
                return false;
            }
            return true;
        }

        protected final boolean resetToSessionStart() {
            Object previousValue = LegacyDraftSession.snapshotValue(this.entry.value, this.entry.value.get());
            try {
                Object resetValue = LegacyDraftSession.snapshotValue(this.entry.value, KonfigConfigScreen.this.draftSession.sessionStartValue(this.entry.value));
                KonfigConfigScreen.this.draftSession.draft(this.entry.value, LegacyDraftSession.copyDraftValue(this.entry.value, resetValue));
                LegacyDraftSession.setRawValue(this.entry.value, resetValue);
                this.entry.handle.save();
                return true;
            } catch (Exception exception) {
                LegacyDraftSession.setRawValue(this.entry.value, previousValue);
                KonfigConfigScreen.this.draftSession.draft(this.entry.value, LegacyDraftSession.copyDraftValue(this.entry.value, previousValue));
                KonfigToastSupport.resetFailed(LegacyDraftSession.exceptionMessage(exception));
                return false;
            }
        }

        protected final void renderEditorChrome(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            AbstractGui.fill(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            drawCenteredString(guiGraphics, this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
            this.font.draw(guiGraphics, this.entry.contextLabel, 12, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
        }
    }

    private enum ColorChannel {
        RED("konfig.screen.color.red"),
        GREEN("konfig.screen.color.green"),
        BLUE("konfig.screen.color.blue"),
        ALPHA("konfig.screen.color.alpha");

        private final String translationKey;

        ColorChannel(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private final class ColorEditorScreen extends EntryEditorScreen {
        private static final int PREVIEW_SIZE = 32;
        private static final int PREVIEW_Y = EDITOR_CONTENT_TOP;
        private static final int HEX_WIDTH = 108;
        private static final int HEX_Y = PREVIEW_Y + PREVIEW_SIZE + 8;
        private static final int SLIDER_WIDTH = 220;
        private static final int SLIDER_Y = HEX_Y + 34;
        private static final int SLIDER_STEP = 26;

        private TextFieldWidget hexInput;
        private ChannelSlider redSlider;
        private ChannelSlider greenSlider;
        private ChannelSlider blueSlider;
        private ChannelSlider alphaSlider;
        private boolean suppressHexResponder;
        private String validationMessage = "";

        private ColorEditorScreen(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void init() {
            this.buttons.clear();
            this.children.clear();

            this.hexInput = this.addButton(new TextFieldWidget(this.font, this.width / 2 - HEX_WIDTH / 2, HEX_Y, HEX_WIDTH, 20, this.entry.label));
            this.hexInput.setMaxLength(this.entry.value.kind() == EntryKind.COLOR_ARGB ? 9 : 7);
            this.hexInput.setValue(this.currentHex());
            this.hexInput.setResponder(this::onHexChanged);

            int sliderX = this.width / 2 - SLIDER_WIDTH / 2;
            this.redSlider = this.addButton(new ChannelSlider(ColorChannel.RED, sliderX, SLIDER_Y));
            this.greenSlider = this.addButton(new ChannelSlider(ColorChannel.GREEN, sliderX, SLIDER_Y + SLIDER_STEP));
            this.blueSlider = this.addButton(new ChannelSlider(ColorChannel.BLUE, sliderX, SLIDER_Y + (SLIDER_STEP * 2)));
            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                this.alphaSlider = this.addButton(new ChannelSlider(ColorChannel.ALPHA, sliderX, SLIDER_Y + (SLIDER_STEP * 3)));
            }

            int footerY = this.height - 26;
            this.addButton(new Button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.syncWidgetsFromDraft();
                }
            }));
            this.addButton(new Button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));

            this.syncWidgetsFromDraft();
        }

        @Override
        public void render(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            int previewX = this.width / 2 - PREVIEW_SIZE / 2;
            drawColorSwatch(guiGraphics, previewX, PREVIEW_Y, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
            this.renderValidationMessage(guiGraphics);
        }

        @Override
        public void tick() {
            super.tick();
        }

        private void onHexChanged(String value) {
            if (this.suppressHexResponder) {
                return;
            }

            String normalized = normalizeHexInput(value);
            int expectedDigits = ColorValueHelper.expectedDigits(this.entry.value.kind());
            if (normalized.isEmpty()) {
                this.validationMessage = "";
                return;
            }
            if (!isHexPrefix(normalized) || normalized.length() > expectedDigits) {
                this.validationMessage = translate("konfig.screen.color.invalid", Integer.valueOf(expectedDigits)).getString();
                return;
            }
            if (normalized.length() < expectedDigits) {
                this.validationMessage = "";
                return;
            }

            Object previousValue = LegacyDraftSession.snapshotValue(this.entry.value, this.entry.value.get());
            try {
                int parsed = LegacyDraftSession.parseColor(this.entry.value, value);
                KonfigConfigScreen.this.draftSession.draft(this.entry.value, Integer.valueOf(parsed));
                if (this.persistEditedValue(previousValue)) {
                    this.validationMessage = "";
                    this.syncWidgetsFromDraft();
                } else {
                    this.syncWidgetsFromDraft();
                }
            } catch (Exception exception) {
                KonfigConfigScreen.this.draftSession.draft(this.entry.value, LegacyDraftSession.copyDraftValue(this.entry.value, previousValue));
                this.validationMessage = exception.getMessage() == null
                        ? translate("konfig.screen.color.invalid", Integer.valueOf(expectedDigits)).getString()
                        : exception.getMessage();
                this.syncWidgetsFromDraft();
            }
        }

        private void renderValidationMessage(MatrixStack guiGraphics) {
            if (!this.validationMessage.isEmpty()) {
                drawCenteredString(guiGraphics, this.font, text(this.validationMessage), this.width / 2, HEX_Y + CONTROL_HEIGHT + 3, VALIDATION_COLOR);
            }
        }

        private String currentHex() {
            int color = KonfigConfigScreen.this.currentColor(this.entry.value);
            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                return ColorValueHelper.formatArgb(color);
            }
            return ColorValueHelper.formatRgb(color);
        }

        private void syncWidgetsFromDraft() {
            this.suppressHexResponder = true;
            this.hexInput.setValue(this.currentHex());
            this.suppressHexResponder = false;
            this.redSlider.syncToDraft();
            this.greenSlider.syncToDraft();
            this.blueSlider.syncToDraft();
            if (this.alphaSlider != null) {
                this.alphaSlider.syncToDraft();
            }
        }

        private int currentChannel(ColorChannel channel) {
            int color = KonfigConfigScreen.this.currentColor(this.entry.value);
            switch (channel) {
                case RED:
                    return ColorValueHelper.red(color);
                case GREEN:
                    return ColorValueHelper.green(color);
                case BLUE:
                    return ColorValueHelper.blue(color);
                case ALPHA:
                    return this.entry.value.kind() == EntryKind.COLOR_ARGB ? ColorValueHelper.alpha(color) : 255;
                default:
                    return 0;
            }
        }

        private int withChannel(ColorChannel channel, int value) {
            int current = KonfigConfigScreen.this.currentColor(this.entry.value);
            int alpha = this.entry.value.kind() == EntryKind.COLOR_ARGB ? ColorValueHelper.alpha(current) : 255;
            int red = ColorValueHelper.red(current);
            int green = ColorValueHelper.green(current);
            int blue = ColorValueHelper.blue(current);

            switch (channel) {
                case RED:
                    red = value;
                    break;
                case GREEN:
                    green = value;
                    break;
                case BLUE:
                    blue = value;
                    break;
                case ALPHA:
                    alpha = value;
                    break;
                default:
                    break;
            }

            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                return ColorValueHelper.argb(alpha, red, green, blue);
            }
            return ColorValueHelper.rgb(red, green, blue);
        }

        private final class ChannelSlider extends BaseSliderWidget {
            private final ColorChannel channel;

            private ChannelSlider(ColorChannel channel, int x, int y) {
                super(ColorEditorScreen.this.currentChannel(channel) / 255.0D);
                this.channel = channel;
                this.x = x;
                this.y = y;
                this.setWidth(SLIDER_WIDTH);
                this.updateMessage();
            }

            private void syncToDraft() {
                this.syncToProgress(ColorEditorScreen.this.currentChannel(this.channel) / 255.0D);
            }

            @Override
            protected void updateMessage() {
                this.setMessage(translate(this.channel.translationKey, Integer.valueOf(ColorEditorScreen.this.currentChannel(this.channel))));
            }

            @Override
            protected void applyValue() {
                KonfigConfigScreen.this.draftSession.draft(ColorEditorScreen.this.entry.value, Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, intFromProgress(this.value, 0, 255))));
            }

            @Override
            public void onRelease(double mouseX, double mouseY) {
                Object previousValue = LegacyDraftSession.snapshotValue(ColorEditorScreen.this.entry.value, ColorEditorScreen.this.entry.value.get());
                super.onRelease(mouseX, mouseY);
                if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                    ColorEditorScreen.this.syncWidgetsFromDraft();
                }
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                Object previousValue = LegacyDraftSession.snapshotValue(ColorEditorScreen.this.entry.value, ColorEditorScreen.this.entry.value.get());
                int before = ColorEditorScreen.this.currentChannel(this.channel);
                boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
                if (handled && before != ColorEditorScreen.this.currentChannel(this.channel)) {
                    if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                        ColorEditorScreen.this.syncWidgetsFromDraft();
                    }
                }
                return handled;
            }
        }
    }

    private final class StringListEditorScreen extends EntryEditorScreen {
        private static final int ITEM_ROW_HEIGHT = 28;

        private ListEntryList list;
        private ListEntryRow activeRegistryRow;
        private ListEntryRow renderedRegistryRow;

        private StringListEditorScreen(EntryRef entry) {
            super(entry);
        }

        @Override
        protected void init() {
            this.rebuildEditorWidgets();
        }

        @Override
        public void render(MatrixStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderedRegistryRow = null;
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            if (this.list != null) {
                this.list.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            String count = translate("konfig.screen.list.count", Integer.valueOf(KonfigConfigScreen.this.currentStringList(this.entry.value).size())).getString();
            this.font.draw(guiGraphics, text(count), this.width - 12 - this.font.width(count), EDITOR_CONTEXT_Y, 0xFFC0C0C0);
            if (KonfigConfigScreen.this.currentStringList(this.entry.value).isEmpty()) {
                drawCenteredString(guiGraphics, this.font, translate("konfig.screen.list.empty"), this.width / 2, this.height / 2 - 12, 0xFFC0C0C0);
            }
            if (this.renderedRegistryRow != null) {
                this.renderedRegistryRow.renderSuggestions(guiGraphics, mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionClick(mouseX, mouseY)) {
                return true;
            }

            boolean handled = super.mouseClicked(mouseX, mouseY, button);
            ListEntryRow focusedRow = this.findFocusedRegistryRow();
            if (focusedRow != null) {
                this.setActiveRegistryRow(focusedRow);
                focusedRow.activateSuggestions();
            } else if (this.activeRegistryRow != null && !this.activeRegistryRow.isPointInsideInput(mouseX, mouseY)) {
                this.activeRegistryRow.closeSuggestions();
            }
            return handled;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionKey(keyCode)) {
                return true;
            }
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (this.activeRegistryRow != null && this.activeRegistryRow.isFocused() && this.activeRegistryRow.hasRegistryBinding()) {
                this.activeRegistryRow.refreshSuggestions();
            }
            return handled;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.list == null) {
                return;
            }
            for (ListEntryRow row : this.list.children()) {
                row.tick();
            }
        }

        private void rebuildEditorWidgets() {
            this.buttons.clear();
            this.children.clear();
            this.activeRegistryRow = null;
            this.renderedRegistryRow = null;
            int listTop = EDITOR_CONTENT_TOP;
            int listHeight = Math.max(48, this.height - listTop - LIST_BOTTOM_MARGIN);
            this.list = new ListEntryList(this.minecraft, this.width, listHeight, listTop);
            this.children.add(this.list);
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            for (int i = 0; i < values.size(); i++) {
                this.list.addListEntry(new ListEntryRow(i, values.get(i)));
            }

            int footerY = this.height - 26;
            this.addButton(new Button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.list.add"), ignored -> this.addValue()));
            this.addButton(new Button(this.width / 2 - 40, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.rebuildEditorWidgets();
                }
            }));
            this.addButton(new Button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));
        }

        private void addValue() {
            Object previousValue = LegacyDraftSession.snapshotValue(this.entry.value, this.entry.value.get());
            KonfigConfigScreen.this.draftSession.draft(
                    this.entry.value,
                    LegacyStringListState.withAdded(
                            KonfigConfigScreen.this.currentStringList(this.entry.value),
                            this.entry.value.hasBoundRegistry() ? "" : translate("konfig.screen.list.new_item").getString()
                    )
            );
            if (this.persistEditedValue(previousValue)) {
                this.rebuildEditorWidgets();
            }
        }

        private ListEntryRow findFocusedRegistryRow() {
            if (this.list == null) {
                return null;
            }
            for (ListEntryRow row : this.list.children()) {
                if (row.hasRegistryBinding() && row.isFocused()) {
                    return row;
                }
            }
            return null;
        }

        private void setActiveRegistryRow(ListEntryRow row) {
            if (this.activeRegistryRow == row) {
                return;
            }
            if (this.activeRegistryRow != null) {
                this.activeRegistryRow.closeSuggestions();
            }
            this.activeRegistryRow = row;
        }

        private final class ListEntryList extends ExtendedList<ListEntryRow> {
            private ListEntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y) {
                super(minecraft, width, height, y, y + height, ITEM_ROW_HEIGHT);
                this.setRenderBackground(false);
                this.setRenderTopAndBottom(false);
            }

            private void addListEntry(ListEntryRow row) {
                super.addEntry(row);
            }

            @Override
            public int getRowWidth() {
                return StringListEditorScreen.this.width - 28;
            }

            @Override
            protected int getScrollbarPosition() {
                return this.x1 - 6;
            }

            @Override
            protected void renderBackground(MatrixStack guiGraphics) {
                AbstractGui.fill(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
            }
        }

        private final class ListEntryRow extends ExtendedList.AbstractListEntry<ListEntryRow> implements INestedGuiEventHandler {
            private static final int ICON_SIZE = 16;
            private static final int ICON_GAP = 4;

            private final int index;
            private final TextFieldWidget input;
            private final Button moveUpButton;
            private final Button moveDownButton;
            private final Button removeButton;
            private final List<String> visibleSuggestions = new ArrayList<String>();
            private boolean suppressResponder;
            private boolean suggestionsDismissed;
            private String dismissedValue = "";
            private int selectedSuggestionIndex;
            private int lastInputX;
            private int lastInputY;
            private int lastInputWidth;
            private int lastDropdownX;
            private int lastDropdownY;
            private int lastDropdownWidth;
            private int lastDropdownHeight;
            private IGuiEventListener focused;
            private boolean dragging;

            private ListEntryRow(int index, String value) {
                this.index = index;
                this.input = new TextFieldWidget(StringListEditorScreen.this.font, 0, 0, 140, CONTROL_HEIGHT, StringListEditorScreen.this.entry.label);
                this.input.setMaxLength(256);
                this.input.setValue(value);
                this.input.setResponder(this::onValueChanged);

                this.moveUpButton = new Button(0, 0, 20, 20, text("^"), ignored -> this.move(-1));
                this.moveDownButton = new Button(0, 0, 20, 20, text("v"), ignored -> this.move(1));
                this.removeButton = new Button(0, 0, 20, 20, text("-"), ignored -> this.remove());
            }

            private void tick() {
                if (this.input.isFocused() && this.hasRegistryBinding()) {
                    StringListEditorScreen.this.setActiveRegistryRow(this);
                    this.refreshSuggestions();
                }
            }

            private void onValueChanged(String value) {
                if (this.suppressResponder) {
                    return;
                }

                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                this.persistListValue(value);
            }

            private boolean hasRegistryBinding() {
                return StringListEditorScreen.this.entry.value.hasBoundRegistry();
            }

            private String registryKey() {
                return StringListEditorScreen.this.entry.value.boundRegistryId();
            }

            public boolean isFocused() {
                return this.input.isFocused();
            }

            private boolean isPointInsideInput(double mouseX, double mouseY) {
                return mouseX >= this.lastInputX
                        && mouseX <= this.lastInputX + this.lastInputWidth
                        && mouseY >= this.lastInputY
                        && mouseY <= this.lastInputY + CONTROL_HEIGHT;
            }

            private boolean persistListValue(String value) {
                Object previousValue = LegacyDraftSession.snapshotValue(StringListEditorScreen.this.entry.value, StringListEditorScreen.this.entry.value.get());
                KonfigConfigScreen.this.draftSession.draft(
                        StringListEditorScreen.this.entry.value,
                        LegacyStringListState.withReplaced(
                                KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value),
                                this.index,
                                value
                        )
                );
                if (!StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    this.suppressResponder = true;
                    this.input.setValue(currentStringList(StringListEditorScreen.this.entry.value).get(this.index));
                    this.suppressResponder = false;
                    this.refreshSuggestions();
                    return false;
                }
                this.refreshSuggestions();
                return true;
            }

            private void move(int delta) {
                int targetIndex = this.index + delta;
                List<String> current = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                if (targetIndex < 0 || targetIndex >= current.size()) {
                    return;
                }

                Object previousValue = LegacyDraftSession.snapshotValue(StringListEditorScreen.this.entry.value, StringListEditorScreen.this.entry.value.get());
                KonfigConfigScreen.this.draftSession.draft(
                        StringListEditorScreen.this.entry.value,
                        LegacyStringListState.withMoved(current, this.index, delta)
                );
                if (StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

            private void remove() {
                List<String> current = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                if (this.index < 0 || this.index >= current.size()) {
                    return;
                }

                Object previousValue = LegacyDraftSession.snapshotValue(StringListEditorScreen.this.entry.value, StringListEditorScreen.this.entry.value.get());
                KonfigConfigScreen.this.draftSession.draft(
                        StringListEditorScreen.this.entry.value,
                        LegacyStringListState.withRemoved(current, this.index)
                );
                if (StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

            @Override
            public void render(MatrixStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered) {
                    AbstractGui.fill(guiGraphics, x, y, x + width, y + ITEM_ROW_HEIGHT, 0x22000000);
                }

                int buttonY = y + 4;
                int removeX = x + width - 20;
                int downX = removeX - 24;
                int upX = downX - 24;
                int iconOffset = 0;
                if (this.hasRegistryBinding() && supportsRegistryIcon(this.registryKey())) {
                    renderRegistryIcon(guiGraphics, this.registryKey(), this.input.getValue(), x, y + (ITEM_ROW_HEIGHT - ICON_SIZE) / 2);
                    iconOffset = ICON_SIZE + ICON_GAP;
                }
                int inputX = x + iconOffset;
                int inputWidth = Math.max(60, upX - inputX - 8);

                this.input.setX(inputX);
                this.input.y = buttonY;
                this.input.setWidth(inputWidth);
                this.lastInputX = inputX;
                this.lastInputY = buttonY;
                this.lastInputWidth = inputWidth;

                this.moveUpButton.x = upX;
                this.moveUpButton.y = buttonY;
                this.moveUpButton.active = this.index > 0;

                this.moveDownButton.x = downX;
                this.moveDownButton.y = buttonY;
                this.moveDownButton.active = this.index + 1 < KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value).size();

                this.removeButton.x = removeX;
                this.removeButton.y = buttonY;

                this.input.render(guiGraphics, mouseX, mouseY, partialTick);
                this.moveUpButton.render(guiGraphics, mouseX, mouseY, partialTick);
                this.moveDownButton.render(guiGraphics, mouseX, mouseY, partialTick);
                this.removeButton.render(guiGraphics, mouseX, mouseY, partialTick);

                if (this.hasRegistryBinding() && this.input.isFocused()) {
                    StringListEditorScreen.this.setActiveRegistryRow(this);
                    this.refreshSuggestions();
                }
                if (StringListEditorScreen.this.activeRegistryRow == this && !this.visibleSuggestions.isEmpty()) {
                    StringListEditorScreen.this.renderedRegistryRow = this;
                }
            }

            private void refreshSuggestions() {
                if (!this.hasRegistryBinding()) {
                    this.closeSuggestions();
                    return;
                }

                if (this.suggestionsDismissed) {
                    if (LegacyDraftSession.sameValue(this.input.getValue(), this.dismissedValue)) {
                        this.visibleSuggestions.clear();
                        this.selectedSuggestionIndex = 0;
                        this.input.setSuggestion("");
                        return;
                    }
                    this.suggestionsDismissed = false;
                    this.dismissedValue = "";
                }

                this.visibleSuggestions.clear();
                this.visibleSuggestions.addAll(filterRegistrySuggestions(
                        KonfigConfigScreen.this.registrySuggestions(this.registryKey()),
                        this.input.getValue()
                ));
                if (this.visibleSuggestions.isEmpty()) {
                    this.selectedSuggestionIndex = 0;
                    this.input.setSuggestion("");
                    return;
                }

                this.selectedSuggestionIndex = MathHelper.clamp(this.selectedSuggestionIndex, 0, this.visibleSuggestions.size() - 1);
                this.updateInlineSuggestion();
            }

            private void activateSuggestions() {
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                this.refreshSuggestions();
            }

            private void dismissSuggestions() {
                this.suggestionsDismissed = true;
                this.dismissedValue = this.input.getValue();
                this.visibleSuggestions.clear();
                this.selectedSuggestionIndex = 0;
                this.input.setSuggestion("");
            }

            private void closeSuggestions() {
                this.suggestionsDismissed = false;
                this.dismissedValue = "";
                this.visibleSuggestions.clear();
                this.selectedSuggestionIndex = 0;
                this.input.setSuggestion("");
                if (StringListEditorScreen.this.activeRegistryRow == this) {
                    StringListEditorScreen.this.activeRegistryRow = null;
                }
            }

            private void renderSuggestions(MatrixStack guiGraphics, int mouseX, int mouseY) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                    return;
                }

                this.layoutSuggestionBox();
                AbstractGui.fill(guiGraphics, this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
                AbstractGui.fill(guiGraphics, this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

                for (int suggestionIndex = 0; suggestionIndex < this.visibleSuggestions.size(); suggestionIndex++) {
                    int rowY = this.lastDropdownY + 2 + (suggestionIndex * SUGGESTION_ROW_HEIGHT);
                    int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                    boolean suggestionHovered = suggestionIndex == this.hoveredSuggestionIndex(mouseX, mouseY);
                    if (suggestionHovered || suggestionIndex == this.selectedSuggestionIndex) {
                        AbstractGui.fill(guiGraphics, this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, suggestionHovered ? 0x80406080 : 0x50303030);
                    }
                    int textX = this.lastDropdownX + 4;
                    if (supportsRegistryIcon(this.registryKey())) {
                        renderRegistryIcon(guiGraphics, this.registryKey(), this.visibleSuggestions.get(suggestionIndex), this.lastDropdownX + 2, rowY - 1);
                        textX += 18;
                    }
                    StringListEditorScreen.this.font.draw(guiGraphics, text(this.visibleSuggestions.get(suggestionIndex)), (float) textX, (float) (rowY + 3), 0xFFFFFFFF);
                }
            }

            private boolean handleSuggestionClick(double mouseX, double mouseY) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.visibleSuggestions.isEmpty()) {
                    return false;
                }

                int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
                if (hovered < 0) {
                    return false;
                }

                this.acceptSuggestion(this.visibleSuggestions.get(hovered));
                return true;
            }

            private boolean handleSuggestionKey(int keyCode) {
                if (StringListEditorScreen.this.activeRegistryRow != this) {
                    return false;
                }
                if (keyCode == KEY_ESCAPE) {
                    this.dismissSuggestions();
                    return true;
                }
                if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER) {
                    this.dismissSuggestions();
                    return true;
                }
                if (this.visibleSuggestions.isEmpty()) {
                    return false;
                }
                if (keyCode == KEY_DOWN) {
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex + 1) % this.visibleSuggestions.size();
                    this.updateInlineSuggestion();
                    return true;
                }
                if (keyCode == KEY_UP) {
                    this.selectedSuggestionIndex = (this.selectedSuggestionIndex + this.visibleSuggestions.size() - 1) % this.visibleSuggestions.size();
                    this.updateInlineSuggestion();
                    return true;
                }
                if (keyCode == KEY_TAB) {
                    this.acceptSuggestion(this.visibleSuggestions.get(this.selectedSuggestionIndex));
                    return true;
                }
                return false;
            }

            private void acceptSuggestion(String suggestion) {
                this.suppressResponder = true;
                this.input.setValue(suggestion);
                this.suppressResponder = false;
                if (this.persistListValue(suggestion)) {
                    this.dismissSuggestions();
                    this.input.setFocus(true);
                }
            }

            private void updateInlineSuggestion() {
                if (this.visibleSuggestions.isEmpty()) {
                    this.input.setSuggestion("");
                    return;
                }
                this.input.setSuggestion(suggestionSuffix(this.input.getValue(), this.visibleSuggestions.get(this.selectedSuggestionIndex)));
            }

            private void layoutSuggestionBox() {
                this.lastDropdownX = this.lastInputX;
                this.lastDropdownWidth = this.lastInputWidth;
                this.lastDropdownHeight = (this.visibleSuggestions.size() * SUGGESTION_ROW_HEIGHT) + 4;
                int belowY = this.lastInputY + CONTROL_HEIGHT + 2;
                int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
                boolean openAbove = belowY + this.lastDropdownHeight > StringListEditorScreen.this.height - 32 && aboveY >= LIST_TOP;
                this.lastDropdownY = openAbove ? aboveY : belowY;
            }

            private int hoveredSuggestionIndex(int mouseX, int mouseY) {
                if (mouseX < this.lastDropdownX
                        || mouseX > this.lastDropdownX + this.lastDropdownWidth
                        || mouseY < this.lastDropdownY + 2
                        || mouseY > this.lastDropdownY + this.lastDropdownHeight - 2) {
                    return -1;
                }
                int suggestionIndex = (mouseY - this.lastDropdownY - 2) / SUGGESTION_ROW_HEIGHT;
                return suggestionIndex >= 0 && suggestionIndex < this.visibleSuggestions.size() ? suggestionIndex : -1;
            }

            @Override
            public List<? extends IGuiEventListener> children() {
                return Arrays.asList(this.input, this.moveUpButton, this.moveDownButton, this.removeButton);
            }

            @Override
            public IGuiEventListener getFocused() {
                return this.focused;
            }

            @Override
            public void setFocused(IGuiEventListener listener) {
                this.focused = listener;
            }

            @Override
            public boolean isDragging() {
                return this.dragging;
            }

            @Override
            public void setDragging(boolean dragging) {
                this.dragging = dragging;
            }
        }
    }

    private static final class EntryRef {
        private final LegacyConfigEntry legacyEntry;
        private final ConfigHandleImpl handle;
        private final ConfigValueImpl<?> value;
        private final ITextComponent label;
        private final ITextComponent contextLabel;
        private final String tooltip;
        private final String categoryPath;
        private final boolean editable;

        private EntryRef(LegacyConfigEntry entry) {
            this.legacyEntry = entry;
            this.handle = entry.handle();
            this.value = entry.value();
            if (value.isDecoration()) {
                this.label = text(value.inlineLabel());
                this.contextLabel = text("");
                this.tooltip = entry.tooltip();
                this.categoryPath = entry.categoryPath();
                this.editable = false;
            } else {
                this.label = translatedLabel(handle, value);
                this.contextLabel = contextLabel(handle, value);
                this.tooltip = entry.tooltip();
                this.categoryPath = entry.categoryPath();
                this.editable = entry.editable();
            }
        }

        private ITextComponent displayLabel() {
            return this.label;
        }
    }
}
