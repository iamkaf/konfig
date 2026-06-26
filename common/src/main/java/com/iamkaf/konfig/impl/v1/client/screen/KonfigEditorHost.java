//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.screen;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.client.editor.KonfigStringListEditorState;
import com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
public final class KonfigEditorHost {
    private final KonfigConfigScreen screen;
    private final KonfigScreenCoordinator coordinator;

    public KonfigEditorHost(KonfigConfigScreen screen, KonfigScreenCoordinator coordinator) {
        this.screen = screen;
        this.coordinator = coordinator;
    }

    public void returnToMainScreen() {
        this.screen.returnToMainScreen();
    }

    public boolean persistEditedValue(EntryRef entry, Object previousValue) {
        if (!this.coordinator.persistEntry(entry)) {
            this.coordinator.setDraft(entry.value, previousValue);
            return false;
        }
        return true;
    }

    public boolean resetToSessionStart(EntryRef entry) {
        return this.coordinator.resetEntry(entry);
    }

    public Object storedSnapshot(ConfigValueImpl<?> value) {
        return this.coordinator.storedSnapshot(value);
    }

    public void setDraft(ConfigValueImpl<?> value, Object draft) {
        this.coordinator.setDraft(value, draft);
    }

    public int currentColor(ConfigValueImpl<?> value) {
        return this.coordinator.currentColor(value);
    }

    public List<String> registrySuggestions(ResourceKey<? extends Registry<?>> registryKey) {
        return this.coordinator.registrySuggestions(registryKey);
    }

    public KonfigStringListEditorState stringListEditorState(EntryRef entry, KonfigStringListEditorState.PersistAction persistAction) {
        return this.coordinator.stringListEditorState(entry, persistAction);
    }
}
//?}
