//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

@ApiStatus.Internal
public interface KonfigFieldsetUiAdapter<E, F> {
    List<E> entries();

    String entryId(E entry);

    Component entryLabel(E entry);

    Component entrySummary(E entry);

    default List<String> entrySearchTerms(E entry) {
        return List.of();
    }

    default boolean matches(E entry, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        if (this.entryLabel(entry).getString().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || this.entrySummary(entry).getString().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            return true;
        }
        for (String term : this.entrySearchTerms(entry)) {
            if (term != null && term.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    KonfigFieldsetAccess fieldsetAccess();

    KonfigFieldsetAccess entryAccess(E entry);

    E createEntry();

    E duplicateEntry(E entry);

    KonfigFieldsetEditResult replaceEntries(List<E> entries);

    List<F> fields(E entry);

    Component fieldLabel(F field);

    Component fieldDescription(F field);

    String fieldPath(F field);

    KonfigFieldsetValueBinding<Object> bind(E entry, F field);

    KonfigFieldsetValidation validation();
}
//?}
