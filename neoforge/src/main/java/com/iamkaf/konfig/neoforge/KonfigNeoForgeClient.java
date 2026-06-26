package com.iamkaf.konfig.neoforge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = KonfigRuntime.MOD_ID, dist = Dist.CLIENT)
@ApiStatus.Internal
public final class KonfigNeoForgeClient {
    public KonfigNeoForgeClient(ModContainer container) {
        KonfigNeoForgeClientScreens.register(container, KonfigRuntime.MOD_ID);
    }
}
