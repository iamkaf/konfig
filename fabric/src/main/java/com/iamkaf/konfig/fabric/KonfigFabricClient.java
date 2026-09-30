package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//? if <=1.20.4 {
import com.iamkaf.konfig.impl.v1.sync.SyncSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
//?}

@ApiStatus.Internal
public final class KonfigFabricClient implements ClientModInitializer {
//? if <=1.20.4 {
    private static final ResourceLocation SYNC_CHANNEL = KonfigNetwork.syncSnapshotChannel();
//?}

    @Override
    public void onInitializeClient() {
        KonfigRuntime.initializeClient(FabricLoader.getInstance().getConfigDir());

//? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(KonfigNetwork.snapshotPayloadType(), (payload, context) ->
                KonfigNetwork.receiveClientSnapshot(payload)
        );
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

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                KonfigSync.onClientConnected(
                        ClientPlayNetworking.canSend(KonfigRemotePayloads.Hello.TYPE)
                                && ClientPlayNetworking.canSend(KonfigRemotePayloads.EditRequest.TYPE)
                )
        );
//?} else {
        // Raw channel handlers run on the network thread: decode there, then hop to the client thread.
        ClientPlayNetworking.registerGlobalReceiver(SYNC_CHANNEL, (client, handler, buffer, responseSender) -> {
            SyncSnapshot snapshot = KonfigNetwork.decodeSnapshot(buffer);
            client.execute(() -> KonfigNetwork.receiveClientSnapshot(snapshot));
        });
        ClientPlayNetworking.registerGlobalReceiver(KonfigFabric.REMOTE_CAPABILITIES, (client, handler, buffer, responseSender) -> {
            KonfigRemotePayloads.Capabilities payload = KonfigRemotePayloads.Capabilities.read(buffer);
            client.execute(() -> KonfigNetwork.receiveClientCapabilities(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(KonfigFabric.REMOTE_SNAPSHOT, (client, handler, buffer, responseSender) -> {
            KonfigRemotePayloads.Snapshot payload = KonfigRemotePayloads.Snapshot.read(buffer);
            client.execute(() -> KonfigNetwork.receiveClientAuthoritySnapshot(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(KonfigFabric.REMOTE_RESULT, (client, handler, buffer, responseSender) -> {
            KonfigRemotePayloads.EditResult payload = KonfigRemotePayloads.EditResult.read(buffer);
            client.execute(() -> KonfigNetwork.receiveClientEditResult(payload));
        });

        KonfigSync.setClientRequestSender(new KonfigSync.ClientRequestSender() {
            @Override
            public void sendHello(int protocolVersion) {
                send(KonfigFabric.REMOTE_HELLO, KonfigNetwork.remoteHelloPayload(protocolVersion), KonfigRemotePayloads.Hello::write);
            }

            @Override
            public void sendEdit(ConfigEditRequest request) {
                send(KonfigFabric.REMOTE_EDIT, KonfigNetwork.remoteEditPayload(request), KonfigRemotePayloads.EditRequest::write);
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                KonfigSync.onClientConnected(
                        ClientPlayNetworking.canSend(KonfigFabric.REMOTE_HELLO)
                                && ClientPlayNetworking.canSend(KonfigFabric.REMOTE_EDIT)
                )
        );
//?}
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KonfigRuntime.clientDisconnected());
    }

//? if <=1.20.4 {
    private static <T> void send(ResourceLocation channel, T payload, BiConsumer<FriendlyByteBuf, T> writer) {
        if (ClientPlayNetworking.canSend(channel)) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            writer.accept(buffer, payload);
            ClientPlayNetworking.send(channel, buffer);
        }
    }
//?}
}
