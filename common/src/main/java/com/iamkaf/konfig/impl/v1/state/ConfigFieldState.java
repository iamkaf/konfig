//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.model.ConfigFieldKind;

import java.util.Objects;

@ApiStatus.Internal
public record ConfigFieldState<T>(
        String fieldId,
        ConfigFieldKind kind,
        T storedValue,
        T effectiveValue,
        T draftValue,
        Object draftInput,
        T sessionStartValue,
        boolean dirty,
        ConfigValidation validation,
        ConfigPermission permission
) {
    public ConfigFieldState {
        fieldId = requireText(fieldId, "fieldId");
        kind = Objects.requireNonNull(kind, "kind");
        storedValue = Objects.requireNonNull(storedValue, "storedValue");
        effectiveValue = Objects.requireNonNull(effectiveValue, "effectiveValue");
        draftValue = Objects.requireNonNull(draftValue, "draftValue");
        draftInput = Objects.requireNonNull(draftInput, "draftInput");
        sessionStartValue = Objects.requireNonNull(sessionStartValue, "sessionStartValue");
        validation = Objects.requireNonNull(validation, "validation");
        permission = Objects.requireNonNull(permission, "permission");
    }

    public boolean canApply() {
        return this.permission.editable() && !this.validation.hasErrors();
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
//?}
