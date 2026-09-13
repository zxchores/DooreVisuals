package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class AspectFeature extends Feature {
    private final Opt.Pick preset = this.opt(
        new Opt.Pick("preset", "\u0424\u043e\u0440\u043c\u0430\u0442", "4:3", "Native", "4:3", "5:4", "16:9", "16:10", "21:9", "32:9", "Custom")
    );
    private final Opt.Num customW = this.opt(new Opt.Num("custom_w", "\u0428\u0438\u0440\u0438\u043d\u0430", 4.0, 1.0, 64.0, 1.0));
    private final Opt.Num customH = this.opt(new Opt.Num("custom_h", "\u0412\u044b\u0441\u043e\u0442\u0430", 3.0, 1.0, 64.0, 1.0));
    private final Opt.Flag bars = this.opt(new Opt.Flag("bars", "\u041f\u043e\u043b\u043e\u0441\u044b \u043f\u043e \u043a\u0440\u0430\u044f\u043c", false));
    private final Opt.Pick barStyle = this.opt(
        new Opt.Pick("bar_style", "\u0421\u0442\u0438\u043b\u044c \u043f\u043e\u043b\u043e\u0441", "Black", "Black", "Dim", "Theme")
    );

    public AspectFeature() {
        super(
            "aspect_ratio",
            "Aspect Ratio",
            "\u0420\u0430\u0441\u0442\u044f\u0433\u0438\u0432\u0430\u0435\u0442 \u043c\u0438\u0440. \u041f\u043e\u043b\u043e\u0441\u044b \u043f\u043e \u0436\u0435\u043b\u0430\u043d\u0438\u044e \u2014 HUD \u043a\u0430\u043a \u0435\u0441\u0442\u044c",
            Category.TOOLS,
            false
        );
    }

    public boolean nativePreset() {
        return "Native".equals(this.preset.get());
    }

    public double targetRatio() {
        if (this.nativePreset()) {
            return AspectRatio.nativeAspect();
        } else if ("Custom".equals(this.preset.get())) {
            return (Double)this.customW.get() / Math.max(0.01, (Double)this.customH.get());
        } else {
            String[] astring = ((String)this.preset.get()).split(":");
            return Double.parseDouble(astring[0]) / Double.parseDouble(astring[1]);
        }
    }

    public boolean bars() {
        return (Boolean)this.bars.get();
    }

    public String barStyle() {
        return (String)this.barStyle.get();
    }

    @Override
    protected void enable() {
        AspectRatio.sync(this);
    }

    @Override
    protected void disable() {
        AspectRatio.clear();
    }

    public void paintBars(DrawContext g, RenderTickCounter dt) {
        AspectRatio.sync(this);
        AspectRatio.Letterbox aspectratio$letterbox = AspectRatio.letterbox(g.getScaledWindowWidth(), g.getScaledWindowHeight());
        if (aspectratio$letterbox != null) {
            String s = (String)this.barStyle.get();

            int i = switch (s) {
                case "Dim" -> -535818730;
                case "Theme" -> -234355184;
                default -> -16777216;
            };
            if (aspectratio$letterbox.vertical()) {
                Paint.box(g, 0, 0, aspectratio$letterbox.x(), g.getScaledWindowHeight(), i, 0.0F);
                Paint.box(
                    g,
                    aspectratio$letterbox.x() + aspectratio$letterbox.w(),
                    0,
                    g.getScaledWindowWidth() - (aspectratio$letterbox.x() + aspectratio$letterbox.w()),
                    g.getScaledWindowHeight(),
                    i,
                    0.0F
                );
                if ("Theme".equals(this.barStyle.get())) {
                    Paint.box(g, aspectratio$letterbox.x() - 1, 0, 2, g.getScaledWindowHeight(), Theme.alpha(Theme.ACCENT, 160), 0.0F);
                    Paint.box(g, aspectratio$letterbox.x() + aspectratio$letterbox.w() - 1, 0, 2, g.getScaledWindowHeight(), Theme.alpha(Theme.ACCENT, 160), 0.0F);
                }
            } else {
                Paint.box(g, 0, 0, g.getScaledWindowWidth(), aspectratio$letterbox.y(), i, 0.0F);
                Paint.box(
                    g,
                    0,
                    aspectratio$letterbox.y() + aspectratio$letterbox.h(),
                    g.getScaledWindowWidth(),
                    g.getScaledWindowHeight() - (aspectratio$letterbox.y() + aspectratio$letterbox.h()),
                    i,
                    0.0F
                );
                if ("Theme".equals(this.barStyle.get())) {
                    Paint.box(g, 0, aspectratio$letterbox.y() - 1, g.getScaledWindowWidth(), 2, Theme.alpha(Theme.ACCENT, 160), 0.0F);
                    Paint.box(g, 0, aspectratio$letterbox.y() + aspectratio$letterbox.h() - 1, g.getScaledWindowWidth(), 2, Theme.alpha(Theme.ACCENT, 160), 0.0F);
                }
            }
        }
    }
}
