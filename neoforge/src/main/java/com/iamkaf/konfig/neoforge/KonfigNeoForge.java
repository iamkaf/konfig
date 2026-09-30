package com.iamkaf.konfig.neoforge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.minecraft.server.level.ServerPlayer;
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
                .optional()
                .playToClient(
                        KonfigNetwork.snapshotPayloadType(),
                        KonfigNetwork.snapshotPayloadCodec(),
                        (payload, context) -> KonfigNetwork.receiveClientSnapshot(payload)
                );

        event.registrar(KonfigRuntime.MOD_ID)
                .optional()
                .playToServer(
                        KonfigRemotePayloads.Hello.TYPE,
                        KonfigRemotePayloads.Hello.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof ServerPlayer player
                                    && supportsRemoteResponses(player)) {
                                KonfigSync.onClientHello(player, payload.protocolVersion(), KonfigNetwork.canEdit(player));
                            }
                        }
                )
                .playToServer(
                        KonfigRemotePayloads.EditRequest.TYPE,
                        KonfigRemotePayloads.EditRequest.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof ServerPlayer player) {
                                KonfigSync.onRemoteEdit(player, KonfigNetwork.canEdit(player), KonfigNetwork.editRequest(payload));
                            }
                        }
                )
                .playToClient(
                        KonfigRemotePayloads.Capabilities.TYPE,
                        KonfigRemotePayloads.Capabilities.STREAM_CODEC,
                        (payload, context) -> KonfigNetwork.receiveClientCapabilities(payload)
                )
                .playToClient(
                        KonfigRemotePayloads.Snapshot.TYPE,
                        KonfigRemotePayloads.Snapshot.STREAM_CODEC,
                        (payload, context) -> KonfigNetwork.receiveClientAuthoritySnapshot(payload)
                )
                .playToClient(
                        KonfigRemotePayloads.EditResult.TYPE,
                        KonfigRemotePayloads.EditResult.STREAM_CODEC,
                        (payload, context) -> KonfigNetwork.receiveClientEditResult(payload)
                );

        KonfigRuntime.setSyncSender((player, configId, jsonPayload) -> {
            if (!player.connection.hasChannel(KonfigNetwork.snapshotPayloadType())) {
                return;
            }
            player.connection.send(KonfigNetwork.snapshotPayload(configId, jsonPayload));
        });
        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(ServerPlayer player, ConfigEditCapabilities capabilities) {
                if (player.connection.hasChannel(KonfigRemotePayloads.Capabilities.TYPE)) {
                    player.connection.send(KonfigNetwork.remoteCapabilitiesPayload(capabilities));
                }
            }

            @Override
            public void sendSnapshot(ServerPlayer player, ConfigEditSnapshot snapshot) {
                if (player.connection.hasChannel(KonfigRemotePayloads.Snapshot.TYPE)) {
                    player.connection.send(KonfigNetwork.remoteSnapshotPayload(snapshot));
                }
            }

            @Override
            public void sendResult(ServerPlayer player, ConfigEditResult result) {
                if (player.connection.hasChannel(KonfigRemotePayloads.EditResult.TYPE)) {
                    player.connection.send(KonfigNetwork.remoteResultPayload(result));
                }
            }
        });
    }

    private static boolean supportsRemoteResponses(ServerPlayer player) {
        return player.connection.hasChannel(KonfigRemotePayloads.Capabilities.TYPE)
                && player.connection.hasChannel(KonfigRemotePayloads.Snapshot.TYPE)
                && player.connection.hasChannel(KonfigRemotePayloads.EditResult.TYPE);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KonfigRuntime.playerJoined(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KonfigRuntime.playerLeft(player);
        }
        if (event.getEntity().level().isClientSide()) {
            KonfigRuntime.clientDisconnected();
        }
    }
}
