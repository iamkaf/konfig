package com.iamkaf.konfig.api.v1;

public interface ConfigListener {
    default void onLoad(ConfigHandle handle) {
    }

    default void onReload(ConfigHandle handle, ReloadCause cause) {
    }

    default void onUnload(ConfigHandle handle) {
    }
}
