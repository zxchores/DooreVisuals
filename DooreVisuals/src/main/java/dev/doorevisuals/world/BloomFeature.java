package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.tools.GlowSystemFeature;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

/** Stable, geometry-based bloom around nearby emissive blocks. */
public final class BloomFeature extends Feature {
    private final Opt.Num intensity = this.opt(new Opt.Num("intensity", "Интенсивность", 0.55, 0.1, 1.0, 0.05));
    private final Opt.Num radius = this.opt(new Opt.Num("radius", "Радиус", 0.7, 0.25, 1.5, 0.05));
    private final Opt.Num threshold = this.opt(new Opt.Num("threshold", "Порог света", 12.0, 6.0, 15.0, 1.0));

    public BloomFeature() {
        super("bloom", "Bloom", "Мягкое свечение вокруг ярких источников света", Category.WORLD, true);
    }

    public void draw(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!this.on() || !GlowSystemFeature.active() || mc.world == null || mc.player == null) return;
        int r = 10;
        int drawn = 0;
        BlockPos base = mc.player.getBlockPos();
        for (int x = -r; x <= r && drawn < 44; x++) {
            for (int y = -5; y <= 6 && drawn < 44; y++) {
                for (int z = -r; z <= r && drawn < 44; z++) {
                    if (x * x + y * y + z * z > r * r) continue;
                    BlockPos p = base.add(x, y, z);
                    BlockState state = mc.world.getBlockState(p);
                    int light = state.getLuminance();
                    if (light < threshold.i()) continue;
                    float factor = Math.max(0.18F, (light - threshold.i() + 1.0F) / (16.0F - threshold.i() + 1.0F));
                    int alpha = Math.round(88.0F * intensity.f() * factor);
                    int color = (alpha << 24) | GlowSystemFeature.color();
                    Mesh.softBillboard(ctx, p.getX() + 0.5, p.getY() + 0.55, p.getZ() + 0.5,
                        radius.f() * (0.8F + factor * 0.55F), color);
                    drawn++;
                }
            }
        }
    }
}
