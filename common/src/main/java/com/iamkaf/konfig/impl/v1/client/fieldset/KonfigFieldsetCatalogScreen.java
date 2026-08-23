//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import static com.iamkaf.konfig.impl.v1.client.render.KonfigRegistryAdapter.hasRegistryIcon;
import static com.iamkaf.konfig.impl.v1.client.render.KonfigUiAdapter.button;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetCatalog;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetEntry;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetField;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetFieldKind;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValidationIssue;
import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import com.iamkaf.konfig.impl.v1.client.control.KonfigRegistrySuggestionController;
import com.iamkaf.konfig.impl.v1.client.render.KonfigRenderContext;
import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@ApiStatus.Internal
final class KonfigFieldsetCatalogScreen extends Screen {
    private static final int HEADER_BOTTOM = 56;
    private static final int FOOTER_HEIGHT = 30;
    private static final int PROFILE_ROW_HEIGHT = 36;
    private static final int RULE_ROW_HEIGHT = 30;
    private static final int DETAIL_FIELD_HEIGHT = 38;
    private static final int DETAIL_SECTION_HEIGHT = 24;
    private static final int CONTROL_HEIGHT = 20;
    private static final int ICON_SIZE = 20;
    private static final int WIDE_MINIMUM = 760;

    private final Screen parent;
    private final Component context;
    private final KonfigFieldsetDraftSession session;
    private final KonfigFieldsetDraftAdapter adapter;
    private final KonfigFieldsetListEditorState<FieldsetEntry, FieldsetField<?>> state;
    private final KonfigFieldsetScreens.RegistrySuggestions registrySuggestions;
    private final KonfigFieldsetScreens.PersistAction persistAction;
    private final FieldsetCatalog catalog;
    private final Deque<FieldsetValue> undoHistory = new ArrayDeque<FieldsetValue>();
    private final KonfigFieldsetScreens.Subscription persistenceSubscription;

    private String selectedProfile = "";
    private String selectedEntryId = "";
    private String query = "";
    private String filterValue = "";
    private View view = View.OVERVIEW;
    private Component message = Component.empty();

    private boolean wide;
    private boolean rebuildPending;
    private EditBox search;
    private Button filter;
    private Button revert;
    private ProfileList profileList;
    private RuleList ruleList;
    private DetailList detailList;
    private TextFieldRow activeTextField;
    private TextFieldRow renderedRegistryField;
    private PendingSave pendingSave;
    private boolean pendingUndo;
    private KonfigFieldsetValidation validation = KonfigFieldsetValidation.valid();
    private double profileScroll;
    private double ruleScroll;
    private double detailScroll;

    KonfigFieldsetCatalogScreen(
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
        this.state = new KonfigFieldsetListEditorState<FieldsetEntry, FieldsetField<?>>(adapter);
        this.registrySuggestions = Objects.requireNonNull(registrySuggestions, "registrySuggestions");
        this.persistAction = Objects.requireNonNull(persistAction, "persistAction");
        this.catalog = session.draft().schema().catalog().orElseThrow();
        this.persistenceSubscription = this.persistAction.observe(this::completePersist);
    }

    @Override
    protected void init() {
        if (this.profileList != null) {
            this.profileScroll = this.profileList.scrollAmount();
        }
        if (this.ruleList != null) {
            this.ruleScroll = this.ruleList.scrollAmount();
        }
        if (this.detailList != null) {
            this.detailScroll = this.detailList.scrollAmount();
        }
        this.clearWidgets();
        this.activeTextField = null;
        this.renderedRegistryField = null;
        this.wide = this.width >= WIDE_MINIMUM;
        this.validation = this.adapter.validation();
        this.reconcileSelection();

        int contentWidth = Math.min(920, Math.max(280, this.width - 24));
        int contentX = (this.width - contentWidth) / 2;
        int bodyHeight = Math.max(80, this.height - HEADER_BOTTOM - FOOTER_HEIGHT);

        if (this.selectedProfile.isEmpty()) {
            this.profileList = this.addRenderableWidget(new ProfileList(contentWidth, bodyHeight, HEADER_BOTTOM, true));
            this.profileList.setX(contentX);
            this.profileList.setScrollAmount(this.profileScroll);
        } else if (this.wide) {
            int gap = 6;
            int profileWidth = Math.min(156, Math.max(126, contentWidth / 5));
            int remaining = contentWidth - profileWidth - gap * 2;
            int ruleWidth = Math.min(300, Math.max(230, remaining * 43 / 100));
            int detailWidth = remaining - ruleWidth;

            this.profileList = this.addRenderableWidget(new ProfileList(profileWidth, bodyHeight, HEADER_BOTTOM, false));
            this.profileList.setX(contentX);
            this.profileList.setScrollAmount(this.profileScroll);
            this.addRuleSearch(contentX + profileWidth + gap, ruleWidth);
            this.ruleList = this.addRenderableWidget(new RuleList(ruleWidth, bodyHeight - 24, HEADER_BOTTOM + 24));
            this.ruleList.setX(contentX + profileWidth + gap);
            this.ruleList.setScrollAmount(this.ruleScroll);
            this.detailList = this.addRenderableWidget(new DetailList(detailWidth, bodyHeight, HEADER_BOTTOM));
            this.detailList.setX(contentX + profileWidth + gap + ruleWidth + gap);
            this.detailList.setScrollAmount(this.detailScroll);
        } else if (this.view == View.DETAIL && this.selectedEntry().isPresent()) {
            this.detailList = this.addRenderableWidget(new DetailList(contentWidth, bodyHeight, HEADER_BOTTOM));
            this.detailList.setX(contentX);
            this.detailList.setScrollAmount(this.detailScroll);
        } else {
            this.view = View.RULES;
            this.addRuleSearch(contentX, contentWidth);
            this.ruleList = this.addRenderableWidget(new RuleList(contentWidth, bodyHeight - 24, HEADER_BOTTOM + 24));
            this.ruleList.setX(contentX);
            this.ruleList.setScrollAmount(this.ruleScroll);
        }

        this.addFooter(contentX, contentWidth);
    }

    private void addRuleSearch(int x, int width) {
        int filterWidth = this.catalog.filterField().isPresent() ? Math.min(126, width / 3) : 0;
        int gap = filterWidth == 0 ? 0 : 4;
        this.search = this.addRenderableWidget(new EditBox(
                this.font,
                x,
                32,
                width - filterWidth - gap,
                20,
                Component.literal("Search catalog entries")
        ));
        this.search.setHint(Component.literal("Search"));
        this.search.setValue(this.query);
        this.search.setResponder(value -> {
            this.query = value == null ? "" : value;
            this.selectedEntryId = "";
            if (this.ruleList != null) {
                this.ruleList.rebuild();
            }
            if (this.detailList != null) {
                this.detailList.rebuild();
            }
        });
        if (filterWidth > 0) {
            this.filter = this.addRenderableWidget(button(
                    x + width - filterWidth,
                    32,
                    filterWidth,
                    20,
                    this.filterLabel(),
                    ignored -> this.nextFilter()
            ));
        }
    }

    private void addFooter(int contentX, int contentWidth) {
        int y = this.height - 26;
        int left = contentX;
        if (!this.selectedProfile.isEmpty()) {
            this.addRenderableWidget(button(left, y, 72, 20, Component.literal("Back"), ignored -> this.back()));
            left += 76;
        }

        if (this.adapter.fieldsetAccess().canAdd() && (this.view != View.DETAIL || this.wide)) {
            this.addRenderableWidget(button(
                    left,
                    y,
                    112,
                    20,
                    Component.literal(this.catalog.newEntryLabel()),
                    ignored -> this.addEntry()
            ));
            left += 116;
        }

        Optional<FieldsetEntry> selected = this.selectedEntry();
        if (selected.isPresent() && (this.view == View.DETAIL || this.wide)) {
            FieldsetEntry entry = selected.get();
            if (entry.editable()) {
                if (this.session.draft().schema().keyField().isEmpty()) {
                    this.addRenderableWidget(button(
                            left,
                            y,
                            88,
                            20,
                            Component.literal(this.catalog.duplicateLabel()),
                            ignored -> this.duplicateSelected()
                    ));
                    left += 92;
                }
                this.addRenderableWidget(button(
                        left,
                        y,
                        72,
                        20,
                        Component.literal(this.catalog.deleteLabel()),
                        ignored -> this.deleteSelected()
                ));
                left += 76;
            } else if (this.adapter.fieldsetAccess().canAdd()) {
                this.addRenderableWidget(button(
                        left,
                        y,
                        92,
                        20,
                        Component.literal(this.catalog.overrideLabel()),
                        ignored -> this.overrideSelected()
                ));
                left += 96;
            }
        }

        if (!this.undoHistory.isEmpty()) {
            this.addRenderableWidget(button(left, y, 64, 20, Component.literal("Undo"), ignored -> this.undo()));
            left += 68;
        }

        this.revert = this.addRenderableWidget(button(
                left,
                y,
                72,
                20,
                Component.literal("Revert"),
                ignored -> this.revertActiveDraft()
        ));
        this.revert.visible = false;

        this.addRenderableWidget(button(
                contentX + contentWidth - 80,
                y,
                80,
                20,
                Component.literal("Done"),
                ignored -> this.closeToParent()
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.detailList != null) {
            this.detailList.tickRows();
        }
        if (this.rebuildPending) {
            this.rebuildPending = false;
            this.init();
        }
    }

    private void requestRebuild() {
        this.rebuildPending = true;
    }

    private KonfigFieldsetCatalogModel model() {
        return new KonfigFieldsetCatalogModel(this.session.draft());
    }

    private List<KonfigFieldsetCatalogModel.Profile> profiles() {
        return this.model().profiles();
    }

    private List<FieldsetEntry> visibleRules() {
        if (this.selectedProfile.isEmpty()) {
            return List.of();
        }
        return this.model().entries(this.selectedProfile, this.query, this.filterValue);
    }

    private Optional<FieldsetEntry> selectedEntry() {
        if (this.selectedEntryId.isEmpty()) {
            return Optional.empty();
        }
        for (FieldsetEntry entry : this.adapter.entries()) {
            if (entry.identity().equals(this.selectedEntryId)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    private void reconcileSelection() {
        Set<String> profiles = new LinkedHashSet<String>();
        for (KonfigFieldsetCatalogModel.Profile profile : this.profiles()) {
            profiles.add(profile.key());
        }
        if (!this.selectedProfile.isEmpty() && !profiles.contains(this.selectedProfile)) {
            this.selectedProfile = "";
            this.selectedEntryId = "";
            this.view = View.OVERVIEW;
        }
        Optional<FieldsetEntry> selected = this.selectedEntry();
        if (selected.isPresent() && !this.model().profileKey(selected.get()).equals(this.selectedProfile)) {
            this.selectedEntryId = "";
        }
        if (this.selectedEntry().isEmpty() && this.view == View.DETAIL) {
            this.view = View.RULES;
        }
    }

    private void selectProfile(String profile) {
        if (!this.commitOrRevertBeforeNavigation()) {
            return;
        }
        this.selectedProfile = profile;
        this.selectedEntryId = "";
        this.query = "";
        this.filterValue = "";
        this.view = View.RULES;
        this.message = Component.empty();
        this.requestRebuild();
    }

    private void selectEntry(String identity) {
        if (!this.commitOrRevertBeforeNavigation()) {
            return;
        }
        this.selectedEntryId = identity;
        this.state.select(identity);
        this.view = View.DETAIL;
        this.message = Component.empty();
        this.requestRebuild();
    }

    private void back() {
        if (!this.commitOrRevertBeforeNavigation()) {
            return;
        }
        if (!this.wide && this.view == View.DETAIL) {
            this.view = View.RULES;
            this.selectedEntryId = "";
        } else {
            this.selectedProfile = "";
            this.selectedEntryId = "";
            this.view = View.OVERVIEW;
        }
        this.message = Component.empty();
        this.requestRebuild();
    }

    private void nextFilter() {
        List<String> values = this.model().filterValues(this.selectedProfile);
        int current = values.indexOf(this.filterValue);
        this.filterValue = current < 0 || current + 1 >= values.size() ? "" : values.get(current + 1);
        this.selectedEntryId = "";
        this.filter.setMessage(this.filterLabel());
        if (this.ruleList != null) {
            this.ruleList.rebuild();
        }
        if (this.detailList != null) {
            this.detailList.rebuild();
        }
    }

    private Component filterLabel() {
        String name = this.catalog.filterField().map(field -> pretty(field.key())).orElse("Filter");
        return Component.literal(name + ": " + (this.filterValue.isEmpty() ? "All" : this.filterValue));
    }

    private void addEntry() {
        FieldsetValue previous = this.session.original();
        KonfigFieldsetEditResult result = this.state.add();
        if (!this.finishMutation(previous, result)) {
            return;
        }
        this.selectedProfile = this.model().editableProfileKey();
        this.selectedEntryId = this.state.selectedEntryId();
        this.view = View.DETAIL;
        this.requestRebuild();
    }

    private void overrideSelected() {
        this.duplicateSelected();
    }

    private void duplicateSelected() {
        Optional<FieldsetEntry> selected = this.selectedEntry();
        if (selected.isEmpty()) {
            return;
        }
        this.state.select(selected.get().identity());
        FieldsetValue previous = this.session.original();
        KonfigFieldsetEditResult result = this.state.duplicateSelected();
        if (!this.finishMutation(previous, result)) {
            return;
        }
        this.selectedProfile = this.model().editableProfileKey();
        this.selectedEntryId = this.state.selectedEntryId();
        this.view = View.DETAIL;
        this.requestRebuild();
    }

    private void deleteSelected() {
        Optional<FieldsetEntry> selected = this.selectedEntry();
        if (selected.isEmpty()) {
            return;
        }
        this.state.select(selected.get().identity());
        FieldsetValue previous = this.session.original();
        if (!this.finishMutation(previous, this.state.deleteSelected())) {
            return;
        }
        this.selectedEntryId = "";
        this.view = View.RULES;
        this.requestRebuild();
    }

    private boolean finishMutation(FieldsetValue previous, KonfigFieldsetEditResult result) {
        if (result.accepted()) {
            result = this.persistDraft(previous, true);
        }
        this.message = result.accepted() ? Component.literal("Saved") : result.message();
        this.state.refresh();
        return result.submitted();
    }

    private KonfigFieldsetEditResult persistDraft(FieldsetValue previous, boolean recordUndo) {
        if (!this.session.dirty()) {
            return KonfigFieldsetEditResult.noChange();
        }
        List<FieldsetValidationIssue> issues = this.session.draft().validate().issues();
        if (!issues.isEmpty()) {
            this.session.restorePersisted();
            return KonfigFieldsetEditResult.invalid(Component.literal(issues.get(0).message()));
        }

        FieldsetValue candidate = this.session.draft();
        KonfigFieldsetEditResult result;
        try {
            result = Objects.requireNonNull(
                    this.persistAction.persist(previous, candidate),
                    "persistAction result"
            );
        } catch (RuntimeException exception) {
            String detail = exception.getMessage();
            this.session.restorePersisted();
            return KonfigFieldsetEditResult.invalid(Component.literal(detail == null || detail.isBlank()
                    ? "The Fieldset could not be saved."
                    : detail));
        }

        if (result.status() == KonfigFieldsetEditResult.Status.PENDING) {
            this.pendingSave = new PendingSave(previous, candidate, recordUndo);
            this.message = result.message();
            this.requestRebuild();
        } else if (result.accepted()) {
            if (recordUndo && result.changed()) {
                this.undoHistory.push(previous);
            }
            this.session.markPersisted();
        } else {
            this.session.restorePersisted();
        }
        return result;
    }

    private void completePersist(KonfigFieldsetEditResult result, FieldsetValue authoritative) {
        PendingSave pending = this.pendingSave;
        if (pending == null) {
            return;
        }
        this.pendingSave = null;
        this.session.adoptPersisted(authoritative);
        if (result.accepted()) {
            if (this.pendingUndo) {
                this.undoHistory.pop();
                this.message = Component.literal("Change undone");
            } else if (pending.recordUndo() && result.changed()) {
                this.undoHistory.push(pending.previous());
                this.message = Component.literal("Saved");
            } else {
                this.message = Component.literal("Saved");
            }
        } else {
            this.message = result.message();
        }
        this.pendingUndo = false;
        this.state.refresh();
        this.activeTextField = null;
        this.requestRebuild();
    }

    private void undo() {
        if (this.undoHistory.isEmpty()) {
            return;
        }
        FieldsetValue current = this.session.original();
        FieldsetValue target = this.undoHistory.peek();
        this.session.update(target);
        KonfigFieldsetEditResult result = this.persistDraft(current, false);
        if (result.status() == KonfigFieldsetEditResult.Status.PENDING) {
            this.pendingUndo = true;
        } else if (result.accepted()) {
            this.undoHistory.pop();
            this.session.adoptPersisted(target);
            this.message = Component.literal("Change undone");
            this.state.refresh();
            this.selectedEntryId = "";
            this.requestRebuild();
        } else {
            this.session.adoptPersisted(current);
            this.message = result.message();
        }
    }

    private boolean commitOrRevertBeforeNavigation() {
        TextFieldRow active = this.activeTextField;
        return active == null || !active.inputFocused() || active.commit();
    }

    private void revertActiveDraft() {
        if (this.activeTextField != null) {
            this.activeTextField.revert();
        }
    }

    private void refreshRevertAction() {
        if (this.revert != null) {
            this.revert.visible = this.activeTextField != null && this.activeTextField.hasLocalDraftError();
        }
    }

    @Override
    public void onClose() {
        if (this.pendingSave != null) {
            return;
        }
        if (this.activeTextField != null && this.activeTextField.inputFocused()) {
            this.activeTextField.revert();
            return;
        }
        this.closeToParent();
    }

    @Override
    public void removed() {
        this.persistenceSubscription.unsubscribe();
        super.removed();
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.pendingSave != null) {
            return true;
        }
        TextFieldRow active = this.activeTextField;
        if (active != null && active.handleSuggestionClick(event)) {
            return true;
        }
        if (active != null
                && active.inputFocused()
                && !active.isPointInsideInput(event.x(), event.y())
                && (this.revert == null || !this.revert.isMouseOver(event.x(), event.y()))) {
            if (!active.commit() || this.pendingSave != null) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.pendingSave != null) {
            return true;
        }
        TextFieldRow active = this.activeTextField;
        if (active != null && active.hasVisibleSuggestions() && active.handleSuggestionKey(event)) {
            return true;
        }
        if (active != null && active.inputFocused()) {
            int key = event.key();
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                return active.commit();
            }
            if (key == InputConstants.KEY_ESCAPE) {
                active.revert();
                return true;
            }
            if (key == InputConstants.KEY_TAB) {
                active.commit();
                return true;
            }
        }
        return super.keyPressed(event);
    }

//? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        this.renderedRegistryField = null;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
        this.renderRegistrySuggestions(context, mouseX, mouseY);
    }
//?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        KonfigRenderContext context = KonfigRenderContext.of(graphics);
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        this.renderedRegistryField = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderChrome(context);
        this.renderRegistrySuggestions(context, mouseX, mouseY);
    }
//?}

    private void renderChrome(KonfigRenderContext context) {
        context.drawCenteredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);
        String location = this.selectedProfile.isEmpty()
                ? "Profiles"
                : this.selectedEntry().map(entry -> this.adapter.entryLabel(entry).getString())
                        .orElseGet(this::selectedProfileLabel);
        Component subtitle = this.message.getString().isBlank()
                ? Component.literal(location)
                : this.message;
        int color = this.message.getString().isBlank() || this.message.getString().equals("Saved")
                || this.message.getString().equals("Change undone")
                ? 0xFFA0A0A0
                : 0xFFFF7070;
        context.drawCenteredText(this.font, subtitle, this.width / 2, 21, color);
    }

    private void renderRegistrySuggestions(KonfigRenderContext context, int mouseX, int mouseY) {
        TextFieldRow rendered = this.renderedRegistryField;
        if (rendered == null) {
            return;
        }
        context.renderFloatingLayers(
                layer -> rendered.renderSuggestions(layer, mouseX, mouseY),
                layer -> {
                }
        );
    }

    private Component fit(Component value, int width) {
        String text = value.getString();
        if (this.font.width(text) <= width) {
            return value;
        }
        String suffix = "...";
        return Component.literal(this.font.plainSubstrByWidth(text, Math.max(0, width - this.font.width(suffix))) + suffix);
    }

    private String selectedProfileLabel() {
        return this.profiles().stream()
                .filter(profile -> profile.key().equals(this.selectedProfile))
                .map(KonfigFieldsetCatalogModel.Profile::label)
                .findFirst()
                .orElse("Profiles");
    }

    private boolean missingRegistryEntry(FieldsetEntry entry) {
        Optional<KonfigFieldsetDraftAdapter.EntryIcon> icon = this.adapter.entryIcon(entry);
        if (icon.isEmpty() || icon.get().value().startsWith("#")) {
            return false;
        }
        return !hasRegistryIcon(icon.get().registryKey(), icon.get().value());
    }

    private Optional<Component> entryWarning(FieldsetEntry entry) {
        Optional<String> configured = this.catalog.warning(entry).filter(message -> !message.isBlank());
        if (configured.isPresent()) {
            return Optional.of(Component.literal(configured.get()));
        }
        return this.missingRegistryEntry(entry)
                ? Optional.of(Component.literal("Missing registry entry"))
                : Optional.empty();
    }

    private static String pretty(String key) {
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (int index = 0; index < key.length(); index++) {
            char character = key.charAt(index);
            if (character == '_' || character == '-' || character == '.') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != ' ') {
                    result.append(' ');
                }
                capitalize = true;
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
        }
        return result.toString().trim();
    }

    private enum View {
        OVERVIEW,
        RULES,
        DETAIL
    }

    private final class ProfileList extends ContainerObjectSelectionList<ProfileRow> {
        private final int rowWidth;

        private ProfileList(int width, int height, int y, boolean overview) {
            super(KonfigFieldsetCatalogScreen.this.minecraft, width, height, y, PROFILE_ROW_HEIGHT);
            this.rowWidth = width - 14;
            for (KonfigFieldsetCatalogModel.Profile profile : KonfigFieldsetCatalogScreen.this.profiles()) {
                this.addEntry(new ProfileRow(profile, overview), PROFILE_ROW_HEIGHT);
            }
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

//? if >=26.1 {
        @Override
        protected int scrollBarX() {
            return this.getRight() - this.scrollbarWidth();
        }
//?} else {
        @Override
        protected int scrollBarX() {
            return this.getRight() - 6;
        }
//?}
    }

    private final class ProfileRow extends ContainerObjectSelectionList.Entry<ProfileRow> {
        private final KonfigFieldsetCatalogModel.Profile profile;
        private final boolean overview;
        private final Component label;
        private final Button hitbox;

        private ProfileRow(KonfigFieldsetCatalogModel.Profile profile, boolean overview) {
            this.profile = profile;
            this.overview = overview;
            this.label = Component.literal(profile.label());
            this.hitbox = button(0, 0, 100, PROFILE_ROW_HEIGHT - 4, this.label, ignored -> {
                KonfigFieldsetCatalogScreen.this.selectProfile(this.profile.key());
            });
        }

        private void renderRow(KonfigRenderContext context, int x, int y, int width, boolean hovered) {
            boolean selected = this.profile.key().equals(KonfigFieldsetCatalogScreen.this.selectedProfile);
            int border = selected ? 0xFFE0E0E0 : hovered ? 0xFF777777 : 0xFF454545;
            context.fill(x, y, x + width, y + PROFILE_ROW_HEIGHT - 4, border);
            context.fill(x + 1, y + 1, x + width - 1, y + PROFILE_ROW_HEIGHT - 5, 0xE81B1B1B);
            this.hitbox.setX(x);
            this.hitbox.setY(y);
            this.hitbox.setWidth(width);
            this.hitbox.setHeight(PROFILE_ROW_HEIGHT - 4);

            int titleX = x + (this.overview ? 12 : 7);
            context.drawText(
                    KonfigFieldsetCatalogScreen.this.font,
                    KonfigFieldsetCatalogScreen.this.fit(Component.literal(this.profile.label()), width - 66),
                    titleX,
                    y + 7,
                    0xFFFFFFFF
            );
            String count = this.profile.entryCount() + (this.profile.entryCount() == 1 ? " item" : " items");
            context.drawText(
                    KonfigFieldsetCatalogScreen.this.font,
                    Component.literal(count),
                    titleX,
                    y + 20,
                    0xFFA0A0A0
            );
            String status = this.profile.editable() ? "Editable" : "Active";
            context.drawText(
                    KonfigFieldsetCatalogScreen.this.font,
                    Component.literal(status),
                    x + width - KonfigFieldsetCatalogScreen.this.font.width(status) - 8,
                    y + 13,
                    this.profile.editable() ? 0xFFFFD66B : 0xFF8FB98F
            );
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), hovered);
        }
//?} else {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), hovered);
        }
//?}

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.hitbox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.hitbox);
        }
    }

    private final class RuleList extends ContainerObjectSelectionList<RuleRow> {
        private final int rowWidth;

        private RuleList(int width, int height, int y) {
            super(KonfigFieldsetCatalogScreen.this.minecraft, width, height, y, RULE_ROW_HEIGHT);
            this.rowWidth = width - 14;
            this.rebuild();
        }

        private void rebuild() {
            double scroll = this.scrollAmount();
            this.clearEntries();
            for (FieldsetEntry entry : KonfigFieldsetCatalogScreen.this.visibleRules()) {
                this.addEntry(new RuleRow(entry), RULE_ROW_HEIGHT);
            }
            this.setScrollAmount(scroll);
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

//? if >=26.1 {
        @Override
        protected int scrollBarX() {
            return this.getRight() - this.scrollbarWidth();
        }
//?} else {
        @Override
        protected int scrollBarX() {
            return this.getRight() - 6;
        }
//?}
    }

    private final class RuleRow extends ContainerObjectSelectionList.Entry<RuleRow> {
        private final String entryId;
        private final Component label;
        private final Button hitbox;

        private RuleRow(FieldsetEntry entry) {
            this.entryId = entry.identity();
            this.label = entry == null
                    ? Component.empty()
                    : KonfigFieldsetCatalogScreen.this.adapter.entryLabel(entry);
            this.hitbox = button(0, 0, 100, RULE_ROW_HEIGHT - 3, this.label, ignored -> {
                KonfigFieldsetCatalogScreen.this.selectEntry(this.entryId);
            });
        }

        private FieldsetEntry entry() {
            return KonfigFieldsetCatalogScreen.this.adapter.entries().stream()
                    .filter(entry -> entry.identity().equals(this.entryId))
                    .findFirst()
                    .orElseThrow();
        }

        private void renderRow(KonfigRenderContext context, int x, int y, int width, boolean hovered) {
            FieldsetEntry entry = this.entry();
            boolean selected = this.entryId.equals(KonfigFieldsetCatalogScreen.this.selectedEntryId);
            KonfigFieldsetValidation validation = KonfigFieldsetCatalogScreen.this.validation.forEntry(this.entryId);
            Optional<Component> warning = KonfigFieldsetCatalogScreen.this.entryWarning(entry);
            int border = !validation.isValid()
                    ? 0xFFB84A4A
                    : warning.isPresent()
                            ? 0xFFC89545
                            : selected ? 0xFFE0E0E0 : hovered ? 0xFF777777 : 0xFF454545;
            context.fill(x, y, x + width, y + RULE_ROW_HEIGHT - 3, border);
            context.fill(x + 1, y + 1, x + width - 1, y + RULE_ROW_HEIGHT - 4, 0xE81B1B1B);
            this.hitbox.setX(x);
            this.hitbox.setY(y);
            this.hitbox.setWidth(width);
            this.hitbox.setHeight(RULE_ROW_HEIGHT - 3);

            int titleX = x + 7;
            Optional<KonfigFieldsetDraftAdapter.EntryIcon> icon = KonfigFieldsetCatalogScreen.this.adapter.entryIcon(entry);
            if (icon.isPresent() && hasRegistryIcon(icon.get().registryKey(), icon.get().value())) {
                context.renderRegistryIcon(icon.get().registryKey(), icon.get().value(), titleX, y + 3, ICON_SIZE);
                titleX += ICON_SIZE + 5;
            }
            int available = Math.max(40, x + width - titleX - 8);
            context.drawText(
                    KonfigFieldsetCatalogScreen.this.font,
                    KonfigFieldsetCatalogScreen.this.fit(KonfigFieldsetCatalogScreen.this.adapter.entryLabel(entry), available),
                    titleX,
                    y + 4,
                    0xFFFFFFFF
            );
            Component summary = warning.orElseGet(() -> KonfigFieldsetCatalogScreen.this.fit(
                            KonfigFieldsetCatalogScreen.this.adapter.entrySummary(entry),
                            available
                    ));
            if (!summary.getString().isBlank()) {
                context.drawText(
                        KonfigFieldsetCatalogScreen.this.font,
                        summary,
                        titleX,
                        y + 16,
                        warning.isPresent() ? 0xFFFFC45C : 0xFFA0A0A0
                );
            }
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), hovered);
        }
//?} else {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth(), hovered);
        }
//?}

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.hitbox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.hitbox);
        }
    }

    private final class DetailList extends ContainerObjectSelectionList<DetailRow> {
        private final int rowWidth;

        private DetailList(int width, int height, int y) {
            super(KonfigFieldsetCatalogScreen.this.minecraft, width, height, y, DETAIL_FIELD_HEIGHT);
            this.rowWidth = width - 14;
            this.rebuild();
        }

        private void rebuild() {
            this.clearEntries();
            Optional<FieldsetEntry> selected = KonfigFieldsetCatalogScreen.this.selectedEntry();
            if (selected.isEmpty()) {
                return;
            }
            FieldsetEntry entry = selected.get();
            this.addEntry(new DetailHeaderRow(entry), 48);

            Set<FieldsetField<?>> included = new LinkedHashSet<FieldsetField<?>>();
            for (FieldsetCatalog.Section section : KonfigFieldsetCatalogScreen.this.catalog.sections()) {
                this.addEntry(new SectionRow(section.label()), DETAIL_SECTION_HEIGHT);
                for (FieldsetField<?> field : section.fields()) {
                    included.add(field);
                    this.addEntry(this.fieldRow(entry, field), DETAIL_FIELD_HEIGHT);
                }
            }
            List<FieldsetField<?>> remaining = new ArrayList<FieldsetField<?>>();
            for (FieldsetField<?> field : KonfigFieldsetCatalogScreen.this.adapter.fields(entry)) {
                if (!included.contains(field)) {
                    remaining.add(field);
                }
            }
            if (!remaining.isEmpty()) {
                this.addEntry(new SectionRow(KonfigFieldsetCatalogScreen.this.catalog.sections().isEmpty() ? "Details" : "Other"), DETAIL_SECTION_HEIGHT);
                for (FieldsetField<?> field : remaining) {
                    this.addEntry(this.fieldRow(entry, field), DETAIL_FIELD_HEIGHT);
                }
            }
        }

        private DetailRow fieldRow(FieldsetEntry entry, FieldsetField<?> field) {
            KonfigFieldsetEntryEditorState<FieldsetEntry, FieldsetField<?>> editor =
                    new KonfigFieldsetEntryEditorState<FieldsetEntry, FieldsetField<?>>(
                            KonfigFieldsetCatalogScreen.this.adapter,
                            entry.identity(),
                            KonfigFieldsetCatalogScreen.this.validation
                    );
            for (KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> state : editor.fields()) {
                if (state.field() != field) {
                    continue;
                }
                if (field.kind() == FieldsetFieldKind.BOOLEAN) {
                    return new BooleanFieldRow(state);
                }
                if (field.kind() == FieldsetFieldKind.DROPDOWN) {
                    return new DropdownFieldRow(state);
                }
                return new TextFieldRow(state);
            }
            throw new IllegalStateException("Missing Fieldset detail field " + field.key());
        }

        private void tickRows() {
            for (DetailRow row : this.children()) {
                row.tick();
            }
        }

        @Override
        public int getRowWidth() {
            return this.rowWidth;
        }

//? if >=26.1 {
        @Override
        protected int scrollBarX() {
            return this.getRight() - this.scrollbarWidth();
        }
//?} else {
        @Override
        protected int scrollBarX() {
            return this.getRight() - 6;
        }
//?}
    }

    private abstract class DetailRow extends ContainerObjectSelectionList.Entry<DetailRow> {
        void tick() {
        }
    }

    private final class DetailHeaderRow extends DetailRow {
        private final FieldsetEntry entry;

        private DetailHeaderRow(FieldsetEntry entry) {
            this.entry = entry;
        }

        private void renderRow(KonfigRenderContext context, int x, int y, int width) {
            int titleX = x + 6;
            Optional<KonfigFieldsetDraftAdapter.EntryIcon> icon = KonfigFieldsetCatalogScreen.this.adapter.entryIcon(this.entry);
            if (icon.isPresent() && hasRegistryIcon(icon.get().registryKey(), icon.get().value())) {
                context.renderRegistryIcon(icon.get().registryKey(), icon.get().value(), titleX, y + 7, 28);
                titleX += 34;
            }
            context.drawText(
                    KonfigFieldsetCatalogScreen.this.font,
                    KonfigFieldsetCatalogScreen.this.fit(KonfigFieldsetCatalogScreen.this.adapter.entryLabel(this.entry), x + width - titleX - 6),
                    titleX,
                    y + 8,
                    0xFFFFFFFF
            );
            String source = this.entry.editable()
                    ? KonfigFieldsetCatalogScreen.this.catalog.editableProfileLabel()
                    : this.entry.source().orElse("Built in");
            context.drawText(KonfigFieldsetCatalogScreen.this.font, Component.literal(source), titleX, y + 22, 0xFFFFD66B);
            Optional<Component> warning = KonfigFieldsetCatalogScreen.this.entryWarning(this.entry);
            context.drawText(
                    KonfigFieldsetCatalogScreen.this.font,
                    warning.orElseGet(() -> Component.literal(
                            this.entry.editable() ? "Editable declaration" : "Effective read-only rule"
                    )),
                    titleX,
                    y + 34,
                    warning.isPresent() ? 0xFFFFC45C : 0xFFA0A0A0
            );
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth());
        }
//?} else {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth());
        }
//?}

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private final class SectionRow extends DetailRow {
        private final Component label;

        private SectionRow(String label) {
            this.label = Component.literal(label);
        }

        private void renderRow(KonfigRenderContext context, int x, int y, int width) {
            context.fill(x + 4, y + 18, x + width - 4, y + 19, 0xFF454545);
            context.drawText(KonfigFieldsetCatalogScreen.this.font, this.label, x + 6, y + 6, 0xFFFFD66B);
        }

//? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth());
        }
//?} else {
        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.renderRow(KonfigRenderContext.of(graphics), this.getContentX(), this.getContentY(), this.getContentWidth());
        }
//?}

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private abstract class FieldRow extends DetailRow {
        final KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field;
        final List<AbstractWidget> controls = new ArrayList<AbstractWidget>();
        String localError = "";

        private FieldRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
            this.field = field;
        }

        final boolean apply(Object value) {
            FieldsetValue previous = KonfigFieldsetCatalogScreen.this.session.original();
            KonfigFieldsetEditResult result = this.field.value().setDraft(value);
            if (result.accepted()) {
                result = KonfigFieldsetCatalogScreen.this.persistDraft(previous, true);
            }
            this.localError = result.submitted() ? "" : result.message().getString();
            KonfigFieldsetCatalogScreen.this.message = result.accepted()
                    ? Component.literal("Saved")
                    : result.message();
            if (result.submitted()) {
                KonfigFieldsetCatalogScreen.this.state.refresh();
                KonfigFieldsetCatalogScreen.this.requestRebuild();
            }
            KonfigFieldsetCatalogScreen.this.refreshRevertAction();
            return result.submitted();
        }

        final String validationMessage() {
            if (!this.localError.isBlank()) {
                return this.localError;
            }
            List<KonfigFieldsetValidation.Issue> issues = this.field.validation().issues();
            return issues.isEmpty() ? "" : issues.get(0).message().getString();
        }

        final void renderRow(
                KonfigRenderContext context,
                int x,
                int y,
                int width,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            int controlX = x + Math.max(106, width * 40 / 100);
            int controlWidth = Math.max(82, x + width - controlX - 4);
            context.drawText(KonfigFieldsetCatalogScreen.this.font, this.field.label(), x + 6, y + 8, 0xFFE8E8E8);
            this.layoutControls(controlX, y + 4, controlWidth);
            this.renderDecoration(context, controlX, y, controlWidth);
            for (AbstractWidget control : this.controls) {
                context.renderWidget(control, mouseX, mouseY, partialTick);
            }
            String validation = this.validationMessage();
            if (!validation.isBlank()) {
                context.drawText(
                        KonfigFieldsetCatalogScreen.this.font,
                        KonfigFieldsetCatalogScreen.this.fit(Component.literal(validation), controlWidth),
                        controlX,
                        y + 27,
                        0xFFFF7070
                );
            }
        }

        void renderDecoration(KonfigRenderContext context, int controlX, int y, int controlWidth) {
        }

        abstract void layoutControls(int x, int y, int width);

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
    }

    private final class BooleanFieldRow extends FieldRow {
        private final Button toggle;

        private BooleanFieldRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
            super(field);
            this.toggle = button(0, 0, 100, CONTROL_HEIGHT, Component.empty(), ignored -> {
                Object current = this.field.value().draft();
                this.apply(Boolean.valueOf(!(current instanceof Boolean) || !((Boolean) current).booleanValue()));
            });
            this.toggle.active = field.value().access().canEdit();
            this.controls.add(this.toggle);
            this.sync();
        }

        private void sync() {
            this.toggle.setMessage(Component.literal(Boolean.TRUE.equals(this.field.value().draft()) ? "On" : "Off"));
        }

        @Override
        void layoutControls(int x, int y, int width) {
            this.toggle.setX(x);
            this.toggle.setY(y);
            this.toggle.setWidth(width);
        }
    }

    private final class DropdownFieldRow extends FieldRow {
        private final Button dropdown;

        private DropdownFieldRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
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
            if (this.apply(options.get((index + 1 + options.size()) % options.size()))) {
                this.sync();
            }
        }

        private void sync() {
            this.dropdown.setMessage(Component.literal(String.valueOf(this.field.value().draft())));
        }

        @Override
        void layoutControls(int x, int y, int width) {
            this.dropdown.setX(x);
            this.dropdown.setY(y);
            this.dropdown.setWidth(width);
        }
    }

    private final class TextFieldRow extends FieldRow {
        private final EditBox input;
        private final KonfigRegistrySuggestionController suggestions;
        private boolean suppressResponder;

        private TextFieldRow(KonfigFieldsetEntryEditorState.FieldState<FieldsetField<?>> field) {
            super(field);
            this.input = new EditBox(
                    KonfigFieldsetCatalogScreen.this.font,
                    0,
                    0,
                    100,
                    CONTROL_HEIGHT,
                    field.label()
            );
            this.input.setMaxLength(512);
            this.input.setValue(this.committedText());
            this.input.moveCursorToStart(false);
            boolean editable = field.value().access().canEdit();
            this.input.setEditable(editable);
            this.input.active = editable;
            this.input.setResponder(value -> {
                if (!this.suppressResponder) {
                    this.localError = "";
                    KonfigFieldsetCatalogScreen.this.message = Component.empty();
                    this.refreshSuggestions();
                    KonfigFieldsetCatalogScreen.this.refreshRevertAction();
                }
            });
            this.controls.add(this.input);

            if (field.field().kind() == FieldsetFieldKind.REGISTRY_STRING && field.field().registryKey().isPresent()) {
                this.suggestions = new KonfigRegistrySuggestionController(new KonfigRegistrySuggestionController.Owner() {
                    @Override
                    public boolean hasRegistryBinding() {
                        return true;
                    }

                    @Override
                    public ResourceKey<? extends Registry<?>> registryKey() {
                        return TextFieldRow.this.registryKey();
                    }

                    @Override
                    public List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
                        List<String> matches = KonfigFieldsetCatalogScreen.this.registrySuggestions.find(
                                registryKey,
                                TextFieldRow.this.input.getValue(),
                                12
                        );
                        return matches == null ? List.of() : matches;
                    }

                    @Override
                    public String inputValue() {
                        return TextFieldRow.this.input.getValue();
                    }

                    @Override
                    public void setInlineSuggestion(String suggestion) {
                        TextFieldRow.this.input.setSuggestion(suggestion);
                    }

                    @Override
                    public boolean applySuggestion(String suggestion) {
                        TextFieldRow.this.suppressResponder = true;
                        TextFieldRow.this.input.setValue(suggestion);
                        TextFieldRow.this.suppressResponder = false;
                        return TextFieldRow.this.commit();
                    }

                    @Override
                    public void focusInput() {
                        TextFieldRow.this.input.setFocused(true);
                    }

                    @Override
                    public Font font() {
                        return KonfigFieldsetCatalogScreen.this.font;
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
                        return KonfigFieldsetCatalogScreen.this.height;
                    }

                    @Override
                    public int listTop() {
                        return HEADER_BOTTOM;
                    }
                });
            } else {
                this.suggestions = null;
            }
        }

        private boolean commit() {
            if (!this.field.value().access().canEdit()) {
                return true;
            }
            Object parsed;
            try {
                parsed = this.parse(this.input.getValue());
            } catch (IllegalArgumentException exception) {
                this.localError = exception.getMessage() == null ? "Invalid value" : exception.getMessage();
                KonfigFieldsetCatalogScreen.this.message = Component.literal(this.localError);
                this.input.setFocused(true);
                KonfigFieldsetCatalogScreen.this.activeTextField = this;
                KonfigFieldsetCatalogScreen.this.refreshRevertAction();
                return false;
            }
            boolean accepted = this.apply(parsed);
            if (accepted) {
                this.input.setFocused(false);
                this.closeSuggestions();
                KonfigFieldsetCatalogScreen.this.activeTextField = null;
            } else {
                this.input.setFocused(true);
                KonfigFieldsetCatalogScreen.this.activeTextField = this;
            }
            KonfigFieldsetCatalogScreen.this.refreshRevertAction();
            return accepted;
        }

        private void revert() {
            this.suppressResponder = true;
            this.input.setValue(this.committedText());
            this.suppressResponder = false;
            this.input.setFocused(false);
            this.input.moveCursorToStart(false);
            this.localError = "";
            this.closeSuggestions();
            if (KonfigFieldsetCatalogScreen.this.activeTextField == this) {
                KonfigFieldsetCatalogScreen.this.activeTextField = null;
            }
            KonfigFieldsetCatalogScreen.this.message = Component.empty();
            KonfigFieldsetCatalogScreen.this.refreshRevertAction();
        }

        private boolean hasLocalDraftError() {
            return !this.localError.isBlank();
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
                throw new IllegalArgumentException("Enter a valid " + kind.name().toLowerCase(Locale.ROOT) + ".");
            }
            if (kind == FieldsetFieldKind.OPTIONAL_STRING) {
                return normalized.isEmpty() ? Optional.empty() : Optional.of(text);
            }
            return text;
        }

        private String committedText() {
            Object value = this.field.value().draft();
            if (value instanceof Optional<?> optional) {
                return optional.map(String::valueOf).orElse("");
            }
            return String.valueOf(value);
        }

        private boolean inputFocused() {
            return this.input.isFocused();
        }

        private boolean isPointInsideInput(double mouseX, double mouseY) {
            return this.input.isMouseOver(mouseX, mouseY);
        }

        private boolean hasVisibleSuggestions() {
            return this.suggestions != null && this.suggestions.hasVisibleSuggestions();
        }

        private boolean handleSuggestionClick(MouseButtonEvent event) {
            return this.suggestions != null && this.suggestions.handleClick(event.x(), event.y());
        }

        private boolean handleSuggestionKey(KeyEvent event) {
            return this.suggestions != null && this.suggestions.handleKey(event.key());
        }

        private ResourceKey<? extends Registry<?>> registryKey() {
            return this.field.field().registryKey().orElseThrow();
        }

        private void refreshSuggestions() {
            if (this.suggestions != null) {
                this.suggestions.refresh();
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

        @Override
        void tick() {
            if (this.input.isFocused()) {
                KonfigFieldsetCatalogScreen.this.activeTextField = this;
                this.refreshSuggestions();
            }
        }

        @Override
        void layoutControls(int x, int y, int width) {
            this.input.setX(x);
            this.input.setY(y);
            this.input.setWidth(width);
            if (this.suggestions != null) {
                this.suggestions.updateInputBounds(x, y, width);
            }
        }

        @Override
        void renderDecoration(KonfigRenderContext context, int controlX, int y, int controlWidth) {
            if (!this.input.isFocused()) {
                this.input.moveCursorToStart(false);
            }
            if (this.suggestions == null) {
                return;
            }
            if (hasRegistryIcon(this.registryKey(), this.input.getValue())) {
                context.renderRegistryIcon(this.registryKey(), this.input.getValue(), controlX - 22, y + 6);
            }
            if (this.input.isFocused()) {
                KonfigFieldsetCatalogScreen.this.activeTextField = this;
                this.refreshSuggestions();
            }
            if (KonfigFieldsetCatalogScreen.this.activeTextField == this && this.hasVisibleSuggestions()) {
                KonfigFieldsetCatalogScreen.this.renderedRegistryField = this;
            }
        }
    }

    private record PendingSave(FieldsetValue previous, FieldsetValue candidate, boolean recordUndo) {
    }
}
//?}
