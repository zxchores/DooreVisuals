package dev.doorevisuals.mix;

import dev.doorevisuals.tools.SwingFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swing Speed: the first-person swing runs for as many ticks as this method returns, so scaling it for the local
 * player makes the animation shorter or longer. It only changes how long the swing is drawn; attacks, cooldowns and
 * what the server sees are not affected.
 */
@Mixin(LivingEntity.class)
public abstract class SwingDurationMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void doore$swingDuration(CallbackInfoReturnable<Integer> cir) {
        ClientPlayerEntity me = MinecraftClient.getInstance().player;
        if (me != null && (Object)this == me && SwingFeature.appliesTo(me.getStackInHand(me.preferredHand == null ? net.minecraft.util.Hand.MAIN_HAND : me.preferredHand))) {
            int scaled = Math.round(cir.getReturnValueI() * SwingFeature.durationScale());
            cir.setReturnValue(Math.max(2, Math.min(60, scaled)));
        }
    }
}
