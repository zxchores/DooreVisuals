package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;

public final class BrightFeature extends Feature implements Tick {
    private static volatile boolean active;
    private static volatile float power = 1.0F;
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "\u0421\u0438\u043b\u0430", 100.0, 10.0, 100.0, 5.0));
    private Double gamma;

    public BrightFeature() {
        super(
            "bright",
            "Full Bright",
            "\u041f\u043e\u043b\u043d\u0430\u044f \u044f\u0440\u043a\u043e\u0441\u0442\u044c lightmap \u0431\u0435\u0437 \u0437\u0435\u043b\u044c\u044f \u043d\u043e\u0447\u043d\u043e\u0433\u043e \u0437\u0440\u0435\u043d\u0438\u044f",
            Category.WORLD,
            false
        );
    }

    public static boolean active() {
        return active;
    }

    public static float power() {
        return power;
    }

    @Override
    protected void enable() {
        active = true;
        power = this.strength.f() / 100.0F;
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        this.gamma = (Double)minecraftclient.options.getGamma().getValue();
        minecraftclient.options.getGamma().setValue(1.0);
        bumpLightmap(minecraftclient);
    }

    @Override
    protected void disable() {
        active = false;
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (this.gamma != null) {
            minecraftclient.options.getGamma().setValue(this.gamma);
            this.gamma = null;
        }

        bumpLightmap(minecraftclient);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (!this.on()) {
            active = false;
        } else {
            active = true;
            power = this.strength.f() / 100.0F;
            if ((Double)mc.options.getGamma().getValue() < 1.0) {
                mc.options.getGamma().setValue(1.0);
            }
        }
    }

    private static void bumpLightmap(MinecraftClient mc) {
        try {
            if (mc.gameRenderer != null) {
                mc.gameRenderer.getLightmapTextureManager().tick();
            }
        } catch (Throwable throwable) {
        }
    }
}
