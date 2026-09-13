package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.audio.ModSounds;
import dev.doorevisuals.audio.Sfx;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.SessionStats;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.SkinColors;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.overlay.HudFeature;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class KillFxFeature extends Feature implements Tick {
    private static final Identifier STEVE = Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    private final Opt.Pick style = this.opt(
        new Opt.Pick(
            "style",
            "\u0421\u0442\u0438\u043b\u044c",
            "\u0414\u0443\u0448\u0430",
            "\u0414\u0443\u0448\u0430",
            "\u041f\u0440\u0438\u0437\u0440\u0430\u043a",
            "\u0412\u0437\u043b\u0451\u0442",
            "\u0418\u0438\u0441\u0443\u0441"
        )
    );
    private final Opt.Num power = this.opt(new Opt.Num("power", "\u0421\u0438\u043b\u0430", 1.2, 0.5, 2.5, 0.05));
    private final Opt.Flag sound = this.opt(new Opt.Flag("sound", "\u0417\u0432\u0443\u043a", true));
    private final Opt.Flag skinTint = this.opt(new Opt.Flag("skin_tint", "\u0426\u0432\u0435\u0442 \u0441\u043a\u0438\u043d\u0430", true));
    private final List<KillFxFeature.Watch> watch = new ArrayList<>();
    private final List<KillFxFeature.Soul> souls = new ArrayList<>();
    private boolean bound;

    public KillFxFeature() {
        super(
            "kill_fx",
            "Kill FX",
            "\u0414\u0443\u0448\u0430 \u043f\u043e\u0434\u043d\u0438\u043c\u0430\u0435\u0442\u0441\u044f; \u0446\u0432\u0435\u0442 \u0438\u0437 \u0441\u043a\u0438\u043d\u0430 \u0436\u0435\u0440\u0442\u0432\u044b",
            Category.WORLD,
            true
        );
    }

    public void bind() {
        if (!this.bound) {
            this.bound = true;
            AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
                if (level.isClient() && this.on() && entity instanceof LivingEntity livingentity && livingentity.isAlive()) {
                    this.watch.add(new KillFxFeature.Watch(livingentity, System.currentTimeMillis()));

                    while (this.watch.size() > 12) {
                        this.watch.remove(0);
                    }
                }

                return ActionResult.PASS;
            });
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && !this.watch.isEmpty()) {
            long i = System.currentTimeMillis();
            Iterator<KillFxFeature.Watch> iterator = this.watch.iterator();

            while (iterator.hasNext()) {
                KillFxFeature.Watch killfxfeature$watch = iterator.next();
                if (i - killfxfeature$watch.at > 2500L) {
                    iterator.remove();
                } else {
                    LivingEntity livingentity = killfxfeature$watch.target;
                    if (livingentity == null) {
                        iterator.remove();
                    } else if (!livingentity.isAlive() || livingentity.isRemoved()) {
                        this.spawn(livingentity);
                        iterator.remove();
                    }
                }
            }
        }
    }

    public void draw(WorldRenderContext ctx) {
        if (!this.souls.isEmpty()) {
            long i = System.currentTimeMillis();
            float f = this.power.f();
            Iterator<KillFxFeature.Soul> iterator = this.souls.iterator();

            while (iterator.hasNext()) {
                KillFxFeature.Soul killfxfeature$soul = iterator.next();
                float f1 = (float)(i - killfxfeature$soul.born) / killfxfeature$soul.ttl;
                if (f1 >= 1.0F) {
                    iterator.remove();
                } else {
                    float f2 = Anim.easeOutQuad(f1);
                    double d0 = killfxfeature$soul.y + f2 * (2.2 + f * 1.4);
                    float f3 = Math.max(0.05F, Anim.easeLife(f1));
                    String yaw = killfxfeature$soul.style;

                    float f4 = switch (yaw) {
                        case "\u0412\u0437\u043b\u0451\u0442" -> 1.0F + f2 * 0.25F * f;
                        case "\u041f\u0440\u0438\u0437\u0440\u0430\u043a" -> 1.0F + (float)Math.sin(f1 * Math.PI) * 0.08F;
                        case "\u0418\u0438\u0441\u0443\u0441" -> 1.0F + f2 * 0.12F * f;
                        default -> 1.0F;
                    };
                    float f9 = killfxfeature$soul.yaw;
                    String s1 = killfxfeature$soul.style;

                    float f7 = f9 + f2 * switch (s1) {
                        case "\u041f\u0440\u0438\u0437\u0440\u0430\u043a" -> 120.0F;
                        case "\u0418\u0438\u0441\u0443\u0441" -> 20.0F;
                        default -> 35.0F;
                    };
                    Mesh.Pose mesh$pose = "\u0418\u0438\u0441\u0443\u0441".equals(killfxfeature$soul.style) ? Mesh.Pose.CRUCIFORM : Mesh.Pose.STAND;
                    float f8 = f3 * ("\u041f\u0440\u0438\u0437\u0440\u0430\u043a".equals(killfxfeature$soul.style) ? 0.7F : 0.95F);
                    int j = killfxfeature$soul.tint;
                    Mesh.playerModel(ctx, killfxfeature$soul.skin, killfxfeature$soul.x, d0, killfxfeature$soul.z, f7, f4, f8, mesh$pose, j);
                    float f5 = f3 * 0.45F;
                    if (f5 > 0.04F && VisualQuality.softOrbs()) {
                        float f6 = 0.35F + f2 * 0.25F * f;
                        Mesh.softOrb(ctx, killfxfeature$soul.x, d0 + 1.0, killfxfeature$soul.z, f6, VisualQuality.orbSegs(), Mesh.alpha(j, f5));
                        int k = Math.max(4, Math.round(8.0F * VisualQuality.particleMul()));

                        for (int l = 0; l < k; l++) {
                            double d1 = f1 * 4.5 + l * ((Math.PI * 2) / k);
                            double d2 = 0.35 + f2 * 0.55;
                            Mesh.softOrb(
                                ctx,
                                killfxfeature$soul.x + Math.cos(d1) * d2,
                                d0 + 0.4 + Math.sin(d1 * 2.0) * 0.2 + f2 * 0.6,
                                killfxfeature$soul.z + Math.sin(d1) * d2,
                                0.08F + 0.06F * (1.0F - f1),
                                VisualQuality.orbSegs(),
                                Mesh.alpha(j, f3 * 0.4F)
                            );
                        }
                    }
                }
            }
        }
    }

    private void spawn(LivingEntity e) {
        ThreadLocalRandom threadlocalrandom = ThreadLocalRandom.current();
        Vec3d vec3d = e.getEntityPos();
        if ((Boolean)this.sound.get()) {
            Sfx.playWorld(ModSounds.KILL_BOOM, vec3d.x, vec3d.y, vec3d.z, 0.7F, 0.85F + threadlocalrandom.nextFloat() * 0.3F);
        }

        KillFxFeature.Soul killfxfeature$soul = new KillFxFeature.Soul();
        killfxfeature$soul.x = vec3d.x;
        killfxfeature$soul.y = vec3d.y;
        killfxfeature$soul.z = vec3d.z;
        killfxfeature$soul.yaw = e.getYaw();
        killfxfeature$soul.born = System.currentTimeMillis();
        killfxfeature$soul.ttl = (int)(1500.0F + this.power.f() * 450.0F);
        killfxfeature$soul.style = (String)this.style.get();
        killfxfeature$soul.skin = skinOf(e);
        killfxfeature$soul.tint = this.skinTint.get() ? SkinColors.ofVictim(e, killfxfeature$soul.skin) : 16777215;
        this.souls.add(killfxfeature$soul);

        while (this.souls.size() > 8) {
            this.souls.remove(0);
        }

        SessionStats.kill();
        App.features().find(HudFeature.class).ifPresent(h -> h.islandEvent("\u041a\u0438\u043b\u043b", e.getName().getString()));
    }

    private static Identifier skinOf(LivingEntity e) {
        try {
            if (e instanceof AbstractClientPlayerEntity abstractclientplayerentity) {
                return abstractclientplayerentity.getSkin().body().texturePath();
            }

            if (e instanceof PlayerEntity) {
                return STEVE;
            }
        } catch (Throwable throwable) {
        }

        return STEVE;
    }

    private static final class Soul {
        double x;
        double y;
        double z;
        float yaw;
        long born;
        int ttl;
        String style;
        Identifier skin;
        int tint;
    }

    private static final class Watch {
        final LivingEntity target;
        final long at;

        Watch(LivingEntity target, long at) {
            this.target = target;
            this.at = at;
        }
    }
}
