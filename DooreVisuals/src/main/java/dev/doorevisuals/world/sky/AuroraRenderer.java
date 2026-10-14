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

/** Smooth, layered aurora curtains with deliberately soft motion and density. */
public final class AuroraRenderer {
    private static final Band[] BANDS = {
        new Band(86.0F, 0.18F, 2.40F, 58.0F, 104.0F, 3.8F, 0.2F, 0.64F),
        new Band(105.0F, 1.40F, 2.00F, 63.0F, 111.0F, 4.6F, 1.6F, 0.54F),
        new Band(124.0F, 2.55F, 2.18F, 55.0F, 108.0F, 5.2F, 3.1F, 0.47F),
        new Band(146.0F, 3.60F, 1.98F, 66.0F, 119.0F, 5.8F, 4.7F, 0.37F),
        new Band(169.0F, 4.56F, 1.72F, 73.0F, 125.0F, 6.2F, 6.0F, 0.26F)
    };

    public void draw(WorldRenderContext ctx, SkyPalette palette, float strength) {
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null || strength <= 0.01F) return;

        int bands = switch (VisualQuality.level()) {
            case HIGH -> BANDS.length;
            case MEDIUM -> 4;
            default -> 2;
        };
        int segments = Math.max(30, VisualQuality.segs(42));
        int vertical = VisualQuality.level() == VisualQuality.Level.HIGH ? 9 : 6;
        float time = Anim.timeSec() * palette.auroraSpeed();

        VertexConsumer buf = consumers.getBuffer(Types.aurora());
        Entry pose = matrices.peek();
        for (int i = 0; i < bands; i++) {
            drawBand(buf, pose, palette, BANDS[i], segments, vertical, time, strength);
        }
    }

    private void drawBand(VertexConsumer buf, Entry pose, SkyPalette palette, Band band,
                          int segments, int vertical, float time, float strength) {
        int cA = palette.auroraA() & 0x00FFFFFF;
        int cB = palette.auroraB() & 0x00FFFFFF;

        for (int x = 0; x < segments; x++) {
            float u0 = (float) x / segments;
            float u1 = (float) (x + 1) / segments;

            for (int y = 0; y < vertical; y++) {
                float v0 = (float) y / vertical;
                float v1 = (float) (y + 1) / vertical;

                Point p00 = point(band, u0, v0, time);
                Point p10 = point(band, u1, v0, time);
                Point p11 = point(band, u1, v1, time);
                Point p01 = point(band, u0, v1, time);

                float a0 = softEdge(v0) * density(band, u0, v0, time);
                float a1 = softEdge(v0) * density(band, u1, v0, time);
                float a2 = softEdge(v1) * density(band, u1, v1, time);
                float a3 = softEdge(v1) * density(band, u0, v1, time);

                float m0 = smooth(v0);
                float m1 = smooth(v1);

                int col00 = Theme.alpha(Theme.lerp(cA, cB, m0) & 0x00FFFFFF,
                    strength * band.alpha * a0);
                int col10 = Theme.alpha(Theme.lerp(cA, cB, m0) & 0x00FFFFFF,
                    strength * band.alpha * a1);
                int col11 = Theme.alpha(Theme.lerp(cA, cB, m1) & 0x00FFFFFF,
                    strength * band.alpha * a2);
                int col01 = Theme.alpha(Theme.lerp(cA, cB, m1) & 0x00FFFFFF,
                    strength * band.alpha * a3);

                float scroll = time * 0.007F + band.phase * 0.08F;

                buf.vertex(pose, p00.x, p00.y, p00.z)
                    .texture(u0 * 4.2F + scroll, v0)
                    .color(col00);
                buf.vertex(pose, p10.x, p10.y, p10.z)
                    .texture(u1 * 4.2F + scroll, v0)
                    .color(col10);
                buf.vertex(pose, p11.x, p11.y, p11.z)
                    .texture(u1 * 4.2F + scroll, v1)
                    .color(col11);
                buf.vertex(pose, p01.x, p01.y, p01.z)
                    .texture(u0 * 4.2F + scroll, v1)
                    .color(col01);
            }
        }
    }

    private static Point point(Band b, float u, float v, float time) {
        float angle = b.start + u * b.span;
        float curtain = (float) Math.sin(v * Math.PI);

        float broad = (float) Math.sin(
            angle * 1.25F + time * 0.105F + b.phase
        ) * b.sway;

        float mid = (float) Math.sin(
            angle * 3.35F - time * 0.070F + v * 2.15F + b.phase * 1.4F
        ) * (0.8F + 1.8F * v);

        float fine = (float) Math.sin(
            angle * 7.0F + time * 0.040F + v * 5.0F + b.phase * 2.1F
        ) * 0.45F * curtain;

        float radial = b.radius + broad + mid + fine;

        float floorWave = (float) Math.sin(
            angle * 1.10F + time * 0.08F + b.phase
        ) * 0.75F;

        float ceilingWave = (float) Math.sin(
            angle * 1.55F - time * 0.065F + b.phase * 1.35F
        ) * 2.2F * v;

        float y = b.base + (b.top - b.base) * v + floorWave + ceilingWave;

        float tangent = (float) Math.sin(
            angle * 2.85F + time * 0.055F + v * 2.2F + b.phase
        ) * 0.65F * curtain;

        float x = (float) Math.cos(angle) * radial;
        float z = (float) Math.sin(angle) * radial;

        x += -(float) Math.sin(angle) * tangent;
        z += (float) Math.cos(angle) * tangent;

        return new Point(x, y, z);
    }

    private static float softEdge(float v) {
        // The lower edge of a real aurora is sharp; the shader shapes it, the vertex alpha only has to stay out of the way.
        float bottom = smoothstep(0.0F, 0.02F, v);
        float top = 1.0F - smoothstep(0.80F, 1.0F, v);
        float middle = 0.90F + 0.10F * (float) Math.sin(v * Math.PI);
        return bottom * top * middle;
    }

    private static float density(Band b, float u, float v, float time) {
        float large = (float) Math.sin(
            u * 17.0F + v * 2.0F + time * 0.14F + b.phase
        );
        float fine = (float) Math.sin(
            u * 39.0F - v * 3.0F - time * 0.075F + b.phase * 1.7F
        );
        return Math.max(0.30F, Math.min(1.0F, 0.80F + 0.11F * large + 0.09F * fine));
    }

    private static float smooth(float x) {
        return x * x * (3.0F - 2.0F * x);
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = Math.max(0.0F, Math.min(1.0F, (x - e0) / (e1 - e0)));
        return t * t * (3.0F - 2.0F * t);
    }

    private record Band(float radius, float start, float span, float base, float top,
                        float sway, float phase, float alpha) { }

    private record Point(float x, float y, float z) { }
}
