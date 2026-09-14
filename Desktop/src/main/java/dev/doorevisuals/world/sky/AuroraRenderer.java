package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;

/**
 * Curtains of aurora: a few arcs of vertical quads around the camera, each one a strip whose streaks
 * and vertical falloff are shaped by the fragment shader.
 */
public final class AuroraRenderer {
    private static final float[] ARC_RADIUS = {92.0F, 116.0F, 138.0F};
    private static final float[] ARC_START = {0.35F, 2.1F, 4.0F};
    private static final float[] ARC_SPAN = {1.9F, 1.5F, 1.7F};
    private static final float[] BASE_Y = {56.0F, 62.0F, 52.0F};
    private static final float[] TOP_Y = {104.0F, 118.0F, 96.0F};
    private static final float[] WEIGHT = {0.85F, 0.7F, 0.6F};

    public void draw(WorldRenderContext ctx, SkyPalette palette, float strength) {
        MatrixStack matrixstack = ctx.matrices();
        VertexConsumerProvider vertexconsumerprovider = ctx.consumers();
        if (matrixstack != null && vertexconsumerprovider != null && strength > 0.01F) {
            Entry entry = matrixstack.peek();
            VertexConsumer vertexconsumer = vertexconsumerprovider.getBuffer(Types.aurora());
            int i = Math.max(10, VisualQuality.segs(22));
            int j = VisualQuality.level() == VisualQuality.Level.HIGH ? ARC_RADIUS.length : 2;
            float f = Anim.timeSec();

            for (int k = 0; k < j; k++) {
                this.arc(vertexconsumer, entry, palette, k, i, f, strength);
            }
        }
    }

    private void arc(VertexConsumer buf, Entry pose, SkyPalette palette, int index, int segs, float time, float strength) {
        float f = ARC_RADIUS[index];
        float f1 = ARC_START[index] + time * 0.021F;
        float f2 = ARC_SPAN[index];
        float f3 = BASE_Y[index];
        float f4 = TOP_Y[index];
        int i = Theme.alpha(palette.auroraA() & 16777215, strength * WEIGHT[index]);
        int j = Theme.alpha(palette.auroraB() & 16777215, strength * WEIGHT[index] * 0.55F);
        float f5 = time * 0.055F + index * 3.7F;

        for (int k = 0; k < segs; k++) {
            float f6 = (float)k / segs;
            float f7 = (float)(k + 1) / segs;
            float f8 = f1 + f6 * f2;
            float f9 = f1 + f7 * f2;
            float f10 = (float)Math.cos(f8) * f;
            float f11 = (float)Math.sin(f8) * f;
            float f12 = (float)Math.cos(f9) * f;
            float f13 = (float)Math.sin(f9) * f;

            // A slow sway keeps the curtain from reading as a rigid cylinder.
            float f14 = f3 + (float)Math.sin(f8 * 2.3F + time * 0.55F) * 5.5F;
            float f15 = f4 + (float)Math.sin(f8 * 1.7F + time * 0.4F) * 8.0F;
            float f16 = f3 + (float)Math.sin(f9 * 2.3F + time * 0.55F) * 5.5F;
            float f17 = f4 + (float)Math.sin(f9 * 1.7F + time * 0.4F) * 8.0F;
            float f18 = f6 * 2.4F + f5;
            float f19 = f7 * 2.4F + f5;
            buf.vertex(pose, f10, f14, f11).texture(f18, 0.0F).color(i);
            buf.vertex(pose, f12, f16, f13).texture(f19, 0.0F).color(i);
            buf.vertex(pose, f12, f17, f13).texture(f19, 1.0F).color(j);
            buf.vertex(pose, f10, f15, f11).texture(f18, 1.0F).color(j);
        }
    }
}
