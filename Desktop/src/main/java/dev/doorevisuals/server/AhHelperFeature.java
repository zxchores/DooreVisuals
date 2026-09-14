package dev.doorevisuals.server;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.AuctionLore;
import dev.doorevisuals.tools.InvClicks;
import dev.doorevisuals.tools.ServerDetect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

public final class AhHelperFeature extends Feature implements Tick {
    private final Opt.Num maxEach = this.opt(new Opt.Num("max_each", "Макс. цена/шт", 0.0, 0.0, 1.0E7, 100.0));
    private final Opt.Num clickDelay = this.opt(new Opt.Num("click_delay", "Задержка клика", 3.0, 1.0, 12.0, 1.0));
    private final Opt.Key search = this.opt(new Opt.Key("search", "Искать предмет в руке", -1));
    private final Opt.Key relist = this.opt(new Opt.Key("relist", "Перевыставить", -1));
    private final Opt.Key sell = this.opt(new Opt.Key("autosell", "Автоселл", -1));
    private boolean searchWas;
    private boolean relistWas;
    private boolean sellWas;
    private AhHelperFeature.Relist job;

    public AhHelperFeature() {
        super("ah_helper", "AhHelper", "Подсветка AH, поиск из руки и перевыставление лотов", Category.SYSTEM, false);
    }

    public static AhHelperFeature live() {
        return App.features().find(AhHelperFeature.class).orElse(null);
    }

    public double maxEach() {
        return (Double)this.maxEach.get();
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            InvClicks.setDelay(this.clickDelay.i());
            this.poll(mc, this.search, this.searchWas, v -> this.searchWas = v, () -> searchHeld(mc));
            this.poll(mc, this.relist, this.relistWas, v -> this.relistWas = v, () -> this.startRelist(mc));
            this.poll(mc, this.sell, this.sellWas, v -> this.sellWas = v, () -> cheapest(mc, this.maxEach()));
            this.stepRelist(mc);
        }
    }

    public static void searchHeld(MinecraftClient mc) {
        if (mc.player != null) {
            String s = AuctionLore.displayName(mc.player.getMainHandStack());
            if (s.isBlank()) {
                s = AuctionLore.displayName(mc.player.getOffHandStack());
            }

            if (!s.isBlank()) {
                ServerDetect.command(mc, "ah search " + s);
                toast("AH", "поиск  " + s);
            }
        }
    }

    public static void cheapest(MinecraftClient mc, double cap) {
        if (mc.player != null && ServerDetect.auction(mc)) {
            ScreenHandler screenhandler = mc.player.currentScreenHandler;
            double d0 = Double.MAX_VALUE;
            int i = -1;

            for (Slot slot : screenhandler.slots) {
                AuctionLore.Deal auctionlore$deal = AuctionLore.parse(slot.getStack());
                if (auctionlore$deal != null && (!(cap > 0.0) || !(auctionlore$deal.each() > cap)) && auctionlore$deal.each() < d0) {
                    d0 = auctionlore$deal.each();
                    i = slot.id;
                }
            }

            if (i >= 0) {
                InvClicks.pickup(screenhandler.syncId, i);
            }
        }
    }

    public void paint(DrawContext g, HandledScreen<?> screen, int mx, int my, int gx, int gy, int bgw) {
        if (this.on() && ServerDetect.auction(MinecraftClient.getInstance())) {
            double d0 = this.maxEach();
            double d1 = Double.MAX_VALUE;
            Slot slot = null;

            for (Slot slot1 : screen.getScreenHandler().slots) {
                AuctionLore.Deal auctionlore$deal = AuctionLore.parse(slot1.getStack());
                if (auctionlore$deal != null && auctionlore$deal.each() < d1) {
                    d1 = auctionlore$deal.each();
                    slot = slot1;
                }
            }

            for (Slot slot2 : screen.getScreenHandler().slots) {
                AuctionLore.Deal auctionlore$deal1 = AuctionLore.parse(slot2.getStack());
                if (auctionlore$deal1 != null) {
                    float f = gx + slot2.x;
                    float f1 = gy + slot2.y;
                    boolean flag = slot2 == slot;
                    boolean flag1 = d0 > 0.0 && auctionlore$deal1.each() > d0;
                    int i = flag1 ? Theme.alpha(14830411, 50) : Theme.alpha(flag ? Theme.ACCENT : 0, flag ? 70 : 35);
                    Paint.box(g, f, f1, 16.0F, 16.0F, i, 2.0F);
                    Paint.text(g, trimPrice(auctionlore$deal1.each()), f, f1 - 6.0F, flag ? Theme.ACCENT_HOT : Theme.TEXT, 5.2F);
                }
            }

            float f2 = gx + bgw + 6.0F;
            float f3 = gy + 52.0F;
            btn(g, mx, my, f2, f3, 64.0F, 16.0F, "селл");
            btn(g, mx, my, f2, f3 + 20.0F, 64.0F, 16.0F, "поиск");
            btn(g, mx, my, f2, f3 + 40.0F, 64.0F, 16.0F, "релист");
        }
    }

    public boolean click(double mx, double my, int gx, int gy, int bgw) {
        if (this.on() && ServerDetect.auction(MinecraftClient.getInstance())) {
            float f = gx + bgw + 6.0F;
            float f1 = gy + 52.0F;
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (Paint.hit(mx, my, f, f1, 64.0F, 16.0F)) {
                cheapest(minecraftclient, this.maxEach());
                return true;
            } else if (Paint.hit(mx, my, f, f1 + 20.0F, 64.0F, 16.0F)) {
                searchHeld(minecraftclient);
                return true;
            } else if (Paint.hit(mx, my, f, f1 + 40.0F, 64.0F, 16.0F)) {
                this.startRelist(minecraftclient);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void startRelist(MinecraftClient mc) {
        if (mc.player != null && !InvClicks.busy()) {
            ServerDetect.Kind serverdetect$kind = ServerDetect.window(mc);
            if (serverdetect$kind == ServerDetect.Kind.OWN_LOTS || serverdetect$kind == ServerDetect.Kind.AUCTION) {
                ScreenHandler screenhandler = mc.player.currentScreenHandler;
                Slot slot = null;
                AuctionLore.Deal auctionlore$deal = null;

                for (Slot slot1 : screenhandler.slots) {
                    AuctionLore.Deal auctionlore$deal1 = AuctionLore.parse(slot1.getStack());
                    if (auctionlore$deal1 != null && !slot1.getStack().isEmpty()) {
                        slot = slot1;
                        auctionlore$deal = auctionlore$deal1;
                        break;
                    }
                }

                if (slot != null) {
                    this.job = new AhHelperFeature.Relist(AuctionLore.displayName(slot.getStack()), auctionlore$deal.total(), 0, System.currentTimeMillis());
                    InvClicks.pickup(screenhandler.syncId, slot.id);
                    toast("AH", "снимаю  " + this.job.name);
                }
            }
        }
    }

    private void stepRelist(MinecraftClient mc) {
        if (this.job != null && mc.player != null) {
            if (System.currentTimeMillis() - this.job.at > 12000L) {
                this.job = null;
            } else if (!InvClicks.busy()) {
                ServerDetect.Kind serverdetect$kind = ServerDetect.window(mc);
                ScreenHandler screenhandler = mc.player.currentScreenHandler;
                if (serverdetect$kind == ServerDetect.Kind.CONFIRM) {
                    int i = ServerDetect.findSlot(screenhandler, "подтверд", "confirm", "да", "yes", "снять", "забрать");
                    if (i >= 0) {
                        InvClicks.pickup(screenhandler.syncId, i);
                        this.job = new AhHelperFeature.Relist(this.job.name, this.job.price, 1, this.job.at);
                    }
                } else if (this.job.stage >= 1) {
                    if (serverdetect$kind == ServerDetect.Kind.NONE) {
                        ServerDetect.command(mc, "ah sell " + this.job.price);
                        toast("AH", "выставляю  " + this.job.price);
                        this.job = null;
                    } else {
                        int j = ServerDetect.findSlot(screenhandler, "выставить", "продать", "sell", "выставление");
                        if (j >= 0) {
                            InvClicks.pickup(screenhandler.syncId, j);
                            this.job = null;
                        }
                    }
                }
            }
        }
    }

    private void poll(MinecraftClient mc, Opt.Key key, boolean was, java.util.function.Consumer<Boolean> setWas, Runnable run) {
        int i = (Integer)key.get();
        if (i > 0 && i != -1 && mc.getWindow() != null) {
            boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
            if (flag && !was) {
                run.run();
            }

            setWas.accept(flag);
        } else {
            setWas.accept(false);
        }
    }

    static void btn(DrawContext g, int mx, int my, float x, float y, float w, float h, String label) {
        boolean flag = Paint.hit((double)mx, (double)my, x, y, w, h);
        Paint.box(g, x, y, w, h, Theme.alpha(flag ? Theme.ACCENT : 329483, flag ? 70 : 200), 5.0F);
        Paint.textC(g, label, x + w * 0.5F, y + 4.0F, flag ? Theme.TEXT : Theme.MUTED, 6.2F);
    }

    static String trimPrice(double each) {
        if (each >= 1000000.0) {
            return String.format("%.1fм", each / 1000000.0);
        } else {
            return each >= 1000.0 ? String.format("%.1fк", each / 1000.0) : String.valueOf(Math.round(each));
        }
    }

    private static void toast(String title, String body) {
        App.features().find(HudFeature.class).ifPresent(h -> h.notify(title, body, HudFeature.NoteKind.OK));
    }

    private record Relist(String name, long price, int stage, long at) {
    }
}
