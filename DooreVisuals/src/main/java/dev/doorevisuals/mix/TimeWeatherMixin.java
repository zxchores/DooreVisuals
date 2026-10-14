package dev.doorevisuals.mix;

import dev.doorevisuals.tools.TimeWeatherFeature;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class TimeWeatherMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void doore$lockTime(CallbackInfoReturnable<Long> cir) {
        if (TimeWeatherFeature.timeLocked()) {
            cir.setReturnValue((long)TimeWeatherFeature.lockedTime());
        }
    }

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void doore$rain(CallbackInfoReturnable<Boolean> cir) {
        String s = TimeWeatherFeature.weatherMode();
        if ("clear".equals(s) || "snow".equals(s) || "fog".equals(s)) {
            cir.setReturnValue(false);
        } else if ("rain".equals(s) || "thunder".equals(s)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void doore$thunder(CallbackInfoReturnable<Boolean> cir) {
        String s = TimeWeatherFeature.weatherMode();
        if ("thunder".equals(s)) {
            cir.setReturnValue(true);
        } else if ("clear".equals(s) || "rain".equals(s) || "snow".equals(s) || "fog".equals(s)) {
            cir.setReturnValue(false);
        }
    }
}
