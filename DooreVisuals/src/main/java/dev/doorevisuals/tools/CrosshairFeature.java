package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

/** Custom crosshairs: a ring, several crosses, brackets, arrows and so on, optionally reacting to movement. */
public final class CrosshairFeature extends Feature {
    private static final String RING = "Кольцо";
    private static final String CROSS = "Крест";
    private static final String PLUS = "Плюс";
    private static final String T_SHAPE = "Т-образный";
    private static final String DOT = "Точка";
    private static final String CIRCLE_DOT = "Круг с точкой";
    private static final String SQUARE = "Квадрат";
    private static final String DIAMOND = "Ромб";
    private static final String BRACKETS = "Скобки";
    private static final String DIAGONAL = "Диагональ";
    private static final String ARROWS = "Стрелки";

    private final Opt.Pick style = this.opt(new Opt.Pick("style", "Вид", RING,
            RING, CROSS, PLUS, T_SHAPE, DOT, CIRCLE_DOT, SQUARE, DIAMOND, BRACKETS, DIAGONAL, ARROWS));
    private final Opt.Num size = this.opt(new Opt.Num("size", "Размер", 6.5, 2.0, 16.0, 0.5));
    private final Opt.Num gap = this.opt(new Opt.Num("gap", "Зазор", 3.0, 0.0, 10.0, 0.5).visibleWhen(this::hasGap));
    private final Opt.Num thick = this.opt(new Opt.Num("thick", "Толщина", 1.35, 0.6, 3.0, 0.1));
    private final Opt.Flag centerDot = this.opt(new Opt.Flag("center_dot", "Точка в центре", false).visibleWhen(this::hasGap));
    private final Opt.Flag outline = this.opt(new Opt.Flag("outline", "Тёмная обводка", true));
    private final Opt.Flag glow = this.opt(new Opt.Flag("glow", "Свечение", true));
    private final Opt.Flag dynamic = this.opt(new Opt.Flag("dynamic", "Расширяется при движении", false));
    private final Opt.Flag cooldown = this.opt(new Opt.Flag("cooldown", "Полоска перезарядки удара", false));
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "Цвет из темы", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "Цвет", 0xFFFFFFFF).visibleWhen(() -> !this.themeColor.get()));
    private final Opt.Num alpha = this.opt(new Opt.Num("alpha", "Прозрачность", 1.0, 0.2, 1.0, 0.05));

    private float spread;
    private long lastNanos;
    private float cooldownShown;

    public CrosshairFeature() {
        super(
                "crosshair",
                "Crosshair",
                "Свои прицелы: кольцо, кресты, скобки, стрелки и другие",
                Category.OVERLAY,
                true
        );
    }

    private boolean hasGap() {
        String s = this.style.get();
        return !RING.equals(s) && !DOT.equals(s) && !CIRCLE_DOT.equals(s) && !SQUARE.equals(s) && !DIAMOND.equals(s);
    }

    public void paint(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) {
            return;
        }
        long now = System.nanoTime();
        float dt = this.lastNanos == 0L ? 0.016F : Math.max(0.0F, Math.min(0.1F, (now - this.lastNanos) / 1.0E9F));
        this.lastNanos = now;
        this.follow(mc.player, dt);

        float cx = g.getScaledWindowWidth() * 0.5F;
        float cy = g.getScaledWindowHeight() * 0.5F;
        float s = this.size.f();
        float t = this.thick.f();
        float gp = this.gap.f() + this.spread * 5.0F;
        int base = this.themeColor.get() ? Theme.HUD : this.color.get();
        int col = withAlpha(base, this.alpha.f());
        String st = this.style.get();

        if (!Nvg.frame()) {
            // Without the NanoVG layer only plain boxes can be drawn: a cross or a dot stands in for any shape.
            this.plain(g, cx, cy, s, gp, t, col, DOT.equals(st));
            return;
        }
        Ui.push();
        if (this.glow.get()) {
            this.shape(st, cx, cy, s, gp, t + 3.2F, Theme.alpha(col, (int)(34 * this.alpha.f())), true);
        }
        if (this.outline.get()) {
            this.shape(st, cx, cy, s, gp, t + 1.7F, Theme.alpha(0x000000, (int)(150 * this.alpha.f())), true);
        }
        this.shape(st, cx, cy, s, gp, t, col, false);
        if (this.centerDot.get() && this.hasGap()) {
            float r = Math.max(0.9F, t * 0.62F);
            if (this.outline.get()) {
                Nvg.circle(cx, cy, r + 0.9F, Theme.alpha(0x000000, (int)(150 * this.alpha.f())));
            }
            Nvg.circle(cx, cy, r, col);
        }
        if (this.cooldown.get()) {
            this.paintCooldown(cx, cy + s + gp + 7.0F, col);
        }
        Ui.pop();
    }

    /** Eases the spread towards what the player is doing: standing, walking, sprinting or in the air. */
    private void follow(ClientPlayerEntity p, float dt) {
        float target = 0.0F;
        if (this.dynamic.get()) {
            double speed = Math.sqrt(p.getVelocity().x * p.getVelocity().x + p.getVelocity().z * p.getVelocity().z);
            target = (float)Math.min(1.0, speed * 4.0);
            if (p.isSprinting()) {
                target = Math.max(target, 0.9F);
            }
            if (!p.isOnGround()) {
                target = Math.max(target, 0.75F);
            }
        }
        float k = 1.0F - (float)Math.exp(-dt * (target > this.spread ? 14.0F : 7.0F));
        this.spread += (target - this.spread) * k;
        float want = p.getAttackCooldownProgress(0.0F);
        this.cooldownShown += (want - this.cooldownShown) * (1.0F - (float)Math.exp(-dt * 18.0F));
    }

    private void paintCooldown(float cx, float y, int col) {
        float p = Math.max(0.0F, Math.min(1.0F, this.cooldownShown));
        if (p >= 0.995F) {
            return;
        }
        float w = 18.0F;
        Nvg.rect(cx - w / 2.0F - 0.8F, y - 0.8F, w + 1.6F, 3.6F, Theme.alpha(0x000000, 120), 1.8F);
        Nvg.rect(cx - w / 2.0F, y, w, 2.0F, Theme.alpha(0xFFFFFF, 40), 1.0F);
        Nvg.rect(cx - w / 2.0F, y, Math.max(1.0F, w * p), 2.0F, col, 1.0F);
    }

    /** One pass of the shape. {@code t} is the stroke width, so the glow and the outline are the same shape, just wider. */
    private void shape(String st, float cx, float cy, float s, float gp, float t, int col, boolean wide) {
        float spreadR = this.spread * 2.5F;
        switch (st) {
            case RING -> {
                float r = s + spreadR;
                Nvg.ring(cx - r, cy - r, r * 2.0F, r * 2.0F, t, col, r);
            }
            case CIRCLE_DOT -> {
                float r = s + spreadR;
                Nvg.ring(cx - r, cy - r, r * 2.0F, r * 2.0F, t, col, r);
                Nvg.circle(cx, cy, Math.max(0.9F, t * 0.75F), col);
            }
            case SQUARE -> {
                float r = s + spreadR;
                Nvg.ring(cx - r, cy - r, r * 2.0F, r * 2.0F, t, col, Math.min(2.2F, r * 0.3F));
            }
            case DOT -> Nvg.circle(cx, cy, Math.max(1.0F, t * 1.15F + s * 0.1F) + (wide ? (t - this.thick.f()) * 0.5F : 0.0F), col);
            case DIAMOND -> {
                float r = s + spreadR;
                Nvg.line(cx, cy - r, cx + r, cy, t, col);
                Nvg.line(cx + r, cy, cx, cy + r, t, col);
                Nvg.line(cx, cy + r, cx - r, cy, t, col);
                Nvg.line(cx - r, cy, cx, cy - r, t, col);
            }
            case PLUS, CROSS, T_SHAPE -> {
                float g0 = PLUS.equals(st) ? this.spread * 5.0F : gp;
                this.arms(cx, cy, s, g0, t, col, T_SHAPE.equals(st));
            }
            case BRACKETS -> {
                float h = s + gp;
                float l = Math.max(2.0F, s * 0.7F);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        float ex = cx + sx * h;
                        float ey = cy + sy * h;
                        // Each corner is two bars that run from the corner point towards the middle.
                        Nvg.rect(sx > 0 ? ex - l : ex - t * 0.5F, ey - t * 0.5F, l + t * 0.5F, t, col, 0.0F);
                        Nvg.rect(ex - t * 0.5F, sy > 0 ? ey - l : ey - t * 0.5F, t, l + t * 0.5F, col, 0.0F);
                    }
                }
            }
            case DIAGONAL -> {
                float a = gp * 0.7071F;
                float b = (gp + s) * 0.7071F;
                Nvg.line(cx + a, cy + a, cx + b, cy + b, t, col);
                Nvg.line(cx - a, cy + a, cx - b, cy + b, t, col);
                Nvg.line(cx + a, cy - a, cx + b, cy - b, t, col);
                Nvg.line(cx - a, cy - a, cx - b, cy - b, t, col);
            }
            case ARROWS -> {
                float grow = (t - this.thick.f()) * 0.5F;
                float w = s * 0.55F + grow;
                float back = gp + s + grow;
                Nvg.triangle(cx - w, cy - back, cx + w, cy - back, cx, cy - gp, col);
                Nvg.triangle(cx - w, cy + back, cx + w, cy + back, cx, cy + gp, col);
                Nvg.triangle(cx - back, cy - w, cx - back, cy + w, cx - gp, cy, col);
                Nvg.triangle(cx + back, cy - w, cx + back, cy + w, cx + gp, cy, col);
            }
            default -> {
            }
        }
    }

    private void arms(float cx, float cy, float s, float gp, float t, int col, boolean noTop) {
        float h = t * 0.5F;
        Nvg.rect(cx - gp - s, cy - h, s, t, col, 0.0F);
        Nvg.rect(cx + gp, cy - h, s, t, col, 0.0F);
        Nvg.rect(cx - h, cy + gp, t, s, col, 0.0F);
        if (!noTop) {
            Nvg.rect(cx - h, cy - gp - s, t, s, col, 0.0F);
        }
    }

    private void plain(DrawContext g, float cx, float cy, float s, float gp, float t, int col, boolean dot) {
        if (dot) {
            Paint.box(g, cx - t, cy - t, t * 2.0F, t * 2.0F, col, t);
            return;
        }
        float h = t * 0.5F;
        Paint.box(g, cx - gp - s, cy - h, s, t, col, 0.0F);
        Paint.box(g, cx + gp, cy - h, s, t, col, 0.0F);
        Paint.box(g, cx - h, cy - gp - s, t, s, col, 0.0F);
        Paint.box(g, cx - h, cy + gp, t, s, col, 0.0F);
    }

    private static int withAlpha(int argb, float a) {
        int base = argb >>> 24 == 0 ? 0xFF : argb >>> 24;
        return (Math.round(base * Math.max(0.0F, Math.min(1.0F, a))) << 24) | (argb & 0xFFFFFF);
    }

    public static boolean hideVanilla() {
        return App.features().find(CrosshairFeature.class).filter(Feature::on).isPresent();
    }
}
