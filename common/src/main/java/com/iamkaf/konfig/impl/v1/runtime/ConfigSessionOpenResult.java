//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.runtime;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.state.ConfigSession;

import java.util.Objects;

@ApiStatus.Internal
public sealed interface ConfigSessionOpenResult permits ConfigSessionOpenResult.Opened, ConfigSessionOpenResult.UnknownConfig, ConfigSessionOpenResult.Unavailable, ConfigSessionOpenResult.Failed {
    record Opened(ConfigSession session) implements ConfigSessionOpenResult {
        public Opened {
            session = Objects.requireNonNull(session, "session");
        }
    }

    record UnknownConfig(String configId) implements ConfigSessionOpenResult {
    }

    record Unavailable(String configId, String message) implements ConfigSessionOpenResult {
    }

    record Failed(String configId, String message, Throwable cause) implements ConfigSessionOpenResult {
        public Failed {
            cause = Objects.requireNonNull(cause, "cause");
        }
    }
}
//?}
