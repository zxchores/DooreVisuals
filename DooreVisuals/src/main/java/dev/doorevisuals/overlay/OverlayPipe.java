package dev.doorevisuals.overlay;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.FeatureGuard;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.tools.AspectFeature;
import dev.doorevisuals.tools.ContainerChrome;
import dev.doorevisuals.tools.CrosshairFeature;
import dev.doorevisuals.tools.CooldownsFeature;
import dev.doorevisuals.tools.GpsFeature;
import dev.doorevisuals.world.TargetEspFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

public final class OverlayPipe {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/HUD");

    private OverlayPipe() {
    }

    public static void draw(DrawContext g, RenderTickCounter dt) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        ContainerChrome.reset();
        if (minecraftclient.player != null && !minecraftclient.options.hudHidden && App.live()) {
            if (!shouldHideHud(minecraftclient)) {
                try {
                    run(AspectFeature.class, "полосы экрана", f -> f.paintBars(g, dt));
                    Ui.frame(g, () -> {
                        run(HudFeature.class, "HUD", hud -> hud.paint(g, dt));
                        run(DamageIndicatorFeature.class, "индикатор урона", fx -> fx.paint(g));
                        run(CrosshairFeature.class, "прицел", c -> c.paint(g));
                        run(GpsFeature.class, "HUD GPS", gps -> gps.paintHud(g));
                        run(TargetEspFeature.class, "HUD Target ESP", esp -> esp.paintHud(g));
                        run(HotbarFeature.class, "панель быстрого доступа", hb -> hb.paintNvg(g));
                    });
                    run(HudFeature.class, "иконки HUD", hud -> hud.paintItems(g));
                    run(HotbarFeature.class, "иконки хотбара", hb -> hb.paintItems(g));
                    run(CooldownsFeature.class, "перезарядка хотбара", c -> c.paintHotbar(g));
                } catch (Throwable throwable) {
                    LOG.error("Overlay frame", throwable);
                }
            }
        }
    }

    private static boolean shouldHideHud(MinecraftClient mc) {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)
            ? mc.currentScreen instanceof GameMenuScreen || mc.currentScreen instanceof HandledScreen
            : false;
    }

    private static <T extends Feature> void run(Class<T> type, String stage, Consumer<T> action) {
        App.features().find(type).ifPresent(feature -> FeatureGuard.run(feature, stage, action));
    }
}
