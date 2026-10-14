package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The pause menu, stripped down to what is needed: return to the game, advancements, statistics, options,
 * open to LAN and leave. The vanilla buttons keep doing the work, only their layout and look are replaced.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    @Unique
    private final List<ClickableWidget> doore$buttons = new ArrayList<>();
    @Unique
    private final Map<ClickableWidget, Anim> doore$hover = new HashMap<>();
    @Unique
    private long doore$last = System.nanoTime();
    @Unique
    private int doore$headerY;

    protected GameMenuScreenMixin() {
        super(Text.empty());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void doore$pauseLayout(CallbackInfo ci) {
        this.doore$buttons.clear();
        List<ClickableWidget> all = new ArrayList<>();
        for (Element child : this.children()) {
            if (child instanceof ClickableWidget widget) {
                all.add(widget);
            }
        }
        ClickableWidget ret = null;
        ClickableWidget exit = null;
        List<ClickableWidget> small = new ArrayList<>();
        ClickableWidget adv = null;
        ClickableWidget stats = null;
        ClickableWidget opts = null;
        ClickableWidget lan = null;
        for (ClickableWidget widget : all) {
            String label = widget.getMessage().getString();
            if (label.equals(Text.translatable("menu.returnToGame").getString())) {
                ret = widget;
            } else if (label.equals(Text.translatable("gui.advancements").getString())) {
                adv = widget;
            } else if (label.equals(Text.translatable("gui.stats").getString())) {
                stats = widget;
            } else if (label.equals(Text.translatable("menu.options").getString())) {
                opts = widget;
            } else if (label.equals(Text.translatable("menu.shareToLan").getString())) {
                lan = widget;
            } else if (label.equals(Text.translatable("menu.returnToMenu").getString()) || label.equals(Text.translatable("menu.disconnect").getString())) {
                exit = widget;
            } else {
                this.remove(widget);
            }
        }
        if (ret == null || exit == null) {
            return;
        }
        for (ClickableWidget widget : new ClickableWidget[] {adv, stats, opts, lan}) {
            if (widget != null) {
                small.add(widget);
            }
        }

        int w = Math.min(320, this.width - 32);
        int gap = 8;
        int bh = this.height < 360 ? 22 : 26;
        int rows = (small.size() + 1) / 2;
        int headerH = 64;
        int blockH = bh + gap + rows * (bh + gap) + 6 + bh;
        int top = Math.max(10, (this.height - headerH - blockH) / 2);
        int left = (this.width - w) / 2;
        this.doore$headerY = top;

        int y = top + headerH;
        this.doore$place(ret, left, y, w, bh);
        y += bh + gap;
        for (int i = 0; i < small.size(); i += 2) {
            boolean pair = i + 1 < small.size();
            int half = (w - gap) / 2;
            this.doore$place(small.get(i), left, y, pair ? half : w, bh);
            if (pair) {
                this.doore$place(small.get(i + 1), left + half + gap, y, w - half - gap, bh);
            }
            y += bh + gap;
        }
        y += 6;
        this.doore$place(exit, left, y, w, bh);
    }

    @Unique
    private void doore$place(ClickableWidget widget, int x, int y, int w, int h) {
        widget.setPosition(x, y);
        widget.setWidth(w);
        widget.setHeight(h);
        this.doore$buttons.add(widget);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void doore$pauseRender(DrawContext g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!App.live() || this.doore$buttons.isEmpty()) {
            return;
        }
        ci.cancel();
        long now = System.nanoTime();
        float dt = Math.max(0.004F, Math.min(0.033F, (now - this.doore$last) / 1.0E9F));
        this.doore$last = now;
        Ui.frame(g, () -> {
            Paint.textC(g, "DooreVisuals", this.width / 2.0F, this.doore$headerY, Theme.ACCENT_HOT, 13.0F);
            Paint.textC(g, "Меню игры", this.width / 2.0F, this.doore$headerY + 26, Theme.MUTED, 10.0F);
            for (ClickableWidget widget : this.doore$buttons) {
                float x = widget.getX();
                float y = widget.getY();
                float w = widget.getWidth();
                float h = widget.getHeight();
                boolean hot = widget.active && Paint.hit(mouseX, mouseY, x, y, w, h);
                Anim anim = this.doore$hover.computeIfAbsent(widget, k -> new Anim(0.0F));
                float hv = anim.springHover(hot ? 1.0F : 0.0F, dt);
                int fill = Theme.lerp(0xE8121018, 0xFFF2F0F6, hv);
                Paint.box(g, x, y, w, h, fill, 9.0F);
                int text = !widget.active ? Theme.GHOST : Theme.lerp(Theme.TEXT | 0xFF000000, 0xFF1A1820, hv);
                float size = 7.6F;
                Paint.textC(g, widget.getMessage().getString(), x + w / 2.0F, y + (h - size * 1.56F) / 2.0F, text, size);
            }
        });
    }
}