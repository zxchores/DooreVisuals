package dev.doorevisuals;

import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.SessionStats;
import dev.doorevisuals.net.CosmeticsNet;
import dev.doorevisuals.overlay.OverlayPipe;
import dev.doorevisuals.overlay.TpsMeter;
import dev.doorevisuals.tools.InvClicks;
import dev.doorevisuals.ui.ConfigApplyScreen;
import dev.doorevisuals.ui.PanelScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DooreClient implements ClientModInitializer {
    public static final String ID = "doorevisuals";
    public static final String VERSION = "3.30.2";
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals");
    private static final Category KEYS = Category.create(Identifier.of("doorevisuals", "main"));

    public void onInitializeClient() {
        App.boot();
        CosmeticsNet.initClient();
        KeyBinding keybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.doorevisuals.open_menu", Type.KEYSYM, 344, KEYS));
        ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
            if (App.live()) {
                ConfigApplyScreen.tick(mc);
                App.aim().pulse(mc);
                SessionStats.tick(mc);
                TpsMeter.tick(mc);
                InvClicks.tick(mc);

                while (keybinding.wasPressed()) {
                    if (mc.currentScreen instanceof PanelScreen panelscreen) {
                        panelscreen.requestClose();
                    } else {
                        mc.setScreen(new PanelScreen());
                    }
                }

                boolean flag = mc.currentScreen != null;
                long i = mc.getWindow().getHandle();

                for (Feature feature : App.features().all()) {
                    try {
                        feature.pollBind(i, flag);
                    } catch (Throwable throwable1) {
                        LOG.error("Bind {}", feature.id(), throwable1);
                    }

                    if (feature.on() && feature instanceof Tick tick) {
                        try {
                            tick.tick(mc);
                        } catch (Throwable throwable) {
                            LOG.error("Tick {}", feature.id(), throwable);
                        }
                    }
                }
            }
        });
        HudElementRegistry.addLast(Identifier.of("doorevisuals", "hud"), OverlayPipe::draw);
        ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted)c -> {
            Thread thread = new Thread(App::startIntegrations, "DooreVisuals-Integrations");
            thread.setDaemon(true);
            thread.start();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)c -> App.shutdown());
        LOG.info("DooreVisuals {} cold-start complete", "3.30.2");
    }
}
