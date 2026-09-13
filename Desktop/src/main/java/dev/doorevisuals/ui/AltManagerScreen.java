package dev.doorevisuals.ui;

import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.session.SessionSwitch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;

public final class AltManagerScreen extends Screen {
    private final Screen parent;
    private final List<AltManagerScreen.Hit> hits = new ArrayList<>();
    private String selected = "";
    private String draft = "";
    private boolean inputFocus;
    private String status = "";
    private int statusColor = Theme.MUTED;
    private float scroll;
    private int mx;
    private int my;

    public AltManagerScreen(Screen parent) {
        super(Text.literal("\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b"));
        this.parent = parent;
    }

    protected void init() {
        AltsStore.boot();
        this.selected = AltsStore.activeName();
        this.draft = "";
        this.inputFocus = false;
        this.status = "\u0412\u044b\u0431\u0435\u0440\u0438 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 \u2192 \u0412\u043e\u0439\u0442\u0438";
        this.statusColor = Theme.MUTED;
    }

    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, -16447480);

        for (int i = 0; i < 10; i++) {
            int j = 22 - i * 2;
            if (j <= 0) {
                break;
            }

            int k = i * Math.max(1, this.width / 24);
            g.fill(k, 0, k + Math.max(1, this.width / 24), this.height, Theme.alpha(Theme.ACCENT_DEEP, j));
        }
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        float f = Math.min(320, this.width - 48);
        float f1 = Math.min(280, this.height - 48);
        float f2 = (this.width - f) / 2.0F;
        float f3 = (this.height - f1) / 2.0F;
        Nvg.run(g, () -> this.paintUi(g, f2, f3, f, f1));
        float f4 = f3 + 42.0F;
        float f5 = f1 - 118.0F;
        List<AltsStore.Alt> list = AltsStore.all();
        int i = 24;
        float f6 = list.size() * i;
        float f7 = Math.max(0.0F, f6 - f5 + 6.0F);
        this.scroll = clamp(this.scroll, 0.0F, f7);
        int j = Math.round(f4 + 4.0F - this.scroll);

        for (AltsStore.Alt altsstore$alt : list) {
            if (!(j + i < f4) && !(j > f4 + f5)) {
                try {
                    SkinTextures skintextures = DefaultSkinHelper.getSkinTextures(altsstore$alt.uuid());
                    PlayerSkinDrawer.draw(g, skintextures, Math.round(f2 + 20.0F), j + 2, 16);
                } catch (Throwable throwable) {
                }

                j += i;
            } else {
                j += i;
            }
        }
    }

    private void paintUi(DrawContext g, float px, float py, float pw, float ph) {
        Paint.panelBody(g, px, py, pw, ph);
        Paint.text(g, "\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b", px + 14.0F, py + 10.0F, Theme.TEXT, 11.0F);
        Paint.text(g, "offline", px + 14.0F, py + 24.0F, Theme.MUTED, 7.0F);
        float f = py + 42.0F;
        float f1 = ph - 118.0F;
        Paint.box(g, px + 12.0F, f, pw - 24.0F, f1, Theme.alpha(Theme.VOID, 180), 4.0F);
        List<AltsStore.Alt> list = AltsStore.all();
        int i = 24;
        float f2 = list.size() * i;
        float f3 = Math.max(0.0F, f2 - f1 + 6.0F);
        this.scroll = clamp(this.scroll, 0.0F, f3);
        float f4 = f + 4.0F - this.scroll;
        String s = AltsStore.activeName();

        for (AltsStore.Alt altsstore$alt : list) {
            if (!(f4 + i < f) && !(f4 > f + f1)) {
                boolean flag = altsstore$alt.name().equalsIgnoreCase(this.selected);
                boolean flag1 = altsstore$alt.name().equalsIgnoreCase(s);
                boolean flag2 = this.mx >= px + 16.0F && this.mx <= px + pw - 16.0F && this.my >= f4 && this.my <= f4 + i - 2.0F;
                int j = flag ? Theme.alpha(Theme.ACCENT_DEEP, 230) : (flag2 ? Theme.alpha(Theme.ROW, 210) : Theme.alpha(Theme.ROW, 100));
                Paint.box(g, px + 14.0F, f4, pw - 28.0F, (float)(i - 3), j, 3.0F);
                if (flag) {
                    Paint.box(g, px + 14.0F, f4, 2.0F, (float)(i - 3), Theme.ACCENT, 0.0F);
                }

                Paint.text(g, altsstore$alt.name(), px + 42.0F, f4 + 5.0F, Theme.TEXT, 8.0F);
                if (flag1) {
                    Paint.text(g, "\u00b7", px + pw - 26.0F, f4 + 5.0F, Theme.ACCENT_HOT, 8.0F);
                }

                String s1 = altsstore$alt.name();
                this.hits.add(new AltManagerScreen.Hit(px + 14.0F, f4, pw - 28.0F, (float)(i - 3), () -> {
                    this.selected = s1;
                    this.status = s1;
                    this.statusColor = Theme.MUTED;
                }));
                f4 += i;
            } else {
                f4 += i;
            }
        }

        if (list.isEmpty()) {
            Paint.textC(
                g, "\u0434\u043e\u0431\u0430\u0432\u044c \u043d\u0438\u043a \u043d\u0438\u0436\u0435", px + pw * 0.5F, f + f1 * 0.5F - 4.0F, Theme.GHOST, 8.0F
            );
        }

        float f5 = py + ph - 62.0F;
        Paint.box(g, px + 12.0F, f5, pw - 24.0F, 20.0F, Theme.alpha(Theme.VOID, 190), 3.0F);
        if (this.inputFocus) {
            Paint.outline(g, px + 12.0F, f5, pw - 24.0F, 20.0F, Theme.alpha(Theme.ACCENT, 180), 3.0F);
        }

        String s2 = this.draft.isEmpty() && !this.inputFocus ? "\u043d\u0438\u043a\u2026" : this.draft + (this.inputFocus ? "|" : "");
        Paint.text(g, s2, px + 18.0F, f5 + 5.0F, this.draft.isEmpty() && !this.inputFocus ? Theme.GHOST : Theme.TEXT, 8.0F);
        this.hits.add(new AltManagerScreen.Hit(px + 12.0F, f5, pw - 24.0F, 20.0F, () -> this.inputFocus = true));
        float f6 = py + ph - 34.0F;
        float f7 = (pw - 24.0F - 12.0F) / 4.0F;
        this.action(g, px + 12.0F, f6, f7, "+", this::addAlt);
        this.action(g, px + 12.0F + (f7 + 4.0F), f6, f7, "\u0412\u043e\u0439\u0442\u0438", this::login);
        this.action(g, px + 12.0F + 2.0F * (f7 + 4.0F), f6, f7, "x", this::removeAlt);
        this.action(g, px + 12.0F + 3.0F * (f7 + 4.0F), f6, f7, "<", () -> this.client.setScreen(this.parent));
        Paint.textC(g, this.status, px + pw * 0.5F, py + ph - 14.0F, this.statusColor, 7.0F);
    }

    private void action(DrawContext g, float x, float y, float w, String label, Runnable act) {
        boolean flag = this.mx >= x && this.mx <= x + w && this.my >= y && this.my <= y + 18.0F;
        Paint.box(g, x, y, w, 18.0F, flag ? Theme.alpha(Theme.PANEL_HI, 245) : Theme.alpha(Theme.HEADER, 220), 3.0F);
        if (flag) {
            Paint.outline(g, x, y, w, 18.0F, Theme.alpha(Theme.ACCENT, 140), 3.0F);
        }

        Paint.textC(g, label, x + w * 0.5F, y + 4.5F, flag ? Theme.ACCENT_HOT : Theme.TEXT, 8.0F);
        this.hits.add(new AltManagerScreen.Hit(x, y, w, 18.0F, act));
    }

    private void addAlt() {
        this.inputFocus = false;
        if (!AltsStore.add(this.draft)) {
            this.status = "\u043d\u0438\u043a \u0437\u0430\u043d\u044f\u0442 / \u043d\u0435\u0432\u0435\u0440\u043d\u044b\u0439";
            this.statusColor = Theme.WARN;
        } else {
            this.selected = this.draft.trim();
            this.draft = "";
            this.status = "\u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d";
            this.statusColor = Theme.OK;
        }
    }

    private void login() {
        this.inputFocus = false;
        String s = this.selected.isBlank() ? this.draft : this.selected;
        if (s.isBlank()) {
            this.status = "\u0432\u044b\u0431\u0435\u0440\u0438 \u043d\u0438\u043a";
            this.statusColor = Theme.WARN;
        } else {
            AltsStore.add(s);
            if (SessionSwitch.applyOffline(s)) {
                this.selected = AltsStore.activeName();
                this.status = "\u0432\u043e\u0439\u0442\u0438: " + this.selected;
                this.statusColor = Theme.OK;
            } else {
                this.status = "\u043e\u0448\u0438\u0431\u043a\u0430 \u0441\u0435\u0441\u0441\u0438\u0438";
                this.statusColor = Theme.BAD;
            }
        }
    }

    private void removeAlt() {
        this.inputFocus = false;
        if (this.selected.isBlank()) {
            this.status = "\u043d\u0435\u0447\u0435\u0433\u043e \u0443\u0434\u0430\u043b\u044f\u0442\u044c";
            this.statusColor = Theme.WARN;
        } else {
            String s = this.selected;
            if (!AltsStore.remove(s)) {
                this.status = "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d";
                this.statusColor = Theme.WARN;
            } else {
                this.selected = AltsStore.activeName();
                this.status = "\u0443\u0434\u0430\u043b\u0451\u043d";
                this.statusColor = Theme.MUTED;
            }
        }
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        } else {
            boolean flag = false;

            for (int i = this.hits.size() - 1; i >= 0; i--) {
                AltManagerScreen.Hit altmanagerscreen$hit = this.hits.get(i);
                if (event.comp_4798() >= altmanagerscreen$hit.x
                    && event.comp_4798() <= altmanagerscreen$hit.x + altmanagerscreen$hit.w
                    && event.comp_4799() >= altmanagerscreen$hit.y
                    && event.comp_4799() <= altmanagerscreen$hit.y + altmanagerscreen$hit.h) {
                    altmanagerscreen$hit.action.run();
                    flag = true;
                    break;
                }
            }

            if (!flag) {
                this.inputFocus = false;
            }

            return flag || super.mouseClicked(event, doubled);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        this.scroll -= (float)(v * 18.0);
        return true;
    }

    public boolean charTyped(CharInput event) {
        if (!this.inputFocus) {
            return super.charTyped(event);
        } else {
            char c0 = (char)event.comp_4793();
            if (c0 < ' ' || c0 == 127 || this.draft.length() >= 16 || (c0 < 'A' || c0 > 'Z') && (c0 < 'a' || c0 > 'z') && (c0 < '0' || c0 > '9') && c0 != '_') {
                return true;
            } else {
                this.draft = this.draft + c0;
                return true;
            }
        }
    }

    public boolean keyPressed(KeyInput event) {
        int i = event.comp_4795();
        if (i == 256) {
            this.client.setScreen(this.parent);
            return true;
        } else if (!this.inputFocus) {
            if (i != 257 && i != 335) {
                return super.keyPressed(event);
            } else {
                this.login();
                return true;
            }
        } else if (i == 259 && !this.draft.isEmpty()) {
            this.draft = this.draft.substring(0, this.draft.length() - 1);
            return true;
        } else if (i != 257 && i != 335) {
            if (event.isPaste() && this.draft.length() < 16) {
                try {
                    String s = this.client.keyboard.getClipboard();
                    if (s != null) {
                        String s1 = s.replaceAll("[^A-Za-z0-9_]", "");
                        if (!s1.isEmpty()) {
                            String s2 = this.draft + s1;
                            this.draft = s2.substring(0, Math.min(16, s2.length()));
                        }
                    }
                } catch (Throwable throwable) {
                }

                return true;
            } else {
                return true;
            }
        } else {
            this.addAlt();
            return true;
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private record Hit(float x, float y, float w, float h, Runnable action) {
    }
}
