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
import net.minecraft.util.math.Vec3d;

/**
 * Two flat discs of noise standing in for a cloud field. Each disc is centred on the camera and fades
 * out at its rim; the shape itself comes from the fragment shader, which reads world position out of
 * the texture coordinates so the field stays anchored while the player walks under it.
 */
public final class CloudRenderer {
    private static final float[] HEIGHT = {38.0F, 54.0F};
    private static final float[] SCALE = {0.0075F, 0.0042F};
    private static final float[] DRIFT = {0.0026F, 0.0015F};
    private static final float[] WEIGHT = {1.0F, 0.72F};
    private int rings;
    private int segs;
    private float[] ringCos = new float[0];
    private float[] ringSin = new float[0];
    private float[] radius = new float[0];

    public void draw(WorldRenderContext ctx, SkyPalette palette, Vec3d cam, double reach) {
        MatrixStack matrixstack = ctx.matrices();
        VertexConsumerProvider vertexconsumerprovider = ctx.consumers();
        if (matrixstack != null && vertexconsumerprovider != null) {
            double d0 = Math.min(reach * 0.9, 360.0);
            if (!(d0 < 32.0)) {
                this.rebuild(VisualQuality.level() == VisualQuality.Level.LOW ? 3 : 4, Math.max(16, VisualQuality.segs(28)), d0);
                Entry entry = matrixstack.peek();
                VertexConsumer vertexconsumer = vertexconsumerprovider.getBuffer(Types.cloud());
                float f = Anim.timeSec();
                int i = VisualQuality.level() == VisualQuality.Level.LOW ? 1 : HEIGHT.length;

                for (int j = 0; j < i; j++) {
                    this.layer(vertexconsumer, entry, palette, cam, j, f);
                }
            }
        }
    }

    private void layer(VertexConsumer buf, Entry pose, SkyPalette palette, Vec3d cam, int index, float time) {
        int i = palette.cloud() & 16777215;
        float f1 = Math.min(1.0F, palette.coverage() * WEIGHT[index]);
        float f2 = HEIGHT[index];
        float f3 = SCALE[index];
        float f4 = time * DRIFT[index];
        float f5 = f4 * 0.6F;

        for (int j = 0; j < this.rings; j++) {
            float f6 = this.radius[j];
            float f7 = this.radius[j + 1];
            int k = Theme.alpha(i, f1 * fade(j, this.rings));
            int l = Theme.alpha(i, f1 * fade(j + 1, this.rings));

            for (int i1 = 0; i1 < this.segs; i1++) {
                float f8 = this.ringCos[i1];
                float f9 = this.ringSin[i1];
                float f10 = this.ringCos[i1 + 1];
                float f11 = this.ringSin[i1 + 1];
                float f12 = f8 * f6;
                float f13 = f9 * f6;
                float f14 = f10 * f6;
                float f15 = f11 * f6;
                float f16 = f8 * f7;
                float f17 = f9 * f7;
                float f18 = f10 * f7;
                float f19 = f11 * f7;
                vertex(buf, pose, f12, f2, f13, cam, f3, f4, f5, k);
                vertex(buf, pose, f14, f2, f15, cam, f3, f4, f5, k);
                vertex(buf, pose, f18, f2, f19, cam, f3, f4, f5, l);
                vertex(buf, pose, f16, f2, f17, cam, f3, f4, f5, l);
            }
        }
    }

    /** Opaque for the inner half of the disc, then off to nothing so the rim never shows an edge. */
    private static float fade(int ring, int rings) {
        float f = (float)ring / rings;
        return f < 0.45F ? 1.0F : 1.0F - (f - 0.45F) / 0.55F;
    }

    private void rebuild(int rings, int segs, double reach) {
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

        if (this.rings != rings || this.radius.length != rings + 1 || Math.abs(this.radius[rings] - reach) > 0.5) {
            this.rings = rings;
            this.radius = new float[rings + 1];

            for (int i = 0; i <= rings; i++) {
                float f = (float)i / rings;
                this.radius[i] = (float)(reach * f * f);
            }
        }
    }

    private static void vertex(
        VertexConsumer buf, Entry pose, float x, float y, float z, Vec3d cam, float scale, float driftU, float driftV, int argb
    ) {
        buf.vertex(pose, x, y, z).texture((float)(cam.x + x) * scale + driftU, (float)(cam.z + z) * scale + driftV).color(argb);
    }
}
