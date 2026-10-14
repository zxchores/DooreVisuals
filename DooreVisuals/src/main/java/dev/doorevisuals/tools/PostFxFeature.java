package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.PostFx;
import dev.doorevisuals.draw.VisualQuality;
import net.minecraft.client.MinecraftClient;

/**
 * Пост-обработка кадра: bloom вокруг ярких участков (небо, лава, свечения клиента), виньетка и
 * зерно. Цепочка проходов лежит в {@code assets/doorevisuals/post_effect/}, применяется в конце
 * кадра.
 */
public final class PostFxFeature extends Feature implements Tick {
    private static volatile boolean live;

    private final Opt.Pick preset = this.opt(
            new Opt.Pick(
                    "preset",
                    "Сила свечения",
                    "Средний",
                    "Мягкий",
                    "Средний",
                    "Сильный"
            )
    );

    public PostFxFeature() {
        super(
                "post_fx",
                "Post Processing",
                "Bloom, виньетка и зерно на финальном кадре",
                Category.SYSTEM,
                false
        );
    }

    public static boolean active() {
        return live;
    }

    @Override
    protected void enable() {
        sync();
    }

    @Override
    protected void disable() {
        live = false;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (on()) {
            sync();
        } else {
            live = false;
        }
    }

    private void sync() {
        PostFx.setPreset(this.preset.get());
        // На низком качестве пост-обработка молчит: она не должна отнимать кадры у слабых машин.
        live = on() && VisualQuality.level() != VisualQuality.Level.LOW;
    }
}
