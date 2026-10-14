package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;

public final class ViewmodelFeature extends Feature implements Tick {
    private static volatile boolean active;
    private static volatile float mainX;
    private static volatile float mainY;
    private static volatile float mainZ;
    private static volatile float mainS = 1.0F;
    private static volatile float offX;
    private static volatile float offY;
    private static volatile float offZ;
    private static volatile float offS = 1.0F;
    private final Opt.Num mx = this.opt(new Opt.Num("main_x", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f X", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num my = this.opt(new Opt.Num("main_y", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f Y", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num mz = this.opt(new Opt.Num("main_z", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f Z", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num ms = this.opt(
        new Opt.Num("main_scale", "\u041e\u0441\u043d\u043e\u0432\u043d\u0430\u044f \u0448\u043a\u0430\u043b\u0430", 1.0, 0.4, 2.0, 0.05)
    );
    private final Opt.Num ox = this.opt(new Opt.Num("off_x", "\u0412\u0442\u043e\u0440\u0430\u044f X", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num oy = this.opt(new Opt.Num("off_y", "\u0412\u0442\u043e\u0440\u0430\u044f Y", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num oz = this.opt(new Opt.Num("off_z", "\u0412\u0442\u043e\u0440\u0430\u044f Z", 0.0, -1.5, 1.5, 0.05));
    private final Opt.Num os = this.opt(new Opt.Num("off_scale", "\u0412\u0442\u043e\u0440\u0430\u044f \u0448\u043a\u0430\u043b\u0430", 1.0, 0.4, 2.0, 0.05));

    public ViewmodelFeature() {
        super(
            "viewmodel",
            "Viewmodel",
            "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430 \u043f\u043e\u0437\u0438\u0446\u0438\u0438 \u0440\u0443\u043a \u043e\u0442 \u043f\u0435\u0440\u0432\u043e\u0433\u043e \u043b\u0438\u0446\u0430",
            Category.TOOLS,
            true
        );
    }

    public static boolean active() {
        return active;
    }

    public static float mainX() {
        return mainX;
    }

    public static float mainY() {
        return mainY;
    }

    public static float mainZ() {
        return mainZ;
    }

    public static float mainS() {
        return mainS;
    }

    public static float offX() {
        return offX;
    }

    public static float offY() {
        return offY;
    }

    public static float offZ() {
        return offZ;
    }

    public static float offS() {
        return offS;
    }

    @Override
    protected void enable() {
        active = true;
        this.sync();
    }

    @Override
    protected void disable() {
        active = false;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            this.sync();
        }
    }

    private void sync() {
        mainX = this.mx.f();
        mainY = this.my.f();
        mainZ = this.mz.f();
        mainS = this.ms.f();
        offX = this.ox.f();
        offY = this.oy.f();
        offZ = this.oz.f();
        offS = this.os.f();
    }
}
