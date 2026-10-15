package dev.doorevisuals;

import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.FeatureGuard;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.SessionStats;
import dev.doorevisuals.net.CosmeticsNet;
import dev.doorevisuals.net.IrisAutoInstaller;
import dev.doorevisuals.overlay.OverlayPipe;
import dev.doorevisuals.overlay.TpsMeter;
import dev.doorevisuals.tools.GuiWindowsFeature;
import dev.doorevisuals.tools.InvClicks;
import dev.doorevisuals.ui.CardGuiScreen;
import dev.doorevisuals.ui.ClickGuiScreen;
import dev.doorevisuals.ui.MenuGui;
import dev.doorevisuals.ui.ConfigApplyScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DooreClient implements ClientModInitializer {
    private static boolean menuKeyWasDown;
    public static final String ID = "doorevisuals";
    public static final String VERSION = "1.2.1";
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals");

    private static boolean pipelinesWarmed;

    @Override
    public void onInitializeClient() {
        App.boot();
        CosmeticsNet.initClient();
        IrisAutoInstaller.ensureStarted();
        ClientTickEvents.END_CLIENT_TICK.register((EndTick)mc -> {
            if (App.live()) {
                // The shader sources exist only after the first resource load, so the pipelines are compiled once the
                // first world is up; compiling them earlier fails, and a failed pipeline stays failed.
                if (!pipelinesWarmed && mc.world != null) {
                    pipelinesWarmed = true;
                    try {
                        dev.doorevisuals.draw.Gpu.precompileAll();
                    } catch (Throwable error) {
                        LOG.warn("Pipeline precompile skipped: {}", error.toString());
                    }
                }
                ConfigApplyScreen.tick(mc);
                App.aim().pulse(mc);
                SessionStats.tick(mc);
                TpsMeter.tick(mc);
                InvClicks.tick(mc);

                boolean rShift = GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                // Right Shift opens the menu in the game only. Chat, inventory and other screens keep their own input.
                if (rShift && !menuKeyWasDown && (mc.currentScreen == null || mc.currentScreen instanceof MenuGui)) {
                    if (mc.currentScreen instanceof MenuGui menu) {
                        menu.requestClose();
                    } else if (GuiWindowsFeature.cardMenu()) {
                        mc.setScreen(new CardGuiScreen(mc.currentScreen));
                    } else {
                        mc.setScreen(new ClickGuiScreen(mc.currentScreen));
                    }
                }
                menuKeyWasDown = rShift;

                boolean flag = mc.currentScreen != null;
                long i = mc.getWindow().getHandle();

                for (Feature feature : App.features().all()) {
                    FeatureGuard.run(feature, "обработка бинда", () -> feature.pollBind(i, flag));

                    if (feature.on() && feature instanceof Tick tick) {
                        FeatureGuard.run(feature, "обновление", () -> tick.tick(mc));
                    }
                }
                FeatureGuard.showPending(mc);
            }
        });
        HudElementRegistry.addLast(Identifier.of("doorevisuals", "hud"), OverlayPipe::draw);
        ClientLifecycleEvents.CLIENT_STARTED.register((ClientStarted)c -> {
            Thread thread = new Thread(App::startIntegrations, "DooreVisuals-Integrations");
            thread.setDaemon(true);
            thread.start();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)c -> App.shutdown());
        LOG.info("DooreVisuals {} cold-start complete", VERSION);
    }
}