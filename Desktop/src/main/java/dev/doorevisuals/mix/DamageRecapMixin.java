package dev.doorevisuals.mix;

import dev.doorevisuals.overlay.DeathRecap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class DamageRecapMixin {
    @Inject(method = "onDamaged", at = @At("HEAD"))
    private void doore$recap(DamageSource source, CallbackInfo ci) {
        LivingEntity livingentity = (LivingEntity)this;
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null && livingentity == minecraftclient.player && source != null) {
            Entity entity = source.getAttacker();
            if (entity == null) {
                entity = source.getSource();
            }

            String s = entity == null ? "" : entity.getName().getString();
            String s1 = "";
            if (entity instanceof LivingEntity livingentity1) {
                ItemStack itemstack = livingentity1.getMainHandStack();
                if (itemstack != null && !itemstack.isEmpty()) {
                    s1 = itemstack.getName().getString();
                }
            }

            String s2 = "";

            try {
                s2 = source.getDeathMessage(livingentity).getString();
            } catch (Throwable throwable) {
                s2 = source.toString();
            }

            DeathRecap.record(s, s1, s2);
        }
    }
}
