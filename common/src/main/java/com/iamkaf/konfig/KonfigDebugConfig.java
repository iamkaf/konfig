package com.iamkaf.konfig;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.konfig.api.v1.ImageOptions;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.SyncMode;
import com.iamkaf.konfig.impl.v1.ConfigHandleImpl;
import com.iamkaf.konfig.impl.v1.KonfigManager;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@ApiStatus.Internal
public final class KonfigDebugConfig {
    private static boolean initialized;
    private static ConfigValue<Boolean> debugEnabled;
    private static ConfigValue<String> debugMode;

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
                .comment("Konfig internal debug settings.")
                .info(info -> info
                        .image(Constants.resource("gui/debug_image"), "Konfig", ImageOptions.banner(180, 84))
                        .header("About Konfig")
                        .inlineText("Konfig provides generated config screens and synchronized config values for Minecraft mods.")
                        .url("Konfig Documentation", "https://github.com/iamkaf/konfig"));

        builder.header("Konfig Debug Settings");
        builder.image(Constants.resource("gui/debug_image"));
        builder.inlineText("These entries exist to test Konfig's own screen and diagnostics.");
        builder.url("Konfig Documentation", "https://github.com/iamkaf/konfig");

        builder.push("debug");
        builder.categoryComment("Verbose diagnostics for config lifecycle and screen creation.");
        builder.categoryInfo(info -> info
                .image(Constants.resource("gui/debug_image"), "Konfig", ImageOptions.banner(180, 84))
                .header("About Konfig Debug Settings")
                .inlineText("These settings control Konfig's debug logging and diagnostics.")
                .url("Konfig Documentation", "https://github.com/iamkaf/konfig"));
        debugMode = builder.dropdown("mode", "standard", options -> options
                        .option("quiet", "Quiet", option -> option
                                .tooltip("Only critical Konfig diagnostics.")
                                .info(info -> info
                                        .header("Quiet")
                                        .inlineText("Keeps Konfig quiet unless something needs attention.")))
                        .option("standard", "Standard", option -> option
                                .tooltip("Balanced Konfig diagnostics.")
                                .info(info -> info
                                        .header("Standard")
                                        .inlineText("Shows the normal amount of diagnostic context while testing generated screens.")))
                        .option("verbose", "Verbose", option -> option
                                .tooltip("Detailed Konfig lifecycle and screen diagnostics.")
                                .info(info -> info
                                        .header("Verbose")
                                        .inlineText("Adds detailed lifecycle and generated-screen diagnostics for development."))))
                .comment("Controls the amount of Konfig diagnostic detail shown in generated debug surfaces.")
                .info(info -> info
                        .header("Debug Mode")
                        .inlineText("Changes how much diagnostic context Konfig exposes while testing generated screens.")
                        .inlineText("Default: Standard"))
                .build();
        debugEnabled = builder.bool("enabled", false)
                .comment("Enable verbose Konfig internal logs")
                .info(info -> info
                        .header("Enable Debug Logging")
                        .inlineText("When enabled, Konfig will write detailed diagnostic and debug information to the log files.")
                        .inlineText("This can help diagnose issues but may produce a lot of log output.")
                        .inlineText("Default: OFF"))
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
