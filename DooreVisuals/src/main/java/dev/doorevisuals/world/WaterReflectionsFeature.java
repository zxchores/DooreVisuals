package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.GlowSystemFeature;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Lightweight stylized water reflections. No framebuffer copies or shader pipeline required. */
public final class WaterReflectionsFeature extends Feature {
    private final Opt.Num radius = this.opt(new Opt.Num("radius", "Радиус", 9.0, 4.0, 14.0, 1.0));
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "Сила отражения", 0.45, 0.1, 1.0, 0.05));
    private final Opt.Num shimmer = this.opt(new Opt.Num("shimmer", "Рябь", 0.7, 0.0, 1.5, 0.05));

    public WaterReflectionsFeature() {
        super("water_reflections", "Water Reflections", "Мягкие отражения и рябь на поверхности воды", Category.WORLD, false);
    }

    public void draw(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.on() || mc.world == null || mc.player == null || mc.world.getRegistryKey() != World.OVERWORLD) return;
        int r = Math.max(1, radius.i());
        double cx = mc.player.getX();
        double cz = mc.player.getZ();
        int drawn = 0;
        float wave = Anim.pulse(0.22F * Math.max(0.15F, shimmer.f()));
        for (int x = -r; x <= r && drawn < 70; x++) {
            for (int z = -r; z <= r && drawn < 70; z++) {
                if (x * x + z * z > r * r) continue;
                BlockPos p = BlockPos.ofFloored(cx + x, mc.player.getY(), cz + z);
                for (int y = p.getY() - 2; y <= p.getY() + 2; y++) {
                    BlockPos pos = new BlockPos(p.getX(), y, p.getZ());
                    if (!mc.world.getFluidState(pos).isOf(Fluids.WATER) || !mc.world.isSkyVisible(pos.up())) continue;
                    double px = pos.getX() + 0.5;
                    double pz = pos.getZ() + 0.5;
                    float local = 0.4F + 0.6F * wave;
                    int alpha = Math.round(26.0F * strength.f() * local);
                    int color = (alpha << 24) | GlowSystemFeature.accentColor(0x9BCBFF) & 0xFFFFFF;
                    Mesh.ellipse(ctx, px, pos.getY() + 1.004, pz,
                        0.34 + 0.08 * wave, 0.20 + 0.06 * wave,
                        (float)((pos.asLong() & 255L) * 0.0245), 16, color, Types.glow());
                    if (strength.f() > 0.62F) {
                        Mesh.ring(ctx, px, pos.getY() + 1.006, pz, 0.18 + 0.08 * wave, 12,
                            Theme.alpha(0xD5EFFF, Math.round(18.0F * strength.f())));
                    }
                    drawn++;
                    break;
                }
            }
        }
    }
}
