package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
//?}
//? if <=1.20.4 {
//? if <=1.16.5 {
import net.minecraft.resources.ResourceLocation;
//?}
//?}
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

@ApiStatus.Internal
public final class KonfigFabricClient implements ClientModInitializer {
//? if <=1.20.4 {
//? if <=1.16.5 {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(KonfigRuntime.MOD_ID, "sync_snapshot");
//?} else {
    private static final net.minecraft.resources.ResourceLocation SYNC_CHANNEL = KonfigNetwork.syncSnapshotChannel();
//?}
//?}

    @Override
    public void onInitializeClient() {
        KonfigRuntime.initializeClient(FabricLoader.getInstance().getConfigDir());

//? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(KonfigNetwork.snapshotPayloadType(), (payload, context) ->
                KonfigNetwork.receiveClientSnapshot(payload)
        );
//? if >=1.21.11 {
        ClientPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.Capabilities.TYPE, (payload, context) ->
                KonfigNetwork.receiveClientCapabilities(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.Snapshot.TYPE, (payload, context) ->
                KonfigNetwork.receiveClientAuthoritySnapshot(payload)
        );
        ClientPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.EditResult.TYPE, (payload, context) ->
                KonfigNetwork.receiveClientEditResult(payload)
        );

        KonfigSync.setClientRequestSender(new KonfigSync.ClientRequestSender() {
            @Override
            public void sendHello(int protocolVersion) {
                if (ClientPlayNetworking.canSend(KonfigRemotePayloads.Hello.TYPE)) {
                    ClientPlayNetworking.send(KonfigNetwork.remoteHelloPayload(protocolVersion));
                }
            }

            @Override
            public void sendEdit(ConfigEditRequest request) {
                if (ClientPlayNetworking.canSend(KonfigRemotePayloads.EditRequest.TYPE)) {
                    ClientPlayNetworking.send(KonfigNetwork.remoteEditPayload(request));
                }
            }
        });
//?}
//?} else {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CHANNEL, (client, handler, buffer, responseSender) -> {
//? if <=1.16.5 {
            KonfigNetwork.receiveClientSnapshot(KonfigNetwork.snapshot(buffer.readUtf(256), buffer.readUtf()));
//?} else {
            KonfigNetwork.receiveClientSnapshot(KonfigNetwork.decodeSnapshot(buffer));
//?}
        });
//?}

//? if >=1.21.11 {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                KonfigSync.onClientConnected(
                        ClientPlayNetworking.canSend(KonfigRemotePayloads.Hello.TYPE)
                                && ClientPlayNetworking.canSend(KonfigRemotePayloads.EditRequest.TYPE)
                )
        );
//?}
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KonfigRuntime.clientDisconnected());
    }
}
