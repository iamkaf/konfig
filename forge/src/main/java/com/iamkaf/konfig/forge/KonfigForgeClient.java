package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//?}
import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;

@ApiStatus.Internal
final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(KonfigRuntime.MOD_ID);
//? if >=1.21.11 {
        KonfigSync.setClientRequestSender(new KonfigSync.ClientRequestSender() {
            @Override
            public void sendHello(int protocolVersion) {
                KonfigForge.sendRemoteHello(protocolVersion);
            }

            @Override
            public void sendEdit(ConfigEditRequest request) {
                KonfigForge.sendRemoteEdit(request);
            }
        });
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event ->
                KonfigSync.onClientConnected(KonfigForge.supportsRemoteEditing(event.getConnection()))
        );
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> KonfigRuntime.clientDisconnected());
//?}
    }
}
