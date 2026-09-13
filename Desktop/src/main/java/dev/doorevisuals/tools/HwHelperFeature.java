package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;

public final class HwHelperFeature extends Feature implements Tick {
    static final String[] HOSTS = new String[]{"hollyworld", "spacetime"};
    private final Opt.Num maxEach = this.opt(new Opt.Num("max_each", "\u041c\u0430\u043a\u0441. \u0446\u0435\u043d\u0430/\u0448\u0442", 0.0, 0.0, 1.0E7, 100.0));
    private final Opt.Key sell = this.opt(new Opt.Key("autosell", "\u0410\u0432\u0442\u043e\u0441\u0435\u043b\u043b", -1));
    private boolean sellWas;
    private boolean armed;

    public HwHelperFeature() {
        super(
            "hw_helper",
            "HW Helper",
            "\u0410\u0443\u043a\u0446\u0438\u043e\u043d HollyWorld: \u0446\u0435\u043d\u0430/\u0448\u0442 \u0438 \u0430\u0432\u0442\u043e\u0441\u0435\u043b\u043b",
            Category.SYSTEM,
            false
        );
    }

    @Override
    public void tick(MinecraftClient mc) {
        boolean flag = AuctionLore.serverMatch(mc, HOSTS);
        if (flag && !this.armed) {
            this.set(true);
            this.armed = true;
        }

        if (!flag) {
            this.armed = false;
        }

        if (this.on()) {
            FtHelperFeature.pollSell(mc, this.sell, this.sellWas, v -> this.sellWas = v, (Double)this.maxEach.get());
        }
    }

    public double maxEach() {
        return (Double)this.maxEach.get();
    }
}
