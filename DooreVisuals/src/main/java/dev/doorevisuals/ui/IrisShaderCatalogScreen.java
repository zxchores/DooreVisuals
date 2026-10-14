package dev.doorevisuals.ui;

import dev.doorevisuals.compat.IrisCompat;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.net.IrisShaderCatalog;
import dev.doorevisuals.net.IrisAutoInstaller;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class IrisShaderCatalogScreen extends Screen {
    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private String query = "";
    private boolean queryFocused;
    private int scroll;
    private int mx, my;
    private float panelX, panelY, panelW, panelH;

    public IrisShaderCatalogScreen(Screen parent) {
        super(Text.literal("DooreVisuals · Iris Shaders"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        IrisShaderCatalog.search("");
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        MenuAtmosphere.paintBackground(g, this.width, this.height);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        this.panelW = Math.min(720.0F, this.width - 32.0F);
        this.panelH = Math.min(520.0F, this.height - 28.0F);
        this.panelX = (this.width - this.panelW) * 0.5F;
        this.panelY = (this.height - this.panelH) * 0.5F;
        this.renderBackground(g, mouseX, mouseY, delta);
        Ui.frame(g, () -> this.paint(g));
        super.render(g, mouseX, mouseY, delta);
    }

    private void paint(DrawContext g) {
        float x = this.panelX, y = this.panelY, w = this.panelW, h = this.panelH;
        Paint.chromeGui(g, x, y, w, h, 14.0F);
        Paint.text(g, "Iris Shaderpacks", x + 20, y + 17, Theme.TEXT, 14.0F);
        Paint.text(g, "Каталог Modrinth · Minecraft 1.21.11", x + 20, y + 38, Theme.MUTED, 7.0F);
        this.button(g, x + w - 204, y + 14, 182, 26, IrisCompat.available() ? "Открыть выбор Iris" : "Iris устанавливается", IrisCompat.available(), () -> {
            var iris = IrisCompat.openScreen(this);
            if (iris != null && this.client != null) this.client.setScreen(iris);
        });
        float searchX = x + 18, searchY = y + 58, searchW = w - 36;
        Paint.box(g, searchX, searchY, searchW, 28, Theme.alpha(16777215, this.queryFocused ? 20 : 11), 8);
        if (this.queryFocused) Paint.outline(g, searchX, searchY, searchW, 28, Theme.alpha(Theme.ACCENT, 100), 8);
        Paint.text(g, this.query.isEmpty() && !this.queryFocused ? "Поиск шейдеров…" : this.query + (this.queryFocused ? "|" : ""), searchX + 10, searchY + 9, this.query.isEmpty() ? Theme.GHOST : Theme.TEXT, 7);
        this.hits.add(new Hit(searchX, searchY, searchW - 78, 28, () -> this.queryFocused = true));
        this.button(g, searchX + searchW - 72, searchY, 72, 28, "Найти", true, () -> IrisShaderCatalog.search(this.query));
        Paint.text(g, IrisShaderCatalog.message(), x + 20, y + 96, Theme.MUTED, 6.2F);
        if (!IrisCompat.available()) Paint.text(g, IrisAutoInstaller.status(), x + 20, y + 108, Theme.ACCENT_HOT, 6.0F);

        float listY = y + 130, rowH = 72, gap = 7;
        float listH = h - 174;
        int columns = w >= 580 ? 2 : 1;
        float colW = (w - 40 - (columns - 1) * 8) / columns;
        int rows = (int)Math.ceil(IrisShaderCatalog.packs().size() / (double)columns);
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, rows * (int)(rowH + gap) - (int)listH)));
        Ui.scissor(x + 16, listY, w - 32, listH);
        for (int i = 0; i < IrisShaderCatalog.packs().size(); i++) {
            IrisShaderCatalog.Pack pack = IrisShaderCatalog.packs().get(i);
            int col = i % columns, row = i / columns;
            float cx = x + 20 + col * (colW + 8), cy = listY + row * (rowH + gap) - this.scroll;
            if (cy + rowH < listY || cy > listY + listH) continue;
            boolean hover = Paint.hit(this.mx, this.my, cx, cy, colW, rowH);
            Paint.box(g, cx, cy, colW, rowH, Theme.alpha(16777215, hover ? 18 : 10), 9);
            if (hover) Paint.outline(g, cx, cy, colW, rowH, Theme.alpha(Theme.ACCENT, 72), 9);
            Paint.text(g, fit(pack.title(), colW - 116, 8), cx + 10, cy + 9, Theme.TEXT, 8);
            Paint.text(g, fit("Автор: " + pack.author() + " · Лицензия: " + (pack.license().isBlank() ? "не указана" : pack.license()), colW - 20, 5.8F), cx + 10, cy + 25, Theme.MUTED, 5.8F);
            Paint.text(g, fit(pack.description(), colW - 20, 5.6F), cx + 10, cy + 39, Theme.GHOST, 5.6F);
            String job = IrisShaderCatalog.job(pack.id());
            String action = job.isBlank() ? "Скачать" : job.startsWith("Установлен") ? (IrisCompat.available() ? "Открыть Iris" : "Перезапуск") : job.startsWith("Ошибка") ? "Повторить" : "Загрузка…";
            float bx = cx + colW - 86, by = cy + 43;
            this.button(g, bx, by, 76, 20, action, !action.equals("Загрузка…") && !action.equals("Перезапуск"), () -> {
                if (job.startsWith("Установлен")) {
                    var iris = IrisCompat.openScreen(this);
                    if (iris != null && this.client != null) this.client.setScreen(iris);
                } else IrisShaderCatalog.install(pack);
            });
        }
        Ui.unscissor();
        this.button(g, x + 18, y + h - 34, 74, 22, "Назад", true, () -> this.close());
        Paint.text(g, "Пакет сохраняется в shaderpacks; выбор и включение выполняются в Iris.", x + 104, y + h - 27, Theme.GHOST, 6.1F);
    }

    private void button(DrawContext g, float x, float y, float w, float h, String label, boolean active, Runnable action) {
        boolean hover = active && Paint.hit(this.mx, this.my, x, y, w, h);
        Paint.box(g, x, y, w, h, Theme.alpha(hover ? Theme.ACCENT : 16777215, hover ? 50 : 14), 7);
        Paint.outline(g, x, y, w, h, Theme.alpha(hover ? Theme.ACCENT_HOT : 16777215, active ? 42 : 18), 7);
        Paint.textC(g, label, x + w * 0.5F, y + (h - 6) * 0.5F, active ? (hover ? Theme.TEXT : Theme.MUTED) : Theme.GHOST, 6.0F);
        if (active) this.hits.add(new Hit(x, y, w, h, action));
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        for (int i = this.hits.size() - 1; i >= 0; i--) {
            Hit hit = this.hits.get(i);
            if (Paint.hit(event.x(), event.y(), hit.x, hit.y, hit.w, hit.h)) { hit.action.run(); return true; }
        }
        this.queryFocused = false;
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        this.scroll = Math.max(0, this.scroll - (int)(vertical * 42));
        return true;
    }

    @Override
    public boolean charTyped(CharInput event) {
        if (this.queryFocused && !Character.isISOControl(event.codepoint())) { this.query += Character.toString(event.codepoint()); return true; }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { this.close(); return true; }
        if (this.queryFocused && event.key() == GLFW.GLFW_KEY_ENTER) { IrisShaderCatalog.search(this.query); this.queryFocused = false; return true; }
        if (this.queryFocused && event.key() == GLFW.GLFW_KEY_BACKSPACE && !this.query.isEmpty()) { this.query = this.query.substring(0, this.query.length() - 1); return true; }
        return super.keyPressed(event);
    }

    private static String fit(String value, float max, float size) {
        String result = value == null ? "" : value;
        while (!result.isEmpty() && Paint.tw(result, size) > max) result = result.substring(0, result.length() - 2) + "…";
        return result;
    }

    @Override public void close() { if (this.client != null) this.client.setScreen(this.parent); }
    private record Hit(float x, float y, float w, float h, Runnable action) {}
}
