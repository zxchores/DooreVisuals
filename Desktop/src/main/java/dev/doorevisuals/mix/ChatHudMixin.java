package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.overlay.HudSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatHudMixin {
    @Unique
    private HudSlot doore$drag;
    @Unique
    private float doore$ox;
    @Unique
    private float doore$oy;

    @Inject(method = "render", at = @At("TAIL"))
    private void doore$render(DrawContext g, int mx, int my, float delta, CallbackInfo ci) {
        if (App.live()) {
            HudFeature hudfeature = App.features().find(HudFeature.class).filter(Feature::on).orElse(null);
            if (hudfeature != null) {
                if (this.doore$drag != null) {
                    if (!doore$left()) {
                        this.doore$drag = null;
                        App.save().flush();
                    } else {
                        MinecraftClient minecraftclient = MinecraftClient.getInstance();
                        float[] afloat = hudfeature.editorRect(this.doore$drag);
                        float f = minecraftclient.getWindow().getScaledWidth() - afloat[2];
                        float f1 = minecraftclient.getWindow().getScaledHeight() - afloat[3];
                        float f2 = doore$snap(mx - this.doore$ox, afloat[2], minecraftclient.getWindow().getScaledWidth());
                        float f3 = doore$snap(my - this.doore$oy, afloat[3], minecraftclient.getWindow().getScaledHeight());
                        hudfeature.moveSlotScreen(this.doore$drag, Math.max(0.0F, Math.min(f, f2)), Math.max(8.0F, Math.min(f1, f3)));
                        hudfeature.poke();
                    }
                }

                Nvg.run(
                    g,
                    () -> {
                        float f4 = hudfeature.editorChromeAlpha();
                        if (!(f4 < 0.02F)) {
                            Nvg.alpha(f4);

                            for (HudSlot hudslot : hudfeature.activeSlots()) {
                                float[] afloat1 = hudfeature.editorRect(hudslot);
                                int i = hudslot.locked() ? Theme.WARN : (hudslot == this.doore$drag ? Theme.OK : Theme.ACCENT);
                                Paint.outline(
                                    g, afloat1[0] - 2.0F, afloat1[1] - 2.0F, afloat1[2] + 4.0F, afloat1[3] + 4.0F, Theme.alpha(i, (int)(200.0F * f4)), 4.0F
                                );
                                String s = hudslot.title() + (hudslot.locked() ? " \u00b7 lock" : "");
                                Paint.text(g, s, afloat1[0], afloat1[1] - 10.0F, Theme.alpha(Theme.TEXT, (int)(255.0F * f4)), 8.0F);
                            }

                            Paint.textC(
                                g,
                                "\u043f\u0435\u0440\u0435\u0442\u0430\u0449\u0438 \u00b7 \u0441\u0435\u0442\u043a\u0430 8px \u00b7 \u041f\u041a\u041c lock \u00b7 Shift+\u041f\u041a\u041c \u0441\u0431\u0440\u043e\u0441 \u00b7 \u043a\u043e\u043b\u0451\u0441\u0438\u043a\u043e \u043c\u0430\u0441\u0448\u0442\u0430\u0431",
                                g.getScaledWindowWidth() / 2.0F,
                                4.0F,
                                Theme.alpha(Theme.MUTED, (int)(255.0F * f4)),
                                8.0F
                            );
                            Nvg.alpha(1.0F);
                        }
                    }
                );
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void doore$click(Click event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (App.live()) {
            HudFeature hudfeature = App.features().find(HudFeature.class).filter(Feature::on).orElse(null);
            if (hudfeature != null) {
                if (event.button() != 1) {
                    if (event.button() == 0) {
                        if (hudfeature.tryIslandClick(event.x(), event.y())) {
                            cir.setReturnValue(true);
                        } else {
                            List<HudSlot> list = new ArrayList<>(hudfeature.activeSlots());

                            for (int j = list.size() - 1; j >= 0; j--) {
                                HudSlot hudslot1 = list.get(j);
                                if (!hudslot1.locked() && hudfeature.hitSlot(hudslot1, event.x(), event.y())) {
                                    float[] afloat = hudfeature.editorRect(hudslot1);
                                    this.doore$drag = hudslot1;
                                    this.doore$ox = (float)event.x() - afloat[0];
                                    this.doore$oy = (float)event.y() - afloat[1];
                                    cir.setReturnValue(true);
                                    return;
                                }
                            }
                        }
                    }
                } else {
                    long i = MinecraftClient.getInstance().getWindow().getHandle();
                    boolean flag = GLFW.glfwGetKey(i, 340) == 1 || GLFW.glfwGetKey(i, 344) == 1;

                    for (HudSlot hudslot : hudfeature.activeSlots()) {
                        if (hudfeature.hitSlot(hudslot, event.x(), event.y())) {
                            if (flag) {
                                hudfeature.resetSlot(hudslot);
                            } else {
                                hudslot.toggleLock();
                            }

                            App.save().flush();
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void doore$scroll(double mx, double my, double h, double v, CallbackInfoReturnable<Boolean> cir) {
        if (App.live()) {
            HudFeature hudfeature = App.features().find(HudFeature.class).filter(Feature::on).orElse(null);
            if (hudfeature != null) {
                if (hudfeature.tryIslandScroll(mx, my, v)) {
                    cir.setReturnValue(true);
                } else {
                    for (HudSlot hudslot : hudfeature.activeSlots()) {
                        if (hudfeature.hitSlot(hudslot, mx, my)) {
                            hudslot.setScale(hudslot.scale() + (float)v * 0.06F);
                            hudfeature.poke();
                            App.save().flush();
                            cir.setReturnValue(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @Unique
    private static boolean doore$left() {
        long i = MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetMouseButton(i, 0) == 1;
    }

    @Unique
    private static float doore$snap(float value, float size, float max) {
        float f = Math.round(value / 8.0F) * 8.0F;
        if (Math.abs(f) < 8.0F) {
            return 0.0F;
        } else if (Math.abs(f - (max - size) / 2.0F) < 8.0F) {
            return (max - size) / 2.0F;
        } else {
            return Math.abs(f - (max - size)) < 8.0F ? max - size : f;
        }
    }
}
