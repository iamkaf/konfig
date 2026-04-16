package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
//? if <=1.20.4 {
import com.iamkaf.konfig.Constants;
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

public final class KonfigFabric implements ModInitializer {
//? if <=1.20.4 {
//? if <=1.16.5 {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(Constants.MOD_ID, "sync_snapshot");
//?} else {
    private static final net.minecraft.resources.ResourceLocation SYNC_CHANNEL = Constants.resource("sync_snapshot");
//?}
//?}

    @Override
    public void onInitialize() {
        RuntimeEnvironment.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
        );
        KonfigCommon.init();

//? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(KonfigSyncPayload.TYPE, KonfigSyncPayload.STREAM_CODEC);
//?} elif >=1.20.5 {
        PayloadTypeRegistry.playS2C().register(KonfigSyncPayload.TYPE, KonfigSyncPayload.STREAM_CODEC);
//?}

//? if >=1.20.5 {
        KonfigSync.setSender((player, snapshot) ->
                ServerPlayNetworking.send(player, new KonfigSyncPayload(snapshot.configId(), snapshot.jsonPayload()))
        );
//?} else {
        KonfigSync.setSender((player, snapshot) -> {
            net.minecraft.server.level.ServerPlayer serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
//? if <=1.16.5 {
            buffer.writeUtf(snapshot.configId(), 256);
            buffer.writeUtf(snapshot.jsonPayload());
//?} else {
            KonfigSyncPayload.encode(new KonfigSyncPayload(snapshot.configId(), snapshot.jsonPayload()), buffer);
//?}
            ServerPlayNetworking.send(serverPlayer, SYNC_CHANNEL, buffer);
        });
//?}

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KonfigSync.onPlayerJoin(handler.player)
        );
    }
}
