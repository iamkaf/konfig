//? if >=1.17 {
// Modern config-screen stack only: 1.16.x keeps legacy loader-specific screens,
// so these shared UI internals begin at the 1.17 client API baseline.
package com.iamkaf.konfig.impl.v1.client.screen;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class KonfigScreenMetrics {
    public static final int LIST_TOP = 28;
    public static final int LIST_BOTTOM_MARGIN = 52;
    public static final int ROW_HEIGHT = 34;
    public static final int CONTROL_HEIGHT = 20;
    public static final int CONTROL_MIN_WIDTH = 132;
    public static final int CONTROL_MAX_WIDTH = 200;
    public static final int VALIDATION_COLOR = 0xFFFF8080;
    public static final int URL_BUTTON_WIDTH = 60;
    public static final int SUGGESTION_LIMIT = 7;
    public static final int SUGGESTION_ROW_HEIGHT = 14;
    public static final int DROPDOWN_CHEVRON_WIDTH = 16;
    public static final long DROPDOWN_TYPE_SELECT_RESET_MS = 1000L;

    private KonfigScreenMetrics() {
    }
}
//?}
