package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;

/**
 * Draws the sky shell. It is centred on the camera, so a vertex position is already camera-relative
 * and the fragment shader can read it straight off as a view direction.
 */
public final class SkyRenderer {
    private static final float TAU = (float)(Math.PI * 2.0);
    private int segs;
    private int stacks;
    private float[] ringCos = new float[0];
    private float[] ringSin = new float[0];
    private float[] stackCos = new float[0];
    private float[] stackSin = new float[0];
    private int[] bandArgb = new int[0];

    /**
     * Radius has to clear the furthest terrain so the depth test lets terrain win, and stay inside the
     * far plane so the shell itself is not clipped. The far plane is four times the view distance and
     * the furthest terrain corner about 1.42 times it, so double sits safely between the two.
     */
    public static double radius(MinecraftClient mc) {
        return Math.max(2, (Integer)mc.options.getViewDistance().getValue()) * 16.0 * 2.0;
    }

    /**
     * @param sunPhase fraction of the day, zero at dawn and a quarter at noon
     * @param ownDiscs whether the shader draws its own sun and moon instead of the vanilla ones
     */
    public void draw(WorldRenderContext ctx, SkyPalette palette, double radius, float sunPhase, boolean ownDiscs) {
        MatrixStack matrixstack = ctx.matrices();
        VertexConsumerProvider vertexconsumerprovider = ctx.consumers();
        if (matrixstack != null && vertexconsumerprovider != null && radius > 1.0) {
            this.rebuild(Math.max(12, VisualQuality.segs(20)), Math.max(10, VisualQuality.segs(16)));
            this.shade(palette);
            Entry entry = matrixstack.peek();
            VertexConsumer vertexconsumer = vertexconsumerprovider.getBuffer(Types.sky());
            float f = (float)radius;

            // Every vertex carries the same sun: it is the only channel this vertex format has spare.
            float f1 = sunPhase - (float)Math.floor(sunPhase);
            float f2 = (float)Math.cos((f1 - 0.25F) * TAU) * 0.5F + 0.5F + (ownDiscs ? 2.0F : 0.0F);

            for (int i = 0; i < this.stacks; i++) {
                int j = this.bandArgb[i];
                int k = this.bandArgb[i + 1];
                float f3 = this.stackSin[i] * f;
                float f4 = this.stackCos[i] * f;
                float f5 = this.stackSin[i + 1] * f;
                float f6 = this.stackCos[i + 1] * f;

                for (int l = 0; l < this.segs; l++) {
                    float f7 = this.ringCos[l];
                    float f8 = this.ringSin[l];
                    float f9 = this.ringCos[l + 1];
                    float f10 = this.ringSin[l + 1];
                    vertex(vertexconsumer, entry, f7 * f3, f4, f8 * f3, f1, f2, j);
                    vertex(vertexconsumer, entry, f9 * f3, f4, f10 * f3, f1, f2, j);
                    vertex(vertexconsumer, entry, f9 * f5, f6, f10 * f5, f1, f2, k);
                    vertex(vertexconsumer, entry, f7 * f5, f6, f8 * f5, f1, f2, k);
                }
            }
        }
    }

    private void shade(SkyPalette palette) {
        for (int i = 0; i <= this.stacks; i++) {
            float f = (float)i / this.stacks;
            this.bandArgb[i] = Theme.alpha(palette.band(f), f < 0.5F ? 0.92F - 0.04F * f * 2.0F : 0.88F + 0.02F * (f - 0.5F) * 2.0F);
        }
    }

    /** Trigonometry for the shell only depends on the tessellation, so it is kept between frames. */
    private void rebuild(int segs, int stacks) {
        if (this.segs != segs) {
            this.segs = segs;
            this.ringCos = new float[segs + 1];
            this.ringSin = new float[segs + 1];

            for (int i = 0; i <= segs; i++) {
                double d0 = i * Math.PI * 2.0 / segs;
                this.ringCos[i] = (float)Math.cos(d0);
                this.ringSin[i] = (float)Math.sin(d0);
            }
        }

        if (this.stacks != stacks) {
            this.stacks = stacks;
            this.stackCos = new float[stacks + 1];
            this.stackSin = new float[stacks + 1];
            this.bandArgb = new int[stacks + 1];

            for (int i = 0; i <= stacks; i++) {
                double d0 = Math.PI * i / stacks;
                this.stackCos[i] = (float)Math.cos(d0);
                this.stackSin[i] = (float)Math.sin(d0);
            }
        }
    }

    private static void vertex(VertexConsumer buf, Entry pose, float x, float y, float z, float u, float v, int argb) {
        buf.vertex(pose, x, y, z).texture(u, v).color(argb);
    }
}
