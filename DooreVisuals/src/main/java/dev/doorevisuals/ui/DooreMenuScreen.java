package dev.doorevisuals.ui;

import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.data.FirstRun;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.SmoothText;
import dev.doorevisuals.draw.Theme;
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
        this.buttons.clear();
        this.buttons.add(new Btn("Одиночная игра", () -> this.client.setScreen(new SelectWorldScreen(this))));
        this.buttons.add(new Btn("Серверная игра", () -> this.client.setScreen(new MultiplayerScreen(this))));
        this.buttons.add(new Btn("Alt Manager", () -> this.client.setScreen(new AltManagerScreen(this))));
        this.buttons.add(new Btn("Настройки", () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        this.buttons.add(new Btn("Выйти из игры", () -> this.client.scheduleStop()));
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

        this.drawBackground(g);
        this.drawGreeting(g);
        this.drawButtons(g);
    }

    /** A soft vertical gradient: slightly lighter in the middle, dark at the top and bottom. */
    private void drawBackground(DrawContext g) {
        float w = this.width;
        float h = this.height;
        Paint.grad(g, 0, 0, w, h * 0.5F, rgba(0x07070C, 1.0F), rgba(0x14121E, 1.0F), 0.0F, true);
        Paint.grad(g, 0, h * 0.5F, w, h * 0.5F, rgba(0x14121E, 1.0F), rgba(0x07070C, 1.0F), 0.0F, true);
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

    private void drawButtons(DrawContext g) {
        this.layout();
        float r = 8.0F * s;
        for (Btn b : this.buttons) {
            boolean hot = Paint.hit(this.mx, this.my, b.x, b.y, b.w, b.h);
            Paint.box(g, b.x, b.y, b.w, b.h, rgba(0xFFFFFF, hot ? 0.10F : 0.03F), r);
            // the exit button lights up red, so it is not mistaken for the others
            boolean exit = b.label.startsWith("Выйти");
            Paint.outline(g, b.x, b.y, b.w, b.h, rgba(hot ? (exit ? 0xFF5C7A : Theme.ACCENT_HOT) : 0xFFFFFF, hot ? 0.8F : 0.18F), r);
            this.text(g, b.label, b.x + b.w / 2.0F, b.y + (b.h - 9.0F) / 2.0F, rgba(hot ? 0xFFFFFF : 0xD8D6E2, 1.0F));
        }
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
        for (Btn b : this.buttons) {
            if (Paint.hit(event.x(), event.y(), b.x, b.y, b.w, b.h)) {
                GuiSfx.click();
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

        Btn(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
    }
}