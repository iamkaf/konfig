//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public record ConfigPermission(boolean visible, boolean editable, Reason reason, String message) {
    public ConfigPermission {
        reason = Objects.requireNonNull(reason, "reason");
        message = message == null ? "" : message.trim();
        if (editable && !visible) {
            throw new IllegalArgumentException("An editable field must be visible");
        }
        if (editable && reason != Reason.EDITABLE) {
            throw new IllegalArgumentException("Editable permission must use the EDITABLE reason");
        }
        if (!editable && reason == Reason.EDITABLE) {
            throw new IllegalArgumentException("Read-only permission needs a read-only reason");
        }
    }

    public static ConfigPermission editablePermission() {
        return new ConfigPermission(true, true, Reason.EDITABLE, "");
    }

    public static ConfigPermission readOnly(Reason reason, String message) {
        return new ConfigPermission(true, false, reason, message);
    }

    public static ConfigPermission hidden(Reason reason, String message) {
        return new ConfigPermission(false, false, reason, message);
    }

    public enum Reason {
        EDITABLE,
        LOCAL_SIDE_ONLY,
        SERVER_AUTHORITY,
        OPERATOR_REQUIRED,
        UNSUPPORTED_PEER,
        MODPACK_LOCKED,
        DEPENDENCY_DISABLED,
        RUNTIME_READ_ONLY
    }
}
//?}
