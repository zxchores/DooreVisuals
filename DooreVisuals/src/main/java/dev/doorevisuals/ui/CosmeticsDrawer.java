package dev.doorevisuals.ui;

import dev.doorevisuals.App;
import dev.doorevisuals.cosmetics.CapeFiles;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CosmeticsDrawer {
    private static float previewX;
    private static float previewY;
    private static float previewW;
    private static float previewH;
    private static float previewYaw = 20.0F;
    private static boolean yawDrag;

    private CosmeticsDrawer() {
    }

    public static void draw(
        DrawContext g,
        float alpha,
        float uiScale,
        float frameX,
        float frameY,
        float frameW,
        float frameH,
        int screenW,
        int screenH,
        ThemeConfigUi.Hits hits,
        Runnable close
    ) {
        float f = 300.0F * uiScale;
        float f1 = (1.0F - alpha) * (f + 24.0F * uiScale);
        float f2 = frameX + frameW - f - 10.0F * uiScale + f1;
        float f3 = frameY + 12.0F * uiScale;
        float f4 = frameH - 24.0F * uiScale;
        Paint.box(g, 0, 0, screenW, screenH, Theme.alpha(197639, (int)(90.0F * alpha)), 0.0F);
        hits.add(0.0F, 0.0F, Math.max(0.0F, f2 - 4.0F), screenH, close);
        Paint.shadow(f2, f3, f, f4, 18.0F, 14.0F);
        Paint.box(g, f2, f3, f, f4, Theme.alpha(658706, 245), 14.0F);
        Paint.grad(g, f2, f3, f, 52.0F * uiScale, Theme.alpha(Theme.ACCENT_DIM, 50), Theme.alpha(658706, 0), 14.0F, true);
        Paint.outline(g, f2, f3, f, f4, Theme.alpha(16777215, 28), 14.0F);
        Paint.text(g, "\u041a\u043e\u0441\u043c\u0435\u0442\u0438\u043a\u0430", f2 + 16.0F * uiScale, f3 + 14.0F * uiScale, Theme.TEXT, 12.0F * uiScale);
        Paint.text(
            g,
            "\u041f\u043b\u0430\u0449 \u00b7 \u043a\u0440\u044b\u043b\u044c\u044f \u00b7 \u0448\u043b\u044f\u043f\u0430",
            f2 + 16.0F * uiScale,
            f3 + 30.0F * uiScale,
            Theme.MUTED,
            6.2F * uiScale
        );
        Paint.textR(g, "\u2715", f2 + f - 16.0F * uiScale, f3 + 14.0F * uiScale, Theme.MUTED, 10.0F * uiScale);
        hits.add(f2 + f - 36.0F * uiScale, f3 + 8.0F * uiScale, 28.0F * uiScale, 24.0F * uiScale, close);
        CosmeticsFeature cosmeticsfeature = App.features().find(CosmeticsFeature.class).orElse(null);
        if (cosmeticsfeature == null) {
            Paint.text(
                g,
                "\u041c\u043e\u0434\u0443\u043b\u044c \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d",
                f2 + 16.0F * uiScale,
                f3 + 80.0F * uiScale,
                Theme.GHOST,
                8.0F * uiScale
            );
        } else {
            Paint.text(g, "\u041f\u043b\u0430\u0449", f2 + 16.0F * uiScale, f3 + 56.0F * uiScale, Theme.GHOST, 5.8F * uiScale);
            pick(
                g,
                hits,
                f2 + 16.0F * uiScale,
                f3 + 70.0F * uiScale,
                f - 32.0F * uiScale,
                uiScale,
                cosmeticsfeature.capeLabel(),
                new String[]{"\u041d\u0435\u0442", "Doore", "\u0422\u0451\u043c\u043d\u044b\u0439"},
                cosmeticsfeature::setCapeLabel
            );
            pick(
                g,
                hits,
                f2 + 16.0F * uiScale,
                f3 + 96.0F * uiScale,
                f - 32.0F * uiScale,
                uiScale,
                cosmeticsfeature.capeLabel(),
                new String[]{"\u0411\u0440\u0430\u0442\u044c\u044f", "\u0421\u0432\u0430\u0434\u044c\u0431\u0430", "\u0411\u0435\u043b\u043a\u0430"},
                cosmeticsfeature::setCapeLabel
            );
            Paint.text(g, "\u041a\u0440\u044b\u043b\u044c\u044f", f2 + 16.0F * uiScale, f3 + 128.0F * uiScale, Theme.GHOST, 5.8F * uiScale);
            pick(
                g,
                hits,
                f2 + 16.0F * uiScale,
                f3 + 142.0F * uiScale,
                f - 32.0F * uiScale,
                uiScale,
                cosmeticsfeature.wingsLabel(),
                new String[]{"\u041d\u0435\u0442", "Doore", "\u041f\u0440\u0438\u0437\u0440\u0430\u043a", "\u0418\u0441\u043a\u0440\u0430"},
                cosmeticsfeature::setWingsLabel
            );
            Paint.text(g, "\u0428\u043b\u044f\u043f\u0430", f2 + 16.0F * uiScale, f3 + 170.0F * uiScale, Theme.GHOST, 5.8F * uiScale);
            pick(
                g,
                hits,
                f2 + 16.0F * uiScale,
                f3 + 184.0F * uiScale,
                f - 32.0F * uiScale,
                uiScale,
                cosmeticsfeature.hatLabel(),
                new String[]{"\u041d\u0435\u0442", "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f", "\u0410\u043d\u0433\u0435\u043b", "67"},
                cosmeticsfeature::setHatLabel
            );
            Paint.text(g, "\u0410\u043a\u0441\u0435\u0441\u0441\u0443\u0430\u0440", f2 + 16.0F * uiScale, f3 + 212.0F * uiScale, Theme.GHOST, 5.8F * uiScale);
            pick(
                g,
                hits,
                f2 + 16.0F * uiScale,
                f3 + 226.0F * uiScale,
                f - 32.0F * uiScale,
                uiScale,
                cosmeticsfeature.accessoryLabel(),
                new String[]{"\u041d\u0435\u0442", "\u0423\u0448\u0438", "\u0420\u043e\u0433\u0430", "\u0411\u0430\u043d\u0434\u0430\u043d\u0430"},
                cosmeticsfeature::setAccessoryLabel
            );
            boolean flag = cosmeticsfeature.showBadge();
            Paint.box(
                g,
                f2 + 16.0F * uiScale,
                f3 + 260.0F * uiScale,
                f - 32.0F * uiScale,
                28.0F * uiScale,
                Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 40 : 10),
                8.0F
            );
            Paint.text(
                g,
                flag
                    ? "\u25c6  \u0411\u0435\u0439\u0434\u0436 \u0443 \u043d\u0438\u043a\u0430 \u00b7 \u0432\u043a\u043b"
                    : "\u25c7  \u0411\u0435\u0439\u0434\u0436 \u0443 \u043d\u0438\u043a\u0430 \u00b7 \u0432\u044b\u043a\u043b",
                f2 + 28.0F * uiScale,
                f3 + 268.0F * uiScale,
                flag ? Theme.ACCENT_HOT : Theme.MUTED,
                7.0F * uiScale
            );
            hits.add(
                f2 + 16.0F * uiScale,
                f3 + 260.0F * uiScale,
                f - 32.0F * uiScale,
                28.0F * uiScale,
                () -> cosmeticsfeature.setBadge(!cosmeticsfeature.showBadge())
            );
            float f5 = Math.max(100.0F * uiScale, f4 - 320.0F * uiScale);
            drawPreview(g, f2 + 16.0F * uiScale, f3 + 298.0F * uiScale, f - 32.0F * uiScale, f5);
            hits.add(previewX, previewY, previewW, previewH, CosmeticsDrawer::beginYawDrag);
            Paint.text(g, cosmeticsfeature.selectionSummary(), f2 + 16.0F * uiScale, f3 + f4 - 28.0F * uiScale, Theme.MUTED, 6.0F * uiScale);
            Paint.text(
                g,
                "\u041b\u041a\u041c \u043f\u043e \u043f\u0440\u0435\u0432\u044c\u044e \u2014 \u043a\u0440\u0443\u0442\u0438\u0442\u044c",
                f2 + 16.0F * uiScale,
                f3 + f4 - 16.0F * uiScale,
                Theme.GHOST,
                5.6F * uiScale
            );
            Paint.textR(
                g, "Esc \u2014 \u0437\u0430\u043a\u0440\u044b\u0442\u044c", f2 + f - 16.0F * uiScale, f3 + f4 - 18.0F * uiScale, Theme.GHOST, 5.8F * uiScale
            );
        }
    }

    public static void drawCompact(DrawContext g, float x, float y, float w, float h, float uiScale, ThemeConfigUi.Hits hits) {
        CosmeticsFeature cosmeticsfeature = App.features().find(CosmeticsFeature.class).orElse(null);
        if (cosmeticsfeature == null) {
            Paint.text(
                g,
                "\u041c\u043e\u0434\u0443\u043b\u044c \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d",
                x + 8.0F * uiScale,
                y + 8.0F * uiScale,
                Theme.GHOST,
                7.0F * uiScale
            );
        } else {
            Paint.text(
                g,
                "\u0421\u0435\u0431\u0435 \u0432\u0441\u0435\u0433\u0434\u0430. \u0414\u0440\u0443\u0433\u0438\u043c \u2014 \u0435\u0441\u043b\u0438 \u043d\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d \u043c\u043e\u0434.",
                x + 4.0F * uiScale,
                y + 2.0F * uiScale,
                Theme.GHOST,
                5.2F * uiScale
            );
            Paint.text(
                g,
                "GitHub-\u0440\u0435\u0435\u0441\u0442\u0440 \u0442\u043e\u043b\u044c\u043a\u043e \u0447\u0442\u0435\u043d\u0438\u0435",
                x + 4.0F * uiScale,
                y + 12.0F * uiScale,
                Theme.GHOST,
                5.2F * uiScale
            );
            CapeFiles.scan();
            List<String> list = CapeFiles.allCapeLabels();
            float f = y + 24.0F * uiScale;
            Paint.text(g, "\u041f\u043b\u0430\u0449", x + 4.0F * uiScale, f, Theme.GHOST, 5.4F * uiScale);
            f += 12.0F * uiScale;
            pickWrap(g, hits, x, f, w, uiScale, cosmeticsfeature.capeLabel(), list.toArray(String[]::new), cosmeticsfeature::setCapeLabel);
            f += 28.0F * uiScale;
            Paint.text(g, "\u041a\u0440\u044b\u043b\u044c\u044f", x + 4.0F * uiScale, f, Theme.GHOST, 5.4F * uiScale);
            f += 12.0F * uiScale;
            pick(
                g,
                hits,
                x,
                f,
                w,
                uiScale,
                cosmeticsfeature.wingsLabel(),
                new String[]{"\u041d\u0435\u0442", "Doore", "\u041f\u0440\u0438\u0437\u0440\u0430\u043a"},
                cosmeticsfeature::setWingsLabel
            );
            f += 28.0F * uiScale;
            float f1 = Math.max(72.0F * uiScale, h - (f - y) - 8.0F * uiScale);
            drawPreview(g, x, f, w, f1);
            hits.add(x, f, w, f1, CosmeticsDrawer::beginYawDrag);
        }
    }

    private static void pickWrap(
        DrawContext g, ThemeConfigUi.Hits hits, float x, float y, float w, float uiScale, String current, String[] opts, Consumer<String> set
    ) {
        int i = Math.min(3, Math.max(1, opts.length));
        float f = (w - (i - 1) * 4.0F * uiScale) / i;

        for (int j = 0; j < Math.min(opts.length, 6); j++) {
            float f1 = x + j % i * (f + 4.0F * uiScale);
            float f2 = y + j / i * (20.0F * uiScale);
            boolean flag = opts[j].equals(current);
            Paint.box(g, f1, f2, f, 18.0F * uiScale, Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 48 : 10), 6.0F);
            Paint.textC(g, fit(opts[j], f - 4.0F, 5.4F * uiScale), f1 + f * 0.5F, f2 + 5.0F * uiScale, flag ? Theme.ACCENT_HOT : Theme.MUTED, 5.4F * uiScale);
            String s = opts[j];
            hits.add(f1, f2, f, 18.0F * uiScale, () -> set.accept(s));
        }
    }

    private static String fit(String s, float max, float size) {
        if (Paint.tw(s, size) <= max) {
            return s;
        } else {
            String sx = s;

            while (sx.length() > 2 && Paint.tw(sx + "\u2026", size) > max) {
                sx = sx.substring(0, sx.length() - 1);
            }

            return sx + "\u2026";
        }
    }

    public static void unload() {
        yawDrag = false;
    }

    public static void beginYawDrag() {
        yawDrag = true;
    }

    public static void endYawDrag() {
        yawDrag = false;
    }

    public static boolean mouseDragged(double mx, double my, double dx, double dy) {
        if (!yawDrag) {
            return false;
        } else {
            previewYaw += (float)dx * 0.85F;
            return true;
        }
    }

    public static void drawDummy(DrawContext g, float x, float y, float w, float h) {
        drawPreview(g, x, y, w, h);
    }

    private static void drawPreview(DrawContext g, float x, float y, float w, float h) {
        previewX = x;
        previewY = y;
        previewW = w;
        previewH = h;
        Paint.box(g, x, y, w, h, Theme.alpha(16777215, 8), 8.0F);
        Paint.outline(g, x, y, w, h, Theme.alpha(16777215, 12), 8.0F);
        Paint.text(g, "\u043f\u0440\u0435\u0432\u044c\u044e \u00b7 drag", x + 8.0F, y + 6.0F, Theme.GHOST, 5.4F);
        ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
        if (clientplayerentity == null) {
            Paint.textC(g, "\u0437\u0430\u0439\u0434\u0438 \u0432 \u043c\u0438\u0440", x + w * 0.5F, y + h * 0.5F - 4.0F, Theme.GHOST, 7.0F);
        } else {
            try {
                int i = Math.round(x + 8.0F);
                int j = Math.round(y + 16.0F);
                int k = Math.round(x + w - 8.0F);
                int l = Math.round(y + h - 8.0F);
                int i1 = Math.max(28, Math.round((l - j) * 0.48F));
                renderPlayer(g, i, j, k, l, i1, clientplayerentity);
            } catch (Throwable throwable) {
                Paint.textC(
                    g,
                    "\u043c\u043e\u0434\u0435\u043b\u044c\u043a\u0430 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u0430",
                    x + w * 0.5F,
                    y + h * 0.5F - 4.0F,
                    Theme.GHOST,
                    6.4F
                );
            }
        }
    }

    private static void renderPlayer(DrawContext g, int x1, int y1, int x2, int y2, int size, LivingEntity entity) {
        EntityRenderManager entityrendermanager = MinecraftClient.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> entityrenderer = entityrendermanager.getRenderer(entity);
        EntityRenderState entityrenderstate = entityrenderer.getAndUpdateRenderState(entity, 1.0F);
        entityrenderstate.light = 15728880;
        entityrenderstate.shadowPieces.clear();
        entityrenderstate.outlineColor = 0;
        if (entityrenderstate instanceof LivingEntityRenderState livingentityrenderstate) {
            livingentityrenderstate.bodyYaw = 180.0F + previewYaw;
            livingentityrenderstate.relativeHeadYaw = previewYaw;
            livingentityrenderstate.pitch = -8.0F;
            livingentityrenderstate.width = livingentityrenderstate.width / Math.max(0.01F, livingentityrenderstate.baseScale);
            livingentityrenderstate.height = livingentityrenderstate.height / Math.max(0.01F, livingentityrenderstate.baseScale);
            livingentityrenderstate.baseScale = 1.0F;
        }

        Quaternionf quaternionf1 = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf = new Quaternionf().rotateX((float) (Math.PI / 10));
        quaternionf1.mul(quaternionf);
        Vector3f vector3f = new Vector3f(0.0F, entityrenderstate.height / 2.0F + 0.0625F, 0.0F);
        g.addEntity(entityrenderstate, size, vector3f, quaternionf1, quaternionf, x1, y1, x2, y2);
    }

    private static void pick(
        DrawContext g, ThemeConfigUi.Hits hits, float x, float y, float w, float uiScale, String current, String[] opts, Consumer<String> set
    ) {
        float f = (w - (opts.length - 1) * 6.0F * uiScale) / opts.length;

        for (int i = 0; i < opts.length; i++) {
            float f1 = x + i * (f + 6.0F * uiScale);
            boolean flag = opts[i].equals(current);
            Paint.box(g, f1, y, f, 22.0F * uiScale, Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 48 : 10), 7.0F);
            Paint.textC(g, opts[i], f1 + f * 0.5F, y + 6.0F * uiScale, flag ? Theme.ACCENT_HOT : Theme.MUTED, 6.4F * uiScale);
            String s = opts[i];
            hits.add(f1, y, f, 22.0F * uiScale, () -> set.accept(s));
        }
    }
}
