package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

@ApiStatus.Internal
// Forge 1.16.5 client registration shim. Screen construction stays behind the
// public bridge so old loader registration does not depend on screen internals.
final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(Constants.MOD_ID);
    }
}
