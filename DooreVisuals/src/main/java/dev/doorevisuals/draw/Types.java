package dev.doorevisuals.draw;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;

public final class Types {
    private static final RenderLayer FILL = sorted("dv3_fill", Gpu.FILL_PIPE);
    private static final RenderLayer GLOW = sorted("dv3_glow", Gpu.GLOW_PIPE);
    private static final RenderLayer PLASMA = sorted("dv3_plasma", Gpu.PLASMA_PIPE);
    private static final RenderLayer HAND_GLOW = sorted("dv3_hand_glow", Gpu.HAND_GLOW_PIPE);
    private static final RenderLayer BLOCK_SHIMMER = sorted("dv3_block_shimmer", Gpu.BLOCK_SHIMMER_PIPE);
    private static final RenderLayer BLOCK_SHIMMER_LINES = RenderLayer.of("dv3_block_shimmer_lines", RenderSetup.builder(Gpu.BLOCK_SHIMMER_LINES_PIPE).translucent().build());
    private static final RenderLayer HOLO = sorted("dv3_holo", Gpu.HOLO_PIPE);
    private static final RenderLayer SCAN = sorted("dv3_scan", Gpu.SCAN_PIPE);
    private static final RenderLayer GHOST = sorted("dv3_ghost", Gpu.GHOST_PIPE);
    private static final RenderLayer ORB = sorted("dv3_orb", Gpu.ORB_PIPE);
    private static final RenderLayer SKY = RenderLayer.of("dv3_sky", RenderSetup.builder(Gpu.SKY_PIPE)
        .texture("Sampler0", dev.doorevisuals.world.sky.MoonTexture.ID).translucent().build());
    private static final RenderLayer CLOUD = sorted("dv3_cloud", Gpu.CLOUD_PIPE);
    private static final RenderLayer AURORA = sorted("dv3_aurora", Gpu.AURORA_PIPE);
    private static final RenderLayer LINES = RenderLayer.of("dv3_lines", RenderSetup.builder(RenderPipelines.LINES).build());
    private static final RenderLayer LINES_TX = RenderLayer.of(
        "dv3_lines_tx", RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT).translucent().build()
    );

    private static final Map<String, RenderLayer> HAND_SURFACES = new ConcurrentHashMap<>();
    private static final Map<String, RenderLayer> HAND_WAVES = new ConcurrentHashMap<>();

    private Types() {
    }

    private static RenderLayer sorted(String name, RenderPipeline pipe) {
        return RenderLayer.of(name, RenderSetup.builder(pipe).translucent().build());
    }

    public static RenderLayer fill() {
        return FILL;
    }

    public static RenderLayer glow() {
        return GLOW;
    }

    public static RenderLayer plasma() {
        return PLASMA;
    }

    public static RenderLayer handGlow() {
        return HAND_GLOW;
    }

    /**
     * Слой обводки руки: пайплайн стиля плюс текстура скина как маска. Слои кэшируются по паре
     * «стиль + текстура»: игроки меняются, а пересоздавать слой каждый кадр незачем.
     */
    public static RenderLayer handSurface(int style, Identifier texture) {
        return HAND_SURFACES.computeIfAbsent(
            style + "|" + texture,
            key -> RenderLayer.of(
                "dv3_hand_surface_" + style + "_" + texture.getPath().replace('/', '_'),
                RenderSetup.builder(Gpu.handSurfacePipe(style))
                    .texture("Sampler0", texture)
                    .texture("Sampler1", AccentTexture.ID)
                    .translucent()
                    .build()
            )
        );
    }

    /** The Hand Shader waves over the given skin (the mask). */
    public static RenderLayer handWave(int style, Identifier texture) {
        return handWave(style, texture, false);
    }

    /** The waves over a texture used as the mask: the skin for the hand, the atlas of the item for the item in the hand. */
    public static RenderLayer handWave(int style, Identifier texture, boolean item) {
        return HAND_WAVES.computeIfAbsent(
            style + "|" + item + "|" + texture,
            key -> RenderLayer.of(
                "dv3_hand_wave_" + style + (item ? "_item_" : "_") + texture.getPath().replace('/', '_'),
                RenderSetup.builder(Gpu.handWavePipe(style, item))
                    .texture("Sampler0", texture)
                    .translucent()
                    .build()
            )
        );
    }

    public static RenderLayer blockShimmer() { return BLOCK_SHIMMER; }
    public static RenderLayer blockShimmerLines() { return BLOCK_SHIMMER_LINES; }

    public static RenderLayer holo() {
        return HOLO;
    }

    public static RenderLayer scan() {
        return SCAN;
    }

    public static RenderLayer ghost() {
        return GHOST;
    }

    public static RenderLayer orb() {
        return ORB;
    }

    public static RenderLayer sky() {
        return SKY;
    }

    public static RenderLayer cloud() {
        return CLOUD;
    }

    public static RenderLayer aurora() {
        return AURORA;
    }

    public static RenderLayer lines() {
        return LINES;
    }

    public static RenderLayer linesTx() {
        return LINES_TX;
    }
}
