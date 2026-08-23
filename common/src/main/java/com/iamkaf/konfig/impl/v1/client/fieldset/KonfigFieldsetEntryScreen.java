//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetFieldKind;
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
final class KonfigFieldsetEntryScreen extends Screen {
    private static final int LIST_TOP = 44;
    private static final int ROW_HEIGHT = 44;
    private static final int CONTROL_HEIGHT = 20;

    private final KonfigFieldsetListScreen parent;
    private final Component context;
    private final String entryId;
    private final KonfigFieldsetDraftAdapter adapter;
    private final KonfigFieldsetEntryEditorState<FieldsetEntry, FieldsetField<?>> state;
    private final KonfigFieldsetScreens.RegistrySuggestions registrySuggestions;

    private FieldList list;
    private Component message = Component.empty();

    KonfigFieldsetEntryScreen(
            KonfigFieldsetListScreen parent,
            Component title,
            Component context,
            String entryId,
            KonfigFieldsetDraftAdapter adapter,
            KonfigFieldsetScreens.RegistrySuggestions registrySuggestions
    ) {
        super(Objects.requireNonNull(title, "title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.context = Objects.requireNonNull(context, "context");
        this.entryId = Objects.requireNonNull(entryId, "entryId");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.state = new KonfigFieldsetEntryEditorState<>(adapter, entryId);
        this.registrySuggestions = Objects.requireNonNull(registrySuggestions, "registrySuggestions");
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int contentWidth = Math.min(460, Math.max(260, this.width - 32));
        int listBottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - 32);
        this.list = this.addRenderableWidget(new FieldList(contentWidth, listBottom - LIST_TOP, LIST_TOP));
        this.list.rebuild();

        int footerY = this.height - 26;
        Button reset = this.addRenderableWidget(button(this.width / 2 - 102, footerY, 100, 20, Component.literal("Reset entry"), ignored -> this.resetEntry()));
        reset.active = this.state.access().canEdit();
        this.addRenderableWidget(button(this.width / 2 + 2, footerY, 100, 20, Component.literal("Done"), ignored -> this.completeEntry()));
    }

    @Override
    public void onClose() {
        this.returnToList();
    }

    private void completeEntry() {
        if (this.state.access().canEdit() && this.list.hasLocalErrors()) {
            this.message = Component.literal("Fix the invalid fields before leaving this entry.");
            return;
        }
        if (this.state.access().canEdit() && !this.state.validation().isValid()) {
            this.message = this.state.validation().issues().get(0).message();
            return;
        }
        this.returnToList();
    }

    private void resetEntry() {
        for (KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field : this.state.fields()) {
            field.value().reset();
        }
        this.message = Component.empty();
        this.rebuildWidgets();
    }

    private void returnToList() {
        this.parent.returnedFromEntryEditor();
    }

    private FieldRow createFieldRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
        FieldsetFieldKind kind = field.field().kind();
        if (kind == FieldsetFieldKind.BOOLEAN) {
            return new BooleanRow(field);
        }
        if (kind == FieldsetFieldKind.DROPDOWN) {
            return new DropdownRow(field);
        }
        return new TextRow(field);
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
        context.drawCenteredText(this.font, this.state.label(), this.width / 2, 8, 0xFFFFFFFF);
        context.drawCenteredText(this.font, this.context, this.width / 2, 22, 0xFFA0A0A0);
        if (!this.message.getString().isBlank()) {
            context.drawCenteredText(this.font, this.message, this.width / 2, 34, 0xFFFF7070);
        } else if (this.state.access().isReadOnly()) {
            context.drawCenteredText(this.font, this.state.access().reason(), this.width / 2, 34, 0xFFFFC060);
        }
    }

    private final class FieldList extends ContainerObjectSelectionList<FieldRow> {
        private final int rowWidth;

        private FieldList(int width, int height, int y) {
            super(KonfigFieldsetEntryScreen.this.minecraft, width, height, y, ROW_HEIGHT);
            this.rowWidth = width - 20;
        }

        private void rebuild() {
            this.clearEntries();
            for (KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field : KonfigFieldsetEntryScreen.this.state.fields()) {
                FieldRow row = KonfigFieldsetEntryScreen.this.createFieldRow(field);
//? if >=26.1 {
                this.addEntry(row, ROW_HEIGHT);
//?} else {
                this.addEntry(row);
//?}
            }
        }

        private boolean hasLocalErrors() {
            for (FieldRow row : this.children()) {
                if (!row.localError.isBlank()) {
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

    private abstract class FieldRow extends ContainerObjectSelectionList.Entry<FieldRow> {
        final KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field;
        final List<AbstractWidget> controls = new ArrayList<>();
        String localError = "";

        FieldRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
            this.field = field;
        }

        final void apply(Object value) {
            KonfigFieldsetEditResult result = this.field.value().setDraft(value);
            this.localError = result.accepted() ? "" : result.message().getString();
            KonfigFieldsetEntryScreen.this.message = result.accepted() ? Component.empty() : result.message();
            this.sync();
        }

        final String validationMessage() {
            if (!this.localError.isBlank()) {
                return this.localError;
            }
            List<KonfigFieldsetValidation.Issue> issues = this.field.value().validation().issues();
            return issues.isEmpty() ? "" : issues.get(0).message().getString();
        }

        abstract void layoutControls(int x, int y, int width);

        abstract void sync();

        final void renderRow(KonfigRenderContext context, int x, int y, int width, int mouseX, int mouseY, float partialTick) {
            context.drawText(KonfigFieldsetEntryScreen.this.font, this.field.label(), x + 4, y + 8, 0xFFFFFFFF);
            this.layoutControls(x + width / 2, y + 4, width / 2 - 4);
            for (AbstractWidget control : this.controls) {
                context.renderWidget(control, mouseX, mouseY, partialTick);
            }
            String validation = this.validationMessage();
            if (!validation.isBlank()) {
                context.drawText(KonfigFieldsetEntryScreen.this.font, Component.literal(validation), x + width / 2, y + 27, 0xFFFF7070);
            }
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
            return this.controls;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.controls;
        }
    }

    private final class BooleanRow extends FieldRow {
        private final Button toggle;

        private BooleanRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
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
            boolean value = Boolean.TRUE.equals(this.field.value().draft());
            this.toggle.setMessage(Component.literal(value ? "On" : "Off"));
        }
    }

    private final class DropdownRow extends FieldRow {
        private final Button dropdown;

        private DropdownRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
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

    private final class TextRow extends FieldRow {
        private final EditBox input;
        private final Button suggest;
        private int suggestionIndex;

        private TextRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
            super(field);
            this.input = new EditBox(
                    KonfigFieldsetEntryScreen.this.font,
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
                this.suggest = button(0, 0, 64, CONTROL_HEIGHT, Component.literal("Suggest"), ignored -> this.suggest());
                this.suggest.active = field.value().access().canEdit();
                this.controls.add(this.suggest);
            } else {
                this.suggest = null;
            }
        }

        private void changed(String text) {
            try {
                Object value = this.parse(text);
                this.apply(value);
            } catch (IllegalArgumentException exception) {
                this.localError = exception.getMessage() == null ? "Invalid value" : exception.getMessage();
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
                throw new IllegalArgumentException("Enter a valid " + this.field.field().kind().name().toLowerCase() + ".");
            }
            if (kind == FieldsetFieldKind.OPTIONAL_STRING) {
                return normalized.isEmpty() ? Optional.empty() : Optional.of(text);
            }
            return text;
        }

        private void suggest() {
            this.field.field().registryKey().ifPresent(registryKey -> {
                List<String> suggestions = KonfigFieldsetEntryScreen.this.registrySuggestions.find(
                        registryKey,
                        this.input.getValue(),
                        12
                );
                if (suggestions == null || suggestions.isEmpty()) {
                    this.localError = "No matching registry values.";
                    return;
                }
                this.suggestionIndex = this.suggestionIndex % suggestions.size();
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
            int suggestWidth = this.suggest == null ? 0 : 68;
            this.input.setX(x);
            this.input.setY(y);
            this.input.setWidth(width - suggestWidth);
            if (this.suggest != null) {
                this.suggest.setX(x + width - 64);
                this.suggest.setY(y);
                this.suggest.setWidth(64);
            }
        }

        @Override
        void sync() {
        }
    }
}
//?}
