package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class WorldParticlesFeature extends Feature implements Tick {
    private final Opt.Pick style = this.opt(
        new Opt.Pick(
            "style",
            "\u0421\u0442\u0438\u043b\u044c",
            "\u0421\u043d\u0435\u0433",
            "\u041e\u0440\u0431\u0438\u0442\u044b",
            "\u0421\u043d\u0435\u0433",
            "\u0418\u0441\u043a\u0440\u044b",
            "\u041f\u044b\u043b\u044c",
            "\u0411\u0430\u0431\u043e\u0447\u043a\u0438",
            "\u041f\u0435\u043f\u0435\u043b"
        )
    );
    private final Opt.Num radius = this.opt(new Opt.Num("radius", "\u0420\u0430\u0434\u0438\u0443\u0441", 14.0, 4.0, 40.0, 0.5));
    private final Opt.Num speed = this.opt(new Opt.Num("speed", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", 1.0, 0.3, 2.5, 0.05));
    private final Opt.Num size = this.opt(new Opt.Num("size", "\u0420\u0430\u0437\u043c\u0435\u0440", 0.16, 0.05, 0.5, 0.01));
    private final Opt.Num length = this.opt(
        new Opt.Num("length", "\u0414\u043b\u0438\u043d\u0430 \u0441\u043b\u0435\u0434\u0430", 18.0, 8.0, 36.0, 1.0)
            .visibleWhen(() -> "\u041e\u0440\u0431\u0438\u0442\u044b".equals(this.style.get()))
    );
    private final Opt.Num count = this.opt(
        new Opt.Num("count", "\u041a\u043e\u043b-\u0432\u043e", 72.0, 16.0, 160.0, 1.0)
            .visibleWhen(() -> !"\u041e\u0440\u0431\u0438\u0442\u044b".equals(this.style.get()))
    );
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme", "\u0422\u0435\u043c\u0430", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.themeColor.get()));
    private final List<WorldParticlesFeature.W> free = new ArrayList<>();
    private long born = System.currentTimeMillis();

    public WorldParticlesFeature() {
        super(
            "world_particles",
            "World Particles",
            "\u0421\u043d\u0435\u0433, \u0438\u0441\u043a\u0440\u044b, \u043f\u044b\u043b\u044c \u0438 \u043e\u0440\u0431\u0438\u0442\u044b \u0432 \u043e\u0431\u044a\u0451\u043c\u0435 \u043a\u0430\u043c\u0435\u0440\u044b",
            Category.WORLD,
            false
        );
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null && mc.world != null) {
            if ("\u041e\u0440\u0431\u0438\u0442\u044b".equals(this.style.get())) {
                this.free.clear();
            } else {
                int i = this.count.i();
                ThreadLocalRandom threadlocalrandom = ThreadLocalRandom.current();
                Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
                double d0 = Math.max(8.0, (Double)this.radius.get());
                double d1 = vec3d.y - d0 * 0.45;
                double d2 = vec3d.y + d0 * 0.9;

                while (this.free.size() < i) {
                    WorldParticlesFeature.W worldparticlesfeature$w = new WorldParticlesFeature.W();
                    worldparticlesfeature$w.x = vec3d.x + (threadlocalrandom.nextDouble() - 0.5) * d0 * 2.0;
                    worldparticlesfeature$w.y = d1 + threadlocalrandom.nextDouble() * (d2 - d1);
                    worldparticlesfeature$w.z = vec3d.z + (threadlocalrandom.nextDouble() - 0.5) * d0 * 2.0;
                    worldparticlesfeature$w.size = this.size.f() * (0.7F + threadlocalrandom.nextFloat() * 0.6F);
                    worldparticlesfeature$w.phase = threadlocalrandom.nextFloat() * 6.2F;
                    worldparticlesfeature$w.color = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
                    worldparticlesfeature$w.vx = (threadlocalrandom.nextFloat() - 0.5F) * 0.04F * this.speed.f();
                    worldparticlesfeature$w.vy = (
                            !"\u0421\u043d\u0435\u0433".equals(this.style.get()) && !"\u041f\u0435\u043f\u0435\u043b".equals(this.style.get()) ? 0.01F : -0.03F
                        )
                        * this.speed.f();
                    worldparticlesfeature$w.vz = (threadlocalrandom.nextFloat() - 0.5F) * 0.04F * this.speed.f();
                    this.free.add(worldparticlesfeature$w);
                }

                while (this.free.size() > i) {
                    this.free.remove(this.free.size() - 1);
                }

                for (WorldParticlesFeature.W worldparticlesfeature$w1 : this.free) {
                    worldparticlesfeature$w1.x = worldparticlesfeature$w1.x + worldparticlesfeature$w1.vx;
                    worldparticlesfeature$w1.y = worldparticlesfeature$w1.y + worldparticlesfeature$w1.vy;
                    worldparticlesfeature$w1.z = worldparticlesfeature$w1.z + worldparticlesfeature$w1.vz;
                    if (worldparticlesfeature$w1.x < vec3d.x - d0
                        || worldparticlesfeature$w1.x > vec3d.x + d0
                        || worldparticlesfeature$w1.z < vec3d.z - d0
                        || worldparticlesfeature$w1.z > vec3d.z + d0
                        || worldparticlesfeature$w1.y < d1
                        || worldparticlesfeature$w1.y > d2) {
                        worldparticlesfeature$w1.x = vec3d.x + (threadlocalrandom.nextDouble() - 0.5) * d0 * 2.0;
                        worldparticlesfeature$w1.y = d1 + threadlocalrandom.nextDouble() * (d2 - d1);
                        worldparticlesfeature$w1.z = vec3d.z + (threadlocalrandom.nextDouble() - 0.5) * d0 * 2.0;
                    }
                }
            }
        } else {
            this.free.clear();
        }
    }

    public void draw(WorldRenderContext ctx) {
        if (this.on()) {
            if ("\u041e\u0440\u0431\u0438\u0442\u044b".equals(this.style.get())) {
                this.drawOrbits(ctx);
            } else {
                this.drawFree(ctx);
            }
        }
    }

    private void drawFree(WorldRenderContext ctx) {
        if (!this.free.isEmpty()) {
            long i = System.currentTimeMillis();
            String s = (String)this.style.get();

            for (WorldParticlesFeature.W worldparticlesfeature$w : this.free) {
                float f = 0.55F + 0.45F * (0.5F + 0.5F * (float)Math.sin(i * 0.003 + worldparticlesfeature$w.phase));
                if ("\u0411\u0430\u0431\u043e\u0447\u043a\u0438".equals(s)) {
                    double d0 = Math.sin(i * 0.004 + worldparticlesfeature$w.phase) * 0.1;
                    double d1 = Math.cos(i * 0.004 + worldparticlesfeature$w.phase) * 0.1;
                    int j = Mesh.alpha(worldparticlesfeature$w.color, f * 0.9F);
                    Mesh.orb(
                        ctx, worldparticlesfeature$w.x + d0, worldparticlesfeature$w.y, worldparticlesfeature$w.z + d1, worldparticlesfeature$w.size * 0.85F, j
                    );
                    Mesh.orb(
                        ctx, worldparticlesfeature$w.x - d0, worldparticlesfeature$w.y, worldparticlesfeature$w.z - d1, worldparticlesfeature$w.size * 0.85F, j
                    );
                } else if ("\u0418\u0441\u043a\u0440\u044b".equals(s)) {
                    int k = Theme.tintMul(-16256, worldparticlesfeature$w.color, 0.55F);
                    Mesh.orb(
                        ctx,
                        worldparticlesfeature$w.x,
                        worldparticlesfeature$w.y,
                        worldparticlesfeature$w.z,
                        worldparticlesfeature$w.size * 1.15F,
                        Mesh.alpha(k, f * 0.45F)
                    );
                    Mesh.orb(
                        ctx,
                        worldparticlesfeature$w.x,
                        worldparticlesfeature$w.y,
                        worldparticlesfeature$w.z,
                        worldparticlesfeature$w.size * 0.7F,
                        Mesh.alpha(k, f)
                    );
                } else if ("\u0421\u043d\u0435\u0433".equals(s)) {
                    int l = Theme.tintMul(-1510145, worldparticlesfeature$w.color, 0.35F);
                    Mesh.orb(
                        ctx,
                        worldparticlesfeature$w.x,
                        worldparticlesfeature$w.y,
                        worldparticlesfeature$w.z,
                        worldparticlesfeature$w.size * 0.55F,
                        Mesh.alpha(l, f * 0.9F)
                    );
                } else if ("\u041f\u0435\u043f\u0435\u043b".equals(s)) {
                    int i1 = Theme.tintMul(-9811398, worldparticlesfeature$w.color, 0.4F);
                    Mesh.orb(
                        ctx,
                        worldparticlesfeature$w.x,
                        worldparticlesfeature$w.y,
                        worldparticlesfeature$w.z,
                        worldparticlesfeature$w.size * 0.48F,
                        Mesh.alpha(i1, f * 0.75F)
                    );
                } else {
                    Mesh.orb(
                        ctx,
                        worldparticlesfeature$w.x,
                        worldparticlesfeature$w.y,
                        worldparticlesfeature$w.z,
                        worldparticlesfeature$w.size,
                        Mesh.alpha(worldparticlesfeature$w.color, f * 0.55F)
                    );
                }
            }
        }
    }

    private void drawOrbits(WorldRenderContext ctx) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null) {
            float f;
            try {
                f = minecraftclient.getRenderTickCounter().getTickProgress(false);
            } catch (Throwable throwable) {
                f = 0.0F;
            }

            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            double d0 = MathHelper.lerp(f, clientplayerentity.lastX, clientplayerentity.getX());
            double d1 = MathHelper.lerp(f, clientplayerentity.lastY, clientplayerentity.getY()) + clientplayerentity.getHeight() * 0.55;
            double d2 = MathHelper.lerp(f, clientplayerentity.lastZ, clientplayerentity.getZ());
            int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
            double d3 = Math.max(1.2, (Double)this.radius.get() * 0.12) * Math.max(0.85, (double)clientplayerentity.getWidth());
            float f1 = this.size.f() * 0.5F;
            int j = this.length.i();
            float f2 = 27.0F / Math.max(0.2F, this.speed.f());
            long k = System.currentTimeMillis();

            for (int l = 0; l < 2; l++) {
                double d4 = d3 * (0.75 + l * 0.32);
                float f3 = 1.0F - l * 0.22F;

                for (int i1 = 0; i1 < 4; i1++) {
                    double d5 = i1 * (Math.PI / 2);

                    for (int j1 = 0; j1 < j; j1++) {
                        double d6 = 0.15 * (k - this.born - j1 * 12) / f2 + d5;
                        double d7 = Math.sin(d6) * d4;
                        double d8 = Math.cos(d6) * d4;
                        int k1 = i1 % 3;
                        double d9;
                        double d10;
                        double d11;
                        switch (k1) {
                            case 0:
                                d9 = d0 + d7;
                                d10 = d1 + d8 * 0.75;
                                d11 = d2 - d8;
                                break;
                            case 1:
                                d9 = d0 - d7;
                                d10 = d1 + d7 * 0.55;
                                d11 = d2 - d8;
                                break;
                            default:
                                d9 = d0 - d7 * 0.8;
                                d10 = d1 - d7 * 0.4;
                                d11 = d2 + d8;
                        }

                        float f4 = Math.max(0.0F, 1.0F - (float)j1 / j);
                        float f5 = f4 * f4 * 0.7F * f3;
                        if (!(f5 < 0.05F)) {
                            float f6 = f1 * (0.45F + 0.5F * f4) * f3;
                            Mesh.orb(ctx, d9, d10, d11, f6 * 1.45F, Mesh.alpha(i, f5 * 0.4F));
                            Mesh.orb(ctx, d9, d10, d11, f6, Mesh.alpha(i, f5));
                        }
                    }
                }
            }
        }
    }

    private static final class W {
        double x;
        double y;
        double z;
        float vx;
        float vy;
        float vz;
        float size;
        float phase;
        int color;
    }
}
