package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.cosmetics.CapeFiles;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.SmoothText;
import dev.doorevisuals.draw.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The cape catalog: every cape (built-in and the ones from the capes folder) as a card with a preview of the back of the
 * cape, a search field, and a click to put it on. Drawn without NanoVG, like the menu, so nothing blinks.
 */
public final class CapeCatalogScreen extends Screen {
    private static final float CARD_W = 92.0F;
    private static final float CARD_H = 128.0F;
    private static final float GAP = 10.0F;

    private final Screen parent;
    private String query = "";
    private float scroll;
    private float scrollTarget;
    private float contentH;
    private int mx;
    private int my;
    private final List<Card> cards = new ArrayList<>();
    private String note = "";

    public CapeCatalogScreen(Screen parent) {
        super(Text.literal("Каталог плащей"));
        this.parent = parent;
    }

    private record Card(String label, float x, float y) {
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        float w = this.width;
        float h = this.height;
        Paint.grad(g, 0, 0, w, h, Theme.alpha(0x07070C, 255), Theme.alpha(0x15131F, 255), 0.0F, true);

        SmoothText.drawCentered(g, "Каталог плащей", w / 2.0F, 18.0F, Theme.alpha(0xFFFFFF, 255), 14.0F);
        // Search field
        float sw = Math.min(280.0F, w - 40.0F);
        float sx = (w - sw) / 2.0F;
        float sy = 42.0F;
        Paint.box(g, sx, sy, sw, 22.0F, Theme.alpha(0xFFFFFF, 18), 8.0F);
        Paint.outline(g, sx, sy, sw, 22.0F, Theme.alpha(Theme.ACCENT, 130), 8.0F);
        boolean caret = (System.currentTimeMillis() / 500L) % 2L == 0L;
        String shown = this.query.isEmpty() ? "Поиск по названию..." : this.query + (caret ? "|" : "");
        SmoothText.draw(g, shown, sx + 10.0F, sy + 5.0F, Theme.alpha(this.query.isEmpty() ? 0x8C8898 : 0xFFFFFF, 255), 10.0F);

        List<String> labels = this.filtered();
        int cols = Math.max(1, (int)((w - 40.0F + GAP) / (CARD_W + GAP)));
        float gridW = cols * CARD_W + (cols - 1) * GAP;
        float gx = (w - gridW) / 2.0F;
        float top = 80.0F;
        float view = h - top - 30.0F;
        this.contentH = ((labels.size() + cols - 1) / cols) * (CARD_H + GAP);
        this.scrollTarget = Math.max(Math.min(0.0F, view - this.contentH), Math.min(0.0F, this.scrollTarget));
        this.scroll += (this.scrollTarget - this.scroll) * 0.25F;

        String current = App.features().find(CosmeticsFeature.class).map(CosmeticsFeature::capeLabel).orElse("");
        this.cards.clear();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            float x = gx + (i % cols) * (CARD_W + GAP);
            float y = top + this.scroll + (i / cols) * (CARD_H + GAP);
            if (y + CARD_H < top - 4.0F || y > h - 24.0F) {
                continue;
            }
            this.cards.add(new Card(label, x, y));
            boolean sel = label.equals(current);
            boolean hot = this.my > top && Paint.hit(this.mx, this.my, x, y, CARD_W, CARD_H);
            Paint.box(g, x, y, CARD_W, CARD_H, Theme.alpha(0xFFFFFF, hot ? 28 : 14), 10.0F);
            Paint.outline(g, x, y, CARD_W, CARD_H, sel ? Theme.alpha(Theme.ACCENT_HOT, 255) : Theme.alpha(0xFFFFFF, hot ? 70 : 30), 10.0F);
            Identifier tex = CosmeticsFeature.previewTexture(label);
            if (tex != null) {
                // The back of the cape in a vanilla cape texture: 10 x 16 pixels at (1, 1) of a 64 x 32 sheet.
                g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, (int)(x + (CARD_W - 60.0F) / 2.0F), (int)(y + 10.0F), 1.0F, 1.0F, 60, 96, 10, 16, 64, 32, -1);
            } else {
                Paint.box(g, x + 18.0F, y + 12.0F, 56.0F, 92.0F, Theme.alpha(0xFFFFFF, 12), 6.0F);
                SmoothText.drawCentered(g, "нет", x + CARD_W / 2.0F, y + 52.0F, Theme.alpha(0x8C8898, 255), 10.0F);
            }
            SmoothText.drawCentered(g, label, x + CARD_W / 2.0F, y + CARD_H - 18.0F, Theme.alpha(sel ? 0xFFFFFF : 0xC8C6D4, 255), 9.6F);
        }
        if (labels.isEmpty()) {
            SmoothText.drawCentered(g, "Ничего не найдено", w / 2.0F, top + 30.0F, Theme.alpha(0x8C8898, 255), 11.0F);
        }
        String hint = this.note.isEmpty() ? "Свои плащи: PNG 64x32 в папку плащей. Esc: назад" : this.note;
        SmoothText.drawCentered(g, hint, w / 2.0F, h - 20.0F, Theme.alpha(this.note.isEmpty() ? 0x8C8898 : Theme.ACCENT_HOT, 255), 8.8F);
    }

    private List<String> filtered() {
        List<String> all = CapeFiles.allCapeLabels();
        if (this.query.isBlank()) {
            return all;
        }
        String q = this.query.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String label : all) {
            if (label.toLowerCase(Locale.ROOT).contains(q)) {
                out.add(label);
            }
        }
        return out;
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 0 && event.y() > 80.0) {
            for (Card c : this.cards) {
                if (Paint.hit(event.x(), event.y(), c.x(), c.y(), CARD_W, CARD_H)) {
                    App.features().find(CosmeticsFeature.class).ifPresent(f -> f.setCapeLabel(c.label()));
                    this.note = "Плащ: " + c.label();
                    GuiSfx.click();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        this.scrollTarget += (float)v * 48.0F;
        return true;
    }

    @Override
    public boolean charTyped(CharInput event) {
        char c = (char)event.codepoint();
        if (c >= ' ' && this.query.length() < 24) {
            this.query += c;
            this.scrollTarget = 0.0F;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        if (event.key() == 259 && !this.query.isEmpty()) {
            this.query = this.query.substring(0, this.query.length() - 1);
            return true;
        }
        if (event.key() == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(this.parent);
    }
}
