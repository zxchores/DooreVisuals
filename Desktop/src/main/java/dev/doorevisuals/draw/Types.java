package dev.doorevisuals.draw;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

public final class Types {
    private static final RenderLayer FILL = sorted("dv3_fill", Gpu.FILL_PIPE);
    private static final RenderLayer GLOW = sorted("dv3_glow", Gpu.GLOW_PIPE);
    private static final RenderLayer PLASMA = sorted("dv3_plasma", Gpu.PLASMA_PIPE);
    private static final RenderLayer HOLO = sorted("dv3_holo", Gpu.HOLO_PIPE);
    private static final RenderLayer SCAN = sorted("dv3_scan", Gpu.SCAN_PIPE);
    private static final RenderLayer GHOST = sorted("dv3_ghost", Gpu.GHOST_PIPE);
    private static final RenderLayer ORB = sorted("dv3_orb", Gpu.ORB_PIPE);
    private static final RenderLayer LINES = RenderLayer.of("dv3_lines", RenderSetup.builder(RenderPipelines.LINES).build());
    private static final RenderLayer LINES_TX = RenderLayer.of(
        "dv3_lines_tx", RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT).translucent().build()
    );

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

    public static RenderLayer lines() {
        return LINES;
    }

    public static RenderLayer linesTx() {
        return LINES_TX;
    }
}
