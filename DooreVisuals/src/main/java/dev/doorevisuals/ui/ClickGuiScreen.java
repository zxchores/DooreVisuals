package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.audio.GuiSfx;
import com.github.noamm9.nvgrenderer.nvg.Image;
import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.data.BannerLibrary;
import dev.doorevisuals.data.Folders;
import dev.doorevisuals.data.GifIO;
import dev.doorevisuals.data.GuiLayout;
import dev.doorevisuals.data.ThemeCodec;
import dev.doorevisuals.data.ThemesStore;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Keys;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.GuiWindowsFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * ClickGUI in the layout of the reference: a themes panel on the left, five module columns in the middle, a config
 * manager on the right and a search field under the columns. Module rows show the name on the left and three dots on
 * the right. Left click toggles a module, right click or the dots open its settings, the wheel scrolls a column.
 */
public final class ClickGuiScreen extends Screen implements MenuGui {
    /** Four main columns, left to right. The last one lists the player tools together with the misc modules. */
    private static final Category[][] COLS = {
            {Category.OVERLAY},
            {Category.WORLD},
            {Category.COSMETICS},
            {Category.TOOLS, Category.SYSTEM},
    };
    private static final String[] TITLES = {"Интерфейс", "Мир", "Косметика", "Игрок"};
    private static final String[] STYLE_NAMES = {"Блюр", "Чёрный", "Белый"};
    private static final int[] PALETTE = {
            0xFFFF5C7A, 0xFFFF8A4C, 0xFFFFC94D, 0xFFA3E635, 0xFF34D399, 0xFF22D3EE,
            0xFF60A5FA, 0xFF818CF8, 0xFFA78BFA, 0xFFD16BE0, 0xFFF472B6, 0xFFFFFFFF
    };

    // Panel palette of the chosen style (blur, black, white). Set once per frame in applyPalette().
    private int panelRgb = 0x0B0A12;
    private float panelA = 0.72F;
    private int subRgb = 0x15131C;
    private int cOn = 0xF2F0F8;
    private int cOff = 0x8C8898;
    private int focusCol;

    // Compact layout, in units of the scale factor.
    private static final float SIDE_W = 120.0F;
    private static final float COL_W = 124.0F;
    private static final float COL_GAP = 4.0F;
    private static final float FLY_W = 196.0F;
    private static final float GAP = 6.0F;
    private static final float BLOCK_H = 300.0F;
    private static final int MODE_NONE = 0;
    private static final int MODE_MODULES = 1;
    private static final int MODE_BANNER = 2;

    /** Which panels are slid out next to the sidebar: none, the five module columns, or the appearance flyout. */
    private int mode = MODE_NONE;
    /** Every plate slides out on its own: a click on its name in the sidebar opens or folds just that one. */
    private final Anim[] colAnim = {new Anim(0.0F), new Anim(0.0F), new Anim(0.0F), new Anim(0.0F)};
    private final boolean[] colOpen = new boolean[COLS.length];
    private final Anim banAnim = new Anim(0.0F);
    private final float[] colXs = new float[COLS.length];
    private float colWNow;
    private float colFade = 1.0F;
    private float flyX;
    private float flyW;
    private float flyScroll;
    private float flyTarget;
    private float flyContent;
    /** The own-theme editor: hue, saturation and brightness of the two colours; a slider being dragged in the flyout. */
    private final float[] hsvA = new float[3];
    private final float[] hsvB = new float[3];
    private FlyDrag flyDrag;
    /** Item and texture previews are drawn by the game's own renderer after the NanoVG frame, so they land on top of it. */
    private final List<Runnable> afterFrame = new ArrayList<>();
    private float flyView;
    private float flyTop;
    private float flyBottom;
    private String flyNote = "";

    private final List<Hit> hits = new ArrayList<>();
    private final Map<Category[], List<Feature>> moduleLists = new java.util.IdentityHashMap<>();
    private final Map<String, Anim> hover = new HashMap<>();
    private final Map<String, Anim> toggle = new HashMap<>();
    private final Map<String, Anim> expand = new HashMap<>();
    private final Map<Category, Float> scroll = new EnumMap<>(Category.class);
    private final Map<Category, Float> content = new EnumMap<>(Category.class);
    private final Map<Category, Float> scrollTarget = new EnumMap<>(Category.class);
    private final Set<String> open = new HashSet<>();
    /** 0..1 per module id: how strongly the row is lit by the current search. */
    private final Map<String, Anim> matchAnim = new HashMap<>();
    private final Anim searchAnim = new Anim(0.0F);
    private int matchCount;
    private final Anim openAnim = new Anim(0.0F);
    private final Anim dropAnim = new Anim(0.0F);
    /** Middle-click hit areas of module rows, rebuilt every frame. */
    private final List<Hit> midHits = new ArrayList<>();
    private Feature hoverFeature;
    /** Module waiting for a key to be bound by a middle click on its row. */
    private Feature bindFeature;
    private Opt.Pick dropShown;
    private float frameAlpha = 1.0F;
    private float glowX;
    private float glowY;
    private boolean glowReady;
    private boolean optHits;
    private final long openedAt = System.currentTimeMillis();

    private float s = 1.0F;
    private float dt = 0.016F;
    private long lastNanos = System.nanoTime();
    private int mx;
    private int my;
    private double clickX;
    private boolean closing;
    private boolean finished;
    private String search = "";
    private DragNum drag;
    private Opt.Key capture;
    private Feature captureOwner;
    private Opt.Pick dropPick;
    private Feature dropOwner;
    private float dropTop;
    private float dropBottom;
    private float dropLeft;
    private float dropWidth;
    private float boxX;
    private float boxY;
    private float boxW;
    private float boxH;

    /** Screen to return to when the menu closes. Null means back to the game. */
    private final Screen parent;

    public ClickGuiScreen() {
        this(null);
    }

    public ClickGuiScreen(Screen parent) {
        super(Text.literal("DooreVisuals"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        BannerLibrary.read(GuiLayout.bannerId());
        BannerLibrary.prepare();
        BannerLibrary.ensureLoaded();
        if (GuiWindowsFeature.soundOn()) {
            GuiSfx.open();
        }
    }

    // ---------- colour helpers (RGB in, ARGB out, so alpha is always explicit) ----------

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static int rgba(int rgb, float a) {
        return Theme.alpha(rgb & 0xFFFFFF, Math.round(clamp01(a) * 255.0F));
    }

    private static int mix(int a, int b, float t) {
        float k = clamp01(t);
        int r = Math.round(((a >> 16) & 255) * (1.0F - k) + ((b >> 16) & 255) * k);
        int g = Math.round(((a >> 8) & 255) * (1.0F - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1.0F - k) + (b & 255) * k);
        return r << 16 | g << 8 | bl;
    }

    private static int accent() {
        return Theme.ACCENT & 0xFFFFFF;
    }

    private static int accentHot() {
        return Theme.ACCENT_HOT & 0xFFFFFF;
    }

    private static int accentDeep() {
        return Theme.ACCENT_DEEP & 0xFFFFFF;
    }

    // ---------- frame ----------

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        if (!App.live()) {
            return;
        }
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        this.midHits.clear();
        this.afterFrame.clear();
        this.hoverFeature = null;
        long now = System.nanoTime();
        this.dt = Math.max(0.004166667F, Math.min(0.033333335F, (float)(now - this.lastNanos) / 1.0E9F));
        this.lastNanos = now;
        Theme.tickTransition(this.dt);

        // The fully open layout is 640 x 360 units; it never grows past 1:1, so it stays in the middle of the screen.
        this.s = Math.max(0.5F, Math.min(1.0F, Math.min((this.width - 24.0F) / 640.0F, (this.height - 30.0F) / 360.0F)));
        this.applyPalette();
        for (int i = 0; i < COLS.length; i++) {
            this.colAnim[i].springPanel(this.colOpen[i] ? 1.0F : 0.0F, this.dt);
        }
        this.banAnim.springPanel(this.mode == MODE_BANNER ? 1.0F : 0.0F, this.dt);

        boolean opening = !this.closing && System.currentTimeMillis() - this.openedAt > 20L;
        float a = opening ? this.openAnim.springOpen(1.0F, this.dt) : this.openAnim.springOpen(0.0F, this.dt);
        if (this.closing && a < 0.04F) {
            this.finish();
            return;
        }
        this.frameAlpha = clamp01(a);
        if (!this.glowReady) {
            this.glowX = mouseX;
            this.glowY = mouseY;
            this.glowReady = true;
        }
        float ease = 1.0F - (float)Math.exp(-this.dt * 9.0F);
        this.glowX += (mouseX - this.glowX) * ease;
        this.glowY += (mouseY - this.glowY) * ease;
        Ui.frame(g, () -> {
            Ui.alpha(this.frameAlpha);
            // Light dim only: the world stays visible behind the menu.
            int dim = GuiWindowsFeature.dimAlpha();
            Paint.box(g, 0, 0, this.width, this.height, rgba(0x000000, Math.max(0.12F, dim / 255.0F) * a), 0.0F);
            float[] r = this.geom();
            this.drawSidebar(g, r);
            this.drawFlyout(g, r);
            this.drawColumns(g, r);
            if (this.columnsOut() > 0.3F) {
                this.drawSearch(g, r);
            }
            this.drawTooltip(g);
            this.drawDropdown(g);
            Ui.alpha(1.0F);
        });
        for (Runnable r : this.afterFrame) {
            r.run();
        }
        super.render(g, mouseX, mouseY, delta);
    }

    /** How far the most open plate is slid out, 0..1. */
    private float columnsOut() {
        float best = 0.0F;
        for (Anim a : this.colAnim) {
            best = Math.max(best, clamp01(a.value()));
        }
        return best;
    }

    private void closeColumns() {
        java.util.Arrays.fill(this.colOpen, false);
    }

    private void openBanner() {
        this.closeColumns();
        this.mode = MODE_BANNER;
    }

    private void closeAll() {
        this.closeColumns();
        this.mode = MODE_NONE;
    }

    /** {sideX, sideY, sideW, sideH, colX0, colY, colW, colGap, colH, searchY}. The group is centred as a whole. */
    private float[] geom() {
        float s = this.s;
        float sideW = SIDE_W * s;
        float gap = GAP * s;
        float colW = COL_W * s;
        float colGap = COL_GAP * s;
        float sum = 0.0F;
        for (Anim a : this.colAnim) {
            sum += clamp01(a.value());
        }
        float modV = this.columnsOut();
        float modW = sum * (colW + colGap) - colGap * modV;
        float banV = clamp01(this.banAnim.value());
        float total = sideW + gap * modV + modW + (gap + FLY_W * s) * banV;
        float x0 = (this.width - total) / 2.0F;
        float blockH = Math.min(this.height - 56.0F * s, BLOCK_H * s);
        float y0 = Math.max(8.0F, (this.height - blockH - 26.0F * s) / 2.0F);
        return new float[] {x0, y0, sideW, blockH, x0 + sideW + gap, y0, colW, colGap, blockH, y0 + blockH + 6.0F * s};
    }

    private void applyPalette() {
        switch (GuiWindowsFeature.panelStyle()) {
            case 1 -> {
                this.panelRgb = 0x050508;
                this.panelA = 0.96F;
                this.subRgb = 0x111016;
                this.cOn = 0xF2F0F8;
                this.cOff = 0x8C8898;
            }
            case 2 -> {
                this.panelRgb = 0xEEECF4;
                this.panelA = 0.90F;
                this.subRgb = 0xFFFFFF;
                this.cOn = 0x1B1925;
                this.cOff = 0x6E6A7E;
            }
            default -> {
                this.panelRgb = 0x0B0A12;
                this.panelA = 0.72F;
                this.subRgb = 0x15131C;
                this.cOn = 0xF2F0F8;
                this.cOff = 0x8C8898;
            }
        }
    }

    private void panelBody(DrawContext g, float x, float y, float w, float h) {
        if (Theme.LIQUID) {
            Paint.liquid(g, x, y, w, h, rgba(accent(), 1.0F), 12.0F * this.s);
            return;
        }
        Paint.box(g, x, y, w, h, rgba(this.panelRgb, this.panelA), 12.0F * this.s);
        Paint.outline(g, x, y, w, h, rgba(this.cOn, 0.06F), 12.0F * this.s);
    }

    private void panelHeader(DrawContext g, float x, float y, float w, String title) {
        float s = this.s;
        float headH = 26.0F * s;
        boolean focus = TITLES[this.focusCol].equals(title);
        Paint.textC(g, title, x + w / 2.0F, y + (headH - 7.2F * s * 1.56F) / 2.0F + 2.0F * s, rgba(focus ? mix(this.cOn, accentHot(), 0.35F) : this.cOn, 1.0F), 7.2F * s);
        if (focus) {
            Paint.box(g, x + w / 2.0F - 11.0F * s, y + headH - 3.0F * s, 22.0F * s, 2.0F * s, rgba(accentHot(), 1.0F), 1.0F * s);
        }
    }

    // ---------- sidebar ----------

    private void drawSidebar(DrawContext g, float[] r) {
        float s = this.s;
        float x = r[0];
        float y = r[1];
        float w = r[2];
        float h = r[3];
        this.panelBody(g, x, y, w, h);
        float pad = 6.0F * s;
        float iw = w - pad * 2.0F;

        // Banner plate: your own picture, GIF or video. A click slides the appearance flyout out.
        float bh = 44.0F * s;
        float bx = x + pad;
        float by = y + pad;
        boolean bhot = Paint.hit(this.mx, this.my, bx, by, iw, bh);
        Image frame = BannerLibrary.currentFrame();
        if (frame != null) {
            Nvg.imageCover(frame, bx, by, iw, bh, 10.0F * s);
        } else {
            Paint.liveBanner(g, bx, by, iw, bh, 10.0F * s);
        }
        boolean flyOpen = this.mode == MODE_BANNER;
        Ui.nvgShapes(() -> {
            Paint.grad(g, bx, by, iw, bh, Theme.alpha(0x050508, 0), Theme.alpha(0x050508, 190), 10.0F * s, true);
            Paint.outline(g, bx, by, iw, bh, rgba(flyOpen ? accentHot() : 0xFFFFFF, flyOpen ? 0.95F : (bhot ? 0.45F : 0.16F)), 10.0F * s);
        });
        if (GuiLayout.bannerLabels) {
            Paint.text(g, "DOORE VISUALS", bx + 8.0F * s, by + bh - 14.0F * s, rgba(0xFFFFFF, 1.0F), 6.2F * s);
        }
        this.hits.add(new Hit(bx, by, iw, bh, () -> {
            if (this.mode == MODE_BANNER) {
                this.closeAll();
            } else {
                this.openBanner();
            }
            GuiSfx.click();
        }, null));

        // Category navigation: the first click slides the five columns out, a click on the active entry folds them back.
        float ny = by + bh + 8.0F * s;
        float rh = 20.0F * s;
        for (int i = 0; i < COLS.length; i++) {
            float iy = ny + i * (rh + 2.0F * s);
            boolean sel = this.colOpen[i];
            boolean hot = Paint.hit(this.mx, this.my, x + pad, iy, iw, rh);
            float hv = this.hover.computeIfAbsent("nav" + i, k -> new Anim(0.0F)).springHover(hot ? 1.0F : 0.0F, this.dt);
            float sv = this.toggle.computeIfAbsent("nav" + i, k -> new Anim(sel ? 1.0F : 0.0F)).springToggle(sel ? 1.0F : 0.0F, this.dt);
            if (hv > 0.01F) {
                Paint.box(g, x + pad, iy, iw, rh, rgba(this.cOn, 0.05F * hv), 7.0F * s);
            }
            if (sv > 0.01F) {
                Paint.box(g, x + pad, iy, iw, rh, rgba(accent(), 0.22F * sv), 7.0F * s);
            }
            int c = rgba(mix(mix(this.cOff, this.cOn, hv * 0.7F), mix(accentHot(), this.cOn, 0.3F), sv), 1.0F);
            this.navIcon(g, i, x + pad + 13.0F * s, iy + rh / 2.0F, c);
            Paint.text(g, TITLES[i], x + pad + 26.0F * s, iy + (rh - 6.0F * s * 1.56F) / 2.0F, c, 6.0F * s);
            final int idx = i;
            this.hits.add(new Hit(x + pad, iy, iw, rh, () -> {
                if (this.colOpen[idx]) {
                    this.colOpen[idx] = false;
                    boolean any = false;
                    for (boolean b : this.colOpen) {
                        any |= b;
                    }
                    this.mode = any ? MODE_MODULES : MODE_NONE;
                } else {
                    this.colOpen[idx] = true;
                    this.mode = MODE_MODULES;
                    this.focusCol = idx;
                    this.scrollTarget.put(COLS[idx][0], 0.0F);
                }
                GuiSfx.click();
            }, null));
        }

        // A few theme colours; the last dot opens the full list in the flyout.
        ThemesStore.MixedPreset[] presets = ThemesStore.MIXED;
        int current = currentPreset(presets);
        int quick = 5;
        float sy = ny + COLS.length * (rh + 2.0F * s) + 10.0F * s;
        float step = iw / (quick + 1);
        float dot = 4.8F * s;
        for (int i = 0; i <= quick; i++) {
            float cx = x + pad + step * (i + 0.5F);
            boolean more = i == quick;
            if (more) {
                for (int k = -1; k <= 1; k++) {
                    Paint.circle(g, cx + k * 3.4F * s, sy, 1.2F * s, rgba(this.cOff, 1.0F));
                }
            } else {
                Paint.circle(g, cx, sy, dot, rgba(presets[i].primary(), 1.0F));
                if (i == current) {
                    Paint.ring(g, cx, sy, dot + 2.2F * s, 1.2F * s, rgba(this.cOn, 1.0F));
                }
            }
            final int idx = i;
            this.hits.add(new Hit(cx - step / 2.0F, sy - 9.0F * s, step, 18.0F * s, () -> {
                if (more) {
                    this.openBanner();
                    GuiSfx.click();
                } else {
                    this.applyPreset(presets[idx]);
                }
            }, null));
        }

        // Panel style: blur / black / white.
        float styleY = sy + 14.0F * s;
        float gap = 4.0F * s;
        float bw = (iw - gap * 2.0F) / 3.0F;
        int style = GuiWindowsFeature.panelStyle();
        int[] preview = {0x1A1824, 0x050508, 0xEEECF4};
        for (int i = 0; i < 3; i++) {
            float sx = x + pad + i * (bw + gap);
            boolean sel = i == style;
            Paint.box(g, sx, styleY, bw, 18.0F * s, rgba(preview[i], 1.0F), 6.0F * s);
            Paint.outline(g, sx, styleY, bw, 18.0F * s, sel ? rgba(accentHot(), 1.0F) : rgba(this.cOn, 0.14F), 6.0F * s);
            Paint.textC(g, STYLE_NAMES[i], sx + bw / 2.0F, styleY + 21.0F * s, rgba(sel ? this.cOn : this.cOff, 1.0F), 4.4F * s);
            final int idx = i;
            this.hits.add(new Hit(sx, styleY, bw, 30.0F * s, () -> {
                GuiWindowsFeature.setPanelStyle(idx);
                GuiSfx.click();
            }, null));
        }

        // Profile plate: player name and active config; opens the config manager.
        float uh = 28.0F * s;
        float uy = y + h - pad - uh;
        boolean uhot = Paint.hit(this.mx, this.my, x + pad, uy, iw, uh);
        Paint.box(g, x + pad, uy, iw, uh, rgba(this.subRgb, uhot ? 0.95F : 0.75F), 8.0F * s);
        MinecraftClient mc = MinecraftClient.getInstance();
        String nick = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        Paint.circle(g, x + pad + 14.0F * s, uy + uh / 2.0F, 8.0F * s, rgba(accent(), 1.0F));
        Paint.textC(g, nick.isEmpty() ? "?" : nick.substring(0, 1).toUpperCase(Locale.ROOT), x + pad + 14.0F * s, uy + uh / 2.0F - 6.0F * s * 1.56F / 2.0F, rgba(0xFFFFFF, 1.0F), 6.0F * s);
        float tx = x + pad + 26.0F * s;
        float tmax = iw - 30.0F * s;
        Paint.text(g, this.fitText(nick, 5.4F * s, tmax), tx, uy + 5.0F * s, rgba(this.cOn, 1.0F), 5.4F * s);
        Paint.text(g, this.fitText("конфиг: " + ConfigFeature.activeName(), 4.2F * s, tmax), tx, uy + 16.0F * s, rgba(this.cOff, 1.0F), 4.2F * s);
        this.hits.add(new Hit(x + pad, uy, iw, uh, () -> {
            if (this.client != null) {
                this.client.setScreen(new ConfigManagerScreen(this));
            }
        }, null));
    }

    // ---------- appearance flyout ----------

    private void drawFlyout(DrawContext g, float[] r) {
        float bv = clamp01(this.banAnim.value());
        this.flyW = 0.0F;
        if (bv < 0.02F) {
            return;
        }
        float s = this.s;
        float w = FLY_W * s;
        float x = r[4] - (1.0F - bv) * 14.0F * s;
        float y = r[5];
        float h = r[8];
        this.flyX = x;
        this.flyW = w;
        Ui.alpha(this.frameAlpha * bv);
        this.panelBody(g, x, y, w, h);
        Paint.textC(g, "Оформление", x + w / 2.0F, y + 8.0F * s, rgba(this.cOn, 1.0F), 7.2F * s);
        float pad = 8.0F * s;
        float iw = w - pad * 2.0F;
        float lx = x + pad;
        int muted = rgba(this.cOff, 1.0F);
        this.flyTop = y + 26.0F * s;
        this.flyBottom = y + h - 6.0F * s;
        this.flyView = this.flyBottom - this.flyTop;
        float min = Math.min(0.0F, this.flyView - this.flyContent);
        this.flyTarget = Math.max(min, Math.min(0.0F, this.flyTarget));
        this.flyScroll += (this.flyTarget - this.flyScroll) * (1.0F - (float)Math.exp(-this.dt * 14.0F));
        Ui.scissor(x, this.flyTop, w, this.flyView);
        float cy = this.flyTop + this.flyScroll;

        // --- banner: built-in animations and everything the player added
        Paint.text(g, "Баннер: картинка, GIF или видео", lx, cy, muted, 4.8F * s);
        cy += 11.0F * s;
        List<BannerLibrary.Entry> entries = BannerLibrary.entries();
        int cols = 3;
        float gp = 5.0F * s;
        float tw = (iw - gp * (cols - 1)) / cols;
        float th = 32.0F * s;
        for (int i = 0; i < entries.size(); i++) {
            BannerLibrary.Entry e = entries.get(i);
            float tx = lx + (i % cols) * (tw + gp);
            float ty = cy + (i / cols) * (th + gp);
            if (ty + th < this.flyTop - 4.0F * s || ty > this.flyBottom + 4.0F * s) {
                continue;
            }
            boolean sel = e.id().equals(BannerLibrary.selectedId());
            Paint.box(g, tx - 2.0F * s, ty - 2.0F * s, tw + 4.0F * s, th + 4.0F * s, sel ? rgba(accentHot(), 0.95F) : rgba(this.cOn, 0.08F), 8.0F * s);
            Image preview = BannerLibrary.preview(e);
            if (preview != null) {
                Nvg.imageCover(preview, tx, ty, tw, th, 6.0F * s);
            } else {
                Paint.box(g, tx, ty, tw, th, rgba(this.subRgb, 1.0F), 6.0F * s);
                Paint.textC(g, e.label(), tx + tw / 2.0F, ty + (th - 5.0F * s * 1.56F) / 2.0F, muted, 5.0F * s);
            }
            final String id = e.id();
            this.flyHit(tx, ty, tw, th, () -> {
                BannerLibrary.select(id);
                if (App.live()) {
                    App.save().defer();
                }
                GuiSfx.click();
            });
        }
        cy += ((entries.size() + cols - 1) / cols) * (th + gp) + 2.0F * s;

        float half = (iw - 5.0F * s) / 2.0F;
        this.flyButton(g, "Добавить файл", lx, cy, half, 16.0F * s, this::pickBanner);
        this.flyButton(g, "Папка", lx + half + 5.0F * s, cy, half, 16.0F * s, () -> {
            Folders.ensure(BannerLibrary.dir());
            Folders.open(BannerLibrary.dir());
            BannerLibrary.reload();
        });
        cy += 20.0F * s;
        String hint = this.flyNote.isEmpty() ? "PNG, GIF, MP4, WEBM: можно перетащить файл в окно" : this.flyNote;
        for (String line : this.wrapText(hint, 4.4F * s, iw, 3)) {
            Paint.text(g, line, lx, cy, rgba(this.flyNote.isEmpty() ? this.cOff : accentHot(), 1.0F), 4.4F * s);
            cy += 7.0F * s;
        }
        cy += 4.0F * s;

        boolean labels = GuiLayout.bannerLabels;
        Paint.text(g, "Подпись на баннере", lx, cy + 2.0F * s, rgba(labels ? this.cOn : this.cOff, 1.0F), 5.2F * s);
        Paint.check(g, lx + iw - 12.0F * s, cy, 11.0F * s, labels);
        this.flyHit(lx, cy, iw, 14.0F * s, () -> {
            GuiLayout.bannerLabels = !GuiLayout.bannerLabels;
            if (App.live()) {
                App.save().defer();
            }
            GuiSfx.toggle(GuiLayout.bannerLabels);
        });
        cy += 20.0F * s;

        // --- themes
        Paint.text(g, "Тема", lx, cy, muted, 4.8F * s);
        cy += 11.0F * s;
        ThemesStore.MixedPreset[] presets = ThemesStore.MIXED;
        int current = currentPreset(presets);
        int per = 6;
        float step = iw / per;
        float dot = 5.4F * s;
        String hovered = null;
        for (int i = 0; i < presets.length; i++) {
            float cx = lx + step * (i % per + 0.5F);
            float py = cy + (i / per) * step + step / 2.0F;
            if (py + step < this.flyTop || py - step > this.flyBottom) {
                continue;
            }
            Paint.circle(g, cx, py, dot, rgba(presets[i].primary(), 1.0F));
            Paint.circle(g, cx + dot * 0.62F, py + dot * 0.62F, dot * 0.42F, rgba(presets[i].secondary(), 1.0F));
            if (i == current) {
                Paint.ring(g, cx, py, dot + 2.4F * s, 1.2F * s, rgba(this.cOn, 1.0F));
            }
            if (this.my >= this.flyTop && this.my <= this.flyBottom && Paint.hit(this.mx, this.my, cx - step / 2.0F, py - step / 2.0F, step, step)) {
                hovered = presets[i].label();
            }
            final int idx = i;
            this.flyHit(cx - step / 2.0F, py - step / 2.0F, step, step, () -> this.applyPreset(presets[idx]));
        }
        cy += ((presets.length + per - 1) / per) * step + 4.0F * s;
        String name = hovered != null ? hovered : (current >= 0 ? presets[current].label() : "своя");
        Paint.text(g, this.fitText("Тема: " + name, 5.0F * s, iw), lx, cy, rgba(this.cOn, 1.0F), 5.0F * s);
        cy += 12.0F * s;
        this.flyButton(g, "Случайная", lx, cy, half, 16.0F * s, () ->
                this.applyPreset(presets[ThreadLocalRandom.current().nextInt(presets.length)]));
        this.flyButton(g, "По умолч.", lx + half + 5.0F * s, cy, half, 16.0F * s, () -> this.applyPreset(presets[0]));
        cy += 22.0F * s;

        // --- own theme: two colours with live preview, export and import as a text code
        Paint.text(g, "Своя тема", lx, cy, muted, 4.8F * s);
        cy += 11.0F * s;
        if (this.flyDrag == null) {
            java.awt.Color.RGBtoHSB((Theme.ACCENT >> 16) & 0xFF, (Theme.ACCENT >> 8) & 0xFF, Theme.ACCENT & 0xFF, this.hsvA);
            java.awt.Color.RGBtoHSB((Theme.ACCENT_HOT >> 16) & 0xFF, (Theme.ACCENT_HOT >> 8) & 0xFF, Theme.ACCENT_HOT & 0xFF, this.hsvB);
        }
        // Preview: the two colours as a gradient and a sample watermark plate in the HUD colour.
        float prevH = 18.0F * s;
        Paint.grad(g, lx, cy, iw, prevH, rgba(Theme.ACCENT, 1.0F), rgba(Theme.ACCENT_HOT, 1.0F), 6.0F * s, false);
        cy += prevH + 4.0F * s;
        Paint.hudPlate(g, lx, cy, iw, 18.0F * s, Theme.HUD);
        Paint.text(g, "DooreVisuals", lx + 8.0F * s, cy + 5.0F * s, Theme.HUD, 6.0F * s);
        Paint.text(g, "Fps 240   Ping 12", lx + 8.0F * s + Paint.tw("DooreVisuals", 6.0F * s) + 8.0F * s, cy + 5.0F * s, Theme.TEXT, 6.0F * s);
        cy += 18.0F * s + 6.0F * s;
        cy = this.hsvSliders(g, "Основной", this.hsvA, lx, cy, iw, true);
        cy = this.hsvSliders(g, "Второй", this.hsvB, lx, cy, iw, false);
        this.flyButton(g, "Копировать код", lx, cy, half, 16.0F * s, this::copyThemeCode);
        this.flyButton(g, "Вставить код", lx + half + 5.0F * s, cy, half, 16.0F * s, this::pasteThemeCode);
        cy += 22.0F * s;

        this.flyContent = cy - (this.flyTop + this.flyScroll);
        Ui.unscissor();
        Paint.scrollbar(g, x + w - 5.0F * s, this.flyTop, this.flyView, this.flyContent, this.flyView, this.flyScroll);
        Ui.alpha(this.frameAlpha);
    }

    private record FlyDrag(float x, float w, java.util.function.Consumer<Float> set) {
    }

    private void applyFlyDrag(double mx) {
        FlyDrag d = this.flyDrag;
        if (d != null && d.w() > 1.0F) {
            d.set().accept(clamp01((float)((mx - d.x()) / d.w())));
        }
    }

    /** Three sliders (hue, saturation, brightness) for one colour of the own theme; returns the next y. */
    private float hsvSliders(DrawContext g, String title, float[] hsv, float x, float y, float w, boolean primary) {
        float s = this.s;
        Paint.text(g, title, x, y, rgba(this.cOff, 1.0F), 4.6F * s);
        y += 8.0F * s;
        String[] names = {"оттенок", "насыщ.", "яркость"};
        for (int k = 0; k < 3; k++) {
            final int channel = k;
            float h = 5.0F * s;
            float tx = x;
            float tw = w;
            if (k == 0) {
                for (int i = 0; i < 6; i++) {
                    int c0 = java.awt.Color.HSBtoRGB(i / 6.0F, 1.0F, 1.0F) | 0xFF000000;
                    int c1 = java.awt.Color.HSBtoRGB((i + 1) / 6.0F, 1.0F, 1.0F) | 0xFF000000;
                    Paint.grad(g, tx + tw * i / 6.0F, y, tw / 6.0F + 0.6F, h, c0, c1, 0.0F, false);
                }
            } else {
                float sat = k == 1 ? 0.0F : hsv[1];
                int c0 = java.awt.Color.HSBtoRGB(hsv[0], k == 1 ? 0.0F : hsv[1], k == 1 ? hsv[2] : 0.0F) | 0xFF000000;
                int c1 = java.awt.Color.HSBtoRGB(hsv[0], k == 1 ? 1.0F : hsv[1], hsv[2] > 0.0F ? (k == 1 ? hsv[2] : 1.0F) : 1.0F) | 0xFF000000;
                Paint.grad(g, tx, y, tw, h, c0, c1, 2.0F * s, false);
            }
            float knob = tx + tw * clamp01(hsv[k]);
            Paint.circle(g, knob, y + h / 2.0F, 4.2F * s, rgba(0xFFFFFF, 1.0F));
            Paint.circle(g, knob, y + h / 2.0F, 2.6F * s, rgba(java.awt.Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]), 1.0F));
            this.flyHit(tx - 3.0F * s, y - 4.0F * s, tw + 6.0F * s, h + 8.0F * s, () -> {
                this.flyDrag = new FlyDrag(tx, tw, value -> {
                    hsv[channel] = value;
                    this.applyOwnTheme();
                });
                this.applyFlyDrag(this.clickX);
            });
            y += h + 5.0F * s;
        }
        return y + 2.0F * s;
    }

    /** Applies the two colours of the editor to the whole client at once. */
    private void applyOwnTheme() {
        int a = java.awt.Color.HSBtoRGB(this.hsvA[0], this.hsvA[1], this.hsvA[2]) | 0xFF000000;
        int b = java.awt.Color.HSBtoRGB(this.hsvB[0], this.hsvB[1], this.hsvB[2]) | 0xFF000000;
        App.features().find(ThemeFeature.class).ifPresent(tf -> tf.applyMixed(new ThemesStore.MixedPreset("своя", a, b)));
    }

    private void copyThemeCode() {
        String code = ThemeCodec.encode(Theme.ACCENT, Theme.ACCENT_HOT, Theme.HUD, Theme.EFFECT);
        if (this.client != null) {
            this.client.keyboard.setClipboard(code);
        }
        this.flyNote = "Код темы скопирован";
    }

    private void pasteThemeCode() {
        String clip = this.client == null ? "" : this.client.keyboard.getClipboard();
        int[] c = ThemeCodec.decode(clip);
        if (c == null) {
            this.flyNote = "В буфере нет кода темы (DVT1:...)";
            return;
        }
        App.features().find(ThemeFeature.class).ifPresent(tf -> tf.applyMixed(new ThemesStore.MixedPreset("импорт", c[0], c[1])));
        Theme.applyMixed(c[0], c[1], c[2], c[3]);
        this.flyNote = "Тема применена";
    }

    private void flyButton(DrawContext g, String label, float x, float y, float w, float h, Runnable action) {
        float s = this.s;
        boolean hot = this.my >= this.flyTop && this.my <= this.flyBottom && Paint.hit(this.mx, this.my, x, y, w, h);
        Paint.box(g, x, y, w, h, rgba(hot ? accent() : this.subRgb, hot ? 0.95F : 0.9F), 6.0F * s);
        Paint.textC(g, label, x + w / 2.0F, y + (h - 5.0F * s * 1.56F) / 2.0F, rgba(hot ? 0xFFFFFF : this.cOn, 1.0F), 5.0F * s);
        this.flyHit(x, y, w, h, () -> {
            GuiSfx.click();
            action.run();
        });
    }

    private void flyHit(float x, float y, float w, float h, Runnable action) {
        float top = Math.max(y, this.flyTop);
        float bottom = Math.min(y + h, this.flyBottom);
        if (bottom > top) {
            this.hits.add(new Hit(x, top, w, bottom - top, action, null));
        }
    }

    private List<String> wrapText(String t, float size, float maxW, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : t.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (Paint.tw(next, size) > maxW && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
                if (lines.size() == maxLines) {
                    return lines;
                }
            } else {
                line = new StringBuilder(next);
            }
        }
        if (lines.size() < maxLines && line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private void pickBanner() {
        Folders.pickImage(path -> MinecraftClient.getInstance().execute(() -> this.importBanner(path)), () ->
                MinecraftClient.getInstance().execute(() -> {
                    Folders.ensure(BannerLibrary.dir());
                    Folders.open(BannerLibrary.dir());
                    BannerLibrary.reload();
                    this.flyNote = "Положите файл в открывшуюся папку";
                }));
    }

    private void importBanner(Path path) {
        boolean ok = BannerLibrary.importFile(path);
        String file = path.getFileName() == null ? "файл" : path.getFileName().toString();
        if (!ok) {
            this.flyNote = "Не удалось импортировать " + file;
        } else if (GifIO.isVideo(path) && BannerLibrary.currentFrame() == null) {
            this.flyNote = "Для видео нужен ffmpeg: добавлено " + file + ", но кадры не получены";
        } else {
            this.flyNote = "Добавлено: " + file;
        }
        if (ok && App.live()) {
            App.save().defer();
        }
    }

    @Override
    public void onFilesDropped(List<Path> paths) {
        if (paths == null || !App.live()) {
            return;
        }
        this.openBanner();
        for (Path path : paths) {
            this.importBanner(path);
        }
    }

    private String fitText(String t, float size, float maxW) {
        if (Paint.tw(t, size) <= maxW) {
            return t;
        }
        String r = t;
        while (r.length() > 1 && Paint.tw(r + "…", size) > maxW) {
            r = r.substring(0, r.length() - 1);
        }
        return r + "…";
    }

    private void navIcon(DrawContext g, int i, float cx, float cy, int c) {
        float u = this.s;
        switch (i) {
            case 0 -> {
                Paint.outline(g, cx - 7.0F * u, cy - 5.5F * u, 14.0F * u, 11.0F * u, c, 3.0F * u);
                Paint.box(g, cx - 4.5F * u, cy - 1.8F * u, 6.0F * u, 1.8F * u, c, 0.9F * u);
                Paint.box(g, cx - 4.5F * u, cy + 1.4F * u, 9.0F * u, 1.8F * u, c, 0.9F * u);
            }
            case 1 -> {
                Paint.ring(g, cx, cy, 6.5F * u, 1.5F * u, c);
                Paint.circle(g, cx + 1.8F * u, cy - 1.4F * u, 2.1F * u, c);
            }
            case 2 -> {
                Paint.box(g, cx - 0.7F * u, cy - 6.0F * u, 1.4F * u, 12.0F * u, c, 0.7F * u);
                Paint.box(g, cx - 6.0F * u, cy - 0.7F * u, 12.0F * u, 1.4F * u, c, 0.7F * u);
                Paint.circle(g, cx + 4.4F * u, cy - 4.4F * u, 1.6F * u, c);
            }
            case 3 -> {
                Paint.circle(g, cx, cy - 3.0F * u, 3.0F * u, c);
                Paint.box(g, cx - 5.2F * u, cy + 1.0F * u, 10.4F * u, 5.6F * u, c, 2.8F * u);
            }
            default -> {
                for (int k = 0; k < 3; k++) {
                    float ly = cy - 4.5F * u + k * 4.5F * u;
                    Paint.box(g, cx - 6.0F * u, ly - 0.6F * u, 12.0F * u, 1.2F * u, c, 0.6F * u);
                    Paint.circle(g, cx - 3.0F * u + ((k * 5) % 7) * u, ly, 1.8F * u, c);
                }
            }
        }
    }

    private void applyPreset(ThemesStore.MixedPreset preset) {
        App.features().find(ThemeFeature.class).ifPresent(tf -> tf.applyMixed(preset));
        GuiSfx.click();
    }

    private int currentPreset(ThemesStore.MixedPreset[] presets) {
        for (int i = 0; i < presets.length; i++) {
            if ((presets[i].primary() & 0xFFFFFF) == accent()) {
                return i;
            }
        }
        return -1;
    }

    // ---------- module columns ----------

    private static List<Feature> allOf(Category[] cats) {
        List<Feature> list = new ArrayList<>();
        for (Category c : cats) {
            for (Feature feature : App.features().of(c)) {
                list.add(feature);
            }
        }
        return list;
    }

    private List<Feature> modulesOf(Category[] cats) {
        // Sorted once per column: the set of modules does not change while the menu is open.
        return this.moduleLists.computeIfAbsent(cats, key -> {
            List<Feature> list = new ArrayList<>();
            for (Feature feature : allOf(key)) {
                if (feature.listed()) {
                    list.add(feature);
                }
            }
            list.sort((p, o) -> p.name().compareToIgnoreCase(o.name()));
            return list;
        });
    }

    /** Search never hides modules: every row stays in place and the matching ones light up. */
    private boolean matches(Feature feature) {
        if (this.search.isEmpty()) {
            return false;
        }
        String q = this.search.toLowerCase(Locale.ROOT);
        return feature.name().toLowerCase(Locale.ROOT).contains(q) || feature.id().toLowerCase(Locale.ROOT).contains(q);
    }

    /** Scrolls every column so its first match is at the top. Called whenever the query changes. */
    private void jumpToMatches() {
        if (this.search.isEmpty()) {
            return;
        }
        for (Category[] cats : COLS) {
            float offset = 0.0F;
            for (Feature feature : this.modulesOf(cats)) {
                if (this.matches(feature)) {
                    this.scrollTarget.put(cats[0], -offset);
                    break;
                }
                float opened = this.open.contains(feature.id()) ? this.optsHeight(feature.visibleOpts()) : 0.0F;
                offset += 20.0F * this.s + opened + 3.0F * this.s;
            }
        }
    }

    private void drawColumns(DrawContext g, float[] r) {
        this.matchCount = 0;
        this.searchAnim.springHover(this.search.isEmpty() ? 0.0F : 1.0F, this.dt);
        float colW = r[6];
        float colGap = r[7];
        this.colWNow = colW;
        for (int i = 0; i < COLS.length; i++) {
            this.colXs[i] = -10000.0F;
        }
        if (this.columnsOut() < 0.02F) {
            return;
        }
        // Each plate takes its place after the ones to its left that are out, and slides in from the sidebar side.
        float[] baseX = new float[COLS.length];
        float acc = r[4];
        for (int i = 0; i < COLS.length; i++) {
            float t = clamp01(this.colAnim[i].value());
            baseX[i] = acc;
            acc += t * (colW + colGap);
        }
        for (int i = COLS.length - 1; i >= 0; i--) {
            float t = clamp01(this.colAnim[i].value());
            if (t < 0.02F) {
                continue;
            }
            float x = baseX[i] - (1.0F - t) * 30.0F * this.s;
            this.colXs[i] = x;
            this.colFade = t;
            Ui.alpha(this.frameAlpha * t);
            this.drawColumn(g, COLS[i], TITLES[i], x, r[5], colW, r[8]);
        }
        this.colFade = 1.0F;
        Ui.alpha(this.frameAlpha);
    }

    private void drawColumn(DrawContext g, Category[] cats, String title, float x, float y, float w, float h) {
        float s = this.s;
        Category cat = cats[0];
        this.panelBody(g, x, y, w, h);
        this.panelHeader(g, x, y, w, title);

        float viewTop = y + 32.0F * s;
        float viewBottom = y + h - 6.0F * s;
        float target = this.scrollTarget.getOrDefault(cat, 0.0F);
        float sc = this.scroll.getOrDefault(cat, 0.0F);
        sc += (target - sc) * (1.0F - (float)Math.exp(-this.dt * 14.0F));
        float cy = viewTop + sc;
        float total = 0;
        List<Feature> modules = this.modulesOf(cats);
        Ui.scissor(x, viewTop, w, viewBottom - viewTop);
        for (Feature feature : modules) {
            float amt = this.expand.computeIfAbsent(feature.id(), k -> new Anim(0.0F))
                    .springExpand(this.open.contains(feature.id()) ? 1.0F : 0.0F, this.dt);
            List<Opt<?>> opts = feature.visibleOpts();
            float rowH = 20.0F * s + amt * this.optsHeight(opts);
            boolean hit = this.matches(feature);
            if (hit) {
                this.matchCount++;
            }
            if (cy + rowH >= viewTop && cy <= viewBottom) {
                float lit = this.matchAnim.computeIfAbsent(feature.id(), k -> new Anim(0.0F)).springHover(hit ? 1.0F : 0.0F, this.dt);
                this.drawRow(g, feature, opts, x + 6.0F * s, cy, w - 12.0F * s, rowH, amt, viewTop, viewBottom, lit);
            }
            cy += rowH + 3.0F * s;
            total += rowH + 3.0F * s;
        }
        Ui.unscissor();
        Paint.scrollbar(g, x + w - 5.0F * s, viewTop, viewBottom - viewTop, total, viewBottom - viewTop, sc);
        if (modules.isEmpty()) {
            Paint.textC(g, "Пусто", x + w / 2.0F, viewTop + 8.0F * s, rgba(this.cOff, 1.0F), 6.2F * s);
        }
        float min = Math.min(0.0F, (viewBottom - viewTop) - total);
        this.content.put(cat, min);
        this.scrollTarget.put(cat, Math.max(min, Math.min(0.0F, target)));
        this.scroll.put(cat, Math.max(min, Math.min(0.0F, sc)));
    }

    private void drawRow(DrawContext g, Feature f, List<Opt<?>> opts, float x, float y, float w, float rowH, float amt, float vy0, float vy1, float lit) {
        float s = this.s;
        // While searching, rows that do not match fade back and the matching ones get a bright frame.
        float searching = clamp01(this.searchAnim.value());
        float fade = 1.0F - 0.62F * searching * (1.0F - clamp01(lit));
        float rowAlpha = this.frameAlpha * this.colFade * fade;
        Ui.alpha(rowAlpha);
        boolean on = f.on();
        float headH = 20.0F * s;
        boolean hot = this.my >= vy0 && this.my <= vy1 && Paint.hit(this.mx, this.my, x, y, w, headH);
        float hv = this.hover.computeIfAbsent(f.id(), k -> new Anim(0.0F)).springHover(hot ? 1.0F : 0.0F, this.dt);
        float tg = this.toggle.computeIfAbsent(f.id(), k -> new Anim(on ? 1.0F : 0.0F)).springToggle(on ? 1.0F : 0.0F, this.dt);

        // Quiet rows stay transparent until hovered; enabled rows get a soft accent wash and an accent-tinted name.
        if (hv > 0.01F) {
            Paint.box(g, x, y, w, headH, rgba(this.cOn, 0.05F * hv), 6.0F * s);
        }
        if (tg > 0.01F) {
            Paint.box(g, x, y, w, headH, rgba(accent(), 0.20F * tg), 6.0F * s);
        }
        if (lit > 0.01F) {
            Paint.outline(g, x - 1.0F * s, y - 1.0F * s, w + 2.0F * s, headH + 2.0F * s, rgba(0xFFFFFF, 0.9F * lit), 5.0F * s);
            Paint.outline(g, x - 3.0F * s, y - 3.0F * s, w + 6.0F * s, headH + 6.0F * s, rgba(accentHot(), 0.45F * lit), 7.0F * s);
        }
        int text = mix(mix(mix(this.cOff, this.cOn, hv * 0.7F), mix(accentHot(), this.cOn, 0.3F), tg), this.cOn, lit);
        float nameX = x + 8.0F * s + hv * 2.0F * s;
        float nameSize = 6.0F * s;
        float nameY = y + (headH - nameSize * 1.56F) / 2.0F;
        float roomRight = x + w - 22.0F * s;
        Opt.Key bindKey = f.toggleBind();
        String bindText = bindKey != null && bindKey.get() != -1 ? Keys.name(bindKey.get()) : "";
        float bindW = bindText.isEmpty() ? 0.0F : Paint.tw(bindText, 5.0F * s) + 5.0F * s;
        String name = f.name();
        float room = roomRight - nameX - bindW;
        String shown = Paint.tw(name, nameSize) <= room ? name : this.fitText(name, nameSize, room);
        int at = this.search.isEmpty() ? -1 : name.toLowerCase(Locale.ROOT).indexOf(this.search.toLowerCase(Locale.ROOT));
        if (at >= 0 && lit > 0.01F && shown.equals(name)) {
            String head = name.substring(0, at);
            String mid = name.substring(at, at + this.search.length());
            String tail = name.substring(at + this.search.length());
            float hx = nameX + Paint.tw(head, nameSize);
            Paint.text(g, head, nameX, nameY, rgba(text, 1.0F), nameSize);
            Paint.text(g, mid, hx, nameY, rgba(mix(text, 0xFFE27A, lit), 1.0F), nameSize);
            Paint.text(g, tail, hx + Paint.tw(mid, nameSize), nameY, rgba(text, 1.0F), nameSize);
        } else {
            Paint.text(g, shown, nameX, nameY, rgba(text, 1.0F), nameSize);
        }
        this.drawBadge(g, f, nameX + Paint.tw(shown, nameSize) + 4.0F * s, y + headH / 2.0F, roomRight - bindW);

        // Bound key as quiet text, "..." while a key is being captured.
        if (f == this.bindFeature) {
            this.pill(g, "…", x + w - 26.0F * s, y + headH / 2.0F, true);
        } else if (!bindText.isEmpty()) {
            Paint.textR(g, bindText, roomRight, y + (headH - 5.0F * s * 1.56F) / 2.0F, rgba(this.cOff, 1.0F), 5.0F * s);
        }

        // Three dots on the right, bright when the module is on.
        float dx = x + w - 10.0F * s;
        float dy = y + headH / 2.0F;
        int dot = on ? mix(accentHot(), this.cOn, 0.3F) : mix(this.cOff, this.panelRgb, 0.25F);
        for (int i = -1; i <= 1; i++) {
            Paint.circle(g, dx + i * 3.6F * s, dy, 1.2F * s, rgba(dot, 1.0F));
        }

        float headTop = Math.max(y, vy0);
        float headBottom = Math.min(y + headH, vy1);
        if (hot) {
            this.hoverFeature = f;
        }
        if (headBottom > headTop) {
            this.midHits.add(new Hit(x, headTop, w - 24.0F * s, headBottom - headTop, () -> this.startBind(f), null));
        }
        if (headBottom > headTop) {
            Runnable expandIt = () -> {
                if (opts.isEmpty()) {
                    return;
                }
                if (!this.open.remove(f.id())) {
                    this.open.add(f.id());
                }
                GuiSfx.click();
            };
            this.hits.add(new Hit(x, headTop, w - 24.0F * s, headBottom - headTop, () -> {
                f.flip();
                this.toggle.computeIfAbsent(f.id(), k -> new Anim(0.0F)).snap(f.on() ? 1.0F : 0.0F);
                GuiSfx.toggle(f.on());
            }, expandIt));
            this.hits.add(new Hit(x + w - 24.0F * s, headTop, 24.0F * s, headBottom - headTop, expandIt, expandIt));
        }
        if (amt > 0.01F) {
            Paint.box(g, x, y + headH + 2.0F * s, w, Math.max(0.0F, rowH - headH - 2.0F * s), rgba(this.subRgb, 0.62F * amt), 7.0F * s);
            float oy = y + headH + 3.0F * s - (1.0F - amt) * 6.0F * s;
            this.optHits = amt > 0.6F;
            Ui.alpha(rowAlpha * amt);
            for (Opt<?> opt : opts) {
                float oh = this.optHeight(opt);
                this.drawOpt(g, f, opt, x + 4.0F * s, oy, w - 8.0F * s, oh, vy0, vy1);
                oy += oh;
            }
            Ui.alpha(rowAlpha);
            this.optHits = false;
        }
        Ui.alpha(this.frameAlpha * this.colFade);
    }

    /** Tiny tag after the module name; skipped when it would run into the bind pill. */
    private void drawBadge(DrawContext g, Feature f, float x, float cy, float limit) {
        Badge badge = f.badge();
        if (badge == null) {
            return;
        }
        float s = this.s;
        float size = 4.6F * s;
        float bw = Paint.tw(badge.label(), size) + 7.0F * s;
        float bh = 9.0F * s;
        if (x + bw > limit) {
            return;
        }
        Paint.box(g, x, cy - bh / 2.0F, bw, bh, rgba(this.panelRgb, 0.62F), 4.0F * s);
        Paint.outline(g, x, cy - bh / 2.0F, bw, bh, rgba(badge.rgb(), 0.7F), 4.0F * s);
        Paint.textC(g, badge.label(), x + bw / 2.0F, cy - size * 1.56F / 2.0F, rgba(badge.rgb(), 1.0F), size);
    }

    private float optsHeight(List<Opt<?>> opts) {
        if (opts.isEmpty()) {
            return 0.0F;
        }
        float sum = 6.0F * this.s;
        for (Opt<?> opt : opts) {
            sum += this.optHeight(opt);
        }
        return sum;
    }

    private static final int CHIP_COLUMNS = 4;
    private final java.util.Map<Opt.Pick, String[]> chipKeys = new java.util.IdentityHashMap<>();

    private float optHeight(Opt<?> opt) {
        if (opt instanceof Opt.Pick pick && pick.isChips()) {
            int rows = (pick.options().size() + CHIP_COLUMNS - 1) / CHIP_COLUMNS;
            return (16 + rows * 16 + 2) * this.s;
        }
        return (opt instanceof Opt.Num ? 28 : 20) * this.s;
    }

    private void addHit(float x, float y, float w, float h, float vy0, float vy1, Runnable left, Runnable right) {
        if (!this.optHits) {
            return;
        }
        float top = Math.max(y, vy0);
        float bottom = Math.min(y + h, vy1);
        if (bottom > top) {
            this.hits.add(new Hit(x, top, w, bottom - top, left, right));
        }
    }

    private void pill(DrawContext g, String text, float right, float cy, boolean accentOn) {
        float s = this.s;
        float size = 5.8F * s;
        float pw = Paint.tw(text, size) + 12.0F * s;
        float ph = 13.0F * s;
        Paint.box(g, right - pw, cy - ph / 2.0F, pw, ph, accentOn ? rgba(accent(), 0.9F) : rgba(this.cOn, 0.10F), 6.0F * s);
        Paint.textC(g, text, right - pw / 2.0F, cy - size * 1.56F / 2.0F, rgba(accentOn ? 0xFFFFFF : this.cOn, 1.0F), size);
    }

    private float pillWidth(String text) {
        return Paint.tw(text, 5.8F * this.s) + 12.0F * this.s;
    }

    private void drawOpt(DrawContext g, Feature f, Opt<?> opt, float x, float y, float w, float h, float vy0, float vy1) {
        float s = this.s;
        float ts = 5.8F * s;
        float lineY = y + (18.0F * s - ts * 1.56F) / 2.0F;
        int muted = rgba(this.cOff, 1.0F);
        if (opt instanceof Opt.Action action) {
            boolean hot = this.my >= vy0 && this.my <= vy1 && Paint.hit(this.mx, this.my, x, y + 2.0F * s, w, h - 4.0F * s);
            Paint.box(g, x, y + 2.0F * s, w, h - 4.0F * s, rgba(accent(), hot ? 0.9F : 0.7F), 4.0F * s);
            Paint.textC(g, opt.label(), x + w / 2.0F, y + 2.0F * s + (h - 4.0F * s - ts * 1.56F) / 2.0F, rgba(0xFFFFFF, 1.0F), ts);
            this.addHit(x, y + 2.0F * s, w, h - 4.0F * s, vy0, vy1, () -> {
                GuiSfx.click();
                action.run();
            }, null);
        } else if (opt instanceof Opt.Flag flag) {
            boolean v = flag.get();
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, lineY, v ? rgba(this.cOn, 1.0F) : muted, ts);
            Paint.check(g, x + w - 13.0F * s, y + 3.0F * s, 11.0F * s, v);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                flag.flip();
                f.poke();
                GuiSfx.toggle(flag.get());
            }, null);
        } else if (opt instanceof Opt.Num num) {
            String value = String.format(Locale.ROOT, "%.2f", num.get()).replaceAll("0+$", "").replaceAll("\\.$", "");
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, y + 2.0F * s, muted, ts);
            Paint.textR(g, value, x + w - 4.0F * s, y + 2.0F * s, rgba(this.cOn, 1.0F), ts);
            float sx = x + 4.0F * s;
            float sw = w - 8.0F * s;
            float p = (float)((num.get() - num.min()) / (num.max() - num.min()));
            Paint.box(g, sx, y + 16.0F * s, sw, 4.0F * s, rgba(this.cOn, 0.12F), 2.0F * s);
            Paint.box(g, sx, y + 16.0F * s, sw * clamp01(p), 4.0F * s, rgba(accentHot(), 1.0F), 2.0F * s);
            this.addHit(sx - 3.0F * s, y + 11.0F * s, sw + 6.0F * s, 14.0F * s, vy0, vy1, () -> {
                this.drag = new DragNum(num, f, sx, sw);
                this.applyDrag(this.clickX);
            }, null);
        } else if (opt instanceof Opt.Pick pick && pick.isChips()) {
            // Compact buttons in several rows; the chosen one is filled with the accent colour.
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, lineY, muted, ts);
            List<String> names = pick.options();
            String[] keys = this.chipKeys.get(pick);
            if (keys == null || keys.length != names.size()) {
                keys = new String[names.size()];
                for (int k = 0; k < keys.length; k++) {
                    keys[k] = f.id() + ":" + opt.id() + ":" + k;
                }
                this.chipKeys.put(pick, keys);
            }
            float gap = 3.0F * s;
            float cw = (w - 8.0F * s - gap * (CHIP_COLUMNS - 1)) / CHIP_COLUMNS;
            float ch = 13.0F * s;
            float top = y + 16.0F * s;
            float size = 5.2F * s;
            for (int n = 0; n < names.size(); n++) {
                String name = names.get(n);
                float cx = x + 4.0F * s + (n % CHIP_COLUMNS) * (cw + gap);
                float cy = top + (n / CHIP_COLUMNS) * (ch + gap);
                boolean chosen = name.equals(pick.get());
                boolean hot = this.my >= vy0 && this.my <= vy1 && Paint.hit(this.mx, this.my, cx, cy, cw, ch);
                float hv = this.hover.computeIfAbsent(keys[n], k -> new Anim(0.0F)).springHover(hot ? 1.0F : 0.0F, this.dt);
                Paint.box(g, cx, cy, cw, ch, chosen ? rgba(accent(), 0.92F) : rgba(this.cOn, 0.07F + 0.07F * hv), 4.0F * s);
                Paint.textC(g, name, cx + cw / 2.0F, cy + (ch - size * 1.56F) / 2.0F, rgba(chosen ? 0xFFFFFF : mix(this.cOff, this.cOn, hv), 1.0F), size);
                this.addHit(cx, cy, cw, ch, vy0, vy1, () -> {
                    pick.set(name);
                    f.poke();
                    GuiSfx.click();
                }, null);
            }
        } else if (opt instanceof Opt.Pick pick) {
            String cur = pick.get();
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, lineY, muted, ts);
            this.pill(g, cur, x + w - 2.0F * s, y + 9.0F * s, true);
            float pw = this.pillWidth(cur);
            float pillLeft = x + w - pw - 2.0F * s;
            float pillTop = y + 2.0F * s;
            float pillBottom = y + 16.0F * s;
            if (this.dropPick == pick) {
                this.dropTop = pillTop;
                this.dropBottom = pillBottom;
                this.dropLeft = pillLeft;
                this.dropWidth = pw;
            }
            List<String> options = pick.options();
            Runnable openList = () -> {
                if (this.dropPick == pick) {
                    this.dropPick = null;
                } else {
                    this.dropPick = pick;
                    this.dropOwner = f;
                    this.dropTop = pillTop;
                    this.dropBottom = pillBottom;
                    this.dropLeft = pillLeft;
                    this.dropWidth = pw;
                }
                GuiSfx.click();
            };
            this.addHit(pillLeft, y, pw + 2.0F * s, h, vy0, vy1, openList, null);
        } else if (opt instanceof Opt.Tint tint) {
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, lineY, muted, ts);
            Paint.circle(g, x + w - 9.0F * s, y + 9.0F * s, 4.5F * s, rgba(tint.get(), 1.0F));
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                int cur = tint.get();
                int idx = -1;
                for (int i = 0; i < PALETTE.length; i++) {
                    if ((PALETTE[i] & 0xFFFFFF) == (cur & 0xFFFFFF)) {
                        idx = i;
                        break;
                    }
                }
                int next = PALETTE[(idx + 1) % PALETTE.length] & 0xFFFFFF;
                int alpha = cur & 0xFF000000;
                tint.set((alpha == 0 ? 0xFF000000 : alpha) | next);
                f.poke();
            }, null);
        } else if (opt instanceof Opt.Key key) {
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, lineY, muted, ts);
            this.pill(g, this.capture == key ? "…" : Keys.name(key.get()), x + w - 2.0F * s, y + 9.0F * s, this.capture == key);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                this.capture = key;
                this.captureOwner = f;
            }, null);
        } else if (opt instanceof Opt.Text text) {
            String v = text.get();
            Paint.text(g, this.fitText(opt.label(), ts, w * 0.58F), x + 4.0F * s, lineY, muted, ts);
            this.pill(g, v.isBlank() ? "вставить" : (v.length() > 10 ? v.substring(0, 10) + "…" : v), x + w - 2.0F * s, y + 9.0F * s, false);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isBlank()) {
                    text.set(clip.trim());
                    f.poke();
                }
            }, null);
        }
    }

    /** Short description of the module under the cursor. */
    private void drawTooltip(DrawContext g) {
        Feature f = this.hoverFeature;
        if (f == null || this.bindFeature != null || f.about() == null || f.about().isEmpty()) {
            return;
        }
        float s = this.s;
        float size = 6.2F * s;
        float tw = Math.max(Paint.tw(f.name(), 7.0F * s), Paint.tw(f.about(), size));
        float bw = Math.min(tw + 20.0F * s, this.width - 16.0F * s);
        float bh = 34.0F * s;
        float bx = Math.min(this.mx + 14.0F * s, this.width - bw - 8.0F * s);
        float by = Math.min(this.my + 14.0F * s, this.height - bh - 8.0F * s);
        Paint.box(g, bx, by, bw, bh, rgba(this.panelRgb, 0.94F), 6.0F * s);
        Paint.outline(g, bx, by, bw, bh, rgba(accent(), 0.5F), 6.0F * s);
        Paint.text(g, f.name(), bx + 10.0F * s, by + 6.0F * s, rgba(this.cOn, 1.0F), 7.0F * s);
        Paint.text(g, f.about(), bx + 10.0F * s, by + 19.0F * s, rgba(this.cOff, 1.0F), size);
        this.drawPreview(g, f, bx, by + bh + 6.0F * s, bw);
    }

    /**
     * A live preview under the tooltip, for three modules only: HUD (a small mock-up of its plates in the theme colours),
     * Cosmetics (the cape that is on) and Custom Swords (the knives, one after another).
     */
    private void drawPreview(DrawContext g, Feature f, float x, float y, float w) {
        String id = f.id();
        boolean hud = "hud".equals(id);
        boolean cosmetics = "cosmetics".equals(id);
        boolean knives = "knives".equals(id);
        if (!hud && !cosmetics && !knives) {
            return;
        }
        float s = this.s;
        float pw = Math.max(w, 150.0F * s);
        float ph = 78.0F * s;
        if (y + ph > this.height - 6.0F * s) {
            y = this.height - ph - 6.0F * s;
        }
        x = Math.min(x, this.width - pw - 8.0F * s);
        Paint.box(g, x, y, pw, ph, rgba(this.panelRgb, 0.94F), 6.0F * s);
        Paint.outline(g, x, y, pw, ph, rgba(accent(), 0.5F), 6.0F * s);
        if (hud) {
            // a watermark plate, a coordinates plate and a row of armor slots
            Paint.hudPlate(g, x + 8.0F * s, y + 8.0F * s, pw - 16.0F * s, 16.0F * s, Theme.HUD);
            Paint.text(g, "DooreVisuals", x + 14.0F * s, y + 12.0F * s, Theme.HUD, 6.0F * s);
            Paint.text(g, "Fps 240  Ping 12", x + 14.0F * s + Paint.tw("DooreVisuals", 6.0F * s) + 8.0F * s, y + 12.0F * s, Theme.TEXT, 6.0F * s);
            Paint.hudPlate(g, x + 8.0F * s, y + ph - 26.0F * s, 80.0F * s, 18.0F * s, Theme.HUD);
            Paint.text(g, "12x 64y -40z", x + 13.0F * s, y + ph - 21.0F * s, Theme.TEXT, 5.6F * s);
            for (int i = 0; i < 4; i++) {
                Paint.box(g, x + pw - 8.0F * s - (4 - i) * 16.0F * s, y + ph - 26.0F * s, 14.0F * s, 14.0F * s, rgba(0x000000, 0.35F), 3.0F * s);
                Paint.box(g, x + pw - 6.0F * s - (4 - i) * 16.0F * s, y + ph - 13.0F * s, 10.0F * s, 2.0F * s, rgba(Theme.OK, 1.0F), 1.0F * s);
            }
        } else if (cosmetics) {
            String label = App.features().find(dev.doorevisuals.cosmetics.CosmeticsFeature.class).map(dev.doorevisuals.cosmetics.CosmeticsFeature::capeLabel).orElse("Нет");
            Paint.text(g, "Плащ: " + label, x + 10.0F * s, y + 8.0F * s, rgba(this.cOn, 1.0F), 6.0F * s);
            net.minecraft.util.Identifier tex = dev.doorevisuals.cosmetics.CosmeticsFeature.previewTexture(label);
            if (tex != null) {
                final float tx = x + pw - 10.0F * s - 40.0F * s;
                final float ty = y + 8.0F * s;
                final float sc = s;
                this.afterFrame.add(() -> g.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, tex, (int)tx, (int)ty, 1.0F, 1.0F, (int)(40.0F * sc), (int)(64.0F * sc), 10, 16, 64, 32, -1));
            } else {
                Paint.text(g, "без плаща", x + 10.0F * s, y + 24.0F * s, rgba(this.cOff, 1.0F), 5.4F * s);
            }
            Paint.text(g, "Каталог плащей: в настройках модуля", x + 10.0F * s, y + ph - 14.0F * s, rgba(this.cOff, 1.0F), 4.8F * s);
        } else {
            int count = dev.doorevisuals.tools.SwordFeature.previewCount();
            int index = (int)((System.currentTimeMillis() / 1400L) % count);
            Paint.text(g, dev.doorevisuals.tools.SwordFeature.previewName(index), x + 10.0F * s, y + 8.0F * s, rgba(this.cOn, 1.0F), 6.4F * s);
            Paint.text(g, (index + 1) + " / " + count, x + 10.0F * s, y + 20.0F * s, rgba(this.cOff, 1.0F), 5.0F * s);
            final float ix = x + pw - 10.0F * s - 58.0F * s;
            final float iy = y + 7.0F * s;
            final float sc = Math.max(1.0F, s * 3.4F);
            net.minecraft.item.ItemStack stack = dev.doorevisuals.tools.SwordFeature.previewStack(index);
            this.afterFrame.add(() -> {
                g.getMatrices().pushMatrix();
                g.getMatrices().translate(ix, iy);
                g.getMatrices().scale(sc, sc);
                g.drawItem(stack, 0, 0);
                g.getMatrices().popMatrix();
            });
        }
    }

    /** Middle click on a row: the next key pressed becomes the module's bind. */
    private void startBind(Feature f) {
        if (f.toggleBind() == null) {
            return;
        }
        this.bindFeature = f;
        GuiSfx.click();
    }

    /** The option list of an open picker. Full names, the selected one in white. */
    private void drawDropdown(DrawContext g) {
        this.boxW = 0.0F;
        float dp = this.dropAnim.springOpen(this.dropPick != null ? 1.0F : 0.0F, this.dt);
        if (this.dropPick != null) {
            this.dropShown = this.dropPick;
        }
        Opt.Pick pick = this.dropShown;
        if (pick == null || dp < 0.01F) {
            return;
        }
        float s = this.s;
        Ui.alpha(this.frameAlpha * dp);
        List<String> options = pick.options();
        String cur = pick.get();
        float rowH = 18.0F * s;
        float pad = 4.0F * s;
        float textSize = 6.4F * s;
        float longest = 0.0F;
        for (String o : options) {
            longest = Math.max(longest, Paint.tw(o, textSize));
        }
        float bw = Math.max(this.dropWidth + 12.0F * s, longest + 22.0F * s);
        float bh = options.size() * rowH + pad * 2.0F;
        float bx = Math.max(4.0F * s, Math.min(this.dropLeft, this.width - bw - 4.0F * s));
        float by = this.dropBottom + 3.0F * s - (1.0F - dp) * 6.0F * s;
        if (by + bh > this.height - 6.0F * s) {
            by = this.dropTop - bh - 3.0F * s;
        }
        this.boxX = bx;
        this.boxY = by;
        this.boxW = bw;
        this.boxH = bh;

        Paint.box(g, bx, by, bw, bh, rgba(this.panelRgb, 0.97F), 6.0F * s);
        Paint.outline(g, bx, by, bw, bh, rgba(accent(), 0.5F), 6.0F * s);
        this.hits.add(new Hit(bx, by, bw, bh, () -> { }, null));
        for (int i = 0; i < options.size(); i++) {
            String o = options.get(i);
            float ry = by + pad + i * rowH;
            boolean sel = o.equals(cur);
            boolean hot = Paint.hit(this.mx, this.my, bx + 3.0F * s, ry, bw - 6.0F * s, rowH);
            if (sel) {
                Paint.box(g, bx + 3.0F * s, ry, bw - 6.0F * s, rowH, rgba(this.cOn, 1.0F), 3.0F * s);
            } else if (hot) {
                Paint.box(g, bx + 3.0F * s, ry, bw - 6.0F * s, rowH, rgba(this.cOn, 0.10F), 3.0F * s);
            }
            int color = sel ? rgba(this.panelRgb, 1.0F) : rgba(hot ? this.cOn : this.cOff, 1.0F);
            Paint.text(g, o, bx + 9.0F * s, ry + (rowH - textSize * 1.56F) / 2.0F, color, textSize);
            final Opt.Pick target = pick;
            final String value = o;
            this.hits.add(new Hit(bx + 3.0F * s, ry, bw - 6.0F * s, rowH, () -> {
                target.set(value);
                if (this.dropOwner != null) {
                    this.dropOwner.poke();
                }
                this.dropPick = null;
                GuiSfx.click();
            }, null));
        }
        Ui.alpha(this.frameAlpha);
    }

    private void applyDrag(double px) {
        DragNum d = this.drag;
        if (d == null || d.w() <= 0) {
            return;
        }
        double p = Math.max(0.0, Math.min(1.0, (px - d.x()) / d.w()));
        d.num().set(d.num().min() + p * (d.num().max() - d.num().min()));
        d.owner().poke();
    }

    // ---------- search, corner icon, input ----------

    private void drawSearch(DrawContext g, float[] r) {
        float s = this.s;
        float w = 260.0F * s;
        float h = 22.0F * s;
        float x = (this.width - w) / 2.0F;
        float y = r[9];
        Paint.box(g, x, y, w, h, rgba(this.panelRgb, 0.92F), 6.0F * s);
        boolean caret = (System.currentTimeMillis() / 500L) % 2L == 0L;
        String text = this.search.isEmpty() ? "Search..." : this.search + (caret ? "|" : "");
        Paint.textC(g, text, x + w / 2.0F, y + (h - 6.6F * s * 1.56F) / 2.0F, this.search.isEmpty() ? rgba(this.cOff, 1.0F) : rgba(this.cOn, 1.0F), 6.6F * s);
        if (!this.search.isEmpty()) {
            String info = this.matchCount == 0 ? "нет совпадений" : this.matchCount + " найдено";
            Paint.textR(g, info, x + w - 8.0F * s, y + (h - 5.4F * s * 1.56F) / 2.0F, rgba(this.matchCount == 0 ? this.cOff : accentHot(), 1.0F), 5.4F * s);
        }
        this.hits.add(new Hit(x, y, w, h, () -> { }, null));
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 2) {
            for (int i = this.midHits.size() - 1; i >= 0; i--) {
                Hit hit = this.midHits.get(i);
                if (Paint.hit(event.x(), event.y(), hit.x, hit.y, hit.w, hit.h)) {
                    hit.left.run();
                    return true;
                }
            }
            return super.mouseClicked(event, doubled);
        }
        boolean right = event.button() == 1;
        boolean left = event.button() == 0;
        if (!left && !right) {
            return super.mouseClicked(event, doubled);
        }
        this.clickX = event.x();
        if (this.capture != null) {
            this.capture = null;
            this.captureOwner = null;
        }
        if (this.dropPick != null && !Paint.hit(event.x(), event.y(), this.boxX, this.boxY, this.boxW, this.boxH)) {
            this.dropPick = null;
        }
        for (int i = this.hits.size() - 1; i >= 0; i--) {
            Hit hit = this.hits.get(i);
            if (Paint.hit(event.x(), event.y(), hit.x, hit.y, hit.w, hit.h)) {
                if (right) {
                    if (hit.right != null) {
                        hit.right.run();
                        return true;
                    }
                } else {
                    hit.left.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(Click event, double dx, double dy) {
        if (this.flyDrag != null) {
            this.applyFlyDrag(event.x());
            return true;
        }
        if (this.drag != null) {
            this.applyDrag(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click event) {
        this.drag = null;
        this.flyDrag = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        this.dropPick = null;
        if (this.banAnim.value() > 0.5F && mouseX >= this.flyX && mouseX <= this.flyX + this.flyW) {
            float flyMin = Math.min(0.0F, this.flyView - this.flyContent);
            this.flyTarget = Math.max(flyMin, Math.min(0.0F, this.flyTarget + (float)v * 40.0F * this.s));
            return true;
        }
        for (int i = 0; i < COLS.length; i++) {
            float cx = this.colXs[i];
            if (mouseX >= cx && mouseX <= cx + this.colWNow) {
                Category cat = COLS[i][0];
                float min = this.content.getOrDefault(cat, 0.0F);
                float next = this.scrollTarget.getOrDefault(cat, 0.0F) + (float)v * 40.0F * this.s;
                this.scrollTarget.put(cat, Math.max(min, Math.min(0.0F, next)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        int key = event.key();
        if (this.bindFeature != null) {
            Feature target = this.bindFeature;
            this.bindFeature = null;
            if (key != 256) {
                Opt.Key bind = target.toggleBind();
                if (bind != null) {
                    bind.set(key == 259 || key == 261 ? -1 : key);
                    target.poke();
                }
            }
            return true;
        }
        if (key == 256 && this.dropPick != null) {
            this.dropPick = null;
            return true;
        }
        if (this.capture != null) {
            if (key == 256) {
                this.capture = null;
                this.captureOwner = null;
                return true;
            }
            Opt.Key target = this.capture;
            Feature owner = this.captureOwner;
            target.set(key == 259 || key == 261 ? -1 : key);
            if (owner != null) {
                owner.poke();
            }
            this.capture = null;
            this.captureOwner = null;
            return true;
        }
        if (key == 256) {
            if (!this.search.isEmpty()) {
                this.search = "";
            } else if (this.mode != MODE_NONE) {
                this.closeAll();
            } else {
                this.requestClose();
            }
            return true;
        }
        if (key == 259 && !this.search.isEmpty()) {
            this.search = this.search.substring(0, this.search.length() - 1);
            this.jumpToMatches();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharInput event) {
        char c = (char)event.codepoint();
        if (this.capture == null && c >= ' ' && this.search.length() < 24) {
            this.search = this.search + c;
            // A search looks through every module, so all the plates slide out.
            java.util.Arrays.fill(this.colOpen, true);
            this.mode = MODE_MODULES;
            this.jumpToMatches();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
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
        if (this.client != null && this.client.currentScreen == this) {
            this.client.setScreen(this.parent);
        }
        BannerLibrary.unload();
        if (App.live()) {
            App.save().flush();
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

    private record DragNum(Opt.Num num, Feature owner, float x, float w) {
    }
}