//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.client.fieldset;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.chat.Component;

import java.util.Objects;

@ApiStatus.Internal
public record KonfigFieldsetRowSummary(
        Component label,
        int entryCount,
        Component countText,
        int errorCount,
        int warningCount,
        Component validationText,
        boolean readOnly,
        Component accessText
) {
    public KonfigFieldsetRowSummary {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(countText, "countText");
        Objects.requireNonNull(validationText, "validationText");
        Objects.requireNonNull(accessText, "accessText");
    }

    public static KonfigFieldsetRowSummary create(Component label, KonfigFieldsetUiAdapter<?, ?> adapter) {
        int entryCount = adapter.entries().size();
        KonfigFieldsetValidation validation = adapter.validation();
        KonfigFieldsetAccess access = adapter.fieldsetAccess();
        return new KonfigFieldsetRowSummary(
                label,
                entryCount,
                Component.translatable("konfig.screen.list.count", Integer.valueOf(entryCount)),
                validation.errorCount(),
                validation.warningCount(),
                validation.summary(),
                access.isReadOnly(),
                access.reason()
        );
    }
}
//?}
