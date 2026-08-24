//? if >=1.21.11 {
package com.iamkaf.konfig.impl.v1.state;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@FunctionalInterface
public interface ConfigSessionObserver {
    void changed(Change change);

    enum Kind {
        DRAFT_CHANGED,
        RESET,
        ROLLED_BACK,
        APPLY_PENDING,
        APPLIED,
        AUTHORITATIVE_REFRESH,
        REJECTED,
        CLOSED
    }

    record Change(Kind kind, ConfigSessionSnapshot snapshot, ConfigChangeResult result) {
    }

    @FunctionalInterface
    interface Subscription {
        void unsubscribe();
    }
}
//?}
