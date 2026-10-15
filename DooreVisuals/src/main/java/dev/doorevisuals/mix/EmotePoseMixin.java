package dev.doorevisuals.mix;

import dev.doorevisuals.cosmetics.EmotesFeature;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets Emotes pose the player model right after the game has set its angles. */
@Mixin(PlayerEntityModel.class)
public abstract class EmotePoseMixin {
    @Inject(
            method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("TAIL")
    )
    private void doore$emote(PlayerEntityRenderState state, CallbackInfo ci) {
        EmotesFeature.pose((PlayerEntityModel)(Object)this, state);
    }
}
