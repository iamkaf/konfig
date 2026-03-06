package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;

public final class KonfigFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RuntimeEnvironment.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
        );
        KonfigCommon.init();

        KonfigSync.setSender((player, snapshot) -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            KonfigSyncPayload.encode(new KonfigSyncPayload(snapshot.configId(), snapshot.jsonPayload()), buffer);
            ServerPlayNetworking.send(player, Constants.resource("sync_snapshot"), buffer);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KonfigSync.onPlayerJoin(handler.player)
        );
    }
}
