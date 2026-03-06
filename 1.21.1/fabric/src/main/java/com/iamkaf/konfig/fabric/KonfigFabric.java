package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public final class KonfigFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RuntimeEnvironment.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
        );
        KonfigCommon.init();

        PayloadTypeRegistry.playS2C().register(KonfigSyncPayload.TYPE, KonfigSyncPayload.STREAM_CODEC);

        KonfigSync.setSender((player, snapshot) ->
                ServerPlayNetworking.send(player, new KonfigSyncPayload(snapshot.configId(), snapshot.jsonPayload()))
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KonfigSync.onPlayerJoin(handler.player)
        );
    }
}
