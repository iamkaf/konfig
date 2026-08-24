//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public record ConfigRuntimeContext(
        LogicalSide side,
        boolean connectedToRemoteServer,
        int permissionLevel,
        boolean peerSupportsWrites,
        boolean modpackLocked
) {
    public ConfigRuntimeContext {
        side = Objects.requireNonNull(side, "side");
        if (permissionLevel < 0) {
            throw new IllegalArgumentException("permissionLevel must be non-negative");
        }
        if (connectedToRemoteServer && side != LogicalSide.CLIENT) {
            throw new IllegalArgumentException("Only a client context can be connected to a remote server");
        }
    }

    public static ConfigRuntimeContext localClient() {
        return new ConfigRuntimeContext(LogicalSide.CLIENT, false, 0, false, false);
    }

    public static ConfigRuntimeContext server() {
        return new ConfigRuntimeContext(LogicalSide.SERVER, false, 4, true, false);
    }

    public static ConfigRuntimeContext remoteClient(int permissionLevel, boolean peerSupportsWrites) {
        return new ConfigRuntimeContext(LogicalSide.CLIENT, true, permissionLevel, peerSupportsWrites, false);
    }

    public enum LogicalSide {
        CLIENT,
        SERVER
    }
}
//?}
