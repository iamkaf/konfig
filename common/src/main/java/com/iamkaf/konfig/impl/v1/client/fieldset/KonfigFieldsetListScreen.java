package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.hasRegistryIcon;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.tagSuggestions;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.focus;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.moveCursorToStart;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.place;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.setHint;
import static com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSupport.text;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetFieldKind;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValidationIssue;
import com.iamkaf.konfig.impl.v1.client.control.KonfigRegistrySuggestionController;
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.iamkaf.konfig.impl.v1.client.row.KonfigListRow;
import com.iamkaf.konfig.impl.v1.client.row.KonfigSelectionList;
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@ApiStatus.Internal
final class KonfigFieldsetListScreen extends Screen {
    private static final int SEARCH_Y = 38;
    private static final int LIST_TOP = 64;
    private static final int COLLAPSED_HEIGHT = 40;
    private static final int FIELD_HEIGHT = 38;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CARD_GAP = 4;
    private static final int ICON_SIZE = 20;

    private final Screen parent;
    private final Component context;
    private final KonfigFieldsetDraftSession session;
    private final KonfigFieldsetDraftAdapter adapter;
    private final KonfigFieldsetListEditorState<FieldsetEntry, FieldsetField<?>> state;
    private final KonfigFieldsetScreens.RegistrySuggestions registrySuggestions;
    private final KonfigFieldsetScreens.PersistAction persistAction;
    private final KonfigFieldsetScreens.Subscription persistenceSubscription;

    private EditBox search;
    private EntryList list;
    private Button add;
    private Button duplicate;
    private Button delete;
    private Button moveUp;
    private Button moveDown;
    private EntryRow.TextControl activeRegistryControl;
    private EntryRow.TextControl renderedRegistryControl;
    private Component message = text("");
    private boolean suppressSearchResponder;
    private boolean rebuildPending;
    private boolean revealPending;
    private boolean savePending;
    private boolean drawSearchHint;
    private int searchX;

    KonfigFieldsetListScreen(
            Screen parent,
            Component title,
            Component context,
            KonfigFieldsetDraftSession session,
            KonfigFieldsetDraftAdapter adapter,
            KonfigFieldsetScreens.RegistrySuggestions registrySuggestions,
            KonfigFieldsetScreens.PersistAction persistAction
    ) {
        super(Objects.requireNonNull(title, "title"));
        this.parent = parent;
        this.context = Objects.requireNonNull(context, "context");
        this.session = Objects.requireNonNull(session, "session");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.state = new KonfigFieldsetListEditorState<>(adapter);
        this.registrySuggestions = Objects.requireNonNull(registrySuggestions, "registrySuggestions");
        this.persistAction = Objects.requireNonNull(persistAction, "persistAction");
        this.persistenceSubscription = this.persistAction.observe(this::completePersist);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int contentWidth = Math.min(440, Math.max(260, this.width - 28));
        int contentX = (this.width - contentWidth) / 2;

        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                contentX,
                SEARCH_Y,
                contentWidth,
                20,
                text("Search fieldset entries")
        ));
        this.drawSearchHint = !setHint(this.search, text("Search"));
        this.searchX = contentX;
        this.search.setValue(this.state.query());
        this.search.setResponder(this::searchChanged);

        int listBottom = Math.max(LIST_TOP + COLLAPSED_HEIGHT, this.height - 60);
        this.list = this.addRenderableWidget(new EntryList(contentWidth, listBottom - LIST_TOP, LIST_TOP));
        this.list.setLeft(contentX);
        this.list.rebuild(false);

        int actionY = this.height - 52;
        int actionGap = 4;
        int addWidth = 68;
        int copyWidth = 64;
        int deleteWidth = 64;
        int moveWidth = 48;
        int spacer = 18;
        int actionsWidth = addWidth + spacer + copyWidth + deleteWidth + moveWidth * 2 + actionGap * 4;
        int actionX = this.width / 2 - actionsWidth / 2;

        this.add = this.addRenderableWidget(button(actionX, actionY, addWidth, 20, text("Add"), ignored -> {
            this.apply(this.state.add(), true);
        }));
        int selectedX = actionX + addWidth + spacer;
        this.duplicate = this.addRenderableWidget(button(selectedX, actionY, copyWidth, 20, text("Copy"), ignored -> {
            this.apply(this.state.duplicateSelected(), true);
        }));
        this.delete = this.addRenderableWidget(button(selectedX + copyWidth + actionGap, actionY, deleteWidth, 20, text("Delete"), ignored -> {
            this.apply(this.state.deleteSelected(), true);
        }));
        this.moveUp = this.addRenderableWidget(button(
                selectedX + copyWidth + deleteWidth + actionGap * 2,
                actionY,
                moveWidth,
                20,
                text("Up"),
                ignored -> this.apply(this.state.moveSelected(-1), true)
        ));
        this.moveDown = this.addRenderableWidget(button(
                selectedX + copyWidth + deleteWidth + moveWidth + actionGap * 3,
                actionY,
                moveWidth,
                20,
                text("Down"),
                ignored -> this.apply(this.state.moveSelected(1), true)
        ));

        int footerY = this.height - 26;
        this.addRenderableWidget(button(this.width / 2 - 80, footerY, 160, 20, text("Done"), ignored -> this.closeToParent()));
        this.refreshControls();
    }

    @Override
    public void onClose() {
        if (this.savePending) {
            return;
        }
        this.closeToParent();
    }

    @Override
    public void removed() {
        this.persistenceSubscription.unsubscribe();
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.list != null) {
            this.list.tickControls();
        }
        if (!this.rebuildPending || this.list == null) {
            return;
        }
        boolean reveal = this.revealPending;
        this.rebuildPending = false;
        this.revealPending = false;
        this.list.rebuild(reveal);
        this.refreshControls();
    }

    private void searchChanged(String query) {
        if (this.suppressSearchResponder) {
            return;
        }
        this.state.setQuery(query);
        if (this.list != null) {
            this.requestRebuild(false);
        }
    }

    private void setSearchValue(String value) {
        this.suppressSearchResponder = true;
        this.search.setValue(value);
        this.suppressSearchResponder = false;
    }

    private void requestRebuild(boolean revealSelection) {
        this.rebuildPending = true;
        this.revealPending |= revealSelection;
    }

    private void toggleEntry(String entryId) {
        if (this.state.toggleExpanded(entryId)) {
            this.message = text("");
            this.requestRebuild(true);
            this.refreshControls();
        }
    }

    private KonfigFieldsetEditResult persistDraft() {
        if (!this.session.dirty()) {
            return KonfigFieldsetEditResult.noChange();
        }
        List<FieldsetValidationIssue> issues = this.session.draft().validate().issues();
        if (!issues.isEmpty()) {
            this.session.restorePersisted();
            return KonfigFieldsetEditResult.invalid(text(issues.get(0).message()));
        }
        KonfigFieldsetEditResult result;
        try {
            result = Objects.requireNonNull(
                    this.persistAction.persist(this.session.original(), this.session.draft()),
                    "persistAction result"
            );
        } catch (RuntimeException exception) {
            String detail = exception.getMessage();
            this.session.restorePersisted();
            return KonfigFieldsetEditResult.invalid(text(detail == null || detail.isBlank()
                    ? "The fieldset could not be saved."
                    : detail));
        }
        if (result.status() == KonfigFieldsetEditResult.Status.PENDING) {
            this.savePending = true;
            this.message = result.message();
        } else if (result.accepted()) {
            this.session.markPersisted();
        } else {
            this.session.restorePersisted();
        }
        return result;
    }

    private void completePersist(KonfigFieldsetEditResult result, com.iamkaf.konfig.api.v1.fieldset.FieldsetValue authoritative) {
        if (!this.savePending) {
            return;
        }
        this.savePending = false;
        this.session.adoptPersisted(authoritative);
        this.message = result.accepted() ? text("") : result.message();
        this.state.refresh();
        this.requestRebuild(false);
        this.refreshControls();
    }

    private void apply(KonfigFieldsetEditResult result, boolean revealSelection) {
        if (result.accepted()) {
            result = this.persistDraft();
        }
        this.message = result.accepted() ? text("") : result.message();
        this.state.refresh();
        if (!this.search.getValue().equals(this.state.query())) {
            this.setSearchValue(this.state.query());
        }
        this.requestRebuild(revealSelection);
        this.refreshControls();
    }

    private void refreshControls() {
        if (this.add == null) {
            return;
        }
        this.add.active = !this.savePending && this.state.canAdd();
        this.duplicate.active = !this.savePending && this.state.canDuplicateSelected();
        this.delete.active = !this.savePending && this.state.canDeleteSelected();
        this.moveUp.active = !this.savePending && this.state.canMoveSelectedUp();
        this.moveDown.active = !this.savePending && this.state.canMoveSelectedDown();
    }

    private void closeToParent() {
        this.setScreen(this.parent);
    }

    private void setScreen(Screen screen) {
//? if >=26.2 {
        this.minecraft.gui.setScreen(screen);
//?} else {
        this.minecraft.setScreen(screen);
//?}
    }

//? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.handleClick(event.x(), event.y(), () -> super.mouseClicked(event, doubleClick));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.handleKey(event.key(), () -> super.keyPressed(event));
    }
//?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.handleClick(mouseX, mouseY, () -> super.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.handleKey(keyCode, () -> super.keyPressed(keyCode, scanCode, modifiers));
    }
//?}

    private boolean handleClick(double mouseX, double mouseY, BooleanSupplier vanilla) {
        if (this.savePending) {
            return true;
        }
        EntryRow.TextControl active = this.activeRegistryControl;
        // Only the popup drawn last frame takes clicks; a field scrolled out of the list hides its popup.
        EntryRow.TextControl shown = this.renderedRegistryControl;
        if (shown != null && shown.handleSuggestionClick(mouseX, mouseY)) {
            return true;
        }

        boolean handled = vanilla.getAsBoolean();
        EntryRow.TextControl focused = this.list == null ? null : this.list.focusedRegistryControl();
        if (focused != null) {
            this.activeRegistryControl = focused;
            focused.activateSuggestions();
        } else if (active != null && !active.isPointInsideInput(mouseX, mouseY)) {
            active.closeSuggestions();
            this.activeRegistryControl = null;
        }
        return handled;
    }

    private boolean handleKey(int keyCode, BooleanSupplier vanilla) {
        if (this.savePending) {
            return true;
        }
        EntryRow.TextControl active = this.activeRegistryControl;
        if (active != null && active == this.renderedRegistryControl && active.handleSuggestionKey(keyCode)) {
            return true;
        }
        boolean handled = vanilla.getAsBoolean();
        if (active != null && active.isFocused()) {
            active.refreshSuggestions();
        }
        return handled;
    }

//? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        this.renderedRegistryControl = null;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
        this.renderRegistrySuggestions(context, mouseX, mouseY);
    }
//?} elif >=1.20 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        this.renderedRegistryControl = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
        this.renderRegistrySuggestions(context, mouseX, mouseY);
    }
//?} else {
    @Override
    public void render(PoseStack graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        this.renderedRegistryControl = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
        this.renderRegistrySuggestions(context, mouseX, mouseY);
    }
//?}

    private void renderRegistrySuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
        EntryRow.TextControl rendered = this.renderedRegistryControl;
        if (rendered == null) {
            return;
        }
        context.renderFloatingLayers(
                layer -> rendered.renderSuggestions(layer, mouseX, mouseY),
                layer -> {
                }
        );
    }

    private void renderChrome(KonfigRenderContext context) {
        context.drawCenteredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        if (!this.message.getString().isBlank()) {
            context.drawCenteredText(this.font, this.message, this.width / 2, 22, 0xFFFF7070);
        } else {
            Component validation = this.adapter.validation().summary();
            if (!validation.getString().isBlank()) {
                context.drawCenteredText(this.font, validation, this.width / 2, 22, 0xFFFF7070);
            } else {
                context.drawCenteredText(this.font, this.context, this.width / 2, 22, 0xFFA0A0A0);
            }
        }
        if (this.drawSearchHint && this.search.getValue().isEmpty() && !this.search.isFocused()) {
            context.drawText(this.font, text("Search"), this.searchX + 4, SEARCH_Y + 6, 0xFF808080);
        }
    }

    private Component fit(Component value, int width) {
        String text = value.getString();
        if (this.font.width(text) <= width) {
            return value;
        }
        String suffix = "...";
        return text(this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width(suffix))) + suffix);
    }

    private final class EntryList extends KonfigSelectionList<EntryRow> {
        private final int rowWidth;

        private EntryList(int width, int height, int y) {
            super(KonfigFieldsetListScreen.this.minecraft, width, height, y, COLLAPSED_HEIGHT);
            this.rowWidth = width - 18;
        }

        private void rebuild(boolean revealSelection) {
            double previousScroll = this.scrollOffset();
            KonfigFieldsetListScreen.this.activeRegistryControl = null;
            KonfigFieldsetListScreen.this.renderedRegistryControl = null;
            this.setFocused(null);
            this.clearEntries();
            for (KonfigFieldsetListEditorState.VisibleEntry<FieldsetEntry> visible : KonfigFieldsetListScreen.this.state.visibleEntries()) {
                EntryRow row = new EntryRow(visible);
                this.addRow(row, row.rowHeight());
            }
            this.setScrollAmount(previousScroll);
            if (revealSelection) {
                this.reveal(KonfigFieldsetListScreen.this.state.selectedEntryId());
            }
        }

        private void reveal(String entryId) {
            for (EntryRow row : this.children()) {
                if (row.entryId.equals(entryId)) {
                    this.revealRow(row);
                    return;
                }
            }
        }

        private void tickControls() {
            for (EntryRow row : this.children()) {
                row.tickControls();
            }
        }

        private EntryRow.TextControl focusedRegistryControl() {
            for (EntryRow row : this.children()) {
                EntryRow.TextControl focused = row.focusedRegistryControl();
                if (focused != null) {
                    return focused;
                }
            }
            return null;
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }
    }

    private final class EntryRow extends KonfigListRow<EntryRow> {
        private final String entryId;
        private final FieldsetEntry snapshot;
        private final boolean selected;
        private final boolean expanded;
        private final Button header;
        // The card's title and summary. UI tests read it as the row label; the header button narrates only the title.
        private Component label = text("");
        private final List<FieldControl> fields = new ArrayList<>();
        private final List<AbstractWidget> controls = new ArrayList<>();

        private EntryRow(KonfigFieldsetListEditorState.VisibleEntry<FieldsetEntry> visible) {
            this.entryId = visible.entry().identity();
            this.snapshot = visible.entry();
            this.selected = visible.selected();
            this.expanded = KonfigFieldsetListScreen.this.state.isExpanded(this.entryId);
            this.header = button(0, 0, 100, COLLAPSED_HEIGHT - CARD_GAP - 2, text(""), ignored -> {
                KonfigFieldsetListScreen.this.toggleEntry(this.entryId);
            });
            this.controls.add(this.header);

            if (this.expanded) {
                KonfigFieldsetEntryEditorState<FieldsetEntry, FieldsetField<?>> editor =
                        new KonfigFieldsetEntryEditorState<>(KonfigFieldsetListScreen.this.adapter, this.entryId);
                for (KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field : editor.fields()) {
                    FieldControl control = this.createFieldControl(field);
                    this.fields.add(control);
                    this.controls.addAll(control.controls);
                }
            }
            this.refreshHeaderNarration();
        }

        private int rowHeight() {
            return this.expanded
                    ? COLLAPSED_HEIGHT + this.fields.size() * FIELD_HEIGHT + 8
                    : COLLAPSED_HEIGHT;
        }

        private FieldControl createFieldControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
            FieldsetFieldKind kind = field.field().kind();
            if (kind == FieldsetFieldKind.BOOLEAN) {
                return new BooleanControl(field);
            }
            if (kind == FieldsetFieldKind.DROPDOWN) {
                return new DropdownControl(field);
            }
            return new TextControl(field);
        }

        private FieldsetEntry currentEntry() {
            return KonfigFieldsetListScreen.this.adapter.entries().stream()
                    .filter(entry -> entry.identity().equals(this.entryId))
                    .findFirst()
                    .orElse(this.snapshot);
        }

        private void refreshHeaderNarration() {
            String action = this.expanded ? "Collapse " : "Expand ";
            FieldsetEntry entry = this.currentEntry();
            String title = KonfigFieldsetListScreen.this.adapter.entryLabel(entry).getString();
            String summary = KonfigFieldsetListScreen.this.adapter.entrySummary(entry).getString();
            this.label = text(summary.isBlank() ? title : title + ", " + summary);
            this.header.setMessage(text(action + title));
        }

        private boolean hasLocalErrors() {
            for (FieldControl field : this.fields) {
                if (!field.localError.isBlank()) {
                    return true;
                }
            }
            return false;
        }

        private void tickControls() {
            for (FieldControl field : this.fields) {
                field.tick();
            }
        }

        private TextControl focusedRegistryControl() {
            for (FieldControl field : this.fields) {
                if (field instanceof TextControl text && text.hasRegistryBinding() && text.isFocused()) {
                    return text;
                }
            }
            return null;
        }

        @Override
        protected void renderRow(
                KonfigRenderContext context,
                int x,
                int y,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick
        ) {
            int cardBottom = y + this.rowHeight() - CARD_GAP;
            KonfigFieldsetValidation validation = KonfigFieldsetListScreen.this.adapter.validation().forEntry(this.entryId);
            int border = !validation.isValid() || this.hasLocalErrors()
                    ? 0xFFB84A4A
                    : this.selected ? 0xFFB8B8B8 : hovered ? 0xFF777777 : 0xFF454545;
            int body = this.expanded ? 0xF0202020 : 0xE81B1B1B;
            context.fill(x, y, x + width, cardBottom, border);
            context.fill(x + 1, y + 1, x + width - 1, cardBottom - 1, body);
            if (this.selected) {
                context.fill(x + 1, y + 1, x + 3, cardBottom - 1, 0xFFE0E0E0);
            }

            this.layoutHeader(x, y, width);
            this.renderHeader(context, x, y, width, validation);
            if (this.expanded) {
                context.fill(x + 8, y + COLLAPSED_HEIGHT - 3, x + width - 8, y + COLLAPSED_HEIGHT - 2, 0xFF3A3A3A);
                int fieldY = y + COLLAPSED_HEIGHT + 2;
                for (FieldControl field : this.fields) {
                    field.render(context, x + 8, fieldY, width - 16, mouseX, mouseY, partialTick);
                    fieldY += FIELD_HEIGHT;
                }
            }
        }

        private void layoutHeader(int x, int y, int width) {
            place(this.header, x + 1, y + 1, width - 2);
        }

        private void renderHeader(
                KonfigRenderContext context,
                int x,
                int y,
                int width,
                KonfigFieldsetValidation validation
        ) {
            FieldsetEntry entry = this.currentEntry();
            KonfigFieldsetAccess access = KonfigFieldsetListScreen.this.adapter.entryAccess(entry);
            String status = !validation.isValid() || this.hasLocalErrors()
                    ? Math.max(1, validation.errorCount()) + " invalid"
                    : access.isReadOnly() ? entry.source().orElse("Built in") : "";
            int statusWidth = status.isBlank() ? 0 : KonfigFieldsetListScreen.this.font.width(status) + 10;

            context.drawText(
                    KonfigFieldsetListScreen.this.font,
                    text(this.expanded ? "v" : ">"),
                    x + 9,
                    y + 9,
                    0xFFD0D0D0
            );

            int titleX = x + 22;
            Optional<KonfigFieldsetDraftAdapter.EntryIcon> icon = KonfigFieldsetListScreen.this.adapter.entryIcon(entry);
            if (icon.isPresent() && hasRegistryIcon(icon.get().registryKey(), icon.get().value())) {
                int iconY = y + (COLLAPSED_HEIGHT - CARD_GAP - ICON_SIZE) / 2;
                context.renderRegistryIcon(icon.get().registryKey(), icon.get().value(), titleX, iconY, ICON_SIZE);
                titleX += ICON_SIZE + 6;
            }

            int available = Math.max(40, x + width - statusWidth - 10 - titleX);
            Component title = KonfigFieldsetListScreen.this.fit(
                    KonfigFieldsetListScreen.this.adapter.entryLabel(entry),
                    available
            );
            Component summary = KonfigFieldsetListScreen.this.fit(
                    KonfigFieldsetListScreen.this.adapter.entrySummary(entry),
                    available
            );
            context.drawText(KonfigFieldsetListScreen.this.font, title, titleX, y + 6, 0xFFFFFFFF);
            if (!summary.getString().isBlank()) {
                context.drawText(KonfigFieldsetListScreen.this.font, summary, titleX, y + 23, 0xFFA8A8A8);
            }
            if (!status.isBlank()) {
                int color = !validation.isValid() || this.hasLocalErrors() ? 0xFFFF7070 : 0xFF8F8F8F;
                context.drawText(
                        KonfigFieldsetListScreen.this.font,
                        text(status),
                        x + width - statusWidth,
                        y + 14,
                        color
                );
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.controls;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.controls;
        }

        private abstract class FieldControl {
            final KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field;
            final List<AbstractWidget> controls = new ArrayList<>();
            String localError = "";

            FieldControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
                this.field = field;
            }

            final boolean apply(Object value) {
                KonfigFieldsetEditResult result = this.field.value().setDraft(value);
                if (result.accepted()) {
                    result = KonfigFieldsetListScreen.this.persistDraft();
                }
                this.localError = result.submitted() ? "" : result.message().getString();
                KonfigFieldsetListScreen.this.message = result.accepted() ? text("") : result.message();
                this.sync();
                EntryRow.this.refreshHeaderNarration();
                KonfigFieldsetListScreen.this.refreshControls();
                return result.submitted();
            }

            final String validationMessage() {
                if (!this.localError.isBlank()) {
                    return this.localError;
                }
                List<KonfigFieldsetValidation.Issue> issues = this.field.value().validation().issues();
                return issues.isEmpty() ? "" : issues.get(0).message().getString();
            }

            final void render(
                    KonfigRenderContext context,
                    int x,
                    int y,
                    int width,
                    int mouseX,
                    int mouseY,
                    float partialTick
            ) {
                int controlX = x + Math.max(112, width * 42 / 100);
                int controlWidth = Math.max(84, x + width - controlX);
                context.drawText(KonfigFieldsetListScreen.this.font, this.field.label(), x + 4, y + 8, 0xFFE8E8E8);
                this.layoutControls(controlX, y + 4, controlWidth);
                this.renderDecoration(context, controlX, y, controlWidth);
                for (AbstractWidget control : this.controls) {
                    context.renderWidget(control, mouseX, mouseY, partialTick);
                }
                String validation = this.validationMessage();
                if (!validation.isBlank()) {
                    context.drawText(
                            KonfigFieldsetListScreen.this.font,
                            KonfigFieldsetListScreen.this.fit(text(validation), controlWidth),
                            controlX,
                            y + 27,
                            0xFFFF7070
                    );
                }
            }

            void tick() {
            }

            void renderDecoration(KonfigRenderContext context, int controlX, int y, int controlWidth) {
            }

            abstract void layoutControls(int x, int y, int width);

            abstract void sync();
        }

        private final class BooleanControl extends FieldControl {
            private final Button toggle;

            private BooleanControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
                super(field);
                this.toggle = button(0, 0, 100, CONTROL_HEIGHT, text(""), ignored -> {
                    Object current = this.field.value().draft();
                    this.apply(Boolean.valueOf(!(current instanceof Boolean) || !((Boolean) current).booleanValue()));
                });
                this.toggle.active = field.value().access().canEdit();
                this.controls.add(this.toggle);
                this.sync();
            }

            @Override
            void layoutControls(int x, int y, int width) {
                place(this.toggle, x, y, width);
            }

            @Override
            void sync() {
                this.toggle.setMessage(text(Boolean.TRUE.equals(this.field.value().draft()) ? "On" : "Off"));
            }
        }

        private final class DropdownControl extends FieldControl {
            private final Button dropdown;

            private DropdownControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
                super(field);
                this.dropdown = button(0, 0, 100, CONTROL_HEIGHT, text(""), ignored -> this.next());
                this.dropdown.active = field.value().access().canEdit();
                this.controls.add(this.dropdown);
                this.sync();
            }

            private void next() {
                List<String> options = this.field.field().options();
                if (options.isEmpty()) {
                    return;
                }
                String current = String.valueOf(this.field.value().draft());
                int index = options.indexOf(current);
                this.apply(options.get((index + 1 + options.size()) % options.size()));
            }

            @Override
            void layoutControls(int x, int y, int width) {
                place(this.dropdown, x, y, width);
            }

            @Override
            void sync() {
                this.dropdown.setMessage(text(String.valueOf(this.field.value().draft())));
            }
        }

        private final class TextControl extends FieldControl {
            private final EditBox input;
            private final KonfigRegistrySuggestionController suggestions;
            private boolean suppressResponder;

            private TextControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
                super(field);
                this.input = new EditBox(
                        KonfigFieldsetListScreen.this.font,
                        0,
                        0,
                        100,
                        CONTROL_HEIGHT,
                        field.label()
                );
                this.input.setMaxLength(512);
                this.input.setValue(this.textValue());
                moveCursorToStart(this.input);
                boolean editable = field.value().access().canEdit();
                this.input.setEditable(editable);
                this.input.active = editable;
                this.input.setResponder(this::changed);
                this.controls.add(this.input);

                if (field.field().registryKey().isPresent()) {
                    this.suggestions = new KonfigRegistrySuggestionController(new KonfigRegistrySuggestionController.Owner() {
                        @Override
                        public boolean hasRegistryBinding() {
                            return true;
                        }

                        @Override
                        public ResourceKey<? extends Registry<?>> registryKey() {
                            return TextControl.this.registryKey();
                        }

                        @Override
                        public List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
                            if (TextControl.this.input.getValue().startsWith("#")) {
                                return tagSuggestions(registryKey);
                            }
                            List<String> matches = KonfigFieldsetListScreen.this.registrySuggestions.find(
                                    registryKey,
                                    TextControl.this.input.getValue(),
                                    12
                            );
                            return matches == null ? List.of() : matches;
                        }

                        @Override
                        public String inputValue() {
                            return TextControl.this.input.getValue();
                        }

                        @Override
                        public void setInlineSuggestion(String suggestion) {
                            TextControl.this.input.setSuggestion(suggestion);
                        }

                        @Override
                        public boolean applySuggestion(String suggestion) {
                            TextControl.this.setInputValue(suggestion);
                            return TextControl.this.apply(TextControl.this.parse(suggestion));
                        }

                        @Override
                        public void focusInput() {
                            focus(TextControl.this.input, true);
                        }

                        @Override
                        public Font font() {
                            return KonfigFieldsetListScreen.this.font;
                        }

                        @Override
                        public int controlHeight() {
                            return CONTROL_HEIGHT;
                        }

                        @Override
                        public int suggestionRowHeight() {
                            return 18;
                        }

                        @Override
                        public int screenHeight() {
                            return KonfigFieldsetListScreen.this.height;
                        }

                        @Override
                        public int listTop() {
                            return LIST_TOP;
                        }
                    });
                } else {
                    this.suggestions = null;
                }
            }

            private void changed(String text) {
                if (this.suppressResponder) {
                    return;
                }
                try {
                    this.apply(this.parse(text));
                    this.refreshSuggestions();
                } catch (IllegalArgumentException exception) {
                    this.localError = exception.getMessage() == null ? "Invalid value" : exception.getMessage();
                    KonfigFieldsetListScreen.this.message = text(this.localError);
                    KonfigFieldsetListScreen.this.refreshControls();
                }
            }

            private Object parse(String text) {
                FieldsetFieldKind kind = this.field.field().kind();
                String normalized = text.trim();
                try {
                    if (kind == FieldsetFieldKind.INTEGER) {
                        return Integer.valueOf(normalized);
                    }
                    if (kind == FieldsetFieldKind.LONG) {
                        return Long.valueOf(normalized);
                    }
                    if (kind == FieldsetFieldKind.DOUBLE) {
                        double value = Double.parseDouble(normalized);
                        if (!Double.isFinite(value)) {
                            throw new NumberFormatException();
                        }
                        return Double.valueOf(value);
                    }
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Enter a valid " + kind.name().toLowerCase() + ".");
                }
                if (kind == FieldsetFieldKind.OPTIONAL_STRING) {
                    return normalized.isEmpty() ? Optional.empty() : Optional.of(text);
                }
                return text;
            }

            private boolean hasRegistryBinding() {
                return this.suggestions != null;
            }

            private ResourceKey<? extends Registry<?>> registryKey() {
                return this.field.field().registryKey().orElseThrow();
            }

            private boolean isFocused() {
                return this.input.isFocused();
            }

            private boolean isPointInsideInput(double mouseX, double mouseY) {
                return this.suggestions != null && this.suggestions.isPointInsideInput(mouseX, mouseY);
            }

            private boolean hasVisibleSuggestions() {
                return this.suggestions != null && this.suggestions.hasVisibleSuggestions();
            }

            private void refreshSuggestions() {
                if (this.suggestions != null) {
                    this.suggestions.refresh();
                }
            }

            private void activateSuggestions() {
                if (this.suggestions != null) {
                    this.suggestions.activate();
                }
            }

            private void closeSuggestions() {
                if (this.suggestions != null) {
                    this.suggestions.close();
                }
            }

            private void renderSuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
                if (this.suggestions != null) {
                    this.suggestions.render(context, mouseX, mouseY);
                }
            }

            private boolean handleSuggestionClick(double mouseX, double mouseY) {
                return this.suggestions != null && this.suggestions.handleClick(mouseX, mouseY);
            }

            private boolean handleSuggestionKey(int keyCode) {
                return this.suggestions != null && this.suggestions.handleKey(keyCode);
            }

            private String textValue() {
                Object value = this.field.value().draft();
                if (value instanceof Optional<?> optional) {
                    return optional.map(String::valueOf).orElse("");
                }
                return String.valueOf(value);
            }

            private void setInputValue(String value) {
                boolean previouslySuppressed = this.suppressResponder;
                this.suppressResponder = true;
                try {
                    this.input.setValue(value);
                } finally {
                    this.suppressResponder = previouslySuppressed;
                }
            }

            private void moveInputCursorToStart() {
                boolean previouslySuppressed = this.suppressResponder;
                this.suppressResponder = true;
                try {
                    moveCursorToStart(this.input);
                } finally {
                    this.suppressResponder = previouslySuppressed;
                }
            }

            @Override
            void layoutControls(int x, int y, int width) {
                place(this.input, x, y, width);
                if (this.suggestions != null) {
                    this.suggestions.updateInputBounds(x, y, width);
                }
            }

            @Override
            void sync() {
                String value = this.textValue();
                if (!this.input.getValue().equals(value)) {
                    this.setInputValue(value);
                }
                if (!this.input.isFocused()) {
                    this.moveInputCursorToStart();
                }
                this.refreshSuggestions();
            }

            @Override
            void tick() {
                if (this.suggestions != null && this.input.isFocused()) {
                    KonfigFieldsetListScreen.this.activeRegistryControl = this;
                    this.refreshSuggestions();
                }
            }

            @Override
            void renderDecoration(KonfigRenderContext context, int controlX, int y, int controlWidth) {
                if (!this.input.isFocused()) {
                    this.moveInputCursorToStart();
                }
                if (this.suggestions == null) {
                    return;
                }
                if (hasRegistryIcon(this.registryKey(), this.input.getValue())) {
                    context.renderRegistryIcon(this.registryKey(), this.input.getValue(), controlX - 22, y + 6);
                }
                if (this.input.isFocused()) {
                    KonfigFieldsetListScreen.this.activeRegistryControl = this;
                    this.refreshSuggestions();
                }
                EntryList list = KonfigFieldsetListScreen.this.list;
                if (KonfigFieldsetListScreen.this.activeRegistryControl == this
                        && this.hasVisibleSuggestions()
                        && this.suggestions.isInputWithin(list.listTop(), list.listBottom())) {
                    KonfigFieldsetListScreen.this.renderedRegistryControl = this;
                }
            }
        }
    }
}
