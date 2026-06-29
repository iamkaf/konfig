package com.iamkaf.konfig.impl.v1.client.toast;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class KonfigToastTimer {
    private final long displayTimeMs;
    private long lastChanged;
    private boolean changed = true;

    KonfigToastTimer(long displayTimeMs) {
        this.displayTimeMs = displayTimeMs;
    }

    void markChanged() {
        this.changed = true;
    }

    boolean isVisible(long visibleForMs, double displayTimeMultiplier) {
        if (this.changed) {
            this.lastChanged = visibleForMs;
            this.changed = false;
        }
        return visibleForMs - this.lastChanged < this.displayTimeMs * displayTimeMultiplier;
    }
}
