package dev.doorevisuals.net;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;

public final class DoorePayloads {
    public static final Identifier HANDSHAKE_ID = Identifier.of("doorevisuals", "handshake");
    public static final Identifier COSMETICS_ID = Identifier.of("doorevisuals", "cosmetics");

    private DoorePayloads() {
    }

    public record CosmeticsPayload(UUID playerId, String capeId, String wingsId, String hatId, String accessoryId) implements CustomPayload {
        public static final Id<DoorePayloads.CosmeticsPayload> TYPE = new Id(DoorePayloads.COSMETICS_ID);
        public static final PacketCodec<RegistryByteBuf, DoorePayloads.CosmeticsPayload> CODEC = PacketCodec.ofStatic((buf, p) -> {
            buf.writeUuid(p.playerId);
            buf.writeString(p.capeId == null ? "" : p.capeId, 32);
            buf.writeString(p.wingsId == null ? "" : p.wingsId, 32);
            buf.writeString(p.hatId == null ? "" : p.hatId, 32);
            buf.writeString(p.accessoryId == null ? "" : p.accessoryId, 32);
        }, buf -> new DoorePayloads.CosmeticsPayload(buf.readUuid(), buf.readString(32), buf.readString(32), buf.readString(32), buf.readString(32)));

        public Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    public record HandshakePayload(UUID playerId) implements CustomPayload {
        public static final Id<DoorePayloads.HandshakePayload> TYPE = new Id(DoorePayloads.HANDSHAKE_ID);
        public static final PacketCodec<RegistryByteBuf, DoorePayloads.HandshakePayload> CODEC = PacketCodec.ofStatic(
            (buf, p) -> buf.writeUuid(p.playerId), buf -> new DoorePayloads.HandshakePayload(buf.readUuid())
        );

        public Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }
}
