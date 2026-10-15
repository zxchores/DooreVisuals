package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.overlay.HudFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/**
 * Performance profiles and automatic relief. A profile changes the visual quality level and, for "Макс fps", switches the
 * heaviest effects off (and remembers which ones, so another profile brings them back). With "Авто" on, the module watches
 * the frame rate and steps the quality down when it stays under the threshold, and back up when there is plenty of room.
 */
public final class PerformanceFeature extends Feature implements Tick {
    private static final String MAX = "Макс fps";
    private static final String BALANCE = "Баланс";
    private static final String BEAUTY = "Красота";
    private static final String[] HEAVY = {"bloom", "water_reflections", "shadow_enhancer", "post_fx", "motion_blur", "world_particles"};
    private static final String[] LEVELS = {"Низкое", "Среднее", "Высокое"};

    private final Opt.Pick profile = this.opt(new Opt.Pick("profile", "Профиль", BEAUTY, MAX, BALANCE, BEAUTY).chips());
    private final Opt.Key cycleKey = this.opt(new Opt.Key("cycle_key", "Клавиша: следующий профиль", -1));
    private final Opt.Flag auto = this.opt(new Opt.Flag("auto", "Авто-снижение при низком fps", true));
    private final Opt.Num floor = this.opt(new Opt.Num("floor", "Порог fps", 40.0, 20.0, 120.0, 5.0).visibleWhen(() -> this.auto.get()));
    /** The effects switched off by "Макс fps", as ids; hidden, but saved so they come back after a restart too. */
    private final Opt.Text restore = this.opt(new Opt.Text("restore", "restore", "").visibleWhen(() -> false));

    private String applied = "";
    private boolean keyWasDown;
    private float fpsAvg = 60.0F;
    private int lowTicks;
    private int highTicks;
    private int lowered;

    public PerformanceFeature() {
        super(
                "performance",
                "Performance",
                "Профили fps / баланс / красота и автоматическое снижение эффектов при просадках",
                Category.TOOLS,
                true
        );
        this.badge(Badge.NEW);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player == null) {
            return;
        }
        String now = this.profile.get();
        if (!now.equals(this.applied)) {
            this.applyProfile(now, !this.applied.isEmpty());
            this.applied = now;
        }

        int key = this.cycleKey.get();
        if (key > 0 && mc.currentScreen == null) {
            boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
            if (down && !this.keyWasDown) {
                this.profile.next();
                this.poke();
            }
            this.keyWasDown = down;
        }

        if (this.auto.get()) {
            this.watchFps(mc);
        }
    }

    /** About three seconds of average fps; below the threshold for four seconds lowers one step, far above it for 25 raises one. */
    private void watchFps(MinecraftClient mc) {
        this.fpsAvg += (mc.getCurrentFps() - this.fpsAvg) * 0.03F;
        float limit = this.floor.f();
        if (this.fpsAvg < limit) {
            this.lowTicks++;
            this.highTicks = 0;
        } else if (this.lowered > 0 && this.fpsAvg > limit * 2.4F) {
            this.highTicks++;
            this.lowTicks = 0;
        } else {
            this.lowTicks = 0;
            this.highTicks = 0;
        }
        if (this.lowTicks > 80) {
            this.lowTicks = 0;
            this.step(-1);
        } else if (this.highTicks > 500) {
            this.highTicks = 0;
            this.step(1);
        }
    }

    private void step(int dir) {
        App.features().find(ThemeFeature.class).ifPresent(theme -> {
            int index = 2;
            String current = theme.qualityLabel();
            for (int i = 0; i < LEVELS.length; i++) {
                if (LEVELS[i].equals(current)) {
                    index = i;
                }
            }
            int next = Math.max(0, Math.min(2, index + dir));
            if (next == index) {
                return;
            }
            theme.setQualityLabel(LEVELS[next]);
            this.lowered = Math.max(0, this.lowered - dir);
            String text = dir < 0
                    ? "Качество эффектов снижено: " + LEVELS[next] + " (fps ~" + Math.round(this.fpsAvg) + ")"
                    : "Качество эффектов возвращено: " + LEVELS[next];
            App.features().find(HudFeature.class).ifPresent(h -> h.notify("Производительность", text));
        });
    }

    private void applyProfile(String name, boolean announce) {
        App.features().find(ThemeFeature.class).ifPresent(theme -> theme.setQualityLabel(switch (name) {
            case MAX -> LEVELS[0];
            case BALANCE -> LEVELS[1];
            default -> LEVELS[2];
        }));
        this.lowered = 0;
        if (MAX.equals(name)) {
            List<String> off = new ArrayList<>();
            for (Feature f : App.features().all()) {
                for (String id : HEAVY) {
                    if (f.id().equals(id) && f.on()) {
                        f.set(false);
                        off.add(id);
                    }
                }
            }
            if (!off.isEmpty()) {
                this.restore.set(String.join(",", off));
            }
        } else if (!this.restore.get().isEmpty()) {
            for (String id : this.restore.get().split(",")) {
                for (Feature f : App.features().all()) {
                    if (f.id().equals(id)) {
                        f.set(true);
                    }
                }
            }
            this.restore.set("");
        }
        if (announce) {
            App.features().find(HudFeature.class).ifPresent(h -> h.notify("Профиль", name));
        }
    }
}
