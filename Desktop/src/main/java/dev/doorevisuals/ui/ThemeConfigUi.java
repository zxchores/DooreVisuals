package dev.doorevisuals.ui;

import com.github.noamm9.nvgrenderer.nvg.Image;
import dev.doorevisuals.App;
import dev.doorevisuals.data.Account;
import dev.doorevisuals.data.BannerLibrary;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.data.Folders;
import dev.doorevisuals.data.GuiLayout;
import dev.doorevisuals.data.Profiles;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.ChatFeature;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.DiscordFeature;
import dev.doorevisuals.tools.GuiWindowsFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;

public final class ThemeConfigUi {
    private static final DateTimeFormatter CFG_DATE = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
    private static String pendingDelete;
    private static float configScroll;
    private static float themeScroll;
    private static float mx;
    private static float my;
    private static float dt = 0.016666668F;
    private static final Map<String, Anim> TOGGLE = new HashMap<>();
    private static final Map<String, Anim> HOVER = new HashMap<>();

    private ThemeConfigUi() {
    }

    public static void drawTheme(
        DrawContext g,
        float px,
        float py,
        float w,
        float h,
        float s,
        float mouseX,
        float mouseY,
        float frameDt,
        ThemeConfigUi.Hits hits,
        Runnable focusName,
        Consumer<ThemeFeature> openColor,
        boolean nameFocus,
        String caret
    ) {
        ThemeFeature themefeature = App.features().find(ThemeFeature.class).orElse(null);
        if (themefeature != null) {
            mx = mouseX;
            my = mouseY;
            dt = frameDt;
            Nvg.scissor(px, py, w, h);
            float f = py + 10.0F * s + themeScroll;
            float f1 = 14.0F * s;
            Paint.text(g, "\u2699", px + f1, f, Theme.ACCENT_HOT, 10.0F * s);
            Paint.text(g, "Settings", px + f1 + 16.0F * s, f + 1.0F * s, Theme.TEXT, 9.2F * s);
            f += 22.0F * s;
            f = section(g, px, f, w, s, "\u043e\u043a\u043d\u043e");
            f = toggleRow(g, hits, px, f, w, s, "dim", "Background dim", GuiWindowsFeature.dimOn(), GuiWindowsFeature::toggleDim);
            if (GuiWindowsFeature.dimOn()) {
                String[] astring = new String[]{
                    "\u0421\u043b\u0430\u0431\u043e\u0435", "\u0421\u0440\u0435\u0434\u043d\u0435\u0435", "\u0421\u0438\u043b\u044c\u043d\u043e\u0435"
                };
                int[] aint = new int[]{70, 140, 180};
                float f2 = (w - f1 * 2.0F - 8.0F * s) / 3.0F;
                int i = GuiWindowsFeature.dimStrength();

                for (int j = 0; j < astring.length; j++) {
                    float f3 = px + f1 + j * (f2 + 4.0F * s);
                    boolean flag = Math.abs(i - aint[j]) <= 20;
                    int k = aint[j];
                    pill(g, hits, f3, f, f2, 18.0F * s, astring[j], flag, () -> GuiWindowsFeature.setDimStrength(k), s);
                }

                f += 24.0F * s;
            }

            boolean flag4 = App.features().find(DiscordFeature.class).map(FeatureOn -> FeatureOn.on()).orElse(false);
            f = toggleRow(
                g, hits, px, f, w, s, "hide_dc", "Hide Discord", !flag4, () -> App.features().find(DiscordFeature.class).ifPresent(fx -> fx.set(!fx.on()))
            );
            f = toggleRow(g, hits, px, f, w, s, "uniform", "Uniform panels", GuiLayout.uniformPanels, () -> {
                GuiLayout.uniformPanels = !GuiLayout.uniformPanels;
                themefeature.reapplyChrome();
                if (App.live()) {
                    App.save().defer();
                }
            });
            f = section(g, px, f, w, s, "Interface scale");
            float[] afloat = new float[]{0.7F, 0.85F, 1.0F, 1.15F, 1.3F, 1.5F};
            String[] astring1 = new String[]{"70%", "85%", "100%", "115%", "130%", "150%"};
            float f9 = (w - f1 * 2.0F - 20.0F * s) / 6.0F;

            for (int k1 = 0; k1 < afloat.length; k1++) {
                float f10 = px + f1 + k1 * (f9 + 4.0F * s);
                boolean flag5 = Math.abs(GuiLayout.scale() - afloat[k1]) < 0.02F;
                float f13 = afloat[k1];
                pill(g, hits, f10, f, f9, 18.0F * s, astring1[k1], flag5, () -> GuiLayout.scale(f13), s);
            }

            f += 26.0F * s;
            f = section(g, px, f, w, s, "\u0446\u0432\u0435\u0442\u0430");
            String[] astring2 = new String[]{"GUI", "HUD", "\u042d\u0444\u0444\u0435\u043a\u0442", "\u0412\u0442\u043e\u0440\u043e\u0439"};
            float f11 = (w - f1 * 2.0F - 12.0F * s) / 4.0F;

            for (int l1 = 0; l1 < 4; l1++) {
                float f14 = px + f1 + l1 * (f11 + 4.0F * s);
                int l = themefeature.channelColor(l1);
                boolean flag1 = themefeature.editChannel() == l1;
                Paint.box(g, f14, f, f11, 28.0F * s, Theme.alpha(flag1 ? Theme.ACCENT : 16777215, flag1 ? 40 : 12), 8.0F * s);
                Nvg.circle(f14 + 12.0F * s, f + 14.0F * s, 6.0F * s, l);
                Paint.text(g, astring2[l1], f14 + 22.0F * s, f + 10.0F * s, Theme.TEXT, 5.6F * s);
                int i1 = l1;
                hits.add(f14, f, f11, 28.0F * s, () -> {
                    themefeature.setEditChannel(i1);
                    openColor.accept(themefeature);
                });
            }

            f += 36.0F * s;
            f = section(g, px, f, w, s, "Show in profile");
            float f12 = (w - f1 * 2.0F - 8.0F * s) / 2.0F;
            pill(g, hits, px + f1, f, f12, 18.0F * s, "Avatar", GuiLayout.showProfileAvatar, () -> {
                GuiLayout.showProfileAvatar = !GuiLayout.showProfileAvatar;
                if (App.live()) {
                    App.save().defer();
                }
            }, s);
            pill(g, hits, px + f1 + f12 + 8.0F * s, f, f12, 18.0F * s, "Nickname", GuiLayout.showProfileNick, () -> {
                GuiLayout.showProfileNick = !GuiLayout.showProfileNick;
                if (App.live()) {
                    App.save().defer();
                }
            }, s);
            f += 22.0F * s;
            float f15 = (w - f1 * 2.0F - 8.0F * s) / 2.0F;
            String sx = Account.roleLabel();
            String s1 = Account.planLabel();
            int i2 = "developer".equals(sx) ? Theme.alpha(Theme.ACCENT, 70) : Theme.alpha(16777215, 12);
            Paint.box(g, px + f1, f, f15, 16.0F * s, i2, 7.0F * s);
            Paint.textC(g, sx, px + f1 + f15 * 0.5F, f + 4.0F * s, "developer".equals(sx) ? Theme.VOID : Theme.TEXT, 5.6F * s);
            Paint.box(g, px + f1 + f15 + 8.0F * s, f, f15, 16.0F * s, Theme.alpha(16777215, 12), 7.0F * s);
            Paint.textC(g, s1, px + f1 + f15 + 8.0F * s + f15 * 0.5F, f + 4.0F * s, Theme.TEXT, 5.6F * s);
            f += 22.0F * s;
            f = toggleRow(
                g,
                hits,
                px,
                f,
                w,
                s,
                "banner_labels",
                "\u041f\u043e\u0434\u043f\u0438\u0441\u0438 \u0431\u0430\u043d\u043d\u0435\u0440\u0430",
                GuiLayout.bannerLabels,
                () -> {
                    GuiLayout.bannerLabels = !GuiLayout.bannerLabels;
                    if (App.live()) {
                        App.save().defer();
                    }
                }
            );
            f = section(g, px, f, w, s, "Profile banner");
            Paint.text(g, "PNG / GIF / MP4", px + f1, f - 12.0F * s, Theme.GHOST, 5.3F * s);
            btn(
                g,
                hits,
                px + w - f1 - 78.0F * s,
                f - 16.0F * s,
                78.0F * s,
                16.0F * s,
                "\u043f\u0430\u043f\u043a\u0430",
                () -> {
                    Folders.ensure(BannerLibrary.dir());
                    Folders.open(BannerLibrary.dir());
                    BannerLibrary.reload();
                    App.features()
                        .find(HudFeature.class)
                        .ifPresent(hud -> hud.notify("\u0411\u0430\u043d\u043d\u0435\u0440\u044b", Folders.pretty(BannerLibrary.dir())));
                },
                s
            );
            f += 4.0F * s;
            float f4 = (w - f1 * 2.0F - 8.0F * s) / 2.0F;
            float f5 = 58.0F * s;
            int j1 = 0;

            for (BannerLibrary.Entry bannerlibrary$entry : BannerLibrary.entries()) {
                float f6 = px + f1 + j1 * (f4 + 8.0F * s);
                boolean flag2 = bannerlibrary$entry.id().equals(BannerLibrary.selectedId());
                boolean flag3 = Paint.hit((double)mx, (double)my, f6, f, f4, f5);
                float f7 = hoverOf(bannerlibrary$entry.id(), flag3 || flag2);
                Paint.box(g, f6, f, f4, f5, Theme.alpha(flag2 ? Theme.ACCENT : 16777215, flag2 ? 38 : (int)(8.0F + 10.0F * f7)), 9.0F * s);
                if (flag2) {
                    Paint.outline(g, f6, f, f4, f5, Theme.alpha(Theme.ACCENT_HOT, 140), 9.0F * s);
                }

                Image image = BannerLibrary.preview(bannerlibrary$entry);
                float f8 = f5 - 18.0F * s;
                if (image != null) {
                    Nvg.imageCover(image, f6 + 4.0F * s, f + 4.0F * s, f4 - 8.0F * s, f8, 7.0F * s);
                } else {
                    Paint.liveBanner(g, f6 + 4.0F * s, f + 4.0F * s, f4 - 8.0F * s, f8, 7.0F * s);
                }

                Paint.box(g, f6 + 4.0F * s, f + 4.0F * s + f8 - 10.0F * s, f4 - 8.0F * s, 10.0F * s, Theme.alpha(329483, 120), 0.0F);
                Paint.text(
                    g,
                    fit(bannerlibrary$entry.label(), f4 - 12.0F * s, 5.5F * s),
                    f6 + 7.0F * s,
                    f + f5 - 12.0F * s,
                    flag2 ? Theme.TEXT : Theme.MUTED,
                    5.5F * s
                );
                hits.add(f6, f, f4, f5, () -> BannerLibrary.select(bannerlibrary$entry.id()));
                if (++j1 >= 2) {
                    j1 = 0;
                    f += f5 + 8.0F * s;
                }
            }

            if (j1 == 1) {
                f += f5 + 8.0F * s;
            }

            f = section(g, px, f, w, s, "Quick access");
            float f16 = (w - f1 * 2.0F - 8.0F * s) / 2.0F;
            boolean flag6 = App.features().find(ChatFeature.class).map(FeatureOn -> FeatureOn.on()).orElse(false);
            pill(g, hits, px + f1, f, f16, 18.0F * s, "Chat", flag6, () -> App.features().find(ChatFeature.class).ifPresent(fx -> fx.set(!fx.on())), s);
            pill(
                g,
                hits,
                px + f1 + f16 + 8.0F * s,
                f,
                f16,
                18.0F * s,
                "Configs",
                GuiWindowsFeature.showConfig(),
                () -> GuiWindowsFeature.setShowConfig(!GuiWindowsFeature.showConfig()),
                s
            );
            f += 26.0F * s;
            Paint.text(g, "\u041f\u0435\u0440\u0435\u0442\u0430\u0449\u0438 Cosmetics / Friends / Config", px + f1, f, Theme.GHOST, 5.4F * s);
            f += 16.0F * s;
            f = section(g, px, f, w, s, "\u0435\u0449\u0451");
            f = toggleRow(g, hits, px, f, w, s, "sfx", "\u0417\u0432\u0443\u043a GUI", GuiWindowsFeature.soundOn(), GuiWindowsFeature::toggleSound);
            String[] astring3 = new String[]{
                "\u041e\u0431\u044b\u0447\u043d\u044b\u0435", "\u0420\u0435\u0437\u043a\u0438\u0435", "\u041c\u044f\u0433\u043a\u0438\u0435"
            };
            float f17 = (w - f1 * 2.0F - 8.0F * s) / 3.0F;

            for (int j2 = 0; j2 < astring3.length; j2++) {
                float f18 = px + f1 + j2 * (f17 + 4.0F * s);
                boolean flag7 = astring3[j2].equals(GuiWindowsFeature.motionLabel());
                String s3 = astring3[j2];
                pill(g, hits, f18, f, f17, 18.0F * s, s3, flag7, () -> GuiWindowsFeature.setMotionLabel(s3), s);
            }

            f += 26.0F * s;
            Paint.box(g, px + f1, f, w - f1 * 2.0F - 88.0F * s, 20.0F * s, Theme.alpha(16777215, nameFocus ? 16 : 10), 7.0F * s);
            if (nameFocus) {
                Paint.outline(g, px + f1, f, w - f1 * 2.0F - 88.0F * s, 20.0F * s, Theme.alpha(Theme.ACCENT, 80), 7.0F * s);
            }

            String s2 = themefeature.draftName().isEmpty() && !nameFocus
                ? "\u0438\u043c\u044f \u0442\u0435\u043c\u044b\u2026"
                : themefeature.draftName() + (nameFocus ? caret : "");
            Paint.text(g, s2, px + f1 + 7.0F * s, f + 6.0F * s, !nameFocus && themefeature.draftName().isEmpty() ? Theme.GHOST : Theme.TEXT, 6.2F * s);
            hits.add(px + f1, f, w - f1 * 2.0F - 88.0F * s, 20.0F * s, focusName);
            btn(g, hits, px + w - f1 - 80.0F * s, f, 80.0F * s, 20.0F * s, "\u0441\u043e\u0445\u0440", themefeature::saveDraft, s);
            f += 28.0F * s;
            float f19 = f - (py + themeScroll);
            float f20 = Math.min(0.0F, h - f19 - 8.0F * s);
            themeScroll = Math.max(f20, Math.min(0.0F, themeScroll));
            Nvg.unscissor();
            Paint.scrollbar(g, px + w - 6.0F * s, py + 8.0F * s, h - 16.0F * s, f19, h, themeScroll);
        }
    }

    public static void drawConfig(
        DrawContext g,
        float px,
        float py,
        float w,
        float h,
        float s,
        float mouseX,
        float mouseY,
        float frameDt,
        ThemeConfigUi.Hits hits,
        Runnable focusName,
        Runnable focusSearch,
        boolean nameFocus,
        boolean searchFocus,
        String caret
    ) {
        ConfigFeature configfeature = App.features().find(ConfigFeature.class).orElse(null);
        if (configfeature != null) {
            mx = mouseX;
            my = mouseY;
            dt = frameDt;
            float f = 12.0F * s;
            float f1 = py + 8.0F * s;
            float f2 = w - f * 2.0F - 52.0F * s;
            Paint.box(g, px + f, f1, Math.max(80.0F * s, f2), 20.0F * s, Theme.alpha(16777215, nameFocus ? 16 : 12), 7.0F * s);
            if (nameFocus) {
                Paint.outline(g, px + f, f1, Math.max(80.0F * s, f2), 20.0F * s, Theme.alpha(Theme.ACCENT, 70), 7.0F * s);
            }

            String sx = configfeature.draftName().isEmpty() && !nameFocus ? "Config name" : configfeature.draftName() + (nameFocus ? caret : "");
            Paint.text(g, sx, px + f + 8.0F * s, f1 + 6.0F * s, !nameFocus && configfeature.draftName().isEmpty() ? Theme.GHOST : Theme.TEXT, 6.4F * s);
            hits.add(px + f, f1, Math.max(80.0F * s, f2), 20.0F * s, focusName);
            float f3 = px + f + Math.max(80.0F * s, f2) + 6.0F * s;
            btn(g, hits, f3, f1, 20.0F * s, 20.0F * s, "+", () -> {
                if (configfeature.draftName().isBlank()) {
                    configfeature.setDraftName("config");
                }

                configfeature.saveDraft();
            }, s);
            btn(g, hits, f3 + 24.0F * s, f1, 20.0F * s, 20.0F * s, "\ud83d\udcc1", configfeature::openConfigsFolder, s);
            f1 += 28.0F * s;
            if (pendingDelete != null) {
                Paint.box(g, px + f, f1, w - f * 2.0F, 44.0F * s, Theme.alpha(Theme.BAD, 28), 8.0F * s);
                Paint.text(g, "Delete \u00ab" + Profiles.fileLabel(pendingDelete) + "\u00bb?", px + f + 8.0F * s, f1 + 8.0F * s, Theme.TEXT, 6.4F * s);
                btn(g, hits, px + f + 8.0F * s, f1 + 24.0F * s, 52.0F * s, 14.0F * s, "Yes", () -> {
                    configfeature.deleteNamed(pendingDelete);
                    pendingDelete = null;
                }, s);
                btn(g, hits, px + f + 66.0F * s, f1 + 24.0F * s, 52.0F * s, 14.0F * s, "No", () -> pendingDelete = null, s);
            } else {
                List<String> list = configfeature.list();
                float f4 = f1;
                float f5 = py + h - 8.0F * s - f1;
                if (list.isEmpty()) {
                    Paint.text(g, "Drop test.cfg \u0441\u044e\u0434\u0430", px + f + 10.0F * s, f1 + 14.0F * s, Theme.TEXT, 6.6F * s);
                    Paint.text(g, Folders.pretty(ClientPaths.configs()), px + f + 10.0F * s, f1 + 30.0F * s, Theme.GHOST, 5.4F * s);
                } else {
                    Nvg.scissor(px + f, f1, w - f * 2.0F, f5);
                    float f6 = 40.0F * s;
                    float f7 = f1 + 4.0F * s + configScroll;
                    float f8 = 8.0F * s;

                    for (String s1 : list) {
                        boolean flag = s1.equalsIgnoreCase(ConfigFeature.activeName());
                        boolean flag1 = Paint.hit((double)mx, (double)my, px + f, f7, w - f * 2.0F, f6 - 4.0F * s);
                        if (f7 + f6 >= f4 && f7 <= f4 + f5) {
                            Paint.box(
                                g,
                                px + f,
                                f7,
                                w - f * 2.0F,
                                f6 - 4.0F * s,
                                flag ? Theme.alpha(Theme.ACCENT, 28) : Theme.alpha(16777215, flag1 ? 12 : 5),
                                8.0F * s
                            );
                            Paint.text(g, Profiles.fileLabel(s1), px + f + 10.0F * s, f7 + 7.0F * s, flag ? Theme.ACCENT_HOT : Theme.TEXT, 7.2F * s);
                            Paint.text(g, profileSub(s1), px + f + 10.0F * s, f7 + 20.0F * s, Theme.GHOST, 5.2F * s);
                            hits.add(px + f + 10.0F * s, f7, w - f * 2.0F - 40.0F * s, f6 - 4.0F * s, () -> configfeature.applyNamed(s1));
                            if (flag) {
                                Paint.textC(g, "\ud83d\udd12", px + w - f - 16.0F * s, f7 + 12.0F * s, Theme.GHOST, 8.0F * s);
                            } else {
                                btn(g, hits, px + w - f - 22.0F * s, f7 + 10.0F * s, 14.0F * s, 16.0F * s, "\u00d7", () -> pendingDelete = s1, s);
                            }
                        }

                        f7 += f6;
                        f8 += f6;
                    }

                    Nvg.unscissor();
                    float f9 = Math.min(0.0F, f5 - f8 - 4.0F * s);
                    configScroll = Math.max(f9, Math.min(0.0F, configScroll));
                    Paint.scrollbar(g, px + w - f - 5.0F * s, f4 + 4.0F * s, f5 - 8.0F * s, f8, f5, configScroll);
                }
            }
        }
    }

    public static boolean scrollConfig(float dy) {
        configScroll += dy;
        return true;
    }

    public static boolean scrollTheme(float dy) {
        themeScroll += dy;
        return true;
    }

    private static float section(DrawContext g, float px, float y, float w, float s, String title) {
        Nvg.circle(px + 16.0F * s, y + 5.0F * s, 2.1F * s, Theme.ACCENT);
        Paint.text(g, title, px + 22.0F * s, y + 2.0F * s, Theme.MUTED, 6.1F * s);
        Paint.box(
            g,
            px + 22.0F * s + Paint.tw(title, 6.1F * s) + 8.0F * s,
            y + 6.5F * s,
            w - 48.0F * s - Paint.tw(title, 6.1F * s),
            1.0F * s,
            Theme.alpha(16777215, 16),
            0.0F
        );
        return y + 16.0F * s;
    }

    private static float toggleRow(
        DrawContext g, ThemeConfigUi.Hits hits, float px, float y, float w, float s, String id, String label, boolean on, Runnable act
    ) {
        boolean flag = Paint.hit((double)mx, (double)my, px, y, w, 22.0F * s);
        float f = hoverOf(id, flag);
        if (f > 0.02F) {
            Paint.box(g, px + 8.0F * s, y, w - 16.0F * s, 20.0F * s, Theme.alpha(16777215, (int)(10.0F * f)), 8.0F * s);
        }

        Paint.text(g, label, px + 14.0F * s, y + 5.0F * s, Theme.TEXT, 7.0F * s);
        Anim anim = TOGGLE.computeIfAbsent(id, k -> new Anim(on ? 1.0F : 0.0F));
        float f1 = anim.springToggle(on ? 1.0F : 0.0F, dt);
        Paint.toggle(g, px + w - 14.0F * s - 34.0F, y + 3.0F * s, on, f1);
        hits.add(px, y, w, 20.0F * s, () -> {
            act.run();
            anim.snap(!on ? 1.0F : 0.0F);
        });
        return y + 22.0F * s;
    }

    private static void pill(DrawContext g, ThemeConfigUi.Hits hits, float x, float y, float w, float h, String label, boolean on, Runnable act, float s) {
        boolean flag = Paint.hit((double)mx, (double)my, x, y, w, h);
        if (on) {
            Paint.box(g, x, y, w, h, Theme.ACCENT, 7.0F * s);
            Paint.textC(g, label, x + w * 0.5F, y + (h - 6.2F * s) * 0.5F, Theme.VOID, 5.8F * s);
        } else {
            Paint.box(g, x, y, w, h, Theme.alpha(16777215, flag ? 18 : 11), 7.0F * s);
            Paint.textC(g, label, x + w * 0.5F, y + (h - 6.2F * s) * 0.5F, Theme.TEXT, 5.8F * s);
        }

        hits.add(x, y, w, h, act);
    }

    private static float hoverOf(String id, boolean hover) {
        return HOVER.computeIfAbsent(id, k -> new Anim(0.0F)).springHover(hover ? 1.0F : 0.0F, dt);
    }

    private static String profileSub(String name) {
        if (!App.live()) {
            return "";
        } else {
            Profiles.Metadata profiles$metadata = App.save().profiles().metadata(name);
            String s = profiles$metadata.author() != null && !profiles$metadata.author().isBlank() ? profiles$metadata.author() : "player";
            if (profiles$metadata.updatedAt() <= 0L) {
                return s;
            } else {
                String s1 = Instant.ofEpochMilli(profiles$metadata.updatedAt()).atZone(ZoneId.systemDefault()).format(CFG_DATE);
                return s + " - " + s1;
            }
        }
    }

    private static String fit(String text, float maxW, float size) {
        if (text == null) {
            return "";
        } else if (Paint.tw(text, size) <= maxW) {
            return text;
        } else {
            String s = "\u2026";
            int i = 0;
            int j = text.length();

            while (i < j) {
                int k = (i + j + 1) / 2;
                if (Paint.tw(text.substring(0, k) + s, size) <= maxW) {
                    i = k;
                } else {
                    j = k - 1;
                }
            }

            return i <= 0 ? s : text.substring(0, i) + s;
        }
    }

    private static void btn(DrawContext g, ThemeConfigUi.Hits hits, float x, float y, float w, float h, String label, Runnable act, float s) {
        boolean flag = Paint.hit((double)mx, (double)my, x, y, w, h);
        Paint.box(g, x, y, w, h, Theme.alpha(Theme.ACCENT, flag ? 70 : 42), 6.0F * s);
        if (flag) {
            Paint.outline(g, x, y, w, h, Theme.alpha(Theme.ACCENT_HOT, 80), 6.0F * s);
        }

        Paint.textC(g, label, x + w * 0.5F, y + (h - 7.0F * s) * 0.5F, Theme.TEXT, 5.8F * s);
        hits.add(x, y, w, h, act);
    }

    public interface Hits {
        void add(float var1, float var2, float var3, float var4, Runnable var5);
    }
}
