package dev.doorevisuals.mix;

import dev.doorevisuals.tools.ChatFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class ChatRenderMixin {
    @Inject(method = "renderChat", at = @At("HEAD"))
    private void doore$chatBg(DrawContext g, RenderTickCounter dt, CallbackInfo ci) {
        if (ChatFeature.glassBackground()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.currentScreen instanceof ChatScreen) {
                int i = g.getScaledWindowHeight();
                int j = Math.min(i / 2, 160);
                ChatFeature.paintBackground(g, g.getScaledWindowWidth(), i, j);
            }
        }
    }
}
