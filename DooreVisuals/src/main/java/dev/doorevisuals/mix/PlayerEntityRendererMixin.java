package dev.doorevisuals.mix;

import dev.doorevisuals.tools.HandShaderFeature;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Отдаёт Hand Shader ту самую часть руки, которую рисует ванильный
 * {@code PlayerEntityRenderer.renderArm}, — эффект ложится ровно на её геометрию,
 * а не на отдельный квад перед камерой.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(
            method = "renderArm",
            at = @At("TAIL")
    )
    private void doore$handShader(
            MatrixStack pose,
            OrderedRenderCommandQueue queue,
            int light,
            Identifier skin,
            ModelPart arm,
            boolean sleeveVisible,
            CallbackInfo ci
    ) {
        HandShaderFeature.renderArm(
                pose,
                queue,
                light,
                skin,
                arm
        );
    }
}
