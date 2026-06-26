package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

@ApiStatus.Internal
final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(KonfigRuntime.MOD_ID);
    }
}
