package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.cosmetics.CosmeticsRegistry;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.tools.SmoothF5Feature;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class ChinaHatFeature extends Feature {
    private final Opt.Pick style = this.opt(
        new Opt.Pick(
            "style",
            "\u0412\u0438\u0434",
            "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f",
            "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f",
            "\u0410\u043d\u0433\u0435\u043b",
            "67"
        )
    );
    private final Opt.Pick targets = this.opt(
        new Opt.Pick("targets", "\u0426\u0435\u043b\u0438", "\u0412\u0441\u0435", "\u0421\u0435\u0431\u044f", "\u0412\u0441\u0435")
    );
    private final Opt.Pick who = this.opt(
        new Opt.Pick(
            "who", "\u0421\u0443\u0449\u043d\u043e\u0441\u0442\u0438", "\u0412\u0441\u0435", "\u0418\u0433\u0440\u043e\u043a\u0438", "\u0412\u0441\u0435"
        )
    );
    private final Opt.Num size = this.opt(new Opt.Num("size", "\u0420\u0430\u0437\u043c\u0435\u0440", 0.72, 0.4, 1.8, 0.05));
    private final Opt.Num height = this.opt(new Opt.Num("height", "\u0412\u044b\u0441\u043e\u0442\u0430", 0.42, 0.22, 1.4, 0.05));
    private final Opt.Num opacity = this.opt(
        new Opt.Num("opacity", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 0.9, 0.3, 1.0, 0.05)
    );
    private final Opt.Num range = this.opt(new Opt.Num("range", "\u0414\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c", 40.0, 12.0, 80.0, 4.0));
    private final Opt.Num spin = this.opt(new Opt.Num("spin", "\u0412\u0440\u0430\u0449\u0435\u043d\u0438\u0435", 0.6, 0.0, 3.0, 0.05));
    private final Opt.Num glow = this.opt(new Opt.Num("glow", "\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435", 0.7, 0.0, 1.5, 0.05));
    private final Opt.Flag walls = this.opt(
        new Opt.Flag("walls", "\u041f\u0440\u044f\u0442\u0430\u0442\u044c \u0437\u0430 \u0441\u0442\u0435\u043d\u0430\u043c\u0438", true)
    );
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.themeColor.get()));
    private volatile List<ChinaHatFeature.Hat> hats = List.of();

    public ChinaHatFeature() {
        super("china_hat", "China Hat", "\u0428\u043b\u044f\u043f\u0430 \u0438\u0437 Cosmetics", Category.COSMETICS, true);
        this.unlist();
    }

    public String style() {
        return (String)this.style.get();
    }

    public int tint() {
        return this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
    }

    public void extract(float tickDelta) {
        if (!this.on()) {
            this.hats = List.of();
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            if (clientplayerentity != null && minecraftclient.world != null) {
                boolean flag = (Boolean)this.walls.get();
                double d0 = (Double)this.range.get() * (Double)this.range.get();
                float f = this.opacity.f();
                double d1 = (Double)this.size.get();
                float f1 = this.height.f();
                float f2 = this.glow.f();
                int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
                boolean flag1 = minecraftclient.options.getPerspective() == Perspective.FIRST_PERSON && !SmoothF5Feature.transitioning();
                double d2 = System.currentTimeMillis() / 1000.0 * this.spin.f();
                String s = hatStyleOf(clientplayerentity.getUuid(), true);
                List<ChinaHatFeature.Hat> list = new ArrayList<>(24);
                if (!s.isEmpty() && !flag1) {
                    add(list, clientplayerentity, clientplayerentity, tickDelta, d1, f1, f, i, s, flag, d2, f2);
                }

                for (Entity entity : minecraftclient.world.getEntities()) {
                    if (entity instanceof PlayerEntity playerentity
                        && playerentity != clientplayerentity
                        && playerentity.isAlive()
                        && !playerentity.isInvisible()) {
                        String s1 = hatStyleOf(playerentity.getUuid(), false);
                        if (!s1.isEmpty() && WorldVis.inFrustumRange(clientplayerentity, playerentity, d0)) {
                            add(list, clientplayerentity, playerentity, tickDelta, d1, f1, f, i, s1, flag, d2, f2);
                            if (list.size() >= 48) {
                                break;
                            }
                        }
                    }
                }

                this.hats = list;
            } else {
                this.hats = List.of();
            }
        }
    }

    private static String hatStyleOf(UUID id, boolean self) {
        return self
            ? App.features().find(CosmeticsFeature.class).filter(Feature::on).map(CosmeticsFeature::hatLabel).map(ChinaHatFeature::fromLabel).orElse("")
            : fromId(CosmeticsRegistry.look(id).hatId());
    }

    private static String fromLabel(String label) {
        String s = label == null ? "" : label;

        return switch (s) {
            case "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f" -> "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f";
            case "\u0410\u043d\u0433\u0435\u043b" -> "\u0410\u043d\u0433\u0435\u043b";
            case "67" -> "67";
            default -> "";
        };
    }

    private static String fromId(String id) {
        String s = id == null ? "" : id;

        return switch (s) {
            case "china" -> "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f";
            case "halo" -> "\u0410\u043d\u0433\u0435\u043b";
            case "67" -> "67";
            default -> "";
        };
    }

    public void draw(WorldRenderContext ctx) {
        for (ChinaHatFeature.Hat chinahatfeature$hat : this.hats) {
            String s = chinahatfeature$hat.style;
            switch (s) {
                case "\u0410\u043d\u0433\u0435\u043b":
                    paintHalo(ctx, chinahatfeature$hat);
                    break;
                case "67":
                    paint67(ctx, chinahatfeature$hat);
                    break;
                default:
                    paintChinese(ctx, chinahatfeature$hat);
            }
        }
    }

    private static boolean add(
        List<ChinaHatFeature.Hat> out,
        ClientPlayerEntity self,
        LivingEntity e,
        float td,
        double sz,
        float ht,
        float op,
        int col,
        String style,
        boolean hideWalls,
        double phase,
        float glow
    ) {
        if (hideWalls && e != self && !WorldVis.canSee(self, e)) {
            return false;
        } else {
            double d0 = MathHelper.lerp(td, e.lastX, e.getX());
            double d1 = MathHelper.lerp(td, e.lastY, e.getY());
            double d2 = MathHelper.lerp(td, e.lastZ, e.getZ());
            float f = MathHelper.lerpAngleDegrees(td, e.lastBodyYaw, e.bodyYaw);
            float f1 = 0.0F;
            if (e.isGliding() || e.isInSwimmingPose()) {
                f1 = MathHelper.lerp(td, e.lastPitch, e.getPitch());
            }

            double d3 = e.getStandingEyeHeight() + 0.16;
            double d4 = Math.toRadians(f);
            double d5 = Math.toRadians(f1);
            d0 += -Math.sin(d4) * Math.sin(d5) * d3 * 0.55;
            d2 += Math.cos(d4) * Math.sin(d5) * d3 * 0.55;
            d1 += d3 + (Math.cos(d5) - 1.0) * d3 * 0.55;
            double d6 = self.distanceTo(e);
            double d7 = Math.max(0.5, (double)e.getWidth()) * sz;
            int i = d6 < 10.0 ? 2 : (d6 < 24.0 ? 1 : 0);
            out.add(new ChinaHatFeature.Hat(d0, d1, d2, d7, ht, op, col, i, style, phase, glow, f));
            return true;
        }
    }

    private static void paintChinese(WorldRenderContext ctx, ChinaHatFeature.Hat h) {
        int i = h.lod == 2 ? 56 : (h.lod == 1 ? 40 : 28);
        double d0 = h.y - Math.max(0.22, h.height * 0.48);
        double d1 = h.scale * 1.08;
        float f = Math.max(0.28F, h.height * 0.92F);
        double d2 = Math.max(0.012, d1 * 0.03);
        float f1 = Math.max(0.2F, h.glow);
        float f2 = h.op;
        int j = Mesh.alpha(15258788, 0.94F * f2);
        int k = Mesh.alpha(9202234, 0.92F * f2);
        int l = Mesh.alpha(2365454, 0.88F * f2);
        int i1 = themeHalo(h.color, f1, f2);
        int j1 = Mesh.alpha(16761576, 0.45F * f2 * f1);
        int k1 = Mesh.alpha(12124152, 0.55F * f2 * f1);
        int l1 = Mesh.alpha(16777215, 0.4F * f2 * f1);
        Mesh.disc(ctx, h.x, d0 + 0.006, h.z, 0.0, d1 * 0.98, i, l, Types.fill());
        Mesh.cone(ctx, h.x, d0 - 0.035, h.z, d1 * 0.92, d1 * 0.22, 0.04F, i, Mesh.alpha(1708554, 0.55F * f2), Types.fill());
        Mesh.cone(ctx, h.x, d0, h.z, d1, d2, f, i, j, Types.fill());
        Mesh.cone(ctx, h.x, d0 + 0.01, h.z, d1 * 0.72, d2 * 1.4, f * 0.78F, Math.max(16, i / 2), Mesh.alpha(12887658, 0.35F * f2), Types.fill());
        Mesh.torus(ctx, h.x, d0 + 0.01, h.z, d1 * 0.99, d1 * 0.028, i, 8, k, Types.fill());
        Mesh.dome(ctx, h.x, d0 + f - d2 * 0.2, h.z, d2 * 2.6, Math.max(12, i / 2), 6, Mesh.alpha(15918276, 0.95F * f2), Types.fill());
        Mesh.disc(ctx, h.x, d0 + 0.018, h.z, d1 * 0.86, d1 * 1.16, i, Mesh.alpha(i1, 0.22F * f2 * f1), Types.holo());
        Mesh.torus(ctx, h.x, d0 + 0.02, h.z, d1 * 1.01, d1 * 0.055, i, 10, k1, Types.glow());
        Mesh.torus(ctx, h.x, d0 + 0.028, h.z, d1 * 1.05, d1 * 0.032, i, 8, l1, Types.holo());
        Mesh.torus(ctx, h.x, d0 + 0.012, h.z, d1 * 0.97, d1 * 0.03, i, 8, j1, Types.holo());
    }

    private static int themeHalo(int theme, float glow, float op) {
        int i = Mesh.alpha(13172726, 0.7F * op * glow);
        return theme == 0 ? i : Mesh.alpha(theme, 0.55F * op * glow);
    }

    private static void paintHalo(WorldRenderContext ctx, ChinaHatFeature.Hat h) {
        int i = h.lod == 2 ? 36 : 24;
        double d0 = h.scale * 0.55;
        float f = (float)(h.y + h.height * 0.35);
        float f1 = Math.max(0.15F, h.glow);
        double d1 = Math.cos(h.phase) * 0.03;
        double d2 = Math.sin(h.phase) * 0.03;
        int j = Mesh.alpha(h.color, 0.9F * h.op);
        int k = Mesh.alpha(h.color, 0.4F * h.op * f1);
        int l = Mesh.alpha(h.color, 0.65F * h.op * f1);
        Mesh.torus(ctx, h.x + d1, f, h.z + d2, d0, d0 * 0.18, i, 12, j, Types.holo());
        Mesh.torus(ctx, h.x, f, h.z, d0, d0 * 0.1, i, 10, l, Types.holo());
        Mesh.disc(ctx, h.x, f, h.z, d0 * 0.7, d0 * 1.05, i, k, Types.holo());
        Mesh.ring(ctx, h.x + d1, f + 0.01, h.z + d2, d0 * 1.05, i, j);
    }

    private static void paint67(WorldRenderContext ctx, ChinaHatFeature.Hat h) {
        double d0 = System.currentTimeMillis() / 1000.0;
        float f = (float)Math.sin(d0 * 5.4);
        float f1 = f * 0.22F;
        float f2 = -f * 0.22F;
        float f3 = (float)(0.26 + Math.sin(d0 * 2.2) * 0.04);
        float f4 = 1.05F + f * 0.08F;
        float f5 = 1.05F - f * 0.08F;
        double d1 = h.y + h.height * 0.12;
        float f6 = (float)(h.scale * 0.58 * f4);
        float f7 = (float)(h.scale * 0.58 * f5);
        float f8 = f6 * 0.62F;
        float f9 = f7 * 0.62F;
        int i = Mesh.alpha(h.color, 0.98F * h.op);
        float f10 = 0.35F * h.op * (0.75F + 0.25F * Math.abs(f));
        double d2 = Math.toRadians(h.yaw);
        double d3 = Math.cos(d2);
        double d4 = Math.sin(d2);
        double d5 = h.x - d3 * f3;
        double d6 = h.z - d4 * f3;
        double d7 = h.x + d3 * f3;
        double d8 = h.z + d4 * f3;
        double d9 = d1 + f1 + f6 * 0.55;
        double d10 = d1 + f2 + f7 * 0.55;
        Mesh.softBillboard(ctx, d5, d9, d6, f6 * 0.42F, Mesh.alpha(h.color, f10));
        Mesh.softBillboard(ctx, d7, d10, d8, f7 * 0.42F, Mesh.alpha(h.color, f10));
        digit(ctx, Sprites.DIGIT_6, d5, d9, d6, h.yaw, f8, f6, i);
        digit(ctx, Sprites.DIGIT_7, d7, d10, d8, h.yaw, f9, f7, i);
    }

    private static void digit(WorldRenderContext ctx, Identifier tex, double x, double y, double z, float yaw, float halfW, float halfH, int tint) {
        Mesh.yawQuad(ctx, tex, x, y, z, yaw, halfW, halfH, 0.02F, 0.0F, 0.0F, 1.0F, 1.0F, tint, true);
        Mesh.yawQuad(ctx, tex, x, y, z, yaw + 180.0F, halfW, halfH, 0.02F, 1.0F, 0.0F, 0.0F, 1.0F, tint, true);
    }

    private record Hat(
        double x, double y, double z, double scale, float height, float op, int color, int lod, String style, double phase, float glow, float yaw
    ) {
    }
}
