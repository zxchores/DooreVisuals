package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.draw.Ui;
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
    /** Guide lines of the snap that is active while dragging; negative = none. */
    @Unique
    private float doore$guideX = -1.0F;
    @Unique
    private float doore$guideY = -1.0F;

    @Inject(method = "render", at = @At("TAIL"))
    private void doore$render(DrawContext g, int mx, int my, float delta, CallbackInfo ci) {
        if (App.live()) {
            HudFeature hudfeature = App.features().find(HudFeature.class).filter(Feature::on).orElse(null);
            if (hudfeature != null) {
                if (this.doore$drag != null) {
                    if (!doore$left()) {
                        this.doore$drag = null;
                        this.doore$guideX = -1.0F;
                        this.doore$guideY = -1.0F;
                        App.save().flush();
                    } else {
                        MinecraftClient minecraftclient = MinecraftClient.getInstance();
                        float[] afloat = hudfeature.editorRect(this.doore$drag);
                        float f = minecraftclient.getWindow().getScaledWidth() - afloat[2];
                        float f1 = minecraftclient.getWindow().getScaledHeight() - afloat[3];
                        List<float[]> marksX = new ArrayList<>();
                        List<float[]> marksY = new ArrayList<>();
                        for (HudSlot other : hudfeature.activeSlots()) {
                            if (other == this.doore$drag) {
                                continue;
                            }
                            float[] o = hudfeature.editorRect(other);
                            float gap = 4.0F;
                            // beside the other widget with a small gap, or flush with its edges
                            marksX.add(new float[] {o[0] - afloat[2] - gap, o[0] - gap});
                            marksX.add(new float[] {o[0] + o[2] + gap, o[0] + o[2] + gap});
                            marksX.add(new float[] {o[0], o[0]});
                            marksX.add(new float[] {o[0] + o[2] - afloat[2], o[0] + o[2]});
                            marksY.add(new float[] {o[1] - afloat[3] - gap, o[1] - gap});
                            marksY.add(new float[] {o[1] + o[3] + gap, o[1] + o[3] + gap});
                            marksY.add(new float[] {o[1], o[1]});
                            marksY.add(new float[] {o[1] + o[3] - afloat[3], o[1] + o[3]});
                        }
                        float[] snapX = doore$snapAxis(mx - this.doore$ox, afloat[2], minecraftclient.getWindow().getScaledWidth(), 0.0F, marksX);
                        float[] snapY = doore$snapAxis(my - this.doore$oy, afloat[3], minecraftclient.getWindow().getScaledHeight(), 8.0F, marksY);
                        this.doore$guideX = snapX[1];
                        this.doore$guideY = snapY[1];
                        float f2 = snapX[0];
                        float f3 = snapY[0];
                        hudfeature.moveSlotScreen(this.doore$drag, Math.max(0.0F, Math.min(f, f2)), Math.max(8.0F, Math.min(f1, f3)));
                        hudfeature.poke();
                    }
                }

                Ui.frame(
                        g,
                        () -> {
                            float f4 = hudfeature.editorChromeAlpha();
                            if (!(f4 < 0.02F)) {
                                Ui.alpha(f4);

                                int gw = g.getScaledWindowWidth();
                                int gh = g.getScaledWindowHeight();
                                // Snap guides: the line the dragged widget is currently stuck to.
                                int guideCol = Theme.alpha(Theme.ACCENT_HOT, (int)(190.0F * f4));
                                if (this.doore$guideX >= 0.0F) {
                                    Paint.box(g, this.doore$guideX, 0.0F, 1.0F, gh, guideCol, 0.0F);
                                }
                                if (this.doore$guideY >= 0.0F) {
                                    Paint.box(g, 0.0F, this.doore$guideY, gw, 1.0F, guideCol, 0.0F);
                                }

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
                                        "перетащи · прилипание к краям и виджетам · ПКМ lock · Shift+ПКМ сброс · колёсико масштаб",
                                        g.getScaledWindowWidth() / 2.0F,
                                        4.0F,
                                        Theme.alpha(Theme.MUTED, (int)(255.0F * f4)),
                                        8.0F
                                );
                                Ui.alpha(1.0F);
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

    /**
     * Snaps one axis of a dragged widget. Candidates: the screen edges and centre and the edges of the other widgets.
     * Returns {snapped position, guide line or -1}.
     */
    @Unique
    private static float[] doore$snapAxis(float value, float size, float max, float min, List<float[]> marks) {
        List<float[]> candidates = new ArrayList<>();
        candidates.add(new float[] {min, min});
        candidates.add(new float[] {max - size, max});
        candidates.add(new float[] {(max - size) / 2.0F, max / 2.0F});
        candidates.addAll(marks);
        float best = value;
        float guide = -1.0F;
        float bestDistance = 6.0F;
        for (float[] c : candidates) {
            float d = Math.abs(c[0] - value);
            if (d < bestDistance) {
                bestDistance = d;
                best = c[0];
                guide = c[1];
            }
        }
        return new float[] {best, guide};
    }
}
