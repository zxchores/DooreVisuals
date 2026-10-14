package dev.doorevisuals.mix;

import dev.doorevisuals.overlay.HudTweaksFeature;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScoreboardEntry.class)
public class ScoreEntryMixin {
    @Inject(method = "formatted", at = @At("HEAD"), cancellable = true)
    private void doore$hideScores(NumberFormat format, CallbackInfoReturnable<Text> cir) {
        if (HudTweaksFeature.scoreboardHideScores() && HudTweaksFeature.sidebarPaint()) {
            cir.setReturnValue(Text.empty());
        }
    }
}
