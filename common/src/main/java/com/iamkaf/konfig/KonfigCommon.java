package com.iamkaf.konfig;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class KonfigCommon {
    private static boolean initialized;

    private KonfigCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        KonfigDebugConfig.bootstrap();

        initialized = true;
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info("Konfig initialized with debug mode enabled.");
            KonfigDebugConfig.logRuntimeState("startup");
        } else {
            Constants.LOG.info("Konfig initialized.");
        }
    }
}
