package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.impl.v1.KonfigConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class KonfigModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KonfigConfigScreen::new;
    }
}
