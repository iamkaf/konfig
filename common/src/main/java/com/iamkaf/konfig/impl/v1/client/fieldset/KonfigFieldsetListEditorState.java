//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@ApiStatus.Internal
public final class KonfigFieldsetListEditorState<E, F> {
    private final KonfigFieldsetUiAdapter<E, F> adapter;
    private String query = "";
    private String selectedEntryId = "";
    private String expandedEntryId = "";
    private KonfigFieldsetEditResult lastResult = KonfigFieldsetEditResult.noChange();

    public KonfigFieldsetListEditorState(KonfigFieldsetUiAdapter<E, F> adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.selectFirstVisibleEntry();
    }

    public List<VisibleEntry<E>> visibleEntries() {
        List<E> entries = this.adapter.entries();
        List<VisibleEntry<E>> visible = new ArrayList<>();
        KonfigFieldsetValidation validation = this.adapter.validation();
        for (int index = 0; index < entries.size(); index++) {
            E entry = entries.get(index);
            if (!this.adapter.matches(entry, this.query)) {
                continue;
            }
            String entryId = this.adapter.entryId(entry);
            visible.add(new VisibleEntry<>(
                    entry,
                    index,
                    entryId.equals(this.selectedEntryId),
                    this.adapter.entryAccess(entry),
                    validation.forEntry(entryId)
            ));
        }
        return List.copyOf(visible);
    }

    public String query() {
        return this.query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Optional<E> selected = this.selectedEntry();
        if (selected.isEmpty() || !this.adapter.matches(selected.get(), this.query)) {
            this.selectFirstVisibleEntry();
        }
        if (this.entry(this.expandedEntryId)
                .filter(entry -> this.adapter.matches(entry, this.query))
                .isEmpty()) {
            this.expandedEntryId = "";
        }
    }

    public String selectedEntryId() {
        return this.selectedEntryId;
    }

    public Optional<E> selectedEntry() {
        return this.entry(this.selectedEntryId);
    }

    public boolean select(String entryId) {
        Optional<E> entry = this.entry(entryId);
        if (entry.isEmpty()) {
            return false;
        }
        this.selectedEntryId = this.adapter.entryId(entry.get());
        return true;
    }

    public String expandedEntryId() {
        return this.expandedEntryId;
    }

    public boolean isExpanded(String entryId) {
        return this.expandedEntryId.equals(entryId);
    }

    public boolean toggleExpanded(String entryId) {
        if (!this.select(entryId)) {
            return false;
        }
        this.expandedEntryId = this.expandedEntryId.equals(entryId) ? "" : entryId;
        return true;
    }

    public KonfigFieldsetEditResult lastResult() {
        return this.lastResult;
    }

    public void clearResult() {
        this.lastResult = KonfigFieldsetEditResult.noChange();
    }

    public boolean canAdd() {
        return this.adapter.fieldsetAccess().canAdd();
    }

    public boolean canDuplicateSelected() {
        return this.selectedEntry()
                .map(entry -> this.adapter.fieldsetAccess().canAdd()
                        && this.adapter.entryAccess(entry).canDuplicate())
                .orElse(false);
    }

    public boolean canDeleteSelected() {
        return this.adapter.fieldsetAccess().canDelete() && this.selectedEntry()
                .map(entry -> this.adapter.entryAccess(entry).canDelete())
                .orElse(false);
    }

    public boolean canMoveSelectedUp() {
        return this.reorderTargetIndex(-1) >= 0;
    }

    public boolean canMoveSelectedDown() {
        return this.reorderTargetIndex(1) >= 0;
    }

    public KonfigFieldsetEditResult add() {
        KonfigFieldsetAccess access = this.adapter.fieldsetAccess();
        if (!access.canAdd()) {
            return this.record(KonfigFieldsetEditResult.readOnly(access.reason()));
        }

        this.query = "";
        E entry = Objects.requireNonNull(this.adapter.createEntry(), "adapter.createEntry()");
        String entryId = this.requireUniqueId(entry, "New fieldset entry");
        List<E> draft = new ArrayList<>(this.adapter.entries());
        draft.add(entry);
        KonfigFieldsetEditResult result = this.apply(draft);
        if (result.accepted()) {
            this.selectedEntryId = entryId;
            this.expandedEntryId = entryId;
        }
        return result;
    }

    public KonfigFieldsetEditResult duplicateSelected() {
        Optional<E> selected = this.selectedEntry();
        if (selected.isEmpty()) {
            return this.record(KonfigFieldsetEditResult.noChange());
        }
        E source = selected.get();
        KonfigFieldsetAccess fieldsetAccess = this.adapter.fieldsetAccess();
        KonfigFieldsetAccess entryAccess = this.adapter.entryAccess(source);
        if (!fieldsetAccess.canAdd()) {
            return this.record(KonfigFieldsetEditResult.readOnly(fieldsetAccess.reason()));
        }
        if (!entryAccess.canDuplicate()) {
            return this.record(KonfigFieldsetEditResult.readOnly(entryAccess.reason()));
        }

        this.query = "";
        E duplicate = Objects.requireNonNull(this.adapter.duplicateEntry(source), "adapter.duplicateEntry(entry)");
        String duplicateId = this.requireUniqueId(duplicate, "Duplicated fieldset entry");
        List<E> draft = new ArrayList<>(this.adapter.entries());
        int sourceIndex = this.selectedIndex();
        draft.add(sourceIndex + 1, duplicate);
        KonfigFieldsetEditResult result = this.apply(draft);
        if (result.accepted()) {
            this.selectedEntryId = duplicateId;
            this.expandedEntryId = duplicateId;
        }
        return result;
    }

    public KonfigFieldsetEditResult deleteSelected() {
        int selectedIndex = this.selectedIndex();
        if (selectedIndex < 0) {
            return this.record(KonfigFieldsetEditResult.noChange());
        }

        List<E> current = this.adapter.entries();
        E selected = current.get(selectedIndex);
        KonfigFieldsetAccess fieldsetAccess = this.adapter.fieldsetAccess();
        KonfigFieldsetAccess access = this.adapter.entryAccess(selected);
        if (!fieldsetAccess.canDelete()) {
            return this.record(KonfigFieldsetEditResult.readOnly(fieldsetAccess.reason()));
        }
        if (!access.canDelete()) {
            return this.record(KonfigFieldsetEditResult.readOnly(access.reason()));
        }

        List<E> draft = new ArrayList<>(current);
        draft.remove(selectedIndex);
        String nextSelection = draft.isEmpty()
                ? ""
                : this.adapter.entryId(draft.get(Math.min(selectedIndex, draft.size() - 1)));
        KonfigFieldsetEditResult result = this.apply(draft);
        if (result.accepted()) {
            this.selectedEntryId = nextSelection;
            if (this.expandedEntryId.equals(this.adapter.entryId(selected))) {
                this.expandedEntryId = "";
            }
            Optional<E> next = this.selectedEntry();
            if (next.isEmpty() || !this.adapter.matches(next.get(), this.query)) {
                this.selectFirstVisibleEntry();
            }
        }
        return result;
    }

    public KonfigFieldsetEditResult moveSelected(int offset) {
        if (offset == 0) {
            return this.record(KonfigFieldsetEditResult.noChange());
        }
        int selectedIndex = this.selectedIndex();
        int targetIndex = this.reorderTargetIndex(offset);
        List<E> current = this.adapter.entries();
        if (selectedIndex < 0 || targetIndex < 0) {
            return this.record(KonfigFieldsetEditResult.noChange());
        }

        E selected = current.get(selectedIndex);
        KonfigFieldsetAccess fieldsetAccess = this.adapter.fieldsetAccess();
        KonfigFieldsetAccess access = this.adapter.entryAccess(selected);
        if (!fieldsetAccess.canReorder()) {
            return this.record(KonfigFieldsetEditResult.readOnly(fieldsetAccess.reason()));
        }
        if (!access.canReorder()) {
            return this.record(KonfigFieldsetEditResult.readOnly(access.reason()));
        }

        List<E> draft = new ArrayList<>(current);
        Collections.swap(draft, selectedIndex, targetIndex);
        return this.apply(draft);
    }

    public void refresh() {
        if (this.selectedEntry().isEmpty()) {
            this.selectFirstVisibleEntry();
        }
        if (this.entry(this.expandedEntryId).isEmpty()) {
            this.expandedEntryId = "";
        }
    }

    private boolean canReorderSelected() {
        return this.adapter.fieldsetAccess().canReorder() && this.selectedEntry()
                .map(entry -> this.adapter.entryAccess(entry).canReorder())
                .orElse(false);
    }

    private int reorderTargetIndex(int offset) {
        if (offset == 0 || !this.canReorderSelected()) {
            return -1;
        }
        List<E> entries = this.adapter.entries();
        int direction = offset < 0 ? -1 : 1;
        int remaining = Math.abs(offset);
        for (int index = this.selectedIndex() + direction; index >= 0 && index < entries.size(); index += direction) {
            if (!this.adapter.entryAccess(entries.get(index)).canReorder()) {
                continue;
            }
            remaining--;
            if (remaining == 0) {
                return index;
            }
        }
        return -1;
    }

    private int selectedIndex() {
        List<E> entries = this.adapter.entries();
        for (int index = 0; index < entries.size(); index++) {
            if (this.adapter.entryId(entries.get(index)).equals(this.selectedEntryId)) {
                return index;
            }
        }
        return -1;
    }

    private Optional<E> entry(String entryId) {
        if (entryId == null || entryId.isEmpty()) {
            return Optional.empty();
        }
        for (E entry : this.adapter.entries()) {
            if (this.adapter.entryId(entry).equals(entryId)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    private void selectFirstVisibleEntry() {
        for (E entry : this.adapter.entries()) {
            if (this.adapter.matches(entry, this.query)) {
                this.selectedEntryId = this.adapter.entryId(entry);
                return;
            }
        }
        this.selectedEntryId = "";
    }

    private String requireUniqueId(E entry, String operation) {
        String entryId = Objects.requireNonNull(this.adapter.entryId(entry), "adapter.entryId(entry)");
        if (entryId.isBlank()) {
            throw new IllegalStateException(operation + " returned a blank identity.");
        }
        if (this.entry(entryId).isPresent()) {
            throw new IllegalStateException(operation + " returned duplicate identity '" + entryId + "'.");
        }
        return entryId;
    }

    private KonfigFieldsetEditResult apply(List<E> draft) {
        return this.record(Objects.requireNonNull(
                this.adapter.replaceEntries(List.copyOf(draft)),
                "adapter.replaceEntries(entries)"
        ));
    }

    private KonfigFieldsetEditResult record(KonfigFieldsetEditResult result) {
        this.lastResult = Objects.requireNonNull(result, "result");
        return result;
    }

    public record VisibleEntry<E>(
            E entry,
            int sourceIndex,
            boolean selected,
            KonfigFieldsetAccess access,
            KonfigFieldsetValidation validation
    ) {
        public VisibleEntry {
            Objects.requireNonNull(entry, "entry");
            Objects.requireNonNull(access, "access");
            Objects.requireNonNull(validation, "validation");
        }
    }
}
//?}
