package com.iamkaf.konfig;

import com.iamkaf.konfig.api.v1.ConfigBuilder;
import com.iamkaf.konfig.api.v1.ConfigScope;
import com.iamkaf.konfig.api.v1.Konfig;
import com.iamkaf.konfig.api.v1.RestartRequirement;
import com.iamkaf.konfig.api.v1.SyncMode;

import java.util.List;

public final class KonfigShowcaseConfig {
    private static boolean initialized;

    private KonfigShowcaseConfig() {
    }

    public static synchronized void bootstrap() {
        if (initialized) {
            return;
        }

        bootstrapCommon();
        bootstrapClient();
        initialized = true;
    }

    private static void bootstrapCommon() {
        ConfigBuilder builder = Konfig.builder(Constants.MOD_ID, "showcase")
                .scope(ConfigScope.COMMON)
                .syncMode(SyncMode.LOGIN_AND_RELOAD)
                .schemaVersion(1)
                .fileName("showcase-common.toml")
                .comment(
                        "Konfig showcase common config.\n" +
                                "This file exists to exercise realistic config structure, comments and sync.\n" +
                                "The values are illustrative and do not change Konfig's own runtime behavior."
                );

        builder.push("general");
        builder.categoryComment("High-level behavior toggles that a real consumer mod might expose.");
        builder.bool("show_tutorial_tips", true)
                .comment("Show first-run helper text for new players.")
                .build();
        builder.string("profile_name", "standard", 3, 24)
                .comment("Logical profile name used when exporting presets or logs.")
                .build();
        builder.enumValue("sort_mode", SortMode.BALANCED)
                .comment("Preferred ordering strategy for results presented to the player.")
                .sync(true)
                .build();
        builder.pop();

        builder.push("mining");
        builder.categoryComment("Shared mining and machine behavior that would normally live on the server.");

        builder.push("scanner");
        builder.categoryComment("Scanner timing and reach. These are good examples of synced gameplay values.");
        builder.intRange("scan_radius", 8, 1, 32)
                .comment("Maximum number of blocks the scanner checks in one pass.")
                .sync(true)
                .build();
        builder.intRange("scan_cooldown_ticks", 12, 0, 100)
                .comment("Delay between consecutive scanner pulses.")
                .sync(true)
                .build();
        builder.doubleRange("break_speed_multiplier", 1.15D, 0.10D, 4.00D)
                .comment("Multiplier applied after the scanner identifies a valid target.")
                .sync(true)
                .build();
        builder.pop();

        builder.push("power");
        builder.categoryComment("Long values are included here because large energy or fluid numbers are common in mods.");
        builder.longRange("starting_charge", 12000L, 0L, 500000L)
                .comment("Starting internal charge for a freshly placed machine.")
                .sync(true)
                .build();
        builder.longRange("max_charge_buffer", 48000L, 0L, 5000000L)
                .comment("Maximum stored charge before the machine begins throttling input.")
                .sync(true)
                .build();
        builder.pop();
        builder.pop();

        builder.push("world");
        builder.categoryComment("World-facing values that typically require a world restart after changes.");
        builder.doubleRange("resource_density", 0.85D, 0.10D, 3.00D)
                .comment("Relative richness of generated resource clusters.")
                .restart(RestartRequirement.WORLD)
                .sync(true)
                .build();
        builder.intRange("structure_weight", 12, 0, 100)
                .comment("Relative spawn weight for showcase structures in test worlds.")
                .restart(RestartRequirement.WORLD)
                .sync(true)
                .build();
        builder.stringList("dimension_allowlist", List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"))
                .comment("Dimensions where the showcase mechanics should be active.")
                .restart(RestartRequirement.WORLD)
                .sync(true)
                .build();
        builder.pop();

        builder.push("network");
        builder.categoryComment("Network-facing knobs that would normally matter for multiplayer polish.");
        builder.bool("allow_remote_access", true)
                .comment("Allow remote terminals to request server-side data from the machine.")
                .sync(true)
                .build();
        builder.intRange("sync_interval_ticks", 40, 5, 600)
                .comment("How often authoritative server data is resent to clients.")
                .sync(true)
                .build();
        builder.enumValue("sync_policy", SyncPolicy.BALANCED)
                .comment("Trade-off between immediate updates and network traffic.")
                .sync(true)
                .build();
        builder.pop();

        builder.build();
    }

    private static void bootstrapClient() {
        ConfigBuilder builder = Konfig.builder(Constants.MOD_ID, "showcase_client")
                .scope(ConfigScope.CLIENT)
                .syncMode(SyncMode.NONE)
                .schemaVersion(1)
                .fileName("showcase-client.toml")
                .comment(
                        "Konfig showcase client config.\n" +
                                "This file is intentionally broad so the config screen can be inspected visually.\n" +
                                "The values are illustrative and do not change Konfig's own runtime behavior."
                );

        builder.push("screen");
        builder.categoryComment("Purely local presentation options for a hypothetical consumer mod.");

        builder.push("layout");
        builder.categoryComment("Layout values are useful for testing pagination and number input fields.");
        builder.bool("compact_rows", false)
                .comment("Prefer denser rows when a large number of entries are visible at once.")
                .clientOnly()
                .build();
        builder.intRange("page_size", 8, 4, 12)
                .comment("Preferred number of rows shown by the consumer mod on one page.")
                .clientOnly()
                .build();
        builder.intRange("row_spacing", 2, 0, 8)
                .comment("Extra vertical spacing inserted between adjacent rows.")
                .clientOnly()
                .build();
        builder.pop();

        builder.push("appearance");
        builder.categoryComment("Visual tuning values with a mix of booleans, doubles and enums.");
        builder.enumValue("accent_preset", AccentPreset.AMBER)
                .comment("Color preset used by the consumer mod for its overlays and highlights.")
                .clientOnly()
                .build();
        builder.colorRgb("accent_color", 0xE1A74A)
                .comment("Primary accent color used for highlights, borders and emphasis text.")
                .clientOnly()
                .build();
        builder.colorArgb("overlay_shadow", 0xCC162029)
                .comment("Shadow tint drawn behind translucent overlay panels.")
                .clientOnly()
                .build();
        builder.doubleRange("panel_opacity", 0.82D, 0.30D, 1.00D)
                .comment("Opacity of overlay panels drawn over the game world.")
                .clientOnly()
                .build();
        builder.bool("reduce_motion", false)
                .comment("Reduce non-essential UI motion for players who prefer calmer screens.")
                .clientOnly()
                .build();
        builder.pop();
        builder.pop();

        builder.push("hud");
        builder.categoryComment("A realistic HUD section with position, visibility and text formatting values.");
        builder.bool("show_status_hud", true)
                .comment("Show a compact HUD card while the feature is active.")
                .clientOnly()
                .build();
        builder.enumValue("hud_anchor", HudAnchor.NORTHEAST)
                .comment("Corner of the screen used for the status HUD.")
                .clientOnly()
                .build();
        builder.intRange("notification_limit", 4, 0, 10)
                .comment("Maximum number of queued status messages shown at one time.")
                .clientOnly()
                .build();
        builder.string("status_template", "Mining: {vein}", 3, 48)
                .comment("Template string used when rendering short status updates.")
                .clientOnly()
                .build();
        builder.stringList("status_examples", List.of("Scanner ready", "Charge stable", "Ore detected"))
                .comment("Example status messages used when previewing HUD spacing and truncation.")
                .clientOnly()
                .build();
        builder.pop();

        builder.push("accessibility");
        builder.categoryComment("Accessibility settings are essential in real mods and useful for visual verification.");
        builder.bool("high_contrast_labels", false)
                .comment("Use brighter label colors for improved readability.")
                .clientOnly()
                .build();
        builder.enumValue("tooltip_detail", TooltipDetail.STANDARD)
                .comment("How much explanatory text should be shown in local tooltips.")
                .clientOnly()
                .build();
        builder.pop();

        builder.build();
    }

    private enum SortMode {
        STABLE,
        BALANCED,
        AGGRESSIVE
    }

    private enum SyncPolicy {
        CAUTIOUS,
        BALANCED,
        EAGER
    }

    private enum AccentPreset {
        SLATE,
        AMBER,
        MOSS,
        CORAL
    }

    private enum HudAnchor {
        NORTHWEST,
        NORTHEAST,
        SOUTHWEST,
        SOUTHEAST
    }

    private enum TooltipDetail {
        BRIEF,
        STANDARD,
        DETAILED
    }
}
