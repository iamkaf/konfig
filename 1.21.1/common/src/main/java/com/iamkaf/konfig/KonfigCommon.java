package com.iamkaf.konfig;

public final class KonfigCommon {
    private static boolean initialized;

    private KonfigCommon() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        KonfigDebugConfig.bootstrap();
        KonfigShowcaseConfig.bootstrap();

        initialized = true;
        if (KonfigDebugConfig.enabled()) {
            Constants.LOG.info("Konfig initialized with debug mode enabled.");
            KonfigDebugConfig.logRuntimeState("startup");
        } else {
            Constants.LOG.info("Konfig initialized.");
        }
    }
}
