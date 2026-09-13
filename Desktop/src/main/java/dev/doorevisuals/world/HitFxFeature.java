package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Sprites;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class HitFxFeature extends Feature implements Tick {
    private final Opt.Pick kind = this.opt(
        new Opt.Pick(
            "kind",
            "\u0412\u0438\u0434",
            "\u0414\u043e\u043b\u043b\u0430\u0440\u044b",
            "\u0414\u043e\u043b\u043b\u0430\u0440\u044b",
            "\u0417\u0432\u0451\u0437\u0434\u043e\u0447\u043a\u0438",
            "\u0421\u0435\u0440\u0434\u0435\u0447\u043a\u0438",
            "\u0422\u043e\u0447\u043a\u0438",
            "67",
            "\u0418\u043a\u043e\u043d\u043a\u0430",
            "Glow"
        )
    );
    private final Opt.Num amount = this.opt(new Opt.Num("amount", "\u041a\u043e\u043b-\u0432\u043e", 18.0, 6.0, 40.0, 1.0));
    private final Opt.Num size = this.opt(new Opt.Num("size", "\u0420\u0430\u0437\u043c\u0435\u0440", 1.0, 0.4, 2.0, 0.05));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -1));
    private final List<HitFxFeature.P> parts = new ArrayList<>();
    private boolean bound;

    public HitFxFeature() {
        super(
            "particles",
            "Hit FX",
            "\u0427\u0430\u0441\u0442\u0438\u0446\u044b \u043f\u0440\u0438 \u0443\u0434\u0430\u0440\u0435 \u043f\u043e \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0438",
            Category.WORLD,
            true
        );
    }

    public void bind() {
        if (!this.bound) {
            this.bound = true;
            AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
                if (level.isClient() && this.on() && entity instanceof LivingEntity livingentity) {
                    this.burst(livingentity);
                }

                return ActionResult.PASS;
            });
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
    }

    public void draw(WorldRenderContext ctx) {
        if (this.on() && !this.parts.isEmpty()) {
            long i = System.currentTimeMillis();
            Iterator<HitFxFeature.P> iterator = this.parts.iterator();

            while (iterator.hasNext()) {
                HitFxFeature.P hitfxfeature$p = iterator.next();
                float f = (float)(i - hitfxfeature$p.born) / hitfxfeature$p.ttl;
                if (f >= 1.0F) {
                    iterator.remove();
                } else {
                    float f1 = 0.05F;
                    hitfxfeature$p.vy = hitfxfeature$p.vy + hitfxfeature$p.grav * f1;
                    hitfxfeature$p.x = hitfxfeature$p.x + hitfxfeature$p.vx * f1;
                    hitfxfeature$p.y = hitfxfeature$p.y + hitfxfeature$p.vy * f1;
                    hitfxfeature$p.z = hitfxfeature$p.z + hitfxfeature$p.vz * f1;
                    hitfxfeature$p.vx = hitfxfeature$p.vx * hitfxfeature$p.drag;
                    hitfxfeature$p.vz = hitfxfeature$p.vz * hitfxfeature$p.drag;
                    float f2 = (1.0F - f) * hitfxfeature$p.alpha;
                    float f3 = hitfxfeature$p.size * (0.75F + 0.35F * (1.0F - f));
                    int j = Mesh.alpha(hitfxfeature$p.color, f2);
                    if (hitfxfeature$p.soft) {
                        Mesh.orb(ctx, hitfxfeature$p.x, hitfxfeature$p.y, hitfxfeature$p.z, f3 * 1.45F, Mesh.alpha(hitfxfeature$p.color, f2 * 0.4F));
                        Mesh.orb(ctx, hitfxfeature$p.x, hitfxfeature$p.y, hitfxfeature$p.z, f3, j);
                    } else {
                        float f4 = hitfxfeature$p.digit ? f3 * 1.45F : f3;
                        Mesh.billboard(ctx, hitfxfeature$p.tex, hitfxfeature$p.x, hitfxfeature$p.y, hitfxfeature$p.z, f3, f4, j);
                    }
                }
            }
        }
    }

    private void burst(LivingEntity e) {
        Vec3d vec3d = e.getEntityPos().add(0.0, e.getHeight() * 0.55, 0.0);
        int i = this.amount.i();
        String s = (String)this.kind.get();
        boolean flag = "67".equals(s);
        boolean flag1 = "Glow".equals(s) || "\u0422\u043e\u0447\u043a\u0438".equals(s);
        ThreadLocalRandom threadlocalrandom = ThreadLocalRandom.current();
        float f = this.size.f();
        int j = (Integer)this.color.get();

        for (int k = 0; k < i; k++) {
            HitFxFeature.P hitfxfeature$p = new HitFxFeature.P();
            hitfxfeature$p.x = vec3d.x + (threadlocalrandom.nextFloat() - 0.5F) * 0.3F;
            hitfxfeature$p.y = vec3d.y + threadlocalrandom.nextFloat() * 0.2F;
            hitfxfeature$p.z = vec3d.z + (threadlocalrandom.nextFloat() - 0.5F) * 0.3F;
            hitfxfeature$p.born = System.currentTimeMillis();
            hitfxfeature$p.color = j;
            hitfxfeature$p.soft = flag1;
            if (flag) {
                hitfxfeature$p.digit = true;
                hitfxfeature$p.tex = threadlocalrandom.nextBoolean() ? Sprites.DIGIT_6 : Sprites.DIGIT_7;
                hitfxfeature$p.size = (0.12F + threadlocalrandom.nextFloat() * 0.14F) * f;
            } else if ("\u0421\u0435\u0440\u0434\u0435\u0447\u043a\u0438".equals(s)) {
                hitfxfeature$p.digit = false;
                hitfxfeature$p.tex = Sprites.HEART;
                hitfxfeature$p.size = (0.1F + threadlocalrandom.nextFloat() * 0.1F) * f;
                hitfxfeature$p.grav = -0.05F;
            } else if ("\u0417\u0432\u0451\u0437\u0434\u043e\u0447\u043a\u0438".equals(s)) {
                hitfxfeature$p.digit = false;
                hitfxfeature$p.tex = Sprites.STAR;
                hitfxfeature$p.size = (0.09F + threadlocalrandom.nextFloat() * 0.12F) * f;
            } else if (flag1) {
                hitfxfeature$p.digit = false;
                hitfxfeature$p.tex = Sprites.GLOW;
                hitfxfeature$p.size = (0.07F + threadlocalrandom.nextFloat() * 0.12F) * f;
            } else if ("\u0418\u043a\u043e\u043d\u043a\u0430".equals(s)) {
                hitfxfeature$p.digit = false;
                hitfxfeature$p.tex = Sprites.ICON;
                hitfxfeature$p.size = (0.11F + threadlocalrandom.nextFloat() * 0.1F) * f;
            } else {
                hitfxfeature$p.digit = false;
                hitfxfeature$p.tex = Sprites.DOLLAR;
                hitfxfeature$p.size = (0.08F + threadlocalrandom.nextFloat() * 0.1F) * f;
            }

            float f1 = threadlocalrandom.nextFloat() * (float) (Math.PI * 2);
            float f2 = (threadlocalrandom.nextFloat() - 0.25F) * 1.7278761F;
            float f3 = 0.14F + threadlocalrandom.nextFloat() * 0.28F;
            hitfxfeature$p.vx = MathHelper.cos(f1) * MathHelper.cos(f2) * f3;
            hitfxfeature$p.vz = MathHelper.sin(f1) * MathHelper.cos(f2) * f3;
            hitfxfeature$p.vy = MathHelper.sin(f2) * f3 + 0.06F + threadlocalrandom.nextFloat() * 0.1F;
            if (hitfxfeature$p.grav == 0.0F) {
                hitfxfeature$p.grav = -0.06F - threadlocalrandom.nextFloat() * 0.04F;
            }

            hitfxfeature$p.drag = 0.985F;
            hitfxfeature$p.ttl = 2600 + threadlocalrandom.nextInt(1400);
            hitfxfeature$p.alpha = 0.95F;
            this.parts.add(hitfxfeature$p);
        }

        while (this.parts.size() > 220) {
            this.parts.remove(0);
        }
    }

    private static final class P {
        double x;
        double y;
        double z;
        float vx;
        float vy;
        float vz;
        float grav;
        float drag;
        float size;
        float alpha;
        int color;
        int ttl;
        long born;
        Identifier tex;
        boolean digit;
        boolean soft;
    }
}
