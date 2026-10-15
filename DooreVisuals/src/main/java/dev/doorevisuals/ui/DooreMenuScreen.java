package dev.doorevisuals.ui;

import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.data.FirstRun;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.SmoothText;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.data.Changelog;
import dev.doorevisuals.media.CoverArt;
import dev.doorevisuals.media.MediaHub;
import dev.doorevisuals.tools.MainMenuFeature;
import net.minecraft.client.gl.RenderPipelines;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Minimal main menu: a dark gradient, a greeting with the player's name and four centred buttons.
 *
 * <p>Why this screen draws without NanoVG: NanoVG renders each frame into an offscreen target that it clears, and on
 * alternate frames that clear wiped the whole window to black, background included. Shapes here go through the SDF
 * path, which needs no NanoVG pass, and text uses the vanilla text renderer, so no frame clears the screen.
 */
public final class DooreMenuScreen extends Screen {
    /** Font id from assets/doorevisuals/font/inter.json (provider file doorevisuals:font/inter.ttf). */
    private static final Identifier FONT_ID = Identifier.of("doorevisuals", "inter");
    private static final Identifier FONT_JSON = Identifier.of("doorevisuals", "font/inter.json");
    private static final Style INTER = Style.EMPTY.withFont(new StyleSpriteSource.Font(FONT_ID));
    /**
     * Inter as a bitmap font (assets/doorevisuals/font/inter.json). Bitmap providers read a plain texture, so they do not
     * depend on the TrueType provider. Set to false to fall back to the default font.
     */
    private static final boolean USE_INTER = true;

    private final List<Btn> buttons = new ArrayList<>();
    private int mx;
    private int my;
    private float s = 1.0F;
    private String nick = "";
    private boolean customFont;
    private long openedAt = System.nanoTime();
    private long lastFrame;
    /** When the exit button was pressed (nanoseconds), 0 while the menu is normal: the farewell fade runs from here. */
    private long exitAt;
    private boolean stopSent;
    // the mini player: hit rectangles of the three buttons (x, y, size), valid only while it is drawn
    private float prevX;
    private float playX;
    private float nextX;
    private float ctlY;
    private float ctlS;
    private boolean playerShown;

    public DooreMenuScreen() {
        super(Text.literal("DooreVisuals"));
    }

    @Override
    protected void init() {
        super.init();
        AltsStore.boot();
        if (FirstRun.needsWizard() && this.client != null) {
            this.client.setScreen(new FirstRunScreen(this));
            return;
        }
        this.customFont = USE_INTER && hasResource(FONT_JSON);
        this.nick = resolveNick();
        if (this.buttons.isEmpty()) {
            this.openedAt = System.nanoTime();
        }
        this.buttons.clear();
        this.buttons.add(new Btn("Одиночная игра", () -> this.client.setScreen(new SelectWorldScreen(this))));
        this.buttons.add(new Btn("Серверная игра", () -> this.client.setScreen(new MultiplayerScreen(this))));
        this.buttons.add(new Btn("Alt Manager", () -> this.client.setScreen(new AltManagerScreen(this))));
        this.buttons.add(new Btn("Настройки", () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.buttons.add(new Btn("Выйти из игры", this::beginExit));
    }

    private static String resolveNick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getSession() != null) {
            String name = mc.getSession().getUsername();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return "игрок";
    }

    private static boolean hasResource(Identifier id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.getResourceManager() != null && mc.getResourceManager().getResource(id).isPresent();
    }

    /** Everything is painted in render(), so the vanilla blur never runs under this screen. */
    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.s = Math.max(0.75F, Math.min(1.5F, Math.min(this.width / 1280.0F, this.height / 720.0F)));

        this.drawBackground(g, partialTick);
        this.drawProfile(g);
        this.drawLogo(g);
        this.drawGreeting(g);
        this.drawButtons(g);
        this.drawNews(g);
        this.drawPlayer(g);
        this.drawFarewell(g);
    }

    @Override
    public void removed() {
        MenuTextures.closeVideo();
        super.removed();
    }

    /**
     * The background of the chosen kind: the soft gradient, the client's picture, the game's rotating panorama, or the frames of
     * the user's video or GIF. Everything but the gradient is dimmed so the text stays readable.
     */
    private void drawBackground(DrawContext g, float partial) {
        float w = this.width;
        float h = this.height;
        boolean drew = false;
        switch (MainMenuFeature.background()) {
            case MainMenuFeature.BG_IMAGE -> drew = this.drawCover(g, Sprites.MENU_BG, 1536, 1024);
            case MainMenuFeature.BG_PANORAMA -> {
                this.renderPanoramaBackground(g, partial);
                drew = true;
            }
            case MainMenuFeature.BG_VIDEO -> {
                MenuTextures.Pic frame = MenuTextures.videoFrame();
                if (frame != null && frame.id() != null) {
                    drew = this.drawCover(g, frame.id(), frame.w(), frame.h());
                }
            }
            default -> {
            }
        }
        if (drew) {
            Paint.box(g, 0, 0, w, h, rgba(0x000000, MainMenuFeature.dim()), 0.0F);
        } else {
            Paint.grad(g, 0, 0, w, h * 0.5F, rgba(0x07070C, 1.0F), rgba(0x14121E, 1.0F), 0.0F, true);
            Paint.grad(g, 0, h * 0.5F, w, h * 0.5F, rgba(0x14121E, 1.0F), rgba(0x07070C, 1.0F), 0.0F, true);
        }
    }

    /** Draws a texture over the whole screen, scaled to cover it and cropped in the middle. */
    private boolean drawCover(DrawContext g, Identifier tex, int tw, int th) {
        float k = Math.max((float)this.width / tw, (float)this.height / th);
        int rw = Math.min(tw, Math.round(this.width / k));
        int rh = Math.min(th, Math.round(this.height / k));
        g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, 0, 0, (float)((tw - rw) / 2), (float)((th - rh) / 2), this.width, this.height, rw, rh, tw, th, -1);
        return true;
    }

    /** The logo above the greeting: the client's own, or a PNG chosen in Main Menu; it fades in and floats a little. */
    private void drawLogo(DrawContext g) {
        if (!MainMenuFeature.logo()) {
            return;
        }
        float size = 56.0F * s;
        float cx = this.width / 2.0F;
        float y = this.height / 2.0F - 120.0F * s - size - 16.0F * s;
        float since = (System.nanoTime() - this.openedAt) / 1.0E9F;
        float in = clamp01(since / 0.7F);
        in = 1.0F - (1.0F - in) * (1.0F - in) * (1.0F - in);
        float bob = (float)Math.sin(since * 1.4F) * 2.0F * s;
        Identifier tex = Sprites.CLIENT_ICON;
        int tw = 128;
        int th = 128;
        MenuTextures.Pic custom = MenuTextures.file(MainMenuFeature.logoFile());
        if (custom != null && custom.id() != null) {
            tex = custom.id();
            tw = custom.w();
            th = custom.h();
        }
        float k = Math.min(size / tw, size / th);
        int dw = Math.max(1, Math.round(tw * k));
        int dh = Math.max(1, Math.round(th * k));
        Paint.circle(g, cx, y + size / 2.0F + bob, size * 0.66F, rgba(Theme.ACCENT, 0.12F * in));
        g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, Math.round(cx - dw / 2.0F), Math.round(y + (size - dh) / 2.0F + bob), 0.0F, 0.0F, dw, dh, dw, dh, rgba(0xFFFFFF, in));
    }

    /** The profile card in the corner: a picture (or the first letter), the nickname and a line of text. */
    private void drawProfile(DrawContext g) {
        if (!MainMenuFeature.profile()) {
            return;
        }
        float em = 9.0F * Math.max(0.9F, s);
        String note = MainMenuFeature.profileText();
        String line2 = note.isBlank() ? "DooreVisuals " + dev.doorevisuals.DooreClient.VERSION : note;
        float pic = 36.0F * s;
        float x = 18.0F * s;
        float y = 18.0F * s;
        float h = pic + 14.0F * s;
        float w = Math.min(280.0F * s, Math.max(150.0F * s, Math.max(SmoothText.width(this.nick, em * 1.1F), SmoothText.width(line2, em * 0.9F)) + pic + 36.0F * s));
        Paint.box(g, x, y, w, h, rgba(0x0B0A12, 0.72F), 12.0F * s);
        Paint.outline(g, x, y, w, h, rgba(0xFFFFFF, 0.10F), 12.0F * s);
        float px = x + 7.0F * s;
        float py = y + 7.0F * s;
        MenuTextures.Pic img = MenuTextures.file(MainMenuFeature.profileImage());
        if (img != null && img.id() != null) {
            float k = Math.min(pic / img.w(), pic / img.h());
            int dw = Math.max(1, Math.round(img.w() * k));
            int dh = Math.max(1, Math.round(img.h() * k));
            g.drawTexture(RenderPipelines.GUI_TEXTURED, img.id(), Math.round(px + (pic - dw) / 2.0F), Math.round(py + (pic - dh) / 2.0F), 0.0F, 0.0F, dw, dh, dw, dh, -1);
        } else {
            Paint.circle(g, px + pic / 2.0F, py + pic / 2.0F, pic / 2.0F, rgba(Theme.ACCENT, 1.0F));
            String initial = this.nick.isEmpty() ? "?" : this.nick.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            SmoothText.drawCentered(g, initial, px + pic / 2.0F, py + (pic - SmoothText.height(em * 1.4F)) / 2.0F, rgba(0xFFFFFF, 1.0F), em * 1.4F);
        }
        float tx = px + pic + 9.0F * s;
        SmoothText.draw(g, this.nick, tx, py + 3.0F * s, rgba(0xFFFFFF, 1.0F), em * 1.1F);
        SmoothText.draw(g, line2, tx, py + 3.0F * s + SmoothText.height(em * 1.1F) + 1.0F * s, rgba(0x9A96AA, 1.0F), em * 0.9F);
    }

    /** What is new in the latest version: the title and the first bullets of the changelog. */
    private void drawNews(DrawContext g) {
        if (!MainMenuFeature.news() || this.width < 980) {
            return;
        }
        List<Changelog.Entry> entries = Changelog.load();
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Changelog.Entry e = entries.get(0);
        float em = 8.4F * Math.max(0.9F, s);
        float w = 250.0F * s;
        float x = this.width - w - 24.0F * s;
        float inner = w - 26.0F * s;
        List<String> lines = new ArrayList<>();
        int bullets = Math.min(6, e.bullets().size());
        for (int i = 0; i < bullets; i++) {
            lines.addAll(wrap("• " + e.bullets().get(i), inner, em, 3));
        }
        float lh = SmoothText.height(em) + 1.5F * s;
        float h = 46.0F * s + lines.size() * lh;
        float y = this.height / 2.0F - h / 2.0F;
        Paint.box(g, x, y, w, h, rgba(0x0B0A12, 0.72F), 12.0F * s);
        Paint.outline(g, x, y, w, h, rgba(0xFFFFFF, 0.10F), 12.0F * s);
        SmoothText.draw(g, "Что нового · " + e.version(), x + 13.0F * s, y + 10.0F * s, rgba(Theme.ACCENT_HOT, 1.0F), em * 1.2F);
        SmoothText.draw(g, e.title(), x + 13.0F * s, y + 10.0F * s + SmoothText.height(em * 1.2F), rgba(0xFFFFFF, 1.0F), em);
        float ly = y + 40.0F * s;
        for (String line : lines) {
            SmoothText.draw(g, line, x + 13.0F * s, ly, rgba(0xC8C6D4, 1.0F), em);
            ly += lh;
        }
    }

    /** Greedy word wrap with the soft font; at most maxLines lines, the last one cut with an ellipsis. */
    private static List<String> wrap(String text, float maxW, float em, int maxLines) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (SmoothText.width(next, em) > maxW && line.length() > 0) {
                out.add(line.toString());
                line = new StringBuilder("  " + word);
                if (out.size() == maxLines - 1) {
                    break;
                }
            } else {
                line = new StringBuilder(next);
            }
        }
        if (out.size() < maxLines && line.length() > 0) {
            String last = line.toString();
            while (last.length() > 3 && SmoothText.width(last, em) > maxW) {
                last = last.substring(0, last.length() - 2) + "…";
            }
            out.add(last);
        }
        return out;
    }

    /** The mini player at the bottom: cover, title, artist, progress and the three controls of the system's media session. */
    private void drawPlayer(DrawContext g) {
        this.playerShown = false;
        if (!MainMenuFeature.player()) {
            return;
        }
        MediaHub.Snap sn = MediaHub.get().snapshot();
        if (!sn.available()) {
            return;
        }
        float em = 8.8F * Math.max(0.9F, s);
        float w = 340.0F * s;
        float h = 54.0F * s;
        float x = (this.width - w) / 2.0F;
        float y = this.height - h - 26.0F * s;
        Paint.box(g, x, y, w, h, rgba(0x0B0A12, 0.78F), 12.0F * s);
        Paint.outline(g, x, y, w, h, rgba(0xFFFFFF, 0.10F), 12.0F * s);
        float cover = 38.0F * s;
        float cx = x + 8.0F * s;
        float cy = y + (h - cover) / 2.0F - 3.0F * s;
        if (CoverArt.ready()) {
            int c = Math.round(cover);
            g.drawTexture(RenderPipelines.GUI_TEXTURED, CoverArt.id(), Math.round(cx), Math.round(cy), 0.0F, 0.0F, c, c, c, c, -1);
        } else {
            Paint.box(g, cx, cy, cover, cover, rgba(Theme.ACCENT, 0.55F), 8.0F * s);
            SmoothText.drawCentered(g, "♪", cx + cover / 2.0F, cy + (cover - SmoothText.height(em * 1.5F)) / 2.0F, rgba(0xFFFFFF, 1.0F), em * 1.5F);
        }
        float tx = cx + cover + 10.0F * s;
        float controls = 3 * 22.0F * s;
        float textW = w - (tx - x) - controls - 12.0F * s;
        SmoothText.draw(g, fit(sn.title(), textW, em), tx, y + 9.0F * s, rgba(0xFFFFFF, 1.0F), em);
        SmoothText.draw(g, fit(sn.artist(), textW, em * 0.9F), tx, y + 9.0F * s + SmoothText.height(em) + 1.0F * s, rgba(0x9A96AA, 1.0F), em * 0.9F);
        // progress
        float bx = cx;
        float bw = w - 16.0F * s;
        float by = y + h - 9.0F * s;
        Paint.box(g, bx, by, bw, 2.4F * s, rgba(0xFFFFFF, 0.16F), 1.2F * s);
        Paint.box(g, bx, by, Math.max(2.0F, bw * (float)Math.max(0.0, Math.min(1.0, sn.progress()))), 2.4F * s, rgba(Theme.ACCENT_HOT, 1.0F), 1.2F * s);
        // controls
        this.ctlS = 18.0F * s;
        this.ctlY = y + 9.0F * s;
        this.nextX = x + w - 8.0F * s - this.ctlS;
        this.playX = this.nextX - 4.0F * s - this.ctlS;
        this.prevX = this.playX - 4.0F * s - this.ctlS;
        this.control(g, this.prevX, 0);
        this.control(g, this.playX, sn.playing() ? 3 : 1);
        this.control(g, this.nextX, 2);
        this.playerShown = true;
    }

    private static String fit(String text, float maxW, float em) {
        String t = text == null ? "" : text;
        while (t.length() > 3 && SmoothText.width(t, em) > maxW) {
            t = t.substring(0, t.length() - 2) + "…";
        }
        return t;
    }

    /** One round control button: 0 previous, 1 play, 2 next, 3 pause; the icon is drawn from boxes. */
    private void control(DrawContext g, float x, int kind) {
        boolean hot = Paint.hit(this.mx, this.my, x, this.ctlY, this.ctlS, this.ctlS);
        float cx = x + this.ctlS / 2.0F;
        float cy = this.ctlY + this.ctlS / 2.0F;
        Paint.circle(g, cx, cy, this.ctlS / 2.0F, rgba(0xFFFFFF, hot ? 0.20F : 0.09F));
        int col = rgba(0xFFFFFF, 1.0F);
        float u = this.ctlS * 0.22F;
        switch (kind) {
            case 1 -> triangle(g, cx - u * 0.5F, cy, u * 2.0F, col, true);
            case 3 -> {
                Paint.box(g, cx - u * 0.9F, cy - u, u * 0.7F, u * 2.0F, col, 0.5F);
                Paint.box(g, cx + u * 0.2F, cy - u, u * 0.7F, u * 2.0F, col, 0.5F);
            }
            case 0 -> {
                triangle(g, cx + u * 0.2F, cy, u * 1.6F, col, false);
                Paint.box(g, cx - u * 1.2F, cy - u * 0.8F, u * 0.4F, u * 1.6F, col, 0.4F);
            }
            default -> {
                triangle(g, cx - u * 0.2F, cy, u * 1.6F, col, true);
                Paint.box(g, cx + u * 0.9F, cy - u * 0.8F, u * 0.4F, u * 1.6F, col, 0.4F);
            }
        }
    }

    /** A triangle pointing right (or left) from stacked rows, centred on (cx, cy) with the given height. */
    private static void triangle(DrawContext g, float cx, float cy, float size, int col, boolean right) {
        int rows = 9;
        float rh = size / rows;
        float width = size * 0.8F;
        for (int i = 0; i < rows; i++) {
            float t = (i + 0.5F) / rows;
            float rw = width * (1.0F - Math.abs(2.0F * t - 1.0F));
            float y = cy - size / 2.0F + i * rh;
            float x = right ? cx - width / 2.0F : cx + width / 2.0F - rw;
            Paint.box(g, x, y, Math.max(0.6F, rw), rh + 0.3F, col, 0.0F);
        }
    }

    /** The farewell: the screen fades to black, then the logo and a goodbye, and the game closes. */
    private void beginExit() {
        if (!MainMenuFeature.farewell()) {
            this.client.scheduleStop();
            return;
        }
        if (this.exitAt == 0L) {
            this.exitAt = System.nanoTime();
        }
    }

    private void drawFarewell(DrawContext g) {
        if (this.exitAt == 0L) {
            return;
        }
        float t = (System.nanoTime() - this.exitAt) / 1.0E9F;
        Paint.box(g, 0, 0, this.width, this.height, rgba(0x000000, clamp01(t / 0.5F)), 0.0F);
        float b = clamp01((t - 0.2F) / 0.45F);
        if (b > 0.0F) {
            float size = 60.0F * s;
            float cx = this.width / 2.0F;
            float y = this.height / 2.0F - size;
            g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.CLIENT_ICON, Math.round(cx - size / 2.0F), Math.round(y), 0.0F, 0.0F, Math.round(size), Math.round(size), Math.round(size), Math.round(size), rgba(0xFFFFFF, b));
            this.text(g, "До встречи, " + this.nick, cx, y + size + 14.0F * s, rgba(0xFFFFFF, b));
        }
        if (t > 1.2F && !this.stopSent) {
            this.stopSent = true;
            this.client.scheduleStop();
        }
    }

    private void drawGreeting(DrawContext g) {
        float y = this.height / 2.0F - 120.0F * s;
        this.text(g, "Приветствую тебя, " + this.nick, this.width / 2.0F, y, rgba(0xFFFFFF, 1.0F));
        float lineW = 120.0F * s;
        Paint.box(g, (this.width - lineW) / 2.0F, y + 20.0F * s, lineW, 1.0F, rgba(Theme.ACCENT_HOT, 0.6F), 0.0F);
    }

    // ---------- buttons ----------

    private void layout() {
        float bw = 260.0F * s;
        float bh = 36.0F * s;
        float gap = 10.0F * s;
        float total = this.buttons.size() * bh + (this.buttons.size() - 1) * gap;
        float top = this.height / 2.0F - total / 2.0F + 10.0F * s;
        for (int i = 0; i < this.buttons.size(); i++) {
            Btn b = this.buttons.get(i);
            b.w = bw;
            b.h = bh;
            b.x = (this.width - bw) / 2.0F;
            b.y = top + i * (bh + gap);
        }
    }

    /**
     * Buttons slide in one after another when the menu opens; the hover state eases in and out with real frame time
     * (so it looks the same at any fps), the button lifts and grows a little, and a click gives a short press dip.
     */
    private void drawButtons(DrawContext g) {
        this.layout();
        long now = System.nanoTime();
        float dt = this.lastFrame == 0L ? 0.016F : Math.max(0.0F, Math.min(0.05F, (now - this.lastFrame) / 1.0E9F));
        this.lastFrame = now;
        float since = (now - this.openedAt) / 1.0E9F;
        float r = 8.0F * s;
        for (int i = 0; i < this.buttons.size(); i++) {
            Btn b = this.buttons.get(i);
            float in = clamp01((since - 0.12F - i * 0.07F) / 0.5F);
            in = 1.0F - (1.0F - in) * (1.0F - in) * (1.0F - in);
            b.dy = (1.0F - in) * 16.0F * s;
            boolean hot = Paint.hit(this.mx, this.my, b.x, b.y + b.dy, b.w, b.h);
            b.hover += ((hot ? 1.0F : 0.0F) - b.hover) * (1.0F - (float)Math.exp(-dt * (hot ? 13.0F : 9.0F)));
            b.press = Math.max(0.0F, b.press - dt * 5.5F);
            float h = b.hover * b.hover * (3.0F - 2.0F * b.hover);
            float dip = b.press * b.press * (3.0F - 2.0F * b.press);

            float grow = (0.018F * h - 0.03F * dip) * b.w;
            float bx = b.x - grow;
            float bw = b.w + grow * 2.0F;
            float by = b.y + b.dy - 1.6F * s * h + 1.0F * s * dip;
            float bh = b.h + grow * 0.35F;
            boolean exit = b.label.startsWith("Выйти");
            int accent = exit ? 0xFF5C7A : Theme.ACCENT_HOT;

            if (h > 0.01F) {
                Paint.box(g, bx - 3.0F * s * h, by - 3.0F * s * h, bw + 6.0F * s * h, bh + 6.0F * s * h, rgba(accent, 0.10F * h * in), r + 3.0F * s * h);
            }
            Paint.box(g, bx, by, bw, bh, rgba(0xFFFFFF, (0.035F + 0.075F * h + 0.05F * dip) * in), r);
            Paint.outline(g, bx, by, bw, bh, rgba(mixRgb(0xFFFFFF, accent, h), (0.18F + 0.62F * h) * in), r);
            // a thin line grows from the middle under the label while the button is hot
            if (h > 0.02F) {
                float lw = bw * 0.34F * h;
                Paint.box(g, bx + (bw - lw) / 2.0F, by + bh - 6.0F * s, lw, 1.2F, rgba(accent, 0.75F * h * in), 0.6F);
            }
            this.text(g, b.label, bx + bw / 2.0F, by + (bh - 9.0F) / 2.0F - 0.6F * s * h, rgba(mixRgb(0xD8D6E2, 0xFFFFFF, h), in));
        }
    }

    private static int mixRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1.0F - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1.0F - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Centred text in Inter when its font file is present, otherwise in the default font. */
    private void text(DrawContext g, String s, float cx, float y, int color) {
        if (SmoothText.ready()) {
            // Anti-aliased Inter on a linearly filtered texture; the vanilla atlas looks pixelated when it is scaled.
            float em = 9.6F * Math.max(0.9F, this.s);
            SmoothText.drawCentered(g, s, cx, y - (SmoothText.height(em) - 9.0F) / 2.0F, color, em);
            return;
        }
        Text t = this.customFont ? Text.literal(s).setStyle(INTER) : Text.literal(s);
        int w = MinecraftClient.getInstance().textRenderer.getWidth(t);
        g.drawText(MinecraftClient.getInstance().textRenderer, t, Math.round(cx) - w / 2, Math.round(y), color, false);
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        }
        if (this.exitAt != 0L) {
            return true;
        }
        if (this.playerShown) {
            if (Paint.hit(event.x(), event.y(), this.prevX, this.ctlY, this.ctlS, this.ctlS)) {
                GuiSfx.click();
                MediaHub.get().prevTrack();
                return true;
            }
            if (Paint.hit(event.x(), event.y(), this.playX, this.ctlY, this.ctlS, this.ctlS)) {
                GuiSfx.click();
                MediaHub.get().togglePlay();
                return true;
            }
            if (Paint.hit(event.x(), event.y(), this.nextX, this.ctlY, this.ctlS, this.ctlS)) {
                GuiSfx.click();
                MediaHub.get().nextTrack();
                return true;
            }
        }
        for (Btn b : this.buttons) {
            if (Paint.hit(event.x(), event.y(), b.x, b.y + b.dy, b.w, b.h)) {
                GuiSfx.click();
                b.press = 1.0F;
                b.action.run();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static int rgba(int rgb, float a) {
        return Theme.alpha(rgb & 0xFFFFFF, Math.round(clamp01(a) * 255.0F));
    }

    private static final class Btn {
        final String label;
        final Runnable action;
        float x;
        float y;
        float w;
        float h;
        float hover;
        float press;
        /** The entrance offset of the current frame, so hit tests match what is drawn. */
        float dy;

        Btn(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }
}