//? if <=1.15.2 {
package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

@ApiStatus.Internal
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

import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

@ApiStatus.Internal
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

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.*;
import static com.iamkaf.konfig.impl.v1.KonfigScreenMetrics.*;
import static com.iamkaf.konfig.impl.v1.KonfigUiAdapter.*;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigDebugConfig;
import com.iamkaf.konfig.api.v1.ImageOptions;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
import net.minecraft.util.Util;
//?} else {
import net.minecraft.Util;
//?}

import java.util.List;
import java.net.URI;

@ApiStatus.Internal
public final class KonfigConfigScreen extends Screen {
    private final Screen parent;
    private final String modIdFilter;
    private final String screenTitle;
    private final KonfigScreenCoordinator coordinator;
    private final KonfigRowHost rowHost;
    private final KonfigRowFactory rowFactory;
    private final KonfigEditorHost editorHost;
    private final KonfigInfoPanelRenderer infoPanelRenderer = new KonfigInfoPanelRenderer();

    private KonfigEntryList list;

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
        List<EntryRef> entries = collectEntries(modIdFilter);
        this.coordinator = new KonfigScreenCoordinator(entries);
        this.rowHost = new KonfigRowHost(this, this.coordinator);
        this.rowFactory = new KonfigRowFactory(this.rowHost);
        this.editorHost = new KonfigEditorHost(this, this.coordinator);
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info(
                    "[Konfig/Debug] creating screen parent={} modFilter={} entries={}",
                    parent == null ? "null" : parent.getClass().getName(),
                    modIdFilter == null ? "<all>" : modIdFilter,
                    entries.size()
            );
        }
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
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownClick(event)) {
            return true;
        }
        if (event.button() == 0 && this.handleInfoPanelClick(event.x(), event.y())) {
            return true;
        }
        RegistryTextInputRow activeRegistry = this.coordinator.activeRegistryRow();
        if (activeRegistry != null && activeRegistry.handleSuggestionClick(event)) {
            return true;
        }

        boolean handled = super.mouseClicked(event, doubleClick);
        activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null
                && !activeDropdown.isPointInsideButton(event.x(), event.y())
                && !activeDropdown.isPointInsideDropdown(event.x(), event.y())) {
            activeDropdown.closeDropdown();
        }
        RegistryTextInputRow focusedRow = this.findFocusedRegistryRow();
        if (focusedRow != null) {
            this.coordinator.setActiveRegistryRow(focusedRow);
            focusedRow.activateSuggestions();
        } else {
            activeRegistry = this.coordinator.activeRegistryRow();
            if (activeRegistry != null && !activeRegistry.isPointInsideInput(event.x(), event.y())) {
                activeRegistry.closeSuggestions();
            }
        }

        return handled;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownKey(event)) {
            return true;
        }
        DropdownRow focusedDropdown = this.findFocusedDropdownRow();
        if (focusedDropdown != null && focusedDropdown.handleClosedDropdownKey(event.key())) {
            return true;
        }
        RegistryTextInputRow activeRegistry = this.coordinator.activeRegistryRow();
        if (activeRegistry != null && activeRegistry.handleSuggestionKey(event)) {
            return true;
        }
        boolean handled = super.keyPressed(event);
        activeRegistry = this.coordinator.activeRegistryRow();
        if (activeRegistry != null && activeRegistry.isFocused()) {
            activeRegistry.refreshSuggestions();
        }
        return handled;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownChar(event.codepoint())) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownScroll(mouseX, mouseY, scrollY)) {
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
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownClick(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && this.handleInfoPanelClick(mouseX, mouseY)) {
            return true;
        }
        RegistryTextInputRow activeRegistry = this.coordinator.activeRegistryRow();
        if (activeRegistry != null && activeRegistry.handleSuggestionClick(mouseX, mouseY)) {
            return true;
        }

        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null
                && !activeDropdown.isPointInsideButton(mouseX, mouseY)
                && !activeDropdown.isPointInsideDropdown(mouseX, mouseY)) {
            activeDropdown.closeDropdown();
        }
        RegistryTextInputRow focusedRow = this.findFocusedRegistryRow();
        if (focusedRow != null) {
            this.coordinator.setActiveRegistryRow(focusedRow);
            focusedRow.activateSuggestions();
        } else {
            activeRegistry = this.coordinator.activeRegistryRow();
            if (activeRegistry != null && !activeRegistry.isPointInsideInput(mouseX, mouseY)) {
                activeRegistry.closeSuggestions();
            }
        }

        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownKey(keyCode)) {
            return true;
        }
        DropdownRow focusedDropdown = this.findFocusedDropdownRow();
        if (focusedDropdown != null && focusedDropdown.handleClosedDropdownKey(keyCode)) {
            return true;
        }
        RegistryTextInputRow activeRegistry = this.coordinator.activeRegistryRow();
        if (activeRegistry != null && activeRegistry.handleSuggestionKey(keyCode)) {
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        activeRegistry = this.coordinator.activeRegistryRow();
        if (activeRegistry != null && activeRegistry.isFocused()) {
            activeRegistry.refreshSuggestions();
        }
        return handled;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownChar(codePoint)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

//? if >=1.20.2 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownScroll(mouseX, mouseY, scrollY)) {
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
        DropdownRow activeDropdown = this.coordinator.activeDropdownRow();
        if (activeDropdown != null && activeDropdown.handleDropdownScroll(mouseX, mouseY, delta)) {
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
        this.coordinator.beginFrame(this.infoPanelBounds(), mouseX, mouseY);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void renderMainScreenChrome(KonfigRenderContext context, int mouseX, int mouseY) {
        context.drawCenteredText(this.font, screenTitle(), this.width / 2, 8, 0xFFFFFFFF);
        context.fill(this.mainPanelRight(), LIST_TOP, this.mainPanelRight() + 1, this.height, 0xFF202020);
        this.renderInfoPanel(context, mouseX, mouseY);
        if (this.coordinator.entries().isEmpty()) {
            context.drawCenteredText(this.font, translate("konfig.screen.empty"), this.mainPanelRight() / 2, this.height / 2 - 10, 0xFFC0C0C0);
        }
        // Painter order matters here: side panels first, floating controls next, queued tooltips last.
        this.coordinator.renderFloatingLayers(context, this, this.font, mouseX, mouseY);
    }

    private void renderInfoPanel(KonfigRenderContext context, int mouseX, int mouseY) {
        this.coordinator.updateActiveDropdownOptionInfo(mouseX, mouseY);
        this.infoPanelRenderer.render(
                context,
                this.font,
                this.coordinator.infoPanel(),
                this.infoPanelBounds(),
                this.coordinator::selectedDropdownOptionInfo,
                mouseX,
                mouseY
        );
    }

    void queueTooltip(String tooltip, int mouseX, int mouseY) {
        this.coordinator.queueTooltip(tooltip, mouseX, mouseY);
    }

    private void rebuildScreenWidgets() {
        this.clearWidgets();
        this.coordinator.clearRebuiltWidgetState();

        int listHeight = Math.max(48, this.height - LIST_TOP - LIST_BOTTOM_MARGIN);
//? if <=1.19.3 {
        this.list = this.addWidget(new KonfigEntryList(this.minecraft, this.mainPanelRight(), this.height, listHeight, LIST_TOP, this.mainPanelRight() - 28));
//?} else {
        this.list = this.addRenderableWidget(new KonfigEntryList(this.minecraft, this.mainPanelRight(), this.height, listHeight, LIST_TOP, this.mainPanelRight() - 28));
//?}
        for (EntryRef entry : this.coordinator.entries()) {
            this.list.addKonfigEntry(this.rowFactory.create(entry));
        }

        int footerY = this.height - 26;
        int footerCenter = this.mainPanelRight() / 2;
        this.addRenderableWidget(button(footerCenter - 82, footerY, 80, 20, translate("konfig.screen.reset"), button -> this.resetEntries()));
        this.addRenderableWidget(button(footerCenter + 2, footerY, 80, 20, translate("konfig.screen.done"), button -> this.onClose()));
    }

    int screenHeight() {
        return this.height;
    }

    private void resetEntries() {
        this.coordinator.resetAll();
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

    private boolean handleInfoPanelClick(double mouseX, double mouseY) {
        String target = this.coordinator.clickedInfoPanelLink(this.infoPanelBounds(), mouseX, mouseY);
        if (target == null) {
            return false;
        }
        this.openUrl(target);
        return true;
    }

    private boolean handleInfoPanelScroll(double mouseX, double mouseY, double scrollY) {
        return this.coordinator.handleInfoPanelScroll(this.infoPanelBounds(), mouseX, mouseY, scrollY);
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
        if (this.list == null || this.coordinator.activeDropdownRow() != null) {
            return null;
        }
        for (KonfigConfigRow row : this.list.children()) {
            if (row instanceof DropdownRow dropdownRow && dropdownRow.isButtonFocused()) {
                return dropdownRow;
            }
        }
        return null;
    }

}
//?}
