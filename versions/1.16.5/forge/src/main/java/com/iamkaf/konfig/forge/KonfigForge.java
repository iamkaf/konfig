package com.iamkaf.konfig.forge;

import com.iamkaf.konfig.Constants;
import com.iamkaf.konfig.KonfigCommon;
import com.iamkaf.konfig.impl.v1.RuntimeEnvironment;
import com.iamkaf.konfig.sync.v1.KonfigSync;
import com.iamkaf.konfig.sync.v1.KonfigSyncPayload;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Supplier;

@Mod(Constants.MOD_ID)
public final class KonfigForge {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public KonfigForge() {
        RuntimeEnvironment.initialize(FMLPaths.CONFIGDIR.get(), FMLLoader.getDist().isClient());
        KonfigCommon.init();

        if (FMLLoader.getDist().isClient()) {
            KonfigForgeClient.init();
        }

        CHANNEL.registerMessage(0, SyncMessage.class, SyncMessage::encode, SyncMessage::decode,
                (message, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    context.enqueueWork(() -> {
                        if (context.getSender() == null) {
                            KonfigSync.onClientSnapshot(message.configId, message.jsonPayload);
                        }
                    });
                    context.setPacketHandled(true);
                });

        KonfigSync.setSender((player, snapshot) ->
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new SyncMessage(snapshot.configId(), snapshot.jsonPayload()))
        );

        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLeave);
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            KonfigSync.onPlayerJoin(player);
        }
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (FMLLoader.getDist().isClient()) {
            KonfigSync.onClientDisconnect();
        }
    }

    private static final class SyncMessage {
        private final String configId;
        private final String jsonPayload;

        private SyncMessage(String configId, String jsonPayload) {
            this.configId = configId;
            this.jsonPayload = jsonPayload;
        }

        private static void encode(SyncMessage message, PacketBuffer buffer) {
            buffer.writeUtf(message.configId);
            buffer.writeUtf(message.jsonPayload);
        }

        private static SyncMessage decode(PacketBuffer buffer) {
            KonfigSyncPayload payload = new KonfigSyncPayload(buffer.readUtf(256), buffer.readUtf());
            return new SyncMessage(payload.configId(), payload.jsonPayload());
        }
    }
}
