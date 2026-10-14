package dev.doorevisuals.cosmetics;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;

/**
 * Светящаяся кромка на косметике. Работает тем же приёмом, что и подсветка руки: та же модель
 * рисуется вторым проходом с пайплайном обводки ({@code Types.handSurface}), поэтому свечение ложится
 * по реальной геометрии плаща, а маской служит его собственная текстура.
 */
public final class CosmeticGlowFeature extends Feature implements Tick {
    private static volatile boolean live = false;
    private static volatile int styleIndex = 0;

    private final Opt.Pick style = this.opt(
            new Opt.Pick(
                    "style",
                    "Стиль свечения",
                    "Неон",
                    "Аура",
                    "Неон",
                    "Пульс",
                    "Контур"
            )
    );

    public CosmeticGlowFeature() {
        super(
                "cosmetic_glow",
                "Cosmetic Glow",
                "Светящаяся кромка на плаще и крыльях",
                Category.COSMETICS,
                false
        );
    }

    public static boolean active() {
        return live;
    }

    public static int styleIndex() {
        return styleIndex;
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
            dev.doorevisuals.draw.AccentTexture.update(dev.doorevisuals.tools.ThemeFeature.effectTint());
        } else {
            live = false;
        }
    }

    private void sync() {
        styleIndex = switch (this.style.get()) {
            case "Аура" -> 0;
            case "Пульс" -> 2;
            case "Контур" -> 3;
            default -> 1;
        };
        live = on();
    }
}
