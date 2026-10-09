package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;
import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//? if <=1.21.5 {
import net.minecraftforge.common.MinecraftForge;
//?}

@ApiStatus.Internal
final class KonfigForgeClient {
    private KonfigForgeClient() {
    }

    static void init() {
        KonfigForgeClientScreens.register(KonfigRuntime.MOD_ID);
        KonfigSync.setClientRequestSender(new KonfigSync.ClientRequestSender() {
            @Override
            public void sendHello(int protocolVersion) {
                if (connected()) {
                    KonfigForge.sendRemoteHello(protocolVersion);
                }
            }

            @Override
            public void sendEdit(ConfigEditRequest request) {
                if (connected()) {
                    KonfigForge.sendRemoteEdit(request);
                }
            }
        });
//? if >=1.21.6 {
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event ->
                KonfigSync.onClientConnected(KonfigForge.supportsRemoteEditing(event.getConnection()))
        );
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> KonfigRuntime.clientDisconnected());
//?} elif >=1.19 {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) ->
                KonfigSync.onClientConnected(KonfigForge.supportsRemoteEditing(event.getConnection()))
        );
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> KonfigRuntime.clientDisconnected());
//?} elif >=1.18 {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedInEvent event) ->
                KonfigSync.onClientConnected(KonfigForge.supportsRemoteEditing(event.getConnection()))
        );
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedOutEvent event) -> KonfigRuntime.clientDisconnected());
//?} else {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedInEvent event) ->
                KonfigSync.onClientConnected(KonfigForge.supportsRemoteEditing(event.getNetworkManager()))
        );
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedOutEvent event) -> KonfigRuntime.clientDisconnected());
//?}
    }

    // A config screen opened from the title screen has no server to talk to.
    private static boolean connected() {
        return Minecraft.getInstance().getConnection() != null;
    }
}
