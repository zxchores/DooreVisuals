package dev.doorevisuals;

import dev.doorevisuals.audio.ModSounds;
import dev.doorevisuals.core.FeatureBus;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.data.Account;
import dev.doorevisuals.data.Aim;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.data.Privileges;
import dev.doorevisuals.data.Save;
import dev.doorevisuals.discord.DiscordRpc;
import dev.doorevisuals.draw.Gpu;
import dev.doorevisuals.friends.FriendsFeature;
import dev.doorevisuals.media.MediaHub;
import dev.doorevisuals.overlay.HotbarFeature;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.overlay.HudTweaksFeature;
import dev.doorevisuals.tools.AspectFeature;
import dev.doorevisuals.tools.AutoSprintFeature;
import dev.doorevisuals.tools.AutoSwapFeature;
import dev.doorevisuals.tools.BrightFeature;
import dev.doorevisuals.tools.ChatFeature;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.CrosshairFeature;
import dev.doorevisuals.tools.DiscordFeature;
import dev.doorevisuals.tools.ElytraSwapFeature;
import dev.doorevisuals.tools.FreeLookFeature;
import dev.doorevisuals.tools.FtHelperFeature;
import dev.doorevisuals.tools.GpsFeature;
import dev.doorevisuals.tools.GuiWindowsFeature;
import dev.doorevisuals.tools.HitSoundFeature;
import dev.doorevisuals.tools.HwHelperFeature;
import dev.doorevisuals.tools.InvToolsFeature;
import dev.doorevisuals.tools.InventoryTintFeature;
import dev.doorevisuals.tools.ItemHighlightFeature;
import dev.doorevisuals.tools.ItemPhysicsFeature;
import dev.doorevisuals.tools.NoRenderFeature;
import dev.doorevisuals.tools.SmoothF5Feature;
import dev.doorevisuals.tools.SwingFeature;
import dev.doorevisuals.tools.ThemeFeature;
import dev.doorevisuals.tools.ViewmodelFeature;
import dev.doorevisuals.tools.ZoomFeature;
import dev.doorevisuals.world.AtmosphereFeature;
import dev.doorevisuals.world.BlockHighlightFeature;
import dev.doorevisuals.world.ChinaHatFeature;
import dev.doorevisuals.world.HitColorFeature;
import dev.doorevisuals.world.HitFxFeature;
import dev.doorevisuals.world.JumpCirclesFeature;
import dev.doorevisuals.world.KillFxFeature;
import dev.doorevisuals.world.MotionTrailFeature;
import dev.doorevisuals.world.PredictionsFeature;
import dev.doorevisuals.world.TargetEspFeature;
import dev.doorevisuals.world.WorldParticlesFeature;
import dev.doorevisuals.world.WorldPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class App {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals");
    private static final App I = new App();
    private FeatureBus features;
    private Aim aim;
    private Save save;
    private DiscordRpc discord;
    private boolean live;

    private App() {
    }

    public static App get() {
        return I;
    }

    public static void boot() {
        I.start();
    }

    public static void shutdown() {
        I.stop();
    }

    public static boolean live() {
        return I.live;
    }

    public static FeatureBus features() {
        return I.features;
    }

    public static Aim aim() {
        return I.aim;
    }

    public static Save save() {
        return I.save;
    }

    private synchronized void start() {
        if (!this.live) {
            LOG.info("Bootstrapping DooreVisuals {}", "3.30.2");
            BootDebugLog.write("H1", "App.java:start", "mod bootstrap reached", "{\"version\":\"3.30.2\"}");
            ModSounds.boot();
            this.aim = new Aim();
            this.aim.bind();
            this.features = new FeatureBus();
            DiscordFeature discordfeature = new DiscordFeature();
            GpsFeature gpsfeature = new GpsFeature();
            HitFxFeature hitfxfeature = new HitFxFeature();
            KillFxFeature killfxfeature = new KillFxFeature();
            HitSoundFeature hitsoundfeature = new HitSoundFeature();
            install(this.features, discordfeature, gpsfeature, hitfxfeature, killfxfeature, hitsoundfeature);
            this.save = new Save(this.features);
            this.save.load();
            this.discord = new DiscordRpc(() -> discordfeature);
            discordfeature.bind(this.discord);
            gpsfeature.bindChat();
            features().find(ConfigFeature.class).ifPresent(ConfigFeature::bind);
            hitfxfeature.bind();
            killfxfeature.bind();
            hitsoundfeature.bind();
            WorldPipe.bind();
            Gpu.warm();
            ClientPaths.ensureAll();
            Account.ensure();
            this.live = true;
            LOG.info("Live \u2014 {} features", this.features.all().size());
        }
    }

    public static void startIntegrations() {
        I.startNatives();
    }

    private synchronized void startNatives() {
        if (this.live) {
            try {
                if (this.discord != null) {
                    this.discord.start();
                }
            } catch (Throwable throwable2) {
                LOG.warn("Discord RPC failed to start: {}", throwable2.toString());
            }

            try {
                MediaHub.get().start();
            } catch (Throwable throwable1) {
                LOG.warn("Media hub failed to start: {}", throwable1.toString());
            }

            try {
                Privileges.refresh();
            } catch (Throwable throwable) {
                LOG.warn("Privileges failed to start: {}", throwable.toString());
            }
        }
    }

    private synchronized void stop() {
        if (this.live) {
            if (this.save != null) {
                this.save.flush();
            }

            if (this.discord != null) {
                this.discord.close();
            }

            MediaHub.get().stop();
            this.live = false;
        }
    }

    private static void install(FeatureBus bus, DiscordFeature discord, GpsFeature gps, HitFxFeature hitFx, KillFxFeature killFx, HitSoundFeature hitSound) {
        bus.add(new HudFeature());
        bus.add(new HotbarFeature());
        bus.add(new CrosshairFeature());
        bus.add(new TargetEspFeature());
        bus.add(new HitColorFeature());
        bus.add(new PredictionsFeature());
        bus.add(new JumpCirclesFeature());
        bus.add(new BlockHighlightFeature());
        bus.add(new ChinaHatFeature());
        bus.add(hitFx);
        bus.add(new WorldParticlesFeature());
        bus.add(killFx);
        bus.add(new AtmosphereFeature());
        bus.add(new BrightFeature());
        bus.add(new NoRenderFeature());
        bus.add(new ZoomFeature());
        bus.add(new SmoothF5Feature());
        bus.add(new FreeLookFeature());
        bus.add(new ViewmodelFeature());
        bus.add(new SwingFeature());
        bus.add(hitSound);
        bus.add(new ItemPhysicsFeature());
        bus.add(new AspectFeature());
        bus.add(new AutoSprintFeature());
        bus.add(new GuiWindowsFeature());
        bus.add(new ThemeFeature());
        bus.add(new ConfigFeature());
        bus.add(new ChatFeature());
        bus.add(new InventoryTintFeature());
        bus.add(new ItemHighlightFeature());
        bus.add(new MotionTrailFeature());
        bus.add(new CosmeticsFeature());
        bus.add(new FriendsFeature());
        bus.add(discord);
        bus.add(gps);
        bus.add(new HudTweaksFeature());
        bus.add(new AutoSwapFeature());
        bus.add(new ElytraSwapFeature());
        bus.add(new InvToolsFeature());
        bus.add(new FtHelperFeature());
        bus.add(new HwHelperFeature());
    }
}
