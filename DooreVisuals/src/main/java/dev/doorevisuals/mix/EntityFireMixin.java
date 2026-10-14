package dev.doorevisuals.mix;

import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityFireMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void doore$noFire(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (NoRenderFeature.fire()) {
            state.onFire = false;
        }
    }
}
