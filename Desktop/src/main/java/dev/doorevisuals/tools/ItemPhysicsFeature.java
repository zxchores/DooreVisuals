package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;

public final class ItemPhysicsFeature extends Feature {
    private static volatile boolean active;
    private static volatile float tiltDeg = 82.0F;
    private static volatile boolean freezeSpin = true;
    private static volatile boolean freezeBob = true;
    private final Opt.Num tilt = this.opt(new Opt.Num("tilt", "\u041d\u0430\u043a\u043b\u043e\u043d", 82.0, 35.0, 90.0, 1.0));
    private final Opt.Flag noSpin = this.opt(new Opt.Flag("freeze_spin", "\u0411\u0435\u0437 \u0432\u0440\u0430\u0449\u0435\u043d\u0438\u044f", true));
    private final Opt.Flag noBob = this.opt(
        new Opt.Flag("freeze_bob", "\u0411\u0435\u0437 \u043f\u043e\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u044f", true)
    );

    public ItemPhysicsFeature() {
        super(
            "item_physics",
            "Item Physics",
            "\u041f\u0440\u0435\u0434\u043c\u0435\u0442\u044b \u043d\u0430 \u0437\u0435\u043c\u043b\u0435 \u043b\u0435\u0436\u0430\u0442 \u043f\u043b\u043e\u0441\u043a\u043e \u2014 \u0431\u0435\u0437 \u043a\u0440\u0443\u0447\u0435\u043d\u0438\u044f",
            Category.WORLD,
            true
        );
    }

    private void sync() {
        active = this.on();
        tiltDeg = this.tilt.f();
        freezeSpin = (Boolean)this.noSpin.get();
        freezeBob = (Boolean)this.noBob.get();
    }

    @Override
    protected void enable() {
        this.sync();
    }

    @Override
    protected void disable() {
        active = false;
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    public static boolean active() {
        return active;
    }

    public static float tilt() {
        return tiltDeg;
    }

    public static boolean freezeSpin() {
        return active && freezeSpin;
    }

    public static boolean freezeBob() {
        return active && freezeBob;
    }
}
