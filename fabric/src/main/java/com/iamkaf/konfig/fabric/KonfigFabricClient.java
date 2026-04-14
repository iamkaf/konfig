package com.iamkaf.konfig.fabric;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public final class KonfigFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RuntimeEnvironment.initialize(FabricLoader.getInstance().getConfigDir(), true);

        ClientPlayNetworking.registerGlobalReceiver(Constants.resource("sync_snapshot"), (client, handler, buffer, responseSender) -> {
            KonfigSyncPayload payload = KonfigSyncPayload.decode(buffer);
            KonfigSync.onClientSnapshot(payload.configId(), payload.jsonPayload());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> KonfigSync.onClientDisconnect());
    }
}
