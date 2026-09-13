package dev.doorevisuals.mix;

import dev.doorevisuals.ui.DooreDeathScreen;
import dev.doorevisuals.ui.DooreMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void doore$customTitle(Screen screen, CallbackInfo ci) {
        if (screen instanceof TitleScreen && !(screen instanceof DooreMenuScreen)) {
            ci.cancel();
            ((MinecraftClient)this).setScreen(new DooreMenuScreen());
        }
    }

    @Redirect(method = "setScreen", at = @At(value = "NEW", target = "(Lnet/minecraft/text/Text;ZLnet/minecraft/client/network/ClientPlayerEntity;)Lnet/minecraft/client/gui/screen/DeathScreen;"))
    private DeathScreen doore$deathScreen(Text cause, boolean hardcore, ClientPlayerEntity player) {
        return new DooreDeathScreen(cause, hardcore, player);
    }
}
