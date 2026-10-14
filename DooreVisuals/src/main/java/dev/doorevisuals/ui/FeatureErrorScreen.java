package dev.doorevisuals.ui;

import dev.doorevisuals.core.FeatureGuard;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import java.awt.Desktop;
import java.nio.file.Path;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** A small actionable report shown after a module is safely disabled. */
public final class FeatureErrorScreen extends Screen {
    private final FeatureGuard.Failure failure;
    private float cardX, cardY, cardW, cardH;
    private float logX, closeX, buttonY, buttonW;
    private String logStatus = "";
    private int mx, my;

    public FeatureErrorScreen(FeatureGuard.Failure failure) {
        super(Text.literal("DooreVisuals — сбой модуля"));
        this.failure = failure;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        mx = mouseX; my = mouseY;
        context.fill(0, 0, width, height, 0xB8000000);
        Ui.frame(context, () -> paint(context));
    }

    private void paint(DrawContext g) {
        cardW = Math.min(410.0F, width - 32.0F);
        cardH = 190.0F;
        cardX = (width - cardW) * 0.5F;
        cardY = (height - cardH) * 0.5F;
        Paint.chromeGui(g, cardX, cardY, cardW, cardH, 12.0F);
        Paint.text(g, "Модуль отключён после ошибки", cardX + 18, cardY + 16, Theme.TEXT, 11);
        Paint.text(g, fit(failure.name(), cardW - 36, 8.5F), cardX + 18, cardY + 42, Theme.ACCENT_HOT, 8.5F);
        Paint.text(g, "Этап: " + failure.stage(), cardX + 18, cardY + 62, Theme.MUTED, 7.2F);
        Paint.text(g, fit("Причина: " + failure.message(), cardW - 36, 7.0F), cardX + 18, cardY + 80, Theme.TEXT, 7.0F);
        Paint.text(g, "Остальные модули продолжают работать. Подробности записаны в logs/latest.log.", cardX + 18, cardY + 101, Theme.MUTED, 6.4F);
        buttonW = Math.min(176, (cardW - 48) * 0.5F);
        buttonY = cardY + cardH - 40;
        logX = cardX + 16;
        closeX = cardX + cardW - buttonW - 16;
        button(g, logX, buttonY, buttonW, "Открыть лог", true);
        button(g, closeX, buttonY, buttonW, "Продолжить", false);
        if (!logStatus.isBlank()) Paint.text(g, fit(logStatus, cardW - 36, 6.0F), cardX + 18, cardY + cardH - 58, Theme.MUTED, 6.0F);
    }

    private void button(DrawContext g, float x, float y, float w, String label, boolean accent) {
        boolean hover = Paint.hit(mx, my, x, y, w, 26);
        Paint.box(g, x, y, w, 26, Theme.alpha(accent ? Theme.ACCENT : 16777215, hover ? 60 : 26), 7);
        Paint.textC(g, label, x + w * 0.5F, y + 8, Theme.TEXT, 7.2F);
    }

    private static String fit(String text, float width, float size) {
        if (text == null) return "";
        String value = text;
        while (value.length() > 4 && Paint.tw(value, size) > width) value = value.substring(0, value.length() - 4) + "...";
        return value;
    }

    private void openLog() {
        Path path = this.client.runDirectory.toPath().resolve("logs/latest.log");
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(path.toFile());
            else logStatus = "Откройте файл вручную: " + path;
        } catch (Throwable error) {
            logStatus = "Не удалось открыть. Файл: " + path;
        }
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 0 && Paint.hit(event.x(), event.y(), logX, buttonY, buttonW, 26)) {
            openLog(); return true;
        }
        if (event.button() == 0 && Paint.hit(event.x(), event.y(), closeX, buttonY, buttonW, 26)) {
            client.setScreen(null); return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean shouldPause() { return false; }
}
