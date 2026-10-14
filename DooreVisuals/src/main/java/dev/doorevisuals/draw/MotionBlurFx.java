package dev.doorevisuals.draw;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.doorevisuals.mix.PostEffectPassAccessor;
import dev.doorevisuals.mix.PostEffectProcessorAccessor;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camera motion blur as a vanilla post-effect chain ({@code post_effect/motion_blur.json}). The chain's uniforms are baked
 * when it loads, so each frame the {@code MotionConfig} buffer of its first pass is replaced with a fresh one carrying the
 * current blur vector and sample count.
 *
 * <p>Loading is retried after a resource reload, and a render error pauses the effect for a while instead of switching it
 * off for good.</p>
 */
public final class MotionBlurFx {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/MotionBlur");
    private static final Set<Identifier> MAIN_TARGET = Set.of(PostEffectProcessor.MAIN);
    private static final String UNIFORM = "MotionConfig";
    private static final long RETRY_MS = 2000L;
    private static final long PAUSE_AFTER_ERROR_MS = 15000L;
    private static final int MAX_ATTEMPTS = 12;
    private static final int MAX_ERRORS = 3;

    private static PostEffectProcessor processor;
    private static GpuBuffer own;
    private static int attempts;
    private static int errors;
    private static long nextAttemptAt;

    private MotionBlurFx() {
    }

    /** Drops the cached chain, e.g. after a resource reload. */
    public static void reset() {
        processor = null;
        attempts = 0;
        nextAttemptAt = 0L;
    }

    /**
     * Blurs the main framebuffer along (dx, dy), given in uv units, using the given number of taps.
     *
     * @return whether the effect was applied
     */
    public static boolean frame(float dx, float dy, int samples) {
        if (errors >= MAX_ERRORS) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getFramebuffer() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < nextAttemptAt && processor == null) {
            return false;
        }
        if (processor == null && !obtain(mc, now)) {
            return false;
        }
        try {
            upload(processor, dx, dy, samples);
            processor.render(mc.getFramebuffer(), ObjectAllocator.TRIVIAL);
            return true;
        } catch (Throwable error) {
            errors++;
            processor = null;
            nextAttemptAt = now + PAUSE_AFTER_ERROR_MS;
            LOG.error("Размытие при движении: ошибка рендера ({}/{}): {}", errors, MAX_ERRORS, error.toString());
            return false;
        }
    }

    private static boolean obtain(MinecraftClient mc, long now) {
        if (attempts >= MAX_ATTEMPTS) {
            return false;
        }
        attempts++;
        nextAttemptAt = now + RETRY_MS;
        for (String candidate : new String[] {"doorevisuals:motion_blur", "doorevisuals:post/motion_blur", "doorevisuals:post_effect/motion_blur"}) {
            try {
                PostEffectProcessor loaded = mc.getShaderLoader().loadPostEffect(Identifier.of(candidate), MAIN_TARGET);
                if (loaded != null) {
                    processor = loaded;
                    LOG.info("Размытие при движении: цепочка {} загружена", candidate);
                    return true;
                }
            } catch (Throwable ignored) {
                // Try the next identifier form.
            }
        }
        if (attempts >= MAX_ATTEMPTS) {
            LOG.warn("Размытие при движении недоступно: цепочка не найдена за {} попыток", attempts);
        }
        return false;
    }

    /**
     * Gives every pass that has a MotionConfig block our own buffer (once) and writes the current values into it each
     * frame, so nothing is allocated per frame.
     */
    private static void upload(PostEffectProcessor chain, float dx, float dy, int samples) {
        if (own == null || own.isClosed()) {
            own = RenderSystem.getDevice().createBuffer(() -> "doore motion blur", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16L);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, 16).putVec2(dx, dy).putFloat((float)samples).putFloat(0.0F).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(own.slice(), data);
        }
        for (PostEffectPass pass : ((PostEffectProcessorAccessor)chain).doore$passes()) {
            Map<String, GpuBuffer> buffers = ((PostEffectPassAccessor)pass).doore$uniformBuffers();
            GpuBuffer current = buffers.get(UNIFORM);
            if (current != null && current != own) {
                buffers.put(UNIFORM, own);
                current.close();
            }
        }
    }
}
