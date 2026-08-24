package com.iamkaf.konfig.neoforge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
//?}
import com.iamkaf.konfig.neoforge.api.v1.KonfigNeoForgeClientScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
//? if >=1.21.11 {
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
//?}

@Mod(value = KonfigRuntime.MOD_ID, dist = Dist.CLIENT)
@ApiStatus.Internal
public final class KonfigNeoForgeClient {
    public KonfigNeoForgeClient(ModContainer container) {
        KonfigNeoForgeClientScreens.register(container, KonfigRuntime.MOD_ID);

//? if >=1.21.11 {
        KonfigSync.setClientRequestSender(new KonfigSync.ClientRequestSender() {
            @Override
            public void sendHello(int protocolVersion) {
                ClientPacketDistributor.sendToServer(KonfigNetwork.remoteHelloPayload(protocolVersion));
            }

            @Override
            public void sendEdit(ConfigEditRequest request) {
                ClientPacketDistributor.sendToServer(KonfigNetwork.remoteEditPayload(request));
            }
        });
        NeoForge.EVENT_BUS.addListener(this::onClientLogin);
        NeoForge.EVENT_BUS.addListener(this::onClientLogout);
//?}
    }

//? if >=1.21.11 {
    private void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        KonfigSync.onClientConnected(
                event.getPlayer().connection.hasChannel(KonfigRemotePayloads.Hello.TYPE)
                        && event.getPlayer().connection.hasChannel(KonfigRemotePayloads.EditRequest.TYPE)
        );
    }

    private void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        KonfigRuntime.clientDisconnected();
    }
//?}
}
