package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.tools.ConfigFeature;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/** Simple config manager: type a name and save, pick a config to load or delete, open the folder. */
public final class ConfigManagerScreen extends Screen {
    private static final int IDLE_TEXT = 0xFF8E8C99;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final Anim openAnim = new Anim(0.0F);
    private final long openedAt = System.currentTimeMillis();

    private float s = 0.9F;
    private float dt = 0.016F;
    private long lastNanos = System.nanoTime();
    private int mx;
    private int my;
    private boolean closing;
    private boolean finished;
    private String name = "default";
    private String selected = "";
    private String status = "";
    private long confirmUntil;
    private float scroll;
    private float maxScroll;

    public ConfigManagerScreen(Screen parent) {
        super(Text.literal("Конфиги"));
        this.parent = parent;
    }

    private ConfigFeature cfg() {
        return App.features().find(ConfigFeature.class).orElse(null);
    }

    @Override
    protected void init() {
        super.init();
        ConfigFeature cfg = this.cfg();
        if (cfg != null) {
            this.name = cfg.draftName();
        }
        this.selected = ConfigFeature.activeName();
        this.refresh();
    }

    private void refresh() {
        this.names.clear();
        ConfigFeature cfg = this.cfg();
        if (cfg != null) {
            this.names.addAll(cfg.list());
        }
    }

    private int body() {
        return Theme.alpha(Theme.lerp(Theme.VOID | 0xFF000000, Theme.ACCENT_DEEP | 0xFF000000, 0.08F), 225);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        if (!App.live()) {
            return;
        }
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        long now = System.nanoTime();
        this.dt = Math.max(0.004166667F, Math.min(0.033333335F, (float)(now - this.lastNanos) / 1.0E9F));
        this.lastNanos = now;
        Theme.tickTransition(this.dt);
        this.s = Math.max(0.6F, Math.min(1.1F, Math.min(this.width / 400.0F, this.height / 440.0F)));

        boolean opening = !this.closing && System.currentTimeMillis() - this.openedAt > 20L;
        float a = opening ? this.openAnim.springOpen(1.0F, this.dt) : this.openAnim.springOpen(0.0F, this.dt);
        if (this.closing && a < 0.04F) {
            this.finish();
            return;
        }
        Ui.frame(g, () -> {
            Ui.alpha(Math.max(0.0F, Math.min(1.0F, a)));
            this.drawPanel(g);
            Ui.alpha(1.0F);
        });
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext g) {
        float s = this.s;
        float w = 360 * s;
        float h = 400 * s;
        float x = (this.width - w) / 2.0F;
        float y = (this.height - h) / 2.0F;
        Paint.box(g, x, y, w, h, this.body(), 12 * s);
        Paint.textC(g, "Конфиги", x + w / 2, y + 14 * s, Theme.TEXT, 9.0F * s);
        this.button(g, "Шейдеры Iris", x + w - 100 * s - 12 * s, y + 10 * s, 100 * s, 20 * s, true, false, () -> {
            if (this.client != null) {
                this.client.setScreen(new IrisShaderCatalogScreen(this));
            }
        });

        ConfigFeature cfg = this.cfg();
        if (cfg == null) {
            Paint.textC(g, "Модуль конфигов недоступен", x + w / 2, y + h / 2, IDLE_TEXT, 7.0F * s);
            return;
        }
        Paint.textC(g, "Активный: " + ConfigFeature.activeName(), x + w / 2, y + 32 * s, IDLE_TEXT, 6.2F * s);

        float pad = 16 * s;
        float fy = y + 52 * s;
        float fh = 26 * s;
        float saveW = 86 * s;
        float fieldW = w - pad * 2 - saveW - 8 * s;
        Paint.box(g, x + pad, fy, fieldW, fh, Theme.alpha(0xFFFFFF, 14), 8 * s);
        boolean caret = (System.currentTimeMillis() / 500L) % 2L == 0L;
        String shown = this.name.length() > 28 ? this.name.substring(this.name.length() - 28) : this.name;
        Paint.text(g, shown + (caret ? "|" : ""), x + pad + 10 * s, fy + (fh - 6.8F * s * 1.56F) / 2, Theme.TEXT, 6.8F * s);
        this.button(g, "Сохранить", x + pad + fieldW + 8 * s, fy, saveW, fh, true, true, this::save);

        float listY = fy + fh + 10 * s;
        float btnH = 26 * s;
        float footY = y + h - btnH - 36 * s;
        float listH = footY - listY - 8 * s;
        float rowH = 24 * s;
        float total = this.names.size() * (rowH + 3 * s);
        this.maxScroll = Math.min(0.0F, listH - total);
        this.scroll = Math.max(this.maxScroll, Math.min(0.0F, this.scroll));

        Paint.box(g, x + pad, listY, w - pad * 2, listH, Theme.alpha(0x000000, 60), 9 * s);
        Ui.scissor(x + pad, listY, w - pad * 2, listH);
        float cy = listY + 4 * s + this.scroll;
        String active = ConfigFeature.activeName();
        for (String n : this.names) {
            if (cy + rowH >= listY && cy <= listY + listH) {
                boolean sel = n.equalsIgnoreCase(this.selected);
                boolean hot = this.my >= listY && this.my <= listY + listH && Paint.hit(this.mx, this.my, x + pad + 4 * s, cy, w - pad * 2 - 8 * s, rowH);
                int fill = sel ? Theme.alpha(Theme.ACCENT, 70) : Theme.alpha(0xFFFFFF, hot ? 16 : 6);
                Paint.box(g, x + pad + 4 * s, cy, w - pad * 2 - 8 * s, rowH, fill, 7 * s);
                Paint.text(g, n, x + pad + 14 * s, cy + (rowH - 6.6F * s * 1.56F) / 2, sel ? Theme.ACCENT_HOT : Theme.TEXT, 6.6F * s);
                if (n.equalsIgnoreCase(active)) {
                    Paint.textR(g, "активный", x + w - pad - 14 * s, cy + (rowH - 5.4F * s * 1.56F) / 2, IDLE_TEXT, 5.4F * s);
                }
                float top = Math.max(cy, listY);
                float bottom = Math.min(cy + rowH, listY + listH);
                if (bottom > top) {
                    this.hits.add(new Hit(x + pad + 4 * s, top, w - pad * 2 - 8 * s, bottom - top, () -> {
                        this.selected = n;
                        GuiSfx.click();
                    }));
                }
            }
            cy += rowH + 3 * s;
        }
        Ui.unscissor();
        if (this.names.isEmpty()) {
            Paint.textC(g, "Пока нет сохранённых конфигов", x + w / 2, listY + listH / 2 - 5 * s, IDLE_TEXT, 6.4F * s);
        }

        boolean has = !this.selected.isBlank() && this.names.stream().anyMatch(n -> n.equalsIgnoreCase(this.selected));
        boolean confirm = System.currentTimeMillis() < this.confirmUntil;
        float bw = (w - pad * 2 - 2 * 6 * s) / 3.0F;
        this.button(g, "Загрузить", x + pad, footY, bw, btnH, has, true, this::load);
        this.button(g, confirm ? "Точно?" : "Удалить", x + pad + bw + 6 * s, footY, bw, btnH, has, confirm, this::delete);
        this.button(g, "Папка", x + pad + (bw + 6 * s) * 2, footY, bw, btnH, true, false, () -> {
            ConfigFeature c = this.cfg();
            if (c != null) {
                c.openConfigsFolder();
            }
        });

        Paint.textC(g, this.status.isEmpty() ? "Esc — назад" : this.status, x + w / 2, y + h - 22 * s, IDLE_TEXT, 5.8F * s);
    }

    private void button(DrawContext g, String label, float x, float y, float w, float h, boolean enabled, boolean accent, Runnable action) {
        float s = this.s;
        boolean hot = enabled && Paint.hit(this.mx, this.my, x, y, w, h);
        int fill = !enabled ? Theme.alpha(0xFFFFFF, 6) : accent ? Theme.alpha(Theme.ACCENT, hot ? 130 : 84) : Theme.alpha(0xFFFFFF, hot ? 26 : 14);
        Paint.box(g, x, y, w, h, fill, 8 * s);
        Paint.textC(g, label, x + w / 2, y + (h - 6.6F * s * 1.56F) / 2, enabled ? Theme.TEXT : 0xFF5A5866, 6.6F * s);
        if (enabled) {
            this.hits.add(new Hit(x, y, w, h, () -> {
                GuiSfx.click();
                action.run();
            }));
        }
    }

    private void save() {
        ConfigFeature cfg = this.cfg();
        if (cfg == null) {
            return;
        }
        cfg.setDraftName(this.name);
        boolean ok = cfg.saveDraft();
        if (ok) {
            this.selected = App.save().profiles().active();
            this.status = "Сохранён: " + this.selected;
        } else {
            String error = App.save().profiles().lastError();
            this.status = error.isEmpty() ? "Не удалось сохранить" : error;
        }
        this.refresh();
    }

    private void load() {
        ConfigFeature cfg = this.cfg();
        if (cfg == null || this.selected.isBlank()) {
            return;
        }
        this.status = cfg.applyNamed(this.selected) ? "Применён: " + this.selected : "Не найден: " + this.selected;
        this.refresh();
    }

    private void delete() {
        ConfigFeature cfg = this.cfg();
        if (cfg == null || this.selected.isBlank()) {
            return;
        }
        if (System.currentTimeMillis() >= this.confirmUntil) {
            this.confirmUntil = System.currentTimeMillis() + 2500L;
            return;
        }
        this.confirmUntil = 0L;
        String gone = this.selected;
        this.status = cfg.deleteNamed(gone) ? "Удалён: " + gone : "Не удалось удалить";
        this.selected = "";
        this.refresh();
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 0) {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                Hit hit = this.hits.get(i);
                if (Paint.hit(event.x(), event.y(), hit.x, hit.y, hit.w, hit.h)) {
                    hit.run.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        this.scroll = Math.max(this.maxScroll, Math.min(0.0F, this.scroll + (float)v * 26.0F * this.s));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        int key = event.key();
        if (key == 256) {
            this.requestClose();
            return true;
        }
        if (key == 257 || key == 335) {
            this.save();
            return true;
        }
        if (key == 259 && !this.name.isEmpty()) {
            this.name = this.name.substring(0, this.name.length() - 1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharInput event) {
        char c = (char)event.codepoint();
        if (c >= ' ' && this.name.length() < 32) {
            this.name = this.name + c;
            return true;
        }
        return super.charTyped(event);
    }

    public void requestClose() {
        if (!this.closing) {
            this.closing = true;
            GuiSfx.close();
        }
    }

    private void finish() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void close() {
        if (!this.finished) {
            if (!this.closing) {
                this.requestClose();
            } else {
                this.finish();
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class Hit {
        final float x;
        final float y;
        final float w;
        final float h;
        final Runnable run;

        Hit(float x, float y, float w, float h, Runnable run) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.run = run;
        }
    }
}
