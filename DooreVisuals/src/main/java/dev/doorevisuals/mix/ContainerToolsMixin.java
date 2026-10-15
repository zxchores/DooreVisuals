package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.InvToolsFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class ContainerToolsMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;

    @Shadow
    public abstract ScreenHandler getScreenHandler();

    @Inject(method = "render", at = @At("TAIL"))
    private void doore$overlay(DrawContext g, int mx, int my, float delta, CallbackInfo ci) {
        if (App.live()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            HandledScreen<?> handledscreen = (HandledScreen<?>)(Object)this;
            if (!(handledscreen instanceof CreativeInventoryScreen)) {
                Ui.frame(
                    g,
                    () -> {
                        if (this.doore$storage(handledscreen)) {
                            App.features().find(InvToolsFeature.class).filter(Feature::on).ifPresent(f -> {
                                float fx = this.x + this.backgroundWidth + 6.0F;
                                float f1 = this.y + 8.0F;
                                doore$btn(g, mx, my, fx, f1, 64.0F, 16.0F, "выброс");
                                doore$btn(g, mx, my, fx, f1 + 20.0F, 64.0F, 16.0F, "сорт");
                                doore$btn(g, mx, my, fx, f1 + 40.0F, 64.0F, 16.0F, "сорт инв.");
                            });
                        } else if (handledscreen instanceof InventoryScreen) {
                            // The player's own inventory: one button, next to the inventory panel.
                            App.features().find(InvToolsFeature.class).filter(Feature::on).ifPresent(f ->
                                doore$btn(g, mx, my, this.x + this.backgroundWidth + 6.0F, this.y + 8.0F, 64.0F, 16.0F, "сорт"));
                        }
                    }
                );
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void doore$click(Click event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (App.live() && event.button() == 0) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            HandledScreen<?> handledscreen = (HandledScreen<?>)(Object)this;
            if (!(handledscreen instanceof CreativeInventoryScreen)) {
                double d0 = event.x();
                double d1 = event.y();
                if (this.doore$storage(handledscreen) && App.features().find(InvToolsFeature.class).filter(Feature::on).isPresent()) {
                    float f = this.x + this.backgroundWidth + 6.0F;
                    float f1 = this.y + 8.0F;
                    if (Paint.hit(d0, d1, f, f1, 64.0F, 16.0F)) {
                        InvToolsFeature.dump(minecraftclient);
                        cir.setReturnValue(true);
                        return;
                    }

                    if (Paint.hit(d0, d1, f, f1 + 20.0F, 64.0F, 16.0F)) {
                        InvToolsFeature.sort(minecraftclient, false);
                        cir.setReturnValue(true);
                        return;
                    }

                    if (Paint.hit(d0, d1, f, f1 + 40.0F, 64.0F, 16.0F)) {
                        InvToolsFeature.sort(minecraftclient, true);
                        cir.setReturnValue(true);
                        return;
                    }
                } else if (handledscreen instanceof InventoryScreen && App.features().find(InvToolsFeature.class).filter(Feature::on).isPresent()
                        && Paint.hit(d0, d1, this.x + this.backgroundWidth + 6.0F, this.y + 8.0F, 64.0F, 16.0F)) {
                    InvToolsFeature.sort(minecraftclient, true);
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Unique
    private boolean doore$storage(HandledScreen<?> self) {
        return !(self instanceof InventoryScreen) && !(this.getScreenHandler() instanceof PlayerScreenHandler)
            ? this.getScreenHandler().slots.size() >= 54
            : false;
    }

    @Unique
    private static void doore$btn(DrawContext g, int mx, int my, float x, float y, float w, float h, String label) {
        boolean flag = Paint.hit((double)mx, (double)my, x, y, w, h);
        Paint.box(g, x, y, w, h, Theme.alpha(flag ? Theme.ACCENT : 329483, flag ? 70 : 200), 5.0F);
        Paint.textC(g, label, x + w * 0.5F, y + 4.0F, flag ? Theme.TEXT : Theme.MUTED, 6.2F);
    }
}
