package dev.doorevisuals.mix;

import dev.doorevisuals.cosmetics.CosmeticsFeature;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel.LayerType;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeLayerMixin {
    @Shadow
    @Final
    private BipedEntityModel<PlayerEntityRenderState> model;

    @Shadow
    private boolean hasCustomModelForLayer(ItemStack stack, LayerType type) {
        throw new AssertionError();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void doore$cape(
        MatrixStack pose, OrderedRenderCommandQueue collector, int light, PlayerEntityRenderState state, float yaw, float pitch, CallbackInfo ci
    ) {
        Identifier identifier = CosmeticsFeature.capeTexture(state);
        if (identifier != null) {
            if (!state.invisible && !this.hasCustomModelForLayer(state.equippedChestStack, LayerType.WINGS)) {
                pose.push();
                if (this.hasCustomModelForLayer(state.equippedChestStack, LayerType.HUMANOID)) {
                    pose.translate(0.0F, -0.053125F, 0.06875F);
                }

                collector.submitModel(
                    this.model, state, pose, RenderLayers.entityTranslucent(identifier), light, OverlayTexture.DEFAULT_UV, state.outlineColor, null
                );
                pose.pop();
                ci.cancel();
            } else {
                ci.cancel();
            }
        }
    }
}
