package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.overlay.HotbarFeature;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.ContainerChrome;
import dev.doorevisuals.tools.CrosshairFeature;
import dev.doorevisuals.tools.NoRenderFeature;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class GuiFxMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void doore$resetChrome(DrawContext g, RenderTickCounter dt, CallbackInfo ci) {
        ContainerChrome.reset();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void doore$crosshair(DrawContext g, RenderTickCounter dt, CallbackInfo ci) {
        if (App.live()) {
            if (CrosshairFeature.hideVanilla()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void doore$effects(DrawContext g, RenderTickCounter dt, CallbackInfo ci) {
        if (App.live()) {
            App.features().find(HudFeature.class).ifPresent(hud -> {
                if (hud.on() && hud.hideVanillaEffects()) {
                    ci.cancel();
                }
            });
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void doore$hotbar(DrawContext g, RenderTickCounter dt, CallbackInfo ci) {
        if (App.live()) {
            App.features().find(HotbarFeature.class).ifPresent(hb -> {
                if (hb.hideVanillaBar()) {
                    ci.cancel();
                }
            });
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void doore$vignette(DrawContext g, Entity entity, CallbackInfo ci) {
        if (NoRenderFeature.vignette()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void doore$portal(DrawContext g, float alpha, CallbackInfo ci) {
        if (NoRenderFeature.portal()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void doore$nausea(DrawContext g, float alpha, CallbackInfo ci) {
        if (NoRenderFeature.nausea()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void doore$pumpkin(DrawContext g, Identifier tex, float alpha, CallbackInfo ci) {
        if (NoRenderFeature.pumpkin() && tex != null && tex.getPath().contains("pumpkin")) {
            ci.cancel();
        }
    }
}
