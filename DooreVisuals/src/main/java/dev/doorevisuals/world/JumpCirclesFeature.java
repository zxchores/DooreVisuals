package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public final class JumpCirclesFeature extends Feature implements Tick {
    private final Opt.Pick who = this.opt(
        new Opt.Pick("who", "\u041a\u0442\u043e", "\u0421\u0435\u0431\u044f", "\u0421\u0435\u0431\u044f", "\u0412\u0441\u0435")
    );
    private final Opt.Num life = this.opt(
        new Opt.Num("life", "\u0414\u043b\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c", 0.85, 0.35, 2.0, 0.05)
    );
    private final Opt.Num radius = this.opt(new Opt.Num("radius", "\u0420\u0430\u0434\u0438\u0443\u0441", 1.35, 0.6, 2.8, 0.05));
    private final Opt.Num thickness = this.opt(new Opt.Num("thickness", "\u0422\u043e\u043b\u0449\u0438\u043d\u0430", 0.045, 0.02, 0.12, 0.005));
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.themeColor.get()));
    private final Opt.Num alpha = this.opt(
        new Opt.Num("alpha", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 0.75, 0.25, 1.0, 0.05)
    );
    private final List<JumpCirclesFeature.Ring> rings = new ArrayList<>();
    private boolean selfWasGround = true;
    private final HashMap<UUID, Boolean> groundMap = new HashMap<>();

    public JumpCirclesFeature() {
        super(
            "jump_circles",
            "Jump Circles",
            "\u041a\u043e\u043b\u044c\u0446\u0430 \u043d\u0430 \u0437\u0435\u043c\u043b\u0435 \u043f\u0440\u0438 \u043f\u0440\u044b\u0436\u043a\u0435",
            Category.WORLD,
            true
        );
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.world != null && mc.player != null) {
            ClientPlayerEntity clientplayerentity = mc.player;
            boolean flag = "\u0421\u0435\u0431\u044f".equals(this.who.get());
            long i = System.currentTimeMillis();
            float f = this.life.f() * 1000.0F;
            this.pulse(clientplayerentity, clientplayerentity.isOnGround(), this.selfWasGround, i, f);
            this.selfWasGround = clientplayerentity.isOnGround();
            if (!flag) {
                for (AbstractClientPlayerEntity abstractclientplayerentity : mc.world.getPlayers()) {
                    if (abstractclientplayerentity != clientplayerentity) {
                        boolean flag1 = this.groundMap.getOrDefault(abstractclientplayerentity.getUuid(), true);
                        this.pulse(abstractclientplayerentity, abstractclientplayerentity.isOnGround(), flag1, i, f);
                        this.groundMap.put(abstractclientplayerentity.getUuid(), abstractclientplayerentity.isOnGround());
                    }
                }

                if (this.groundMap.size() > 64) {
                    this.groundMap.keySet().removeIf(id -> mc.world.getPlayerByUuid(id) == null);
                }
            }

            Iterator<JumpCirclesFeature.Ring> iterator = this.rings.iterator();

            while (iterator.hasNext()) {
                if ((float)(i - iterator.next().born) > f) {
                    iterator.remove();
                }
            }

            while (this.rings.size() > 48) {
                this.rings.remove(0);
            }
        } else {
            this.rings.clear();
        }
    }

    private void pulse(AbstractClientPlayerEntity p, boolean onGround, boolean wasGround, long now, float ttl) {
        if (wasGround && !onGround && p.getVelocity().y > 0.03) {
            this.rings.add(new JumpCirclesFeature.Ring(p.getX(), p.getY(), p.getZ(), now, ttl, p.getWidth() * 0.55));
        }
    }

    public void draw(WorldRenderContext ctx) {
        if (this.on() && !this.rings.isEmpty()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
            float f = this.alpha.f();
            float f1 = this.radius.f();
            float f2 = this.thickness.f();
            long j = System.currentTimeMillis();
            int k = VisualQuality.segs(48);
            int l = VisualQuality.tubeSegs(10);
            float f3 = VisualQuality.maxFxDistSq();

            for (JumpCirclesFeature.Ring jumpcirclesfeature$ring : this.rings) {
                if (minecraftclient.player != null) {
                    double d0 = jumpcirclesfeature$ring.x - minecraftclient.player.getX();
                    double d1 = jumpcirclesfeature$ring.y - minecraftclient.player.getY();
                    double d3 = jumpcirclesfeature$ring.z - minecraftclient.player.getZ();
                    if (d0 * d0 + d1 * d1 + d3 * d3 > f3) {
                        continue;
                    }
                }

                float f7 = MathHelper.clamp((float)(j - jumpcirclesfeature$ring.born) / jumpcirclesfeature$ring.life, 0.0F, 1.0F);
                float f4 = Anim.easeOutQuad(f7);
                float f8 = 1.0F - f7;
                f8 *= f8;
                double d2 = jumpcirclesfeature$ring.seed + f4 * f1;
                float f5 = f8 * f;
                Mesh.disc(
                    ctx,
                    jumpcirclesfeature$ring.x,
                    jumpcirclesfeature$ring.y + 0.02,
                    jumpcirclesfeature$ring.z,
                    Math.max(0.02, d2 - f2 * 3.5),
                    d2 + f2 * 0.5,
                    k,
                    Mesh.alpha(i, 0.12F * f5),
                    Types.glow()
                );
                Mesh.torus(
                    ctx,
                    jumpcirclesfeature$ring.x,
                    jumpcirclesfeature$ring.y + 0.03,
                    jumpcirclesfeature$ring.z,
                    d2,
                    f2,
                    k,
                    l,
                    Mesh.alpha(i, 0.55F * f5),
                    Types.glow()
                );
                Mesh.ring(
                    ctx,
                    jumpcirclesfeature$ring.x,
                    jumpcirclesfeature$ring.y + 0.035,
                    jumpcirclesfeature$ring.z,
                    d2,
                    Math.max(16, k - 8),
                    Mesh.alpha(i, 0.85F * f5)
                );
                if (f7 < 0.35F && VisualQuality.softOrbs()) {
                    float f6 = (0.35F - f7) / 0.35F;
                    Mesh.softOrb(
                        ctx,
                        jumpcirclesfeature$ring.x,
                        jumpcirclesfeature$ring.y + 0.05,
                        jumpcirclesfeature$ring.z,
                        (float)(d2 * 0.35),
                        VisualQuality.orbSegs(),
                        Mesh.alpha(i, 0.22F * f6 * f)
                    );
                }
            }
        }
    }

    @Override
    protected void disable() {
        this.rings.clear();
        this.groundMap.clear();
    }

    private record Ring(double x, double y, double z, long born, float life, double seed) {
    }
}
