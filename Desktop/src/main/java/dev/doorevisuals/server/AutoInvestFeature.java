package dev.doorevisuals.server;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.AuctionLore;
import dev.doorevisuals.tools.InvClicks;
import dev.doorevisuals.tools.ServerDetect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class AutoInvestFeature extends Feature implements Tick {
    private final Opt.Num clickDelay = this.opt(new Opt.Num("click_delay", "Задержка клика", 4.0, 1.0, 16.0, 1.0));
    private final Opt.Num cap = this.opt(new Opt.Num("cap", "Стоп при сумме", 0.0, 0.0, 1.0E9, 1000.0));
    private final Opt.Flag run = this.opt(new Opt.Flag("run", "Вносить в казну", true));
    private boolean armed;

    public AutoInvestFeature() {
        super("auto_invest", "AutoInvest", "Автоматически вносит валюту в GUI казны клана", Category.SYSTEM, false);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            InvClicks.setDelay(Math.max(InvClicks.delay(), this.clickDelay.i()));
            if (ServerDetect.window(mc) != ServerDetect.Kind.TREASURY) {
                this.armed = false;
            } else if ((Boolean)this.run.get() && mc.player != null && !InvClicks.busy()) {
                this.step(mc);
            }
        }
    }

    public void paint(DrawContext g, HandledScreen<?> screen, int mx, int my, int gx, int gy, int bgw) {
        if (this.on() && ServerDetect.window(MinecraftClient.getInstance()) == ServerDetect.Kind.TREASURY) {
            AhHelperFeature.btn(g, mx, my, gx + bgw + 6.0F, gy + 8.0F, 64.0F, 16.0F, this.armed ? "стоп" : "внести");
        }
    }

    public boolean click(double mx, double my, int gx, int gy, int bgw) {
        if (this.on() && ServerDetect.window(MinecraftClient.getInstance()) == ServerDetect.Kind.TREASURY) {
            if (Paint.hit(mx, my, gx + bgw + 6.0F, gy + 8.0F, 64.0F, 16.0F)) {
                this.armed = !this.armed;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void step(MinecraftClient mc) {
        if (!this.armed) {
            this.armed = true;
        }

        ScreenHandler screenhandler = mc.player.currentScreenHandler;
        if (this.overCap(screenhandler)) {
            this.armed = false;
            App.features().find(HudFeature.class).ifPresent(h -> h.notify("Казна", "кап", HudFeature.NoteKind.WARN));
        } else {
            int i = ServerDetect.findSlot(screenhandler, "внести", "пополнить", "deposit", "вклад", "положить");
            int j = this.currency(screenhandler);
            if (j < 0) {
                this.armed = false;
            } else if (i >= 0) {
                InvClicks.pickup(screenhandler.syncId, j);
                InvClicks.pickup(screenhandler.syncId, i);
            } else {
                InvClicks.pickup(screenhandler.syncId, j);
            }
        }
    }

    private boolean overCap(ScreenHandler menu) {
        double d0 = (Double)this.cap.get();
        if (!(d0 > 0.0)) {
            return false;
        } else {
            for (Slot slot : menu.slots) {
                AuctionLore.Deal auctionlore$deal = AuctionLore.parse(slot.getStack());
                if (auctionlore$deal != null && AuctionLore.nameHas(slot.getStack(), "казна", "баланс", "treasury", "всего")) {
                    return auctionlore$deal.total() >= (long)d0;
                }
            }

            return false;
        }
    }

    private int currency(ScreenHandler menu) {
        int i = -1;

        for (Slot slot : menu.slots) {
            ItemStack itemstack = slot.getStack();
            if (!itemstack.isEmpty() && slot.id >= menu.slots.size() - 36 && this.money(itemstack)) {
                i = slot.id;
                break;
            }
        }

        return i;
    }

    private boolean money(ItemStack stack) {
        return stack.isOf(Items.EMERALD)
            || stack.isOf(Items.GOLD_INGOT)
            || stack.isOf(Items.GOLD_NUGGET)
            || stack.isOf(Items.DIAMOND)
            || AuctionLore.nameHas(stack, "монет", "валют", "coin", "money", "казна", "emerald");
    }
}
