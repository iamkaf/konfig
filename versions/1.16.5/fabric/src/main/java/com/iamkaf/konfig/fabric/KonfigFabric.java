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
import net.minecraft.resources.ResourceLocation;

public final class KonfigFabric implements ModInitializer {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(Constants.MOD_ID, "sync_snapshot");

    @Override
    public void onInitialize() {
        RuntimeEnvironment.initialize(
                FabricLoader.getInstance().getConfigDir(),
                FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT
        );
        KonfigCommon.init();

        KonfigSync.setSender((player, snapshot) -> {
            net.minecraft.server.level.ServerPlayer serverPlayer = (net.minecraft.server.level.ServerPlayer) player;
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeUtf(snapshot.configId(), 256);
            buffer.writeUtf(snapshot.jsonPayload());
            ServerPlayNetworking.send(serverPlayer, SYNC_CHANNEL, buffer);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KonfigSync.onPlayerJoin(handler.player)
        );
    }
}
