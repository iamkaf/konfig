//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public sealed interface ConfigMutation permits ConfigMutation.SetDraft, ConfigMutation.ResetField, ConfigMutation.ResetAll, ConfigMutation.RestoreField, ConfigMutation.RestoreAll, ConfigMutation.Rollback {
    record SetDraft(String fieldId, Object input) implements ConfigMutation {
        public SetDraft {
            fieldId = requireFieldId(fieldId);
            input = Objects.requireNonNull(input, "input");
        }
    }

    record ResetField(String fieldId) implements ConfigMutation {
        public ResetField {
            fieldId = requireFieldId(fieldId);
        }
    }

    record ResetAll() implements ConfigMutation {
    }

    record RestoreField(String fieldId) implements ConfigMutation {
        public RestoreField {
            fieldId = requireFieldId(fieldId);
        }
    }

    record RestoreAll() implements ConfigMutation {
    }

    record Rollback() implements ConfigMutation {
    }

    private static String requireFieldId(String fieldId) {
        String normalized = Objects.requireNonNull(fieldId, "fieldId").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("fieldId cannot be blank");
        }
        return normalized;
    }
}
//?}
