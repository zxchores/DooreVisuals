package dev.doorevisuals.overlay;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.WorldGui;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Client-side floating health-delta indicator. */
public final class DamageIndicatorFeature extends Feature implements dev.doorevisuals.core.Tick {
    private final Opt.Num duration = this.opt(new Opt.Num("duration", "Время", 0.9, 0.35, 1.8, 0.05));
    private final Opt.Num scale = this.opt(new Opt.Num("scale", "Размер", 1.0, 0.7, 1.6, 0.05));
    private final Opt.Num range = this.opt(new Opt.Num("range", "Дистанция", 18.0, 6.0, 28.0, 1.0));
    private final Map<Integer, Float> health = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();

    public DamageIndicatorFeature() {
        super("damage_indicator", "Damage Indicator", "Показывает количество потерянного здоровья над сущностями", Category.OVERLAY, true);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            health.clear();
            hits.clear();
            return;
        }
        long now = System.currentTimeMillis();
        for (LivingEntity entity : mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range.f()), e -> e != mc.player && e.isAlive())) {
            float hp = entity.getHealth();
            Float previous = health.put(entity.getId(), hp);
            if (previous != null && previous - hp > 0.01F && previous - hp < entity.getMaxHealth() + 1.0F) {
                hits.add(new Hit(entity.getId(), previous - hp, now, entity));
            }
        }
        if (health.size() > 512) health.clear();
        Iterator<Hit> it = hits.iterator();
        long life = Math.round(duration.f() * 1000.0F);
        while (it.hasNext()) {
            Hit hit = it.next();
            if (now - hit.born > life || hit.entity() == null || !hit.entity().isAlive()) it.remove();
        }
        if (hits.size() > 48) hits.subList(0, hits.size() - 48).clear();
    }

    public void paint(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.on() || mc.world == null || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();
        long life = Math.round(duration.f() * 1000.0F);
        for (Hit hit : List.copyOf(hits)) {
            float[] screen = WorldGui.project(hit.entity().getX(), hit.entity().getY() + hit.entity().getHeight() + 0.35, hit.entity().getZ());
            if (screen == null) continue;
            float progress = MathHelper.clamp((now - hit.born) / (float)life, 0.0F, 1.0F);
            float eased = Anim.easeLife(progress);
            int alpha = Math.round(255.0F * eased);
            int color = Theme.alpha(0xFFD7E6FF, alpha);
            String text = "-" + trim(hit.amount);
            int w = tr.getWidth(text);
            float x = screen[0] - w * 0.5F;
            float y = screen[1] - progress * 22.0F;
            g.drawTextWithShadow(tr, text, Math.round(x), Math.round(y), color);
        }
    }

    private static String trim(float amount) {
        if (Math.abs(amount - Math.round(amount)) < 0.05F) return Integer.toString(Math.round(amount));
        return String.format(java.util.Locale.ROOT, "%.1f", amount);
    }

    private record Hit(int id, float amount, long born, LivingEntity entity) {}
}
