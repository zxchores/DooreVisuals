package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.data.GuiLayout;
import dev.doorevisuals.draw.Anim;

public final class GuiWindowsFeature extends Feature {
    private final Opt.Flag config = this.opt(new Opt.Flag("show_config", "\u041e\u043a\u043d\u043e \u041a\u043e\u043d\u0444\u0438\u0433", true));
    private final Opt.Flag dim = this.opt(new Opt.Flag("dim", "\u0417\u0430\u0442\u0435\u043c\u043d\u0435\u043d\u0438\u0435", true));
    private final Opt.Num dimStrength = this.opt(
        new Opt.Num("dim_strength", "\u0421\u0438\u043b\u0430 \u0437\u0430\u0442\u0435\u043c\u043d\u0435\u043d\u0438\u044f", 140.0, 40.0, 180.0, 10.0)
            .visibleWhen(this.dim::get)
    );
    private final Opt.Flag guiSound = this.opt(new Opt.Flag("gui_sound", "\u0417\u0432\u0443\u043a GUI", true));
    private final Opt.Pick motion = this.opt(
        new Opt.Pick(
            "motion",
            "\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u0438",
            "\u041e\u0431\u044b\u0447\u043d\u044b\u0435",
            "\u041e\u0431\u044b\u0447\u043d\u044b\u0435",
            "\u0420\u0435\u0437\u043a\u0438\u0435",
            "\u041c\u044f\u0433\u043a\u0438\u0435"
        )
    );

    public GuiWindowsFeature() {
        super(
            "gui_windows",
            "GUI Windows",
            "\u041a\u043e\u043d\u0444\u0438\u0433 \u0432 \u0434\u043e\u043a\u0435 \u00b7 \u0437\u0432\u0443\u043a \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u044f \u00b7 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438",
            Category.SYSTEM,
            true
        );
        this.sync();
    }

    public static boolean windowsOn() {
        return App.features().find(GuiWindowsFeature.class).map(Feature::on).orElse(false);
    }

    public static boolean showConfig() {
        return windowsOn() && GuiLayout.showConfig;
    }

    public static boolean soundOn() {
        return App.features().find(GuiWindowsFeature.class).map(f -> f.on() && (Boolean)f.guiSound.get()).orElse(true);
    }

    public static boolean dimOn() {
        return dimAlpha() > 0;
    }

    public static int dimAlpha() {
        return App.features().find(GuiWindowsFeature.class).map(f -> f.on() && f.dim.get() ? f.dimStrength.i() : 0).orElse(140);
    }

    public static int dimStrength() {
        return App.features().find(GuiWindowsFeature.class).map(f -> f.dimStrength.i()).orElse(140);
    }

    public static void setDimStrength(int value) {
        App.features().find(GuiWindowsFeature.class).ifPresent(f -> {
            f.dimStrength.set((double)value);
            f.dim.set(value > 0);
            f.poke();
        });
    }

    public static String motionLabel() {
        return App.features().find(GuiWindowsFeature.class).map(f -> (String)f.motion.get()).orElse("\u041e\u0431\u044b\u0447\u043d\u044b\u0435");
    }

    public static void setMotionLabel(String label) {
        App.features().find(GuiWindowsFeature.class).ifPresent(f -> {
            f.motion.set(label);
            f.poke();
        });
    }

    public static void toggleDim() {
        App.features().find(GuiWindowsFeature.class).ifPresent(f -> f.dim.flip());
    }

    public static void toggleSound() {
        App.features().find(GuiWindowsFeature.class).ifPresent(f -> f.guiSound.flip());
    }

    public static void setShowConfig(boolean v) {
        GuiLayout.showConfig = v;
        App.features().find(GuiWindowsFeature.class).ifPresent(f -> {
            f.config.set(v);
            f.sync();
        });
    }

    @Override
    protected void enable() {
        this.sync();
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    private void sync() {
        GuiLayout.showConfig = (Boolean)this.config.get();
        String s = (String)this.motion.get();

        Anim.setMotion(switch (s) {
            case "\u041c\u044f\u0433\u043a\u0438\u0435" -> Anim.Motion.SOFT;
            case "\u0420\u0435\u0437\u043a\u0438\u0435" -> Anim.Motion.SNAPPY;
            default -> Anim.Motion.NORMAL;
        });
    }

    public void pullFromLayout() {
        this.config.set(GuiLayout.showConfig);
    }
}
