package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
//?}

@ApiStatus.Internal
public final class KonfigFabric implements ModInitializer {
//? if <=1.20.4 {
    private static final ResourceLocation SYNC_CHANNEL = KonfigNetwork.syncSnapshotChannel();
    static final ResourceLocation REMOTE_HELLO = KonfigRuntime.resource(KonfigRemotePayloads.Hello.PATH);
    static final ResourceLocation REMOTE_CAPABILITIES = KonfigRuntime.resource(KonfigRemotePayloads.Capabilities.PATH);
    static final ResourceLocation REMOTE_SNAPSHOT = KonfigRuntime.resource(KonfigRemotePayloads.Snapshot.PATH);
    static final ResourceLocation REMOTE_EDIT = KonfigRuntime.resource(KonfigRemotePayloads.EditRequest.PATH);
    static final ResourceLocation REMOTE_RESULT = KonfigRuntime.resource(KonfigRemotePayloads.EditResult.PATH);
//?}

    @Override
    public void onInitialize() {
        KonfigRuntime.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
        );

//? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(KonfigNetwork.snapshotPayloadType(), KonfigNetwork.snapshotPayloadCodec());
        PayloadTypeRegistry.serverboundPlay().register(KonfigRemotePayloads.Hello.TYPE, KonfigRemotePayloads.Hello.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(KonfigRemotePayloads.EditRequest.TYPE, KonfigRemotePayloads.EditRequest.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KonfigRemotePayloads.Capabilities.TYPE, KonfigRemotePayloads.Capabilities.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KonfigRemotePayloads.Snapshot.TYPE, KonfigRemotePayloads.Snapshot.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KonfigRemotePayloads.EditResult.TYPE, KonfigRemotePayloads.EditResult.STREAM_CODEC);
//?} elif >=1.20.5 {
        PayloadTypeRegistry.playS2C().register(KonfigNetwork.snapshotPayloadType(), KonfigNetwork.snapshotPayloadCodec());
        PayloadTypeRegistry.playC2S().register(KonfigRemotePayloads.Hello.TYPE, KonfigRemotePayloads.Hello.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(KonfigRemotePayloads.EditRequest.TYPE, KonfigRemotePayloads.EditRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KonfigRemotePayloads.Capabilities.TYPE, KonfigRemotePayloads.Capabilities.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KonfigRemotePayloads.Snapshot.TYPE, KonfigRemotePayloads.Snapshot.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KonfigRemotePayloads.EditResult.TYPE, KonfigRemotePayloads.EditResult.STREAM_CODEC);
//?}

//? if >=1.20.5 {
        ServerPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.Hello.TYPE, (payload, context) ->
                onHello(context.player(), payload)
        );
        ServerPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.EditRequest.TYPE, (payload, context) ->
                onEdit(context.player(), payload)
        );

        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(ServerPlayer player, ConfigEditCapabilities capabilities) {
                if (ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Capabilities.TYPE)) {
                    ServerPlayNetworking.send(player, KonfigNetwork.remoteCapabilitiesPayload(capabilities));
                }
            }

            @Override
            public void sendSnapshot(ServerPlayer player, ConfigEditSnapshot snapshot) {
                if (ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Snapshot.TYPE)) {
                    ServerPlayNetworking.send(player, KonfigNetwork.remoteSnapshotPayload(snapshot));
                }
            }

            @Override
            public void sendResult(ServerPlayer player, ConfigEditResult result) {
                if (ServerPlayNetworking.canSend(player, KonfigRemotePayloads.EditResult.TYPE)) {
                    ServerPlayNetworking.send(player, KonfigNetwork.remoteResultPayload(result));
                }
            }
        });

        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                ServerPlayNetworking.send(player, KonfigNetwork.snapshotPayload(configId, jsonPayload))
        );
//?} else {
        // Raw channel handlers run on the network thread: decode there, then hop to the server thread.
        ServerPlayNetworking.registerGlobalReceiver(REMOTE_HELLO, (server, player, handler, buffer, responseSender) -> {
            KonfigRemotePayloads.Hello payload = KonfigRemotePayloads.Hello.read(buffer);
            server.execute(() -> onHello(player, payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(REMOTE_EDIT, (server, player, handler, buffer, responseSender) -> {
            KonfigRemotePayloads.EditRequest payload = KonfigRemotePayloads.EditRequest.read(buffer);
            server.execute(() -> onEdit(player, payload));
        });

        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(ServerPlayer player, ConfigEditCapabilities capabilities) {
                send(player, REMOTE_CAPABILITIES, KonfigNetwork.remoteCapabilitiesPayload(capabilities), KonfigRemotePayloads.Capabilities::write);
            }

            @Override
            public void sendSnapshot(ServerPlayer player, ConfigEditSnapshot snapshot) {
                send(player, REMOTE_SNAPSHOT, KonfigNetwork.remoteSnapshotPayload(snapshot), KonfigRemotePayloads.Snapshot::write);
            }

            @Override
            public void sendResult(ServerPlayer player, ConfigEditResult result) {
                send(player, REMOTE_RESULT, KonfigNetwork.remoteResultPayload(result), KonfigRemotePayloads.EditResult::write);
            }
        });

        KonfigRuntime.setSyncSender((player, configId, jsonPayload) -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            KonfigNetwork.encodeSnapshot(KonfigNetwork.snapshot(configId, jsonPayload), buffer);
            ServerPlayNetworking.send(player, SYNC_CHANNEL, buffer);
        });
//?}

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KonfigRuntime.playerJoined(handler.player)
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                KonfigRuntime.playerLeft(handler.player)
        );
    }

    private static void onHello(ServerPlayer player, KonfigRemotePayloads.Hello payload) {
        if (supportsRemoteResponses(player)) {
            KonfigSync.onClientHello(player, payload.protocolVersion(), KonfigNetwork.canEdit(player));
        }
    }

    private static void onEdit(ServerPlayer player, KonfigRemotePayloads.EditRequest payload) {
        KonfigSync.onRemoteEdit(player, KonfigNetwork.canEdit(player), KonfigNetwork.editRequest(payload));
    }

    private static boolean supportsRemoteResponses(ServerPlayer player) {
//? if >=1.20.5 {
        return ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Capabilities.TYPE)
                && ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Snapshot.TYPE)
                && ServerPlayNetworking.canSend(player, KonfigRemotePayloads.EditResult.TYPE);
//?} else {
        return ServerPlayNetworking.canSend(player, REMOTE_CAPABILITIES)
                && ServerPlayNetworking.canSend(player, REMOTE_SNAPSHOT)
                && ServerPlayNetworking.canSend(player, REMOTE_RESULT);
//?}
    }

//? if <=1.20.4 {
    private static <T> void send(ServerPlayer player, ResourceLocation channel, T payload, BiConsumer<FriendlyByteBuf, T> writer) {
        if (ServerPlayNetworking.canSend(player, channel)) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            writer.accept(buffer, payload);
            ServerPlayNetworking.send(player, channel, buffer);
        }
    }
//?}
}
