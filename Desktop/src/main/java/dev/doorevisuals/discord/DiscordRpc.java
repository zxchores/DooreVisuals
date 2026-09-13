package dev.doorevisuals.discord;

import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.ActivityType;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.StatusDisplayType;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.entities.RichPresence.Builder;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DiscordRpc implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Discord");
    private final Supplier<DiscordRpc.Settings> settings;
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "DooreDiscord");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicInteger fails = new AtomicInteger();
    private volatile IPCClient client;
    private volatile long started = System.currentTimeMillis() / 1000L;
    private volatile long boundAppId;
    private volatile String last = "";
    private volatile String status = "\u041e\u0436\u0438\u0434\u0430\u043d\u0438\u0435";
    private volatile boolean connecting;

    public DiscordRpc(Supplier<DiscordRpc.Settings> settings) {
        this.settings = settings;
    }

    public void start() {
        this.io.scheduleWithFixedDelay(this::tick, 1L, 2L, TimeUnit.SECONDS);
    }

    public boolean connected() {
        return this.connected.get();
    }

    public String status() {
        return this.status;
    }

    public void forceRefresh() {
        this.last = "";
        this.io.execute(() -> {
            try {
                this.tick();
            } catch (Throwable throwable) {
            }
        });
    }

    private void tick() {
        try {
            DiscordRpc.Settings discordrpc$settings = this.settings.get();
            if (discordrpc$settings == null || !discordrpc$settings.enabled()) {
                this.disconnect("\u0412\u044b\u043a\u043b");
                return;
            }

            long j = discordrpc$settings.appId();
            if (j <= 0L) {
                this.disconnect("\u041d\u0435\u0442 App ID");
                return;
            }

            if (this.boundAppId != 0L && this.boundAppId != j && this.client != null) {
                this.disconnect(null);
            }

            this.ensure(j);
            if (!this.connected.get() || this.client == null) {
                return;
            }

            DiscordRpc.PresenceSnap discordrpc$presencesnap = this.buildSnap(discordrpc$settings);
            if (discordrpc$presencesnap.key().equals(this.last)) {
                this.status = "Live";
                return;
            }

            this.last = discordrpc$presencesnap.key();
            this.client.sendRichPresence(discordrpc$presencesnap.presence());
            this.status = "Live";
            this.fails.set(0);
        } catch (Throwable throwable) {
            int i = this.fails.incrementAndGet();
            this.status = i > 3
                ? "Discord \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d"
                : "\u041f\u0435\u0440\u0435\u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435\u2026";
            LOG.debug("Discord RPC tick failed: {}", throwable.toString());
            this.disconnect(null);
        }
    }

    private void ensure(long applicationId) throws NoDiscordClientException {
        if (!this.connected.get() || this.client == null || this.boundAppId != applicationId) {
            if (!this.connecting || this.client == null || this.boundAppId != applicationId) {
                this.disconnect(null);
                this.connecting = true;
                IPCClient ipcclient = new IPCClient(applicationId);
                ipcclient.setListener(new IPCListener() {
                    public void onPacketSent(IPCClient c, Packet p) {
                    }

                    public void onPacketReceived(IPCClient c, Packet p) {
                    }

                    public void onActivityJoin(IPCClient c, String secret) {
                    }

                    public void onActivitySpectate(IPCClient c, String secret) {
                    }

                    public void onActivityJoinRequest(IPCClient c, String secret, User user) {
                    }

                    public void onReady(IPCClient ready) {
                        DiscordRpc.this.connected.set(true);
                        DiscordRpc.this.connecting = false;
                        DiscordRpc.this.boundAppId = applicationId;
                        DiscordRpc.this.started = System.currentTimeMillis() / 1000L;
                        DiscordRpc.this.last = "";
                        DiscordRpc.this.status = "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e";
                        DiscordRpc.LOG.info("Discord RPC connected as Doore Visuals ({})", applicationId);
                    }

                    public void onClose(IPCClient c, JsonObject json) {
                        DiscordRpc.this.connected.set(false);
                        DiscordRpc.this.connecting = false;
                        DiscordRpc.this.status = "\u0417\u0430\u043a\u0440\u044b\u0442\u043e";
                    }

                    public void onDisconnect(IPCClient c, Throwable t) {
                        DiscordRpc.this.connected.set(false);
                        DiscordRpc.this.connecting = false;
                        DiscordRpc.this.status = "\u041e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e";
                    }
                });
                this.client = ipcclient;
                this.boundAppId = applicationId;
                this.status = "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435\u2026";
                ipcclient.connect(new DiscordBuild[0]);
            }
        }
    }

    private DiscordRpc.PresenceSnap buildSnap(DiscordRpc.Settings s) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        Builder builder = new Builder();
        String sx;
        String s1;
        if (s.streamerSafe()) {
            sx = "DooreVisuals 3.31.0";
            s1 = "\u0412 \u0438\u0433\u0440\u0435";
        } else {
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            if (clientplayerentity != null) {
                StringBuilder stringbuilder = new StringBuilder(clientplayerentity.getGameProfile().name());
                if (s.showWorld() && minecraftclient.world != null) {
                    stringbuilder.append(" \u00b7 ").append(prettyDim(minecraftclient.world.getRegistryKey().getValue().getPath()));
                }

                if (s.showFps()) {
                    stringbuilder.append(" \u00b7 ").append(minecraftclient.getCurrentFps()).append(" FPS");
                }

                sx = stringbuilder.toString();
            } else {
                sx = "DooreVisuals 3.31.0";
            }

            if (s.showServer()) {
                ServerInfo serverinfo = minecraftclient.getCurrentServerEntry();
                if (serverinfo != null && serverinfo.address != null && !serverinfo.address.isBlank()) {
                    s1 = "\u0421\u0435\u0440\u0432\u0435\u0440 \u00b7 " + serverinfo.address;
                } else if (minecraftclient.isIntegratedServerRunning()) {
                    s1 = "\u041e\u0434\u0438\u043d\u043e\u0447\u043d\u0430\u044f \u0438\u0433\u0440\u0430";
                } else if (minecraftclient.world != null) {
                    s1 = "\u0412 \u043c\u0438\u0440\u0435";
                } else {
                    s1 = "\u0413\u043b\u0430\u0432\u043d\u043e\u0435 \u043c\u0435\u043d\u044e";
                }
            } else {
                s1 = "Visual client";
            }
        }

        sx = clip(sx);
        s1 = clip(s1);
        builder.setActivityType(ActivityType.Playing);
        builder.setStatusDisplayType(StatusDisplayType.Name);
        builder.setDetails(sx);
        builder.setState(s1);
        if (s.useLogo()) {
            String s2 = s.artKey();

            try {
                builder.setLargeImage(s2, "DooreVisuals", null);
            } catch (Throwable throwable1) {
                try {
                    builder.setLargeImage(s2);
                } catch (Throwable throwable) {
                    this.status = "\u041d\u0435\u0442 Art Asset \u00ab" + s2 + "\u00bb \u2014 \u0437\u0430\u0433\u0440\u0443\u0437\u0438 \u0432 Discord Portal";
                }
            }
        }

        if (s.showElapsed()) {
            builder.setStartTimestamp(this.started);
        }

        RichPresence richpresence = builder.build();
        String s3 = sx + "|" + s1 + "|" + (s.showElapsed() ? this.started : 0L);
        return new DiscordRpc.PresenceSnap(richpresence, s3);
    }

    private static String prettyDim(String path) {
        return switch (path) {
            case "overworld" -> "\u041e\u0431\u044b\u0447\u043d\u044b\u0439 \u043c\u0438\u0440";
            case "the_nether" -> "\u0410\u0434";
            case "the_end" -> "\u042d\u043d\u0434";
            default -> path;
        };
    }

    private static String clip(String v) {
        if (v != null && !v.isBlank()) {
            return v.length() > 120 ? v.substring(0, 117) + "..." : v;
        } else {
            return "DooreVisuals";
        }
    }

    private void disconnect(String reason) {
        this.connected.set(false);
        this.connecting = false;
        this.last = "";
        this.boundAppId = 0L;
        if (reason != null) {
            this.status = reason;
        }

        IPCClient ipcclient = this.client;
        this.client = null;
        if (ipcclient != null) {
            try {
                ipcclient.close();
            } catch (Throwable throwable) {
            }
        }
    }

    @Override
    public void close() {
        this.io.shutdownNow();
        this.disconnect("\u0421\u0442\u043e\u043f");
    }

    private record PresenceSnap(RichPresence presence, String key) {
    }

    public interface Settings {
        boolean enabled();

        long appId();

        boolean showElapsed();

        boolean showServer();

        boolean showWorld();

        boolean showFps();

        boolean streamerSafe();

        boolean useLogo();

        String artKey();
    }
}
