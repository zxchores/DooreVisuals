package dev.doorevisuals.draw;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class Gpu {
    private static final Identifier FILL = id("core/esp_fill");
    private static final Identifier GLOW = id("core/esp_glow");
    private static final Identifier PLASMA = id("core/esp_plasma");
    private static final Identifier HOLO = id("core/esp_holo");
    private static final Identifier SCAN = id("core/esp_scan");
    private static final Identifier GHOST = id("core/esp_ghost");
    private static final Identifier ORB = id("core/esp_orb");
    public static final RenderPipeline FILL_PIPE = quad("pipeline/v3_fill", FILL, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline GLOW_PIPE = quad("pipeline/v3_glow", GLOW, BlendFunction.LIGHTNING);
    public static final RenderPipeline PLASMA_PIPE = quad("pipeline/v3_plasma", PLASMA, BlendFunction.LIGHTNING);
    public static final RenderPipeline HOLO_PIPE = quad("pipeline/v3_holo", HOLO, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline SCAN_PIPE = quad("pipeline/v3_scan", SCAN, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline GHOST_PIPE = quad("pipeline/v3_ghost", GHOST, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline ORB_PIPE = RenderPipelines.register(
        RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
            .withLocation(id("pipeline/v3_orb"))
            .withVertexShader(ORB)
            .withFragmentShader(ORB)
            .withBlend(BlendFunction.LIGHTNING)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
            .build()
    );
    private static final Identifier SKY = id("core/atmo_sky");
    public static final RenderPipeline SKY_PIPE = quad("pipeline/v3_sky", SKY, BlendFunction.TRANSLUCENT);

    private Gpu() {
    }

    public static void warm() {
        FILL_PIPE.getClass();
        GLOW_PIPE.getClass();
        PLASMA_PIPE.getClass();
        HOLO_PIPE.getClass();
        SCAN_PIPE.getClass();
        GHOST_PIPE.getClass();
        ORB_PIPE.getClass();
        SKY_PIPE.getClass();
    }

    private static RenderPipeline quad(String loc, Identifier shader, BlendFunction blend) {
        return RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
                .withLocation(id(loc))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withBlend(blend)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
                .build()
        );
    }

    private static Identifier id(String path) {
        return Identifier.of("doorevisuals", path);
    }
}
