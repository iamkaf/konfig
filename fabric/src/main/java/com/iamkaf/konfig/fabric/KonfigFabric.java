package com.iamkaf.konfig.fabric;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
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
}
