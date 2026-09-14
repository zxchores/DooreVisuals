package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.ConfigFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class ConfigApplyScreen extends Screen {
    private static String pendingIp = "";
    private static String pendingName = "";
    private static long showAt;
    private final String ip;
    private final String configName;
    private float noX;
    private float yesX;
    private float by;
    private float bw;
    private float bh;
    private int mx;
    private int my;

    private ConfigApplyScreen(String ip, String configName) {
        super(Text.literal("\u041a\u043e\u043d\u0444\u0438\u0433"));
        this.ip = ip;
        this.configName = configName;
    }

    public static void offer(String ip, String name) {
        pendingIp = ip == null ? "" : ip;
        pendingName = name == null ? "" : name;
        showAt = System.currentTimeMillis() + 1800L;
    }

    public static void tick(MinecraftClient mc) {
        if (!pendingName.isBlank() && mc != null && mc.player != null && mc.world != null) {
            if (System.currentTimeMillis() >= showAt) {
                if (mc.currentScreen == null) {
                    String s = pendingName;
                    String s1 = pendingIp;
                    pendingName = "";
                    pendingIp = "";
                    mc.setScreen(new ConfigApplyScreen(s1, s));
                }
            }
        }
    }

    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 1711276032);
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        super.render(g, mouseX, mouseY, partialTick);
        Ui.frame(g, () -> this.paintCard(g));
    }

    private void paintCard(DrawContext g) {
        float f = Math.min(340.0F, this.width - 48.0F);
        float f1 = 108.0F;
        float f2 = (this.width - f) * 0.5F;
        float f3 = this.height * 0.38F;
        Paint.chromeGui(g, f2, f3, f, f1, 12.0F);
        Paint.textC(
            g,
            "\u041d\u0435 \u0445\u043e\u0442\u0438\u0442\u0435 \u043b\u0438 \u043f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c \u044d\u0442\u043e\u0442 \u043a\u043e\u043d\u0444\u0438\u0433",
            f2 + f * 0.5F,
            f3 + 16.0F,
            Theme.TEXT,
            8.2F
        );
        Paint.textC(g, "(" + this.configName + ")?", f2 + f * 0.5F, f3 + 32.0F, Theme.ACCENT_HOT, 8.6F);
        this.bw = (f - 36.0F) * 0.5F;
        this.bh = 26.0F;
        this.by = f3 + f1 - 40.0F;
        this.noX = f2 + 12.0F;
        this.yesX = f2 + 24.0F + this.bw;
        boolean flag = Paint.hit((double)this.mx, (double)this.my, this.noX, this.by, this.bw, this.bh);
        boolean flag1 = Paint.hit((double)this.mx, (double)this.my, this.yesX, this.by, this.bw, this.bh);
        Paint.box(g, this.noX, this.by, this.bw, this.bh, Theme.alpha(16777215, flag ? 22 : 12), 8.0F);
        Paint.box(g, this.yesX, this.by, this.bw, this.bh, Theme.alpha(Theme.ACCENT, flag1 ? 70 : 48), 8.0F);
        Paint.textC(g, "\u041d\u0435 \u043f\u0440\u0438\u043c\u0435\u043d\u044f\u0442\u044c", this.noX + this.bw * 0.5F, this.by + 8.0F, Theme.TEXT, 7.4F);
        Paint.textC(g, "\u041f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c", this.yesX + this.bw * 0.5F, this.by + 8.0F, Theme.VOID, 7.4F);
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        } else if (Paint.hit(event.x(), event.y(), this.noX, this.by, this.bw, this.bh)) {
            this.decline();
            return true;
        } else if (Paint.hit(event.x(), event.y(), this.yesX, this.by, this.bw, this.bh)) {
            this.apply();
            return true;
        } else {
            return super.mouseClicked(event, doubled);
        }
    }

    public boolean keyPressed(KeyInput event) {
        if (event.key() == 256) {
            this.decline();
            return true;
        } else if (event.key() != 257 && event.key() != 335) {
            return super.keyPressed(event);
        } else {
            this.apply();
            return true;
        }
    }

    public boolean shouldPause() {
        return false;
    }

    private void apply() {
        if (App.live()) {
            App.features().find(ConfigFeature.class).ifPresent(c -> c.applyNamed(this.configName));
            App.save().profiles().bindServer(this.ip, this.configName);
        }

        this.close();
    }

    private void decline() {
        if (App.live()) {
            App.save().profiles().skipPrompt(this.ip, this.configName);
            App.features()
                .find(HudFeature.class)
                .ifPresent(
                    h -> h.notify("\u041a\u043e\u043d\u0444\u0438\u0433", "\u041e\u0441\u0442\u0430\u0432\u043b\u0435\u043d " + App.save().profiles().active())
                );
        }

        this.close();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }
}
