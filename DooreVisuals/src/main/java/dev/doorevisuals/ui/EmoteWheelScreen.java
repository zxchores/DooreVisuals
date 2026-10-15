package dev.doorevisuals.ui;

import dev.doorevisuals.cosmetics.EmotesFeature;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * The wheel of emotes: eight discs round the middle of the screen, the emotes in pages. Hold the key, point at a disc and let
 * go of the key (or click a disc) to play that emote; the mouse wheel, the arrow keys or the arrows under the wheel turn the
 * page; Esc closes the wheel without playing anything.
 */
public final class EmoteWheelScreen extends Screen {
    private static final float RING = 78.0F;
    private static final float DISC = 27.0F;
    private static final float DEAD = 22.0F;

    private static int page;

    private final int key;
    private final long openedAt = System.currentTimeMillis();
    private int mx;
    private int my;

    public EmoteWheelScreen(int key) {
        super(Text.literal("Эмоции"));
        this.key = key;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0x66000000);
    }

    /** The slot under the pointer, or -1 while it is in the middle. */
    private int hovered() {
        float dx = this.mx - this.width / 2.0F;
        float dy = this.my - this.height / 2.0F;
        if (dx * dx + dy * dy < DEAD * DEAD) {
            return -1;
        }
        double angle = Math.atan2(dx, -dy);
        int slot = (int)Math.floor((angle + Math.PI / EmotesFeature.SLOTS) / (2.0 * Math.PI / EmotesFeature.SLOTS));
        return Math.floorMod(slot, EmotesFeature.SLOTS);
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        // Letting go of the key plays what is pointed at; a quick tap that never pointed at anything just closes the wheel.
        if (this.key > 0 && GLFW.glfwGetKey(mc.getWindow().getHandle(), this.key) != GLFW.GLFW_PRESS && System.currentTimeMillis() - this.openedAt > 150L) {
            this.choose(this.hovered());
        }
    }

    private void choose(int slot) {
        this.close();
        String name = slot >= 0 ? EmotesFeature.at(page, slot) : null;
        if (name != null) {
            EmotesFeature.play(name);
        }
    }

    private void turn(int by) {
        page = Math.floorMod(page + by, EmotesFeature.pages());
    }

    private float arrowY() {
        return this.height / 2.0F + RING + DISC + 16.0F;
    }

    /** -1 for the left arrow, 1 for the right one, 0 elsewhere. */
    private int arrowAt(double x, double y) {
        float cx = this.width / 2.0F;
        float ay = this.arrowY();
        if (Paint.hit(x, y, cx - 62.0F, ay - 10.0F, 24.0F, 20.0F)) {
            return -1;
        }
        return Paint.hit(x, y, cx + 38.0F, ay - 10.0F, 24.0F, 20.0F) ? 1 : 0;
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        super.render(g, mouseX, mouseY, partialTick);
        Ui.frame(g, () -> this.paint(g));
    }

    private void paint(DrawContext g) {
        float cx = this.width / 2.0F;
        float cy = this.height / 2.0F;
        float open = Math.min(1.0F, (System.currentTimeMillis() - this.openedAt) / 140.0F);
        open = 1.0F - (1.0F - open) * (1.0F - open);
        int hover = this.hovered();
        Paint.ring(g, cx, cy, RING * open, 1.2F, Theme.alpha(0xFFFFFF, 40));
        for (int i = 0; i < EmotesFeature.SLOTS; i++) {
            double a = i * (2.0 * Math.PI / EmotesFeature.SLOTS);
            float x = cx + (float)Math.sin(a) * RING * open;
            float y = cy - (float)Math.cos(a) * RING * open;
            String name = EmotesFeature.at(page, i);
            if (name == null) {
                Paint.circle(g, x, y, DISC - 4.0F, Theme.alpha(0x14121C, 90));
                continue;
            }
            boolean on = i == hover;
            float r = on ? DISC + 3.0F : DISC;
            Paint.circle(g, x, y, r, on ? Theme.alpha(Theme.ACCENT, 200) : Theme.alpha(0x14121C, 215));
            Paint.ring(g, x, y, r, 1.0F, on ? Theme.alpha(0xFFFFFF, 140) : Theme.alpha(0xFFFFFF, 40));
            Paint.textC(g, name, x, y - 3.5F, on ? Theme.VOID : Theme.TEXT, 6.4F);
        }
        Paint.circle(g, cx, cy, DEAD, Theme.alpha(0x14121C, 200));
        String chosen = hover >= 0 ? EmotesFeature.at(page, hover) : null;
        Paint.textC(g, chosen != null ? chosen : "Эмоции", cx, cy - 3.5F, chosen != null ? Theme.TEXT : Theme.MUTED, 6.0F);
        // The pages: the arrows and the number of the page under the wheel.
        float ay = this.arrowY();
        int arrow = this.arrowAt(this.mx, this.my);
        Paint.box(g, cx - 62.0F, ay - 10.0F, 24.0F, 20.0F, Theme.alpha(arrow < 0 ? Theme.ACCENT : 0x14121C, arrow < 0 ? 200 : 215), 6.0F);
        Paint.box(g, cx + 38.0F, ay - 10.0F, 24.0F, 20.0F, Theme.alpha(arrow > 0 ? Theme.ACCENT : 0x14121C, arrow > 0 ? 200 : 215), 6.0F);
        Paint.textC(g, "<", cx - 50.0F, ay - 4.0F, Theme.TEXT, 7.4F);
        Paint.textC(g, ">", cx + 50.0F, ay - 4.0F, Theme.TEXT, 7.4F);
        Paint.textC(g, (page + 1) + " / " + EmotesFeature.pages(), cx, ay - 3.5F, Theme.TEXT, 6.4F);
    }

    @Override
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 0) {
            this.mx = (int)event.x();
            this.my = (int)event.y();
            int arrow = this.arrowAt(event.x(), event.y());
            if (arrow != 0) {
                this.turn(arrow);
                return true;
            }
            int slot = this.hovered();
            if (slot >= 0) {
                this.choose(slot);
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0.0) {
            this.turn(vertical < 0.0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            this.turn(-1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            this.turn(1);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(event);
    }
}
