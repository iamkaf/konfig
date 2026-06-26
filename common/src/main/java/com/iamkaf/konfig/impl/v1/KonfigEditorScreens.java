//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import static com.iamkaf.konfig.impl.v1.KonfigRegistryAdapter.supportsRegistryIcon;
import static com.iamkaf.konfig.impl.v1.KonfigScreenSupport.*;
import static com.iamkaf.konfig.impl.v1.KonfigUiAdapter.button;

import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} elif >=1.20 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import com.mojang.blaze3d.vertex.PoseStack;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

abstract class KonfigEntryEditorScreen extends Screen {
    protected static final int EDITOR_TITLE_Y = 8;
    protected static final int EDITOR_CONTEXT_Y = 24;
    protected static final int EDITOR_CONTENT_TOP = 42;

    protected final KonfigEditorHost host;
    protected final EntryRef entry;

    KonfigEntryEditorScreen(KonfigEditorHost host, EntryRef entry) {
        super(entry.label);
        this.host = host;
        this.entry = entry;
    }

    @Override
    public void onClose() {
        this.returnToParent();
    }

    protected final void returnToParent() {
        this.host.returnToMainScreen();
    }

    protected final boolean persistEditedValue(Object previousValue) {
        return this.host.persistEditedValue(this.entry, previousValue);
    }

    protected final boolean resetToSessionStart() {
        return this.host.resetToSessionStart(this.entry);
    }

//? if >=26.1 {
    protected final KonfigRenderContext renderEditorChrome(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.renderEditorBackground(context);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        this.renderEditorTitle(context);
        return context;
    }
//?} elif >=1.20 {
    protected final KonfigRenderContext renderEditorChrome(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.renderEditorBackground(context);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderEditorTitle(context);
        return context;
    }
//?} else {
    protected final KonfigRenderContext renderEditorChrome(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(guiGraphics);
        this.renderEditorBackground(context);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderEditorTitle(context);
        return context;
    }
//?}

    private void renderEditorBackground(KonfigRenderContext context) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    private void renderEditorTitle(KonfigRenderContext context) {
        context.drawCenteredText(this.font, this.title, this.width / 2, EDITOR_TITLE_Y, 0xFFFFFFFF);
        context.drawText(this.font, this.entry.contextLabel, 12, EDITOR_CONTEXT_Y, 0xFFA0A0A0);
    }
}

enum ColorChannel {
    RED("konfig.screen.color.red"),
    GREEN("konfig.screen.color.green"),
    BLUE("konfig.screen.color.blue"),
    ALPHA("konfig.screen.color.alpha");

    final String translationKey;

    ColorChannel(String translationKey) {
        this.translationKey = translationKey;
    }
}

final class ColorEditorScreen extends KonfigEntryEditorScreen {
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

    ColorEditorScreen(KonfigEditorHost host, EntryRef entry) {
        super(host, entry);
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
        this.renderColorEditorOverlay(this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick));
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderColorEditorOverlay(this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick));
    }
//?} else {
    @Override
    public void render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderColorEditorOverlay(this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick));
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

        Object previousValue = this.host.storedSnapshot(this.entry.value);
        try {
            int parsed = parseColor(this.entry.value, value);
            this.host.setDraft(this.entry.value, Integer.valueOf(parsed));
            if (this.persistEditedValue(previousValue)) {
                this.validationMessage = "";
                this.syncWidgetsFromDraft();
            } else {
                this.syncWidgetsFromDraft();
            }
        } catch (Exception exception) {
            this.host.setDraft(this.entry.value, previousValue);
            this.validationMessage = exception.getMessage() == null
                    ? translate("konfig.screen.color.invalid", Integer.valueOf(expectedDigits)).getString()
                    : exception.getMessage();
            this.syncWidgetsFromDraft();
        }
    }

    private void renderColorEditorOverlay(KonfigRenderContext context) {
        int previewX = this.width / 2 - PREVIEW_SIZE / 2;
        context.drawColorSwatch(previewX, PREVIEW_Y, PREVIEW_SIZE, this.host.currentColor(this.entry.value), this.entry.value.kind());
        this.renderValidationMessage(context);
    }

    private void renderValidationMessage(KonfigRenderContext context) {
        if (!this.validationMessage.isEmpty()) {
            context.drawCenteredText(this.font, text(this.validationMessage), this.width / 2, HEX_Y + KonfigScreenMetrics.CONTROL_HEIGHT + 3, KonfigScreenMetrics.VALIDATION_COLOR);
        }
    }

    private String currentHex() {
        int color = this.host.currentColor(this.entry.value);
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
        int color = this.host.currentColor(this.entry.value);
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
        int current = this.host.currentColor(this.entry.value);
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
            ColorEditorScreen.this.host.setDraft(ColorEditorScreen.this.entry.value, Integer.valueOf(ColorEditorScreen.this.withChannel(this.channel, intFromProgress(this.value, 0, 255))));
        }

//? if >=1.21.9 {
        @Override
        public void onRelease(MouseButtonEvent event) {
            Object previousValue = ColorEditorScreen.this.host.storedSnapshot(ColorEditorScreen.this.entry.value);
            super.onRelease(event);
            if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                ColorEditorScreen.this.syncWidgetsFromDraft();
            }
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            Object previousValue = ColorEditorScreen.this.host.storedSnapshot(ColorEditorScreen.this.entry.value);
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
            Object previousValue = ColorEditorScreen.this.host.storedSnapshot(ColorEditorScreen.this.entry.value);
            super.onRelease(mouseX, mouseY);
            if (ColorEditorScreen.this.persistEditedValue(previousValue)) {
                ColorEditorScreen.this.syncWidgetsFromDraft();
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            Object previousValue = ColorEditorScreen.this.host.storedSnapshot(ColorEditorScreen.this.entry.value);
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

final class StringListEditorScreen extends KonfigEntryEditorScreen {
    private static final int ITEM_ROW_HEIGHT = 28;

    private final KonfigStringListEditorState editorState;
    private ListEntryList list;
    private ListEntryRow activeRegistryRow;
    private ListEntryRow renderedRegistryRow;

    StringListEditorScreen(KonfigEditorHost host, EntryRef entry) {
        super(host, entry);
        this.editorState = host.stringListEditorState(entry, this::persistEditedValue);
    }

    @Override
    protected void init() {
        this.rebuildEditorWidgets();
    }

//? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderedRegistryRow = null;
        this.renderStringListEditorOverlay(this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick), mouseX, mouseY);
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderedRegistryRow = null;
        this.renderStringListEditorOverlay(this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick), mouseX, mouseY);
    }
//?} else {
    @Override
    public void render(PoseStack guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderedRegistryRow = null;
        this.renderStringListEditorOverlay(this.renderEditorChrome(guiGraphics, mouseX, mouseY, partialTick), mouseX, mouseY);
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
        int listHeight = Math.max(48, this.height - listTop - KonfigScreenMetrics.LIST_BOTTOM_MARGIN);
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
            this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.getX(), this.getY(), this.getRight(), this.getBottom());
            super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
//?} elif >=1.20.3 {
        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.getX(), this.getY(), this.getRight(), this.getBottom());
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        }
//?} elif >=1.20 {
        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.x0, this.y0, this.x1, this.y1);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }
//?} else {
        @Override
        protected void renderBackground(PoseStack guiGraphics) {
            this.renderListBackground(KonfigRenderContext.of(guiGraphics), this.x0, this.y0, this.x1, this.y1);
        }
//?}

        private void renderListBackground(KonfigRenderContext context, int left, int top, int right, int bottom) {
            context.fill(left, top, right, bottom, 0x66000000);
        }
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
            this.input = new EditBox(StringListEditorScreen.this.font, 0, 0, 140, KonfigScreenMetrics.CONTROL_HEIGHT, StringListEditorScreen.this.entry.label);
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
                    && mouseY <= this.lastInputY + KonfigScreenMetrics.CONTROL_HEIGHT;
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
                    StringListEditorScreen.this.host.registrySuggestions(this.registryKey()),
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
                    StringListEditorScreen.this.host.registrySuggestions(this.registryKey()),
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
                int rowY = this.lastDropdownY + 2 + (suggestionIndex * KonfigScreenMetrics.SUGGESTION_ROW_HEIGHT);
                int rowBottom = rowY + KonfigScreenMetrics.SUGGESTION_ROW_HEIGHT;
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
            this.lastDropdownHeight = (this.suggestions.size() * KonfigScreenMetrics.SUGGESTION_ROW_HEIGHT) + 4;
            int belowY = this.lastInputY + KonfigScreenMetrics.CONTROL_HEIGHT + 2;
            int aboveY = this.lastInputY - this.lastDropdownHeight - 2;
            boolean openAbove = belowY + this.lastDropdownHeight > StringListEditorScreen.this.height - 32 && aboveY >= KonfigScreenMetrics.LIST_TOP;
            this.lastDropdownY = openAbove ? aboveY : belowY;
        }

        private int hoveredSuggestionIndex(int mouseX, int mouseY) {
            return this.suggestions.hoveredIndex(mouseX, mouseY, this.lastDropdownX, this.lastDropdownY, this.lastDropdownWidth, this.lastDropdownHeight, KonfigScreenMetrics.SUGGESTION_ROW_HEIGHT);
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
//?}
