//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.impl.v1.state.ConfigPermission;

import java.util.Objects;

@ApiStatus.Internal
public final class ConfigScopeRules {
    public static final int REMOTE_EDIT_PERMISSION_LEVEL = 2;

    public boolean loadsOn(ConfigScope scope, ConfigRuntimeContext context) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(context, "context");
        return switch (scope) {
            case CLIENT -> context.side() == ConfigRuntimeContext.LogicalSide.CLIENT;
            case COMMON -> true;
            case SERVER -> context.side() == ConfigRuntimeContext.LogicalSide.SERVER;
        };
    }

    public boolean visible(ConfigScope scope, boolean synchronizedValue, ConfigRuntimeContext context) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(context, "context");
        if (loadsOn(scope, context)) {
            return true;
        }
        return context.connectedToRemoteServer() && synchronizedValue
                && (scope == ConfigScope.COMMON || scope == ConfigScope.SERVER);
    }

    public ConfigPermission permission(
            ConfigScope scope,
            boolean synchronizedValue,
            ConfigRuntimeContext context
    ) {
        if (!visible(scope, synchronizedValue, context)) {
            return ConfigPermission.hidden(
                    ConfigPermission.Reason.LOCAL_SIDE_ONLY,
                    "This value belongs to the other logical side"
            );
        }
        if (context.modpackLocked()) {
            return ConfigPermission.readOnly(
                    ConfigPermission.Reason.MODPACK_LOCKED,
                    "This config is locked by the modpack"
            );
        }
        if (!context.connectedToRemoteServer()) {
            return ConfigPermission.editablePermission();
        }
        if (scope == ConfigScope.CLIENT || !synchronizedValue) {
            return ConfigPermission.editablePermission();
        }
        if (!context.peerSupportsWrites()) {
            return ConfigPermission.readOnly(
                    ConfigPermission.Reason.UNSUPPORTED_PEER,
                    "The server does not support remote config changes"
            );
        }
        if (context.permissionLevel() < REMOTE_EDIT_PERMISSION_LEVEL) {
            return ConfigPermission.readOnly(
                    ConfigPermission.Reason.OPERATOR_REQUIRED,
                    "Operator permission level 2 is required"
            );
        }
        return ConfigPermission.editablePermission();
    }
}
//?}
