package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;

/** Shared background blur used by transparent client screens. */
public final class BlurSystemFeature extends Feature {
    private static volatile boolean live;
    private static volatile int passes = 1;
    private final Opt.Num strength = this.opt(new Opt.Num("strength", "Сила размытия", 0.55, 0.15, 1.0, 0.05));
    private final Opt.Flag clickGui = this.opt(new Opt.Flag("click_gui", "ClickGUI", true));
    private final Opt.Flag menus = this.opt(new Opt.Flag("menus", "Игровые меню", true));
    private final Opt.Flag chat = this.opt(new Opt.Flag("chat", "Чат", false));

    public BlurSystemFeature() {
        super("blur_system", "Blur System", "Общее размытие мира под интерфейсом", Category.SYSTEM, false);
        this.sync();
    }

    public static boolean active() {
        return live;
    }

    public static int passes() {
        return live ? passes : 0;
    }

    public static boolean shouldBlur(MinecraftClient mc) {
        if (!live || mc == null || dev.doorevisuals.App.features() == null) return false;
        Screen screen = mc.currentScreen;
        if (screen == null || screen instanceof HandledScreen<?>) return false;
        BlurSystemFeature feature = dev.doorevisuals.App.features().find(BlurSystemFeature.class).orElse(null);
        if (feature == null) return false;
        if (screen instanceof GameMenuScreen) {
            return feature.menus.get();
        }
        return feature.chat.get() && screen instanceof ChatScreen;
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    @Override
    protected void enable() {
        live = true;
        this.sync();
    }

    @Override
    protected void disable() {
        live = false;
    }

    private void sync() {
        passes = Math.max(1, Math.min(2, Math.round((float)(this.strength.f() * 1.7F))));
    }
}
