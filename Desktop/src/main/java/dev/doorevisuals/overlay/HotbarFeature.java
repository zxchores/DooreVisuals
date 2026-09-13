package dev.doorevisuals.overlay;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.tools.ItemHighlightFeature;
import dev.doorevisuals.tools.ThemeFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

public final class HotbarFeature extends Feature {
    private final Opt.Flag hideVanilla = this.opt(
        new Opt.Flag("hide_vanilla", "\u0421\u043a\u0440\u044b\u0442\u044c \u0432\u0430\u043d\u0438\u043b\u044c\u043d\u044b\u0439", true)
    );
    private final Opt.Flag offhand = this.opt(new Opt.Flag("offhand", "\u041b\u0435\u0432\u0430\u044f \u0440\u0443\u043a\u0430", true));
    private final Opt.Flag numbers = this.opt(new Opt.Flag("numbers", "\u041d\u043e\u043c\u0435\u0440\u0430 \u0441\u043b\u043e\u0442\u043e\u0432", false));
    private final Opt.Num scale = this.opt(new Opt.Num("scale", "\u041c\u0430\u0441\u0448\u0442\u0430\u0431", 1.0, 0.7, 1.4, 0.05));
    private final Opt.Num yOff = this.opt(new Opt.Num("y_off", "\u0421\u043c\u0435\u0449\u0435\u043d\u0438\u0435 Y", 0.0, -40.0, 40.0, 1.0));
    private final Opt.Pick anim = this.opt(
        new Opt.Pick(
            "anim",
            "\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f",
            "\u041c\u044f\u0433\u043a\u0430\u044f",
            "\u041f\u0440\u044b\u0436\u043e\u043a",
            "\u0421\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435",
            "\u041f\u0443\u043b\u044c\u0441",
            "\u041c\u044f\u0433\u043a\u0430\u044f"
        )
    );
    private final Opt.Flag useTheme = this.opt(new Opt.Flag("theme", "\u0426\u0432\u0435\u0442 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint accent = this.opt(new Opt.Tint("accent", "\u0410\u043a\u0446\u0435\u043d\u0442", -12533600));
    private final Anim[] slotPop = new Anim[9];
    private final Anim selectX = new Anim(0.0F);
    private final Anim selectGlow = new Anim(0.0F);
    private final Anim trailX = new Anim(0.0F);
    private int lastSlot = -1;
    private long switchAt;
    private float cachedX0;
    private float cachedY0;
    private float cachedSc = 1.0F;
    private float cachedPad = 1.0F;
    private float cachedSlot = 20.0F;
    private float cachedGap = 0.0F;
    private float cachedOffX;
    private float cachedOffY;
    private float cachedBounce;
    private int cachedSelected;
    private boolean cachedDrawOff;

    public HotbarFeature() {
        super(
            "hotbar",
            "Hotbar",
            "\u0412\u0430\u043d\u0438\u043b\u044c\u043d\u044b\u0439 \u0440\u0430\u0437\u043c\u0435\u0440, \u0441\u043a\u0440\u0443\u0433\u043b\u0435\u043d\u0438\u0435 \u0438 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0441\u043b\u043e\u0442\u043e\u0432",
            Category.OVERLAY,
            true
        );

        for (int i = 0; i < this.slotPop.length; i++) {
            this.slotPop[i] = new Anim(0.0F);
        }
    }

    public boolean hideVanillaBar() {
        return this.on() && (Boolean)this.hideVanilla.get();
    }

    public void paint(DrawContext g) {
        this.paintNvg(g);
        this.paintItems(g);
    }

    public void paintNvg(DrawContext g) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null && !minecraftclient.options.hudHidden) {
            PlayerInventory playerinventory = minecraftclient.player.getInventory();
            int i = playerinventory.getSelectedSlot();
            if (i != this.lastSlot) {
                if (this.lastSlot >= 0) {
                    this.trailX.snap(this.selectX.value());
                }

                this.lastSlot = i;
                this.switchAt = System.currentTimeMillis();
                this.slotPop[i].snap(0.15F);
                this.selectGlow.snap(1.0F);
            }

            float f = this.scale.f();
            float f1 = 20.0F;
            float f2 = 0.0F;
            float f3 = 1.0F;
            float f4 = 182.0F;
            float f5 = 22.0F;
            float f6 = g.getScaledWindowWidth();
            float f7 = g.getScaledWindowHeight();
            float f8 = (f6 - f4 * f) * 0.5F / f;
            float f9 = (f7 - this.yOff.f()) / f - f5;
            int j = this.useTheme.get() ? ThemeFeature.hudTint() : (Integer)this.accent.get();
            float f10 = f3 + i * (f1 + f2);
            float f11 = this.selectX.to(f10, this.animSpeed());
            this.trailX.to(f10, this.animSpeed() * 0.55F);
            this.selectGlow.to(0.0F, 14.0F);
            float f12 = Math.min(1.0F, (float)(System.currentTimeMillis() - this.switchAt) / 220.0F);
            float f13 = this.switchBounce(f12);

            for (int k = 0; k < 9; k++) {
                this.slotPop[k].to(k == i ? 1.0F : 0.0F, 18.0F);
            }

            float f17 = -29.0F;
            float f14 = 0.0F;
            boolean flag = (Boolean)this.offhand.get() && !minecraftclient.player.getOffHandStack().isEmpty();
            float f15 = this.trailX.value();
            float f16 = this.selectGlow.value();
            Runnable runnable = () -> {
                Nvg.push();
                Nvg.scale(f, f);
                Nvg.move(f8, f9);
                Paint.hudBar(g, 0.0F, 0.0F, f4, f5, 5.0F);
                if (Math.abs(f15 - f11) > 0.5F) {
                    Paint.box(g, f15, f3, f1, f1, Theme.alpha(j, 36), 4.0F);
                }

                Paint.box(g, f11, f3, f1, f1, Theme.alpha(j, 42 + Math.round(40.0F * f16)), 4.0F);
                Paint.outline(g, f11, f3, f1, f1, Theme.alpha(j, 150 + Math.round(70.0F * f16)), 4.0F);

                for (int l = 0; l < 9; l++) {
                    float f18 = f3 + l * (f1 + f2);
                    Paint.box(g, f18, f3, f1, f1, Theme.alpha(16777215, l == i ? 18 : 8), 4.0F);
                    ItemStack itemstack = playerinventory.getStack(l);
                    if (ItemHighlightFeature.matches(itemstack)) {
                        Paint.box(g, f18, f3, f1, f1, ItemHighlightFeature.glowColor(), 4.0F);
                        Paint.outline(g, f18, f3, f1, f1, ItemHighlightFeature.rimColor(), 4.0F);
                    }

                    if ((Boolean)this.numbers.get()) {
                        Paint.textC(g, String.valueOf(l + 1), f18 + f1 * 0.5F, f3 + 1.0F, Theme.alpha(16777215, l == i ? 170 : 70), 6.0F);
                    }
                }

                if (flag) {
                    Paint.hudBar(g, -29.0F, 0.0F, 22.0F, f5, 5.0F);
                    Paint.box(g, -28.0F, 1.0F, 20.0F, 20.0F, Theme.alpha(16777215, 10), 4.0F);
                    if (ItemHighlightFeature.matches(minecraftclient.player.getOffHandStack())) {
                        Paint.box(g, -28.0F, 1.0F, 20.0F, 20.0F, ItemHighlightFeature.glowColor(), 4.0F);
                        Paint.outline(g, -28.0F, 1.0F, 20.0F, 20.0F, ItemHighlightFeature.rimColor(), 4.0F);
                    }
                }

                Nvg.pop();
            };
            this.cachedX0 = f8;
            this.cachedY0 = f9;
            this.cachedSc = f;
            this.cachedPad = f3;
            this.cachedSlot = f1;
            this.cachedGap = f2;
            this.cachedOffX = -29.0F;
            this.cachedOffY = 0.0F;
            this.cachedDrawOff = flag;
            this.cachedBounce = f13;
            this.cachedSelected = i;
            if (Nvg.frame()) {
                runnable.run();
            } else {
                Nvg.run(g, runnable);
            }
        }
    }

    public void paintItems(DrawContext g) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null && !minecraftclient.options.hudHidden) {
            PlayerInventory playerinventory = minecraftclient.player.getInventory();
            float f = this.cachedSc > 0.01F ? this.cachedSc : this.scale.f();
            float f1 = this.cachedX0;
            float f2 = this.cachedY0;
            float f3 = this.cachedPad;
            float f4 = this.cachedSlot;
            float f5 = this.cachedGap;
            float f6 = this.cachedBounce;
            int i = this.cachedSelected;
            boolean flag = this.cachedDrawOff;
            g.getMatrices().pushMatrix();

            try {
                g.getMatrices().translate(f1 * f, f2 * f);
                g.getMatrices().scale(f, f);

                for (int j = 0; j < 9; j++) {
                    ItemStack itemstack = playerinventory.getStack(j);
                    if (!itemstack.isEmpty()) {
                        float f7 = this.slotPop[j].value();
                        float f8 = f3 + j * (f4 + f5) + 2.0F;
                        float f9 = f3 + 2.0F;
                        float f10 = 1.0F + this.itemPop(f7, f6, j == i);
                        g.getMatrices().pushMatrix();

                        try {
                            g.getMatrices().translate(f8 + 8.0F, f9 + 8.0F);
                            g.getMatrices().scale(f10, f10);
                            g.getMatrices().translate(-8.0F, -8.0F);
                            g.drawItem(itemstack, 0, 0);
                            g.drawStackOverlay(minecraftclient.textRenderer, itemstack, 0, 0);
                        } finally {
                            g.getMatrices().popMatrix();
                        }
                    }
                }

                if (flag) {
                    ItemStack itemstack1 = minecraftclient.player.getOffHandStack();
                    g.drawItem(itemstack1, Math.round(this.cachedOffX + 3.0F), Math.round(this.cachedOffY + 3.0F));
                    g.drawStackOverlay(minecraftclient.textRenderer, itemstack1, Math.round(this.cachedOffX + 3.0F), Math.round(this.cachedOffY + 3.0F));
                }
            } finally {
                g.getMatrices().popMatrix();
            }
        }
    }

    private float itemPop(float pop, float bounce, boolean selected) {
        if (!selected) {
            return 0.0F;
        } else {
            String s = (String)this.anim.get();

            return switch (s) {
                case "\u041f\u0440\u044b\u0436\u043e\u043a" -> 0.1F * bounce;
                case "\u041f\u0443\u043b\u044c\u0441" -> 0.04F + 0.03F * (0.5F + 0.5F * MathHelper.sin((float)System.currentTimeMillis() * 0.012F));
                case "\u0421\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435" -> 0.05F * pop;
                default -> 0.08F * Anim.easeOut(pop);
            };
        }
    }

    private float switchBounce(float age) {
        String s = (String)this.anim.get();

        return switch (s) {
            case "\u0421\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435" -> Anim.easeOut(age);
            case "\u041f\u0443\u043b\u044c\u0441" -> 1.0F;
            case "\u041c\u044f\u0433\u043a\u0430\u044f" -> Anim.easeInOut(age);
            default -> {
                float f = Anim.easeOut(age);
                yield f < 0.55F ? f / 0.55F * 1.2F : 1.2F - (f - 0.55F) / 0.45F * 0.2F;
            }
        };
    }

    private float animSpeed() {
        String s = (String)this.anim.get();

        return switch (s) {
            case "\u0421\u043a\u043e\u043b\u044c\u0436\u0435\u043d\u0438\u0435" -> 26.0F;
            case "\u041f\u0443\u043b\u044c\u0441" -> 32.0F;
            case "\u041c\u044f\u0433\u043a\u0430\u044f" -> 18.0F;
            default -> 28.0F;
        };
    }
}
