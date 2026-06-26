package com.iamkaf.konfig.neoforge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.bootstrap.KonfigCommon;
import com.iamkaf.konfig.impl.v1.bootstrap.RuntimeEnvironment;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import com.iamkaf.konfig.impl.v1.sync.KonfigSyncPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Constants.MOD_ID)
@ApiStatus.Internal
public final class KonfigNeoForge {
    public KonfigNeoForge(IEventBus eventBus) {
//? if >=1.21.9 {
        RuntimeEnvironment.initialize(FMLPaths.CONFIGDIR.get(), FMLEnvironment.getDist().isClient());
//?} else {
        RuntimeEnvironment.initialize(FMLPaths.CONFIGDIR.get(), FMLEnvironment.dist.isClient());
//?}
        KonfigCommon.init();

        eventBus.addListener(this::onRegisterPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(Constants.MOD_ID)
                .playToClient(
                        KonfigSyncPayload.TYPE,
                        KonfigSyncPayload.STREAM_CODEC,
                        (payload, context) -> KonfigSync.onClientSnapshot(payload.configId(), payload.jsonPayload())
                );

        KonfigSync.setSender((player, snapshot) ->
                player.connection.send(new KonfigSyncPayload(snapshot.configId(), snapshot.jsonPayload()))
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigSync.onPlayerJoin(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigSync.onPlayerLeave(player);
        }
        if (event.getEntity().level().isClientSide()) {
            KonfigSync.onClientDisconnect();
        }
    }
}
