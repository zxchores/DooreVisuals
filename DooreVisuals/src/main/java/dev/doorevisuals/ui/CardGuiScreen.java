package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.data.ThemesStore;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Keys;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.GuiWindowsFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

/**
 * Card layout of the menu: a rail of categories on the left, module cards in the middle and a settings
 * drawer on the right. Same modules, options and search as {@link ClickGuiScreen}, different shell.
 */
public final class CardGuiScreen extends Screen implements MenuGui {
    private static final int TAB_ALL = 0;
    private static final int TAB_THEMES = 5;
    private static final int TAB_CONFIGS = 6;
    private static final String[] TAB_NAMES = {"Все модули", "HUD", "Мир", "Косметика", "Игрок", "Темы", "Конфиги"};
    private static final Category[][] TAB_CATS = {
            {Category.OVERLAY, Category.WORLD, Category.COSMETICS, Category.TOOLS, Category.SYSTEM},
            {Category.OVERLAY},
            {Category.WORLD},
            {Category.COSMETICS},
            {Category.TOOLS, Category.SYSTEM},
    };
    private static final int[] SWATCHES = {
            0xFFFF5C7A, 0xFFFF8A4C, 0xFFFFC94D, 0xFFA3E635, 0xFF34D399, 0xFF22D3EE,
            0xFF60A5FA, 0xFF818CF8, 0xFFA78BFA, 0xFFD16BE0, 0xFFF472B6, 0xFFFFFFFF
    };
    private static final int BASE = 0x100F16;
    private static final int SHEET = 0x17151F;
    private static final int CARD = 0x1F1C2A;
    private static final int CARD_HOT = 0x282536;
    private static final int OFF = 0x8E8A9E;

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<Integer, List<Feature>> moduleLists = new HashMap<>();
    private final Map<String, Anim> hover = new HashMap<>();
    private final Map<String, Anim> toggle = new HashMap<>();
    private final Map<String, Anim> matchAnim = new HashMap<>();
    private final Anim openAnim = new Anim(0.0F);
    private final Anim drawerAnim = new Anim(0.0F);
    private final Anim searchAnim = new Anim(0.0F);
    private final long openedAt = System.currentTimeMillis();

    private int tab;
    private String search = "";
    private Feature selected;
    private Feature drawerShown;
    private Opt.Pick pickOpen;
    private Opt.Key capture;
    private Feature captureOwner;
    private DragNum drag;
    private int matchCount;
    private String railTip;
    private float railTipY;

    private float gridScroll;
    private float gridTarget;
    private float gridContent;
    private float gridView;
    private float drawScroll;
    private float drawTarget;
    private float drawContent;
    private float drawView;
    private float gridL;
    private float gridW;
    private float gridT;
    private float gridB;
    private float drawL;
    private float drawW;

    private float s = 1.0F;
    private float dt = 0.016F;
    private long lastNanos = System.nanoTime();
    private float frameAlpha = 1.0F;
    private int mx;
    private int my;
    private double clickX;
    private boolean closing;
    private boolean finished;

    public CardGuiScreen() {
        this(null);
    }

    public CardGuiScreen(Screen parent) {
        super(Text.literal("DooreVisuals"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (GuiWindowsFeature.soundOn()) {
            GuiSfx.open();
        }
    }

    // ---------- colour helpers ----------

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

    private float textY(float cy, float size) {
        return cy - size * 1.56F / 2.0F;
    }

    private String fit(String t, float size, float maxW) {
        if (Paint.tw(t, size) <= maxW) {
            return t;
        }
        String r = t;
        while (r.length() > 1 && Paint.tw(r + "…", size) > maxW) {
            r = r.substring(0, r.length() - 1);
        }
        return r + "…";
    }

    private List<String> wrap(String t, float size, float maxW, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : t.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (Paint.tw(next, size) > maxW && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
                if (lines.size() == maxLines) {
                    break;
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

    private Anim anim(Map<String, Anim> map, String key, float seed) {
        return map.computeIfAbsent(key, k -> new Anim(seed));
    }

    // ---------- modules ----------

    private int viewTab() {
        return this.search.isEmpty() ? this.tab : TAB_ALL;
    }

    private List<Feature> modules(int tabIndex) {
        // Sorted once per tab: the set of modules does not change while the menu is open.
        List<Feature> cached = this.moduleLists.get(tabIndex);
        if (cached != null) {
            return cached;
        }
        List<Feature> list = new ArrayList<>();
        for (Category c : TAB_CATS[tabIndex]) {
            for (Feature f : App.features().of(c)) {
                if (f.listed()) {
                    list.add(f);
                }
            }
        }
        list.sort((p, o) -> {
            int byCat = Integer.compare(tabOf(p), tabOf(o));
            return byCat != 0 ? byCat : p.name().compareToIgnoreCase(o.name());
        });
        this.moduleLists.put(tabIndex, list);
        return list;
    }

    private static int tabOf(Feature f) {
        return switch (f.category()) {
            case OVERLAY -> 1;
            case WORLD -> 2;
            case COSMETICS -> 3;
            default -> 4;
        };
    }

    private boolean matches(Feature f) {
        if (this.search.isEmpty()) {
            return false;
        }
        String q = this.search.toLowerCase(Locale.ROOT);
        return f.name().toLowerCase(Locale.ROOT).contains(q) || f.id().toLowerCase(Locale.ROOT).contains(q);
    }

    private int cols() {
        return this.gridW > 280.0F * this.s ? 3 : 2;
    }

    private float cardH() {
        return 42.0F * this.s;
    }

    private float gap() {
        return 5.0F * this.s;
    }

    private void jumpToMatches() {
        if (this.search.isEmpty()) {
            return;
        }
        List<Feature> list = this.modules(TAB_ALL);
        for (int i = 0; i < list.size(); i++) {
            if (this.matches(list.get(i))) {
                this.gridTarget = -(i / this.cols()) * (this.cardH() + this.gap());
                return;
            }
        }
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
        this.railTip = null;
        long now = System.nanoTime();
        this.dt = Math.max(0.004166667F, Math.min(0.033333335F, (float)(now - this.lastNanos) / 1.0E9F));
        this.lastNanos = now;
        Theme.tickTransition(this.dt);
        this.s = Math.max(0.55F, Math.min(1.25F, Math.min((this.width - 40.0F) / 480.0F, (this.height - 40.0F) / 290.0F)));

        boolean opening = !this.closing && System.currentTimeMillis() - this.openedAt > 20L;
        float a = opening ? this.openAnim.springOpen(1.0F, this.dt) : this.openAnim.springOpen(0.0F, this.dt);
        if (this.closing && a < 0.04F) {
            this.finish();
            return;
        }
        this.frameAlpha = clamp01(a);
        Ui.frame(g, () -> {
            Ui.alpha(this.frameAlpha);
            int dim = GuiWindowsFeature.dimAlpha();
            Paint.box(g, 0, 0, this.width, this.height, rgba(0x000000, Math.max(0.12F, dim / 255.0F) * a), 0.0F);
            this.drawPanel(g, a);
            Ui.alpha(1.0F);
        });
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawPanel(DrawContext g, float open) {
        float s = this.s;
        float pw = 480.0F * s;
        float ph = 290.0F * s;
        float px = (this.width - pw) / 2.0F;
        float py = (this.height - ph) / 2.0F + (1.0F - clamp01(open)) * 12.0F * s;
        float railW = 38.0F * s;

        Paint.box(g, px, py, pw, ph, rgba(BASE, 0.97F), 14.0F * s);
        Paint.outline(g, px, py, pw, ph, rgba(0xFFFFFF, 0.07F), 14.0F * s);
        float sheetX = px + railW;
        float sheetY = py + 6.0F * s;
        float sheetW = pw - railW - 6.0F * s;
        float sheetH = ph - 12.0F * s;
        Paint.box(g, sheetX, sheetY, sheetW, sheetH, rgba(SHEET, 1.0F), 10.0F * s);

        this.drawRail(g, px, py, railW, ph);

        boolean moduleTab = this.tab != TAB_THEMES && this.tab != TAB_CONFIGS;
        boolean wantDrawer = this.selected != null && (moduleTab || !this.search.isEmpty());
        float da = this.drawerAnim.springPanel(wantDrawer ? 1.0F : 0.0F, this.dt);
        if (wantDrawer) {
            this.drawerShown = this.selected;
        }
        float drawerFull = 150.0F * s;
        this.drawW = drawerFull;
        this.drawL = sheetX + sheetW - drawerFull * clamp01(da) - 6.0F * s * clamp01(da);
        float pad = 8.0F * s;
        this.gridL = sheetX + pad;
        this.gridW = sheetW - pad * 2.0F - (drawerFull + 6.0F * s) * clamp01(da);
        this.gridT = sheetY + pad + 18.0F * s + 6.0F * s;
        this.gridB = sheetY + sheetH - pad;

        this.drawSearch(g, this.gridL, sheetY + pad, this.gridW, 18.0F * s);
        this.searchAnim.springHover(this.search.isEmpty() ? 0.0F : 1.0F, this.dt);
        this.matchCount = 0;
        if (this.tab == TAB_THEMES && this.search.isEmpty()) {
            this.drawThemeCards(g);
        } else if (this.tab == TAB_CONFIGS && this.search.isEmpty()) {
            this.drawConfigCards(g);
        } else {
            this.drawModuleCards(g);
        }
        if (da > 0.02F && this.drawerShown != null) {
            this.drawDrawer(g, sheetY + 6.0F * s, sheetH - 12.0F * s, da);
        }
        this.drawRailTip(g);
    }

    // ---------- rail ----------

    private void drawRail(DrawContext g, float px, float py, float railW, float ph) {
        float s = this.s;
        float item = 26.0F * s;
        float x = px + (railW - item) / 2.0F - 2.0F * s;
        float y = py + 10.0F * s;
        int active = this.search.isEmpty() ? this.tab : TAB_ALL;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            // modules from the top, themes and configs pinned to the bottom edge
            float iy = i < TAB_THEMES
                    ? y + i * (item + 3.0F * s)
                    : py + ph - 10.0F * s - item - (TAB_CONFIGS - i) * (item + 3.0F * s);
            boolean sel = i == active;
            boolean hot = Paint.hit(this.mx, this.my, x, iy, item, item);
            float hv = this.anim(this.hover, "rail" + i, 0.0F).springHover(hot ? 1.0F : 0.0F, this.dt);
            if (sel) {
                Paint.box(g, x, iy, item, item, rgba(accent(), 0.20F), 10.0F * s);
                Paint.box(g, px + 2.0F * s, iy + item / 2.0F - 8.0F * s, 3.0F * s, 16.0F * s, rgba(accentHot(), 1.0F), 1.5F * s);
            } else if (hv > 0.01F) {
                Paint.box(g, x, iy, item, item, rgba(0xFFFFFF, 0.06F * hv), 10.0F * s);
            }
            int color = sel ? rgba(accentHot(), 1.0F) : rgba(mix(OFF, 0xFFFFFF, hv), 1.0F);
            this.railIcon(g, i, x + item / 2.0F, iy + item / 2.0F, color);
            if (hot) {
                this.railTip = TAB_NAMES[i];
                this.railTipY = iy + item / 2.0F;
            }
            final int target = i;
            this.hits.add(new Hit(x, iy, item, item, () -> {
                this.tab = target;
                this.search = "";
                this.gridTarget = 0.0F;
                this.pickOpen = null;
                GuiSfx.click();
            }, null, null));
        }
    }

    private void railIcon(DrawContext g, int tabIndex, float cx, float cy, int c) {
        float u = this.s;
        switch (tabIndex) {
            case TAB_ALL -> {
                for (int i = 0; i < 4; i++) {
                    Paint.box(g, cx - 7.0F * u + (i % 2) * 8.0F * u, cy - 7.0F * u + (i / 2) * 8.0F * u, 6.0F * u, 6.0F * u, c, 2.0F * u);
                }
            }
            case 1 -> {
                Paint.outline(g, cx - 8.0F * u, cy - 6.0F * u, 16.0F * u, 12.0F * u, c, 3.0F * u);
                Paint.box(g, cx - 5.0F * u, cy - 2.0F * u, 7.0F * u, 2.0F * u, c, 1.0F * u);
                Paint.box(g, cx - 5.0F * u, cy + 1.5F * u, 10.0F * u, 2.0F * u, c, 1.0F * u);
            }
            case 2 -> {
                Paint.ring(g, cx, cy, 7.5F * u, 1.6F * u, c);
                Paint.circle(g, cx + 2.0F * u, cy - 1.5F * u, 2.4F * u, c);
            }
            case 3 -> {
                Paint.box(g, cx - 0.8F * u, cy - 7.0F * u, 1.6F * u, 14.0F * u, c, 0.8F * u);
                Paint.box(g, cx - 7.0F * u, cy - 0.8F * u, 14.0F * u, 1.6F * u, c, 0.8F * u);
                Paint.circle(g, cx + 5.0F * u, cy - 5.0F * u, 1.8F * u, c);
                Paint.circle(g, cx - 5.0F * u, cy + 5.0F * u, 1.4F * u, c);
            }
            case 4 -> {
                Paint.circle(g, cx, cy - 3.5F * u, 3.4F * u, c);
                Paint.box(g, cx - 6.0F * u, cy + 1.0F * u, 12.0F * u, 6.5F * u, c, 3.2F * u);
            }
            case TAB_THEMES -> {
                Paint.circle(g, cx - 2.5F * u, cy, 5.2F * u, (c & 0xFFFFFF) | 0xE6000000);
                Paint.circle(g, cx + 2.8F * u, cy, 5.2F * u, (c & 0xFFFFFF) | 0x80000000);
            }
            default -> {
                for (int i = 0; i < 3; i++) {
                    float ly = cy - 5.0F * u + i * 5.0F * u;
                    Paint.box(g, cx - 7.0F * u, ly - 0.7F * u, 14.0F * u, 1.4F * u, c, 0.7F * u);
                    Paint.circle(g, cx - 3.5F * u + ((i * 5) % 7) * u, ly, 2.0F * u, c);
                }
            }
        }
    }

    private void drawRailTip(DrawContext g) {
        if (this.railTip == null) {
            return;
        }
        float s = this.s;
        float size = 5.0F * s;
        float w = Paint.tw(this.railTip, size) + 14.0F * s;
        float h = 17.0F * s;
        float x = (this.width - 480.0F * s) / 2.0F + 40.0F * s;
        float y = this.railTipY - h / 2.0F;
        Paint.box(g, x, y, w, h, rgba(0x0B0A12, 0.96F), 6.0F * s);
        Paint.outline(g, x, y, w, h, rgba(0xFFFFFF, 0.10F), 6.0F * s);
        Paint.text(g, this.railTip, x + 7.0F * s, this.textY(y + h / 2.0F, size), rgba(0xFFFFFF, 1.0F), size);
    }

    // ---------- search ----------

    private void drawSearch(DrawContext g, float x, float y, float w, float h) {
        float s = this.s;
        Paint.box(g, x, y, w, h, rgba(BASE, 1.0F), 8.0F * s);
        float ix = x + 12.0F * s;
        float cy = y + h / 2.0F;
        int c = rgba(this.search.isEmpty() ? OFF : accentHot(), 1.0F);
        Paint.ring(g, ix, cy - 0.8F * s, 3.6F * s, 1.3F * s, c);
        Paint.box(g, ix + 2.4F * s, cy + 2.0F * s, 3.4F * s, 1.3F * s, c, 0.6F * s);
        float size = 5.6F * s;
        boolean caret = (System.currentTimeMillis() / 500L) % 2L == 0L;
        String text = this.search.isEmpty() ? "Поиск модулей" : this.search + (caret ? "|" : "");
        Paint.text(g, text, x + 26.0F * s, this.textY(cy, size), rgba(this.search.isEmpty() ? OFF : 0xFFFFFF, 1.0F), size);
        if (!this.search.isEmpty()) {
            String info = this.matchCount == 0 ? "нет совпадений" : this.matchCount + " найдено";
            Paint.textR(g, info, x + w - 10.0F * s, this.textY(cy, 4.8F * s), rgba(this.matchCount == 0 ? OFF : accentHot(), 1.0F), 4.8F * s);
        }
    }

    // ---------- cards ----------

    private void drawModuleCards(DrawContext g) {
        float s = this.s;
        List<Feature> list = this.modules(this.viewTab());
        int cols = this.cols();
        float gap = this.gap();
        float cw = (this.gridW - gap * (cols - 1)) / cols;
        float ch = this.cardH();
        int rows = (list.size() + cols - 1) / cols;
        this.beginGrid(rows * (ch + gap) - gap);
        float searching = clamp01(this.searchAnim.value());
        if (list.isEmpty()) {
            Paint.textC(g, "Пусто", this.gridL + this.gridW / 2.0F, this.gridT + 12.0F * s, rgba(OFF, 1.0F), 5.6F * s);
        }
        for (int i = 0; i < list.size(); i++) {
            Feature f = list.get(i);
            float x = this.gridL + (i % cols) * (cw + gap);
            float y = this.gridT + this.gridScroll + (i / cols) * (ch + gap);
            boolean hit = this.matches(f);
            if (hit) {
                this.matchCount++;
            }
            if (y + ch < this.gridT - 4.0F * s || y > this.gridB + 4.0F * s) {
                continue;
            }
            float lit = this.anim(this.matchAnim, f.id(), 0.0F).springHover(hit ? 1.0F : 0.0F, this.dt);
            float fade = 1.0F - 0.62F * searching * (1.0F - clamp01(lit));
            Ui.alpha(this.frameAlpha * fade);
            this.drawModuleCard(g, f, x, y, cw, ch, lit);
            Ui.alpha(this.frameAlpha);
        }
        this.endGrid();
    }

    private void drawModuleCard(DrawContext g, Feature f, float x, float y, float w, float h, float lit) {
        float s = this.s;
        boolean on = f.on();
        boolean inView = this.my >= this.gridT && this.my <= this.gridB;
        boolean hot = inView && Paint.hit(this.mx, this.my, x, y, w, h);
        float hv = this.anim(this.hover, f.id(), 0.0F).springHover(hot ? 1.0F : 0.0F, this.dt);
        float tg = this.anim(this.toggle, f.id(), on ? 1.0F : 0.0F).springToggle(on ? 1.0F : 0.0F, this.dt);
        boolean sel = f == this.selected;

        Paint.box(g, x, y, w, h, rgba(mix(CARD, CARD_HOT, hv), 1.0F), 10.0F * s);
        if (tg > 0.01F) {
            Paint.outline(g, x, y, w, h, rgba(accent(), 0.35F * tg), 10.0F * s);
        }
        if (sel) {
            Paint.outline(g, x - 1.0F * s, y - 1.0F * s, w + 2.0F * s, h + 2.0F * s, rgba(accentHot(), 1.0F), 11.0F * s);
        }
        if (lit > 0.01F) {
            Paint.outline(g, x - 1.0F * s, y - 1.0F * s, w + 2.0F * s, h + 2.0F * s, rgba(0xFFFFFF, 0.9F * lit), 11.0F * s);
            Paint.outline(g, x - 3.0F * s, y - 3.0F * s, w + 6.0F * s, h + 6.0F * s, rgba(accentHot(), 0.45F * lit), 13.0F * s);
        }

        this.monogram(g, f, x + 10.0F * s, y + 7.0F * s, 15.0F * s);
        this.switchPill(g, x + w - 10.0F * s - 20.0F * s, y + 8.5F * s, 20.0F * s, 11.0F * s, tg);

        float nameSize = 5.3F * s;
        float nameY = y + h - 19.0F * s;
        float nameMax = w - 20.0F * s - (f.visibleOpts().isEmpty() ? 0.0F : 14.0F * s);
        String name = f.name();
        int at = this.search.isEmpty() ? -1 : name.toLowerCase(Locale.ROOT).indexOf(this.search.toLowerCase(Locale.ROOT));
        int nameColor = rgba(mix(0xD8D5E4, 0xFFFFFF, Math.max(tg, hv)), 1.0F);
        if (at >= 0 && lit > 0.01F && Paint.tw(name, nameSize) <= nameMax) {
            String head = name.substring(0, at);
            String mid = name.substring(at, at + this.search.length());
            String tail = name.substring(at + this.search.length());
            float hx = x + 10.0F * s + Paint.tw(head, nameSize);
            Paint.text(g, head, x + 10.0F * s, nameY, nameColor, nameSize);
            Paint.text(g, mid, hx, nameY, rgba(mix(0xFFFFFF, 0xFFE27A, lit), 1.0F), nameSize);
            Paint.text(g, tail, hx + Paint.tw(mid, nameSize), nameY, nameColor, nameSize);
        } else {
            Paint.text(g, this.fit(name, nameSize, nameMax), x + 10.0F * s, nameY, nameColor, nameSize);
        }

        float subSize = 4.2F * s;
        float subY = y + h - 10.0F * s;
        String cat = TAB_NAMES[tabOf(f)];
        Paint.text(g, cat, x + 10.0F * s, subY, rgba(OFF, 1.0F), subSize);
        Badge badge = f.badge();
        float bx = x + 10.0F * s + Paint.tw(cat, subSize) + 6.0F * s;
        if (badge != null) {
            float bw = Paint.tw(badge.label(), 4.0F * s) + 7.0F * s;
            float bh = 9.0F * s;
            if (bx + bw < x + w - 30.0F * s) {
                Paint.box(g, bx, subY + subSize * 0.78F - bh / 2.0F, bw, bh, rgba(0x0B0A12, 0.62F), 4.0F * s);
                Paint.outline(g, bx, subY + subSize * 0.78F - bh / 2.0F, bw, bh, rgba(badge.rgb(), 0.7F), 4.0F * s);
                Paint.textC(g, badge.label(), bx + bw / 2.0F, this.textY(subY + subSize * 0.78F, 4.0F * s), rgba(badge.rgb(), 1.0F), 4.0F * s);
            }
        }
        Opt.Key bind = f.toggleBind();
        if (bind != null && bind.get() != -1) {
            Paint.textR(g, Keys.name(bind.get()), x + w - 10.0F * s, subY, rgba(OFF, 1.0F), subSize);
        }

        boolean hasOpts = !f.visibleOpts().isEmpty();
        float top = Math.max(y, this.gridT);
        float bottom = Math.min(y + h, this.gridB);
        if (bottom > top) {
            Runnable open = () -> this.select(f);
            this.hits.add(new Hit(x, top, w, bottom - top, () -> {
                f.flip();
                this.anim(this.toggle, f.id(), 0.0F).snap(f.on() ? 1.0F : 0.0F);
                GuiSfx.toggle(f.on());
            }, open, () -> this.startBind(f)));
            if (hasOpts) {
                float dx = x + w - 24.0F * s;
                float dy = y + h - 19.0F * s;
                float dh = Math.min(dy + 20.0F * s, bottom) - Math.max(dy, top);
                int dots = rgba(sel ? accentHot() : mix(OFF, 0xFFFFFF, hv), 1.0F);
                for (int i = -1; i <= 1; i++) {
                    Paint.circle(g, x + w - 14.0F * s + i * 3.4F * s, y + h - 14.0F * s, 1.1F * s, dots);
                }
                if (dh > 0.0F) {
                    this.hits.add(new Hit(dx, Math.max(dy, top), 24.0F * s, dh, open, open, null));
                }
            }
        }
    }

    private void monogram(DrawContext g, Feature f, float x, float y, float size) {
        int base = SWATCHES[Math.abs(f.id().hashCode()) % SWATCHES.length] & 0xFFFFFF;
        Paint.box(g, x, y, size, size, rgba(base, 0.20F), 6.0F * this.s);
        String letter = f.name().isEmpty() ? "?" : f.name().substring(0, 1).toUpperCase(Locale.ROOT);
        float ts = 6.4F * this.s;
        Paint.textC(g, letter, x + size / 2.0F, this.textY(y + size / 2.0F, ts), rgba(base, 1.0F), ts);
    }

    private void switchPill(DrawContext g, float x, float y, float w, float h, float t) {
        float k = clamp01(t);
        Paint.box(g, x, y, w, h, rgba(mix(0x3A3748, accent(), k), 1.0F), h / 2.0F);
        float r = h / 2.0F - 2.0F * this.s;
        Paint.circle(g, x + h / 2.0F + k * (w - h), y + h / 2.0F, r, rgba(mix(0xA7A3B8, 0xFFFFFF, k), 1.0F));
    }

    private void select(Feature f) {
        if (this.selected == f) {
            this.selected = null;
        } else {
            this.selected = f;
            this.drawTarget = 0.0F;
            this.drawScroll = 0.0F;
            this.pickOpen = null;
        }
        GuiSfx.click();
    }

    private void startBind(Feature f) {
        Opt.Key bind = f.toggleBind();
        if (bind != null) {
            this.capture = bind;
            this.captureOwner = f;
            GuiSfx.click();
        }
    }

    // ---------- grid scroll ----------

    private void beginGrid(float content) {
        this.gridView = this.gridB - this.gridT;
        this.gridContent = content;
        float min = Math.min(0.0F, this.gridView - content);
        this.gridTarget = Math.max(min, Math.min(0.0F, this.gridTarget));
        this.gridScroll += (this.gridTarget - this.gridScroll) * (1.0F - (float)Math.exp(-this.dt * 14.0F));
        this.gridScroll = Math.max(min - 20.0F, Math.min(20.0F, this.gridScroll));
        Ui.scissor(this.gridL - 4.0F * this.s, this.gridT - 4.0F * this.s, this.gridW + 8.0F * this.s, this.gridView + 8.0F * this.s);
    }

    private void endGrid() {
        Ui.unscissor();
    }

    // ---------- themes and configs ----------

    private void drawThemeCards(DrawContext g) {
        float s = this.s;
        ThemesStore.MixedPreset[] presets = ThemesStore.MIXED;
        int cols = this.cols();
        float gap = this.gap();
        float cw = (this.gridW - gap * (cols - 1)) / cols;
        float ch = 34.0F * s;
        int total = presets.length + 1;
        this.beginGrid(((total + cols - 1) / cols) * (ch + gap) - gap);
        int current = -1;
        for (int i = 0; i < presets.length; i++) {
            if ((presets[i].primary() & 0xFFFFFF) == accent()) {
                current = i;
            }
        }
        for (int i = 0; i < total; i++) {
            float x = this.gridL + (i % cols) * (cw + gap);
            float y = this.gridT + this.gridScroll + (i / cols) * (ch + gap);
            if (y + ch < this.gridT - 4.0F * s || y > this.gridB + 4.0F * s) {
                continue;
            }
            boolean random = i == presets.length;
            int color = random ? 0xFFFFFF : presets[i].primary() & 0xFFFFFF;
            String label = random ? "Случайная" : presets[i].label();
            final int idx = i;
            Runnable action = random
                    ? () -> this.applyPreset(presets[ThreadLocalRandom.current().nextInt(presets.length)])
                    : () -> this.applyPreset(presets[idx]);
            this.infoCard(g, "theme" + i, x, y, cw, ch, label, random ? "Сюрприз" : "Тема", color, !random && i == current, action);
        }
        this.endGrid();
    }

    private void applyPreset(ThemesStore.MixedPreset preset) {
        App.features().find(ThemeFeature.class).ifPresent(tf -> tf.applyMixed(preset));
        GuiSfx.click();
    }

    private void drawConfigCards(DrawContext g) {
        float s = this.s;
        ConfigFeature cfg = App.features().find(ConfigFeature.class).orElse(null);
        List<String> names = cfg == null ? new ArrayList<>() : cfg.list();
        String active = ConfigFeature.activeName();
        int cols = this.cols();
        float gap = this.gap();
        float cw = (this.gridW - gap * (cols - 1)) / cols;
        float ch = 34.0F * s;
        int total = names.size() + 3;
        this.beginGrid(((total + cols - 1) / cols) * (ch + gap) - gap);
        for (int i = 0; i < total; i++) {
            float x = this.gridL + (i % cols) * (cw + gap);
            float y = this.gridT + this.gridScroll + (i / cols) * (ch + gap);
            if (y + ch < this.gridT - 4.0F * s || y > this.gridB + 4.0F * s) {
                continue;
            }
            if (i < 3) {
                String[] labels = {"Новый", "Сохранить", "Открыть папку"};
                String[] subs = {"Создать конфиг", "Текущие настройки", "В проводнике"};
                final int which = i;
                this.infoCard(g, "cfgbtn" + i, x, y, cw, ch, labels[i], subs[i], accent(), false, () -> {
                    if (which == 0 && this.client != null) {
                        this.client.setScreen(new ConfigManagerScreen(this));
                    } else if (cfg != null && which == 1) {
                        cfg.saveDraft();
                        GuiSfx.click();
                    } else if (cfg != null) {
                        cfg.openConfigsFolder();
                    }
                });
            } else {
                String n = names.get(i - 3);
                this.infoCard(g, "cfg" + n, x, y, cw, ch, n, "Конфиг", 0xA78BFA, n.equalsIgnoreCase(active), () -> {
                    if (cfg != null) {
                        cfg.applyNamed(n);
                        GuiSfx.click();
                    }
                });
            }
        }
        this.endGrid();
    }

    private void infoCard(DrawContext g, String key, float x, float y, float w, float h, String title, String sub, int color, boolean selected, Runnable action) {
        float s = this.s;
        boolean hot = this.my >= this.gridT && this.my <= this.gridB && Paint.hit(this.mx, this.my, x, y, w, h);
        float hv = this.anim(this.hover, key, 0.0F).springHover(hot ? 1.0F : 0.0F, this.dt);
        Paint.box(g, x, y, w, h, rgba(mix(CARD, CARD_HOT, hv), 1.0F), 10.0F * s);
        if (selected) {
            Paint.outline(g, x - 1.0F * s, y - 1.0F * s, w + 2.0F * s, h + 2.0F * s, rgba(accentHot(), 1.0F), 11.0F * s);
        }
        Paint.box(g, x + 10.0F * s, y + h / 2.0F - 9.0F * s, 18.0F * s, 18.0F * s, rgba(color, 1.0F), 6.0F * s);
        float size = 5.6F * s;
        Paint.text(g, this.fit(title, size, w - 48.0F * s), x + 36.0F * s, y + h / 2.0F - 10.0F * s, rgba(0xFFFFFF, 1.0F), size);
        Paint.text(g, sub, x + 36.0F * s, y + h / 2.0F + 2.5F * s, rgba(OFF, 1.0F), 4.5F * s);
        float top = Math.max(y, this.gridT);
        float bottom = Math.min(y + h, this.gridB);
        if (bottom > top) {
            this.hits.add(new Hit(x, top, w, bottom - top, action, null, null));
        }
    }

    // ---------- settings drawer ----------

    private void drawDrawer(DrawContext g, float y, float h, float da) {
        float s = this.s;
        Feature f = this.drawerShown;
        float x = this.drawL;
        float w = this.drawW;
        Ui.alpha(this.frameAlpha * clamp01(da));
        Paint.box(g, x, y, w, h, rgba(CARD, 1.0F), 10.0F * s);
        float pad = 8.0F * s;
        float ix = x + pad;
        float iw = w - pad * 2.0F;

        this.monogram(g, f, ix, y + pad, 15.0F * s);
        float titleSize = 6.2F * s;
        Paint.text(g, this.fit(f.name(), titleSize, iw - 38.0F * s), ix + 21.0F * s, y + pad + 1.0F * s, rgba(0xFFFFFF, 1.0F), titleSize);
        Paint.text(g, TAB_NAMES[tabOf(f)], ix + 21.0F * s, y + pad + 11.0F * s, rgba(OFF, 1.0F), 4.5F * s);
        boolean closeHot = Paint.hit(this.mx, this.my, x + w - 26.0F * s, y + 8.0F * s, 20.0F * s, 20.0F * s);
        int cc = rgba(closeHot ? 0xFFFFFF : OFF, 1.0F);
        Paint.box(g, x + w - 19.0F * s, y + 17.0F * s, 8.0F * s, 1.4F * s, cc, 0.7F * s);
        this.hits.add(new Hit(x + w - 26.0F * s, y + 8.0F * s, 20.0F * s, 20.0F * s, () -> {
            this.selected = null;
            GuiSfx.click();
        }, null, null));

        float cy = y + pad + 21.0F * s;
        if (f.about() != null && !f.about().isEmpty()) {
            float as = 4.7F * s;
            for (String line : this.wrap(f.about(), as, iw, 4)) {
                Paint.text(g, line, ix, cy, rgba(OFF, 1.0F), as);
                cy += 8.0F * s;
            }
            cy += 4.0F * s;
        }
        Paint.box(g, ix, cy, iw, 1.0F, rgba(0xFFFFFF, 0.07F), 0.0F);
        cy += 8.0F * s;

        float on = this.anim(this.toggle, f.id(), f.on() ? 1.0F : 0.0F).springToggle(f.on() ? 1.0F : 0.0F, this.dt);
        Paint.text(g, "Включён", ix, this.textY(cy + 7.0F * s, 5.4F * s), rgba(0xFFFFFF, 1.0F), 5.4F * s);
        this.switchPill(g, ix + iw - 24.0F * s, cy + 0.5F * s, 24.0F * s, 13.0F * s, on);
        this.hits.add(new Hit(ix, cy, iw, 14.0F * s, () -> {
            f.flip();
            GuiSfx.toggle(f.on());
        }, null, null));
        cy += 22.0F * s;

        float resetH = 18.0F * s;
        float viewTop = cy;
        float viewBottom = y + h - pad - resetH - 8.0F * s;
        this.drawView = viewBottom - viewTop;
        float min = Math.min(0.0F, this.drawView - this.drawContent);
        this.drawTarget = Math.max(min, Math.min(0.0F, this.drawTarget));
        this.drawScroll += (this.drawTarget - this.drawScroll) * (1.0F - (float)Math.exp(-this.dt * 14.0F));
        Ui.scissor(x, viewTop, w, this.drawView);
        float oy = viewTop + this.drawScroll;
        List<Opt<?>> opts = f.visibleOpts();
        for (Opt<?> opt : opts) {
            oy += this.drawOpt(g, f, opt, ix, oy, iw, viewTop, viewBottom);
        }
        this.drawContent = oy - (viewTop + this.drawScroll);
        Ui.unscissor();
        if (opts.isEmpty()) {
            Paint.text(g, "Настроек нет", ix, viewTop + 2.0F * s, rgba(OFF, 1.0F), 4.8F * s);
        }

        float by = y + h - pad - resetH;
        boolean hot = Paint.hit(this.mx, this.my, ix, by, iw, resetH);
        Paint.box(g, ix, by, iw, resetH, rgba(hot ? accent() : 0x2B2738, hot ? 0.95F : 1.0F), 6.0F * s);
        Paint.textC(g, "Сбросить настройки", ix + iw / 2.0F, this.textY(by + resetH / 2.0F, 5.0F * s), rgba(0xFFFFFF, 1.0F), 5.0F * s);
        this.hits.add(new Hit(ix, by, iw, resetH, () -> {
            for (Opt<?> o : f.opts()) {
                if (!(o instanceof Opt.Action) && !(o instanceof Opt.Key)) {
                    o.reset();
                }
            }
            f.poke();
            GuiSfx.click();
        }, null, null));
        Ui.alpha(this.frameAlpha);
    }

    /** Draws one option row and returns the height it took. */
    private float drawOpt(DrawContext g, Feature f, Opt<?> opt, float x, float y, float w, float vy0, float vy1) {
        float s = this.s;
        float ts = 5.0F * s;
        int muted = rgba(OFF, 1.0F);
        int white = rgba(0xFFFFFF, 1.0F);
        if (opt instanceof Opt.Action action) {
            float h = 22.0F * s;
            boolean hot = this.over(x, y, w, h - 4.0F * s, vy0, vy1);
            Paint.box(g, x, y, w, h - 4.0F * s, rgba(accent(), hot ? 0.95F : 0.75F), 6.0F * s);
            Paint.textC(g, opt.label(), x + w / 2.0F, this.textY(y + (h - 4.0F * s) / 2.0F, ts), white, ts);
            this.addHit(x, y, w, h - 4.0F * s, vy0, vy1, () -> {
                GuiSfx.click();
                action.run();
            }, null);
            return h;
        }
        if (opt instanceof Opt.Flag flag) {
            float h = 20.0F * s;
            boolean v = flag.get();
            float t = this.anim(this.toggle, "opt" + f.id() + opt.id(), v ? 1.0F : 0.0F).springToggle(v ? 1.0F : 0.0F, this.dt);
            Paint.text(g, this.fit(opt.label(), ts, w - 34.0F * s), x, this.textY(y + h / 2.0F, ts), v ? white : muted, ts);
            this.switchPill(g, x + w - 22.0F * s, y + h / 2.0F - 5.5F * s, 22.0F * s, 11.0F * s, t);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                flag.flip();
                f.poke();
                GuiSfx.toggle(flag.get());
            }, null);
            return h;
        }
        if (opt instanceof Opt.Num num) {
            float h = 28.0F * s;
            String value = String.format(Locale.ROOT, "%.2f", num.get()).replaceAll("0+$", "").replaceAll("\\.$", "");
            Paint.text(g, this.fit(opt.label(), ts, w - 34.0F * s), x, y + 2.0F * s, muted, ts);
            Paint.textR(g, value, x + w, y + 2.0F * s, white, ts);
            float p = (float)((num.get() - num.min()) / (num.max() - num.min()));
            float ty = y + 17.0F * s;
            Paint.box(g, x, ty, w, 3.5F * s, rgba(0xFFFFFF, 0.12F), 1.75F * s);
            Paint.box(g, x, ty, w * clamp01(p), 3.5F * s, rgba(accentHot(), 1.0F), 1.75F * s);
            Paint.circle(g, x + w * clamp01(p), ty + 1.75F * s, 4.0F * s, white);
            this.addHit(x - 3.0F * s, y + 10.0F * s, w + 6.0F * s, 16.0F * s, vy0, vy1, () -> {
                this.drag = new DragNum(num, f, x, w);
                this.applyDrag(this.clickX);
            }, null);
            return h;
        }
        if (opt instanceof Opt.Pick pick) {
            float h = 20.0F * s;
            boolean open = this.pickOpen == pick;
            String cur = pick.get();
            Paint.text(g, this.fit(opt.label(), ts, w * 0.5F), x, this.textY(y + h / 2.0F, ts), muted, ts);
            float pw = Math.min(w * 0.55F, Paint.tw(cur, ts) + 14.0F * s);
            Paint.box(g, x + w - pw, y + 3.0F * s, pw, h - 6.0F * s, rgba(open ? accent() : 0x2B2738, 1.0F), 5.0F * s);
            Paint.textC(g, this.fit(cur, ts, pw - 8.0F * s), x + w - pw / 2.0F, this.textY(y + h / 2.0F, ts), white, ts);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                this.pickOpen = open ? null : pick;
                GuiSfx.click();
            }, null);
            float used = h;
            if (open) {
                float rowH = 16.0F * s;
                for (String o : pick.options()) {
                    boolean sel = o.equals(cur);
                    boolean hot = this.over(x, y + used, w, rowH, vy0, vy1);
                    if (sel) {
                        Paint.box(g, x, y + used, w, rowH, rgba(0xFFFFFF, 1.0F), 4.0F * s);
                    } else if (hot) {
                        Paint.box(g, x, y + used, w, rowH, rgba(0xFFFFFF, 0.10F), 4.0F * s);
                    }
                    Paint.text(g, this.fit(o, ts, w - 12.0F * s), x + 6.0F * s, this.textY(y + used + rowH / 2.0F, ts), sel ? rgba(0x0B0A12, 1.0F) : (hot ? white : muted), ts);
                    this.addHit(x, y + used, w, rowH, vy0, vy1, () -> {
                        pick.set(o);
                        f.poke();
                        this.pickOpen = null;
                        GuiSfx.click();
                    }, null);
                    used += rowH;
                }
                used += 4.0F * s;
            }
            return used;
        }
        if (opt instanceof Opt.Tint tint) {
            int cur = tint.get() & 0xFFFFFF;
            float d = 13.0F * s;
            int perRow = Math.max(1, (int)((w + 5.0F * s) / (d + 5.0F * s)));
            int rows = (SWATCHES.length + perRow - 1) / perRow;
            Paint.text(g, this.fit(opt.label(), ts, w), x, y + 2.0F * s, muted, ts);
            for (int i = 0; i < SWATCHES.length; i++) {
                float cx = x + d / 2.0F + (i % perRow) * (d + 5.0F * s);
                float cy2 = y + 16.0F * s + d / 2.0F + (i / perRow) * (d + 5.0F * s);
                int rgb = SWATCHES[i] & 0xFFFFFF;
                Paint.circle(g, cx, cy2, d / 2.0F, rgba(rgb, 1.0F));
                if (rgb == cur) {
                    Paint.ring(g, cx, cy2, d / 2.0F + 2.0F * s, 1.2F * s, white);
                }
                this.addHit(cx - d / 2.0F, cy2 - d / 2.0F, d, d, vy0, vy1, () -> {
                    int alpha = tint.get() & 0xFF000000;
                    tint.set((alpha == 0 ? 0xFF000000 : alpha) | rgb);
                    f.poke();
                    GuiSfx.click();
                }, null);
            }
            return 16.0F * s + rows * (d + 5.0F * s) + 4.0F * s;
        }
        if (opt instanceof Opt.Key key) {
            float h = 20.0F * s;
            boolean waiting = this.capture == key;
            String cur = waiting ? "…" : Keys.name(key.get());
            Paint.text(g, this.fit(opt.label(), ts, w * 0.5F), x, this.textY(y + h / 2.0F, ts), muted, ts);
            float pw = Paint.tw(cur, ts) + 14.0F * s;
            Paint.box(g, x + w - pw, y + 3.0F * s, pw, h - 6.0F * s, rgba(waiting ? accent() : 0x2B2738, 1.0F), 5.0F * s);
            Paint.textC(g, cur, x + w - pw / 2.0F, this.textY(y + h / 2.0F, ts), white, ts);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                this.capture = key;
                this.captureOwner = f;
                GuiSfx.click();
            }, null);
            return h;
        }
        if (opt instanceof Opt.Text text) {
            float h = 20.0F * s;
            String v = text.get();
            String shown = v.isBlank() ? "вставить" : (v.length() > 10 ? v.substring(0, 10) + "…" : v);
            Paint.text(g, this.fit(opt.label(), ts, w * 0.5F), x, this.textY(y + h / 2.0F, ts), muted, ts);
            float pw = Paint.tw(shown, ts) + 14.0F * s;
            Paint.box(g, x + w - pw, y + 3.0F * s, pw, h - 6.0F * s, rgba(0x2B2738, 1.0F), 5.0F * s);
            Paint.textC(g, shown, x + w - pw / 2.0F, this.textY(y + h / 2.0F, ts), white, ts);
            this.addHit(x, y, w, h, vy0, vy1, () -> {
                String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clip != null && !clip.isBlank()) {
                    text.set(clip.trim());
                    f.poke();
                }
            }, null);
            return h;
        }
        return 0.0F;
    }

    private boolean over(float x, float y, float w, float h, float vy0, float vy1) {
        return this.my >= vy0 && this.my <= vy1 && Paint.hit(this.mx, this.my, x, y, w, h);
    }

    private void addHit(float x, float y, float w, float h, float vy0, float vy1, Runnable left, Runnable right) {
        float top = Math.max(y, vy0);
        float bottom = Math.min(y + h, vy1);
        if (bottom > top) {
            this.hits.add(new Hit(x, top, w, bottom - top, left, right, null));
        }
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

    // ---------- input ----------

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        int button = event.button();
        if (button > 2) {
            return super.mouseClicked(event, doubled);
        }
        this.clickX = event.x();
        if (this.capture != null) {
            this.capture = null;
            this.captureOwner = null;
        }
        for (int i = this.hits.size() - 1; i >= 0; i--) {
            Hit hit = this.hits.get(i);
            if (Paint.hit(event.x(), event.y(), hit.x, hit.y, hit.w, hit.h)) {
                Runnable action = button == 0 ? hit.left : button == 1 ? hit.right : hit.mid;
                if (action != null) {
                    action.run();
                    return true;
                }
                if (button == 0) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(Click event, double dx, double dy) {
        if (this.drag != null) {
            this.applyDrag(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click event) {
        this.drag = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float step = (float)v * 40.0F * this.s;
        if (this.drawerAnim.value() > 0.5F && mouseX >= this.drawL) {
            float min = Math.min(0.0F, this.drawView - this.drawContent);
            this.drawTarget = Math.max(min, Math.min(0.0F, this.drawTarget + step));
            return true;
        }
        float min = Math.min(0.0F, this.gridView - this.gridContent);
        this.gridTarget = Math.max(min, Math.min(0.0F, this.gridTarget + step));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        int key = event.key();
        if (this.capture != null) {
            if (key != 256) {
                this.capture.set(key == 259 || key == 261 ? -1 : key);
                if (this.captureOwner != null) {
                    this.captureOwner.poke();
                }
            }
            this.capture = null;
            this.captureOwner = null;
            return true;
        }
        if (key == 256) {
            if (this.pickOpen != null) {
                this.pickOpen = null;
            } else if (!this.search.isEmpty()) {
                this.search = "";
            } else if (this.selected != null) {
                this.selected = null;
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
            this.gridTarget = 0.0F;
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
        final Runnable mid;

        Hit(float x, float y, float w, float h, Runnable left, Runnable right, Runnable mid) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.left = left;
            this.right = right;
            this.mid = mid;
        }
    }

    private record DragNum(Opt.Num num, Feature owner, float x, float w) {
    }
}
