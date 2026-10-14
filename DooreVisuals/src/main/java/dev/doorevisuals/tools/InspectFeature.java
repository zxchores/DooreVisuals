package dev.doorevisuals.tools;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Осмотр предмета в руке: по нажатию своей клавиши рука с предметом прокручивается, переворачивается
 * или проходит восьмёркой и плавно возвращается назад. Работает и на ванильном мече, и на кастомных
 * клинках — анимация задаёт позу, а не конкретную геометрию.
 */
public final class InspectFeature extends Feature implements Tick {
    private static volatile boolean live = false;
    private static volatile long startedAt = 0L;
    private static volatile float duration = 1.2F;
    private static volatile String style = "Прокрут";
    private static boolean wasDown;

    private final Opt.Pick styleOpt = this.opt(
            new Opt.Pick("style", "Стиль осмотра", "Прокрут", "Прокрут", "Переворот", "Восьмёрка")
    );

    private final Opt.Num durationOpt = this.opt(
            new Opt.Num("duration", "Длительность", 1.2, 0.6, 2.5, 0.1)
    );

    private final Opt.Key inspectKey = this.opt(
            new Opt.Key("inspect_key", "Клавиша осмотра", -1)
    );

    public InspectFeature() {
        super(
                "inspect",
                "Inspect",
                "Осмотр предмета в руке по клавише",
                Category.TOOLS,
                false
        );
        this.badge(Badge.NEW);
    }

    @Override
    protected void enable() {
        live = true;
        startedAt = 0L;
    }

    @Override
    protected void disable() {
        live = false;
        startedAt = 0L;
    }

    @Override
    public void tick(MinecraftClient mc) {
        live = on();
        style = this.styleOpt.get();
        duration = this.durationOpt.f();
        if (!live || mc == null || mc.getWindow() == null) {
            startedAt = 0L;
            return;
        }

        int code = this.inspectKey.get();
        boolean down = code > 0 && GLFW.glfwGetKey(mc.getWindow().getHandle(), code) == GLFW.GLFW_PRESS;
        if (down && !wasDown && !playing()) {
            startedAt = System.currentTimeMillis();
        }
        wasDown = down;
    }

    /** Идёт ли анимация прямо сейчас. */
    public static boolean playing() {
        return live && startedAt > 0L && progress() < 1.0F;
    }

    /** Прогресс анимации 0..1 с плавным ускорением и торможением. */
    public static float progress() {
        if (startedAt <= 0L) {
            return 1.0F;
        }
        float raw = (System.currentTimeMillis() - startedAt) / (Math.max(0.4F, duration) * 1000.0F);
        float t = Math.max(0.0F, Math.min(1.0F, raw));
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * Углы поворота руки с предметом: рыскание, тангаж, крен. Начало и конец — нули, поэтому поза
     * возвращается точно в исходное положение без рывка.
     */
    public static float[] angles() {
        float t = progress();
        if (!playing()) {
            return new float[] {0.0F, 0.0F, 0.0F};
        }

        // The same three styles Custom Sword uses live in one place, KnifeAnim; only the rotations matter here.
        float[] pose = KnifeAnim.generic(style, t);
        return new float[] {pose[3], pose[4], pose[5]};
    }
}
