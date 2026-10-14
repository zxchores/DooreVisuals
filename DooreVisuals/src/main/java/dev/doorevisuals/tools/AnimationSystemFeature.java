package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;

/** Central easing/speed profile used by client animations. */
public final class AnimationSystemFeature extends Feature {
    private final Opt.Pick easing = this.opt(new Opt.Pick(
        "easing", "Easing", "Стандартный",
        "Стандартный", "Out Quint", "In Out", "Back", "Elastic"
    ));
    private final Opt.Num speed = this.opt(new Opt.Num("speed", "Скорость анимаций", 1.0, 0.5, 1.8, 0.05));

    public AnimationSystemFeature() {
        super("animation_system", "Animation System", "Единый easing и скорость анимаций клиента", Category.SYSTEM, true);
        this.sync();
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    @Override
    protected void enable() { this.sync(); }
    @Override
    protected void disable() { Anim.setGlobalEasing(Anim.Easing.STANDARD); Anim.setGlobalSpeed(1.0F); }

    private void sync() {
        Anim.setGlobalSpeed(speed.f());
        Anim.setGlobalEasing(switch ((String)easing.get()) {
            case "Out Quint" -> Anim.Easing.OUT_QUINT;
            case "In Out" -> Anim.Easing.IN_OUT;
            case "Back" -> Anim.Easing.BACK;
            case "Elastic" -> Anim.Easing.ELASTIC;
            default -> Anim.Easing.STANDARD;
        });
    }
}
