package com.iamkaf.konfig.forge;

import org.jetbrains.annotations.ApiStatus;

import com.iamkaf.konfig.impl.v1.bootstrap.Constants;
import com.iamkaf.konfig.impl.v1.runtime.KonfigRuntime;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditCapabilities;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditRequest;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditResult;
import com.iamkaf.konfig.impl.v1.sync.ConfigEditSnapshot;
import com.iamkaf.konfig.impl.v1.sync.ConfigSyncAuthority;
import com.iamkaf.konfig.impl.v1.sync.KonfigNetwork;
import com.iamkaf.konfig.impl.v1.sync.KonfigRemotePayloads;
import com.iamkaf.konfig.impl.v1.sync.KonfigSync;
import com.iamkaf.konfig.impl.v1.sync.SyncSnapshot;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
//? if >=1.20.2 {
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
//?} elif >=1.18 {
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?} else {
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
//?}

//? if <=1.20.1 {
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
//?}

@Mod(KonfigRuntime.MOD_ID)
@ApiStatus.Internal
public final class KonfigForge {
//? if >=1.20.2 {
    private static final int PROTOCOL = KonfigNetwork.FORGE_PROTOCOL_VERSION;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(KonfigNetwork.mainChannel())
            .networkProtocolVersion(PROTOCOL)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .optional()
            .simpleChannel();
    private static final SimpleChannel REMOTE_CHANNEL = ChannelBuilder
            .named(Constants.resource("remote_edit_v1"))
            .networkProtocolVersion(ConfigSyncAuthority.PROTOCOL_VERSION)
            .optional()
            .simpleChannel();
//?} else {
    private static final String PROTOCOL = KonfigNetwork.FORGE_PROTOCOL;
    private static final String REMOTE_PROTOCOL = String.valueOf(ConfigSyncAuthority.PROTOCOL_VERSION);
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            KonfigNetwork.mainChannel(),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL)
    );
    private static final SimpleChannel REMOTE_CHANNEL = NetworkRegistry.newSimpleChannel(
            Constants.resource("remote_edit_v1"),
            () -> REMOTE_PROTOCOL,
            NetworkRegistry.acceptMissingOr(REMOTE_PROTOCOL),
            NetworkRegistry.acceptMissingOr(REMOTE_PROTOCOL)
    );
//?}

    public KonfigForge() {
        KonfigRuntime.initialize(FMLPaths.CONFIGDIR.get(), FMLLoader.getDist().isClient());

        if (FMLLoader.getDist().isClient()) {
            KonfigForgeClient.init();
        }

//? if >=1.20.2 {
        CHANNEL.messageBuilder(SyncMessage.class)
                .encoder(SyncMessage::encode)
                .decoder(SyncMessage::decode)
                .consumerMainThread((message, context) -> {
                    if (context.getSender() == null) {
                        KonfigNetwork.receiveClientSnapshot(message.snapshot);
                    }
                })
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.Hello.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> KonfigRemotePayloads.Hello.write(buffer, message))
                .decoder(KonfigRemotePayloads.Hello::read)
                .consumerMainThread((message, context) -> onHello(context.getSender(), message))
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.Capabilities.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> KonfigRemotePayloads.Capabilities.write(buffer, message))
                .decoder(KonfigRemotePayloads.Capabilities::read)
                .consumerMainThread((message, context) -> KonfigNetwork.receiveClientCapabilities(message))
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.Snapshot.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> KonfigRemotePayloads.Snapshot.write(buffer, message))
                .decoder(KonfigRemotePayloads.Snapshot::read)
                .consumerMainThread((message, context) -> KonfigNetwork.receiveClientAuthoritySnapshot(message))
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.EditRequest.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> KonfigRemotePayloads.EditRequest.write(buffer, message))
                .decoder(KonfigRemotePayloads.EditRequest::read)
                .consumerMainThread((message, context) -> onEdit(context.getSender(), message))
                .add();
        REMOTE_CHANNEL.messageBuilder(KonfigRemotePayloads.EditResult.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> KonfigRemotePayloads.EditResult.write(buffer, message))
                .decoder(KonfigRemotePayloads.EditResult::read)
                .consumerMainThread((message, context) -> KonfigNetwork.receiveClientEditResult(message))
                .add();
//?} else {
        CHANNEL.registerMessage(0, SyncMessage.class, SyncMessage::encode, SyncMessage::decode,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getSender() == null) {
                            KonfigNetwork.receiveClientSnapshot(message.snapshot);
                        }
                    });
                    context.setPacketHandled(true);
                });
        registerRemote(0, KonfigRemotePayloads.Hello.class,
                (message, buffer) -> KonfigRemotePayloads.Hello.write(buffer, message), KonfigRemotePayloads.Hello::read,
                NetworkDirection.PLAY_TO_SERVER, (message, sender) -> onHello(sender, message));
        registerRemote(1, KonfigRemotePayloads.Capabilities.class,
                (message, buffer) -> KonfigRemotePayloads.Capabilities.write(buffer, message), KonfigRemotePayloads.Capabilities::read,
                NetworkDirection.PLAY_TO_CLIENT, (message, sender) -> KonfigNetwork.receiveClientCapabilities(message));
        registerRemote(2, KonfigRemotePayloads.Snapshot.class,
                (message, buffer) -> KonfigRemotePayloads.Snapshot.write(buffer, message), KonfigRemotePayloads.Snapshot::read,
                NetworkDirection.PLAY_TO_CLIENT, (message, sender) -> KonfigNetwork.receiveClientAuthoritySnapshot(message));
        registerRemote(3, KonfigRemotePayloads.EditRequest.class,
                (message, buffer) -> KonfigRemotePayloads.EditRequest.write(buffer, message), KonfigRemotePayloads.EditRequest::read,
                NetworkDirection.PLAY_TO_SERVER, (message, sender) -> onEdit(sender, message));
        registerRemote(4, KonfigRemotePayloads.EditResult.class,
                (message, buffer) -> KonfigRemotePayloads.EditResult.write(buffer, message), KonfigRemotePayloads.EditResult::read,
                NetworkDirection.PLAY_TO_CLIENT, (message, sender) -> KonfigNetwork.receiveClientEditResult(message));
//?}

        KonfigRuntime.setSyncSender((player, configId, jsonPayload) ->
                sendToPlayer(CHANNEL, player, SyncMessage.of(configId, jsonPayload))
        );
        KonfigSync.setRemoteSender(new KonfigSync.RemoteSender() {
            @Override
            public void sendCapabilities(ServerPlayer player, ConfigEditCapabilities capabilities) {
                sendToPlayer(REMOTE_CHANNEL, player, KonfigNetwork.remoteCapabilitiesPayload(capabilities));
            }

            @Override
            public void sendSnapshot(ServerPlayer player, ConfigEditSnapshot snapshot) {
                sendToPlayer(REMOTE_CHANNEL, player, KonfigNetwork.remoteSnapshotPayload(snapshot));
            }

            @Override
            public void sendResult(ServerPlayer player, ConfigEditResult result) {
                sendToPlayer(REMOTE_CHANNEL, player, KonfigNetwork.remoteResultPayload(result));
            }
        });

//? if >=1.21.6 {
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(this::onPlayerJoin);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(this::onPlayerLeave);
//?} else {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);
//?}
    }

    static boolean supportsRemoteEditing(Connection connection) {
        return REMOTE_CHANNEL.isRemotePresent(connection);
    }

    static void sendRemoteHello(int protocolVersion) {
        sendToServer(KonfigNetwork.remoteHelloPayload(protocolVersion));
    }

    static void sendRemoteEdit(ConfigEditRequest request) {
        sendToServer(KonfigNetwork.remoteEditPayload(request));
    }

    private static void onHello(ServerPlayer player, KonfigRemotePayloads.Hello message) {
        if (player != null) {
            KonfigSync.onClientHello(player, message.protocolVersion(), KonfigNetwork.canEdit(player));
        }
    }

    private static void onEdit(ServerPlayer player, KonfigRemotePayloads.EditRequest message) {
        if (player != null) {
            KonfigSync.onRemoteEdit(player, KonfigNetwork.canEdit(player), KonfigNetwork.editRequest(message));
        }
    }

    // Channels are optional, so every clientbound send checks that the peer negotiated the channel.
    private static void sendToPlayer(SimpleChannel channel, ServerPlayer player, Object message) {
        if (!channel.isRemotePresent(connectionOf(player))) {
            return;
        }
//? if >=1.20.2 {
        channel.send(message, PacketDistributor.PLAYER.with(player));
//?} else {
        channel.send(PacketDistributor.PLAYER.with(() -> player), message);
//?}
    }

    private static void sendToServer(Object message) {
//? if >=1.20.2 {
        REMOTE_CHANNEL.send(message, PacketDistributor.SERVER.noArg());
//?} else {
        REMOTE_CHANNEL.sendToServer(message);
//?}
    }

    private static Connection connectionOf(ServerPlayer player) {
//? if >=1.19.4 && <1.20.2 {
        // Vanilla has no getter on these lines; Forge's access transformer opens the field.
        return player.connection.connection;
//?} else {
        return player.connection.getConnection();
//?}
    }

//? if <=1.20.1 {
    private static <M> void registerRemote(
            int index,
            Class<M> type,
            BiConsumer<M, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, M> decoder,
            NetworkDirection direction,
            BiConsumer<M, ServerPlayer> handler
    ) {
        REMOTE_CHANNEL.registerMessage(index, type, encoder, decoder, (message, contextSupplier) -> {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> handler.accept(message, context.getSender()));
            context.setPacketHandled(true);
        }, Optional.of(direction));
    }
//?}

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KonfigRuntime.playerJoined(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KonfigRuntime.playerLeft(player);
        }
//? if >=1.20.2 {
        if (event.getEntity().level().isClientSide()) {
            KonfigRuntime.clientDisconnected();
        }
//?} else {
        if (FMLLoader.getDist().isClient()) {
            KonfigRuntime.clientDisconnected();
        }
//?}
    }

    private static final class SyncMessage {
        private final SyncSnapshot snapshot;

        private SyncMessage(SyncSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        private static SyncMessage of(String configId, String jsonPayload) {
            return new SyncMessage(KonfigNetwork.snapshot(configId, jsonPayload));
        }

        private static void encode(SyncMessage message, FriendlyByteBuf buffer) {
            KonfigNetwork.encodeSnapshot(message.snapshot, buffer);
        }

        private static SyncMessage decode(FriendlyByteBuf buffer) {
            return new SyncMessage(KonfigNetwork.decodeSnapshot(buffer));
        }
    }
}
