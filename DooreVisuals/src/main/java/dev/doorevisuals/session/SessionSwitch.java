package dev.doorevisuals.session;

import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.mix.MinecraftUserAccessor;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.util.Uuids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionSwitch {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals");

    private SessionSwitch() {
    }

    public static boolean applyOffline(String rawName) {
        String s = rawName == null ? "" : rawName.trim();
        if (!s.isEmpty() && s.length() <= 16) {
            s = s.replaceAll("[^A-Za-z0-9_]", "");
            if (s.isEmpty()) {
                return false;
            } else {
                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                if (minecraftclient == null) {
                    return false;
                } else {
                    AltsStore.Alt altsstore$alt = AltsStore.find(s);
                    UUID uuid = altsstore$alt != null ? altsstore$alt.uuid() : Uuids.getOfflinePlayerUuid(s);
                    Session session = new Session(s, uuid, "0", Optional.empty(), Optional.empty());

                    try {
                        ((MinecraftUserAccessor)minecraftclient).doore$setUser(session);
                        AltsStore.markActive(s);
                        LOG.info("Session \u2192 {}", s);
                        return true;
                    } catch (Throwable throwable) {
                        LOG.error("Failed to switch session to {}", s, throwable);
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }
}
