package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(Constants.MOD_ID);
    }
}
