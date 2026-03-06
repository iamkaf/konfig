package com.iamkaf.konfig.neoforge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.impl.v1.KonfigConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public final class KonfigNeoForgeClient {
    public KonfigNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> new KonfigConfigScreen(parent));
    }
}
