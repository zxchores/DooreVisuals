package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.AuctionLore;
import dev.doorevisuals.tools.FtHelperFeature;
import dev.doorevisuals.tools.HwHelperFeature;
import dev.doorevisuals.tools.InvToolsFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
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
                Nvg.run(g, () -> {
                    if (this.doore$storage(handledscreen)) {
                        App.features().find(InvToolsFeature.class).filter(Feature::on).ifPresent(f -> {
                            float fx = this.x + this.backgroundWidth + 6.0F;
                            float f1 = this.y + 8.0F;
                            doore$btn(g, mx, my, fx, f1, 64.0F, 16.0F, "\u0432\u044b\u0431\u0440\u043e\u0441");
                            doore$btn(g, mx, my, fx, f1 + 20.0F, 64.0F, 16.0F, "\u0441\u043e\u0440\u0442");
                        });
                    }

                    if (this.doore$ah(minecraftclient)) {
                        this.doore$auction(g, mx, my);
                    }
                });
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
                        InvToolsFeature.sort(minecraftclient);
                        cir.setReturnValue(true);
                        return;
                    }
                }

                if (this.doore$ah(minecraftclient)) {
                    float f2 = this.x + this.backgroundWidth + 6.0F;
                    float f3 = this.y + 52.0F;
                    if (Paint.hit(d0, d1, f2, f3, 64.0F, 16.0F)) {
                        double d2 = this.doore$cap();
                        FtHelperFeature.autosell(minecraftclient, d2);
                        cir.setReturnValue(true);
                    }
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
    private boolean doore$ah(MinecraftClient mc) {
        if (!AuctionLore.auctionScreen(mc)) {
            return false;
        } else {
            boolean flag = App.features().find(FtHelperFeature.class).filter(Feature::on).isPresent();
            boolean flag1 = App.features().find(HwHelperFeature.class).filter(Feature::on).isPresent();
            return flag || flag1;
        }
    }

    @Unique
    private double doore$cap() {
        double d0 = App.features().find(FtHelperFeature.class).filter(Feature::on).map(FtHelperFeature::maxEach).orElse(0.0);
        if (d0 <= 0.0) {
            d0 = App.features().find(HwHelperFeature.class).filter(Feature::on).map(HwHelperFeature::maxEach).orElse(0.0);
        }

        return d0;
    }

    @Unique
    private void doore$auction(DrawContext g, int mx, int my) {
        double d0 = this.doore$cap();
        double d1 = Double.MAX_VALUE;
        Slot slot = null;

        for (Slot slot1 : this.getScreenHandler().slots) {
            AuctionLore.Deal auctionlore$deal = AuctionLore.parse(slot1.getStack());
            if (auctionlore$deal != null && auctionlore$deal.each() < d1) {
                d1 = auctionlore$deal.each();
                slot = slot1;
            }
        }

        for (Slot slot2 : this.getScreenHandler().slots) {
            AuctionLore.Deal auctionlore$deal1 = AuctionLore.parse(slot2.getStack());
            if (auctionlore$deal1 != null) {
                float f = this.x + slot2.x;
                float f1 = this.y + slot2.y;
                boolean flag = slot2 == slot;
                boolean flag1 = d0 > 0.0 && auctionlore$deal1.each() > d0;
                int i = flag1 ? Theme.alpha(14830411, 50) : Theme.alpha(flag ? Theme.ACCENT : 0, flag ? 70 : 35);
                Paint.box(g, f, f1, 16.0F, 16.0F, i, 2.0F);
                String s = trimPrice(auctionlore$deal1.each());
                Paint.text(g, s, f, f1 - 6.0F, flag ? Theme.ACCENT_HOT : Theme.TEXT, 5.2F);
            }
        }

        float f2 = this.x + this.backgroundWidth + 6.0F;
        float f3 = this.y + 52.0F;
        doore$btn(g, mx, my, f2, f3, 64.0F, 16.0F, "\u0441\u0435\u043b\u043b");
    }

    @Unique
    private static String trimPrice(double each) {
        if (each >= 1000000.0) {
            return String.format("%.1f\u043c", each / 1000000.0);
        } else {
            return each >= 1000.0 ? String.format("%.1f\u043a", each / 1000.0) : String.valueOf(Math.round(each));
        }
    }

    @Unique
    private static void doore$btn(DrawContext g, int mx, int my, float x, float y, float w, float h, String label) {
        boolean flag = Paint.hit((double)mx, (double)my, x, y, w, h);
        Paint.box(g, x, y, w, h, Theme.alpha(flag ? Theme.ACCENT : 329483, flag ? 70 : 200), 5.0F);
        Paint.textC(g, label, x + w * 0.5F, y + 4.0F, flag ? Theme.TEXT : Theme.MUTED, 6.2F);
    }
}
