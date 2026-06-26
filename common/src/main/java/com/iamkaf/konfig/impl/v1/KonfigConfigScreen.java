//? if <=1.15.2 {
package com.iamkaf.konfig.impl.v1;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public final class KonfigConfigScreen extends Screen {
    private final Screen parent;
    private final String screenTitle;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        this(parent, modIdFilter, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter, String screenTitle) {
        super(new TextComponent(defaultScreenTitle(modIdFilter, screenTitle)));
        this.parent = parent;
        this.screenTitle = screenTitle;
    }

    private static String defaultScreenTitle(String modIdFilter, String screenTitle) {
        if (!isBlank(screenTitle)) {
            return screenTitle;
        }
        if (!isBlank(modIdFilter)) {
            return prettySegment(modIdFilter);
        }
        return "Configurations";
    }

    private static String prettySegment(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (character == '_' || character == '-' || character == '.') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
                    builder.append(' ');
                }
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext) {
                builder.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else if (Character.isUpperCase(character) && i > 0 && Character.isLowerCase(raw.charAt(i - 1))) {
                builder.append(' ').append(character);
            } else {
                builder.append(Character.toLowerCase(character));
            }
        }
        if (builder.length() > 0) {
            builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        }
        return builder.toString().trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void init() {
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTick) {
        this.renderBackground();
        drawCenteredString(this.font, this.title.getString(), this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        drawCenteredString(this.font, "Use the loader-specific config screen on 1.16.5.", this.width / 2, this.height / 2 + 4, 0xFFA0A0A0);
        super.render(mouseX, mouseY, partialTick);
    }
}
//?} elif <=1.16.5 {
package com.iamkaf.konfig.impl.v1;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public final class KonfigConfigScreen extends Screen {
    private final Screen parent;
    private final String screenTitle;

    public KonfigConfigScreen(Screen parent) {
        this(parent, null, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter) {
        this(parent, modIdFilter, null);
    }

    public KonfigConfigScreen(Screen parent, String modIdFilter, String screenTitle) {
        super(new TextComponent(defaultScreenTitle(modIdFilter, screenTitle)));
        this.parent = parent;
        this.screenTitle = screenTitle;
    }

    private static String defaultScreenTitle(String modIdFilter, String screenTitle) {
        if (!isBlank(screenTitle)) {
            return screenTitle;
        }
        if (!isBlank(modIdFilter)) {
            return prettySegment(modIdFilter);
        }
        return "Configurations";
    }

    private static String prettySegment(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(raw.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (character == '_' || character == '-' || character == '.') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != ' ') {
                    builder.append(' ');
                }
                capitalizeNext = true;
                continue;
            }

            if (capitalizeNext) {
                builder.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else if (Character.isUpperCase(character) && i > 0 && Character.isLowerCase(raw.charAt(i - 1))) {
                builder.append(' ').append(character);
            } else {
                builder.append(Character.toLowerCase(character));
            }
        }
        if (builder.length() > 0) {
            builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
        }
        return builder.toString().trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void init() {
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        drawCenteredString(poseStack, this.font, new TextComponent("Use the loader-specific config screen on 1.16.5."), this.width / 2, this.height / 2 + 4, 0xFFA0A0A0);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
}
//?} else {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigRegistryAdapter.*;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.*;
import static com.iamkaf.konfig.impl.v1.KonfigUiAdapter.*;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.core.Registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
import net.minecraft.Util;
//?}
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.net.URI;

public final class KonfigConfigScreen extends Screen {
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
    private static final int INFO_PANEL_MIN_WIDTH = 170;
    private static final int INFO_PANEL_MAX_WIDTH = 310;
    private static final int INFO_PANEL_PADDING = 16;
    private static final int INFO_PANEL_GAP = 10;
    private static final int INFO_PANEL_SCROLLBAR_WIDTH = 4;
    private static final int INFO_PANEL_SCROLL_STEP = 18;

    private final Screen parent;
    private final String modIdFilter;
    private final String screenTitle;
    private final List<EntryRef> entries;
    private final KonfigScreenSession session;
    private final Map<ResourceKey<? extends Registry<?>>, List<String>> registrySuggestionCache = new LinkedHashMap<ResourceKey<? extends Registry<?>>, List<String>>();

    private EntryList list;
    private RegistryTextInputRow activeRegistryRow;
    private RegistryTextInputRow renderedRegistryRow;
    private DropdownRow activeDropdownRow;
    private DropdownRow renderedDropdownRow;
    private EntryRef hoveredEntry;
    private EntryRef activeInfoEntry;
    private EntryRef activeDropdownOptionEntry;
    private List<InfoPanelItem> activeDropdownOptionInfo = Collections.emptyList();
    private boolean mouseOverInfoPanel;
    private boolean mouseOverInfoPanelBridge;
    private final List<InfoPanelLink> infoPanelLinks = new ArrayList<InfoPanelLink>();
    private final Map<List<InfoPanelItem>, Double> infoPanelScrollPositions = new IdentityHashMap<List<InfoPanelItem>, Double>();
    private List<InfoPanelItem> renderedInfoPanelItems = Collections.emptyList();
    private double infoPanelScroll;
    private int infoPanelMaxScroll;
    private String pendingTooltip;
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
        this.entries = collectEntries(modIdFilter);
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] creating screen parent={} modFilter={} entries={}",
                    parent == null ? "null" : parent.getClass().getName(),
                    modIdFilter == null ? "<all>" : modIdFilter,
                    this.entries.size()
            );
        }
        this.session = new KonfigScreenSession(this.entries);
    }

    private static Component defaultScreenTitle(String modIdFilter, String screenTitle) {
        if (!isBlank(screenTitle)) {
            return text(screenTitle);
        }
        if (!isBlank(modIdFilter)) {
            return translatedModTitle(modIdFilter);
        }
        return translate("konfig.screen.title.configurations");
    }

    private static Component translatedModTitle(String modId) {
        String titleKey = "konfig.config." + modId + ".title";
        Component translated = translate(titleKey);
        if (!titleKey.equals(translated.getString())) {
            return translated;
        }

        String legacyTitleKey = modId + ".configuration.title";
        translated = translate(legacyTitleKey);
        if (!legacyTitleKey.equals(translated.getString())) {
            return translated;
        }

        return text(prettySegment(modId));
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
        this.setScreen(this.parent);
    }

    private void setScreen(Screen screen) {
//? if >=26.2 {
        this.minecraft.gui.setScreen(screen);
//?} else {
        this.minecraft.setScreen(screen);
//?}
    }

    private void openInlineUrl(EntryRef entry) {
        this.openUrl(entry.value.inlineUrl());
    }

    private void openUrl(String target) {
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

//? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownClick(event)) {
            return true;
        }
        if (event.button() == 0 && this.handleInfoPanelClick(event.x(), event.y())) {
            return true;
        }
        if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionClick(event)) {
            return true;
        }

        boolean handled = super.mouseClicked(event, doubleClick);
        if (this.activeDropdownRow != null
                && !this.activeDropdownRow.isPointInsideButton(event.x(), event.y())
                && !this.activeDropdownRow.isPointInsideDropdown(event.x(), event.y())) {
            this.activeDropdownRow.closeDropdown();
        }
        RegistryTextInputRow focusedRow = this.findFocusedRegistryRow();
        if (focusedRow != null) {
            this.setActiveRegistryRow(focusedRow);
            focusedRow.activateSuggestions();
        } else if (this.activeRegistryRow != null && !this.activeRegistryRow.isPointInsideInput(event.x(), event.y())) {
            this.activeRegistryRow.closeSuggestions();
        }

        return handled;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownKey(event)) {
            return true;
        }
        DropdownRow focusedDropdown = this.findFocusedDropdownRow();
        if (focusedDropdown != null && focusedDropdown.handleClosedDropdownKey(event.key())) {
            return true;
        }
        if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionKey(event)) {
            return true;
        }
        boolean handled = super.keyPressed(event);
        if (this.activeRegistryRow != null && this.activeRegistryRow.isFocused()) {
            this.activeRegistryRow.refreshSuggestions();
        }
        return handled;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownChar(event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (this.handleInfoPanelScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
//?} else {
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

//? if >=1.20.2 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeDropdownRow != null && this.activeDropdownRow.handleDropdownScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (this.handleInfoPanelScroll(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
//?} else {
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
//?}
//?}

//? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.beginMainScreenRender(context, mouseX, mouseY);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        this.renderMainScreenChrome(context, mouseX, mouseY, () -> this.renderInfoPanel(guiGraphics, mouseX, mouseY));
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.beginMainScreenRender(context, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderMainScreenChrome(context, mouseX, mouseY, () -> this.renderInfoPanel(guiGraphics, mouseX, mouseY));
    }
//?} else {
    @Override
    public void render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.beginMainScreenRender(context, mouseX, mouseY);
//? if <=1.19.3 {
        if (this.list != null) {
            this.list.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        context.fill(0, this.height - LIST_BOTTOM_MARGIN, this.mainPanelRight(), this.height, 0xC0101010);
//?}
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderMainScreenChrome(context, mouseX, mouseY, () -> this.renderInfoPanel(guiGraphics, mouseX, mouseY));
    }
//?}

    private void beginMainScreenRender(KonfigRenderContext context, int mouseX, int mouseY) {
        this.renderedRegistryRow = null;
        this.renderedDropdownRow = null;
        this.hoveredEntry = null;
        this.pendingTooltip = null;
        this.mouseOverInfoPanel = this.isPointInInfoPanel(mouseX, mouseY);
        this.mouseOverInfoPanelBridge = this.isPointInInfoPanelBridge(mouseX, mouseY);
        this.infoPanelLinks.clear();
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void renderMainScreenChrome(KonfigRenderContext context, int mouseX, int mouseY, Runnable infoPanelRenderer) {
        context.drawCenteredText(this.font, screenTitle(), this.width / 2, 8, 0xFFFFFFFF);
        context.fill(this.mainPanelRight(), LIST_TOP, this.mainPanelRight() + 1, this.height, 0xFF202020);
        infoPanelRenderer.run();
        if (this.entries.isEmpty()) {
            context.drawCenteredText(this.font, translate("konfig.screen.empty"), this.mainPanelRight() / 2, this.height / 2 - 10, 0xFFC0C0C0);
        }
        // Painter order matters here: side panels first, floating controls next, queued tooltips last.
        context.renderFloatingLayers(
                layer -> {
                    if (this.renderedRegistryRow != null) {
                        this.renderedRegistryRow.renderSuggestions(layer, mouseX, mouseY);
                    }
                    if (this.renderedDropdownRow != null) {
                        this.renderedDropdownRow.renderDropdown(layer, mouseX, mouseY);
                    }
                },
                this::renderPendingTooltip
        );
    }

    void queueTooltip(String tooltip, int mouseX, int mouseY) {
        this.pendingTooltip = tooltip;
        this.pendingTooltipMouseX = mouseX;
        this.pendingTooltipMouseY = mouseY;
    }

    private void renderPendingTooltip(KonfigRenderContext context) {
        context.renderTooltipNow(this, this.font, this.pendingTooltip, this.pendingTooltipMouseX, this.pendingTooltipMouseY);
    }

    private void rebuildScreenWidgets() {
        this.clearWidgets();
        this.activeDropdownRow = null;
        this.renderedDropdownRow = null;

        int listHeight = Math.max(48, this.height - LIST_TOP - LIST_BOTTOM_MARGIN);
//? if <=1.19.3 {
        this.list = this.addWidget(new EntryList(this.minecraft, this.mainPanelRight(), listHeight, LIST_TOP));
//?} else {
        this.list = this.addRenderableWidget(new EntryList(this.minecraft, this.mainPanelRight(), listHeight, LIST_TOP));
//?}
        for (EntryRef entry : this.entries) {
            this.list.addKonfigEntry(createRow(entry));
        }

        int footerY = this.height - 26;
        int footerCenter = this.mainPanelRight() / 2;
        this.addRenderableWidget(button(footerCenter - 82, footerY, 80, 20, translate("konfig.screen.reset"), button -> this.resetEntries()));
        this.addRenderableWidget(button(footerCenter + 2, footerY, 80, 20, translate("konfig.screen.done"), button -> this.onClose()));
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
        try {
            this.session.persist(entry);
            return true;
        } catch (RuntimeException exception) {
            KonfigToastSupport.saveFailed(exceptionMessage(exception));
            return false;
        }
    }

    private static String exceptionMessage(Exception exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }

    private void resetEntries() {
        try {
            this.session.resetAll();
        } catch (RuntimeException exception) {
            KonfigToastSupport.resetFailed(exceptionMessage(exception));
        }
        this.rebuildScreenWidgets();
    }

    private int infoPanelWidth() {
        if (this.width < 520) {
            return Math.max(120, this.width / 3);
        }
        return Mth.clamp(this.width / 3, INFO_PANEL_MIN_WIDTH, INFO_PANEL_MAX_WIDTH);
    }

    private int mainPanelRight() {
        return this.width - this.infoPanelWidth();
    }

    private void updateHoveredEntry(EntryRef entry, boolean hovered) {
        if (hovered) {
            this.hoveredEntry = entry;
            this.activeInfoEntry = entry;
        }
    }

    private List<InfoPanelItem> activeInfoItems() {
        if (this.activeDropdownOptionEntry != null && !this.activeDropdownOptionInfo.isEmpty()) {
            return this.activeDropdownOptionInfo;
        }

        EntryRef hovered = this.hoveredEntry;
        if (hovered == null && (this.mouseOverInfoPanel || this.mouseOverInfoPanelBridge)) {
            hovered = this.activeInfoEntry;
        }
        if (hovered != null) {
            List<InfoPanelItem> selectedDropdownInfo = this.selectedDropdownOptionInfo(hovered);
            if (!selectedDropdownInfo.isEmpty()) {
                return selectedDropdownInfo;
            }
            List<InfoPanelItem> entryInfo = hovered.handle.entryInfo(hovered.value.path());
            if (!entryInfo.isEmpty()) {
                return entryInfo;
            }
            if (!isBlank(hovered.categoryPath)) {
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

    private void updateActiveDropdownOptionInfo(int mouseX, int mouseY) {
        this.activeDropdownOptionEntry = null;
        this.activeDropdownOptionInfo = Collections.emptyList();
        if (this.activeDropdownRow == null) {
            return;
        }

        DropdownOptionMetadata option = this.activeDropdownRow.activeInfoOption(mouseX, mouseY);
        if (option == null || option.info().isEmpty()) {
            return;
        }

        this.activeDropdownOptionEntry = this.activeDropdownRow.entry;
        this.activeDropdownOptionInfo = option.info();
    }

    private List<InfoPanelItem> selectedDropdownOptionInfo(EntryRef entry) {
        if (entry.value.kind() != EntryKind.DROPDOWN) {
            return Collections.emptyList();
        }
        if (this.activeDropdownRow != null && this.activeDropdownRow.entry == entry) {
            return Collections.emptyList();
        }

        DropdownOptionMetadata option = entry.value.dropdownOption(this.currentDropdownValue(entry.value));
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
                this.openUrl(link.target);
                return true;
            }
        }
        return false;
    }

    private boolean handleInfoPanelScroll(double mouseX, double mouseY, double scrollY) {
        if (!this.isPointInInfoPanel(mouseX, mouseY) || this.infoPanelMaxScroll <= 0) {
            return false;
        }
        this.infoPanelScroll = Mth.clamp(this.infoPanelScroll - (scrollY * INFO_PANEL_SCROLL_STEP), 0.0D, (double) this.infoPanelMaxScroll);
        this.rememberInfoPanelScroll();
        return true;
    }

//? if >=26.1 {
    private void renderInfoPanel(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int left = this.mainPanelRight() + 1;
        int right = this.width;
        int top = LIST_TOP;
        int bottom = this.height;
        fillRect(guiGraphics, left, top, right, bottom, 0x22000000);

        this.updateActiveDropdownOptionInfo(mouseX, mouseY);
        List<InfoPanelItem> items = this.activeInfoItems();
        if (items.isEmpty()) {
            this.setRenderedInfoPanelItems(Collections.emptyList());
            this.infoPanelScroll = 0.0D;
            this.infoPanelMaxScroll = 0;
            return;
        }

        this.setRenderedInfoPanelItems(items);

        int x = left + INFO_PANEL_PADDING;
        int viewportTop = top + INFO_PANEL_PADDING;
        int viewportBottom = bottom - INFO_PANEL_PADDING;
        int contentWidth = Math.max(20, right - left - (INFO_PANEL_PADDING * 2) - INFO_PANEL_SCROLLBAR_WIDTH - 4);
        int contentHeight = this.measureInfoPanelItems(items, contentWidth);
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        this.infoPanelMaxScroll = Math.max(0, contentHeight - viewportHeight);
        this.infoPanelScroll = Mth.clamp(this.infoPanelScroll, 0.0D, (double) this.infoPanelMaxScroll);
        this.rememberInfoPanelScroll();

        int y = viewportTop - (int) Math.round(this.infoPanelScroll);
        guiGraphics.enableScissor(left, viewportTop, right, viewportBottom);
        for (InfoPanelItem item : items) {
            y = this.renderInfoPanelItem(guiGraphics, item, x, y, contentWidth, mouseX, mouseY);
        }
        guiGraphics.disableScissor();

        this.renderInfoPanelScrollbar(guiGraphics, right, viewportTop, viewportBottom);
    }

    private int renderInfoPanelItem(GuiGraphicsExtractor guiGraphics, InfoPanelItem item, int x, int y, int width, int mouseX, int mouseY) {
        if (item.kind == EntryKind.HEADER) {
            drawText(guiGraphics, this.font, infoLabel(item), x, y, 0xFFFFFFFF);
            return y + 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.renderInfoImage(guiGraphics, item, x, y, width);
        }
        if (item.kind == EntryKind.URL) {
            Component label = text(infoText(item) + " >");
            int linkWidth = this.font.width(label);
            InfoPanelLink link = new InfoPanelLink(x, y, Math.min(width, linkWidth), this.font.lineHeight, item.target);
            this.infoPanelLinks.add(link);
            boolean hovered = link.contains(mouseX, mouseY);
            drawText(guiGraphics, this.font, label, x, y, hovered ? 0xFFFFFFFF : 0xFF80C8FF);
            if (hovered) {
                fillRect(guiGraphics, x, y + this.font.lineHeight, x + Math.min(width, linkWidth), y + this.font.lineHeight + 1, 0xFFFFFFFF);
            }
            return y + 16;
        }
        return this.renderInfoParagraph(guiGraphics, infoText(item), x, y, width, 0xFFCFCFCF) + INFO_PANEL_GAP;
    }

    private int renderInfoImage(GuiGraphicsExtractor guiGraphics, InfoPanelItem item, int x, int y, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(options.width(), width - (options.padding() * 2)));
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

    private void renderInfoPanelScrollbar(GuiGraphicsExtractor guiGraphics, int right, int top, int bottom) {
        if (this.infoPanelMaxScroll <= 0) {
            return;
        }

        int trackLeft = right - INFO_PANEL_SCROLLBAR_WIDTH - 4;
        int trackRight = right - 4;
        int viewportHeight = Math.max(1, bottom - top);
        int contentHeight = viewportHeight + this.infoPanelMaxScroll;
        int thumbHeight = Mth.clamp((viewportHeight * viewportHeight) / contentHeight, 18, viewportHeight);
        int thumbTop = top + (int) Math.round((viewportHeight - thumbHeight) * (this.infoPanelScroll / (double) this.infoPanelMaxScroll));
        fillRect(guiGraphics, trackLeft, top, trackRight, bottom, 0x44000000);
        fillRect(guiGraphics, trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0xAAFFFFFF);
    }

    private int renderInfoParagraph(GuiGraphicsExtractor guiGraphics, String value, int x, int y, int width, int color) {
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                y += 8;
                continue;
            }
            y = this.renderWrappedLines(guiGraphics, paragraph.trim(), x, y, width, color) + 4;
        }
        return y;
    }
//?} elif >=1.20 {
    private void renderInfoPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int left = this.mainPanelRight() + 1;
        int right = this.width;
        int top = LIST_TOP;
        int bottom = this.height;
        fillRect(guiGraphics, left, top, right, bottom, 0x22000000);

        this.updateActiveDropdownOptionInfo(mouseX, mouseY);
        List<InfoPanelItem> items = this.activeInfoItems();
        if (items.isEmpty()) {
            this.setRenderedInfoPanelItems(Collections.emptyList());
            this.infoPanelScroll = 0.0D;
            this.infoPanelMaxScroll = 0;
            return;
        }

        this.setRenderedInfoPanelItems(items);

        int x = left + INFO_PANEL_PADDING;
        int viewportTop = top + INFO_PANEL_PADDING;
        int viewportBottom = bottom - INFO_PANEL_PADDING;
        int contentWidth = Math.max(20, right - left - (INFO_PANEL_PADDING * 2) - INFO_PANEL_SCROLLBAR_WIDTH - 4);
        int contentHeight = this.measureInfoPanelItems(items, contentWidth);
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        this.infoPanelMaxScroll = Math.max(0, contentHeight - viewportHeight);
        this.infoPanelScroll = Mth.clamp(this.infoPanelScroll, 0.0D, (double) this.infoPanelMaxScroll);
        this.rememberInfoPanelScroll();

        int y = viewportTop - (int) Math.round(this.infoPanelScroll);
        guiGraphics.enableScissor(left, viewportTop, right, viewportBottom);
        for (InfoPanelItem item : items) {
            y = this.renderInfoPanelItem(guiGraphics, item, x, y, contentWidth, mouseX, mouseY);
        }
        guiGraphics.disableScissor();

        this.renderInfoPanelScrollbar(guiGraphics, right, viewportTop, viewportBottom);
    }

    private int renderInfoPanelItem(GuiGraphics guiGraphics, InfoPanelItem item, int x, int y, int width, int mouseX, int mouseY) {
        if (item.kind == EntryKind.HEADER) {
            drawText(guiGraphics, this.font, infoLabel(item), x, y, 0xFFFFFFFF);
            return y + 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.renderInfoImage(guiGraphics, item, x, y, width);
        }
        if (item.kind == EntryKind.URL) {
            Component label = text(infoText(item) + " >");
            int linkWidth = this.font.width(label);
            InfoPanelLink link = new InfoPanelLink(x, y, Math.min(width, linkWidth), this.font.lineHeight, item.target);
            this.infoPanelLinks.add(link);
            boolean hovered = link.contains(mouseX, mouseY);
            drawText(guiGraphics, this.font, label, x, y, hovered ? 0xFFFFFFFF : 0xFF80C8FF);
            if (hovered) {
                fillRect(guiGraphics, x, y + this.font.lineHeight, x + Math.min(width, linkWidth), y + this.font.lineHeight + 1, 0xFFFFFFFF);
            }
            return y + 16;
        }
        return this.renderInfoParagraph(guiGraphics, infoText(item), x, y, width, 0xFFCFCFCF) + INFO_PANEL_GAP;
    }

    private int renderInfoImage(GuiGraphics guiGraphics, InfoPanelItem item, int x, int y, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(options.width(), width - (options.padding() * 2)));
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

    private void renderInfoPanelScrollbar(GuiGraphics guiGraphics, int right, int top, int bottom) {
        if (this.infoPanelMaxScroll <= 0) {
            return;
        }

        int trackLeft = right - INFO_PANEL_SCROLLBAR_WIDTH - 4;
        int trackRight = right - 4;
        int viewportHeight = Math.max(1, bottom - top);
        int contentHeight = viewportHeight + this.infoPanelMaxScroll;
        int thumbHeight = Mth.clamp((viewportHeight * viewportHeight) / contentHeight, 18, viewportHeight);
        int thumbTop = top + (int) Math.round((viewportHeight - thumbHeight) * (this.infoPanelScroll / (double) this.infoPanelMaxScroll));
        fillRect(guiGraphics, trackLeft, top, trackRight, bottom, 0x44000000);
        fillRect(guiGraphics, trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0xAAFFFFFF);
    }

    private int renderInfoParagraph(GuiGraphics guiGraphics, String value, int x, int y, int width, int color) {
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                y += 8;
                continue;
            }
            y = this.renderWrappedLines(guiGraphics, paragraph.trim(), x, y, width, color) + 4;
        }
        return y;
    }
//?} else {
    private void renderInfoPanel(PoseStack guiGraphics, int mouseX, int mouseY) {
        int left = this.mainPanelRight() + 1;
        int right = this.width;
        int top = LIST_TOP;
        int bottom = this.height;
        fillRect(guiGraphics, left, top, right, bottom, 0x22000000);

        this.updateActiveDropdownOptionInfo(mouseX, mouseY);
        List<InfoPanelItem> items = this.activeInfoItems();
        if (items.isEmpty()) {
            this.setRenderedInfoPanelItems(Collections.emptyList());
            this.infoPanelScroll = 0.0D;
            this.infoPanelMaxScroll = 0;
            return;
        }

        this.setRenderedInfoPanelItems(items);

        int x = left + INFO_PANEL_PADDING;
        int viewportTop = top + INFO_PANEL_PADDING;
        int viewportBottom = bottom - INFO_PANEL_PADDING;
        int contentWidth = Math.max(20, right - left - (INFO_PANEL_PADDING * 2) - INFO_PANEL_SCROLLBAR_WIDTH - 4);
        int contentHeight = this.measureInfoPanelItems(items, contentWidth);
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        this.infoPanelMaxScroll = Math.max(0, contentHeight - viewportHeight);
        this.infoPanelScroll = Mth.clamp(this.infoPanelScroll, 0.0D, (double) this.infoPanelMaxScroll);
        this.rememberInfoPanelScroll();

        int y = viewportTop - (int) Math.round(this.infoPanelScroll);
        this.enableInfoPanelScissor(left, viewportTop, right, viewportBottom);
        for (InfoPanelItem item : items) {
            y = this.renderInfoPanelItem(guiGraphics, item, x, y, contentWidth, mouseX, mouseY);
        }
        this.disableInfoPanelScissor();

        this.renderInfoPanelScrollbar(guiGraphics, right, viewportTop, viewportBottom);
    }

    private int renderInfoPanelItem(PoseStack guiGraphics, InfoPanelItem item, int x, int y, int width, int mouseX, int mouseY) {
        if (item.kind == EntryKind.HEADER) {
            drawText(guiGraphics, this.font, infoLabel(item), x, y, 0xFFFFFFFF);
            return y + 16;
        }
        if (item.kind == EntryKind.IMAGE) {
            return this.renderInfoImage(guiGraphics, item, x, y, width);
        }
        if (item.kind == EntryKind.URL) {
            Component label = text(infoText(item) + " >");
            int linkWidth = this.font.width(label);
            InfoPanelLink link = new InfoPanelLink(x, y, Math.min(width, linkWidth), this.font.lineHeight, item.target);
            this.infoPanelLinks.add(link);
            boolean hovered = link.contains(mouseX, mouseY);
            drawText(guiGraphics, this.font, label, x, y, hovered ? 0xFFFFFFFF : 0xFF80C8FF);
            if (hovered) {
                fillRect(guiGraphics, x, y + this.font.lineHeight, x + Math.min(width, linkWidth), y + this.font.lineHeight + 1, 0xFFFFFFFF);
            }
            return y + 16;
        }
        return this.renderInfoParagraph(guiGraphics, infoText(item), x, y, width, 0xFFCFCFCF) + INFO_PANEL_GAP;
    }

    private int renderInfoImage(PoseStack guiGraphics, InfoPanelItem item, int x, int y, int width) {
        ImageOptions options = item.imageOptions;
        int imageWidth = Math.max(1, Math.min(options.width(), width - (options.padding() * 2)));
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

    private void renderInfoPanelScrollbar(PoseStack guiGraphics, int right, int top, int bottom) {
        if (this.infoPanelMaxScroll <= 0) {
            return;
        }

        int trackLeft = right - INFO_PANEL_SCROLLBAR_WIDTH - 4;
        int trackRight = right - 4;
        int viewportHeight = Math.max(1, bottom - top);
        int contentHeight = viewportHeight + this.infoPanelMaxScroll;
        int thumbHeight = Mth.clamp((viewportHeight * viewportHeight) / contentHeight, 18, viewportHeight);
        int thumbTop = top + (int) Math.round((viewportHeight - thumbHeight) * (this.infoPanelScroll / (double) this.infoPanelMaxScroll));
        fillRect(guiGraphics, trackLeft, top, trackRight, bottom, 0x44000000);
        fillRect(guiGraphics, trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, 0xAAFFFFFF);
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

    private int renderInfoParagraph(PoseStack guiGraphics, String value, int x, int y, int width, int color) {
        for (String paragraph : value.replace('\r', '\n').split("\\n")) {
            if (paragraph.trim().isEmpty()) {
                y += 8;
                continue;
            }
            y = this.renderWrappedLines(guiGraphics, paragraph.trim(), x, y, width, color) + 4;
        }
        return y;
    }
//?}

    private void setRenderedInfoPanelItems(List<InfoPanelItem> items) {
        if (items == this.renderedInfoPanelItems) {
            return;
        }
        this.rememberInfoPanelScroll();
        this.renderedInfoPanelItems = items;
        Double rememberedScroll = this.infoPanelScrollPositions.get(items);
        this.infoPanelScroll = rememberedScroll == null ? 0.0D : rememberedScroll.doubleValue();
    }

    private void rememberInfoPanelScroll() {
        if (!this.renderedInfoPanelItems.isEmpty()) {
            this.infoPanelScrollPositions.put(this.renderedInfoPanelItems, this.infoPanelScroll);
        }
    }

    private static Component infoLabel(InfoPanelItem item) {
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
        int imageWidth = Math.max(1, Math.min(options.width(), width - (options.padding() * 2)));
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
            height += this.font.split(text(paragraph.trim()), Math.max(1, width)).size() * this.font.lineHeight;
            height += 4;
        }
        return height;
    }

//? if >=26.1 {
    private int renderWrappedLines(GuiGraphicsExtractor guiGraphics, String value, int x, int y, int width, int color) {
//?} elif >=1.20 {
    private int renderWrappedLines(GuiGraphics guiGraphics, String value, int x, int y, int width, int color) {
//?} else {
    private int renderWrappedLines(PoseStack guiGraphics, String value, int x, int y, int width, int color) {
//?}
        List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(text(value), Math.max(1, width));
        for (net.minecraft.util.FormattedCharSequence line : lines) {
//? if >=26.1 {
            guiGraphics.text(this.font, line, x, y, color);
//?} elif >=1.20 {
            guiGraphics.drawString(this.font, line, x, y, color);
//?} else {
            this.font.draw(guiGraphics, line, (float) x, (float) y, color);
//?}
            y += this.font.lineHeight;
        }
        return y;
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

    private Component screenTitle() {
        if (!isBlank(this.screenTitle)) {
            return text(this.screenTitle);
        }
        return this.title;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean readBoolean(ConfigValueImpl<?> value) {
        return this.session.readBoolean(value);
    }

    private Enum<?> currentEnum(ConfigValueImpl<?> value) {
        return this.session.currentEnum(value);
    }

    private Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        return this.session.nextEnum(value);
    }

    private int currentColor(ConfigValueImpl<?> value) {
        return this.session.currentColor(value);
    }

    private List<String> currentStringList(ConfigValueImpl<?> value) {
        return this.session.currentStringList(value);
    }

    private String currentDropdownValue(ConfigValueImpl<?> value) {
        return this.session.currentDropdownValue(value);
    }

    private Component booleanText(ConfigValueImpl<?> value) {
        return CommonComponents.optionStatus(readBoolean(value));
    }

    private Component enumText(EntryRef entry, Enum<?> value) {
        return translatedEnumValue(entry, value);
    }

    private Component colorText(ConfigValueImpl<?> value) {
        int color = currentColor(value);
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return text(ColorValueHelper.formatArgb(color));
        }
        return text(ColorValueHelper.formatRgb(color));
    }

    private Component stringListText(ConfigValueImpl<?> value) {
        List<String> values = currentStringList(value);
        if (values.isEmpty()) {
            return translate("konfig.screen.list.empty");
        }
        if (values.size() == 1) {
            return text(values.get(0));
        }
        if (values.size() == 2) {
            return text(values.get(0) + ", " + values.get(1));
        }
        return translate("konfig.screen.list.summary", values.get(0), Integer.valueOf(values.size() - 1));
    }

    private Component dropdownText(EntryRef entry, String option) {
        DropdownOptionMetadata metadata = entry.value.dropdownOption(option);
        return metadata == null ? translatedDropdownValue(entry, option) : translatedDropdownOption(entry, metadata);
    }

    private String currentStringValue(ConfigValueImpl<?> value) {
        return this.session.currentStringValue(value);
    }

    private RegistryTextInputRow findFocusedRegistryRow() {
        if (this.list == null) {
            return null;
        }
        for (ConfigRow row : this.list.children()) {
            if (row instanceof RegistryTextInputRow registryRow && registryRow.isFocused()) {
                return registryRow;
            }
        }
        return null;
    }

    private DropdownRow findFocusedDropdownRow() {
        if (this.list == null || this.activeDropdownRow != null) {
            return null;
        }
        for (ConfigRow row : this.list.children()) {
            if (row instanceof DropdownRow dropdownRow && dropdownRow.isButtonFocused()) {
                return dropdownRow;
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

    private List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        List<String> cached = this.registrySuggestionCache.get(registryKey);
        if (cached != null) {
            return cached;
        }

        List<String> values = new ArrayList<String>();
        Registry<?> registry = builtInRegistry(registryKey);
        if (registry != null) {
            for (Object key : registry.keySet()) {
                values.add(String.valueOf(key));
            }
            Collections.sort(values);
        }

        List<String> immutable = Collections.unmodifiableList(values);
        this.registrySuggestionCache.put(registryKey, immutable);
        return immutable;
    }


    private final class EntryList extends ContainerObjectSelectionList<ConfigRow> {
        private EntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y) {
//? if >=1.20.3 {
            super(minecraft, width, height, y, ROW_HEIGHT);
//?} else {
            super(minecraft, width, KonfigConfigScreen.this.height, y, y + height, ROW_HEIGHT);
//?}
//? if <=1.16.3 {
            this.setRenderHeader(false, 0);
//?} elif <=1.20.4 {
            this.setRenderBackground(false);
//?}
        }

        private void addKonfigEntry(ConfigRow row) {
//? if >=26.1 {
            super.addEntry(row, row.preferredHeight(this.getRowWidth()));
//?} else {
            super.addEntry(row);
//?}
        }

        @Override
        public int getRowWidth() {
            return KonfigConfigScreen.this.mainPanelRight() - 28;
        }

//? if <=1.20.6 {
        @Override
        protected int getScrollbarPosition() {
//? if >=1.20.3 {
            return this.getRight() - 6;
//?} else {
            return this.x1 - 6;
//?}
        }
//?}

//? if >=26.1 {
        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            fillRect(guiGraphics, this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x66000000);
            super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
//?} elif >=1.20.3 {
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            fillRect(guiGraphics, this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x66000000);
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
//?} elif >=1.20 {
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            fillRect(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
//?} else {
        @Override
        protected void renderBackground(PoseStack guiGraphics) {
            fillRect(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
        }
//?}
    }

    private abstract class ConfigRow extends ContainerObjectSelectionList.Entry<ConfigRow> {
        protected final EntryRef entry;

        private ConfigRow(EntryRef entry) {
            this.entry = entry;
        }

        protected abstract AbstractWidget control();

        protected void tick() {
        }

        protected int preferredHeight(int rowWidth) {
            return ROW_HEIGHT;
        }

        protected String validationMessage() {
            return "";
        }

        protected String rowTooltip() {
            return this.entry.tooltip;
        }

        protected final RowLayout rowLayout(int x, int y, int width, int height) {
            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            return new RowLayout(x, y, width, height, controlWidth, x + width - controlWidth, y + (height - CONTROL_HEIGHT) / 2);
        }

        protected final void renderStandardRow(KonfigRenderContext context, RowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom, int labelColor) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                context.fill(layout.x, layout.y, layout.x + layout.width, layout.y + layout.height, 0x22000000);
            }

            context.showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, this.rowTooltip(), mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);
            layoutControl(this.control(), layout.controlX, layout.controlY, layout.controlWidth);
            context.drawText(KonfigConfigScreen.this.font, this.entry.contextLabel, layout.x + 4, layout.y + 1, 0xFFA0A0A0);
            context.drawText(KonfigConfigScreen.this.font, this.entry.displayLabel(), layout.x + 4, layout.y + 12, labelColor);
            context.renderWidget(this.control(), mouseX, mouseY, partialTick);
            if (!this.validationMessage().isEmpty()) {
                context.drawText(KonfigConfigScreen.this.font, text(this.validationMessage()), layout.controlX, layout.controlY + CONTROL_HEIGHT + 2, VALIDATION_COLOR);
            }
        }

        protected final void renderColorRow(KonfigRenderContext context, RowLayout layout, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom, int previewX, int previewY) {
            this.renderStandardRow(context, layout, mouseX, mouseY, hovered, partialTick, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom, 0xFFFFFFFF);
            context.drawColorSwatch(previewX, previewY, ColorRow.PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
//?} elif >=1.20 {
        @Override
        public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
        }

        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(x, y, width, height);
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
//?} else {
        @Override
        public void render(PoseStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
        }

        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(x, y, width, height);
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, this.entry.editable ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
//?}

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.control());
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(this.control());
        }

        protected final void layoutControl(AbstractWidget control, int x, int y, int width) {
//? if >=1.19.3 {
            control.setX(x);
            control.setY(y);
//?} else {
            control.x = x;
            control.y = y;
//?}
            control.setWidth(width);
        }

        protected void revertDraft(Object previousValue) {
            KonfigConfigScreen.this.session.setDraft(this.entry.value, previousValue);
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
            this.button = button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, translate("konfig.screen.unsupported"), ignored -> {
            });
            this.button.active = false;
        }

        @Override
        protected AbstractWidget control() {
            return this.button;
        }
    }

    private abstract class DecorationRow extends ConfigRow {
        private final Button spacer;

        private DecorationRow(EntryRef entry) {
            super(entry);
            this.spacer = button(0, 0, 0, 0, text(""), ignored -> {});
            this.spacer.visible = false;
            this.spacer.active = false;
        }

        @Override
        protected final AbstractWidget control() {
            return this.spacer;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.emptyList();
        }
    }

    private final class HeaderRow extends DecorationRow {
        private HeaderRow(EntryRef entry) {
            super(entry);
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            fillRect(guiGraphics, x, y + 4, x + width, y + height - 4, 0x552B3550);
            drawCenteredText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), x + (width / 2), y + 10, 0xFFF8E38F);
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            fillRect(guiGraphics, x, y + 4, x + width, y + height - 4, 0x552B3550);
            drawCenteredText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), x + (width / 2), y + 10, 0xFFF8E38F);
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, x, y, x + width, y + height);
            fillRect(guiGraphics, x, y + 4, x + width, y + height - 4, 0x552B3550);
            drawCenteredText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), x + (width / 2), y + 10, 0xFFF8E38F);
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, x, y, x + width, y + height);
            fillRect(guiGraphics, x, y + 4, x + width, y + height - 4, 0x552B3550);
            drawCenteredText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), x + (width / 2), y + 10, 0xFFF8E38F);
        }
//?}
    }

    private final class ImageRow extends DecorationRow {
        private ImageRow(EntryRef entry) {
            super(entry);
        }

        private boolean hasCaption() {
            return !KonfigScreenSupport.isBlank(this.entry.value.inlineLabel()) && this.entry.value.imageOptions().captionPosition() != ImageOptions.CaptionPosition.NONE;
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
            return new int[] {
                    Math.max(1, (int) Math.round(options.width() * scale)),
                    Math.max(1, (int) Math.round(options.height() * scale))
            };
        }

        private int contentWidth(int imageWidth) {
            ImageOptions options = this.entry.value.imageOptions();
            if (this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                return imageWidth + 8 + this.captionWidth();
            }
            return imageWidth;
        }

        private int contentHeight(int imageHeight) {
            ImageOptions options = this.entry.value.imageOptions();
            if (this.hasCaption() && options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                return imageHeight + 12;
            }
            return imageHeight;
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

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            this.renderImageRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered);
        }

        private void renderImageRow(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            int[] imageSize = imageSize(width, height);
            int contentWidth = contentWidth(imageSize[0]);
            int contentHeight = contentHeight(imageSize[1]);
            int imageX = imageX(x, width, contentWidth);
            int imageY = imageY(y, height, contentHeight);
            drawImage(guiGraphics, this.entry.value.inlineTarget(), imageX, imageY, imageSize[0], imageSize[1], this.entry.value.imageOptions().width(), this.entry.value.imageOptions().height());
            if (this.hasCaption()) {
                ImageOptions options = this.entry.value.imageOptions();
                if (options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + imageSize[0] + 8, imageY + Math.max(0, (imageSize[1] - 8) / 2), 0xFFCFCFCF);
                } else if (options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + Math.max(0, (imageSize[0] - this.captionWidth()) / 2), imageY + imageSize[1] + 2, 0xFFCFCFCF);
                }
            }
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            this.renderImageRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered);
        }

        private void renderImageRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            int[] imageSize = imageSize(width, height);
            int contentWidth = contentWidth(imageSize[0]);
            int contentHeight = contentHeight(imageSize[1]);
            int imageX = imageX(x, width, contentWidth);
            int imageY = imageY(y, height, contentHeight);
            drawImage(guiGraphics, this.entry.value.inlineTarget(), imageX, imageY, imageSize[0], imageSize[1], this.entry.value.imageOptions().width(), this.entry.value.imageOptions().height());
            if (this.hasCaption()) {
                ImageOptions options = this.entry.value.imageOptions();
                if (options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + imageSize[0] + 8, imageY + Math.max(0, (imageSize[1] - 8) / 2), 0xFFCFCFCF);
                } else if (options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + Math.max(0, (imageSize[0] - this.captionWidth()) / 2), imageY + imageSize[1] + 2, 0xFFCFCFCF);
                }
            }
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderImageRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered);
        }

        private void renderImageRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, x, y, x + width, y + height);
            int[] imageSize = imageSize(width, height);
            int contentWidth = contentWidth(imageSize[0]);
            int contentHeight = contentHeight(imageSize[1]);
            int imageX = imageX(x, width, contentWidth);
            int imageY = imageY(y, height, contentHeight);
            drawImage(guiGraphics, this.entry.value.inlineTarget(), imageX, imageY, imageSize[0], imageSize[1], this.entry.value.imageOptions().width(), this.entry.value.imageOptions().height());
            if (this.hasCaption()) {
                ImageOptions options = this.entry.value.imageOptions();
                if (options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + imageSize[0] + 8, imageY + Math.max(0, (imageSize[1] - 8) / 2), 0xFFCFCFCF);
                } else if (options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + Math.max(0, (imageSize[0] - this.captionWidth()) / 2), imageY + imageSize[1] + 2, 0xFFCFCFCF);
                }
            }
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, x, y, x + width, y + height);
            int[] imageSize = imageSize(width, height);
            int contentWidth = contentWidth(imageSize[0]);
            int contentHeight = contentHeight(imageSize[1]);
            int imageX = imageX(x, width, contentWidth);
            int imageY = imageY(y, height, contentHeight);
            drawImage(guiGraphics, this.entry.value.inlineTarget(), imageX, imageY, imageSize[0], imageSize[1], this.entry.value.imageOptions().width(), this.entry.value.imageOptions().height());
            if (this.hasCaption()) {
                ImageOptions options = this.entry.value.imageOptions();
                if (options.captionPosition() == ImageOptions.CaptionPosition.RIGHT) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + imageSize[0] + 8, imageY + Math.max(0, (imageSize[1] - 8) / 2), 0xFFCFCFCF);
                } else if (options.captionPosition() == ImageOptions.CaptionPosition.BELOW) {
                    drawText(guiGraphics, KonfigConfigScreen.this.font, this.entry.displayLabel(), imageX + Math.max(0, (imageSize[0] - this.captionWidth()) / 2), imageY + imageSize[1] + 2, 0xFFCFCFCF);
                }
            }
        }
//?}
    }

    private final class InlineTextRow extends DecorationRow {
        private InlineTextRow(EntryRef entry) {
            super(entry);
        }

        @Override
        protected int preferredHeight(int rowWidth) {
            int textWidth = Math.max(1, rowWidth - 20);
            int lineCount = KonfigConfigScreen.this.font.split(this.entry.displayLabel(), textWidth).size();
            return Math.max(ROW_HEIGHT, (lineCount * KonfigConfigScreen.this.font.lineHeight) + 16);
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            List<net.minecraft.util.FormattedCharSequence> lines = KonfigConfigScreen.this.font.split(this.entry.displayLabel(), Math.max(1, width - 16));
            int textY = y + Math.max(4, (height - (lines.size() * KonfigConfigScreen.this.font.lineHeight)) / 2);
            for (net.minecraft.util.FormattedCharSequence line : lines) {
                guiGraphics.text(KonfigConfigScreen.this.font, line, x + 8, textY, 0xFFCFCFCF);
                textY += KonfigConfigScreen.this.font.lineHeight;
            }
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());
            int lineCount = KonfigConfigScreen.this.font.split(this.entry.displayLabel(), Math.max(1, width - 16)).size();
            int textY = y + Math.max(4, (height - (lineCount * KonfigConfigScreen.this.font.lineHeight)) / 2);
            KonfigConfigScreen.this.renderWrappedLines(guiGraphics, this.entry.displayLabel().getString(), x + 8, textY, Math.max(1, width - 16), 0xFFCFCFCF);
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, x, y, x + width, y + height);
            int lineCount = KonfigConfigScreen.this.font.split(this.entry.displayLabel(), Math.max(1, width - 16)).size();
            int textY = y + Math.max(4, (height - (lineCount * KonfigConfigScreen.this.font.lineHeight)) / 2);
            KonfigConfigScreen.this.renderWrappedLines(guiGraphics, this.entry.displayLabel().getString(), x + 8, textY, Math.max(1, width - 16), 0xFFCFCFCF);
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                fillRect(guiGraphics, x, y, x + width, y + height, 0x16000000);
            }
            showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, guiGraphics, this.entry.tooltip, mouseX, mouseY, x, y, x + width, y + height);
            List<net.minecraft.util.FormattedCharSequence> lines = KonfigConfigScreen.this.font.split(this.entry.displayLabel(), Math.max(1, width - 16));
            int textY = y + Math.max(4, (height - (lines.size() * KonfigConfigScreen.this.font.lineHeight)) / 2);
            KonfigConfigScreen.this.renderWrappedLines(guiGraphics, this.entry.displayLabel().getString(), x + 8, textY, Math.max(1, width - 16), 0xFFCFCFCF);
        }
//?}
    }

    private final class UrlRow extends ConfigRow {
        private final Button button;

        private UrlRow(EntryRef entry) {
            super(entry);
            this.button = button(0, 0, URL_BUTTON_WIDTH, CONTROL_HEIGHT, text("Open"), ignored -> KonfigConfigScreen.this.openInlineUrl(this.entry));
        }

        @Override
        protected AbstractWidget control() {
            return this.button;
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF80C8FF);
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF80C8FF);
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(x, y, width, height);
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, 0xFF80C8FF);
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(x, y, width, height);
            this.renderStandardRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, 0xFF80C8FF);
        }
//?}
    }

    private final class BooleanRow extends ConfigRow {
        private final Button button;

        private BooleanRow(EntryRef entry) {
            super(entry);
            this.button = button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, booleanText(entry.value), button -> {
                Object previousDraft = KonfigConfigScreen.this.session.draft(entry.value);
                KonfigConfigScreen.this.session.setDraft(entry.value, Boolean.valueOf(!KonfigConfigScreen.this.readBoolean(entry.value)));
                this.commitOrRevert(previousDraft);
                this.syncFromDraft();
            });
        }

        @Override
        protected AbstractWidget control() {
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
            this.button = button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, enumText(entry, KonfigConfigScreen.this.currentEnum(entry.value)), button -> {
                Object previousDraft = KonfigConfigScreen.this.session.draft(entry.value);
                KonfigConfigScreen.this.session.setDraft(entry.value, KonfigConfigScreen.this.cycleEnum(entry.value));
                this.commitOrRevert(previousDraft);
                this.syncFromDraft();
            });
        }

        @Override
        protected AbstractWidget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(enumText(this.entry, KonfigConfigScreen.this.currentEnum(this.entry.value)));
        }
    }

    private final class DropdownRow extends ConfigRow {
        private final Button button;
        private boolean open;
        private int selectedIndex;
        private int scrollOffset;
        private int lastButtonX;
        private int lastButtonY;
        private int lastButtonWidth = CONTROL_MIN_WIDTH;
        private int lastDropdownX;
        private int lastDropdownY;
        private int lastDropdownWidth;
        private int lastDropdownHeight;
        private final StringBuilder typeSelectBuffer = new StringBuilder();
        private long lastTypeSelectMillis;

        private DropdownRow(EntryRef entry) {
            super(entry);
            this.button = button(
                    0,
                    0,
                    CONTROL_MIN_WIDTH,
                    CONTROL_HEIGHT,
                    text(""),
                    ignored -> this.toggleDropdown()
            );
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
            if (this.open) {
                return this.entry.tooltip;
            }
            String optionTooltip = translatedDropdownTooltip(this.currentOption());
            return isBlank(optionTooltip) ? this.entry.tooltip : optionTooltip;
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.extractContent(guiGraphics, mouseX, mouseY, hovered, partialTick);
            this.captureButtonBounds();
            this.renderButtonLabel(KonfigRenderContext.of(guiGraphics));
            if (this.open) {
                KonfigConfigScreen.this.renderedDropdownRow = this;
            }
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderContent(guiGraphics, mouseX, mouseY, hovered, partialTick);
            this.captureButtonBounds();
            this.renderButtonLabel(KonfigRenderContext.of(guiGraphics));
            if (this.open) {
                KonfigConfigScreen.this.renderedDropdownRow = this;
            }
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
            this.captureButtonBounds();
            this.renderButtonLabel(KonfigRenderContext.of(guiGraphics));
            if (this.open) {
                KonfigConfigScreen.this.renderedDropdownRow = this;
            }
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
//? if >=1.19.3 {
            this.captureButtonBounds();
//?} else {
            this.lastButtonX = this.button.x;
            this.lastButtonY = this.button.y;
            this.lastButtonWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
//?}
            this.renderButtonLabel(KonfigRenderContext.of(guiGraphics));
            if (this.open) {
                KonfigConfigScreen.this.renderedDropdownRow = this;
            }
        }
//?}

        private void toggleDropdown() {
            if (this.open) {
                this.closeDropdown();
            } else {
                this.openDropdown();
            }
        }

        private void openDropdown() {
            if (this.options().isEmpty()) {
                return;
            }
            this.open = true;
            this.selectedIndex = this.optionIndex(KonfigConfigScreen.this.currentDropdownValue(this.entry.value));
            this.ensureSelectedVisible();
            this.typeSelectBuffer.setLength(0);
            this.lastTypeSelectMillis = 0L;
            KonfigConfigScreen.this.setActiveDropdownRow(this);
        }

        private void closeDropdown() {
            this.open = false;
            this.typeSelectBuffer.setLength(0);
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
            if (!this.open) {
                return null;
            }

            this.layoutDropdown();
            int hovered = this.hoveredOptionIndex(mouseX, mouseY);
            if (hovered >= 0) {
                return this.option(hovered);
            }
            return this.option(this.selectedIndex);
        }

        private int optionIndex(String option) {
            List<String> options = this.options();
            for (int index = 0; index < options.size(); index++) {
                if (sameValue(options.get(index), option)) {
                    return index;
                }
            }
            return 0;
        }

        private int visibleOptionCount() {
            return Math.min(SUGGESTION_LIMIT, this.options().size());
        }

        private int maxScrollOffset() {
            return Math.max(0, this.options().size() - this.visibleOptionCount());
        }

        private void ensureSelectedVisible() {
            int visibleCount = this.visibleOptionCount();
            if (visibleCount <= 0) {
                this.scrollOffset = 0;
                return;
            }
            if (this.selectedIndex < this.scrollOffset) {
                this.scrollOffset = this.selectedIndex;
            } else if (this.selectedIndex >= this.scrollOffset + visibleCount) {
                this.scrollOffset = this.selectedIndex - visibleCount + 1;
            }
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScrollOffset());
        }

        private void selectOption(int optionIndex) {
            List<String> options = this.options();
            if (optionIndex < 0 || optionIndex >= options.size()) {
                return;
            }

            Object previousDraft = KonfigConfigScreen.this.session.draft(this.entry.value);
            KonfigConfigScreen.this.session.setDraft(this.entry.value, options.get(optionIndex));
            this.commitOrRevert(previousDraft);
            this.syncFromDraft();
            this.closeDropdown();
        }

//? if >=1.21.9 {
        private boolean handleDropdownClick(MouseButtonEvent event) {
            return this.handleDropdownClick(event.x(), event.y());
        }

        private boolean handleDropdownKey(KeyEvent event) {
            return this.handleDropdownKey(event.key());
        }
//?}

        private boolean handleDropdownClick(double mouseX, double mouseY) {
            if (!this.open) {
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
            if (!this.open || options.isEmpty()) {
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
                this.selectOption(this.selectedIndex);
                return true;
            }
            if (keyCode == InputConstants.KEY_DOWN) {
                this.selectedIndex = (this.selectedIndex + 1) % options.size();
                this.ensureSelectedVisible();
                return true;
            }
            if (keyCode == InputConstants.KEY_UP) {
                this.selectedIndex = (this.selectedIndex + options.size() - 1) % options.size();
                this.ensureSelectedVisible();
                return true;
            }
            return false;
        }

        private boolean handleClosedDropdownKey(int keyCode) {
            if (this.open || this.options().isEmpty()) {
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

        private boolean handleDropdownChar(int codePoint) {
            if (!this.open
                    || this.options().isEmpty()
                    || !Character.isValidCodePoint(codePoint)
                    || Character.isISOControl(codePoint)) {
                return false;
            }

            long now = System.currentTimeMillis();
            if (now - this.lastTypeSelectMillis > DROPDOWN_TYPE_SELECT_RESET_MS) {
                this.typeSelectBuffer.setLength(0);
            }
            this.lastTypeSelectMillis = now;
            int normalizedCodePoint = Character.toLowerCase(codePoint);
            this.typeSelectBuffer.appendCodePoint(normalizedCodePoint);

            if (!this.focusFirstTypeMatch(this.typeSelectBuffer.toString())) {
                this.typeSelectBuffer.setLength(0);
                this.typeSelectBuffer.appendCodePoint(normalizedCodePoint);
                this.focusFirstTypeMatch(this.typeSelectBuffer.toString());
            }
            return true;
        }

        private boolean focusFirstTypeMatch(String query) {
            if (isBlank(query)) {
                return false;
            }

            String normalizedQuery = query.toLowerCase(Locale.ROOT);
            List<String> options = this.options();
            int start = Math.max(0, this.selectedIndex + 1);
            for (int offset = 0; offset < options.size(); offset++) {
                int index = (start + offset) % options.size();
                if (this.optionSearchText(index).startsWith(normalizedQuery)) {
                    this.selectedIndex = index;
                    this.ensureSelectedVisible();
                    return true;
                }
            }
            return false;
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
            if (!this.open) {
                return false;
            }
            this.layoutDropdown();
            if (!this.isPointInsideDropdown(mouseX, mouseY)) {
                return false;
            }

            int previousOffset = this.scrollOffset;
            if (scrollY > 0.0D) {
                this.scrollOffset--;
            } else if (scrollY < 0.0D) {
                this.scrollOffset++;
            }
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScrollOffset());
            if (this.scrollOffset != previousOffset && this.visibleOptionCount() > 0) {
                this.selectedIndex = Mth.clamp(this.selectedIndex, this.scrollOffset, this.scrollOffset + this.visibleOptionCount() - 1);
            }
            return true;
        }

        private boolean isPointInsideButton(double mouseX, double mouseY) {
            return mouseX >= this.lastButtonX
                    && mouseX <= this.lastButtonX + this.lastButtonWidth
                    && mouseY >= this.lastButtonY
                    && mouseY <= this.lastButtonY + CONTROL_HEIGHT;
        }

        private boolean isPointInsideDropdown(double mouseX, double mouseY) {
            if (!this.open) {
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
            this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.maxScrollOffset());
        }

//? if >=1.19.3 {
        private void captureButtonBounds() {
            this.lastButtonX = this.button.getX();
            this.lastButtonY = this.button.getY();
            this.lastButtonWidth = this.button.getWidth();
        }
//?}

        private void renderButtonLabel(KonfigRenderContext context) {
            int textX = this.lastButtonX + 6;
            int chevronLeft = this.lastButtonX + this.lastButtonWidth - DROPDOWN_CHEVRON_WIDTH;
            int textMaxWidth = Math.max(0, chevronLeft - textX - 4);
            int textY = this.lastButtonY + ((CONTROL_HEIGHT - KonfigConfigScreen.this.font.lineHeight) / 2) + 1;
            Component valueText = this.fitDropdownText(dropdownText(this.entry, KonfigConfigScreen.this.currentDropdownValue(this.entry.value)), textMaxWidth);
            context.drawText(KonfigConfigScreen.this.font, valueText, textX, textY, 0xFFFFFFFF);

            Component chevron = text(this.open ? "\u25B4" : "\u25BE");
            int chevronX = chevronLeft + Math.max(0, (DROPDOWN_CHEVRON_WIDTH - KonfigConfigScreen.this.font.width(chevron)) / 2);
            context.drawText(KonfigConfigScreen.this.font, chevron, chevronX, textY, this.open ? 0xFFF8E38F : 0xFFCFCFCF);
        }

        private Component fitDropdownText(Component value, int maxWidth) {
            if (maxWidth <= 0) {
                return text("");
            }
            if (KonfigConfigScreen.this.font.width(value) <= maxWidth) {
                return value;
            }

            String ellipsis = "...";
            int available = Math.max(0, maxWidth - KonfigConfigScreen.this.font.width(ellipsis));
            String trimmed = KonfigConfigScreen.this.font.plainSubstrByWidth(value.getString(), available).trim();
            return text(trimmed + ellipsis);
        }

        private int hoveredOptionIndex(int mouseX, int mouseY) {
            if (mouseX < this.lastDropdownX
                    || mouseX > this.lastDropdownX + this.lastDropdownWidth
                    || mouseY < this.lastDropdownY + 2
                    || mouseY > this.lastDropdownY + this.lastDropdownHeight - 2) {
                return -1;
            }

            int visibleIndex = (mouseY - this.lastDropdownY - 2) / SUGGESTION_ROW_HEIGHT;
            int index = this.scrollOffset + visibleIndex;
            return index >= 0 && index < this.options().size() && visibleIndex < this.visibleOptionCount() ? index : -1;
        }

        private void renderDropdown(KonfigRenderContext context, int mouseX, int mouseY) {
            List<String> options = this.options();
            if (!this.open || options.isEmpty()) {
                return;
            }

            this.layoutDropdown();
            context.fill(this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
            context.fill(this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

            int hovered = this.hoveredOptionIndex(mouseX, mouseY);
            DropdownOptionMetadata tooltipOption = this.option(hovered >= 0 ? hovered : this.selectedIndex);
            String tooltip = translatedDropdownTooltip(tooltipOption);
            if (!isBlank(tooltip)) {
                KonfigConfigScreen.this.queueTooltip(tooltip, mouseX, mouseY);
            }
            int visibleCount = this.visibleOptionCount();
            int currentIndex = this.optionIndex(KonfigConfigScreen.this.currentDropdownValue(this.entry.value));
            for (int visibleIndex = 0; visibleIndex < visibleCount; visibleIndex++) {
                int optionIndex = this.scrollOffset + visibleIndex;
                if (optionIndex >= options.size()) {
                    break;
                }

                int rowY = this.lastDropdownY + 2 + (visibleIndex * SUGGESTION_ROW_HEIGHT);
                int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                boolean rowHovered = optionIndex == hovered;
                boolean focused = optionIndex == this.selectedIndex;
                boolean current = optionIndex == currentIndex;
                if (rowHovered || focused || current) {
                    int color = rowHovered ? 0x805C6FA8 : focused ? 0x60406080 : 0x50303030;
                    context.fill(this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, color);
                }
                if (current) {
                    context.fill(this.lastDropdownX + 2, rowY + 2, this.lastDropdownX + 4, rowBottom - 2, 0xFFF8E38F);
                }
                int textX = this.lastDropdownX + 8;
                int textRight = this.maxScrollOffset() > 0 ? this.lastDropdownX + this.lastDropdownWidth - 8 : this.lastDropdownX + this.lastDropdownWidth - 4;
                context.drawText(KonfigConfigScreen.this.font, this.fitDropdownText(dropdownText(this.entry, options.get(optionIndex)), Math.max(0, textRight - textX)), textX, rowY + 3, 0xFFFFFFFF);
            }

            if (this.maxScrollOffset() > 0) {
                int trackTop = this.lastDropdownY + 2;
                int trackBottom = this.lastDropdownY + this.lastDropdownHeight - 2;
                int trackHeight = Math.max(1, trackBottom - trackTop);
                int thumbHeight = Mth.clamp((trackHeight * visibleCount) / options.size(), 10, trackHeight);
                int thumbTop = trackTop + ((trackHeight - thumbHeight) * this.scrollOffset / this.maxScrollOffset());
                context.fill(this.lastDropdownX + this.lastDropdownWidth - 4, trackTop, this.lastDropdownX + this.lastDropdownWidth - 2, trackBottom, 0x44000000);
                context.fill(this.lastDropdownX + this.lastDropdownWidth - 4, thumbTop, this.lastDropdownX + this.lastDropdownWidth - 2, thumbTop + thumbHeight, 0xAAFFFFFF);
            }
        }
    }

    private final class ColorRow extends ConfigRow {
        private static final int PREVIEW_SIZE = 16;
        private static final int PREVIEW_GAP = 6;

        private final Button button;

        private ColorRow(EntryRef entry) {
            super(entry);
            this.button = button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, colorText(entry.value), ignored -> {
                KonfigConfigScreen.this.setScreen(new ColorEditorScreen(entry));
            });
        }

        @Override
        protected AbstractWidget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(colorText(this.entry.value));
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
            int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
            this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), previewX, previewY);
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(this.getContentX(), this.getContentY(), this.getContentWidth(), this.getContentHeight());
            int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
            this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), previewX, previewY);
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(x, y, width, height);
            int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
            this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, previewX, previewY);
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            RowLayout layout = this.rowLayout(x, y, width, height);
            int previewX = layout.controlX - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = layout.y + (layout.height - PREVIEW_SIZE) / 2;
            this.renderColorRow(KonfigRenderContext.of(guiGraphics), layout, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, previewX, previewY);
        }
//?}
    }

    private static final class RowLayout {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int controlWidth;
        private final int controlX;
        private final int controlY;

        private RowLayout(int x, int y, int width, int height, int controlWidth, int controlX, int controlY) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.controlWidth = controlWidth;
            this.controlX = controlX;
            this.controlY = controlY;
        }
    }

    private final class StringListRow extends ConfigRow {
        private static final int PREVIEW_SIZE = 16;
        private static final int PREVIEW_GAP = 6;

        private final Button button;

        private StringListRow(EntryRef entry) {
            super(entry);
            this.button = button(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, stringListText(entry.value), ignored -> {
                KonfigConfigScreen.this.setScreen(new StringListEditorScreen(entry));
            });
        }

        @Override
        protected AbstractWidget control() {
            return this.button;
        }

        @Override
        protected void syncFromDraft() {
            this.button.setMessage(stringListText(this.entry.value));
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.extractContent(guiGraphics, mouseX, mouseY, hovered, partialTick);
            if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                return;
            }

            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (values.isEmpty()) {
                return;
            }

            int previewX = this.button.getX() - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.getY() + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
            renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryKey(), values.get(0), previewX, previewY);
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderContent(guiGraphics, mouseX, mouseY, hovered, partialTick);
            if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                return;
            }

            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (values.isEmpty()) {
                return;
            }

            int previewX = this.button.getX() - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.getY() + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
            renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryKey(), values.get(0), previewX, previewY);
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
            if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                return;
            }

            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (values.isEmpty()) {
                return;
            }

//? if >=1.19.3 {
            int previewX = this.button.getX() - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.getY() + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
//?} else {
            int previewX = this.button.x - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.y + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
//?}
            renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryKey(), values.get(0), previewX, previewY);
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            super.renderRow(guiGraphics, x, y, width, height, mouseX, mouseY, hovered, partialTick);
            if (!this.entry.value.hasBoundRegistry() || !supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                return;
            }

            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            if (values.isEmpty()) {
                return;
            }

//? if >=1.19.3 {
            int previewX = this.button.getX() - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.getY() + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
//?} else {
            int previewX = this.button.x - PREVIEW_GAP - PREVIEW_SIZE;
            int previewY = this.button.y + (CONTROL_HEIGHT - PREVIEW_SIZE) / 2;
//?}
            renderRegistryIcon(guiGraphics, this.entry.value.boundRegistryKey(), values.get(0), previewX, previewY);
        }
//?}
    }

    private abstract class BaseSliderWidget extends AbstractSliderButton {
        private BaseSliderWidget(double initialProgress) {
            super(0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, text(""), initialProgress);
        }

        protected final void syncToProgress(double progress) {
            this.value = Mth.clamp(progress, 0.0D, 1.0D);
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
        protected AbstractWidget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private int currentValue() {
            return KonfigConfigScreen.this.session.currentInt(this.entry.value);
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.session.setDraft(this.entry.value, Integer.valueOf(intFromProgress(progress, this.min, this.max)));
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

//? if >=1.21.9 {
            @Override
            public void onRelease(MouseButtonEvent event) {
                Object previousValue = IntegerSliderRow.this.entry.value.get();
                super.onRelease(event);
                IntegerSliderRow.this.commitOrRevert(previousValue);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                int previousValue = IntegerSliderRow.this.currentValue();
                boolean handled = super.keyPressed(event);
                if (handled && previousValue != IntegerSliderRow.this.currentValue()) {
                    IntegerSliderRow.this.commitOrRevert(Integer.valueOf(previousValue));
                }
                return handled;
            }
//?} else {
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
//?}
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
        protected AbstractWidget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private long currentValue() {
            return KonfigConfigScreen.this.session.currentLong(this.entry.value);
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.session.setDraft(this.entry.value, Long.valueOf(longFromProgress(progress, this.min, this.max)));
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

//? if >=1.21.9 {
            @Override
            public void onRelease(MouseButtonEvent event) {
                Object previousValue = LongSliderRow.this.entry.value.get();
                super.onRelease(event);
                LongSliderRow.this.commitOrRevert(previousValue);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                long previousValue = LongSliderRow.this.currentValue();
                boolean handled = super.keyPressed(event);
                if (handled && previousValue != LongSliderRow.this.currentValue()) {
                    LongSliderRow.this.commitOrRevert(Long.valueOf(previousValue));
                }
                return handled;
            }
//?} else {
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
//?}
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
        protected AbstractWidget control() {
            return this.slider;
        }

        @Override
        protected void syncFromDraft() {
            this.slider.syncToProgress(progressFor(this.currentValue(), this.min, this.max));
        }

        private double currentValue() {
            return KonfigConfigScreen.this.session.currentDouble(this.entry.value);
        }

        private void updateDraftFromSlider(double progress) {
            KonfigConfigScreen.this.session.setDraft(this.entry.value, Double.valueOf(doubleFromProgress(progress, this.min, this.max)));
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

//? if >=1.21.9 {
            @Override
            public void onRelease(MouseButtonEvent event) {
                Object previousValue = DoubleSliderRow.this.entry.value.get();
                super.onRelease(event);
                DoubleSliderRow.this.commitOrRevert(previousValue);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                double previousValue = DoubleSliderRow.this.currentValue();
                boolean handled = super.keyPressed(event);
                if (handled && !sameValue(Double.valueOf(previousValue), Double.valueOf(DoubleSliderRow.this.currentValue()))) {
                    DoubleSliderRow.this.commitOrRevert(Double.valueOf(previousValue));
                }
                return handled;
            }
//?} else {
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
                if (handled && !sameValue(Double.valueOf(previousValue), Double.valueOf(DoubleSliderRow.this.currentValue()))) {
                    DoubleSliderRow.this.commitOrRevert(Double.valueOf(previousValue));
                }
                return handled;
            }
//?}
        }
    }

    private final class RegistryTextInputRow extends ConfigRow {
        private static final int ICON_SIZE = 16;
        private static final int ICON_GAP = 6;

        private final EditBox input;
        private final KonfigSuggestionState suggestions = new KonfigSuggestionState();
        private boolean suppressResponder;
        private int lastInputX;
        private int lastInputY;
        private int lastInputWidth;
        private int lastDropdownX;
        private int lastDropdownY;
        private int lastDropdownWidth;
        private int lastDropdownHeight;

        private RegistryTextInputRow(EntryRef entry) {
            super(entry);
            this.input = new EditBox(KonfigConfigScreen.this.font, 0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, entry.label);
            this.input.setMaxLength(256);
            this.input.setValue(KonfigConfigScreen.this.currentStringValue(entry.value));
            this.input.setResponder(value -> {
                if (this.suppressResponder) {
                    return;
                }
                KonfigConfigScreen.this.session.setDraft(entry.value, value);
                KonfigConfigScreen.this.persistEntry(entry);
                this.refreshSuggestions();
            });
        }

        @Override
        protected AbstractWidget control() {
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
            this.input.setValue(KonfigConfigScreen.this.currentStringValue(this.entry.value));
            this.suppressResponder = false;
            this.activateSuggestions();
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int labelRight = x + width - controlWidth - 8;
            this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, labelRight, y + height, false);
        }
//?} elif >=1.21.9 {
        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int x = this.getContentX();
            int y = this.getContentY();
            int width = this.getContentWidth();
            int height = this.getContentHeight();
            this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), true);
        }
//?} elif >=1.20 {
        @Override
        protected void renderRow(GuiGraphics guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, true);
        }
//?} else {
        @Override
        protected void renderRow(PoseStack guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRegistryTextInputRow(KonfigRenderContext.of(guiGraphics), x, y, width, height, mouseX, mouseY, hovered, partialTick, x, y, x + width, y + height, true);
        }
//?}

        private void renderRegistryTextInputRow(KonfigRenderContext context, int x, int y, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, int tooltipLeft, int tooltipTop, int tooltipRight, int tooltipBottom, boolean renderIcon) {
            KonfigConfigScreen.this.updateHoveredEntry(this.entry, hovered);
            if (hovered) {
                context.fill(x, y, x + width, y + height, 0x22000000);
            }

            context.showTooltip(KonfigConfigScreen.this, KonfigConfigScreen.this.font, this.entry.tooltip, mouseX, mouseY, tooltipLeft, tooltipTop, tooltipRight, tooltipBottom);

            int controlWidth = Math.min(CONTROL_MAX_WIDTH, Math.max(CONTROL_MIN_WIDTH, width / 2));
            int controlX = x + width - controlWidth;
            int controlY = y + (height - CONTROL_HEIGHT) / 2;
            layoutControl(this.control(), controlX, controlY, controlWidth);
            this.lastInputX = controlX;
            this.lastInputY = controlY;
            this.lastInputWidth = controlWidth;

            context.drawText(KonfigConfigScreen.this.font, this.entry.contextLabel, x + 4, y + 1, 0xFFA0A0A0);
            context.drawText(KonfigConfigScreen.this.font, this.entry.displayLabel(), x + 4, y + 12, 0xFFFFFFFF);
            if (renderIcon && this.entry.value.boundRegistryKey() != null && supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                context.renderRegistryIcon(
                        this.entry.value.boundRegistryKey(),
                        KonfigConfigScreen.this.currentStringValue(this.entry.value),
                        controlX - ICON_GAP - ICON_SIZE,
                        y + (height - ICON_SIZE) / 2
                );
            }
            context.renderWidget(this.input, mouseX, mouseY, partialTick);

            if (this.input.isFocused()) {
                KonfigConfigScreen.this.setActiveRegistryRow(this);
                this.refreshSuggestions();
            }
            if (KonfigConfigScreen.this.activeRegistryRow == this && !this.suggestions.isEmpty()) {
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
            if (this.entry.value.boundRegistryKey() == null) {
                this.closeSuggestions();
                return;
            }

            this.suggestions.refresh(
                    KonfigConfigScreen.this.registrySuggestions(this.entry.value.boundRegistryKey()),
                    this.input.getValue()
            );
            this.updateInlineSuggestion();
        }

        private void activateSuggestions() {
            if (this.entry.value.boundRegistryKey() == null) {
                this.closeSuggestions();
                return;
            }

            this.suggestions.activate(
                    KonfigConfigScreen.this.registrySuggestions(this.entry.value.boundRegistryKey()),
                    this.input.getValue()
            );
            this.updateInlineSuggestion();
        }

        private void dismissSuggestions() {
            this.suggestions.dismiss(this.input.getValue());
            this.updateInlineSuggestion();
        }

        private void closeSuggestions() {
            this.suggestions.close();
            this.updateInlineSuggestion();
            if (KonfigConfigScreen.this.activeRegistryRow == this) {
                KonfigConfigScreen.this.activeRegistryRow = null;
            }
        }

        private void renderSuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.suggestions.isEmpty()) {
                return;
            }

            this.layoutSuggestionBox();
            context.fill(this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
            context.fill(this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

            for (int index = 0; index < this.suggestions.size(); index++) {
                int rowY = this.lastDropdownY + 2 + (index * SUGGESTION_ROW_HEIGHT);
                int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                boolean hovered = index == this.hoveredSuggestionIndex(mouseX, mouseY);
                if (hovered || index == this.suggestions.selectedIndex()) {
                    context.fill(this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, hovered ? 0x80406080 : 0x50303030);
                }
                int textX = this.lastDropdownX + 4;
                if (this.entry.value.boundRegistryKey() != null && supportsRegistryIcon(this.entry.value.boundRegistryKey())) {
                    context.renderRegistryIcon(this.entry.value.boundRegistryKey(), this.suggestions.suggestion(index), this.lastDropdownX + 2, rowY - 1);
                    textX += 18;
                }
                context.drawText(KonfigConfigScreen.this.font, text(this.suggestions.suggestion(index)), textX, rowY + 3, 0xFFFFFFFF);
            }
        }

//? if >=1.21.9 {
        private boolean handleSuggestionClick(MouseButtonEvent event) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.suggestions.isEmpty()) {
                return false;
            }

            int hovered = this.hoveredSuggestionIndex((int) event.x(), (int) event.y());
            if (hovered < 0) {
                return false;
            }

            this.acceptSuggestion(this.suggestions.suggestion(hovered));
            return true;
        }

        private boolean handleSuggestionKey(KeyEvent event) {
            if (KonfigConfigScreen.this.activeRegistryRow != this) {
                return false;
            }

            int keyCode = event.key();
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.dismissSuggestions();
                return true;
            }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                this.dismissSuggestions();
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
                this.acceptSuggestion(this.suggestions.selectedSuggestion());
                return true;
            }
            return false;
        }
//?} else {
        private boolean handleSuggestionClick(double mouseX, double mouseY) {
            if (KonfigConfigScreen.this.activeRegistryRow != this || this.suggestions.isEmpty()) {
                return false;
            }

            int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
            if (hovered < 0) {
                return false;
            }

            this.acceptSuggestion(this.suggestions.suggestion(hovered));
            return true;
        }

        private boolean handleSuggestionKey(int keyCode) {
            if (KonfigConfigScreen.this.activeRegistryRow != this) {
                return false;
            }

            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.dismissSuggestions();
                return true;
            }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                this.dismissSuggestions();
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
                this.acceptSuggestion(this.suggestions.selectedSuggestion());
                return true;
            }
            return false;
        }
//?}

        private void acceptSuggestion(String suggestion) {
            this.suppressResponder = true;
            this.input.setValue(suggestion);
            this.suppressResponder = false;
            KonfigConfigScreen.this.session.setDraft(this.entry.value, suggestion);
            KonfigConfigScreen.this.persistEntry(this.entry);
            this.dismissSuggestions();
//? if >=1.19.4 {
            this.input.setFocused(true);
//?} else {
            this.input.setFocus(true);
//?}
        }

        private void updateInlineSuggestion() {
            this.input.setSuggestion(this.suggestions.inlineSuggestion(this.input.getValue()));
        }

        private void layoutSuggestionBox() {
            this.lastDropdownX = this.lastInputX;
            this.lastDropdownWidth = this.lastInputWidth;
            this.lastDropdownHeight = (this.suggestions.size() * SUGGESTION_ROW_HEIGHT) + 4;

            int belowY = this.lastInputY + CONTROL_HEIGHT + 2;
            int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
            boolean openAbove = belowY + this.lastDropdownHeight > KonfigConfigScreen.this.height - 32 && aboveY >= LIST_TOP;
            this.lastDropdownY = openAbove ? aboveY : belowY;
        }

        private int hoveredSuggestionIndex(int mouseX, int mouseY) {
            return this.suggestions.hoveredIndex(mouseX, mouseY, this.lastDropdownX, this.lastDropdownY, this.lastDropdownWidth, this.lastDropdownHeight, SUGGESTION_ROW_HEIGHT);
        }
    }

    private final class TextInputRow extends ConfigRow {
        private final EditBox input;
        private String validationMessage = "";

        private TextInputRow(EntryRef entry) {
            super(entry);
            this.input = new EditBox(KonfigConfigScreen.this.font, 0, 0, CONTROL_MIN_WIDTH, CONTROL_HEIGHT, entry.label);
            this.input.setMaxLength(256);
            this.input.setValue(KonfigConfigScreen.this.currentStringValue(entry.value));
            this.input.setResponder(value -> {
                KonfigConfigScreen.this.session.setDraft(entry.value, value);
                try {
                    parseDraft(entry.value, value);
                    this.validationMessage = "";
                    KonfigConfigScreen.this.persistEntry(entry);
                } catch (Exception exception) {
                    this.validationMessage = exceptionMessage(exception);
                }
            });
        }

        @Override
        protected AbstractWidget control() {
            return this.input;
        }

        @Override
        protected int preferredHeight(int rowWidth) {
            return ROW_HEIGHT + 12;
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
            this.input.setValue(KonfigConfigScreen.this.currentStringValue(this.entry.value));
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
            KonfigConfigScreen.this.setScreen(KonfigConfigScreen.this);
        }

        protected final boolean persistEditedValue(Object previousValue) {
            if (!KonfigConfigScreen.this.persistEntry(this.entry)) {
                KonfigConfigScreen.this.session.setDraft(this.entry.value, previousValue);
                return false;
            }
            return true;
        }

        protected final boolean resetToSessionStart() {
            try {
                KonfigConfigScreen.this.session.resetEntry(this.entry);
                return true;
            } catch (RuntimeException exception) {
                KonfigToastSupport.resetFailed(exceptionMessage(exception));
                return false;
            }
        }

//? if >=26.1 {
        protected final void renderEditorChrome(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            fillRect(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            drawCenteredText(guiGraphics, this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
            drawText(guiGraphics, this.font, this.entry.contextLabel, 12, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
        }
//?} elif >=1.20 {
        protected final void renderEditorChrome(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            fillRect(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            drawCenteredText(guiGraphics, this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
            drawText(guiGraphics, this.font, this.entry.contextLabel, 12, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
        }
//?} else {
        protected final void renderEditorChrome(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            fillRect(guiGraphics, 0, 0, this.width, this.height, 0xC0101010);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            drawCenteredText(guiGraphics, this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
            drawText(guiGraphics, this.font, this.entry.contextLabel, 12, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
        }
//?}
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

        private EditBox hexInput;
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
            this.clearWidgets();

            this.hexInput = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - HEX_WIDTH / 2, HEX_Y, HEX_WIDTH, 20, this.entry.label));
            this.hexInput.setMaxLength(this.entry.value.kind() == EntryKind.COLOR_ARGB ? 9 : 7);
            this.hexInput.setValue(this.currentHex());
            this.hexInput.setResponder(this::onHexChanged);

            int sliderX = this.width / 2 - SLIDER_WIDTH / 2;
            this.redSlider = this.addRenderableWidget(new ChannelSlider(ColorChannel.RED, sliderX, SLIDER_Y));
            this.greenSlider = this.addRenderableWidget(new ChannelSlider(ColorChannel.GREEN, sliderX, SLIDER_Y + SLIDER_STEP));
            this.blueSlider = this.addRenderableWidget(new ChannelSlider(ColorChannel.BLUE, sliderX, SLIDER_Y + (SLIDER_STEP * 2)));
            if (this.entry.value.kind() == EntryKind.COLOR_ARGB) {
                this.alphaSlider = this.addRenderableWidget(new ChannelSlider(ColorChannel.ALPHA, sliderX, SLIDER_Y + (SLIDER_STEP * 3)));
            }

            int footerY = this.height - 26;
            this.addRenderableWidget(button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.syncWidgetsFromDraft();
                }
            }));
            this.addRenderableWidget(button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));

            this.syncWidgetsFromDraft();
        }

//? if >=26.1 {
        @Override
        public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            int previewX = this.width / 2 - PREVIEW_SIZE / 2;
            drawColorSwatch(guiGraphics, previewX, PREVIEW_Y, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
            this.renderValidationMessage(guiGraphics);
        }
//?} elif >=1.20 {
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            int previewX = this.width / 2 - PREVIEW_SIZE / 2;
            drawColorSwatch(guiGraphics, previewX, PREVIEW_Y, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
            this.renderValidationMessage(guiGraphics);
        }
//?} else {
        @Override
        public void render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            int previewX = this.width / 2 - PREVIEW_SIZE / 2;
            drawColorSwatch(guiGraphics, previewX, PREVIEW_Y, PREVIEW_SIZE, KonfigConfigScreen.this.currentColor(this.entry.value), this.entry.value.kind());
            this.renderValidationMessage(guiGraphics);
        }
//?}

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

            Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(this.entry.value);
            try {
                int parsed = parseColor(this.entry.value, value);
                KonfigConfigScreen.this.session.setDraft(this.entry.value, Integer.valueOf(parsed));
                if (this.persistEditedValue(previousValue)) {
                    this.validationMessage = "";
                    this.syncWidgetsFromDraft();
                } else {
                    this.syncWidgetsFromDraft();
                }
            } catch (Exception exception) {
                KonfigConfigScreen.this.session.setDraft(this.entry.value, previousValue);
                this.validationMessage = exception.getMessage() == null
                        ? translate("konfig.screen.color.invalid", Integer.valueOf(expectedDigits)).getString()
                        : exception.getMessage();
                this.syncWidgetsFromDraft();
            }
        }

//? if >=26.1 {
        private void renderValidationMessage(GuiGraphicsExtractor guiGraphics) {
            if (!this.validationMessage.isEmpty()) {
                drawCenteredText(guiGraphics, this.font, text(this.validationMessage), this.width / 2, HEX_Y + CONTROL_HEIGHT + 3, VALIDATION_COLOR);
            }
        }
//?} elif >=1.20 {
        private void renderValidationMessage(GuiGraphics guiGraphics) {
            if (!this.validationMessage.isEmpty()) {
                drawCenteredText(guiGraphics, this.font, text(this.validationMessage), this.width / 2, HEX_Y + CONTROL_HEIGHT + 3, VALIDATION_COLOR);
            }
        }
//?} else {
        private void renderValidationMessage(PoseStack guiGraphics) {
            if (!this.validationMessage.isEmpty()) {
                drawCenteredText(guiGraphics, this.font, text(this.validationMessage), this.width / 2, HEX_Y + CONTROL_HEIGHT + 3, VALIDATION_COLOR);
            }
        }
//?}

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
//? if >=1.19.3 {
                this.setX(x);
                this.setY(y);
//?} else {
                this.x = x;
                this.y = y;
//?}
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
                KonfigConfigScreen.this.session.setDraft(ColorEditorScreen.this.entry.value, Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, intFromProgress(this.value, 0, 255))));
            }

//? if >=1.21.9 {
            @Override
            public void onRelease(MouseButtonEvent event) {
                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(ColorEditorScreen.this.entry.value);
                super.onRelease(event);
                if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                    ColorEditorScreen.this.syncWidgetsFromDraft();
                }
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(ColorEditorScreen.this.entry.value);
                int before = ColorEditorScreen.this.currentChannel(this.channel);
                boolean handled = super.keyPressed(event);
                if (handled && before != ColorEditorScreen.this.currentChannel(this.channel)) {
                    if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                        ColorEditorScreen.this.syncWidgetsFromDraft();
                    }
                }
                return handled;
            }
//?} else {
            @Override
            public void onRelease(double mouseX, double mouseY) {
                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(ColorEditorScreen.this.entry.value);
                super.onRelease(mouseX, mouseY);
                if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                    ColorEditorScreen.this.syncWidgetsFromDraft();
                }
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(ColorEditorScreen.this.entry.value);
                int before = ColorEditorScreen.this.currentChannel(this.channel);
                boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
                if (handled && before != ColorEditorScreen.this.currentChannel(this.channel)) {
                    if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                        ColorEditorScreen.this.syncWidgetsFromDraft();
                    }
                }
                return handled;
            }
//?}
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

//? if >=26.1 {
        @Override
        public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderedRegistryRow = null;
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            this.renderStringListEditorOverlay(KonfigRenderContext.of(guiGraphics), mouseX, mouseY);
        }
//?} elif >=1.20 {
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderedRegistryRow = null;
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            this.renderStringListEditorOverlay(KonfigRenderContext.of(guiGraphics), mouseX, mouseY);
        }
//?} else {
        @Override
        public void render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderedRegistryRow = null;
            this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick);
            this.renderStringListEditorOverlay(KonfigRenderContext.of(guiGraphics), mouseX, mouseY);
        }
//?}

        private void renderStringListEditorOverlay(KonfigRenderContext context, int mouseX, int mouseY) {
            String count = translate("konfig.screen.list.count", Integer.valueOf(KonfigConfigScreen.this.currentStringList(this.entry.value).size())).getString();
            context.drawText(this.font, text(count), this.width - 12 - this.font.width(count), EDITOR_CONTEXT_Y, 0xFFC0C0C0);
            if (KonfigConfigScreen.this.currentStringList(this.entry.value).isEmpty()) {
                context.drawCenteredText(this.font, translate("konfig.screen.list.empty"), this.width / 2, this.height / 2 - 12, 0xFFC0C0C0);
            }
            if (this.renderedRegistryRow != null) {
                this.renderedRegistryRow.renderSuggestions(context, mouseX, mouseY);
            }
        }

//? if >=1.21.9 {
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionClick(event)) {
                return true;
            }

            boolean handled = super.mouseClicked(event, doubleClick);
            ListEntryRow focusedRow = this.findFocusedRegistryRow();
            if (focusedRow != null) {
                this.setActiveRegistryRow(focusedRow);
                focusedRow.activateSuggestions();
            } else if (this.activeRegistryRow != null && !this.activeRegistryRow.isPointInsideInput(event.x(), event.y())) {
                this.activeRegistryRow.closeSuggestions();
            }
            return handled;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (this.activeRegistryRow != null && this.activeRegistryRow.handleSuggestionKey(event)) {
                return true;
            }
            boolean handled = super.keyPressed(event);
            if (this.activeRegistryRow != null && this.activeRegistryRow.isFocused() && this.activeRegistryRow.hasRegistryBinding()) {
                this.activeRegistryRow.refreshSuggestions();
            }
            return handled;
        }
//?} else {
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
//?}

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
            this.clearWidgets();
            this.activeRegistryRow = null;
            this.renderedRegistryRow = null;
            int listTop = EDITOR_CONTENT_TOP;
            int listHeight = Math.max(48, this.height - listTop - LIST_BOTTOM_MARGIN);
            this.list = this.addRenderableWidget(new ListEntryList(this.minecraft, this.width, listHeight, listTop));
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            for (int i = 0; i < values.size(); i++) {
                this.list.addListEntry(new ListEntryRow(i, values.get(i)));
            }

            int footerY = this.height - 26;
            this.addRenderableWidget(button(this.width / 2 - 122, footerY, 80, 20, translate("konfig.screen.list.add"), ignored -> this.addValue()));
            this.addRenderableWidget(button(this.width / 2 - 40, footerY, 80, 20, translate("konfig.screen.reset"), ignored -> {
                if (this.resetToSessionStart()) {
                    this.rebuildEditorWidgets();
                }
            }));
            this.addRenderableWidget(button(this.width / 2 + 42, footerY, 80, 20, translate("konfig.screen.done"), ignored -> this.onClose()));
        }

        private void addValue() {
            Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(this.entry.value);
            List<String> values = KonfigConfigScreen.this.currentStringList(this.entry.value);
            values.add(this.entry.value.hasBoundRegistry() ? "" : translate("konfig.screen.list.new_item").getString());
            KonfigConfigScreen.this.session.setDraft(this.entry.value, values);
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

        private final class ListEntryList extends ContainerObjectSelectionList<ListEntryRow> {
            private ListEntryList(net.minecraft.client.Minecraft minecraft, int width, int height, int y) {
//? if >=1.20.3 {
                super(minecraft, width, height, y, ITEM_ROW_HEIGHT);
//?} else {
                super(minecraft, width, height, y, y + height, ITEM_ROW_HEIGHT);
//?}
//? if <=1.16.3 {
                this.setRenderHeader(false, 0);
//?} elif <=1.20.4 {
                this.setRenderBackground(false);
//?}
            }

            private void addListEntry(ListEntryRow row) {
                super.addEntry(row);
            }

            @Override
            public int getRowWidth() {
                return StringListEditorScreen.this.width - 28;
            }

//? if <=1.20.6 {
            @Override
            protected int getScrollbarPosition() {
//? if >=1.20.3 {
                return this.getRight() - 6;
//?} else {
                return this.x1 - 6;
//?}
            }
//?}

//? if >=26.1 {
            @Override
            public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
                fillRect(guiGraphics, this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x66000000);
                super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
            }
//?} elif >=1.20.3 {
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                fillRect(guiGraphics, this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x66000000);
                super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
            }
//?} elif >=1.20 {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                fillRect(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
                super.render(guiGraphics, mouseX, mouseY, partialTick);
            }
//?} else {
            @Override
            protected void renderBackground(PoseStack guiGraphics) {
                fillRect(guiGraphics, this.x0, this.y0, this.x1, this.y1, 0x66000000);
            }
//?}
        }

        private final class ListEntryRow extends ContainerObjectSelectionList.Entry<ListEntryRow> {
            private static final int ICON_SIZE = 16;
            private static final int ICON_GAP = 4;

            private final int index;
            private final EditBox input;
            private final Button moveUpButton;
            private final Button moveDownButton;
            private final Button removeButton;
            private final KonfigSuggestionState suggestions = new KonfigSuggestionState();
            private boolean suppressResponder;
            private int lastInputX;
            private int lastInputY;
            private int lastInputWidth;
            private int lastDropdownX;
            private int lastDropdownY;
            private int lastDropdownWidth;
            private int lastDropdownHeight;

            private ListEntryRow(int index, String value) {
                this.index = index;
                this.input = new EditBox(StringListEditorScreen.this.font, 0, 0, 140, CONTROL_HEIGHT, StringListEditorScreen.this.entry.label);
                this.input.setMaxLength(256);
                this.input.setValue(value);
                this.input.setResponder(this::onValueChanged);

                this.moveUpButton = button(0, 0, 20, 20, text("^"), ignored -> this.move(-1));
                this.moveDownButton = button(0, 0, 20, 20, text("v"), ignored -> this.move(1));
                this.removeButton = button(0, 0, 20, 20, text("-"), ignored -> this.remove());
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

                this.persistListValue(value);
            }

            private boolean hasRegistryBinding() {
                return StringListEditorScreen.this.entry.value.hasBoundRegistry();
            }

            private ResourceKey<? extends Registry<?>> registryKey() {
                return StringListEditorScreen.this.entry.value.boundRegistryKey();
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
                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(StringListEditorScreen.this.entry.value);
                List<String> values = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                values.set(this.index, value);
                KonfigConfigScreen.this.session.setDraft(StringListEditorScreen.this.entry.value, values);
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

                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(StringListEditorScreen.this.entry.value);
                Collections.swap(current, this.index, targetIndex);
                KonfigConfigScreen.this.session.setDraft(StringListEditorScreen.this.entry.value, current);
                if (StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

            private void remove() {
                List<String> current = KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value);
                if (this.index < 0 || this.index >= current.size()) {
                    return;
                }

                Object previousValue = KonfigConfigScreen.this.session.storedSnapshot(StringListEditorScreen.this.entry.value);
                current.remove(this.index);
                KonfigConfigScreen.this.session.setDraft(StringListEditorScreen.this.entry.value, current);
                if (StringListEditorScreen.this.persistEditedValue(previousValue)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

//? if >=26.1 {
            @Override
            public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int x = this.getContentX();
                int y = this.getContentY();
                int width = this.getContentWidth();
                this.renderListEntryRow(KonfigRenderContext.of(guiGraphics), x, y, width, mouseX, mouseY, hovered, partialTick);
            }
//?} elif >=1.21.9 {
            @Override
            public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
                int x = this.getContentX();
                int y = this.getContentY();
                int width = this.getContentWidth();
                this.renderListEntryRow(KonfigRenderContext.of(guiGraphics), x, y, width, mouseX, mouseY, hovered, partialTick);
            }
//?} elif >=1.20 {
            @Override
            public void render(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                this.renderListEntryRow(KonfigRenderContext.of(guiGraphics), x, y, width, mouseX, mouseY, hovered, partialTick);
            }
//?} else {
            @Override
            public void render(PoseStack guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
                this.renderListEntryRow(KonfigRenderContext.of(guiGraphics), x, y, width, mouseX, mouseY, hovered, partialTick);
            }
//?}

            private void renderListEntryRow(KonfigRenderContext context, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered) {
                    context.fill(x, y, x + width, y + ITEM_ROW_HEIGHT, 0x22000000);
                }

                int buttonY = y + 4;
                int removeX = x + width - 20;
                int downX = removeX - 24;
                int upX = downX - 24;
                int iconOffset = 0;
                if (this.hasRegistryBinding() && supportsRegistryIcon(this.registryKey())) {
                    context.renderRegistryIcon(this.registryKey(), this.input.getValue(), x, y + (ITEM_ROW_HEIGHT - ICON_SIZE) / 2);
                    iconOffset = ICON_SIZE + ICON_GAP;
                }
                int inputX = x + iconOffset;
                int inputWidth = Math.max(60, upX - inputX - 8);

                this.layoutInput(inputX, buttonY, inputWidth);
                this.positionButton(this.moveUpButton, upX, buttonY);
                this.moveUpButton.active = this.index > 0;
                this.positionButton(this.moveDownButton, downX, buttonY);
                this.moveDownButton.active = this.index + 1 < KonfigConfigScreen.this.currentStringList(StringListEditorScreen.this.entry.value).size();
                this.positionButton(this.removeButton, removeX, buttonY);

                context.renderWidget(this.input, mouseX, mouseY, partialTick);
                context.renderWidget(this.moveUpButton, mouseX, mouseY, partialTick);
                context.renderWidget(this.moveDownButton, mouseX, mouseY, partialTick);
                context.renderWidget(this.removeButton, mouseX, mouseY, partialTick);

                if (this.hasRegistryBinding() && this.input.isFocused()) {
                    StringListEditorScreen.this.setActiveRegistryRow(this);
                    this.refreshSuggestions();
                }
                if (StringListEditorScreen.this.activeRegistryRow == this && !this.suggestions.isEmpty()) {
                    StringListEditorScreen.this.renderedRegistryRow = this;
                }
            }

            private void layoutInput(int x, int y, int width) {
                this.input.setX(x);
//? if >=1.19.3 {
                this.input.setY(y);
//?} else {
                this.input.y = y;
//?}
                this.input.setWidth(width);
                this.lastInputX = x;
                this.lastInputY = y;
                this.lastInputWidth = width;
            }

            private void positionButton(Button button, int x, int y) {
//? if >=1.19.3 {
                button.setX(x);
                button.setY(y);
//?} else {
                button.x = x;
                button.y = y;
//?}
            }

            private void refreshSuggestions() {
                if (!this.hasRegistryBinding()) {
                    this.closeSuggestions();
                    return;
                }

                this.suggestions.refresh(
                        KonfigConfigScreen.this.registrySuggestions(this.registryKey()),
                        this.input.getValue()
                );
                this.updateInlineSuggestion();
            }

            private void activateSuggestions() {
                if (!this.hasRegistryBinding()) {
                    this.closeSuggestions();
                    return;
                }

                this.suggestions.activate(
                        KonfigConfigScreen.this.registrySuggestions(this.registryKey()),
                        this.input.getValue()
                );
                this.updateInlineSuggestion();
            }

            private void dismissSuggestions() {
                this.suggestions.dismiss(this.input.getValue());
                this.updateInlineSuggestion();
            }

            private void closeSuggestions() {
                this.suggestions.close();
                this.updateInlineSuggestion();
                if (StringListEditorScreen.this.activeRegistryRow == this) {
                    StringListEditorScreen.this.activeRegistryRow = null;
                }
            }

            private void renderSuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.suggestions.isEmpty()) {
                    return;
                }

                this.layoutSuggestionBox();
                context.fill(this.lastDropdownX - 1, this.lastDropdownY - 1, this.lastDropdownX + this.lastDropdownWidth + 1, this.lastDropdownY + this.lastDropdownHeight + 1, 0xFF202020);
                context.fill(this.lastDropdownX, this.lastDropdownY, this.lastDropdownX + this.lastDropdownWidth, this.lastDropdownY + this.lastDropdownHeight, 0xFF101010);

                for (int suggestionIndex = 0; suggestionIndex < this.suggestions.size(); suggestionIndex++) {
                    int rowY = this.lastDropdownY + 2 + (suggestionIndex * SUGGESTION_ROW_HEIGHT);
                    int rowBottom = rowY + SUGGESTION_ROW_HEIGHT;
                    boolean suggestionHovered = suggestionIndex == this.hoveredSuggestionIndex(mouseX, mouseY);
                    if (suggestionHovered || suggestionIndex == this.suggestions.selectedIndex()) {
                        context.fill(this.lastDropdownX + 1, rowY, this.lastDropdownX + this.lastDropdownWidth - 1, rowBottom, suggestionHovered ? 0x80406080 : 0x50303030);
                    }
                    int textX = this.lastDropdownX + 4;
                    if (supportsRegistryIcon(this.registryKey())) {
                        context.renderRegistryIcon(this.registryKey(), this.suggestions.suggestion(suggestionIndex), this.lastDropdownX + 2, rowY - 1);
                        textX += 18;
                    }
                    context.drawText(StringListEditorScreen.this.font, text(this.suggestions.suggestion(suggestionIndex)), textX, rowY + 3, 0xFFFFFFFF);
                }
            }

//? if >=1.21.9 {
            private boolean handleSuggestionClick(MouseButtonEvent event) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.suggestions.isEmpty()) {
                    return false;
                }

                int hovered = this.hoveredSuggestionIndex((int) event.x(), (int) event.y());
                if (hovered < 0) {
                    return false;
                }

                this.acceptSuggestion(this.suggestions.suggestion(hovered));
                return true;
            }

            private boolean handleSuggestionKey(KeyEvent event) {
                if (StringListEditorScreen.this.activeRegistryRow != this) {
                    return false;
                }
                int keyCode = event.key();
                if (keyCode == InputConstants.KEY_ESCAPE) {
                    this.dismissSuggestions();
                    return true;
                }
                if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                    this.dismissSuggestions();
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
                    this.acceptSuggestion(this.suggestions.selectedSuggestion());
                    return true;
                }
                return false;
            }
//?} else {
            private boolean handleSuggestionClick(double mouseX, double mouseY) {
                if (StringListEditorScreen.this.activeRegistryRow != this || this.suggestions.isEmpty()) {
                    return false;
                }

                int hovered = this.hoveredSuggestionIndex((int) mouseX, (int) mouseY);
                if (hovered < 0) {
                    return false;
                }

                this.acceptSuggestion(this.suggestions.suggestion(hovered));
                return true;
            }

            private boolean handleSuggestionKey(int keyCode) {
                if (StringListEditorScreen.this.activeRegistryRow != this) {
                    return false;
                }
                if (keyCode == InputConstants.KEY_ESCAPE) {
                    this.dismissSuggestions();
                    return true;
                }
                if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                    this.dismissSuggestions();
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
                    this.acceptSuggestion(this.suggestions.selectedSuggestion());
                    return true;
                }
                return false;
            }
//?}

            private void acceptSuggestion(String suggestion) {
                this.suppressResponder = true;
                this.input.setValue(suggestion);
                this.suppressResponder = false;
                if (this.persistListValue(suggestion)) {
                    this.dismissSuggestions();
//? if >=1.19.4 {
                    this.input.setFocused(true);
//?} else {
                    this.input.setFocus(true);
//?}
                }
            }

            private void updateInlineSuggestion() {
                this.input.setSuggestion(this.suggestions.inlineSuggestion(this.input.getValue()));
            }

            private void layoutSuggestionBox() {
                this.lastDropdownX = this.lastInputX;
                this.lastDropdownWidth = this.lastInputWidth;
                this.lastDropdownHeight = (this.suggestions.size() * SUGGESTION_ROW_HEIGHT) + 4;
                int belowY = this.lastInputY + CONTROL_HEIGHT + 2;
                int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
                boolean openAbove = belowY + this.lastDropdownHeight > StringListEditorScreen.this.height - 32 && aboveY >= LIST_TOP;
                this.lastDropdownY = openAbove ? aboveY : belowY;
            }

            private int hoveredSuggestionIndex(int mouseX, int mouseY) {
                return this.suggestions.hoveredIndex(mouseX, mouseY, this.lastDropdownX, this.lastDropdownY, this.lastDropdownWidth, this.lastDropdownHeight, SUGGESTION_ROW_HEIGHT);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.input, this.moveUpButton, this.moveDownButton, this.removeButton);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(this.input, this.moveUpButton, this.moveDownButton, this.removeButton);
            }
        }
    }

}
//?}
