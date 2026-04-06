package com.iamkaf.konfig;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.konfig.impl.v1.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.KonfigManager;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public final class KonfigDebugConfig {
    private static boolean initialized;
    private static ConfigValue<Boolean> debugEnabled;

    private KonfigDebugConfig() {
    }

    public static synchronized void bootstrap() {
        if (initialized) {
            return;
        }

        Path configPath = RuntimeEnvironment.configDirectory()
                .resolve(Constants.MOD_ID)
                .resolve("konfig.toml");
        boolean configFound = Files.exists(configPath);

        ConfigBuilder builder = Konfig.builder(Constants.MOD_ID, "konfig")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.NONE)
                .fileName("konfig.toml")
                .comment("Konfig internal debug settings.");

        builder.push("debug");
        builder.categoryComment("Verbose diagnostics for config lifecycle and screen creation.");
        debugEnabled = builder.bool("enabled", false)
                .comment("Enable verbose Konfig internal logs")
                .build();
        builder.pop();

        builder.build();
        initialized = true;

        if (enabled()) {
            if (configFound) {
                Constants.LOG.info("[Konfig/Debug] config found at {}", configPath.toAbsolutePath());
            } else {
                Constants.LOG.info("[Konfig/Debug] config not found, created defaults at {}", configPath.toAbsolutePath());
            }
        }
    }

    public static boolean enabled() {
        return debugEnabled != null && Boolean.TRUE.equals(debugEnabled.get());
    }

    public static void logRuntimeState(String reason) {
        if (!enabled()) {
            return;
        }

        Collection<ConfigHandleImpl> handles = KonfigManager.get().all();
        Constants.LOG.info(
                "[Konfig/Debug] {} | side={} | configDir={} | handles={}",
                reason,
                RuntimeEnvironment.isClient() ? "client" : "server",
                RuntimeEnvironment.configDirectory().toAbsolutePath(),
                handles.size()
        );

        for (ConfigHandleImpl handle : handles) {
            Constants.LOG.info(
                    "[Konfig/Debug] handle={} scope={} syncMode={} path={}",
                    handle.id(),
                    handle.scope(),
                    handle.syncMode(),
                    handle.path().toAbsolutePath()
            );
        }
    }
}
