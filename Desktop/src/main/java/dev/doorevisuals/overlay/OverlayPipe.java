package dev.doorevisuals.overlay;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.tools.AspectFeature;
import dev.doorevisuals.tools.ContainerChrome;
import dev.doorevisuals.tools.CrosshairFeature;
import dev.doorevisuals.server.EventsFeature;
import dev.doorevisuals.tools.CooldownsFeature;
import dev.doorevisuals.tools.GpsFeature;
import dev.doorevisuals.ui.PanelScreen;
import dev.doorevisuals.world.TargetEspFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                    App.features().find(AspectFeature.class).filter(Feature::on).ifPresent(f -> f.paintBars(g, dt));
                    Ui.frame(g, () -> {
                        App.features().find(HudFeature.class).filter(Feature::on).ifPresent(hud -> {
                            try {
                                hud.paint(g, dt);
                            } catch (Throwable throwable1) {
                                LOG.error("HUD", throwable1);
                            }
                        });
                        App.features().find(CrosshairFeature.class).filter(Feature::on).ifPresent(c -> {
                            try {
                                c.paint(g);
                            } catch (Throwable throwable1) {
                                LOG.error("Crosshair", throwable1);
                            }
                        });
                        App.features().find(GpsFeature.class).filter(Feature::on).ifPresent(gps -> {
                            try {
                                gps.paintHud(g);
                            } catch (Throwable throwable1) {
                                LOG.error("GPS", throwable1);
                            }
                        });
                        App.features().find(TargetEspFeature.class).filter(Feature::on).ifPresent(esp -> {
                            try {
                                esp.paintHud(g);
                            } catch (Throwable throwable1) {
                                LOG.error("Target ESP HUD", throwable1);
                            }
                        });
                        App.features().find(HotbarFeature.class).filter(Feature::on).ifPresent(hb -> {
                            try {
                                hb.paintNvg(g);
                            } catch (Throwable throwable1) {
                                LOG.error("Hotbar NVG", throwable1);
                            }
                        });
                        App.features().find(EventsFeature.class).filter(Feature::on).ifPresent(ev -> {
                            try {
                                ev.paintHud(g);
                            } catch (Throwable throwable1) {
                                LOG.error("Events", throwable1);
                            }
                        });
                    });
                    App.features().find(HudFeature.class).filter(Feature::on).ifPresent(hud -> {
                        try {
                            hud.paintItems(g);
                        } catch (Throwable throwable1) {
                            LOG.error("Armor icons", throwable1);
                        }
                    });
                    App.features().find(HotbarFeature.class).filter(Feature::on).ifPresent(hb -> {
                        try {
                            hb.paintItems(g);
                        } catch (Throwable throwable1) {
                            LOG.error("Hotbar items", throwable1);
                        }
                    });
                    try {
                        CooldownsFeature.paintHotbar(g);
                    } catch (Throwable throwable1) {
                        LOG.error("Cooldowns hotbar", throwable1);
                    }
                } catch (Throwable throwable) {
                    LOG.error("Overlay frame", throwable);
                }
            }
        }
    }

    private static boolean shouldHideHud(MinecraftClient mc) {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)
            ? mc.currentScreen instanceof PanelScreen || mc.currentScreen instanceof GameMenuScreen || mc.currentScreen instanceof HandledScreen
            : false;
    }
}
