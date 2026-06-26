package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

@ApiStatus.Internal
final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(Constants.MOD_ID);
    }
}
