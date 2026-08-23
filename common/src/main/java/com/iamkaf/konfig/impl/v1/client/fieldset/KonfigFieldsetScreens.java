//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.fieldset.FieldsetValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import java.util.List;

@ApiStatus.Internal
public final class KonfigFieldsetScreens {
    private KonfigFieldsetScreens() {
    }

    public static KonfigFieldsetRowSummary summary(Component label, FieldsetValue value, boolean editable) {
        KonfigFieldsetAccess access = editable
                ? KonfigFieldsetAccess.editable()
                : KonfigFieldsetAccess.remoteReadOnly();
        KonfigFieldsetDraftSession session = new KonfigFieldsetDraftSession(value);
        return KonfigFieldsetRowSummary.create(label, new KonfigFieldsetDraftAdapter(session, access));
    }

    public static Screen create(
            Screen parent,
            Component title,
            Component context,
            FieldsetValue value,
            boolean editable,
            RegistrySuggestions registrySuggestions,
            PersistAction persistAction
    ) {
        KonfigFieldsetAccess access = editable
                ? KonfigFieldsetAccess.editable()
                : KonfigFieldsetAccess.remoteReadOnly();
        return create(parent, title, context, value, access, registrySuggestions, persistAction);
    }

    public static Screen create(
            Screen parent,
            Component title,
            Component context,
            FieldsetValue value,
            KonfigFieldsetAccess access,
            RegistrySuggestions registrySuggestions,
            PersistAction persistAction
    ) {
        KonfigFieldsetDraftSession session = new KonfigFieldsetDraftSession(value);
        KonfigFieldsetDraftAdapter adapter = new KonfigFieldsetDraftAdapter(session, access);
        return new KonfigFieldsetListScreen(
                parent,
                title,
                context,
                session,
                adapter,
                registrySuggestions,
                persistAction
        );
    }

    @FunctionalInterface
    public interface RegistrySuggestions {
        List<String> find(ResourceKey<? extends Registry<?>> registryKey, String query, int limit);

        static RegistrySuggestions none() {
            return (registryKey, query, limit) -> List.of();
        }
    }

    @FunctionalInterface
    public interface PersistAction {
        KonfigFieldsetEditResult persist(FieldsetValue previousValue, FieldsetValue newValue);
    }
}
//?}
