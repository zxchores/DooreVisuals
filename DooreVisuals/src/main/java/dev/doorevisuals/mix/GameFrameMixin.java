package dev.doorevisuals.mix;

import dev.doorevisuals.draw.PostFx;
import dev.doorevisuals.tools.PostFxFeature;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Применяет пост-обработку в самом конце кадра: к этому моменту мир, сущности и интерфейс уже
 * лежат в главном буфере, поэтому bloom собирает свет со всей сцены сразу.
 */
@Mixin(GameRenderer.class)
public abstract class GameFrameMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void doore$postFx(
            RenderTickCounter counter,
            boolean tick,
            CallbackInfo ci
    ) {
        dev.doorevisuals.tools.ClipsFeature.frame();
        if (PostFxFeature.active()) {
            PostFx.frame();
        }
    }
}
