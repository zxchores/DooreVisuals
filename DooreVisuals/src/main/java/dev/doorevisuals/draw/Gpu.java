package dev.doorevisuals.draw;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class Gpu {
    private static final Identifier FILL = id("core/esp_fill");
    private static final Identifier GLOW = id("core/esp_glow");
    private static final Identifier PLASMA = id("core/esp_plasma");
    private static final Identifier HAND_GLOW = id("core/hand_glow");
    private static final Identifier HAND_SURFACE = id("core/hand_surface");
    private static final Identifier HAND_WAVE = id("core/hand_wave");
    private static final Identifier BLOCK_SHIMMER = id("core/block_shimmer");
    private static final Identifier HOLO = id("core/esp_holo");
    private static final Identifier SCAN = id("core/esp_scan");
    private static final Identifier GHOST = id("core/esp_ghost");
    private static final Identifier ORB = id("core/esp_orb");
    public static final RenderPipeline FILL_PIPE = quad("pipeline/v3_fill", FILL, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline GLOW_PIPE = quad("pipeline/v3_glow", GLOW, BlendFunction.LIGHTNING);
    public static final RenderPipeline PLASMA_PIPE = quad("pipeline/v3_plasma", PLASMA, BlendFunction.LIGHTNING);
    public static final RenderPipeline HAND_GLOW_PIPE = quad("pipeline/v3_hand_glow", HAND_GLOW, BlendFunction.LIGHTNING);
    public static final RenderPipeline BLOCK_SHIMMER_PIPE = pipe("pipeline/v3_block_shimmer", BLOCK_SHIMMER, BlendFunction.TRANSLUCENT, VertexFormats.POSITION_COLOR_NORMAL);
    public static final RenderPipeline BLOCK_SHIMMER_LINES_PIPE = linePipe("pipeline/v3_block_shimmer_lines", BLOCK_SHIMMER, BlendFunction.LIGHTNING, VertexFormats.POSITION_COLOR_NORMAL);
    public static final RenderPipeline HOLO_PIPE = quad("pipeline/v3_holo", HOLO, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline SCAN_PIPE = quad("pipeline/v3_scan", SCAN, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline GHOST_PIPE = quad("pipeline/v3_ghost", GHOST, BlendFunction.TRANSLUCENT);
    private static final Identifier SKY = id("core/atmo_sky");
    private static final Identifier CLOUD = id("core/atmo_cloud");
    private static final Identifier AURORA = id("core/atmo_aurora");
    public static final RenderPipeline ORB_PIPE = textured("pipeline/v3_orb", ORB, BlendFunction.LIGHTNING);
    public static final RenderPipeline SKY_PIPE = sky();
    public static final RenderPipeline CLOUD_PIPE = pipe("pipeline/v3_cloud", CLOUD, BlendFunction.TRANSLUCENT, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
    public static final RenderPipeline AURORA_PIPE = textured("pipeline/v3_aurora", AURORA, BlendFunction.LIGHTNING);
    private static final Identifier UI_RECT = id("core/ui_rect");
    public static final RenderPipeline UI_RECT_PIPE = uiRect();
    public static final RenderPipeline HAND_SURFACE_AURA_PIPE = handSurface("aura", 0);
    public static final RenderPipeline HAND_SURFACE_NEON_PIPE = handSurface("neon", 1);
    public static final RenderPipeline HAND_SURFACE_PULSE_PIPE = handSurface("pulse", 2);
    public static final RenderPipeline HAND_SURFACE_OUTLINE_PIPE = handSurface("outline", 3);
    public static final RenderPipeline HAND_WAVE_WAVES_PIPE = handWave("waves", 0);
    public static final RenderPipeline HAND_WAVE_RIPPLES_PIPE = handWave("ripples", 1);
    public static final RenderPipeline HAND_WAVE_PULSE_PIPE = handWave("pulse", 2);
    public static final RenderPipeline HAND_WAVE_OUTLINE_PIPE = handWave("outline", 3);
    public static final RenderPipeline HAND_WAVE_ITEM_WAVES_PIPE = handWave("item_waves", 0, true);
    public static final RenderPipeline HAND_WAVE_ITEM_RIPPLES_PIPE = handWave("item_ripples", 1, true);
    public static final RenderPipeline HAND_WAVE_ITEM_PULSE_PIPE = handWave("item_pulse", 2, true);
    public static final RenderPipeline HAND_WAVE_ITEM_OUTLINE_PIPE = handWave("item_outline", 3, true);

    private Gpu() {
    }

    /**
     * Compiles every pipeline of the client now, at start, instead of at the moment an effect is first drawn, which is where
     * the first-use stutter came from. A pipeline that fails to compile is only logged here, so one bad shader cannot stop
     * the others or the start.
     */
    public static void precompileAll() {
        long started = System.nanoTime();
        int done = 0;
        for (java.lang.reflect.Field field : Gpu.class.getDeclaredFields()) {
            if (RenderPipeline.class.isAssignableFrom(field.getType()) && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    com.mojang.blaze3d.systems.RenderSystem.getDevice().precompilePipeline((RenderPipeline)field.get(null));
                    done++;
                } catch (Throwable error) {
                    org.slf4j.LoggerFactory.getLogger("DooreVisuals/Gpu").warn("Pipeline {} did not precompile: {}", field.getName(), error.toString());
                }
            }
        }
        org.slf4j.LoggerFactory.getLogger("DooreVisuals/Gpu").info("Precompiled {} pipelines in {} ms", done, (System.nanoTime() - started) / 1_000_000L);
    }

    public static void warm() {
        FILL_PIPE.getClass();
        GLOW_PIPE.getClass();
        PLASMA_PIPE.getClass();
        HAND_GLOW_PIPE.getClass();
        HAND_SURFACE_AURA_PIPE.getClass();
        HAND_SURFACE_NEON_PIPE.getClass();
        HAND_SURFACE_PULSE_PIPE.getClass();
        HAND_SURFACE_OUTLINE_PIPE.getClass();
        HAND_WAVE_WAVES_PIPE.getClass();
        HAND_WAVE_RIPPLES_PIPE.getClass();
        HAND_WAVE_PULSE_PIPE.getClass();
        HAND_WAVE_OUTLINE_PIPE.getClass();
        HAND_WAVE_ITEM_WAVES_PIPE.getClass();
        BLOCK_SHIMMER_PIPE.getClass();
        BLOCK_SHIMMER_LINES_PIPE.getClass();
        HOLO_PIPE.getClass();
        SCAN_PIPE.getClass();
        GHOST_PIPE.getClass();
        ORB_PIPE.getClass();
        SKY_PIPE.getClass();
        CLOUD_PIPE.getClass();
        AURORA_PIPE.getClass();
        UI_RECT_PIPE.getClass();
    }

    /**
     * The only 2D pipeline in the client. GUI draws are ordered by the layers the vanilla render
     * state builds rather than by depth, so the depth test goes away and the extra UV slot carries
     * the corner radius and border width the distance field needs.
     */
    private static RenderPipeline uiRect() {
        return RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET})
                .withLocation(id("pipeline/v3_ui_rect"))
                .withVertexShader(UI_RECT)
                .withFragmentShader(UI_RECT)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, DrawMode.QUADS)
                .build()
        );
    }

    private static RenderPipeline quad(String loc, Identifier shader, BlendFunction blend) {
        return pipe(loc, shader, blend, VertexFormats.POSITION_COLOR);
    }

    private static RenderPipeline textured(String loc, Identifier shader, BlendFunction blend) {
        return pipe(loc, shader, blend, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    private static RenderPipeline linePipe(String loc, Identifier shader, BlendFunction blend, VertexFormat format) {
        return RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
                .withLocation(id(loc)).withVertexShader(shader).withFragmentShader(shader).withBlend(blend)
                .withDepthWrite(false).withCull(false).withVertexFormat(format, DrawMode.LINES).build()
        );
    }

    /** Пайплайн нужного стиля обводки руки: стиль задаётся дефайном, шейдер один и тот же. */
    public static RenderPipeline handSurfacePipe(int style) {
        return switch (style) {
            case 1 -> HAND_SURFACE_NEON_PIPE;
            case 2 -> HAND_SURFACE_PULSE_PIPE;
            case 3 -> HAND_SURFACE_OUTLINE_PIPE;
            default -> HAND_SURFACE_AURA_PIPE;
        };
    }

    /**
     * Обводка руки рисует ту же часть модели, что и ванильный проход, поэтому пайплайн принимает полный
     * формат модели игрока и подвязывает текстуру скина как маску. Стиль передаётся дефайном, поэтому
     * один шейдер даёт четыре разные картинки (Аура / Неон / Пульс / Контур).
     */
    private static RenderPipeline handSurface(String name, int style) {
        return RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
                .withLocation(id("pipeline/v3_hand_surface_" + name))
                .withVertexShader(HAND_SURFACE)
                .withFragmentShader(HAND_SURFACE)
                .withShaderDefine("DOORE_STYLE", style)
                .withSampler("Sampler0")
                .withSampler("Sampler1")
                .withBlend(BlendFunction.LIGHTNING)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
                .build()
        );
    }

    public static RenderPipeline handWavePipe(int style, boolean item) {
        if (item) {
            return switch (style) {
                case 1 -> HAND_WAVE_ITEM_RIPPLES_PIPE;
                case 2 -> HAND_WAVE_ITEM_PULSE_PIPE;
                case 3 -> HAND_WAVE_ITEM_OUTLINE_PIPE;
                default -> HAND_WAVE_ITEM_WAVES_PIPE;
            };
        }
        return switch (style) {
            case 1 -> HAND_WAVE_RIPPLES_PIPE;
            case 2 -> HAND_WAVE_PULSE_PIPE;
            case 3 -> HAND_WAVE_OUTLINE_PIPE;
            default -> HAND_WAVE_WAVES_PIPE;
        };
    }

    private static RenderPipeline handWave(String name, int style) {
        return handWave(name, style, false);
    }

    /**
     * The Hand Shader waves: the vertex format of the player model, which is also the format of item quads. The colour,
     * opacity, speed and size ride in the vertex attributes, the wave phase comes from the game clock. For the item variant the
     * colour travels in the light value (item quads have no tint of their own to carry it).
     */
    private static RenderPipeline handWave(String name, int style, boolean item) {
        RenderPipeline.Builder builder = RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
            .withLocation(id("pipeline/v3_hand_wave_" + name))
            .withVertexShader(HAND_WAVE)
            .withFragmentShader(HAND_WAVE)
            .withShaderDefine("DOORE_WAVE_STYLE", style)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            // The same geometry as what was drawn first: a small bias towards the camera keeps the waves on top of it.
            .withDepthBias(-1.0F, -10.0F)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS);
        if (item) {
            builder = builder.withShaderDefine("DOORE_WAVE_ITEM", 1);
        }
        return RenderPipelines.register(builder.build());
    }

    private static RenderPipeline sky() {
        return RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
                .withLocation(id("pipeline/v3_sky"))
                .withVertexShader(SKY)
                .withFragmentShader(SKY)
                .withSampler("Sampler0")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthWrite(false)
                .withCull(false)
                // The sky needs more than the four channels of a plain colour-texture vertex: the
                // lunar phase and cloud coverage ride in UV1, the sun visibility in the normal.
                .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, DrawMode.QUADS)
                .build()
        );
    }

    private static RenderPipeline pipe(String loc, Identifier shader, BlendFunction blend, VertexFormat format) {
        return RenderPipelines.register(
            RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
                .withLocation(id(loc))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withBlend(blend)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(format, DrawMode.QUADS)
                .build()
        );
    }

    private static Identifier id(String path) {
        return Identifier.of("doorevisuals", path);
    }
}
