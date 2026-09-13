package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.data.FirstRun;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.ThemeFeature;
import dev.doorevisuals.world.TargetEspFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class FirstRunScreen extends Screen {
    private final Screen next;
    private int step;
    private final List<FirstRunScreen.Hit> hits = new ArrayList<>();
    private int mx;
    private int my;

    public FirstRunScreen(Screen next) {
        super(Text.literal("Setup"));
        this.next = next;
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        g.fill(0, 0, this.width, this.height, -16447480);
        Nvg.run(g, () -> this.paint(g));
    }

    private void paint(DrawContext g) {
        float f = 390.0F;
        float f1 = 238.0F;
        float f2 = (this.width - f) / 2.0F;
        float f3 = (this.height - f1) / 2.0F;
        Paint.panelBody(g, f2, f3, f, f1);
        Paint.text(g, "DooreVisuals \u00b7 Setup", f2 + 16.0F, f3 + 14.0F, Theme.TEXT, 12.0F);
        Paint.text(g, "\u0428\u0430\u0433 " + (this.step + 1) + " / 2", f2 + 16.0F, f3 + 32.0F, Theme.MUTED, 8.0F);
        float f4 = f3 + 56.0F;
        switch (this.step) {
            case 0:
                Paint.text(g, "\u0412\u0438\u0437\u0443\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u0442\u0438\u043b\u044c", f2 + 16.0F, f4, Theme.TEXT, 9.0F);
                this.chip(g, f2 + 16.0F, f4 + 24.0F, "Doore", () -> App.features().find(ThemeFeature.class).ifPresent(t -> t.applyPreset("Doore")));
                this.chip(
                    g,
                    f2 + 100.0F,
                    f4 + 24.0F,
                    "\u041b\u0451\u0434",
                    () -> App.features().find(ThemeFeature.class).ifPresent(t -> t.applyPreset("\u041b\u0451\u0434"))
                );
                this.chip(
                    g,
                    f2 + 184.0F,
                    f4 + 24.0F,
                    "\u0417\u0430\u043a\u0430\u0442",
                    () -> App.features().find(ThemeFeature.class).ifPresent(t -> t.applyPreset("\u0417\u0430\u043a\u0430\u0442"))
                );
                this.chip(
                    g,
                    f2 + 16.0F,
                    f4 + 54.0F,
                    "\u0421\u0442\u0435\u043a\u043b\u043e",
                    () -> App.features().find(ThemeFeature.class).ifPresent(t -> t.setHudLook("\u0421\u0442\u0435\u043a\u043b\u043e"))
                );
                this.chip(
                    g,
                    f2 + 100.0F,
                    f4 + 54.0F,
                    "\u041d\u0435\u043e\u043d",
                    () -> App.features().find(ThemeFeature.class).ifPresent(t -> t.setHudLook("\u041d\u0435\u043e\u043d"))
                );
                this.chip(
                    g,
                    f2 + 184.0F,
                    f4 + 54.0F,
                    "\u041c\u0438\u043d\u0438\u043c\u0430\u043b",
                    () -> App.features().find(ThemeFeature.class).ifPresent(t -> t.setHudLook("\u041c\u0438\u043d\u0438\u043c\u0430\u043b"))
                );
                this.drawThemePreview(g, f2 + 292.0F, f4 + 16.0F);
                break;
            default:
                Paint.text(
                    g,
                    "\u042d\u0444\u0444\u0435\u043a\u0442\u044b \u0438 \u043f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c",
                    f2 + 16.0F,
                    f4,
                    Theme.TEXT,
                    9.0F
                );
                this.chip(g, f2 + 16.0F, f4 + 24.0F, "\u0428\u0430\u0440\u044b ESP", () -> this.setEsp("\u0428\u0430\u0440\u044b"));
                this.chip(g, f2 + 16.0F, f4 + 54.0F, "\u041d\u0438\u0437\u043a\u043e\u0435", () -> this.setQuality("\u041d\u0438\u0437\u043a\u043e\u0435"));
                this.chip(
                    g,
                    f2 + 100.0F,
                    f4 + 54.0F,
                    "\u0421\u0440\u0435\u0434\u043d\u0435\u0435",
                    () -> this.setQuality("\u0421\u0440\u0435\u0434\u043d\u0435\u0435")
                );
                this.chip(
                    g,
                    f2 + 200.0F,
                    f4 + 54.0F,
                    "\u0412\u044b\u0441\u043e\u043a\u043e\u0435",
                    () -> this.setQuality("\u0412\u044b\u0441\u043e\u043a\u043e\u0435")
                );
                this.drawEspPreview(g, f2 + 320.0F, f4 + 52.0F);
        }

        this.action(
            g,
            f2 + f - 100.0F,
            f3 + f1 - 40.0F,
            80.0F,
            22.0F,
            this.step < 1 ? "\u0414\u0430\u043b\u0435\u0435" : "\u0413\u043e\u0442\u043e\u0432\u043e",
            this::advance
        );
        if (this.step > 0) {
            this.action(g, f2 + 16.0F, f3 + f1 - 40.0F, 70.0F, 22.0F, "\u041d\u0430\u0437\u0430\u0434", () -> this.step--);
        }
    }

    private void setEsp(String style) {
        App.features().find(TargetEspFeature.class).ifPresent(t -> t.setStyle(style));
    }

    private void setQuality(String label) {
        App.features().find(ThemeFeature.class).ifPresent(t -> t.setQualityLabel(label));
    }

    private void advance() {
        if (this.step < 1) {
            this.step++;
        } else {
            FirstRun.markDone();
            this.client.setScreen(this.next);
        }
    }

    private void drawThemePreview(DrawContext g, float x, float y) {
        float f = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() * 0.002);
        Paint.box(g, x, y, 72.0F, 82.0F, Theme.alpha(527118, 220), 12.0F);
        Paint.grad(g, x + 7.0F, y + 7.0F, 58.0F, 30.0F, Theme.ACCENT, Theme.ACCENT_DIM, 9.0F, false);
        Paint.box(g, x + 8.0F, y + 47.0F, 56.0F, 10.0F, Theme.alpha(Theme.ACCENT, 35 + (int)(f * 30.0F)), 5.0F);
        Paint.box(g, x + 8.0F, y + 63.0F, 38.0F, 7.0F, Theme.alpha(16777215, 16), 4.0F);
        Paint.outline(g, x, y, 72.0F, 82.0F, Theme.alpha(Theme.ACCENT, 60), 12.0F);
    }

    private void drawEspPreview(DrawContext g, float x, float y) {
        Paint.box(g, x - 6.0F, y - 22.0F, 12.0F, 28.0F, Theme.alpha(16777215, 22), 5.0F);
        float f = (float)(System.currentTimeMillis() * 0.003);

        for (int i = 0; i < 3; i++) {
            float f1 = f + i * (float) (Math.PI * 2.0 / 3.0);
            float f2 = x + (float)Math.cos(f1) * 16.0F;
            float f3 = y + (float)Math.sin(f1 * 1.15) * 11.0F;
            Nvg.circle(f2, f3, 5.5F, Theme.alpha(Theme.ACCENT, 50));
            Nvg.circle(f2, f3, 3.2F, Theme.alpha(Theme.ACCENT, 210));
        }
    }

    private void chip(DrawContext g, float x, float y, String lab, Runnable act) {
        boolean flag = this.mx >= x && this.mx <= x + 76.0F && this.my >= y && this.my <= y + 18.0F;
        Paint.box(g, x, y, 76.0F, 18.0F, Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 70 : 16), 4.0F);
        Paint.textC(g, lab, x + 38.0F, y + 4.0F, Theme.TEXT, 8.0F);
        this.hits.add(new FirstRunScreen.Hit(x, y, 76.0F, 18.0F, act));
    }

    private void action(DrawContext g, float x, float y, float w, float h, String lab, Runnable act) {
        boolean flag = this.mx >= x && this.mx <= x + w && this.my >= y && this.my <= y + h;
        Paint.box(g, x, y, w, h, Theme.alpha(Theme.ACCENT, flag ? 90 : 50), 4.0F);
        Paint.textC(g, lab, x + w * 0.5F, y + 5.0F, Theme.TEXT, 8.5F);
        this.hits.add(new FirstRunScreen.Hit(x, y, w, h, act));
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        } else {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                FirstRunScreen.Hit firstrunscreen$hit = this.hits.get(i);
                if (event.comp_4798() >= firstrunscreen$hit.x
                    && event.comp_4798() <= firstrunscreen$hit.x + firstrunscreen$hit.w
                    && event.comp_4799() >= firstrunscreen$hit.y
                    && event.comp_4799() <= firstrunscreen$hit.y + firstrunscreen$hit.h) {
                    firstrunscreen$hit.act.run();
                    return true;
                }
            }

            return super.mouseClicked(event, doubled);
        }
    }

    private record Hit(float x, float y, float w, float h, Runnable act) {
    }
}
