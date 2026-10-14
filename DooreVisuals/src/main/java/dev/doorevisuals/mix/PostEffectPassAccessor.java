package dev.doorevisuals.mix;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.Map;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the motion blur replace the uniform buffers of its post pass each frame (they are baked at load time). */
@Mixin(PostEffectPass.class)
public interface PostEffectPassAccessor {
    @Accessor("uniformBuffers")
    Map<String, GpuBuffer> doore$uniformBuffers();
}
