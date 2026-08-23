package com.iamkaf.konfig.neoforge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
//?}
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
//? if >=1.21.11 {
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import net.minecraft.server.permissions.Permissions;
//?}
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

//? if >=1.21.11 {
        event.registrar(KonfigRuntime.MOD_ID)
                .optional()
                .playToServer(
                        KonfigRemotePayloads.Hello.TYPE,
                        KonfigRemotePayloads.Hello.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player
                                    && supportsRemoteResponses(player)) {
                                KonfigSync.onClientHello(player, payload.protocolVersion(), canEdit(player));
                            }
                        }
                )
                .playToServer(
                        KonfigRemotePayloads.EditRequest.TYPE,
                        KonfigRemotePayloads.EditRequest.STREAM_CODEC,
                        (payload, context) -> {
                            if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                                KonfigSync.onRemoteEdit(player, canEdit(player), KonfigNetwork.editRequest(payload));
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
//?}

        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                player.connection.send(KonfigNetwork.snapshotPayload(configId, jsonPayload))
        );
//? if >=1.21.11 {
        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(net.minecraft.server.level.ServerPlayer player, ConfigEditCapabilities capabilities) {
                if (player.connection.hasChannel(KonfigRemotePayloads.Capabilities.TYPE)) {
                    player.connection.send(KonfigNetwork.remoteCapabilitiesPayload(capabilities));
                }
            }

            @Override
            public void sendSnapshot(net.minecraft.server.level.ServerPlayer player, ConfigEditSnapshot snapshot) {
                if (player.connection.hasChannel(KonfigRemotePayloads.Snapshot.TYPE)) {
                    player.connection.send(KonfigNetwork.remoteSnapshotPayload(snapshot));
                }
            }

            @Override
            public void sendResult(net.minecraft.server.level.ServerPlayer player, ConfigEditResult result) {
                if (player.connection.hasChannel(KonfigRemotePayloads.EditResult.TYPE)) {
                    player.connection.send(KonfigNetwork.remoteResultPayload(result));
                }
            }
        });
//?}
    }

//? if >=1.21.11 {
    private static boolean canEdit(net.minecraft.server.level.ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static boolean supportsRemoteResponses(net.minecraft.server.level.ServerPlayer player) {
        return player.connection.hasChannel(KonfigRemotePayloads.Capabilities.TYPE)
                && player.connection.hasChannel(KonfigRemotePayloads.Snapshot.TYPE)
                && player.connection.hasChannel(KonfigRemotePayloads.EditResult.TYPE);
    }
//?}

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
