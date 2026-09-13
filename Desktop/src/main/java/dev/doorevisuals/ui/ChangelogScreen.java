package dev.doorevisuals.ui;

import dev.doorevisuals.data.Changelog;
import dev.doorevisuals.data.ChangelogSeen;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class ChangelogScreen extends Screen {
    private final Screen parent;
    private final List<Changelog.Entry> entries = Changelog.load();
    private final List<ChangelogScreen.Hit> hits = new ArrayList<>();
    private int mx;
    private int my;

    public ChangelogScreen(Screen parent) {
        super(Text.literal("Updates"));
        this.parent = parent;
    }

    protected void init() {
        ChangelogSeen.markSeen();
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        g.fill(0, 0, this.width, this.height, -872085496);
        Nvg.run(g, () -> this.paint(g));
    }

    private void paint(DrawContext g) {
        float f = Math.min(380, this.width - 40);
        float f1 = Math.min(340, this.height - 40);
        float f2 = (this.width - f) / 2.0F;
        float f3 = (this.height - f1) / 2.0F;
        Paint.panelBody(g, f2, f3, f, f1);
        Paint.text(g, "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f DooreVisuals", f2 + 16.0F, f3 + 14.0F, Theme.TEXT, 12.0F);
        Paint.text(
            g,
            "\u0427\u0442\u043e \u043d\u043e\u0432\u043e\u0433\u043e \u0432 \u0440\u0435\u043b\u0438\u0437\u0430\u0445",
            f2 + 16.0F,
            f3 + 30.0F,
            Theme.MUTED,
            8.0F
        );
        float f4 = f3 + 52.0F;
        if (this.entries.isEmpty()) {
            Paint.text(g, "changelog.json \u043f\u0443\u0441\u0442", f2 + 16.0F, f4, Theme.MUTED, 9.0F);
        } else {
            for (Changelog.Entry changelog$entry : this.entries) {
                if (f4 > f3 + f1 - 50.0F) {
                    break;
                }

                boolean flag = Changelog.isCurrent(changelog$entry.version());
                int i = flag ? Theme.ACCENT_HOT : Theme.TEXT;
                String s = "v" + changelog$entry.version() + (flag ? " \u00b7 NEW" : "") + " \u2014 " + changelog$entry.title();
                Paint.text(g, s, f2 + 16.0F, f4, i, 9.0F);
                f4 += 14.0F;

                for (String s1 : changelog$entry.bullets()) {
                    if (f4 > f3 + f1 - 50.0F) {
                        break;
                    }

                    Paint.text(g, "\u00b7 " + s1, f2 + 22.0F, f4, Theme.MUTED, 7.5F);
                    f4 += 11.0F;
                }

                f4 += 8.0F;
            }
        }

        this.action(g, f2 + f - 90.0F, f3 + f1 - 36.0F, 74.0F, 22.0F, "\u041d\u0430\u0437\u0430\u0434", () -> this.client.setScreen(this.parent));
    }

    private void action(DrawContext g, float x, float y, float w, float h, String lab, Runnable act) {
        boolean flag = this.mx >= x && this.mx <= x + w && this.my >= y && this.my <= y + h;
        Paint.box(g, x, y, w, h, Theme.alpha(Theme.ACCENT, flag ? 90 : 50), 4.0F);
        Paint.textC(g, lab, x + w * 0.5F, y + 5.0F, Theme.TEXT, 8.5F);
        this.hits.add(new ChangelogScreen.Hit(x, y, w, h, act));
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        } else {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                ChangelogScreen.Hit changelogscreen$hit = this.hits.get(i);
                if (event.x() >= changelogscreen$hit.x
                    && event.x() <= changelogscreen$hit.x + changelogscreen$hit.w
                    && event.y() >= changelogscreen$hit.y
                    && event.y() <= changelogscreen$hit.y + changelogscreen$hit.h) {
                    changelogscreen$hit.act.run();
                    return true;
                }
            }

            return super.mouseClicked(event, doubled);
        }
    }

    private record Hit(float x, float y, float w, float h, Runnable act) {
    }
}
