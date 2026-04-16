package com.iamkaf.konfig.impl.v1;

import java.nio.file.Path;
//? if <=1.16.5 {
import java.nio.file.Paths;
//?}

public final class RuntimeEnvironment {
//? if <=1.16.5 {
    private static volatile Path configDirectory = Paths.get("config");
//?} else {
    private static volatile Path configDirectory = Path.of("config");
//?}
    private static volatile boolean client;
    private static volatile boolean initialized;

    private RuntimeEnvironment() {
    }

    public static void initialize(Path configDir, boolean isClient) {
        if (configDir != null) {
            configDirectory = configDir;
        }
        client = isClient;
        initialized = true;
    }

    public static Path configDirectory() {
        return configDirectory;
    }

    public static boolean isClient() {
        return client;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
