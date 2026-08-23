//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.supportsRegistryIcon;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetFieldKind;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValidationIssue;
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
final class KonfigFieldsetListScreen extends Screen {
    private static final int LIST_TOP = 64;
    private static final int COLLAPSED_HEIGHT = 44;
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

    private EditBox search;
    private EntryList list;
    private Button add;
    private Button duplicate;
    private Button delete;
    private Button moveUp;
    private Button moveDown;
    private Button save;
    private Component message = Component.empty();
    private boolean suppressSearchResponder;
    private boolean rebuildPending;
    private boolean revealPending;

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
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int contentWidth = Math.min(500, Math.max(260, this.width - 28));
        int contentX = (this.width - contentWidth) / 2;

        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                contentX,
                38,
                contentWidth,
                20,
                Component.literal("Search fieldset entries")
        ));
        this.search.setHint(Component.literal("Search"));
        this.search.setValue(this.state.query());
        this.search.setResponder(this::searchChanged);

        int listBottom = Math.max(LIST_TOP + COLLAPSED_HEIGHT, this.height - 60);
        this.list = this.addRenderableWidget(new EntryList(contentWidth, listBottom - LIST_TOP, LIST_TOP));
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

        this.add = this.addRenderableWidget(button(actionX, actionY, addWidth, 20, Component.literal("Add"), ignored -> {
            this.apply(this.state.add(), true);
        }));
        int selectedX = actionX + addWidth + spacer;
        this.duplicate = this.addRenderableWidget(button(selectedX, actionY, copyWidth, 20, Component.literal("Copy"), ignored -> {
            this.apply(this.state.duplicateSelected(), true);
        }));
        this.delete = this.addRenderableWidget(button(selectedX + copyWidth + actionGap, actionY, deleteWidth, 20, Component.literal("Delete"), ignored -> {
            this.apply(this.state.deleteSelected(), true);
        }));
        this.moveUp = this.addRenderableWidget(button(
                selectedX + copyWidth + deleteWidth + actionGap * 2,
                actionY,
                moveWidth,
                20,
                Component.literal("Up"),
                ignored -> this.apply(this.state.moveSelected(-1), true)
        ));
        this.moveDown = this.addRenderableWidget(button(
                selectedX + copyWidth + deleteWidth + moveWidth + actionGap * 3,
                actionY,
                moveWidth,
                20,
                Component.literal("Down"),
                ignored -> this.apply(this.state.moveSelected(1), true)
        ));

        int footerY = this.height - 26;
        this.addRenderableWidget(button(this.width / 2 - 102, footerY, 100, 20, Component.literal("Cancel"), ignored -> this.closeToParent()));
        this.save = this.addRenderableWidget(button(this.width / 2 + 2, footerY, 100, 20, Component.literal("Save"), ignored -> this.save()));
        this.refreshControls();
    }

    @Override
    public void onClose() {
        this.closeToParent();
    }

    @Override
    public void tick() {
        super.tick();
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
        if (this.list != null && this.list.hasLocalErrors()) {
            this.message = Component.literal("Fix the invalid field before changing the filter.");
            this.setSearchValue(this.state.query());
            this.refreshControls();
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
        if (this.list.hasLocalErrors()) {
            this.message = Component.literal("Fix the invalid field before closing this entry.");
            this.refreshControls();
            return;
        }
        if (this.state.toggleExpanded(entryId)) {
            this.message = Component.empty();
            this.requestRebuild(true);
            this.refreshControls();
        }
    }

    private void save() {
        if (this.list.hasLocalErrors()) {
            this.message = Component.literal("Fix the invalid field before saving.");
            this.refreshControls();
            return;
        }
        List<FieldsetValidationIssue> issues = this.session.draft().validate().issues();
        if (!issues.isEmpty()) {
            FieldsetValidationIssue issue = issues.get(0);
            this.state.setQuery("");
            this.setSearchValue("");
            this.state.select(issue.entryIdentity());
            if (!this.state.isExpanded(issue.entryIdentity())) {
                this.state.toggleExpanded(issue.entryIdentity());
            }
            this.message = Component.literal(issue.message());
            this.requestRebuild(true);
            this.refreshControls();
            return;
        }
        if (!this.session.dirty()) {
            this.closeToParent();
            return;
        }
        KonfigFieldsetEditResult result;
        try {
            result = Objects.requireNonNull(
                    this.persistAction.persist(this.session.original(), this.session.draft()),
                    "persistAction result"
            );
        } catch (RuntimeException exception) {
            String detail = exception.getMessage();
            this.message = Component.literal(detail == null || detail.isBlank()
                    ? "The fieldset could not be saved."
                    : detail);
            return;
        }
        if (result.accepted()) {
            this.closeToParent();
            return;
        }
        this.message = result.message();
        this.refreshControls();
    }

    private void apply(KonfigFieldsetEditResult result, boolean revealSelection) {
        this.message = result.accepted() ? Component.empty() : result.message();
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
        boolean hasLocalErrors = this.list != null && this.list.hasLocalErrors();
        this.add.active = !hasLocalErrors && this.state.canAdd();
        this.duplicate.active = !hasLocalErrors && this.state.canDuplicateSelected();
        this.delete.active = !hasLocalErrors && this.state.canDeleteSelected();
        this.moveUp.active = !hasLocalErrors && this.state.canMoveSelectedUp();
        this.moveDown.active = !hasLocalErrors && this.state.canMoveSelectedDown();
        this.save.active = this.adapter.fieldsetAccess().canEdit()
                && !hasLocalErrors
                && this.adapter.validation().isValid();
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

//? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
    }
//?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
    }
//?}

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
    }

    private Component fit(Component value, int width) {
        String text = value.getString();
        if (this.font.width(text) <= width) {
            return value;
        }
        String suffix = "...";
        return Component.literal(this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width(suffix))) + suffix);
    }

    private final class EntryList extends ContainerObjectSelectionList<EntryRow> {
        private final int rowWidth;

        private EntryList(int width, int height, int y) {
            super(KonfigFieldsetListScreen.this.minecraft, width, height, y, COLLAPSED_HEIGHT);
            this.rowWidth = width - 18;
        }

        private void rebuild(boolean revealSelection) {
            double previousScroll = this.scrollAmount();
            this.setFocused(null);
            this.clearEntries();
            for (KonfigFieldsetListEditorState.VisibleEntry<FieldsetEntry> visible : KonfigFieldsetListScreen.this.state.visibleEntries()) {
                EntryRow row = new EntryRow(visible);
                this.addEntry(row, row.rowHeight());
            }
            this.setScrollAmount(previousScroll);
            if (revealSelection) {
                this.reveal(KonfigFieldsetListScreen.this.state.selectedEntryId());
            }
        }

        private void reveal(String entryId) {
            for (EntryRow row : this.children()) {
                if (row.entryId.equals(entryId)) {
                    if (row.getHeight() > this.getHeight() - 4) {
                        int topOffset = row.getY() - this.getY() - 2;
                        this.setScrollAmount(this.scrollAmount() + topOffset);
                    } else {
                        this.scrollToEntry(row);
                    }
                    return;
                }
            }
        }

        private boolean hasLocalErrors() {
            for (EntryRow row : this.children()) {
                if (row.hasLocalErrors()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }
    }

    private final class EntryRow extends ContainerObjectSelectionList.Entry<EntryRow> {
        private final String entryId;
        private final FieldsetEntry snapshot;
        private final boolean selected;
        private final boolean expanded;
        private final Button header;
        private final List<FieldControl> fields = new ArrayList<>();
        private final List<AbstractWidget> controls = new ArrayList<>();

        private EntryRow(KonfigFieldsetListEditorState.VisibleEntry<FieldsetEntry> visible) {
            this.entryId = visible.entry().identity();
            this.snapshot = visible.entry();
            this.selected = visible.selected();
            this.expanded = KonfigFieldsetListScreen.this.state.isExpanded(this.entryId);
            this.header = button(0, 0, 100, COLLAPSED_HEIGHT - CARD_GAP, Component.empty(), ignored -> {
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
            this.header.setMessage(Component.literal(action + KonfigFieldsetListScreen.this.adapter.entryLabel(this.currentEntry()).getString()));
        }

        private boolean hasLocalErrors() {
            for (FieldControl field : this.fields) {
                if (!field.localError.isBlank()) {
                    return true;
                }
            }
            return false;
        }

        private void renderRow(
                KonfigRenderContext context,
                int x,
                int y,
                int width,
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
            this.header.setX(x + 1);
            this.header.setY(y + 1);
            this.header.setWidth(width - 2);
            this.header.setHeight(COLLAPSED_HEIGHT - CARD_GAP - 2);
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
                    : access.isReadOnly() ? "Built in" : "";
            int statusWidth = status.isBlank() ? 0 : KonfigFieldsetListScreen.this.font.width(status) + 10;

            context.drawText(
                    KonfigFieldsetListScreen.this.font,
                    Component.literal(this.expanded ? "v" : ">"),
                    x + 9,
                    y + 9,
                    0xFFD0D0D0
            );

            int titleX = x + 22;
            Optional<KonfigFieldsetDraftAdapter.EntryIcon> icon = KonfigFieldsetListScreen.this.adapter.entryIcon(entry);
            if (icon.isPresent() && supportsRegistryIcon(icon.get().registryKey())) {
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
                        Component.literal(status),
                        x + width - statusWidth,
                        y + 14,
                        color
                );
            }
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(
                    KonfigRenderContext.of(graphics),
                    this.getContentX(),
                    this.getContentY(),
                    this.getContentWidth(),
                    mouseX,
                    mouseY,
                    hovered,
                    partialTick
            );
        }
//?} else {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(
                    KonfigRenderContext.of(graphics),
                    this.getContentX(),
                    this.getContentY(),
                    this.getContentWidth(),
                    mouseX,
                    mouseY,
                    hovered,
                    partialTick
            );
        }
//?}

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

            final void apply(Object value) {
                KonfigFieldsetEditResult result = this.field.value().setDraft(value);
                this.localError = result.accepted() ? "" : result.message().getString();
                KonfigFieldsetListScreen.this.message = result.accepted() ? Component.empty() : result.message();
                this.sync();
                EntryRow.this.refreshHeaderNarration();
                KonfigFieldsetListScreen.this.refreshControls();
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
                for (AbstractWidget control : this.controls) {
                    context.renderWidget(control, mouseX, mouseY, partialTick);
                }
                String validation = this.validationMessage();
                if (!validation.isBlank()) {
                    context.drawText(
                            KonfigFieldsetListScreen.this.font,
                            KonfigFieldsetListScreen.this.fit(Component.literal(validation), controlWidth),
                            controlX,
                            y + 27,
                            0xFFFF7070
                    );
                }
            }

            abstract void layoutControls(int x, int y, int width);

            abstract void sync();
        }

        private final class BooleanControl extends FieldControl {
            private final Button toggle;

            private BooleanControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
                super(field);
                this.toggle = button(0, 0, 100, CONTROL_HEIGHT, Component.empty(), ignored -> {
                    Object current = this.field.value().draft();
                    this.apply(Boolean.valueOf(!(current instanceof Boolean) || !((Boolean) current).booleanValue()));
                });
                this.toggle.active = field.value().access().canEdit();
                this.controls.add(this.toggle);
                this.sync();
            }

            @Override
            void layoutControls(int x, int y, int width) {
                this.toggle.setX(x);
                this.toggle.setY(y);
                this.toggle.setWidth(width);
            }

            @Override
            void sync() {
                this.toggle.setMessage(Component.literal(Boolean.TRUE.equals(this.field.value().draft()) ? "On" : "Off"));
            }
        }

        private final class DropdownControl extends FieldControl {
            private final Button dropdown;

            private DropdownControl(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
                super(field);
                this.dropdown = button(0, 0, 100, CONTROL_HEIGHT, Component.empty(), ignored -> this.next());
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
                this.dropdown.setX(x);
                this.dropdown.setY(y);
                this.dropdown.setWidth(width);
            }

            @Override
            void sync() {
                this.dropdown.setMessage(Component.literal(String.valueOf(this.field.value().draft())));
            }
        }

        private final class TextControl extends FieldControl {
            private final EditBox input;
            private final Button suggest;
            private int suggestionIndex;

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
                this.input.setEditable(field.value().access().canEdit());
                this.input.setResponder(this::changed);
                this.controls.add(this.input);

                if (field.field().kind() == FieldsetFieldKind.REGISTRY_STRING && field.field().registryKey().isPresent()) {
                    this.suggest = button(0, 0, 58, CONTROL_HEIGHT, Component.literal("Suggest"), ignored -> this.suggest());
                    this.suggest.active = field.value().access().canEdit();
                    this.controls.add(this.suggest);
                } else {
                    this.suggest = null;
                }
            }

            private void changed(String text) {
                try {
                    this.apply(this.parse(text));
                } catch (IllegalArgumentException exception) {
                    this.localError = exception.getMessage() == null ? "Invalid value" : exception.getMessage();
                    KonfigFieldsetListScreen.this.message = Component.literal(this.localError);
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

            private void suggest() {
                this.field.field().registryKey().ifPresent(registryKey -> {
                    List<String> suggestions = KonfigFieldsetListScreen.this.registrySuggestions.find(
                            registryKey,
                            this.input.getValue(),
                            12
                    );
                    if (suggestions == null || suggestions.isEmpty()) {
                        this.localError = "No matching registry values.";
                        KonfigFieldsetListScreen.this.message = Component.literal(this.localError);
                        KonfigFieldsetListScreen.this.refreshControls();
                        return;
                    }
                    this.suggestionIndex %= suggestions.size();
                    this.input.setValue(suggestions.get(this.suggestionIndex++));
                });
            }

            private String textValue() {
                Object value = this.field.value().draft();
                if (value instanceof Optional<?> optional) {
                    return optional.map(String::valueOf).orElse("");
                }
                return String.valueOf(value);
            }

            @Override
            void layoutControls(int x, int y, int width) {
                int suggestWidth = this.suggest == null ? 0 : 62;
                this.input.setX(x);
                this.input.setY(y);
                this.input.setWidth(width - suggestWidth);
                if (this.suggest != null) {
                    this.suggest.setX(x + width - 58);
                    this.suggest.setY(y);
                    this.suggest.setWidth(58);
                }
            }

            @Override
            void sync() {
            }
        }
    }
}
//?}
