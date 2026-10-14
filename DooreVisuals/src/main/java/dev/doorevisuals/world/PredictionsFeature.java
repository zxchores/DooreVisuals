package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import java.util.ArrayList;
import java.util.Collections;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class PredictionsFeature extends Feature {
    private final Opt.Flag players = this.opt(new Opt.Flag("players", "\u0418\u0433\u0440\u043e\u043a\u0438", true));
    private final Opt.Flag projectiles = this.opt(new Opt.Flag("projectiles", "\u0421\u043d\u0430\u0440\u044f\u0434\u044b", true));
    private final Opt.Flag throwPredict = this.opt(new Opt.Flag("throw", "\u041f\u0440\u0435\u0432\u044c\u044e \u0431\u0440\u043e\u0441\u043a\u0430", true));
    private final Opt.Flag pearlLand = this.opt(new Opt.Flag("pearl_land", "\u0422\u043e\u0447\u043a\u0430 \u043f\u0435\u0440\u043b\u0430", true));
    private final Opt.Flag projTrails = this.opt(
        new Opt.Flag("proj_trails", "\u041b\u0435\u043d\u0442\u044b \u0441\u043d\u0430\u0440\u044f\u0434\u043e\u0432", true)
    );
    private final Opt.Num ticks = this.opt(new Opt.Num("ticks", "\u0422\u0438\u043a\u0438 (\u0438\u0433\u0440\u043e\u043a\u0438)", 4.0, 1.0, 12.0, 1.0));
    private final Opt.Num range = this.opt(new Opt.Num("range", "\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c", 48.0, 8.0, 96.0, 1.0));
    private final Opt.Flag onlyTarget = this.opt(new Opt.Flag("target", "\u0422\u043e\u043b\u044c\u043a\u043e \u0446\u0435\u043b\u044c", false));
    private final Opt.Flag trail = this.opt(new Opt.Flag("trail", "\u0421\u043b\u0435\u0434", true));
    private final Opt.Num arcSteps = this.opt(new Opt.Num("arc_steps", "\u0414\u043b\u0438\u043d\u0430 \u0434\u0443\u0433\u0438", 60.0, 20.0, 120.0, 5.0));
    private final Opt.Flag useTheme = this.opt(new Opt.Flag("theme", "\u0426\u0432\u0435\u0442 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.useTheme.get()));
    private final Opt.Num alpha = this.opt(
        new Opt.Num("alpha", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 0.55, 0.15, 1.0, 0.05)
    );
    private final Opt.Num beadSize = this.opt(
        new Opt.Num("bead", "\u0420\u0430\u0437\u043c\u0435\u0440 \u0447\u0430\u0441\u0442\u0438\u0446", 0.22, 0.1, 0.5, 0.01)
    );
    private long born = System.currentTimeMillis();

    public PredictionsFeature() {
        super(
            "predictions",
            "\u0422\u0440\u0430\u0435\u043a\u0442\u043e\u0440\u0438\u0438",
            "\u041f\u043e\u0437\u0438\u0446\u0438\u044f \u0438\u0433\u0440\u043e\u043a\u043e\u0432, \u043f\u0435\u0440\u043b \u0438 \u043b\u0435\u043d\u0442\u044b \u0441\u043d\u0430\u0440\u044f\u0434\u043e\u0432",
            Category.WORLD,
            true
        );
    }

    public void draw(WorldRenderContext ctx, float tickDelta) {
        if (this.on()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.world != null && minecraftclient.player != null) {
                int i = this.useTheme.get() ? Theme.EFFECT : (Integer)this.color.get();
                float f = this.alpha.f();
                if ((Boolean)this.players.get()) {
                    this.drawPlayers(ctx, minecraftclient, tickDelta, i, f);
                }

                if ((Boolean)this.projectiles.get()) {
                    this.drawProjectiles(ctx, minecraftclient, tickDelta, i, f);
                }

                if ((Boolean)this.throwPredict.get()) {
                    this.drawThrowPreview(ctx, minecraftclient, i, f);
                }
            }
        }
    }

    private void drawPlayers(WorldRenderContext ctx, MinecraftClient mc, float tickDelta, int base, float a) {
        double d0 = Math.min((Double)this.range.get() * (Double)this.range.get(), (double)VisualQuality.maxFxDistSq());
        LivingEntity livingentity = this.resolveTarget();
        int i = Math.max(1, this.ticks.i());
        float f = Anim.timeSec() - (float)this.born * 0.001F;
        float f1 = this.beadSize.f();
        int j = VisualQuality.segs(28);

        for (AbstractClientPlayerEntity abstractclientplayerentity : mc.world.getPlayers()) {
            if (abstractclientplayerentity != mc.player
                && abstractclientplayerentity.isAlive()
                && (!(Boolean)this.onlyTarget.get() || livingentity != null && abstractclientplayerentity == livingentity)
                && !WorldFx.espOwns(abstractclientplayerentity)
                && !(mc.player.squaredDistanceTo(abstractclientplayerentity) > d0)
                && WorldVis.canSee(mc.player, abstractclientplayerentity)) {
                Vec3d vec3d = abstractclientplayerentity.getVelocity();
                double d1 = vec3d.horizontalLength();
                if (!(d1 < 0.02) || !(Math.abs(vec3d.y) < 0.02)) {
                    double d2 = MathHelper.lerp(tickDelta, abstractclientplayerentity.lastX, abstractclientplayerentity.getX());
                    double d3 = MathHelper.lerp(tickDelta, abstractclientplayerentity.lastY, abstractclientplayerentity.getY());
                    double d4 = MathHelper.lerp(tickDelta, abstractclientplayerentity.lastZ, abstractclientplayerentity.getZ());
                    double d5 = d2 + vec3d.x * i;
                    double d6 = d3 + vec3d.y * i;
                    double d7 = d4 + vec3d.z * i;
                    float f2 = abstractclientplayerentity.getWidth() * 0.55F;
                    float f3 = abstractclientplayerentity.getHeight();
                    float f4 = Math.max(0.28F, f2 * 0.95F);
                    double d8 = d6 + f3 * 0.45;
                    Mesh.torus(ctx, d5, d8, d7, f4, 0.04F, j, VisualQuality.tubeSegs(8), Mesh.alpha(base, 0.28F * a), Types.ghost());
                    Mesh.ring(ctx, d5, d8, d7, f4, j, Mesh.alpha(base, 0.45F * a));
                    if (VisualQuality.softOrbs()) {
                        int k = Math.max(4, Math.round(8.0F * VisualQuality.particleMul()));

                        for (int l = 0; l < k; l++) {
                            double d9 = f * 2.4 + l * ((Math.PI * 2) / k);
                            double d10 = f4 * (1.05 + 0.12 * Math.sin(f * 3.0F + l));
                            double d11 = d5 + Math.cos(d9) * d10;
                            double d13 = d8 + Math.sin(d9 * 2.0 + f) * f3 * 0.12;
                            double d15 = d7 + Math.sin(d9) * d10;
                            float f5 = 0.35F + 0.45F * (0.5F + 0.5F * (float)Math.sin(d9 * 2.0 + f));
                            Mesh.softOrb(ctx, d11, d13, d15, f1 * 0.4F, VisualQuality.orbSegs(), Mesh.alpha(shift(base, l * 7), f5 * a));
                        }
                    }

                    if ((Boolean)this.trail.get() && i > 1) {
                        double d18 = d3 + f3 * 0.5;

                        for (int i1 = 1; i1 <= i; i1++) {
                            double d12 = (double)i1 / i;
                            double d14 = d2 + vec3d.x * i1;
                            double d16 = d3 + vec3d.y * i1 + f3 * 0.5;
                            double d17 = d4 + vec3d.z * i1;
                            Mesh.orb(ctx, d14, d16, d17, f1 * (0.35F + 0.45F * (float)d12), Mesh.alpha(shift(base, i1 * 5), (0.25F + 0.5F * (float)d12) * a));
                        }
                    }
                }
            }
        }
    }

    private void drawProjectiles(WorldRenderContext ctx, MinecraftClient mc, float tickDelta, int base, float a) {
        int i = Math.max(20, this.arcSteps.i());
        double d0 = (Double)this.range.get() * (Double)this.range.get();

        for (Entity entity : mc.world.getEntities()) {
            if (!(mc.player.squaredDistanceTo(entity) > d0)) {
                if (entity instanceof ThrownEntity thrownentity) {
                    Vec3d vec3d = new Vec3d(
                        MathHelper.lerp(tickDelta, thrownentity.lastX, thrownentity.getX()),
                        MathHelper.lerp(tickDelta, thrownentity.lastY, thrownentity.getY()),
                        MathHelper.lerp(tickDelta, thrownentity.lastZ, thrownentity.getZ())
                    );
                    this.drawArc(ctx, mc, vec3d, thrownentity.getVelocity(), 0.03, 0.99F, i, base, a, (Boolean)this.pearlLand.get());
                    if ((Boolean)this.projTrails.get()) {
                        this.drawProjRibbon(ctx, thrownentity, tickDelta, base, a);
                    }
                } else if (entity instanceof PersistentProjectileEntity persistentprojectileentity && !persistentprojectileentity.isOnGround()) {
                    Vec3d vec3d1 = new Vec3d(
                        MathHelper.lerp(tickDelta, persistentprojectileentity.lastX, persistentprojectileentity.getX()),
                        MathHelper.lerp(tickDelta, persistentprojectileentity.lastY, persistentprojectileentity.getY()),
                        MathHelper.lerp(tickDelta, persistentprojectileentity.lastZ, persistentprojectileentity.getZ())
                    );
                    this.drawArc(ctx, mc, vec3d1, persistentprojectileentity.getVelocity(), 0.05, 0.99F, i, base, a, false);
                    if ((Boolean)this.projTrails.get()) {
                        this.drawProjRibbon(ctx, persistentprojectileentity, tickDelta, base, a);
                    }
                }
            }
        }
    }

    private void drawThrowPreview(WorldRenderContext ctx, MinecraftClient mc, int base, float a) {
        ClientPlayerEntity clientplayerentity = mc.player;
        if (clientplayerentity != null) {
            boolean flag = clientplayerentity.getMainHandStack().isOf(Items.ENDER_PEARL) || clientplayerentity.getOffHandStack().isOf(Items.ENDER_PEARL);
            boolean flag1 = clientplayerentity.isUsingItem()
                && (clientplayerentity.getActiveItem().isOf(Items.BOW) || clientplayerentity.getActiveItem().isOf(Items.CROSSBOW));
            if (flag || flag1) {
                Vec3d vec3d = clientplayerentity.getCameraPosVec(1.0F);
                Vec3d vec3d1 = clientplayerentity.getRotationVector();
                if (flag) {
                    Vec3d vec3d2 = vec3d1.multiply(1.5);
                    this.drawArc(ctx, mc, vec3d, vec3d2, 0.03, 0.99F, Math.max(20, this.arcSteps.i()), base, a * 0.85F, (Boolean)this.pearlLand.get());
                } else {
                    float f1 = MathHelper.clamp(clientplayerentity.getItemUseTime() / 20.0F, 0.0F, 1.0F);
                    float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                    if (f < 0.1F) {
                        return;
                    }

                    f = Math.min(1.0F, f);
                    Vec3d vec3d3 = vec3d1.multiply(f * 3.0);
                    this.drawArc(ctx, mc, vec3d, vec3d3, 0.05, 0.99F, Math.max(20, this.arcSteps.i()), base, a * 0.75F, false);
                }
            }
        }
    }

    private void drawArc(
        WorldRenderContext ctx, MinecraftClient mc, Vec3d start, Vec3d velocity, double gravity, float drag, int maxSteps, int base, float a, boolean landing
    ) {
        Vec3d vec3d = start;
        Vec3d vec3d1 = velocity;
        boolean flag = false;
        Vec3d vec3d2 = start;
        float f = Anim.timeSec() - (float)this.born * 0.001F;
        float f1 = this.beadSize.f();
        int i = Math.max(12, Math.round(maxSteps * VisualQuality.particleMul()));
        Vec3d[] avec3d = new Vec3d[i + 1];
        int j = 0;
        avec3d[j++] = start;

        for (int k = 0; k < i; k++) {
            vec3d1 = vec3d1.add(0.0, -gravity, 0.0).multiply(drag);
            Vec3d vec3d3 = vec3d.add(vec3d1);
            BlockHitResult blockhitresult = mc.world
                .raycast(new RaycastContext(vec3d, vec3d3, ShapeType.COLLIDER, FluidHandling.NONE, mc.player));
            if (blockhitresult.getType() != Type.MISS) {
                vec3d3 = blockhitresult.getPos();
                flag = true;
                vec3d2 = vec3d3;
            }

            float f2 = (float)k / i;
            float f3 = 0.3F + 0.7F * (1.0F - f2);
            Mesh.orb(ctx, vec3d3.x, vec3d3.y, vec3d3.z, f1 * (0.28F + 0.4F * f3), Mesh.alpha(shift(base, k * 3), f3 * a * 0.75F));
            avec3d[j++] = vec3d3;
            vec3d = vec3d3;
            if (flag) {
                break;
            }
        }

        if (j > 2 && VisualQuality.softOrbs()) {
            float f4 = f * 1.8F % 1.0F;
            int l = Math.min(j - 1, Math.max(0, Math.round(f4 * (j - 1))));
            Vec3d vec3d4 = avec3d[l];
            float f5 = f1 * (0.85F + 0.25F * (float)Math.sin(f * 10.0F));
            Mesh.softOrb(ctx, vec3d4.x, vec3d4.y, vec3d4.z, f5, VisualQuality.orbSegs(), Mesh.alpha(base, 0.7F * a));
        }

        if (landing && flag) {
            this.drawLanding(ctx, vec3d2, base, a, f, f1);
        } else if (flag && VisualQuality.softOrbs()) {
            Mesh.softOrb(ctx, vec3d2.x, vec3d2.y, vec3d2.z, f1 * 0.7F, VisualQuality.orbSegs(), Mesh.alpha(base, 0.5F * a));
        }
    }

    private void drawLanding(WorldRenderContext ctx, Vec3d land, int base, float a, float phase, float half) {
        int i = VisualQuality.segs(32);
        Mesh.disc(ctx, land.x, land.y + 0.02, land.z, 0.12, 0.48, i, Mesh.alpha(base, 0.18F * a), Types.glow());
        Mesh.torus(
            ctx,
            land.x,
            land.y + 0.03,
            land.z,
            0.38,
            0.025,
            i,
            VisualQuality.tubeSegs(8),
            Mesh.alpha(base, 0.5F * a),
            Types.plasma()
        );
        Mesh.ring(ctx, land.x, land.y + 0.04, land.z, 0.4, Math.max(16, i - 4), Mesh.alpha(base, 0.55F * a));
        if (VisualQuality.softOrbs()) {
            Mesh.softOrb(ctx, land.x, land.y + 0.1, land.z, half * 0.9F, VisualQuality.orbSegs(), Mesh.alpha(base, 0.35F * a));
        }
    }

    private static int shift(int argb, int hueShift) {
        int i = argb >> 16 & 0xFF;
        int j = argb >> 8 & 0xFF;
        int k = argb & 0xFF;
        float f = hueShift % 40 / 80.0F;
        i = MathHelper.clamp((int)(i + (255 - i) * f * 0.35F), 0, 255);
        j = MathHelper.clamp((int)(j * (1.0F - f * 0.1F)), 0, 255);
        k = MathHelper.clamp((int)(k + (255 - k) * f * 0.2F), 0, 255);
        return i << 16 | j << 8 | k;
    }

    private LivingEntity resolveTarget() {
        LivingEntity livingentity = App.aim().get();
        return livingentity != null && livingentity.isAlive() && App.aim().hot() ? livingentity : null;
    }

    private void drawProjRibbon(WorldRenderContext ctx, Entity e, float tickDelta, int base, float a) {
        Vec3d vec3d = new Vec3d(
            MathHelper.lerp(tickDelta, e.lastX, e.getX()),
            MathHelper.lerp(tickDelta, e.lastY, e.getY()),
            MathHelper.lerp(tickDelta, e.lastZ, e.getZ())
        );
        Vec3d vec3d1 = e.getVelocity();
        ArrayList<Vec3d> arraylist = new ArrayList<>();
        Vec3d vec3d2 = vec3d;

        for (int i = 0; i < 8; i++) {
            arraylist.add(vec3d2);
            vec3d2 = vec3d2.subtract(vec3d1.multiply(0.35));
        }

        Collections.reverse(arraylist);
        float f1 = this.beadSize.f() * 0.45F;
        int j = arraylist.size();

        for (int k = 0; k < j; k++) {
            float f = j <= 1 ? 1.0F : (float)k / (j - 1);
            Vec3d vec3d3 = arraylist.get(k);
            Mesh.orb(ctx, vec3d3.x, vec3d3.y, vec3d3.z, f1 * (0.35F + 0.75F * f), Mesh.alpha(base, 0.55F * a * f));
        }
    }
}
