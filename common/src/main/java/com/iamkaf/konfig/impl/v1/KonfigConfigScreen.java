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
    static final int LIST_BOTTOM_MARGIN = 52;
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
    private final KonfigEditorHost editorHost;
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
        this.editorHost = new KonfigEditorHost(this);
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
        this.setScreen(new ColorEditorScreen(this.editorHost, entry));
    }

    void openStringListEditor(EntryRef entry) {
        this.setScreen(new StringListEditorScreen(this.editorHost, entry));
    }

    void returnToMainScreen() {
        this.rebuildScreenWidgets();
        this.setScreen(this);
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

    Object storedSnapshot(ConfigValueImpl<?> value) {
        return this.session.storedSnapshot(value);
    }

    KonfigStringListEditorState stringListEditorState(EntryRef entry, KonfigStringListEditorState.PersistAction persistAction) {
        return new KonfigStringListEditorState(this.session, entry, persistAction);
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

    boolean resetEntry(EntryRef entry) {
        try {
            this.session.resetEntry(entry);
            return true;
        } catch (RuntimeException exception) {
            KonfigToastSupport.resetFailed(exceptionMessage(exception));
            return false;
        }
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


}
//?}