//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
final class KonfigEditorHost {
    private final KonfigConfigScreen screen;
    private final KonfigScreenCoordinator coordinator;

    KonfigEditorHost(KonfigConfigScreen screen, KonfigScreenCoordinator coordinator) {
        this.screen = screen;
        this.coordinator = coordinator;
    }

    void returnToMainScreen() {
        this.screen.returnToMainScreen();
    }

    boolean persistEditedValue(EntryRef entry, Object previousValue) {
        if (!this.coordinator.persistEntry(entry)) {
            this.coordinator.setDraft(entry.value, previousValue);
            return false;
        }
        return true;
    }

    boolean resetToSessionStart(EntryRef entry) {
        return this.coordinator.resetEntry(entry);
    }

    Object storedSnapshot(ConfigValueImpl<?> value) {
        return this.coordinator.storedSnapshot(value);
    }

    void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.coordinator.setDraft(value, draft);
    }

    int currentColor(ConfigValueImpl<?> value) {
        return this.coordinator.currentColor(value);
    }

    List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        return this.coordinator.registrySuggestions(registryKey);
    }

    KonfigStringListEditorState stringListEditorState(EntryRef entry, KonfigStringListEditorState.PersistAction persistAction) {
        return this.coordinator.stringListEditorState(entry, persistAction);
    }
}
//?}
