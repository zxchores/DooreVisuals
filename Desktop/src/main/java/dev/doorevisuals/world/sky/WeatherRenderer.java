package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Mesh;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec3d;

/** Thunder flashes plus the tint applied to vanilla rain and snow. */
public final class WeatherRenderer {
    private WeatherRenderer() {
    }

    public static void lightning(WorldRenderContext ctx, Vec3d cam) {
        float f = Anim.timeSec() * 11.11F % 17.0F;
        if (f < 2.0F) {
            Mesh.orb(ctx, cam.x, cam.y + 40.0, cam.z, 70.0F, Mesh.alpha(-1, 0.08F * (2.0F - f)));
        }
    }

    /**
     * Wraps the buffers vanilla precipitation draws into so its vertex colours pick up the palette.
     * Multiplying rather than replacing keeps the shading and the fade toward the edge of the field.
     */
    public static VertexConsumerProvider tinted(VertexConsumerProvider src, int rgb, float mix) {
        return mix <= 0.002F ? src : new WeatherRenderer.Tinted(src, rgb, Math.min(1.0F, mix));
    }

    private record Tinted(VertexConsumerProvider src, int rgb, float mix) implements VertexConsumerProvider {
        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            return new WeatherRenderer.TintedBuffer(this.src.getBuffer(layer), this.rgb, this.mix);
        }
    }

    private record TintedBuffer(VertexConsumer src, int rgb, float mix) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            this.src.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.src
                .color(
                    channel(red, this.rgb >> 16 & 0xFF),
                    channel(green, this.rgb >> 8 & 0xFF),
                    channel(blue, this.rgb & 0xFF),
                    alpha
                );
            return this;
        }

        @Override
        public VertexConsumer color(int argb) {
            return this.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            this.src.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            this.src.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            this.src.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.src.normal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            this.src.lineWidth(width);
            return this;
        }

        private int channel(int in, int target) {
            return Math.round(in * (1.0F - this.mix + this.mix * target / 255.0F));
        }
    }
}
