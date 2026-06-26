package com.iamkaf.konfig.impl.v1;

//? if >=1.17 {
import java.util.Collections;
import java.util.List;

final class KonfigStringListEditorState {
    interface PersistAction {
        boolean persist(Object previousValue);
    }

    private final KonfigScreenSession session;
    private final EntryRef entry;
    private final PersistAction persistAction;

    KonfigStringListEditorState(KonfigScreenSession session, EntryRef entry, PersistAction persistAction) {
        this.session = session;
        this.entry = entry;
        this.persistAction = persistAction;
    }

    List<String> values() {
        return this.session.currentStringList(this.entry.value);
    }

    int size() {
        return this.values().size();
    }

    boolean isEmpty() {
        return this.size() == 0;
    }

    String valueAt(int index) {
        List<String> values = this.values();
        return isValidIndex(index, values.size()) ? values.get(index) : "";
    }

    boolean canMoveUp(int index) {
        return index > 0 && index < this.size();
    }

    boolean canMoveDown(int index) {
        return index >= 0 && index + 1 < this.size();
    }

    boolean add(String fallbackValue) {
        List<String> values = this.values();
        values.add(this.entry.value.hasBoundRegistry() ? "" : fallbackValue);
        return this.commit(values);
    }

    boolean set(int index, String value) {
        List<String> values = this.values();
        if (!isValidIndex(index, values.size())) {
            return false;
        }

        values.set(index, value);
        return this.commit(values);
    }

    boolean move(int index, int delta) {
        int targetIndex = index + delta;
        List<String> values = this.values();
        if (!isValidIndex(index, values.size()) || !isValidIndex(targetIndex, values.size())) {
            return false;
        }

        Collections.swap(values, index, targetIndex);
        return this.commit(values);
    }

    boolean remove(int index) {
        List<String> values = this.values();
        if (!isValidIndex(index, values.size())) {
            return false;
        }

        values.remove(index);
        return this.commit(values);
    }

    private boolean commit(List<String> values) {
        Object previousValue = this.session.storedSnapshot(this.entry.value);
        this.session.setDraft(this.entry.value, values);
        return this.persistAction.persist(previousValue);
    }

    private static boolean isValidIndex(int index, int size) {
        return index >= 0 && index < size;
    }
}
//?}
