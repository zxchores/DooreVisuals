package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.DeathRecap;
import dev.doorevisuals.tools.GpsFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class DooreDeathScreen extends DeathScreen {
    private final boolean hardcoreFlag;
    private final Text cause;
    private final double deathX;
    private final double deathY;
    private final double deathZ;
    private final String dimension;
    private final List<DooreDeathScreen.Hit> hits = new ArrayList<>();
    private int mx;
    private int my;
    private String status = "";

    public DooreDeathScreen(Text causeOfDeath, boolean hardcore, ClientPlayerEntity player) {
        super(causeOfDeath, hardcore, player);
        this.hardcoreFlag = hardcore;
        this.cause = causeOfDeath;
        if (player != null) {
            this.deathX = player.getX();
            this.deathY = player.getY();
            this.deathZ = player.getZ();
            RegistryKey<World> registrykey = player.getEntityWorld().getRegistryKey();
            this.dimension = registrykey.getValue().toString();
        } else {
            this.deathX = this.deathY = this.deathZ = 0.0;
            this.dimension = "minecraft:overworld";
        }
    }

    protected void init() {
        this.clearChildren();
        App.features().find(GpsFeature.class).ifPresent(gps -> {
            gps.set(true);
            gps.addDeath(this.deathX, this.deathY, this.deathZ, this.dimension);
        });
    }

    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, -301660152);
        g.fill(0, 0, this.width, this.height / 3, Theme.alpha(Theme.ACCENT_DEEP, 90));
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        Nvg.run(g, () -> {
            float f = this.width * 0.5F;
            float f1 = Math.min(340, this.width - 40);
            float f2 = 228.0F;
            float f3 = f - f1 * 0.5F;
            float f4 = this.height * 0.5F - f2 * 0.5F;
            Paint.glass(g, f3, f4, f1, f2, 10.0F);
            Paint.textC(g, this.hardcoreFlag ? "Hardcore" : "You Died", f, f4 + 14.0F, Theme.TEXT, 16.0F);
            if (DeathRecap.any()) {
                String s = DeathRecap.attacker().isBlank() ? "\u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e" : DeathRecap.attacker();
                Paint.textC(g, "\u0443\u0431\u0438\u043b  " + s, f, f4 + 34.0F, Theme.ACCENT_HOT, 8.4F);
                if (!DeathRecap.weapon().isBlank()) {
                    Paint.textC(g, DeathRecap.weapon(), f, f4 + 48.0F, Theme.MUTED, 7.2F);
                }
            } else if (this.cause != null) {
                String s1 = this.cause.getString();
                if (s1.length() > 36) {
                    s1 = s1.substring(0, 35) + "\u2026";
                }

                Paint.textC(g, s1, f, f4 + 34.0F, Theme.MUTED, 8.0F);
            }

            String s2 = String.format(Locale.ROOT, "%.0f  %.0f  %.0f", this.deathX, this.deathY, this.deathZ);
            Paint.textC(g, s2, f, f4 + 64.0F, Theme.ACCENT_HOT, 11.0F);
            Paint.textC(g, this.dimension, f, f4 + 80.0F, Theme.MUTED, 7.0F);
            float f5 = f4 + 102.0F;
            float f6 = (f1 - 24.0F) / 2.0F;
            this.btn(g, f3 + 10.0F, f5, f6 - 4.0F, 18.0F, "\u0412\u043e\u0437\u0440\u043e\u0434\u0438\u0442\u044c\u0441\u044f", this::respawn);
            this.btn(g, f3 + 14.0F + f6, f5, f6 - 4.0F, 18.0F, "\u0412 \u043c\u0435\u043d\u044e", this::toTitle);
            f5 += 24.0F;
            this.btn(g, f3 + 10.0F, f5, f6 - 4.0F, 18.0F, "\u041a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c XYZ", this::copyCoords);
            this.btn(g, f3 + 14.0F + f6, f5, f6 - 4.0F, 18.0F, "/back", this::sendBack);
            f5 += 24.0F;
            this.btn(g, f3 + 10.0F, f5, f1 - 20.0F, 18.0F, "GPS here", this::gpsHere);
            if (!this.status.isEmpty()) {
                Paint.textC(g, this.status, f, f4 + f2 - 14.0F, Theme.MUTED, 7.0F);
            }
        });
    }

    private void btn(DrawContext g, float x, float y, float w, float h, String label, Runnable act) {
        boolean flag = Paint.hit((double)this.mx, (double)this.my, x, y, w, h);
        Paint.box(g, x, y, w, h, flag ? Theme.alpha(Theme.ACCENT, 80) : Theme.alpha(16777215, 14), 5.0F);
        if (flag) {
            Paint.outline(g, x, y, w, h, Theme.alpha(Theme.ACCENT, 160), 5.0F);
        }

        Paint.textC(g, label, x + w * 0.5F, y + 4.5F, flag ? Theme.ACCENT_HOT : Theme.TEXT, 8.5F);
        this.hits.add(new DooreDeathScreen.Hit(x, y, w, h, act));
    }

    private void respawn() {
        if (this.client != null && this.client.player != null) {
            this.client.player.requestRespawn();
            this.client.setScreen(null);
        }
    }

    private void toTitle() {
        if (this.client != null) {
            try {
                this.client.disconnectWithProgressScreen();
            } catch (Throwable throwable) {
                this.client.setScreen(new TitleScreen());
            }
        }
    }

    private void copyCoords() {
        String s = String.format(Locale.ROOT, "%.1f %.1f %.1f", this.deathX, this.deathY, this.deathZ);
        if (this.client != null) {
            this.client.keyboard.setClipboard(s);
        }

        this.status = "\u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u043e";
    }

    private void sendBack() {
        if (this.client != null && this.client.player != null) {
            this.client.player.networkHandler.sendChatMessage("/back");
            this.status = "/back \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d";
        }
    }

    private void gpsHere() {
        App.features().find(GpsFeature.class).ifPresent(gps -> {
            gps.set(true);
            gps.addDeath(this.deathX, this.deathY, this.deathZ, this.dimension);
        });
        this.status = "\u0442\u043e\u0447\u043a\u0430 death";
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 0) {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                DooreDeathScreen.Hit dooredeathscreen$hit = this.hits.get(i);
                if (Paint.hit(
                    event.x(), event.y(), dooredeathscreen$hit.x, dooredeathscreen$hit.y, dooredeathscreen$hit.w, dooredeathscreen$hit.h
                )) {
                    dooredeathscreen$hit.action.run();
                    return true;
                }
            }
        }

        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private record Hit(float x, float y, float w, float h, Runnable action) {
    }
}
