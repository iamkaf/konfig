package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public final class KonfigFabricClient implements ClientModInitializer {
    private static final ResourceLocation SYNC_CHANNEL = new ResourceLocation(Constants.MOD_ID, "sync_snapshot");

    @Override
    public void onInitializeClient() {
        RuntimeEnvironment.initialize(FabricLoader.getInstance().getConfigDir(), true);

        ClientPlayNetworking.registerGlobalReceiver(SYNC_CHANNEL, (client, handler, buffer, responseSender) -> {
            KonfigSyncPayload payload = new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf());
            KonfigSync.onClientSnapshot(payload.configId(), payload.jsonPayload());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KonfigSync.onClientDisconnect());
    }
}
