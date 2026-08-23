package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.minecraft.server.permissions.Permissions;
//?}
//? if <=1.20.4 {
import io.netty.buffer.Unpooled;
//?}
import net.fabricmc.api.ModInitializer;
//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//? if <=1.20.4 {
import net.minecraft.network.FriendlyByteBuf;
//? if <=1.16.5 {
import net.minecraft.resources.ResourceLocation;
//?}
//?}

@ApiStatus.Internal
public final class KonfigFabric implements ModInitializer {
//? if <=1.20.4 {
//? if <=1.16.5 {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(KonfigRuntime.MOD_ID, "sync_snapshot");
//?} else {
    private static final net.minecraft.resources.ResourceLocation SYNC_CHANNEL = KonfigNetwork.syncSnapshotChannel();
//?}
//?}

    @Override
    public void onInitialize() {
        KonfigRuntime.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
        );

//? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(KonfigNetwork.snapshotPayloadType(), KonfigNetwork.snapshotPayloadCodec());
//?} elif >=1.20.5 {
        PayloadTypeRegistry.playS2C().register(KonfigNetwork.snapshotPayloadType(), KonfigNetwork.snapshotPayloadCodec());
//?}

//? if >=26.1 {
        PayloadTypeRegistry.serverboundPlay().register(KonfigRemotePayloads.Hello.TYPE, KonfigRemotePayloads.Hello.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(KonfigRemotePayloads.EditRequest.TYPE, KonfigRemotePayloads.EditRequest.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KonfigRemotePayloads.Capabilities.TYPE, KonfigRemotePayloads.Capabilities.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KonfigRemotePayloads.Snapshot.TYPE, KonfigRemotePayloads.Snapshot.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KonfigRemotePayloads.EditResult.TYPE, KonfigRemotePayloads.EditResult.STREAM_CODEC);
//?} elif >=1.21.11 {
        PayloadTypeRegistry.playC2S().register(KonfigRemotePayloads.Hello.TYPE, KonfigRemotePayloads.Hello.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(KonfigRemotePayloads.EditRequest.TYPE, KonfigRemotePayloads.EditRequest.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KonfigRemotePayloads.Capabilities.TYPE, KonfigRemotePayloads.Capabilities.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KonfigRemotePayloads.Snapshot.TYPE, KonfigRemotePayloads.Snapshot.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KonfigRemotePayloads.EditResult.TYPE, KonfigRemotePayloads.EditResult.STREAM_CODEC);
//?}

//? if >=1.21.11 {
        ServerPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.Hello.TYPE, (payload, context) -> {
            if (!supportsRemoteResponses(context.player())) {
                return;
            }
            KonfigSync.onClientHello(context.player(), payload.protocolVersion(), canEdit(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(KonfigRemotePayloads.EditRequest.TYPE, (payload, context) ->
                KonfigSync.onRemoteEdit(
                        context.player(),
                        canEdit(context.player()),
                        KonfigNetwork.editRequest(payload)
                )
        );

        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(net.minecraft.server.level.ServerPlayer player, ConfigEditCapabilities capabilities) {
                if (ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Capabilities.TYPE)) {
                    ServerPlayNetworking.send(player, KonfigNetwork.remoteCapabilitiesPayload(capabilities));
                }
            }

            @Override
            public void sendSnapshot(net.minecraft.server.level.ServerPlayer player, ConfigEditSnapshot snapshot) {
                if (ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Snapshot.TYPE)) {
                    ServerPlayNetworking.send(player, KonfigNetwork.remoteSnapshotPayload(snapshot));
                }
            }

            @Override
            public void sendResult(net.minecraft.server.level.ServerPlayer player, ConfigEditResult result) {
                if (ServerPlayNetworking.canSend(player, KonfigRemotePayloads.EditResult.TYPE)) {
                    ServerPlayNetworking.send(player, KonfigNetwork.remoteResultPayload(result));
                }
            }
        });
//?}

//? if >=1.20.5 {
        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                ServerPlayNetworking.send(player, KonfigNetwork.snapshotPayload(configId, jsonPayload))
        );
//?} else {
        KonfigRuntime.setSyncSender((player, configId, jsonPayload) -> {
            net.minecraft.server.level.ServerPlayer serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
//? if <=1.16.5 {
            buffer.writeUtf(configId, 256);
            buffer.writeUtf(jsonPayload);
//?} else {
            KonfigNetwork.encodeSnapshot(KonfigNetwork.snapshot(configId, jsonPayload), buffer);
//?}
            ServerPlayNetworking.send(serverPlayer, SYNC_CHANNEL, buffer);
        });
//?}

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KonfigRuntime.playerJoined(handler.player)
        );
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                KonfigRuntime.playerLeft(handler.player)
        );
    }

//? if >=1.21.11 {
    private static boolean canEdit(net.minecraft.server.level.ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static boolean supportsRemoteResponses(net.minecraft.server.level.ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Capabilities.TYPE)
                && ServerPlayNetworking.canSend(player, KonfigRemotePayloads.Snapshot.TYPE)
                && ServerPlayNetworking.canSend(player, KonfigRemotePayloads.EditResult.TYPE);
    }
//?}
}
