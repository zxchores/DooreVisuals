package dev.doorevisuals.ui;

import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.SmoothText;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.session.SessionSwitch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Offline accounts. Same look as the main menu and the same way of drawing: shapes go through the distance-field path and
 * text through the vanilla text renderer, because a NanoVG frame on a full-screen menu made it blink. The panel eases in,
 * rows react to the mouse, the list scrolls smoothly, and everything has a shortcut: double click or Enter logs in, the
 * arrows move the selection, Delete removes, Esc goes back.
 */
public final class AltManagerScreen extends Screen {
    private static final Identifier FONT_ID = Identifier.of("doorevisuals", "inter");
    private static final Identifier FONT_JSON = Identifier.of("doorevisuals", "font/inter.json");
    private static final Style INTER = Style.EMPTY.withFont(new StyleSpriteSource.Font(FONT_ID));
    private static final String[] ADJECTIVES = {"Swift", "Silent", "Neon", "Frost", "Lunar", "Pixel", "Amber", "Cosmic", "Crimson", "Misty", "Brave", "Ghost"};
    private static final String[] NOUNS = {"Fox", "Wolf", "Raven", "Comet", "Blade", "Echo", "Drift", "Spark", "Nova", "Tiger", "Shade", "Orbit"};

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Anim> hover = new HashMap<>();
    private final Map<String, Anim> pick = new HashMap<>();
    private final Anim open = new Anim(0.0F);
    private final Anim toast = new Anim(0.0F);

    private boolean customFont;
    private String selected = "";
    private String draft = "";
    private boolean inputFocus;
    private String status = "";
    private int statusRgb = 0x8C8898;
    private float scroll;
    private float scrollTarget;
    private float listHeight;
    private float listContent;
    private float s = 1.0F;
    private float dt = 0.016F;
    private long lastNanos = System.nanoTime();
    private int mx;
    private int my;
    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;
    /** Fade of the whole screen while it opens, folded into every colour. */
    private float fade = 1.0F;

    public AltManagerScreen(Screen parent) {
        super(Text.literal("Аккаунты"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltsStore.boot();
        MinecraftClient mc = MinecraftClient.getInstance();
        this.customFont = mc != null && mc.getResourceManager() != null && mc.getResourceManager().getResource(FONT_JSON).isPresent();
        this.selected = AltsStore.activeName();
        this.draft = "";
        this.inputFocus = false;
        this.say("Выберите аккаунт и нажмите «Войти»", 0x8C8898);
    }

    private void say(String text, int rgb) {
        this.status = text;
        this.statusRgb = rgb & 0xFFFFFF;
        this.toast.snap(0.0F);
    }

    /** Colour with alpha, multiplied by the screen fade. */
    private int c(int rgb, float a) {
        return Theme.alpha(rgb & 0xFFFFFF, Math.round(Math.max(0.0F, Math.min(1.0F, a * this.fade)) * 255.0F));
    }

    private static int accent() {
        return Theme.ACCENT & 0xFFFFFF;
    }

    private static int accentHot() {
        return Theme.ACCENT_HOT & 0xFFFFFF;
    }

    private static int mix(int a, int b, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int r = Math.round(((a >> 16) & 255) * (1.0F - k) + ((b >> 16) & 255) * k);
        int g = Math.round(((a >> 8) & 255) * (1.0F - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1.0F - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }

    private Anim anim(Map<String, Anim> map, String key) {
        return map.computeIfAbsent(key, k -> new Anim(0.0F));
    }

    // ---------------------------------------------------------------- text (vanilla renderer, scaled to a size)

    private Text styled(String t) {
        return this.customFont ? Text.literal(t).setStyle(INTER) : Text.literal(t);
    }

    /** The em size of the soft font that looks as big as a text of the given size did in the old renderer. */
    private static float em(float size) {
        return size * 1.18F;
    }

    private float tw(String t, float size) {
        if (SmoothText.ready()) {
            return SmoothText.width(t, em(size));
        }
        return MinecraftClient.getInstance().textRenderer.getWidth(this.styled(t)) * size / 7.0F;
    }

    /** Text with its top-left corner at (x, y); size is the same scale the old NanoVG text used. */
    private void text(DrawContext g, String t, float x, float y, int argb, float size) {
        if (SmoothText.ready()) {
            SmoothText.draw(g, t, x, y, argb, em(size));
            return;
        }
        float scale = size / 7.0F;
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(Math.round(x), Math.round(y));
        g.getMatrices().scale(scale, scale);
        g.drawText(MinecraftClient.getInstance().textRenderer, this.styled(t), 0, 0, argb, false);
        g.getMatrices().popMatrix();
    }

    private void textC(DrawContext g, String t, float cx, float y, int argb, float size) {
        this.text(g, t, cx - this.tw(t, size) / 2.0F, y, argb, size);
    }

    /** Offset that centres a line of text of the given size in a box of height h. */
    private float vc(float h, float size) {
        return (h - (SmoothText.ready() ? SmoothText.height(em(size)) : size * 1.14F)) / 2.0F;
    }

    // ---------------------------------------------------------------- frame

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        long now = System.nanoTime();
        this.dt = Math.max(0.004F, Math.min(0.033F, (now - this.lastNanos) / 1.0E9F));
        this.lastNanos = now;
        this.s = Math.max(0.75F, Math.min(1.15F, Math.min(this.width / 520.0F, this.height / 400.0F)));

        float k = Math.max(0.0F, Math.min(1.0F, this.open.springOpen(1.0F, this.dt)));
        this.fade = k;

        Paint.grad(g, 0, 0, this.width, this.height * 0.5F, Theme.alpha(0x07070C, 255), Theme.alpha(0x14121E, 255), 0.0F, true);
        Paint.grad(g, 0, this.height * 0.5F, this.width, this.height * 0.5F, Theme.alpha(0x14121E, 255), Theme.alpha(0x07070C, 255), 0.0F, true);

        this.panelW = Math.min(280.0F * this.s, this.width - 24.0F);
        this.panelH = Math.min(262.0F * this.s, this.height - 24.0F);
        this.panelX = (this.width - this.panelW) / 2.0F;
        this.panelY = (this.height - this.panelH) / 2.0F + (1.0F - k) * 12.0F * this.s;

        float ease = 1.0F - (float)Math.exp(-this.dt * 14.0F);
        this.scrollTarget = Math.max(0.0F, Math.min(Math.max(0.0F, this.listContent - this.listHeight), this.scrollTarget));
        this.scroll += (this.scrollTarget - this.scroll) * ease;

        this.paintPanel(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void paintPanel(DrawContext g) {
        float s = this.s;
        float x = this.panelX;
        float y = this.panelY;
        float w = this.panelW;
        float h = this.panelH;
        Paint.box(g, x, y, w, h, this.c(0x0B0A12, 0.92F), 12.0F * s);
        Paint.outline(g, x, y, w, h, this.c(0xFFFFFF, 0.08F), 12.0F * s);

        // header
        this.text(g, "Аккаунты", x + 14.0F * s, y + 11.0F * s, this.c(0xF2F0F8, 1.0F), 8.6F * s);
        List<AltsStore.Alt> alts = AltsStore.all();
        this.text(g, "offline · " + alts.size(), x + 14.0F * s, y + 24.0F * s, this.c(0x8C8898, 1.0F), 5.2F * s);
        String active = AltsStore.activeName();
        if (!active.isBlank()) {
            String chip = "в игре: " + active;
            float cw = this.tw(chip, 5.2F * s) + 14.0F * s;
            float cx = x + w - cw - 12.0F * s;
            Paint.box(g, cx, y + 12.0F * s, cw, 15.0F * s, this.c(accent(), 0.22F), 7.5F * s);
            this.textC(g, chip, cx + cw / 2.0F, y + 12.0F * s + vc(15.0F * s, 5.2F * s), this.c(mix(accentHot(), 0xFFFFFF, 0.3F), 1.0F), 5.2F * s);
        }

        // list
        float listX = x + 10.0F * s;
        float listY = y + 38.0F * s;
        float listW = w - 20.0F * s;
        this.listHeight = h - 38.0F * s - 82.0F * s;
        Paint.box(g, listX, listY, listW, this.listHeight, this.c(0x050508, 0.6F), 9.0F * s);

        float rowH = 24.0F * s;
        float gap = 3.0F * s;
        this.listContent = alts.size() * (rowH + gap) + 4.0F * s;
        g.enableScissor(Math.round(listX), Math.round(listY), Math.round(listX + listW), Math.round(listY + this.listHeight));
        float ry = listY + 3.0F * s - this.scroll;
        for (AltsStore.Alt alt : alts) {
            if (ry + rowH >= listY && ry <= listY + this.listHeight) {
                this.paintRow(g, alt, listX + 3.0F * s, ry, listW - 6.0F * s, rowH, listY, active);
            }
            ry += rowH + gap;
        }
        g.disableScissor();
        if (alts.isEmpty()) {
            this.textC(g, "Здесь пока пусто", listX + listW / 2.0F, listY + this.listHeight / 2.0F - 10.0F * s, this.c(0xF2F0F8, 0.9F), 6.4F * s);
            this.textC(g, "Введите ник ниже и нажмите «Добавить»", listX + listW / 2.0F, listY + this.listHeight / 2.0F + 3.0F * s, this.c(0x8C8898, 1.0F), 4.8F * s);
        }
        if (this.listContent > this.listHeight + 2.0F) {
            float bar = Math.max(18.0F * s, this.listHeight * this.listHeight / this.listContent);
            float range = Math.max(1.0F, this.listContent - this.listHeight);
            float by = listY + 3.0F * s + (this.listHeight - 6.0F * s - bar) * Math.max(0.0F, Math.min(1.0F, this.scroll / range));
            Paint.box(g, listX + listW - 5.0F * s, by, 2.4F * s, bar, this.c(accentHot(), 0.55F), 1.2F * s);
        }

        // input row
        float iy = y + h - 74.0F * s;
        float dice = 22.0F * s;
        float add = 58.0F * s;
        float fw = w - 20.0F * s - dice - add - 12.0F * s;
        boolean fieldHot = Paint.hit(this.mx, this.my, listX, iy, fw, 22.0F * s);
        Paint.box(g, listX, iy, fw, 22.0F * s, this.c(0x050508, 0.85F), 7.0F * s);
        Paint.outline(g, listX, iy, fw, 22.0F * s, this.inputFocus ? this.c(accentHot(), 0.9F) : this.c(0xFFFFFF, fieldHot ? 0.2F : 0.1F), 7.0F * s);
        boolean caret = this.inputFocus && (System.currentTimeMillis() / 500L) % 2L == 0L;
        String text = this.draft.isEmpty() && !this.inputFocus ? "Новый ник…" : this.draft + (caret ? "|" : "");
        this.text(g, text, listX + 9.0F * s, iy + vc(22.0F * s, 5.8F * s), this.c(this.draft.isEmpty() && !this.inputFocus ? 0x6E6A7C : 0xF2F0F8, 1.0F), 5.8F * s);
        this.hits.add(new Hit(listX, iy, fw, 22.0F * s, () -> this.inputFocus = true, null));
        this.button(g, "dice", listX + fw + 6.0F * s, iy, dice, 22.0F * s, "↻", false, this::randomNick);
        this.button(g, "add", listX + fw + 12.0F * s + dice, iy, add, 22.0F * s, "Добавить", false, this::addAlt);

        // bottom actions
        float by = y + h - 46.0F * s;
        float half = (w - 20.0F * s - 8.0F * s) / 2.0F;
        this.button(g, "login", listX, by, half, 24.0F * s, "Войти", true, () -> this.login(null));
        this.button(g, "back", listX + half + 8.0F * s, by, half, 24.0F * s, "Назад", false, () -> this.client.setScreen(this.parent));

        // status line, fading in
        float t = Math.max(0.0F, Math.min(1.0F, this.toast.springHover(1.0F, this.dt)));
        this.textC(g, this.status, x + w / 2.0F, y + h - 16.0F * s, this.c(this.statusRgb, 0.25F + 0.75F * t), 5.2F * s);
    }

    private void paintRow(DrawContext g, AltsStore.Alt alt, float x, float y, float w, float h, float clipTop, String active) {
        float s = this.s;
        String name = alt.name();
        boolean on = name.equalsIgnoreCase(this.selected);
        boolean isActive = name.equalsIgnoreCase(active);
        boolean inside = this.my >= clipTop && this.my <= clipTop + this.listHeight;
        boolean hot = inside && Paint.hit(this.mx, this.my, x, y, w, h);
        float hv = this.anim(this.hover, name).springHover(hot ? 1.0F : 0.0F, this.dt);
        float sv = this.anim(this.pick, name).springToggle(on ? 1.0F : 0.0F, this.dt);

        Paint.box(g, x, y, w, h, this.c(0xFFFFFF, 0.04F * hv), 7.0F * s);
        if (sv > 0.01F) {
            Paint.box(g, x, y, w, h, this.c(accent(), 0.20F * sv), 7.0F * s);
            Paint.outline(g, x, y, w, h, this.c(accentHot(), 0.55F * sv), 7.0F * s);
        }

        // skin face: base layer and hat layer of the 64x64 skin, drawn in line with everything else
        float face = 16.0F * s;
        try {
            SkinTextures skin = DefaultSkinHelper.getSkinTextures(alt.uuid());
            Identifier tex = skin.body().texturePath();
            int argb = (Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, this.fade))) << 24) | 0xFFFFFF;
            int fx = Math.round(x + 7.0F * s);
            int fy = Math.round(y + (h - face) / 2.0F);
            int size = Math.round(face);
            g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, fx, fy, 8.0F, 8.0F, size, size, 8, 8, 64, 64, argb);
            g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, fx, fy, 40.0F, 8.0F, size, size, 8, 8, 64, 64, argb);
        } catch (Throwable ignored) {
            // a missing texture only costs the face
        }

        float textX = x + 31.0F * s;
        int nameColor = this.c(mix(0xB8B5C6, 0xFFFFFF, Math.max(sv, hv)), 1.0F);
        if (isActive) {
            this.text(g, name, textX, y + 3.0F * s, nameColor, 6.4F * s);
            this.text(g, "в игре", textX, y + h - 9.0F * s, this.c(accentHot(), 0.9F), 4.4F * s);
        } else {
            this.text(g, name, textX, y + vc(h, 6.4F * s), nameColor, 6.4F * s);
        }

        // quick actions appear on hover or on the selected row
        float reveal = Math.max(hv, sv * 0.9F);
        float top = Math.max(y, clipTop);
        float bottom = Math.min(y + h, clipTop + this.listHeight);
        if (reveal > 0.02F) {
            float bw = 40.0F * s;
            float bh = 16.0F * s;
            float bx = x + w - bw - 26.0F * s;
            float by = y + (h - bh) / 2.0F;
            boolean inHot = hot && Paint.hit(this.mx, this.my, bx, by, bw, bh);
            Paint.box(g, bx, by, bw, bh, this.c(inHot ? accentHot() : accent(), (inHot ? 0.95F : 0.55F) * reveal), 5.0F * s);
            this.textC(g, "Войти", bx + bw / 2.0F, by + vc(bh, 5.0F * s), this.c(0xFFFFFF, reveal), 5.0F * s);
            float xs = 18.0F * s;
            float xx = x + w - xs - 4.0F * s;
            boolean xHot = hot && Paint.hit(this.mx, this.my, xx, by, xs, bh);
            Paint.box(g, xx, by, xs, bh, this.c(xHot ? 0xFF5C7A : 0xFFFFFF, (xHot ? 0.85F : 0.08F) * reveal), 5.0F * s);
            this.textC(g, "×", xx + xs / 2.0F, by + vc(bh, 6.4F * s), this.c(0xFFFFFF, reveal), 6.4F * s);
            if (bottom > top) {
                float hy = Math.max(by, top);
                float hh = Math.min(by + bh, bottom) - hy;
                if (hh > 0.0F) {
                    this.hits.add(new Hit(bx, hy, bw, hh, () -> this.login(name), null));
                    this.hits.add(new Hit(xx, hy, xs, hh, () -> this.removeAlt(name), null));
                }
            }
        }
        if (bottom > top) {
            // first in the list, so the action buttons added after it win the click
            this.hits.add(0, new Hit(x, top, w, bottom - top, () -> {
                this.selected = name;
                this.inputFocus = false;
                GuiSfx.click();
            }, () -> this.login(name)));
        }
    }

    private void button(DrawContext g, String key, float x, float y, float w, float h, String label, boolean primary, Runnable action) {
        float s = this.s;
        boolean hot = Paint.hit(this.mx, this.my, x, y, w, h);
        float hv = this.anim(this.hover, "btn" + key).springHover(hot ? 1.0F : 0.0F, this.dt);
        int fill = primary ? mix(accent(), accentHot(), hv) : mix(0x1A1824, 0x2A2638, hv);
        Paint.box(g, x, y, w, h, this.c(fill, primary ? 0.95F : 0.9F), 7.0F * s);
        Paint.outline(g, x, y, w, h, this.c(primary ? 0xFFFFFF : accentHot(), primary ? 0.18F : 0.12F + 0.5F * hv), 7.0F * s);
        this.textC(g, label, x + w / 2.0F, y + vc(h, 5.8F * s), this.c(0xFFFFFF, 1.0F), 5.8F * s);
        this.hits.add(new Hit(x, y, w, h, () -> {
            GuiSfx.click();
            action.run();
        }, null));
    }

    // ---------------------------------------------------------------- actions

    private void addAlt() {
        this.inputFocus = false;
        String nick = this.draft.trim();
        if (nick.isEmpty()) {
            this.say("Введите ник", Theme.WARN);
        } else if (!AltsStore.add(nick)) {
            this.say("Ник занят или неверный (до 16 символов: буквы, цифры, _)", Theme.WARN);
        } else {
            this.selected = nick;
            this.draft = "";
            this.say("Добавлен: " + nick, Theme.OK);
            this.scrollTo(nick);
        }
    }

    private void randomNick() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        String nick;
        int tries = 0;
        do {
            nick = ADJECTIVES[r.nextInt(ADJECTIVES.length)] + NOUNS[r.nextInt(NOUNS.length)] + r.nextInt(10, 100);
            tries++;
        } while (AltsStore.find(nick) != null && tries < 10);
        this.draft = nick.length() > 16 ? nick.substring(0, 16) : nick;
        this.inputFocus = true;
    }

    /** Logs in as the given nick, or as the selected one (or the one being typed) when null. */
    private void login(String nick) {
        this.inputFocus = false;
        String name = nick != null ? nick : (this.selected.isBlank() ? this.draft : this.selected);
        if (name == null || name.isBlank()) {
            this.say("Сначала выберите аккаунт", Theme.WARN);
            return;
        }
        AltsStore.add(name);
        if (SessionSwitch.applyOffline(name)) {
            this.selected = AltsStore.activeName();
            this.say("Вы вошли как " + this.selected, Theme.OK);
        } else {
            this.say("Не удалось сменить сессию", Theme.BAD);
        }
    }

    private void removeAlt(String nick) {
        this.inputFocus = false;
        String name = nick != null ? nick : this.selected;
        if (name == null || name.isBlank()) {
            this.say("Нечего удалять", Theme.WARN);
        } else if (!AltsStore.remove(name)) {
            this.say("Аккаунт не найден", Theme.WARN);
        } else {
            this.selected = AltsStore.activeName();
            this.say("Удалён: " + name, 0x8C8898);
        }
    }

    private void move(int delta) {
        List<AltsStore.Alt> alts = AltsStore.all();
        if (alts.isEmpty()) {
            return;
        }
        int index = -1;
        for (int i = 0; i < alts.size(); i++) {
            if (alts.get(i).name().equalsIgnoreCase(this.selected)) {
                index = i;
            }
        }
        index = Math.max(0, Math.min(alts.size() - 1, index + delta));
        this.selected = alts.get(index).name();
        this.scrollTo(this.selected);
    }

    /** Scrolls the list just enough to show the nick. */
    private void scrollTo(String nick) {
        List<AltsStore.Alt> alts = AltsStore.all();
        float rowH = (24.0F + 3.0F) * this.s;
        for (int i = 0; i < alts.size(); i++) {
            if (alts.get(i).name().equalsIgnoreCase(nick)) {
                float top = i * rowH;
                if (top < this.scrollTarget) {
                    this.scrollTarget = top;
                } else if (top + rowH > this.scrollTarget + this.listHeight - 8.0F * this.s) {
                    this.scrollTarget = top + rowH - this.listHeight + 8.0F * this.s;
                }
                return;
            }
        }
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        int button = event.button();
        if (button > 1) {
            return super.mouseClicked(event, doubled);
        }
        boolean handled = false;
        for (int i = this.hits.size() - 1; i >= 0; i--) {
            Hit hit = this.hits.get(i);
            if (event.x() >= hit.x && event.x() <= hit.x + hit.w && event.y() >= hit.y && event.y() <= hit.y + hit.h) {
                Runnable action = button == 0 ? hit.left : hit.right;
                if (action != null) {
                    action.run();
                    // a double click on a row logs in as well
                    if (button == 0 && doubled && hit.right != null) {
                        hit.right.run();
                    }
                    handled = true;
                    break;
                }
            }
        }
        if (!handled) {
            this.inputFocus = false;
        }
        return handled || super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        this.scrollTarget -= (float)(v * 30.0 * this.s);
        return true;
    }

    @Override
    public boolean charTyped(CharInput event) {
        if (!this.inputFocus) {
            return super.charTyped(event);
        }
        char ch = (char)event.codepoint();
        boolean allowed = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_';
        if (allowed && this.draft.length() < 16) {
            this.draft = this.draft + ch;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        int key = event.key();
        if (key == 256) {
            if (this.inputFocus) {
                this.inputFocus = false;
            } else {
                this.client.setScreen(this.parent);
            }
            return true;
        }
        if (this.inputFocus) {
            if (key == 259 && !this.draft.isEmpty()) {
                this.draft = this.draft.substring(0, this.draft.length() - 1);
            } else if (key == 257 || key == 335) {
                this.addAlt();
            } else if (event.isPaste() && this.draft.length() < 16) {
                try {
                    String clip = this.client.keyboard.getClipboard();
                    if (clip != null) {
                        String clean = clip.replaceAll("[^A-Za-z0-9_]", "");
                        String joined = this.draft + clean;
                        this.draft = joined.substring(0, Math.min(16, joined.length()));
                    }
                } catch (Throwable ignored) {
                    // clipboard unavailable
                }
            }
            return true;
        }
        if (key == 265) {
            this.move(-1);
            return true;
        }
        if (key == 264) {
            this.move(1);
            return true;
        }
        if (key == 257 || key == 335) {
            this.login(null);
            return true;
        }
        if (key == 261) {
            this.removeAlt(null);
            return true;
        }
        if (event.isPaste()) {
            this.inputFocus = true;
            return this.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    private static final class Hit {
        final float x;
        final float y;
        final float w;
        final float h;
        final Runnable left;
        final Runnable right;

        Hit(float x, float y, float w, float h, Runnable left, Runnable right) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.left = left;
            this.right = right;
        }
    }
}
