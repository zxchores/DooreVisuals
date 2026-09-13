package dev.doorevisuals.mix;

import dev.doorevisuals.tools.SwingFeature;
import dev.doorevisuals.tools.ViewmodelFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HandMixin {
    @Inject(method = "applyEquipOffset", at = @At("TAIL"))
    private void doore$viewmodel(MatrixStack pose, Arm arm, float equipProgress, CallbackInfo ci) {
        if (ViewmodelFeature.active() && !holdingMap()) {
            ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
            if (clientplayerentity != null) {
                boolean flag = arm != clientplayerentity.getMainArm();
                if (flag) {
                    pose.translate(ViewmodelFeature.offX(), ViewmodelFeature.offY(), ViewmodelFeature.offZ());
                    float f = ViewmodelFeature.offS();
                    pose.scale(f, f, f);
                } else {
                    pose.translate(ViewmodelFeature.mainX(), ViewmodelFeature.mainY(), ViewmodelFeature.mainZ());
                    float f1 = ViewmodelFeature.mainS();
                    pose.scale(f1, f1, f1);
                }
            }
        }
    }

    @Inject(method = "renderArmHoldingItem", at = @At("TAIL"))
    private void doore$armGlow(MatrixStack pose, OrderedRenderCommandQueue collector, int light, float equipped, float swing, Arm arm, CallbackInfo ci) {
        ViewmodelFeature.submitGlow(pose, collector, arm);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("TAIL"))
    private void doore$itemGlow(
        AbstractClientPlayerEntity player,
        float partialTicks,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        MatrixStack pose,
        OrderedRenderCommandQueue collector,
        int packedLight,
        CallbackInfo ci
    ) {
        if (stack != null && !stack.isEmpty()) {
            ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
            Arm arm = clientplayerentity == null
                ? Arm.RIGHT
                : (hand == Hand.MAIN_HAND ? clientplayerentity.getMainArm() : clientplayerentity.getMainArm().getOpposite());
            ViewmodelFeature.submitGlow(pose, collector, arm);
        }
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void doore$swingArm(float swingProgress, MatrixStack pose, int armX, Arm arm, CallbackInfo ci) {
        if (SwingFeature.active() && !holdingMap()) {
            ci.cancel();
            ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
            if (clientplayerentity != null && !SwingFeature.staticSwing() && !(swingProgress <= 0.001F)) {
                if (arm == clientplayerentity.getMainArm()) {
                    if (SwingFeature.isPlusStyle()) {
                        applyPlus(pose, arm, swingProgress);
                    } else {
                        applyLegacy(pose, arm, swingProgress);
                    }
                }
            }
        }
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void doore$suppressAttack(MatrixStack pose, Arm arm, float swing, CallbackInfo ci) {
        if (SwingFeature.active() && !holdingMap()) {
            ci.cancel();
        }
    }

    private static boolean holdingMap() {
        ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
        return clientplayerentity == null ? false : isMap(clientplayerentity.getMainHandStack()) || isMap(clientplayerentity.getOffHandStack());
    }

    private static boolean isMap(ItemStack stack) {
        return !stack.isEmpty() && (stack.isOf(Items.FILLED_MAP) || stack.isOf(Items.MAP));
    }

    private static void applyPlus(MatrixStack pose, Arm arm, float swingProgress) {
        float f = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        float f1 = (float)Math.sin(swingProgress * Math.PI);
        float f2 = arm == Arm.LEFT ? -1.0F : 1.0F;
        float f3 = SwingFeature.strength();
        String s = SwingFeature.style();
        switch (s) {
            case "\u0420\u0435\u0436\u0438\u043c 1":
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f2 * (45.0F + f1 * -20.0F)));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * f * -20.0F));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -80.0F));
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f2 * -45.0F));
                break;
            case "\u0420\u0435\u0436\u0438\u043c 2":
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f2 * -60.0F));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * (110.0F + f3 * f)));
                break;
            case "\u0420\u0435\u0436\u0438\u043c 3":
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50.0F));
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f2 * (-30.0F * (1.0F - f) - 30.0F + (f3 - 20.0F) * f)));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * 110.0F));
                break;
            case "\u0420\u0435\u0436\u0438\u043c 4":
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f2 * 90.0F));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f2 * -30.0F));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - f3 * f1 + 10.0F));
                break;
            case "\u0420\u0435\u0436\u0438\u043c 5":
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingProgress * -360.0F));
        }
    }

    private static void applyLegacy(MatrixStack pose, Arm arm, float swing) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = SwingFeature.power();
        float f1 = MathHelper.clamp(swing, 0.0F, 1.0F);
        float f2 = f1 * f1 * (3.0F - 2.0F * f1);
        float f3 = (float)Math.sin(f1 * Math.PI);
        float f4 = (float)Math.sin(f1 * Math.PI * 1.15F);
        float f5 = f1 < 0.35F ? f3 * f3 : f3;
        float f6 = f1 < 0.55F ? f2 : 1.0F - (f1 - 0.55F) / 0.45F;
        float f7 = MathHelper.sin(MathHelper.sqrt(f1) * (float) Math.PI);
        pose.translate(i * -0.4F * f7 * 0.35F * f, 0.2F * f7 * 0.25F * f, -0.2F * f3 * 0.3F * f);
        String s = SwingFeature.style();
        switch (s) {
            case "\u041a\u043b\u0430\u0441\u0441\u0438\u043a\u0430":
                pose.translate(i * 0.05F * f3 * f, 0.02F * f3 * f, -0.14F * f3 * f);
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (22.0F + f4 * 16.0F * f)));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-52.0F * f3 * f));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -12.0F * f3 * f));
                break;
            case "\u041f\u043b\u0430\u0432\u043d\u044b\u0439":
                pose.translate(i * 0.025F * f2 * f, 0.01F * f2 * f, -0.08F * f2 * f);
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (18.0F * f2 * f)));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -8.0F * f2 * f));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-42.0F * f2 * f));
                break;
            case "\u0420\u0435\u0437\u043a\u0438\u0439":
                pose.translate(i * 0.015F * f5, 0.04F * f5 * f, -0.2F * f5 * f);
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (70.0F * f4 * f)));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-118.0F * f5 * f));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -14.0F * f5));
                break;
            case "\u041a\u0440\u0443\u0433\u043e\u0432\u043e\u0439":
                pose.translate(0.0F, 0.03F * f3, -0.05F * f3 * f);
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (110.0F * f2 * f)));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * (-55.0F * f3 * f)));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * f3 * f));
                break;
            case "\u0421\u0442\u0438\u043b\u0435\u0442":
                pose.translate(i * 0.01F * f3, -0.06F * f3 * f, -0.28F * f3 * f);
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-138.0F * f3 * f));
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 6.0F * f4));
                pose.scale(1.0F, 1.0F, 1.0F + 0.08F * f3 * f);
                break;
            case "\u0422\u044f\u0436\u0451\u043b\u044b\u0439":
                pose.translate(i * 0.03F * f3, 0.09F * f6 * f, -0.04F * f3 * f);
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-78.0F * f3 * f));
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (32.0F + 24.0F * f4)));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * -32.0F * f3 * f));
                break;
            case "\u0425\u0443\u043a":
                pose.translate(i * 0.16F * f3 * f, 0.02F * f3, -0.09F * f3 * f);
                pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (-68.0F * f3 * f)));
                pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * (-42.0F * f3 * f)));
                pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-28.0F * f3 * f));
        }
    }
}
