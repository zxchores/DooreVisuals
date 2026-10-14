package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

/** Adds small contact shadows under nearby living entities without replacing the vanilla shadow pass. */
public final class ShadowEnhancerFeature extends Feature {
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "Интенсивность", 0.48, 0.1, 1.0, 0.05));
    private final Opt.Num radius = this.opt(new Opt.Num("radius", "Размер", 0.65, 0.25, 1.3, 0.05));
    private final Opt.Num distance = this.opt(new Opt.Num("distance", "Дистанция", 9.0, 4.0, 14.0, 1.0));

    public ShadowEnhancerFeature() {
        super("enhanced_shadows", "Enhanced Shadows", "Мягкие контактные тени для более выразительного мира", Category.WORLD, true);
    }

    public void draw(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.on() || mc.world == null || mc.player == null) return;
        double maxSq = distance.f() * distance.f();
        int drawn = 0;
        for (LivingEntity e : mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(distance.f()), value -> value != mc.player && value.isAlive())) {
            if (drawn >= 32 || e.squaredDistanceTo(mc.player) > maxSq) continue;
            BlockPos feet = BlockPos.ofFloored(e.getX(), e.getY() - 0.02, e.getZ());
            float scale = Math.min(1.45F, 0.45F + e.getWidth() * 0.5F);
            int alpha = Math.round(72.0F * strength.f());
            int color = Theme.alpha(0x0B0A10, alpha);
            Mesh.ellipse(ctx, e.getX(), feet.getY() + 0.018, e.getZ(),
                radius.f() * scale, radius.f() * 0.62F * scale, e.getYaw() * 0.017453292F, 18, color, Types.fill());
            drawn++;
        }
    }
}
