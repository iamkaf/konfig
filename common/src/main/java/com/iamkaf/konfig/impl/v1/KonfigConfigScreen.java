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
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.Font;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;

public final class KonfigConfigScreen extends Screen {
    static final int LIST_TOP = 28;
    private static final int LIST_BOTTOM_MARGIN = 52;
    static final int ROW_HEIGHT = 34;
    static final int CONTROL_HEIGHT = 20;
    static final int CONTROL_MIN_WIDTH = 132;
    static final int CONTROL_MAX_WIDTH = 200;
    static final int VALIDATION_COLOR = 0xFFFF8080;
    static final int URL_BUTTON_WIDTH = 60;
    static final int SUGGESTION_LIMIT = 7;
    static final int SUGGESTION_ROW_HEIGHT = 14;
    static final int DROPDOWN_CHEVRON_WIDTH = 16;
    static final long DROPDOWN_TYPE_SELECT_RESET_MS = 1000L;

    private final Screen parent;
    private final String modIdFilter;
    private final String screenTitle;
    private final List<EntryRef> entries;
    private final KonfigScreenSession session;
    private final KonfigRowHost rowHost;
    private final KonfigInfoPanelState infoPanel;
    private final KonfigInfoPanelRenderer infoPanelRenderer = new KonfigInfoPanelRenderer();
    private final Map<ResourceKey<? extends Registry<?>>, List<String>> registrySuggestionCache = new LinkedHashMap<ResourceKey<? extends Registry<?>>, List<String>>();

    private EntryList list;
    private RegistryTextInputRow activeRegistryRow;
    private RegistryTextInputRow renderedRegistryRow;
    private DropdownRow activeDropdownRow;
    private DropdownRow renderedDropdownRow;
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
        this.rowHost = new KonfigRowHost(this);
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] creating screen parent={} modFilter={} entries={}",
                    parent == null ? "null" : parent.getClass().getName(),
                    modIdFilter == null ? "<all>" : modIdFilter,
                    this.entries.size()
            );
        }
        this.session = new KonfigScreenSession(this.entries);
        this.infoPanel = new KonfigInfoPanelState(this.entries);
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
        for (KonfigConfigRow row : this.list.children()) {
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

    void openInlineUrl(EntryRef entry) {
        this.openUrl(entry.value.inlineUrl());
    }

    void openColorEditor(EntryRef entry) {
        this.setScreen(new ColorEditorScreen(entry));
    }

    void openStringListEditor(EntryRef entry) {
        this.setScreen(new StringListEditorScreen(entry));
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
        this.renderMainScreenChrome(context, mouseX, mouseY);
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.beginMainScreenRender(context, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderMainScreenChrome(context, mouseX, mouseY);
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
        this.renderMainScreenChrome(context, mouseX, mouseY);
    }
//?}

    private void beginMainScreenRender(KonfigRenderContext context, int mouseX, int mouseY) {
        this.renderedRegistryRow = null;
        this.renderedDropdownRow = null;
        this.pendingTooltip = null;
        this.infoPanel.beginFrame(this.infoPanelBounds(), mouseX, mouseY);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void renderMainScreenChrome(KonfigRenderContext context, int mouseX, int mouseY) {
        context.drawCenteredText(this.font, screenTitle(), this.width / 2, 8, 0xFFFFFFFF);
        context.fill(this.mainPanelRight(), LIST_TOP, this.mainPanelRight() + 1, this.height, 0xFF202020);
        this.renderInfoPanel(context, mouseX, mouseY);
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

    private void renderInfoPanel(KonfigRenderContext context, int mouseX, int mouseY) {
        this.updateActiveDropdownOptionInfo(mouseX, mouseY);
        this.infoPanelRenderer.render(
                context,
                this.font,
                this.infoPanel,
                this.infoPanelBounds(),
                this::selectedDropdownOptionInfo,
                mouseX,
                mouseY
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

    private KonfigConfigRow createRow(EntryRef entry) {
        if (entry.value.kind() == EntryKind.HEADER) {
            return new HeaderRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.IMAGE) {
            return new ImageRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.INLINE_TEXT) {
            return new InlineTextRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.URL) {
            return new UrlRow(this.rowHost, entry);
        }
        if (!entry.editable) {
            return new UnsupportedRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.BOOLEAN) {
            return new BooleanRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.ENUM) {
            return new EnumRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.COLOR_RGB || entry.value.kind() == EntryKind.COLOR_ARGB) {
            return new ColorRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.STRING_LIST) {
            return new StringListRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.DROPDOWN) {
            return new DropdownRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.INTEGER && entry.value.hasNumericRange()) {
            return new IntegerSliderRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.LONG && entry.value.hasNumericRange()) {
            return new LongSliderRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.DOUBLE && entry.value.hasNumericRange()) {
            return new DoubleSliderRow(this.rowHost, entry);
        }
        if (entry.value.kind() == EntryKind.STRING && entry.value.hasBoundRegistry()) {
            return new RegistryTextInputRow(this.rowHost, entry);
        }
        return new TextInputRow(this.rowHost, entry);
    }

    int screenHeight() {
        return this.height;
    }

    boolean persistEntry(EntryRef entry) {
        try {
            this.session.persist(entry);
            return true;
        } catch (RuntimeException exception) {
            KonfigToastSupport.saveFailed(exceptionMessage(exception));
            return false;
        }
    }

    void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.session.setDraft(value, draft);
    }

    Object draft(ConfigValueImpl<?> value) {
        return this.session.draft(value);
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
        return this.infoPanelRenderer.widthFor(this.width);
    }

    private int mainPanelRight() {
        return this.width - this.infoPanelWidth();
    }

    private KonfigInfoPanelBounds infoPanelBounds() {
        int panelLeft = this.mainPanelRight() + 1;
        return new KonfigInfoPanelBounds(
                panelLeft,
                LIST_TOP,
                this.width,
                this.height,
                this.mainPanelRight() - 24,
                LIST_TOP,
                panelLeft,
                this.height - LIST_BOTTOM_MARGIN
        );
    }

    void updateHoveredEntry(EntryRef entry, boolean hovered) {
        this.infoPanel.updateHoveredEntry(entry, hovered);
    }

    private void updateActiveDropdownOptionInfo(int mouseX, int mouseY) {
        if (this.activeDropdownRow == null) {
            this.infoPanel.setActiveDropdownOptionInfo(null, Collections.emptyList());
            return;
        }

        DropdownOptionMetadata option = this.activeDropdownRow.activeInfoOption(mouseX, mouseY);
        if (option == null || option.info().isEmpty()) {
            this.infoPanel.setActiveDropdownOptionInfo(null, Collections.emptyList());
            return;
        }

        this.infoPanel.setActiveDropdownOptionInfo(this.activeDropdownRow.entry, option.info());
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

    private boolean handleInfoPanelClick(double mouseX, double mouseY) {
        String target = this.infoPanel.clickedLink(this.infoPanelBounds(), mouseX, mouseY);
        if (target == null) {
            return false;
        }
        this.openUrl(target);
        return true;
    }

    private boolean handleInfoPanelScroll(double mouseX, double mouseY, double scrollY) {
        return this.infoPanel.handleScroll(this.infoPanelBounds(), mouseX, mouseY, scrollY);
    }

    private Component screenTitle() {
        if (!isBlank(this.screenTitle)) {
            return text(this.screenTitle);
        }
        return this.title;
    }

    Font rowFont() {
        return this.font;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    boolean readBoolean(ConfigValueImpl<?> value) {
        return this.session.readBoolean(value);
    }

    Enum<?> currentEnum(ConfigValueImpl<?> value) {
        return this.session.currentEnum(value);
    }

    Enum<?> cycleEnum(ConfigValueImpl<?> value) {
        return this.session.nextEnum(value);
    }

    int currentColor(ConfigValueImpl<?> value) {
        return this.session.currentColor(value);
    }

    List<String> currentStringList(ConfigValueImpl<?> value) {
        return this.session.currentStringList(value);
    }

    String currentDropdownValue(ConfigValueImpl<?> value) {
        return this.session.currentDropdownValue(value);
    }

    int currentInt(ConfigValueImpl<?> value) {
        return this.session.currentInt(value);
    }

    long currentLong(ConfigValueImpl<?> value) {
        return this.session.currentLong(value);
    }

    double currentDouble(ConfigValueImpl<?> value) {
        return this.session.currentDouble(value);
    }

    Component booleanText(ConfigValueImpl<?> value) {
        return CommonComponents.optionStatus(readBoolean(value));
    }

    Component enumText(EntryRef entry, Enum<?> value) {
        return translatedEnumValue(entry, value);
    }

    Component colorText(ConfigValueImpl<?> value) {
        int color = currentColor(value);
        if (value.kind() == EntryKind.COLOR_ARGB) {
            return text(ColorValueHelper.formatArgb(color));
        }
        return text(ColorValueHelper.formatRgb(color));
    }

    Component stringListText(ConfigValueImpl<?> value) {
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

    Component dropdownText(EntryRef entry, String option) {
        DropdownOptionMetadata metadata = entry.value.dropdownOption(option);
        return metadata == null ? translatedDropdownValue(entry, option) : translatedDropdownOption(entry, metadata);
    }

    String currentStringValue(ConfigValueImpl<?> value) {
        return this.session.currentStringValue(value);
    }

    private RegistryTextInputRow findFocusedRegistryRow() {
        if (this.list == null) {
            return null;
        }
        for (KonfigConfigRow row : this.list.children()) {
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
        for (KonfigConfigRow row : this.list.children()) {
            if (row instanceof DropdownRow dropdownRow && dropdownRow.isButtonFocused()) {
                return dropdownRow;
            }
        }
        return null;
    }

    void setActiveRegistryRow(RegistryTextInputRow row) {
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

    boolean isActiveRegistryRow(RegistryTextInputRow row) {
        return this.activeRegistryRow == row;
    }

    void clearActiveRegistryRow(RegistryTextInputRow row) {
        if (this.activeRegistryRow == row) {
            this.activeRegistryRow = null;
        }
    }

    void markRenderedRegistryRow(RegistryTextInputRow row) {
        this.renderedRegistryRow = row;
    }

    void setActiveDropdownRow(DropdownRow row) {
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

    void clearActiveDropdownRow(DropdownRow row) {
        if (this.activeDropdownRow == row) {
            this.activeDropdownRow = null;
        }
    }

    void clearRenderedDropdownRow(DropdownRow row) {
        if (this.renderedDropdownRow == row) {
            this.renderedDropdownRow = null;
        }
    }

    void markRenderedDropdownRow(DropdownRow row) {
        this.renderedDropdownRow = row;
    }

    List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
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


    private final class EntryList extends ContainerObjectSelectionList<KonfigConfigRow> {
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

        private void addKonfigEntry(KonfigConfigRow row) {
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

        private final KonfigStringListEditorState editorState;
        private ListEntryList list;
        private ListEntryRow activeRegistryRow;
        private ListEntryRow renderedRegistryRow;

        private StringListEditorScreen(EntryRef entry) {
            super(entry);
            this.editorState = new KonfigStringListEditorState(KonfigConfigScreen.this.session, entry, this::persistEditedValue);
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
            String count = translate("konfig.screen.list.count", Integer.valueOf(this.editorState.size())).getString();
            context.drawText(this.font, text(count), this.width - 12 - this.font.width(count), EDITOR_CONTEXT_Y, 0xFFC0C0C0);
            if (this.editorState.isEmpty()) {
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
            List<String> values = this.editorState.values();
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
            if (this.editorState.add(translate("konfig.screen.list.new_item").getString())) {
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
                if (!StringListEditorScreen.this.editorState.set(this.index, value)) {
                    this.suppressResponder = true;
                    this.input.setValue(StringListEditorScreen.this.editorState.valueAt(this.index));
                    this.suppressResponder = false;
                    this.refreshSuggestions();
                    return false;
                }
                this.refreshSuggestions();
                return true;
            }

            private void move(int delta) {
                if (StringListEditorScreen.this.editorState.move(this.index, delta)) {
                    StringListEditorScreen.this.rebuildEditorWidgets();
                }
            }

            private void remove() {
                if (StringListEditorScreen.this.editorState.remove(this.index)) {
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
                this.moveUpButton.active = StringListEditorScreen.this.editorState.canMoveUp(this.index);
                this.positionButton(this.moveDownButton, downX, buttonY);
                this.moveDownButton.active = StringListEditorScreen.this.editorState.canMoveDown(this.index);
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
