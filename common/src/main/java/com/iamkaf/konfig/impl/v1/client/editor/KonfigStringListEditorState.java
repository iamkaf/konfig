package com.iamkaf.konfig.impl.v1.client.editor;

import org.jetbrains.annotations.ApiStatus;

//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so editor session state belongs to the 1.17 client API baseline.
import com.iamkaf.konfig.impl.v1.client.screen.EntryRef;
import com.iamkaf.konfig.impl.v1.client.screen.KonfigScreenSession;

import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public final class KonfigStringListEditorState {
    public interface PersistAction {
        boolean persist(Object previousValue);
    }

    private final KonfigScreenSession session;
    private final EntryRef entry;
    private final PersistAction persistAction;

    public KonfigStringListEditorState(KonfigScreenSession session, EntryRef entry, PersistAction persistAction) {
        this.session = session;
        this.entry = entry;
        this.persistAction = persistAction;
    }

    public List<String> values() {
        return this.session.currentStringList(this.entry.value);
    }

    public int size() {
        return this.values().size();
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public String valueAt(int index) {
        List<String> values = this.values();
        return isValidIndex(index, values.size()) ? values.get(index) : "";
    }

    public boolean canMoveUp(int index) {
        return index > 0 && index < this.size();
    }

    public boolean canMoveDown(int index) {
        return index >= 0 && index + 1 < this.size();
    }

    public boolean add(String fallbackValue) {
        List<String> values = this.values();
        values.add(this.entry.value.hasBoundRegistry() ? "" : fallbackValue);
        return this.commit(values);
    }

    public boolean set(int index, String value) {
        List<String> values = this.values();
        if (!isValidIndex(index, values.size())) {
            return false;
        }

        values.set(index, value);
        return this.commit(values);
    }

    public boolean move(int index, int delta) {
        int targetIndex = index + delta;
        List<String> values = this.values();
        if (!isValidIndex(index, values.size()) || !isValidIndex(targetIndex, values.size())) {
            return false;
        }

        Collections.swap(values, index, targetIndex);
        return this.commit(values);
    }

    public boolean remove(int index) {
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
