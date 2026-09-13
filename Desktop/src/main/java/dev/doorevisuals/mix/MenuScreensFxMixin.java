package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.ui.MenuAtmosphere;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MenuScreensFxMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void doore$menuBrand(DrawContext g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (App.live()) {
            Screen screen = (Screen)this;
            String s = titleOf(screen);
            if (s != null) {
                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                int i = minecraftclient.getWindow().getScaledWidth();
                int j = minecraftclient.getWindow().getScaledHeight();
                MenuAtmosphere.paintBackground(g, i, j);
                MenuAtmosphere.paintOverlay(g, i, j, s);
                ci.cancel();
            }
        }
    }

    private static String titleOf(Screen self) {
        if (self instanceof SelectWorldScreen) {
            return "\u041e\u0434\u0438\u043d\u043e\u0447\u043d\u0430\u044f \u0438\u0433\u0440\u0430";
        } else if (self instanceof MultiplayerScreen) {
            return "\u041c\u0443\u043b\u044c\u0442\u0438\u043f\u043b\u0435\u0435\u0440";
        } else {
            return self instanceof OptionsScreen ? "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438" : null;
        }
    }
}
