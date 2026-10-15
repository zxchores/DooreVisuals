package dev.doorevisuals.mix;

import dev.doorevisuals.tools.SwingAnimations;
import dev.doorevisuals.tools.SwingFeature;
import dev.doorevisuals.tools.ViewmodelFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HandMixin {
    @Shadow
    private ItemStack mainHand;

    @Shadow
    private ItemStack offHand;


    /*
     * =========================
     * VIEWMODEL
     * =========================
     */
    @Inject(
            method = "applyEquipOffset",
            at = @At("TAIL")
    )
    private void doore$viewmodel(
            MatrixStack pose,
            Arm arm,
            float equipProgress,
            CallbackInfo ci
    ) {
        dev.doorevisuals.tools.ItemAnimationsFeature.swapPose(pose, arm, equipProgress);
        ClientPlayerEntity inspector = MinecraftClient.getInstance().player;
        if (inspector != null && arm == inspector.getMainArm()) {
            // The blade of Custom Sword: draw flourish, sway and its own inspect. Anything else can still be inspected.
            float[] handPose = dev.doorevisuals.tools.SwordFeature.handPose(this.mainHand, equipProgress);
            if (handPose == null && dev.doorevisuals.tools.InspectFeature.playing()) {
                float[] angles = dev.doorevisuals.tools.InspectFeature.angles();
                handPose = new float[] {0.0F, 0.0F, 0.0F, angles[0], angles[1], angles[2]};
            }
            if (handPose != null) {
                pose.translate(handPose[0], handPose[1], handPose[2]);
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(handPose[3]));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(handPose[4]));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(handPose[5]));
            }
        }
        if (ViewmodelFeature.active() && !holdingMap()) {
            ClientPlayerEntity player =
                    MinecraftClient.getInstance().player;

            if (player != null) {
                boolean offHand =
                        arm != player.getMainArm();

                if (offHand) {
                    pose.translate(
                            ViewmodelFeature.offX(),
                            ViewmodelFeature.offY(),
                            ViewmodelFeature.offZ()
                    );

                    float scale =
                            ViewmodelFeature.offS();

                    pose.scale(
                            scale,
                            scale,
                            scale
                    );
                } else {
                    pose.translate(
                            ViewmodelFeature.mainX(),
                            ViewmodelFeature.mainY(),
                            ViewmodelFeature.mainZ()
                    );

                    float scale =
                            ViewmodelFeature.mainS();

                    pose.scale(
                            scale,
                            scale,
                            scale
                    );
                }
            }
        }
    }

    /*
     * =========================
     * SWING ANIMATIONS
     * =========================
     * The vanilla swing is applied in applySwingOffset (held item) and swingArm (empty hand). With a custom type
     * chosen both are replaced by the pose of SwingAnimations; with Vanilla, or for items the module skips (Only
     * Swords), the game's own code runs untouched.
     */
    @Inject(
            method = "applySwingOffset",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doore$swingOffset(
            MatrixStack pose,
            Arm arm,
            float swing,
            CallbackInfo ci
    ) {
        if (!SwingFeature.active() || holdingMap()) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack held = arm == player.getMainArm() ? this.mainHand : this.offHand;
        if (!SwingFeature.appliesTo(held)) {
            return;
        }
        ci.cancel();
        SwingAnimations.apply(pose, arm, SwingFeature.shape(swing), SwingFeature.type(), SwingFeature.strength());
    }

    @Inject(
            method = "swingArm",
            at = @At("HEAD"),
            cancellable = true
    )
    private void doore$swingArm(
            float swingProgress,
            MatrixStack pose,
            int armX,
            Arm arm,
            CallbackInfo ci
    ) {
        if (!SwingFeature.active() || holdingMap() || !SwingFeature.appliesTo(ItemStack.EMPTY)) {
            return;
        }
        ci.cancel();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && arm == player.getMainArm()) {
            SwingAnimations.apply(pose, arm, SwingFeature.shape(swingProgress), SwingFeature.type(), SwingFeature.strength());
        }
    }

    /*
     * =========================
     * MAP CHECK
     * =========================
     */
    private static boolean holdingMap() {
        ClientPlayerEntity player =
                MinecraftClient.getInstance().player;

        if (player == null) {
            return false;
        }

        return isMap(player.getMainHandStack())
                || isMap(player.getOffHandStack());
    }

    private static boolean isMap(ItemStack stack) {
        return !stack.isEmpty()
                && (
                stack.isOf(Items.FILLED_MAP)
                        || stack.isOf(Items.MAP)
        );
    }
}
