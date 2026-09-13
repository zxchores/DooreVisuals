package dev.doorevisuals.cosmetics;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CosmeticsRegistry {
    private static final Set<UUID> DOORE_PEERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, CosmeticsRegistry.Look> LOOKS = new ConcurrentHashMap<>();

    private CosmeticsRegistry() {
    }

    public static void markDoore(UUID id) {
        if (id != null) {
            DOORE_PEERS.add(id);
        }
    }

    public static boolean isDoore(UUID id) {
        return id != null && DOORE_PEERS.contains(id);
    }

    public static void setLook(UUID id, String capeId, String wingsId) {
        setLook(id, capeId, wingsId, "", "");
    }

    public static void setLook(UUID id, String capeId, String wingsId, String hatId, String accessoryId) {
        if (id != null) {
            LOOKS.put(
                id,
                new CosmeticsRegistry.Look(
                    capeId == null ? "" : capeId, wingsId == null ? "" : wingsId, hatId == null ? "" : hatId, accessoryId == null ? "" : accessoryId
                )
            );
        }
    }

    public static CosmeticsRegistry.Look look(UUID id) {
        return id == null ? CosmeticsRegistry.Look.NONE : LOOKS.getOrDefault(id, CosmeticsRegistry.Look.NONE);
    }

    public static void clear() {
        DOORE_PEERS.clear();
        LOOKS.clear();
    }

    public record Look(String capeId, String wingsId, String hatId, String accessoryId) {
        public static final CosmeticsRegistry.Look NONE = new CosmeticsRegistry.Look("", "", "", "");
    }
}
