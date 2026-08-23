//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValidationIssue;
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
final class KonfigFieldsetListScreen extends Screen {
    private static final int LIST_TOP = 62;
    private static final int ROW_HEIGHT = 32;

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
    private Button edit;
    private Button save;
    private Component message = Component.empty();

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
        int contentWidth = Math.min(420, Math.max(240, this.width - 32));
        int contentX = (this.width - contentWidth) / 2;

        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                contentX,
                36,
                contentWidth,
                20,
                Component.literal("Search fieldset entries")
        ));
        this.search.setHint(Component.literal("Search"));
        this.search.setValue(this.state.query());
        this.search.setResponder(query -> {
            this.state.setQuery(query);
            if (this.list != null) {
                this.list.rebuild();
            }
        });

        int listBottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - 76);
        this.list = this.addRenderableWidget(new EntryList(contentWidth, listBottom - LIST_TOP, LIST_TOP));
        this.list.rebuild();

        int actionY = this.height - 52;
        int actionWidth = Math.max(44, (contentWidth - 20) / 6);
        int actionGap = 4;
        int actionsTotal = actionWidth * 6 + actionGap * 5;
        int actionX = this.width / 2 - actionsTotal / 2;
        this.add = this.addRenderableWidget(button(actionX, actionY, actionWidth, 20, Component.literal("Add"), ignored -> {
            this.apply(this.state.add());
        }));
        this.duplicate = this.addRenderableWidget(button(actionX + (actionWidth + actionGap), actionY, actionWidth, 20, Component.literal("Copy"), ignored -> {
            this.apply(this.state.duplicateSelected());
        }));
        this.delete = this.addRenderableWidget(button(actionX + (actionWidth + actionGap) * 2, actionY, actionWidth, 20, Component.literal("Delete"), ignored -> {
            this.apply(this.state.deleteSelected());
        }));
        this.moveUp = this.addRenderableWidget(button(actionX + (actionWidth + actionGap) * 3, actionY, actionWidth, 20, Component.literal("Up"), ignored -> {
            this.apply(this.state.moveSelected(-1));
        }));
        this.moveDown = this.addRenderableWidget(button(actionX + (actionWidth + actionGap) * 4, actionY, actionWidth, 20, Component.literal("Down"), ignored -> {
            this.apply(this.state.moveSelected(1));
        }));
        this.edit = this.addRenderableWidget(button(actionX + (actionWidth + actionGap) * 5, actionY, actionWidth, 20, Component.literal("Edit"), ignored -> this.openSelected()));

        int footerY = this.height - 26;
        this.addRenderableWidget(button(this.width / 2 - 102, footerY, 100, 20, Component.literal("Cancel"), ignored -> this.closeToParent()));
        this.save = this.addRenderableWidget(button(this.width / 2 + 2, footerY, 100, 20, Component.literal("Save"), ignored -> this.save()));
        this.refreshControls();
    }

    @Override
    public void onClose() {
        this.closeToParent();
    }

    void returnedFromEntryEditor() {
        this.state.refresh();
        this.rebuildWidgets();
        this.setScreen(this);
    }

    private void openSelected() {
        this.state.selectedEntry().ifPresent(entry -> this.setScreen(new KonfigFieldsetEntryScreen(
                this,
                this.title,
                this.context,
                entry.identity(),
                this.adapter,
                this.registrySuggestions
        )));
    }

    private void save() {
        List<FieldsetValidationIssue> issues = this.session.draft().validate().issues();
        if (!issues.isEmpty()) {
            FieldsetValidationIssue issue = issues.get(0);
            this.state.setQuery("");
            this.search.setValue("");
            this.state.select(issue.entryIdentity());
            this.message = Component.literal(issue.message());
            this.refreshAfterSelection();
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

    private void apply(KonfigFieldsetEditResult result) {
        this.message = result.accepted() ? Component.empty() : result.message();
        this.state.refresh();
        this.list.rebuild();
        this.refreshControls();
    }

    private void refreshAfterSelection() {
        this.list.refreshSelection();
        this.refreshControls();
    }

    private void refreshControls() {
        if (this.add == null) {
            return;
        }
        this.add.active = this.state.canAdd();
        this.duplicate.active = this.state.canDuplicateSelected();
        this.delete.active = this.state.canDeleteSelected();
        this.moveUp.active = this.state.canMoveSelectedUp();
        this.moveDown.active = this.state.canMoveSelectedDown();
        this.edit.active = this.state.selectedEntry().isPresent();
        this.save.active = this.adapter.fieldsetAccess().canEdit();
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
        context.drawCenteredText(this.font, this.context, this.width / 2, 22, 0xFFA0A0A0);
        if (!this.message.getString().isBlank()) {
            context.drawCenteredText(this.font, this.message, this.width / 2, this.height - 70, 0xFFFF7070);
        } else {
            Component validation = this.adapter.validation().summary();
            if (!validation.getString().isBlank()) {
                context.drawCenteredText(this.font, validation, this.width / 2, this.height - 70, 0xFFFF7070);
            }
        }
    }

    private final class EntryList extends ContainerObjectSelectionList<EntryRow> {
        private final int rowWidth;

        private EntryList(int width, int height, int y) {
            super(KonfigFieldsetListScreen.this.minecraft, width, height, y, ROW_HEIGHT);
            this.rowWidth = width - 20;
        }

        private void rebuild() {
            this.clearEntries();
            for (KonfigFieldsetListEditorState.VisibleEntry<FieldsetEntry> visible : KonfigFieldsetListScreen.this.state.visibleEntries()) {
                EntryRow row = new EntryRow(visible);
//? if >=26.1 {
                this.addEntry(row, ROW_HEIGHT);
//?} else {
                this.addEntry(row);
//?}
            }
        }

        private void refreshSelection() {
            for (EntryRow row : this.children()) {
                row.refresh();
            }
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }
    }

    private final class EntryRow extends ContainerObjectSelectionList.Entry<EntryRow> {
        private final FieldsetEntry entry;
        private final Button select;

        private EntryRow(KonfigFieldsetListEditorState.VisibleEntry<FieldsetEntry> visible) {
            this.entry = visible.entry();
            this.select = button(0, 0, 100, 24, this.label(), ignored -> {
                KonfigFieldsetListScreen.this.state.select(this.entry.identity());
                KonfigFieldsetListScreen.this.message = Component.empty();
                KonfigFieldsetListScreen.this.refreshAfterSelection();
            });
        }

        private void refresh() {
            this.select.setMessage(this.label());
        }

        private Component label() {
            boolean selected = this.entry.identity().equals(KonfigFieldsetListScreen.this.state.selectedEntryId());
            Component label = KonfigFieldsetListScreen.this.adapter.entryLabel(this.entry);
            Component summary = KonfigFieldsetListScreen.this.adapter.entrySummary(this.entry);
            StringBuilder text = new StringBuilder(selected ? "> " : "  ").append(label.getString());
            if (!summary.getString().isBlank()) {
                text.append("  ").append(summary.getString());
            }
            KonfigFieldsetValidation validation = KonfigFieldsetListScreen.this.adapter.validation().forEntry(this.entry.identity());
            if (!validation.isValid()) {
                text.append("  [").append(validation.errorCount()).append(" invalid]");
            }
            if (!this.entry.editable()) {
                text.append("  [built in]");
            }
            return Component.literal(text.toString());
        }

        private void renderRow(KonfigRenderContext context, int x, int y, int width, int mouseX, int mouseY, float partialTick) {
            this.select.setX(x);
            this.select.setY(y + 2);
            this.select.setWidth(width);
            this.select.active = true;
            context.renderWidget(this.select, mouseX, mouseY, partialTick);
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), mouseX, mouseY, partialTick);
        }
//?} else {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), mouseX, mouseY, partialTick);
        }
//?}

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.select);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return Collections.singletonList(this.select);
        }
    }
}
//?}
