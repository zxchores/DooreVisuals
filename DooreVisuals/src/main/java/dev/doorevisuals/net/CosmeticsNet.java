package dev.doorevisuals.net;

import dev.doorevisuals.cosmetics.CosmeticsRegistry;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.Context;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CosmeticsNet {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Net");
    private static boolean bound;

    private CosmeticsNet() {
    }

    public static void initClient() {
        if (!bound) {
            bound = true;
            PayloadTypeRegistry.playC2S().register(DoorePayloads.HandshakePayload.TYPE, DoorePayloads.HandshakePayload.CODEC);
            PayloadTypeRegistry.playC2S().register(DoorePayloads.CosmeticsPayload.TYPE, DoorePayloads.CosmeticsPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DoorePayloads.HandshakePayload.TYPE, DoorePayloads.HandshakePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(DoorePayloads.CosmeticsPayload.TYPE, DoorePayloads.CosmeticsPayload.CODEC);
            ClientPlayNetworking.registerGlobalReceiver(
                DoorePayloads.HandshakePayload.TYPE, (payload, ctx) -> ctx.client().execute(() -> CosmeticsRegistry.markDoore(payload.playerId()))
            );
            ClientPlayNetworking.registerGlobalReceiver(
                DoorePayloads.CosmeticsPayload.TYPE,
                (payload, ctx) -> ctx.client()
                    .execute(() -> CosmeticsRegistry.setLook(payload.playerId(), payload.capeId(), payload.wingsId(), payload.hatId(), payload.accessoryId()))
            );
            ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> client.execute(() -> {
                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                if (minecraftclient.player != null) {
                    UUID uuid = minecraftclient.player.getUuid();
                    CosmeticsRegistry.markDoore(uuid);
                    if (ClientPlayNetworking.canSend(DoorePayloads.HandshakePayload.TYPE)) {
                        ClientPlayNetworking.send(new DoorePayloads.HandshakePayload(uuid));
                    }

                    CosmeticsCloud.fetch(uuid);
                }
            }));
            ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> CosmeticsRegistry.clear());
        }
    }

    public static void sendCosmetics(UUID id, String capeId, String wingsId, String hatId, String accessoryId) {
        if (id != null) {
            CosmeticsRegistry.setLook(id, capeId, wingsId, hatId, accessoryId);
            if (ClientPlayNetworking.canSend(DoorePayloads.CosmeticsPayload.TYPE)) {
                ClientPlayNetworking.send(new DoorePayloads.CosmeticsPayload(id, capeId, wingsId, hatId, accessoryId));
            }

            CosmeticsCloud.push(id, capeId, wingsId);
        }
    }

    public static void sendCosmetics(UUID id, String capeId, String wingsId) {
        sendCosmetics(id, capeId, wingsId, "", "");
    }
}
