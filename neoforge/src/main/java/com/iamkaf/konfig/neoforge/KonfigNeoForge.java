package com.iamkaf.konfig.neoforge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(KonfigRuntime.MOD_ID)
@ApiStatus.Internal
public final class KonfigNeoForge {
    public KonfigNeoForge(IEventBus eventBus) {
//? if >=1.21.9 {
        KonfigRuntime.initialize(FMLPaths.CONFIGDIR.get(), FMLEnvironment.getDist().isClient());
//?} else {
        KonfigRuntime.initialize(FMLPaths.CONFIGDIR.get(), FMLEnvironment.dist.isClient());
//?}

        eventBus.addListener(this::onRegisterPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(KonfigRuntime.MOD_ID)
                .playToClient(
                        KonfigNetwork.snapshotPayloadType(),
                        KonfigNetwork.snapshotPayloadCodec(),
                        (payload, context) -> KonfigNetwork.receiveClientSnapshot(payload)
                );

        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                player.connection.send(KonfigNetwork.snapshotPayload(configId, jsonPayload))
        );
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigRuntime.playerJoined(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            KonfigRuntime.playerLeft(player);
        }
        if (event.getEntity().level().isClientSide()) {
            KonfigRuntime.clientDisconnected();
        }
    }
}
