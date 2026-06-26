//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

final class KonfigEditorHost {
    private final KonfigConfigScreen screen;

    KonfigEditorHost(KonfigConfigScreen screen) {
        this.screen = screen;
    }

    void returnToMainScreen() {
        this.screen.returnToMainScreen();
    }

    boolean persistEditedValue(EntryRef entry, Object previousValue) {
        if (!this.screen.persistEntry(entry)) {
            this.screen.setDraft(entry.value, previousValue);
            return false;
        }
        return true;
    }

    boolean resetToSessionStart(EntryRef entry) {
        return this.screen.resetEntry(entry);
    }

    Object storedSnapshot(ConfigValueImpl<?> value) {
        return this.screen.storedSnapshot(value);
    }

    void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.screen.setDraft(value, draft);
    }

    int currentColor(ConfigValueImpl<?> value) {
        return this.screen.currentColor(value);
    }

    List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        return this.screen.registrySuggestions(registryKey);
    }

    KonfigStringListEditorState stringListEditorState(EntryRef entry, KonfigStringListEditorState.PersistAction persistAction) {
        return this.screen.stringListEditorState(entry, persistAction);
    }
}
//?}
