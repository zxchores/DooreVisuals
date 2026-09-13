package dev.doorevisuals.ui;

import com.github.noamm9.nvgrenderer.nvg.Image;
import dev.doorevisuals.App;
import dev.doorevisuals.audio.GuiSfx;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.data.Account;
import dev.doorevisuals.data.BannerLibrary;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.data.Favorites;
import dev.doorevisuals.data.Folders;
import dev.doorevisuals.data.GuiLayout;
import dev.doorevisuals.data.JsonImport;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Keys;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Skin;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.friends.FriendsFeature;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.tools.ConfigFeature;
import dev.doorevisuals.tools.GpsFeature;
import dev.doorevisuals.tools.GuiWindowsFeature;
import dev.doorevisuals.tools.ThemeFeature;
import dev.doorevisuals.world.AtmosphereFeature;
import dev.doorevisuals.world.TargetEspFeature;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class PanelScreen extends Screen {
    private static final float NAV_W = 200.0F;
    private static final float BODY_W = 470.0F;
    private static final float PANEL_H = 470.0F;
    private static final float BANNER_H = 118.0F;
    private static final float LOGO_S = 52.0F;
    private static final float RADIUS = 12.0F;
    private static final float HEADER_H = 36.0F;
    private static final float ROW_H = 28.0F;
    private static final float ROW_GAP = 5.0F;
    private static final float OPT = 22.0F;
    private static final float CAT_ROW = 28.0F;
    private static final float LIST_W = 196.0F;
    private final Anim appearNav = new Anim(0.0F);
    private final Anim appearBody = new Anim(0.0F);
    private final Anim closeAnim = new Anim(1.0F);
    private final Anim posNavX = new Anim(0.0F);
    private final Anim posNavY = new Anim(0.0F);
    private final Anim posBodyX = new Anim(0.0F);
    private final Anim posBodyY = new Anim(0.0F);
    private final Anim cosmeticsReveal = new Anim(0.0F);
    private final Anim previewReveal = new Anim(0.0F);
    private Feature previewHold;
    private final Set<String> open = new HashSet<>();
    private final Map<String, Anim> expand = new HashMap<>();
    private final Map<String, Anim> rowHover = new HashMap<>();
    private final Map<String, Anim> toggleAnim = new HashMap<>();
    private final Map<String, Anim> optToggleAnim = new HashMap<>();
    private final Map<String, Anim> catHover = new HashMap<>();
    private final List<PanelScreen.Hit> hits = new ArrayList<>();
    private Opt.Key capture;
    private Feature captureOwner;
    private PanelScreen.DragNum dragNum;
    private PanelScreen.DropMenu drop;
    private PanelScreen.ColorPick colorPick;
    private boolean bannerPick;
    private float bannerPickScroll;
    private boolean themeNameFocus;
    private boolean configNameFocus;
    private boolean searchFocus;
    private boolean cosmeticsOpen;
    private boolean settingsOpen;
    private boolean bindsOpen;
    private boolean bodyOpen;
    private boolean hadCategory;
    private boolean bannerDown;
    private boolean bannerDragged;
    private float bannerStartX;
    private float bannerStartY;
    private boolean navFront = true;
    private boolean draggingNav;
    private boolean draggingBody;
    private float dragOffX;
    private float dragOffY;
    private boolean closing;
    private boolean finished;
    private boolean closeSfx;
    private int mx;
    private int my;
    private float uiScale = 1.0F;
    private float frameDt = 0.016666668F;
    private long openedAt = System.currentTimeMillis();
    private long lastFrameNanos = System.nanoTime();
    private String moduleSearch = "";
    private float[] navRect = new float[4];
    private float[] bodyRect = new float[4];
    private int navLogoX;
    private int navLogoY;
    private int navLogoS;
    private int navFaceX;
    private int navFaceY;
    private int navFaceS;
    private boolean navChromeReady;
    private String tipId = "";
    private long tipSince;
    private float scrollVel;
    private PanelScreen.NumEdit numEdit;
    private String optTip = "";
    private long optTipSince;
    private Feature selectedFeature;
    private float inspectScroll;
    private float inspectScrollVel;
    private float[] inspectRect = new float[4];
    private Category dockHold;
    private long dockHoldAt;
    private boolean dockDragging;
    private float dockHoldX;
    private float dockHoldY;
    private final float[] dockRowY = new float[3];
    private int dockRowCount;

    public PanelScreen() {
        super(Text.literal("DooreVisuals"));
        this.posNavX.snap(GuiLayout.navX);
        this.posNavY.snap(GuiLayout.navY + 36.0F);
        this.posBodyX.snap(GuiLayout.bodyX);
        this.posBodyY.snap(GuiLayout.bodyY + 36.0F);
    }

    protected void init() {
        super.init();
        BannerLibrary.read(GuiLayout.bannerId());
        BannerLibrary.prepare();
        BannerLibrary.ensureLoaded();
        ClientPaths.ensureAll();
        if (GuiWindowsFeature.soundOn()) {
            GuiSfx.open();
        }
    }

    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        if (App.live()) {
            this.mx = mouseX;
            this.my = mouseY;
            this.hits.clear();
            this.uiScale = GuiLayout.scale();
            Theme.tickTransition(this.frameDt);
            long i = System.nanoTime();
            this.frameDt = Math.max(0.004166667F, Math.min(0.033333335F, (float)(i - this.lastFrameNanos) / 1.0E9F));
            this.lastFrameNanos = i;
            if (this.bannerDown && leftDown() && !this.draggingNav) {
                float f = this.mx - this.bannerStartX;
                float f1 = this.my - this.bannerStartY;
                if (f * f + f1 * f1 > 36.0F) {
                    this.bannerDragged = true;
                    this.draggingNav = true;
                    this.navFront = true;
                }
            }

            if (this.draggingNav && leftDown()) {
                GuiLayout.navX = this.clampX(this.mx - this.dragOffX, 200.0F * this.uiScale);
                GuiLayout.navY = this.clampY(this.my - this.dragOffY, 470.0F * this.uiScale);
            } else if (this.draggingNav) {
                this.finishPanelDrag(true);
            }

            if (this.draggingBody && leftDown()) {
                GuiLayout.bodyX = this.clampX(this.mx - this.dragOffX, 470.0F * this.uiScale);
                GuiLayout.bodyY = this.clampY(this.my - this.dragOffY, 470.0F * this.uiScale);
            } else if (this.draggingBody) {
                this.finishPanelDrag(false);
            }

            if (this.dragNum != null && leftDown()) {
                this.applyDrag(mouseX);
            }

            this.tickDockDrag();
            float f2 = this.closing ? this.closeAnim.springOpen(0.0F, this.frameDt) : this.closeAnim.springOpen(1.0F, this.frameDt);
            if (this.closing && f2 < 0.04F) {
                this.finishClose();
            } else {
                long j = System.currentTimeMillis() - this.openedAt;
                this.appearNav.springOpen(!this.closing && j > 20L ? 1.0F : 0.0F, this.frameDt);
                boolean flag = !this.closing && (this.bodyOpen || this.settingsOpen || this.bindsOpen);
                this.appearBody.springPop(flag && j > 40L ? 1.0F : 0.0F, this.frameDt);
                this.posNavX.springOpen(GuiLayout.navX, this.frameDt);
                this.posNavY.springOpen(GuiLayout.navY, this.frameDt);
                this.posBodyX.springOpen(GuiLayout.bodyX, this.frameDt);
                this.posBodyY.springOpen(GuiLayout.bodyY, this.frameDt);
                Nvg.run(g, () -> {
                    Nvg.alpha(f2);
                    int k = GuiWindowsFeature.dimAlpha();
                    if (k > 0) {
                        Paint.box(g, 0, 0, this.width, this.height, Theme.alpha(197639, (int)(k * f2)), 0.0F);
                    }

                    if (this.navFront) {
                        if (this.appearBody.value() > 0.02F) {
                            this.drawBody(g);
                        }

                        this.drawNav(g);
                    } else {
                        this.drawNav(g);
                        if (this.appearBody.value() > 0.02F) {
                            this.drawBody(g);
                        }
                    }

                    this.drawBridge(g);
                    if (this.drop != null) {
                        this.drawDrop(g);
                    }

                    if (this.colorPick != null) {
                        this.drawColor(g);
                    }

                    if (this.bannerPick) {
                        this.drawBannerPick(g);
                    }

                    Nvg.alpha(1.0F);
                });
                if (this.navChromeReady) {
                    if (this.navLogoS > 4) {
                        try {
                            g.drawTexture(
                                RenderPipelines.GUI_TEXTURED,
                                Sprites.LOGO,
                                this.navLogoX,
                                this.navLogoY,
                                0.0F,
                                0.0F,
                                this.navLogoS,
                                this.navLogoS,
                                this.navLogoS,
                                this.navLogoS
                            );
                        } catch (Throwable throwable1) {
                        }
                    }

                    if (GuiLayout.showProfileAvatar && this.navFaceS > 4 && !Account.hasAvatar() && this.client != null) {
                        ClientPlayerEntity clientplayerentity = this.client.player;
                        if (clientplayerentity instanceof AbstractClientPlayerEntity) {
                            AbstractClientPlayerEntity abstractclientplayerentity = clientplayerentity;

                            try {
                                PlayerSkinDrawer.draw(g, abstractclientplayerentity.getSkin(), this.navFaceX, this.navFaceY, this.navFaceS);
                            } catch (Throwable throwable) {
                            }
                        }
                    }
                }

                super.render(g, mouseX, mouseY, delta);
            }
        }
    }

    private void drawNav(DrawContext g) {
        float f = this.uiScale;
        float f1 = 200.0F * f;
        float f2 = 470.0F * f;
        float f3 = this.appearNav.value();
        float f4 = this.posNavX.value() - (1.0F - f3) * 22.0F * f;
        float f5 = this.posNavY.value() + (1.0F - Anim.easeOut(f3)) * 18.0F * f;
        this.navRect = new float[]{f4, f5, f1, f2};
        this.navChromeReady = false;
        Nvg.push();
        Nvg.alpha(Math.max(0.0F, f3 * f3));
        Paint.chromeGui(g, f4, f5, f1, f2, 12.0F * f);
        float f6 = 52.0F * f;
        this.navLogoS = Math.round(f6);
        this.navLogoX = Math.round(f4 + (f1 - f6) * 0.5F);
        this.navLogoY = Math.round(f5 + 10.0F * f);
        Paint.box(g, this.navLogoX - 2.0F * f, this.navLogoY - 2.0F * f, f6 + 4.0F * f, f6 + 4.0F * f, Theme.alpha(526604, 180), 14.0F * f);
        float f7 = f5 + 10.0F * f + f6 + 12.0F * f;

        for (Category category : GuiLayout.MAIN_CATS) {
            f7 = this.drawNavCat(g, category, f4, f7, f1, f, false);
        }

        f7 += 6.0F * f;
        Paint.box(g, f4 + 18.0F * f, f7, f1 - 36.0F * f, 1.0F * f, Theme.alpha(16777215, 16), 0.0F);
        f7 += 8.0F * f;
        List<Category> list = GuiLayout.dockVisible();
        this.dockRowCount = Math.min(list.size(), this.dockRowY.length);

        for (int j = 0; j < this.dockRowCount; j++) {
            Category category1 = list.get(j);
            float f11 = f7;
            if (this.dockDragging && this.dockHold == category1) {
                f11 = Math.max(f7 - 28.0F * f, Math.min(f7 + 28.0F * f, this.my - 28.0F * f * 0.5F));
            }

            this.dockRowY[j] = f7;
            this.drawNavCat(g, category1, f4, f11, f1, f, true);
            f7 += 28.0F * f;
        }

        float f9 = f4 + 10.0F * f;
        float f10 = f1 - 20.0F * f;
        boolean flag2 = Paint.hit((double)this.mx, (double)this.my, f9, f7, f10, 28.0F * f);
        if (this.bindsOpen) {
            Paint.box(g, f9, f7 + 7.0F * f, 2.4F * f, 28.0F * f - 14.0F * f, Theme.ACCENT_HOT, 1.2F * f);
        } else if (flag2) {
            Paint.box(g, f9, f7, f10, 28.0F * f, Theme.alpha(16777215, 14), 10.0F * f);
        }

        Paint.box(g, f4 + 24.0F * f, f7 + 8.0F * f, 10.0F * f, 3.0F * f, this.bindsOpen ? Theme.ACCENT_HOT : Theme.MUTED, 1.2F * f);
        Paint.box(g, f4 + 24.0F * f, f7 + 13.0F * f, 10.0F * f, 3.0F * f, this.bindsOpen ? Theme.ACCENT_HOT : Theme.MUTED, 1.2F * f);
        Paint.box(g, f4 + 24.0F * f, f7 + 18.0F * f, 7.0F * f, 3.0F * f, this.bindsOpen ? Theme.ACCENT_HOT : Theme.MUTED, 1.2F * f);
        Paint.text(g, "Binds", f4 + 46.0F * f, f7 + 10.0F * f, this.bindsOpen ? Theme.TEXT : Theme.MUTED, 7.8F * f);
        this.hits.add(new PanelScreen.Hit(f9, f7, f10, 28.0F * f, this::openBinds));
        f7 += 28.0F * f;
        f7 += 8.0F * f;
        float f8 = f4 + 16.0F * f;
        int i = 0;

        for (Skin skin : Skin.values()) {
            if (skin != Skin.CUSTOM && i < 4) {
                boolean flag = skin.label().equals(App.features().find(ThemeFeature.class).map(ThemeFeature::presetLabel).orElse(""));
                boolean flag1 = Paint.hit((double)this.mx, (double)this.my, f8, f7, 18.0F * f, 18.0F * f);
                Nvg.circle(f8 + 8.0F * f, f7 + 8.0F * f, !flag && !flag1 ? 6.6F * f : 8.4F * f, flag ? Theme.TEXT : Theme.alpha(skin.accent(), 90));
                Nvg.circle(f8 + 8.0F * f, f7 + 8.0F * f, 6.2F * f, skin.accent());
                String s = skin.label();
                this.hits
                    .add(new PanelScreen.Hit(f8, f7, 18.0F * f, 18.0F * f, () -> App.features().find(ThemeFeature.class).ifPresent(tf -> tf.applyPreset(s))));
                f8 += 22.0F * f;
                i++;
            }
        }

        if (i > 0) {
            f7 += 22.0F * f;
        }

        float f12 = 118.0F * f;
        float f13 = f5 + f2 - f12 - 10.0F * f;
        float f14 = f7 + 6.0F * f;
        if (f14 < f13 - 4.0F * f) {
            this.hits.add(new PanelScreen.Hit(f4, f14, f1, f13 - f14, this::startDragNav, true));
        }

        this.drawFooter(g, f4 + 8.0F * f, f13, f1 - 16.0F * f, f12, f);
        this.navChromeReady = true;
        Nvg.pop();
    }

    private void drawFooter(DrawContext g, float bx, float by, float bw, float banner, float s) {
        float f = 12.0F * s;
        Image image = BannerLibrary.currentFrame();
        if (image != null) {
            Nvg.imageCover(image, bx, by, bw, banner, f);
        } else {
            Paint.liveBanner(g, bx, by, bw, banner, f);
        }

        Paint.grad(g, bx, by + banner - 52.0F * s, bw, 52.0F * s, Theme.alpha(329483, 0), Theme.alpha(329483, 210), 0.0F, true);
        boolean flag = Paint.hit((double)this.mx, (double)this.my, bx, by, bw, banner);
        Paint.outline(g, bx, by, bw, banner, Theme.alpha(16777215, !this.settingsOpen && !flag ? 18 : 55), f);
        float f1 = 32.0F * s;
        float f2 = by + banner - f1 - 10.0F * s;
        this.navFaceS = GuiLayout.showProfileAvatar ? Math.round(f1) : 0;
        this.navFaceX = Math.round(bx + 10.0F * s);
        this.navFaceY = Math.round(f2);
        if (GuiLayout.showProfileAvatar) {
            Paint.box(g, bx + 10.0F * s, f2, f1, f1, Theme.alpha(526604, 240), 8.0F * s);
            Image image1 = Account.avatarImage();
            if (image1 != null) {
                Nvg.imageCover(image1, bx + 10.0F * s, f2, f1, f1, 8.0F * s);
                this.navFaceS = 0;
            }
        }

        String s1 = Account.nick();
        float f3 = bx + 10.0F * s + (GuiLayout.showProfileAvatar ? f1 + 8.0F * s : 0.0F);
        if (!GuiLayout.bannerLabels && !GuiLayout.showProfileNick) {
            Paint.text(g, "Settings", bx + 10.0F * s, by + banner - 16.0F * s, Theme.alpha(16777215, !this.settingsOpen && !flag ? 160 : 230), 6.0F * s);
        } else {
            if (GuiLayout.showProfileNick) {
                Paint.text(g, s1, f3, f2 + 2.0F * s, Theme.TEXT, 8.0F * s);
                String sx = Account.roleLabel();
                float f4 = Paint.tw(s1, 8.0F * s);
                float f5 = Paint.tw(sx, 5.2F * s) + 10.0F * s;
                float f6 = Math.min(f3 + f4 + 6.0F * s, bx + bw - 10.0F * s - f5);
                int i = "developer".equals(sx) ? Theme.ACCENT : Theme.alpha(16777215, 22);
                Paint.box(g, f6, f2 + 2.0F * s, f5, 12.0F * s, Theme.alpha(i, 200), 4.0F * s);
                Paint.textC(g, sx, f6 + f5 * 0.5F, f2 + 4.2F * s, "developer".equals(sx) ? Theme.VOID : Theme.TEXT, 5.2F * s);
            }

            Paint.text(g, "#" + Account.uid(), f3, f2 + 18.0F * s, Theme.GHOST, 5.8F * s);
        }

        this.hits.add(new PanelScreen.Hit(bx, by, bw, banner, this::toggleSettings));
    }

    private float drawNavCat(DrawContext g, Category cat, float x, float y, float w, float s, boolean dock) {
        boolean flag = !this.settingsOpen && !this.bindsOpen && this.bodyOpen && GuiLayout.selected() == cat;
        boolean flag1 = Paint.hit((double)this.mx, (double)this.my, x + 10.0F * s, y, w - 20.0F * s, 28.0F * s);
        float f = this.catHover
            .computeIfAbsent(cat.name(), k -> new Anim(0.0F))
            .springHover(!flag1 && !flag && (!this.dockDragging || this.dockHold != cat) ? 0.0F : 1.0F, this.frameDt);
        float f1 = x + 10.0F * s;
        float f2 = w - 20.0F * s;
        if (flag) {
            Paint.box(g, f1, y + 7.0F * s, 2.4F * s, 28.0F * s - 14.0F * s, Theme.ACCENT_HOT, 1.2F * s);
        } else if (f > 0.02F) {
            Paint.box(g, f1, y, f2, 28.0F * s, Theme.alpha(16777215, (int)(14.0F * f)), 10.0F * s);
        }

        this.drawCatIcon(g, cat, x + 24.0F * s, y + 8.0F * s, s, flag ? Theme.ACCENT_HOT : Theme.MUTED);
        Paint.text(g, cat.label(), x + 46.0F * s, y + 10.0F * s, flag ? Theme.TEXT : Theme.MUTED, 7.8F * s);
        if (dock) {
            this.hits.add(new PanelScreen.Hit(f1, y, f2, 28.0F * s, () -> this.pressDock(cat), true));
        } else {
            this.hits.add(new PanelScreen.Hit(f1, y, f2, 28.0F * s, () -> this.openCategory(cat)));
        }

        return y + 28.0F * s;
    }

    private void drawCatIcon(DrawContext g, Category cat, float x, float y, float s, int color) {
        switch (cat) {
            case OVERLAY:
                Paint.box(g, x, y + 1.5F * s, 12.0F * s, 8.0F * s, color, 2.2F * s);
                Paint.box(g, x + 4.0F * s, y + 10.0F * s, 4.0F * s, 2.2F * s, color, 1.0F * s);
                break;
            case WORLD:
                Nvg.circle(x + 6.0F * s, y + 6.2F * s, 5.4F * s, color);
                Nvg.ring(x + 2.2F * s, y + 3.4F * s, 7.6F * s, 5.6F * s, 1.15F * s, Theme.alpha(329483, 180), 3.0F * s);
                break;
            case TOOLS:
                Paint.box(g, x + 1.5F * s, y + 5.0F * s, 9.0F * s, 3.2F * s, color, 1.4F * s);
                Nvg.circle(x + 9.2F * s, y + 6.6F * s, 3.4F * s, color);
                Paint.box(g, x + 8.2F * s, y + 1.6F * s, 2.2F * s, 4.0F * s, Theme.alpha(329483, 160), 0.8F * s);
                break;
            case SYSTEM:
                Nvg.circle(x + 6.0F * s, y + 6.2F * s, 2.4F * s, color);
                Nvg.ring(x, y + 0.4F * s, 12.0F * s, 12.0F * s, 1.5F * s, color, 4.0F * s);
                break;
            case COSMETICS:
                Nvg.circle(x + 6.0F * s, y + 6.0F * s, 2.1F * s, color);
                Paint.box(g, x + 5.2F * s, y + 1.2F * s, 1.6F * s, 3.2F * s, color, 0.7F * s);
                Paint.box(g, x + 1.4F * s, y + 5.4F * s, 3.2F * s, 1.6F * s, color, 0.7F * s);
                Paint.box(g, x + 7.4F * s, y + 5.4F * s, 3.2F * s, 1.6F * s, color, 0.7F * s);
                Paint.box(g, x + 5.2F * s, y + 8.2F * s, 1.6F * s, 3.2F * s, color, 0.7F * s);
                break;
            case FRIENDS:
                Nvg.circle(x + 4.2F * s, y + 4.2F * s, 3.1F * s, color);
                Paint.box(g, x + 1.4F * s, y + 8.2F * s, 5.6F * s, 3.4F * s, color, 1.6F * s);
                Nvg.circle(x + 8.8F * s, y + 5.2F * s, 2.5F * s, Theme.alpha(color, 170));
                break;
            case THEME:
                Nvg.circle(x + 4.2F * s, y + 5.2F * s, 3.5F * s, color);
                Nvg.circle(x + 8.2F * s, y + 8.2F * s, 3.2F * s, Theme.alpha(color, 160));
                break;
            case CONFIG:
                Paint.box(g, x + 1.4F * s, y + 1.6F * s, 9.4F * s, 10.2F * s, color, 2.0F * s);
                Paint.box(g, x + 3.4F * s, y + 4.2F * s, 5.4F * s, 1.3F * s, Theme.alpha(329483, 140), 0.6F * s);
                Paint.box(g, x + 3.4F * s, y + 7.0F * s, 4.2F * s, 1.3F * s, Theme.alpha(329483, 140), 0.6F * s);
        }
    }

    private void drawBody(DrawContext g) {
        if (this.appearBody.value() < 0.02F) {
            this.bodyRect = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
        } else {
            float f = this.uiScale;
            float f1 = 470.0F * f;
            float f2 = 470.0F * f;
            float f3 = this.appearBody.value();
            float f4 = Anim.easeBack(Math.min(1.0F, f3));
            float f5 = this.posBodyX.value() - (1.0F - f4) * 48.0F * f;
            float f6 = this.posBodyY.value() + (1.0F - f3) * 10.0F * f;
            this.bodyRect = new float[]{f5, f6, f1, f2};
            Category category = GuiLayout.selected();
            Nvg.push();
            Nvg.alpha(Math.max(0.0F, f3 * f3));
            Paint.chromeGui(g, f5, f6, f1, f2, 12.0F * f);
            float f7 = 36.0F * f;
            Paint.text(g, this.bodyTitle(category), f5 + 16.0F * f, f6 + 13.0F * f, Theme.TEXT, 10.5F * f);
            float f8 = f5 + f1 - 28.0F * f;
            boolean flag = Paint.hit((double)this.mx, (double)this.my, f8, f6 + 10.0F * f, 16.0F * f, 16.0F * f);
            Paint.textC(g, "\u00d7", f8 + 8.0F * f, f6 + 12.0F * f, flag ? Theme.TEXT : Theme.GHOST, 9.0F * f);
            this.hits.add(new PanelScreen.Hit(f8, f6 + 8.0F * f, 18.0F * f, 20.0F * f, this::closeBody));
            if (!this.settingsOpen) {
                float f9 = 118.0F * f;
                float f10 = f5 + f1 - 36.0F * f - f9;
                Paint.box(g, f10, f6 + 10.0F * f, f9, 20.0F * f, Theme.alpha(16777215, this.searchFocus ? 18 : 10), 8.0F * f);
                if (this.searchFocus) {
                    Paint.outline(g, f10, f6 + 10.0F * f, f9, 20.0F * f, Theme.alpha(Theme.ACCENT, 110), 8.0F * f);
                }

                Nvg.circle(f10 + 10.0F * f, f6 + 20.0F * f, 3.4F * f, this.searchFocus ? Theme.ACCENT : Theme.GHOST);
                Nvg.ring(f10 + 7.2F * f, f6 + 17.2F * f, 5.6F * f, 5.6F * f, 1.15F * f, this.searchFocus ? Theme.ACCENT : Theme.GHOST, 2.0F * f);
                Paint.box(g, f10 + 12.6F * f, f6 + 22.4F * f, 3.2F * f, 1.2F * f, this.searchFocus ? Theme.ACCENT : Theme.GHOST, 0.6F * f);
                String s = this.searchText(category);
                String s1 = s.isEmpty() && !this.searchFocus ? "Search" : s + (this.searchFocus && blink() ? "|" : "");
                Paint.text(g, s1, f10 + 20.0F * f, f6 + 15.0F * f, !this.searchFocus && s.isEmpty() ? Theme.GHOST : Theme.TEXT, 6.0F * f);
                this.hits.add(new PanelScreen.Hit(f10, f6 + 10.0F * f, f9, 20.0F * f, () -> {
                    this.searchFocus = true;
                    this.themeNameFocus = false;
                    this.configNameFocus = false;
                }));
                this.hits.add(new PanelScreen.Hit(f5, f6, f1 - f9 - 40.0F * f, f7, this::startDragBody, true));
            } else {
                this.hits.add(new PanelScreen.Hit(f5, f6, f1 - 36.0F * f, f7, this::startDragBody, true));
            }

            float f11 = f6 + f7;
            float f12 = f2 - f7 - 8.0F * f;
            if (this.settingsOpen) {
                ThemeConfigUi.drawTheme(
                    g,
                    f5,
                    f11,
                    f1,
                    f12,
                    f,
                    this.mx,
                    this.my,
                    this.frameDt,
                    (hx, hy, hw, hh, action) -> this.hits.add(new PanelScreen.Hit(hx, hy, hw, hh, action)),
                    () -> {
                        this.themeNameFocus = true;
                        this.searchFocus = false;
                        this.configNameFocus = false;
                    },
                    this::openThemeColor,
                    this.themeNameFocus,
                    blink() ? "|" : ""
                );
            } else if (this.bindsOpen) {
                this.drawBinds(g, f5, f11, f1, f12);
            } else if (category == Category.CONFIG) {
                ThemeConfigUi.drawConfig(
                    g,
                    f5,
                    f11,
                    f1,
                    f12,
                    f,
                    this.mx,
                    this.my,
                    this.frameDt,
                    (hx, hy, hw, hh, action) -> this.hits.add(new PanelScreen.Hit(hx, hy, hw, hh, action)),
                    () -> {
                        this.configNameFocus = true;
                        this.searchFocus = false;
                        this.themeNameFocus = false;
                    },
                    () -> {
                        this.searchFocus = true;
                        this.configNameFocus = false;
                        this.themeNameFocus = false;
                    },
                    this.configNameFocus,
                    this.searchFocus,
                    blink() ? "|" : ""
                );
            } else {
                this.drawModules(g, category, f5, f11, f1, f12);
            }

            Nvg.pop();
        }
    }

    private String bodyTitle(Category sel) {
        if (this.settingsOpen) {
            return "Settings";
        } else if (this.bindsOpen) {
            return "Binds";
        } else {
            return switch (sel) {
                case CONFIG -> "Configs";
                default -> sel.label();
            };
        }
    }

    private String searchText(Category sel) {
        return sel == Category.CONFIG ? App.features().find(ConfigFeature.class).map(ConfigFeature::filter).orElse("") : this.moduleSearch;
    }

    private void drawModules(DrawContext g, Category category, float x, float y, float w, float h) {
        float f = this.uiScale;
        float f1 = 28.0F * f;
        float f2 = 5.0F * f;
        List<Feature> list = this.modulesOf(category);
        this.ensureSelected(list);
        float f3 = Math.min(196.0F * f, w * 0.46F);
        float f4 = x + f3 + 6.0F * f;
        float f5 = w - f3 - 10.0F * f;
        this.inspectRect = new float[]{f4, y, f5, h};
        float f6 = GuiLayout.get(category).scroll;
        if (Math.abs(this.scrollVel) > 0.15F) {
            f6 += this.scrollVel;
            this.scrollVel *= 0.86F;
            GuiLayout.get(category).scroll = f6;
        } else {
            this.scrollVel = 0.0F;
        }

        if (Math.abs(this.inspectScrollVel) > 0.15F) {
            this.inspectScroll = this.inspectScroll + this.inspectScrollVel;
            this.inspectScrollVel *= 0.86F;
        } else {
            this.inspectScrollVel = 0.0F;
        }

        Nvg.scissor(x, y, f3, h);
        float f7 = y + 8.0F * f + f6;
        float f8 = 12.0F * f;
        Feature feature = null;
        if (list.isEmpty()) {
            Paint.textC(g, "\u043f\u0443\u0441\u0442\u043e", x + f3 * 0.5F, y + 18.0F * f, Theme.GHOST, 7.0F * f);
        }

        for (Feature feature1 : list) {
            boolean flag = f7 + f1 >= y - 6.0F && f7 <= y + h + 6.0F;
            if (flag) {
                this.drawModuleRow(g, feature1, x + 8.0F * f, f7, f3 - 16.0F * f, f1);
                if (Paint.hit((double)this.mx, (double)this.my, x + 8.0F * f, f7, f3 - 16.0F * f, f1)) {
                    feature = feature1;
                }
            }

            f7 += f1 + f2;
            f8 += f1 + f2;
        }

        Nvg.unscissor();
        float f9 = Math.min(0.0F, h - f8);
        GuiLayout.Panel guilayout$panel = GuiLayout.get(category);
        guilayout$panel.scroll = Math.max(f9, Math.min(0.0F, guilayout$panel.scroll));
        Paint.scrollbar(g, x + f3 - 6.0F * f, y + 6.0F * f, h - 12.0F * f, f8, h, guilayout$panel.scroll);
        Paint.box(g, f4 - 4.0F * f, y + 8.0F * f, 1.0F * f, h - 16.0F * f, Theme.alpha(16777215, 14), 0.0F);
        this.drawInspector(g, this.selectedFeature, f4, y, f5, h);
        this.drawModuleTip(g, feature);
    }

    private void drawBinds(DrawContext g, float x, float y, float w, float h) {
        float f = this.uiScale;
        List<Feature> list = this.visualBinds();
        if (Math.abs(this.inspectScrollVel) > 0.15F) {
            this.inspectScroll = this.inspectScroll + this.inspectScrollVel;
            this.inspectScrollVel *= 0.86F;
        } else {
            this.inspectScrollVel = 0.0F;
        }

        float f1 = y + 8.0F * f;
        float f2 = h - 14.0F * f;
        Nvg.scissor(x + 8.0F * f, f1, w - 16.0F * f, f2);
        float f3 = f1 + this.inspectScroll;
        float f4 = 26.0F * f;
        float f5 = 10.0F * f;
        if (list.isEmpty()) {
            Paint.textC(
                g,
                "\u043d\u0435\u0442 \u0431\u0438\u043d\u0434\u043e\u0432 \u0432\u0438\u0437\u0443\u0430\u043b\u043e\u0432",
                x + w * 0.5F,
                y + 22.0F * f,
                Theme.GHOST,
                7.0F * f
            );
        }

        for (Feature feature : list) {
            boolean flag = Paint.hit((double)this.mx, (double)this.my, x + 12.0F * f, f3, w - 24.0F * f, f4);
            Paint.box(g, x + 12.0F * f, f3, w - 24.0F * f, f4, Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 28 : 8), 12.0F * f);
            Paint.box(
                g,
                x + 14.0F * f,
                f3 + 6.0F * f,
                2.2F * f,
                f4 - 12.0F * f,
                Theme.alpha(feature.on() ? Theme.ACCENT_HOT : 16777215, feature.on() ? 220 : 40),
                1.2F * f
            );
            Paint.text(g, feature.name(), x + 22.0F * f, f3 + 8.0F * f, feature.on() ? Theme.TEXT : Theme.MUTED, 7.2F * f);
            Opt.Key opt$key = feature.toggleBind();
            if (opt$key != null) {
                String s = this.capture == opt$key ? "\u2026" : Keys.name((Integer)opt$key.get());
                float f6 = Math.max(36.0F * f, Paint.tw(s, 6.0F * f) + 12.0F * f);
                float f7 = x + w - 16.0F * f - f6;
                boolean flag1 = (Integer)opt$key.get() > 0 && (Integer)opt$key.get() != -1;
                Paint.box(
                    g, f7, f3 + 5.0F * f, f6, 16.0F * f, Theme.alpha(!flag1 && this.capture != opt$key ? 16777215 : Theme.ACCENT, flag1 ? 36 : 12), 6.0F * f
                );
                Paint.textC(g, s, f7 + f6 * 0.5F, f3 + 8.0F * f, !flag1 && this.capture != opt$key ? Theme.MUTED : Theme.ACCENT_HOT, 6.0F * f);
                this.hits.add(new PanelScreen.Hit(f7, f3, f6, f4, () -> {
                    this.capture = opt$key;
                    this.captureOwner = feature;
                }));
            }

            f3 += f4 + 4.0F * f;
            f5 += f4 + 4.0F * f;
        }

        Nvg.unscissor();
        float f8 = Math.min(0.0F, f2 - f5);
        this.inspectScroll = Math.max(f8, Math.min(0.0F, this.inspectScroll));
        Paint.scrollbar(g, x + w - 8.0F * f, f1, f2, f5, f2, this.inspectScroll);
    }

    private List<Feature> visualBinds() {
        String s = this.moduleSearch.toLowerCase(Locale.ROOT);
        List<Feature> list = new ArrayList<>();

        for (Feature feature : App.features().all()) {
            if (feature.listed() && feature.toggleBind() != null) {
                Category category = feature.category();
                if ((category == Category.OVERLAY || category == Category.WORLD || category == Category.COSMETICS)
                    && (s.isEmpty() || feature.name().toLowerCase(Locale.ROOT).contains(s) || feature.id().toLowerCase(Locale.ROOT).contains(s))) {
                    list.add(feature);
                }
            }
        }

        return list;
    }

    private void drawInspector(DrawContext g, Feature feature, float x, float y, float w, float h) {
        float f = this.uiScale;
        if (feature == null) {
            Paint.textC(g, "\u0432\u044b\u0431\u0435\u0440\u0438 \u043c\u043e\u0434\u0443\u043b\u044c", x + w * 0.5F, y + 22.0F * f, Theme.GHOST, 7.0F * f);
        } else {
            Paint.text(g, feature.name(), x + 8.0F * f, y + 8.0F * f, Theme.TEXT, 8.4F * f);
            String s = feature.about() == null ? "" : feature.about();
            if (!s.isBlank()) {
                Paint.text(g, fit(s, w - 16.0F * f, 5.4F * f), x + 8.0F * f, y + 22.0F * f, Theme.MUTED, 5.4F * f);
            }

            if (feature instanceof HudFeature hudfeature) {
                Paint.text(
                    g,
                    "\u0427\u0430\u0442: \u041b\u041a\u041c \u0434\u0432\u0438\u0433\u0430\u0442\u044c, \u041f\u041a\u041c \u043b\u043e\u043a, \u043a\u043e\u043b\u0435\u0441\u043e \u043c\u0430\u0441\u0448\u0442\u0430\u0431",
                    x + 8.0F * f,
                    y + 32.0F * f,
                    Theme.GHOST,
                    5.2F * f
                );
                Paint.box(g, x + w - 78.0F * f, y + 6.0F * f, 70.0F * f, 16.0F * f, Theme.alpha(Theme.ACCENT, 48), 5.0F * f);
                Paint.textC(g, "\u0441\u0431\u0440\u043e\u0441", x + w - 43.0F * f, y + 9.0F * f, Theme.TEXT, 5.4F * f);
                this.hits.add(new PanelScreen.Hit(x + w - 78.0F * f, y + 6.0F * f, 70.0F * f, 16.0F * f, hudfeature::resetLayout));
            }

            float f5 = y + 36.0F * f;
            float f1 = h - 44.0F * f;
            Nvg.scissor(x, f5, w, f1);
            float f2 = f5 + this.inspectScroll;
            float f3 = 8.0F * f;
            if (feature instanceof CosmeticsFeature) {
                float f4 = 168.0F * f;
                CosmeticsDrawer.drawCompact(
                    g, x + 8.0F * f, f2, w - 16.0F * f, f4, f, (hx, hy, hw, hh, action) -> this.hits.add(new PanelScreen.Hit(hx, hy, hw, hh, action, true))
                );
                f2 += f4 + 8.0F * f;
                f3 += f4 + 8.0F * f;
            } else if (feature instanceof HudFeature || feature instanceof TargetEspFeature || feature instanceof AtmosphereFeature) {
                float f6 = 88.0F * f;
                PreviewPane.drawSnippet(g, feature, x + 8.0F * f, f2, w - 16.0F * f, f6, f);
                f2 += f6 + 8.0F * f;
                f3 += f6 + 8.0F * f;
            }

            for (Opt<?> opt : feature.visibleOpts()) {
                this.drawOpt(g, feature, opt, x + 8.0F * f, f2, w - 16.0F * f);
                f2 += 22.0F * f;
                f3 += 22.0F * f;
            }

            if (feature instanceof GpsFeature gpsfeature) {
                Paint.text(g, "+ here", x + 12.0F * f, f2 + 4.0F * f, Theme.ACCENT_HOT, 5.8F * f);
                this.hits
                    .add(
                        new PanelScreen.Hit(
                            x + 8.0F * f,
                            f2,
                            w - 16.0F * f,
                            22.0F * f,
                            () -> {
                                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                                if (minecraftclient.player != null) {
                                    gpsfeature.addPoint(
                                        "here-" + (gpsfeature.markers().size() + 1),
                                        minecraftclient.player.getX(),
                                        minecraftclient.player.getY(),
                                        minecraftclient.player.getZ(),
                                        minecraftclient.world == null
                                            ? "minecraft:overworld"
                                            : minecraftclient.world.getRegistryKey().getValue().toString()
                                    );
                                }
                            }
                        )
                    );
                f2 += 22.0F * f;
                f3 += 22.0F * f;

                for (GpsFeature.Marker gpsfeature$marker : gpsfeature.markers()) {
                    boolean flag = gpsfeature$marker.name().equalsIgnoreCase(gpsfeature.activeMarkerName());
                    Paint.text(
                        g, (flag ? "\u25cf " : "\u25cb ") + gpsfeature$marker.name(), x + 12.0F * f, f2 + 4.0F * f, flag ? Theme.ACCENT : Theme.MUTED, 5.8F * f
                    );
                    String s1 = gpsfeature$marker.name();
                    this.hits.add(new PanelScreen.Hit(x + 8.0F * f, f2, w - 34.0F * f, 22.0F * f, () -> gpsfeature.setActive(s1)));
                    Paint.textR(g, "\u00d7", x + w - 12.0F * f, f2 + 4.0F * f, Theme.BAD, 6.5F * f);
                    this.hits.add(new PanelScreen.Hit(x + w - 26.0F * f, f2, 18.0F * f, 22.0F * f, () -> gpsfeature.removeNamed(s1)));
                    f2 += 22.0F * f;
                    f3 += 22.0F * f;
                }
            }

            if (feature instanceof FriendsFeature friendsfeature) {
                Paint.text(g, "+ \u043f\u043e\u0434 \u043f\u0440\u0438\u0446\u0435\u043b\u043e\u043c", x + 12.0F * f, f2 + 4.0F * f, Theme.ACCENT_HOT, 5.8F * f);
                this.hits.add(new PanelScreen.Hit(x + 8.0F * f, f2, w - 16.0F * f, 22.0F * f, friendsfeature::addLooked));
                f2 += 22.0F * f;
                f3 += 22.0F * f;

                for (Entry<UUID, String> entry : friendsfeature.friends().entrySet()) {
                    Paint.text(g, "\u2605 " + entry.getValue(), x + 12.0F * f, f2 + 4.0F * f, Theme.ACCENT_HOT, 5.8F * f);
                    UUID uuid = entry.getKey();
                    Paint.textR(g, "\u00d7", x + w - 12.0F * f, f2 + 4.0F * f, Theme.BAD, 6.5F * f);
                    this.hits.add(new PanelScreen.Hit(x + w - 26.0F * f, f2, 18.0F * f, 22.0F * f, () -> friendsfeature.remove(uuid)));
                    f2 += 22.0F * f;
                    f3 += 22.0F * f;
                }

                if (friendsfeature.friends().isEmpty()) {
                    Paint.text(
                        g,
                        "\u041d\u0438\u043a\u043e\u0433\u043e. \u0411\u0438\u043d\u0434 \u0438\u043b\u0438 \u043a\u043d\u043e\u043f\u043a\u0430 \u0432\u044b\u0448\u0435",
                        x + 10.0F * f,
                        f2 + 4.0F * f,
                        Theme.GHOST,
                        5.4F * f
                    );
                    f3 += 22.0F * f;
                }
            }

            Nvg.unscissor();
            float f7 = Math.min(0.0F, f1 - f3);
            this.inspectScroll = Math.max(f7, Math.min(0.0F, this.inspectScroll));
            Paint.scrollbar(g, x + w - 6.0F * f, f5, f1, f3, f1, this.inspectScroll);
        }
    }

    private void drawModuleRow(DrawContext g, Feature feature, float x, float y, float w, float h) {
        float f = this.uiScale;
        boolean flag = Favorites.isModuleFav(feature.id());
        boolean flag1 = this.selectedFeature == feature;
        Anim anim = this.toggleAnim.computeIfAbsent(feature.id(), ignored -> new Anim(feature.on() ? 1.0F : 0.0F));
        float f1 = anim.springToggle(feature.on() ? 1.0F : 0.0F, this.frameDt);
        boolean flag2 = Paint.hit((double)this.mx, (double)this.my, x, y, w, h);
        float f2 = this.rowHover.computeIfAbsent(feature.id(), ignored -> new Anim(0.0F)).springHover(!flag2 && !flag1 ? 0.0F : 1.0F, this.frameDt);
        int i = feature.on() ? Theme.alpha(Theme.ACCENT, (int)(22.0F + 14.0F * f2)) : Theme.alpha(16777215, (int)(8.0F + 12.0F * f2));
        Paint.box(g, x, y, w, h, i, 12.0F * f);
        if (flag1 || f2 > 0.04F) {
            Paint.outline(g, x, y, w, h, Theme.alpha(Theme.ACCENT, flag1 ? 110 : (int)(70.0F * f2)), 12.0F * f);
        }

        if (feature.on()) {
            Paint.box(g, x + 1.5F * f, y + 6.0F * f, 2.2F * f, h - 12.0F * f, Theme.alpha(Theme.ACCENT_HOT, (int)(230.0F * f1)), 1.2F * f);
        }

        Paint.text(g, flag ? "\u2605" : "\u2606", x + 7.0F * f, y + 6.0F * f, flag ? Theme.ACCENT : Theme.GHOST, 6.6F * f);
        this.hits.add(new PanelScreen.Hit(x, y, 20.0F * f, h, () -> Favorites.toggleModule(feature.id())));
        float f3 = x + 22.0F * f;
        float f4 = x + w - 40.0F * f;
        Opt.Key opt$key = feature.toggleBind();
        String s = "";
        float f5 = 0.0F;
        if (opt$key != null) {
            s = this.capture == opt$key ? "\u2026" : Keys.name((Integer)opt$key.get());
            f5 = Math.max(Paint.tw(s, 5.0F * f) + 8.0F * f, 20.0F * f);
            f4 -= f5 + 4.0F * f;
        }

        Paint.text(g, fit(feature.name(), f4 - f3 - 4.0F * f, 7.0F * f), f3, y + 7.0F * f, feature.on() ? Theme.TEXT : Theme.MUTED, 7.0F * f);
        Paint.toggle(g, x + w - 36.0F * f, y + (h - 16.0F * f) * 0.5F, feature.on(), f1);
        this.hits.add(new PanelScreen.Hit(x + 20.0F * f, y, w - 60.0F * f, h, () -> this.selectFeature(feature), true));
        this.hits.add(new PanelScreen.Hit(x + w - 38.0F * f, y, 38.0F * f, h, () -> {
            feature.flip();
            this.toggleAnim.computeIfAbsent(feature.id(), ignored -> new Anim(0.0F)).snap(feature.on() ? 1.0F : 0.0F);
            GuiSfx.toggle(feature.on());
            toastModule(feature);
        }, () -> this.selectFeature(feature), true));
        if (opt$key != null) {
            float f6 = f4 + 2.0F * f;
            boolean flag3 = (Integer)opt$key.get() > 0 && (Integer)opt$key.get() != -1;
            Paint.box(g, f6, y + 6.0F * f, f5, 14.0F * f, Theme.alpha(flag3 ? Theme.ACCENT : 16777215, flag3 ? 28 : 10), 4.0F * f);
            Paint.textC(g, s, f6 + f5 * 0.5F, y + 8.0F * f, flag3 ? Theme.ACCENT_HOT : Theme.MUTED, 5.0F * f);
            this.hits.add(new PanelScreen.Hit(f6, y, f5, h, () -> {
                this.capture = opt$key;
                this.captureOwner = feature;
            }));
        }
    }

    private void pressBanner() {
        this.bannerDown = true;
        this.bannerDragged = false;
        this.bannerStartX = this.mx;
        this.bannerStartY = this.my;
        this.dragOffX = this.mx - GuiLayout.navX;
        this.dragOffY = this.my - GuiLayout.navY;
        this.navFront = true;
        this.drop = null;
        this.colorPick = null;
    }

    private void toggleSettings() {
        this.bindsOpen = false;
        this.settingsOpen = !this.settingsOpen;
        if (this.settingsOpen) {
            this.bodyOpen = true;
            this.launchBody();
        } else {
            this.bodyOpen = this.hadCategory;
        }

        this.themeNameFocus = false;
        this.searchFocus = false;
        this.navFront = false;
    }

    private void openBinds() {
        if (this.bindsOpen && this.bodyOpen && !this.settingsOpen) {
            this.closeBody();
        } else {
            boolean flag = !this.bodyOpen || this.settingsOpen || !this.bindsOpen;
            this.settingsOpen = false;
            this.bindsOpen = true;
            this.bodyOpen = true;
            this.hadCategory = true;
            this.searchFocus = false;
            this.navFront = false;
            this.inspectScroll = 0.0F;
            this.inspectScrollVel = 0.0F;
            if (flag) {
                this.launchBody();
            }
        }
    }

    private void openCategory(Category cat) {
        if (!this.settingsOpen && !this.bindsOpen && this.bodyOpen && GuiLayout.selected() == cat) {
            this.closeBody();
        } else {
            boolean flag = !this.bodyOpen || this.settingsOpen || this.bindsOpen;
            this.settingsOpen = false;
            this.bindsOpen = false;
            this.bodyOpen = true;
            this.hadCategory = true;
            GuiLayout.select(cat);
            this.searchFocus = false;
            this.navFront = false;
            this.inspectScroll = 0.0F;
            this.selectedFeature = null;
            this.ensureSelected(this.modulesOf(cat));
            if (flag) {
                this.launchBody();
            }
        }
    }

    private void selectFeature(Feature feature) {
        if (feature != null) {
            if (feature.listed() && feature.category() != GuiLayout.selected()) {
                GuiLayout.select(feature.category());
            }

            if (this.selectedFeature != feature) {
                this.inspectScroll = 0.0F;
                this.inspectScrollVel = 0.0F;
            }

            this.selectedFeature = feature;
            this.open.clear();
            this.open.add(feature.id());
            this.drop = null;
        }
    }

    private void ensureSelected(List<Feature> features) {
        if (features.isEmpty()) {
            this.selectedFeature = null;
        } else {
            if (this.selectedFeature == null || !features.contains(this.selectedFeature)) {
                this.selectFeature(features.get(0));
            }
        }
    }

    private void pressDock(Category cat) {
        this.dockHold = cat;
        this.dockHoldAt = System.currentTimeMillis();
        this.dockDragging = false;
        this.dockHoldX = this.mx;
        this.dockHoldY = this.my;
        this.navFront = true;
        this.drop = null;
        this.colorPick = null;
    }

    private void tickDockDrag() {
        if (this.dockHold != null && leftDown()) {
            float f = this.mx - this.dockHoldX;
            float f1 = this.my - this.dockHoldY;
            if (!this.dockDragging && (f * f + f1 * f1 > 36.0F || System.currentTimeMillis() - this.dockHoldAt > 180L)) {
                this.dockDragging = true;
            }

            if (this.dockDragging) {
                List<Category> list = GuiLayout.dockVisible();
                int i = list.indexOf(this.dockHold);
                int j = i;

                for (int k = 0; k < this.dockRowCount && k < list.size(); k++) {
                    float f2 = this.dockRowY[k];
                    float f3 = f2 + 28.0F * this.uiScale;
                    if (this.my >= f2 && this.my < f3) {
                        j = k;
                        break;
                    }
                }

                if (i >= 0 && j >= 0 && i != j && j < list.size()) {
                    List<Category> list1 = GuiLayout.dockOrder();
                    int l = list1.indexOf(list.get(i));
                    int i1 = list1.indexOf(list.get(j));
                    GuiLayout.moveDock(l, i1);
                }
            }
        }
    }

    private void finishDock() {
        if (this.dockHold != null) {
            Category category = this.dockHold;
            float f = this.mx - this.dockHoldX;
            float f1 = this.my - this.dockHoldY;
            boolean flag = this.dockDragging && f * f + f1 * f1 > 36.0F;
            this.dockHold = null;
            this.dockDragging = false;
            if (!flag) {
                this.openCategory(category);
                GuiSfx.click();
            } else {
                this.persistLayout();
            }
        }
    }

    private void closeBody() {
        this.settingsOpen = false;
        this.bindsOpen = false;
        this.bodyOpen = false;
        this.hadCategory = false;
        this.searchFocus = false;
        this.themeNameFocus = false;
        this.configNameFocus = false;
        this.navFront = true;
    }

    private void launchBody() {
        this.appearBody.snap(Math.max(0.05F, Math.min(0.2F, this.appearBody.value())));
        this.posBodyX.snap(GuiLayout.navX + 200.0F * this.uiScale - 18.0F);
        this.posBodyY.snap(GuiLayout.bodyY);
    }

    private void startDragNav() {
        this.navFront = true;
        this.draggingNav = true;
        this.draggingBody = false;
        this.dragOffX = this.mx - GuiLayout.navX;
        this.dragOffY = this.my - GuiLayout.navY;
        this.drop = null;
        this.colorPick = null;
    }

    private void startDragBody() {
        this.navFront = false;
        this.draggingBody = true;
        this.draggingNav = false;
        this.dragOffX = this.mx - GuiLayout.bodyX;
        this.dragOffY = this.my - GuiLayout.bodyY;
        this.drop = null;
        this.colorPick = null;
    }

    private float clampX(float x, float w) {
        return Math.max(4.0F, Math.min(this.width - Math.max(48.0F, w) - 4.0F, x));
    }

    private float clampY(float y, float h) {
        return Math.max(4.0F, Math.min(this.height - 36.0F * this.uiScale - 4.0F, y));
    }

    private void finishPanelDrag(boolean nav) {
        if (nav) {
            if (!this.draggingNav) {
                return;
            }

            this.snapPanel(true);
            GuiLayout.navX = this.clampX(GuiLayout.navX, 200.0F * this.uiScale);
            GuiLayout.navY = this.clampY(GuiLayout.navY, 470.0F * this.uiScale);
            this.draggingNav = false;
        } else {
            if (!this.draggingBody) {
                return;
            }

            this.snapPanel(false);
            GuiLayout.bodyX = this.clampX(GuiLayout.bodyX, 470.0F * this.uiScale);
            GuiLayout.bodyY = this.clampY(GuiLayout.bodyY, 470.0F * this.uiScale);
            this.draggingBody = false;
        }

        this.persistLayout();
    }

    private void snapPanel(boolean nav) {
    }

    private static float snapEdge(float pos, float size, float max, float margin, float thresh) {
        if (Math.abs(pos - margin) < thresh) {
            return margin;
        } else if (Math.abs(pos + size - (max - margin)) < thresh) {
            return max - size - margin;
        } else {
            float f = (max - size) * 0.5F;
            return Math.abs(pos - f) < thresh ? f : pos;
        }
    }

    private static float[] snapTogether(float x, float y, float w, float h, float ox, float oy, float ow, float oh, float gap, float thresh) {
        if (Math.abs(x - (ox + ow + gap)) < thresh) {
            x = ox + ow + gap;
        }

        if (Math.abs(x + w + gap - ox) < thresh) {
            x = ox - w - gap;
        }

        if (Math.abs(y - oy) < thresh) {
            y = oy;
        }

        if (Math.abs(y + h - (oy + oh)) < thresh) {
            y = oy + oh - h;
        }

        if (Math.abs(y - (oy + oh + gap)) < thresh) {
            y = oy + oh + gap;
        }

        if (Math.abs(y + h + gap - oy) < thresh) {
            y = oy - h - gap;
        }

        return new float[]{x, y};
    }

    private List<Feature> modulesOf(Category category) {
        List<Feature> list = new ArrayList<>();
        String s = this.moduleSearch.toLowerCase(Locale.ROOT);

        for (Feature feature : s.isEmpty() ? App.features().of(category) : App.features().all()) {
            if (feature.listed()
                && (!s.isEmpty() || feature.category() == category)
                && (
                    s.isEmpty()
                        || feature.name().toLowerCase(Locale.ROOT).contains(s)
                        || feature.id().toLowerCase(Locale.ROOT).contains(s)
                        || feature.about() != null && feature.about().toLowerCase(Locale.ROOT).contains(s)
                )) {
                list.add(feature);
            }
        }

        list.sort((a, b) -> {
            boolean flag = Favorites.isModuleFav(a.id());
            boolean flag1 = Favorites.isModuleFav(b.id());
            if (flag != flag1) {
                return flag ? -1 : 1;
            } else {
                return a.name().compareToIgnoreCase(b.name());
            }
        });
        return list;
    }

    private float springExpand(Feature feature) {
        boolean flag = this.open.contains(feature.id());
        Anim anim = this.expand.computeIfAbsent(feature.id(), ignored -> new Anim(0.0F));
        if (!flag && anim.value() < 0.01F) {
            anim.snap(0.0F);
            return 0.0F;
        } else {
            return anim.springExpand(flag ? 1.0F : 0.0F, this.frameDt);
        }
    }

    private float optsBlockHeight(Feature feature) {
        float f = 6.0F;
        f += feature.visibleOpts().size() * 22.0F;
        if (feature instanceof GpsFeature gpsfeature) {
            f += (1 + gpsfeature.markers().size()) * 22.0F;
        }

        if (feature instanceof FriendsFeature friendsfeature) {
            f += (1 + friendsfeature.friends().size()) * 22.0F;
        }

        return f;
    }

    private void openFeatureSettings(Feature feature) {
        if (!feature.visibleOpts().isEmpty() || PreviewPane.supports(feature) || feature instanceof FriendsFeature) {
            String s = feature.id();
            if (this.open.contains(s)) {
                this.open.remove(s);
                this.expand.computeIfAbsent(s, ignored -> new Anim(1.0F)).nudge(0.0F, 0.4F);
            } else {
                for (String s1 : this.open) {
                    this.expand.computeIfAbsent(s1, ignored -> new Anim(1.0F)).nudge(0.0F, 0.35F);
                }

                this.open.clear();
                this.open.add(s);
                this.expand.computeIfAbsent(s, ignored -> new Anim(0.0F)).nudge(1.0F, 0.5F);
            }

            this.drop = null;
        }
    }

    private Feature previewableExpanded() {
        if (!this.open.isEmpty() && App.live()) {
            for (Feature feature : App.features().all()) {
                if (this.open.contains(feature.id()) && PreviewPane.supports(feature)) {
                    return feature;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private void openThemeColor(ThemeFeature themeFeature) {
        Opt.Tint opt$tint = themeFeature.channelTint(themeFeature.editChannel());
        float[] afloat = rgbToHsv((Integer)opt$tint.get());
        this.colorPick = new PanelScreen.ColorPick(
            opt$tint, themeFeature, this.width * 0.5F - 70.0F, this.height * 0.5F - 70.0F, afloat[0], afloat[1], afloat[2]
        );
        this.drop = null;
    }

    private void drawOpt(DrawContext g, Feature feature, Opt<?> opt, float x, float y, float w) {
        float f = this.uiScale;
        float f1 = 22.0F * f;
        boolean flag = Paint.hit((double)this.mx, (double)this.my, x, y, w, f1);
        Paint.box(g, x, y + 1.0F * f, w, f1 - 2.0F * f, Theme.alpha(flag ? Theme.ACCENT_DEEP : 16777215, flag ? 40 : 6), 7.0F * f);
        if (opt instanceof Opt.Flag opt$flag) {
            Paint.text(g, opt.label(), x + 6.0F * f, y + 6.0F * f, Theme.TEXT, 6.2F * f);
            String s = feature.id() + "/" + opt.id();
            Anim anim = this.optToggleAnim.computeIfAbsent(s, k -> new Anim(opt$flag.get() ? 1.0F : 0.0F));
            float f2 = anim.springToggle(opt$flag.get() ? 1.0F : 0.0F, this.frameDt);
            Paint.toggle(g, x + w - 36.0F * f, y + (f1 - 16.0F * f) * 0.5F, (Boolean)opt$flag.get(), f2);
            this.hits.add(new PanelScreen.Hit(x, y, w, f1, () -> {
                opt$flag.flip();
                anim.snap(opt$flag.get() ? 1.0F : 0.0F);
                feature.poke();
                GuiSfx.toggle((Boolean)opt$flag.get());
            }, true));
        } else if (opt instanceof Opt.Num opt$num) {
            Paint.text(g, opt.label(), x + 6.0F * f, y + 3.0F * f, Theme.MUTED, 5.4F * f);
            String s1 = this.numEdit != null && this.numEdit.num == opt$num
                ? this.numEdit.buf + (blink() ? "|" : "")
                : String.format(Locale.ROOT, "%.2f", opt$num.get()).replaceAll("0+$", "").replaceAll("\\.$", "");
            Paint.textR(g, s1, x + w - 6.0F * f, y + 3.0F * f, Theme.ACCENT_HOT, 5.6F * f);
            float f4 = x + 6.0F * f;
            float f6 = w - 12.0F * f;
            float f3 = (float)(((Double)opt$num.get() - opt$num.min()) / (opt$num.max() - opt$num.min()));
            Paint.slider(g, f4, y + 12.0F * f, f6, 6.0F * f, f3);
            this.hits.add(new PanelScreen.Hit(f4, y + 12.0F * f, f6, 10.0F * f, () -> {
                this.dragNum = new PanelScreen.DragNum(opt$num, feature, f4, f6);
                this.applyDrag(this.mx);
            }, true));
            this.hits.add(new PanelScreen.Hit(x + w - 54.0F * f, y, 54.0F * f, 12.0F * f, () -> {
                this.numEdit = new PanelScreen.NumEdit(opt$num, feature, s1.replace("|", ""));
                this.searchFocus = false;
            }));
            if (flag) {
                this.optTip = opt.label()
                    + " \u00b7 \u043a\u043b\u0438\u043a \u043f\u043e \u0447\u0438\u0441\u043b\u0443 \u0434\u043b\u044f \u0432\u0432\u043e\u0434\u0430";
                this.optTipSince = System.currentTimeMillis();
            }
        } else if (opt instanceof Opt.Pick opt$pick) {
            Paint.text(g, opt.label(), x + 6.0F * f, y + 6.0F * f, Theme.MUTED, 5.8F * f);
            String s2 = shorten((String)opt$pick.get(), 12);
            float f5 = 78.0F * f;
            Paint.box(g, x + w - f5 - 4.0F * f, y + 4.0F * f, f5, 14.0F * f, Theme.alpha(Theme.ACCENT_DEEP, 120), 5.0F * f);
            Paint.textR(g, s2, x + w - 8.0F * f, y + 6.0F * f, Theme.TEXT, 5.6F * f);
            this.hits
                .add(
                    new PanelScreen.Hit(
                        x + w - f5 - 4.0F * f, y, f5, f1, () -> this.drop = new PanelScreen.DropMenu(opt$pick, feature, x + w - 120.0F * f, y + f1, 116.0F * f)
                    )
                );
            if (flag) {
                this.optTip = opt.label() + " \u00b7 " + (String)opt$pick.get();
                this.optTipSince = System.currentTimeMillis();
            }
        } else if (opt instanceof Opt.Tint opt$tint) {
            Paint.text(g, opt.label(), x + 6.0F * f, y + 6.0F * f, Theme.MUTED, 5.8F * f);
            Nvg.circle(x + w - 14.0F * f, y + f1 * 0.5F, 5.5F * f, (Integer)opt$tint.get());
            this.hits.add(new PanelScreen.Hit(x + w - 28.0F * f, y, 28.0F * f, f1, () -> this.openColor(opt$tint, feature)));
        } else if (opt instanceof Opt.Key opt$key) {
            Paint.text(g, opt.label(), x + 6.0F * f, y + 6.0F * f, Theme.MUTED, 5.8F * f);
            String s3 = this.capture == opt$key ? "\u2026" : Keys.name((Integer)opt$key.get());
            Paint.textR(g, s3, x + w - 6.0F * f, y + 6.0F * f, Theme.TEXT, 5.8F * f);
            this.hits.add(new PanelScreen.Hit(x, y, w, f1, () -> {
                this.capture = opt$key;
                this.captureOwner = feature;
            }));
        } else if (opt instanceof Opt.Text opt$text) {
            Paint.text(g, opt.label(), x + 6.0F * f, y + 6.0F * f, Theme.MUTED, 5.8F * f);
            Paint.textR(
                g,
                ((String)opt$text.get()).isBlank() ? "\u0432\u0441\u0442\u0430\u0432\u0438\u0442\u044c" : shorten((String)opt$text.get(), 10),
                x + w - 6.0F * f,
                y + 6.0F * f,
                Theme.TEXT,
                5.4F * f
            );
            this.hits.add(new PanelScreen.Hit(x, y, w, f1, () -> {
                String s4 = MinecraftClient.getInstance().keyboard.getClipboard();
                if (s4 != null && !s4.isBlank()) {
                    opt$text.set(s4.trim());
                    feature.poke();
                }
            }));
        }
    }

    private void drawDrop(DrawContext g) {
        PanelScreen.DropMenu panelscreen$dropmenu = this.drop;
        if (panelscreen$dropmenu != null) {
            float f = 18.0F * this.uiScale;
            float f1 = 160.0F * this.uiScale;
            float f2 = panelscreen$dropmenu.pick.options().size() * f + 8.0F * this.uiScale;
            float f3 = Math.min(f1, f2);
            Paint.shadow(panelscreen$dropmenu.x, panelscreen$dropmenu.y, panelscreen$dropmenu.w, f3, 12.0F, 12.0F);
            Paint.box(g, panelscreen$dropmenu.x, panelscreen$dropmenu.y, panelscreen$dropmenu.w, f3, Theme.PANEL_HI, 8.4F);
            Nvg.scissor(panelscreen$dropmenu.x, panelscreen$dropmenu.y, panelscreen$dropmenu.w, f3);
            float f4 = panelscreen$dropmenu.y + 4.0F * this.uiScale + panelscreen$dropmenu.scroll;

            for (String s : panelscreen$dropmenu.pick.options()) {
                boolean flag = s.equals(panelscreen$dropmenu.pick.get());
                boolean flag1 = Paint.hit((double)this.mx, (double)this.my, panelscreen$dropmenu.x, f4, panelscreen$dropmenu.w, f);
                if (flag || flag1) {
                    Paint.box(
                        g,
                        panelscreen$dropmenu.x + 3.0F * this.uiScale,
                        f4,
                        panelscreen$dropmenu.w - 6.0F * this.uiScale,
                        f,
                        flag ? Theme.alpha(Theme.ACCENT, 48) : Theme.alpha(16777215, 10),
                        5.0F * this.uiScale
                    );
                }

                Paint.text(
                    g, s, panelscreen$dropmenu.x + 8.0F * this.uiScale, f4 + 4.0F * this.uiScale, flag ? Theme.ACCENT_HOT : Theme.TEXT, 6.0F * this.uiScale
                );
                this.hits.add(new PanelScreen.Hit(panelscreen$dropmenu.x, f4, panelscreen$dropmenu.w, f, () -> {
                    panelscreen$dropmenu.pick.set(s);
                    panelscreen$dropmenu.owner.poke();
                    this.drop = null;
                }));
                f4 += f;
            }

            Nvg.unscissor();
            panelscreen$dropmenu.maxScroll = Math.min(0.0F, f3 - f2);
            panelscreen$dropmenu.scroll = Math.max(panelscreen$dropmenu.maxScroll, Math.min(0.0F, panelscreen$dropmenu.scroll));
        }
    }

    private void openColor(Opt.Tint tint, Feature owner) {
        float[] afloat = rgbToHsv((Integer)tint.get());
        this.colorPick = new PanelScreen.ColorPick(
            tint, owner, this.width * 0.5F - 70.0F, this.height * 0.5F - 70.0F, afloat[0], afloat[1], afloat[2]
        );
        this.drop = null;
    }

    private void drawColor(DrawContext g) {
        PanelScreen.ColorPick panelscreen$colorpick = this.colorPick;
        if (panelscreen$colorpick != null) {
            float f = panelscreen$colorpick.x;
            float f1 = panelscreen$colorpick.y;
            float f2 = f + 64.0F;
            float f3 = f1 + 78.0F;
            float f4 = 52.0F;
            Paint.shadow(f - 8.0F, f1 - 8.0F, 148.0F, 188.0F, 18.0F, 12.0F);
            Paint.box(g, f - 8.0F, f1 - 8.0F, 148.0F, 188.0F, Theme.PANEL, 12.0F);
            Paint.textC(g, "\u0426\u0432\u0435\u0442", f + 66.0F, f1 - 2.0F, Theme.TEXT, 8.0F);
            int i = 18;
            int j = 48;

            for (int k = 0; k < i; k++) {
                float f5 = (k + 0.5F) / i;
                float f6 = f4 * k / i;
                float f7 = f4 * (k + 1) / i;

                for (int l = 0; l < j; l++) {
                    float f8 = (float)l / j;
                    double d0 = f8 * Math.PI * 2.0;
                    float f9 = f2 + (float)Math.cos(d0) * ((f6 + f7) * 0.5F);
                    float f10 = f3 + (float)Math.sin(d0) * ((f6 + f7) * 0.5F);
                    Nvg.circle(f9, f10, (f7 - f6) * 0.7F + 1.2F, hsvToRgb(f8, f5, panelscreen$colorpick.v));
                }
            }

            float f11 = panelscreen$colorpick.h * (float) Math.PI * 2.0F;
            Nvg.circle(f2 + (float)Math.cos(f11) * panelscreen$colorpick.s * f4, f3 + (float)Math.sin(f11) * panelscreen$colorpick.s * f4, 5.0F, Theme.TEXT);

            for (int i1 = 0; i1 < 36; i1++) {
                float f12 = i1 / 35.0F;
                Paint.box(g, f + 8.0F + i1 * 3.3F, f1 + 138.0F, 3.6F, 10.0F, hsvToRgb(panelscreen$colorpick.h, panelscreen$colorpick.s, f12), 0.0F);
            }

            int j1 = hsvToRgb(panelscreen$colorpick.h, panelscreen$colorpick.s, panelscreen$colorpick.v);
            Paint.box(g, f + 8.0F, f1 + 154.0F, 128.0F, 16.0F, j1, 6.0F);
            Paint.textC(g, "\u0413\u043e\u0442\u043e\u0432\u043e", f + 72.0F, f1 + 157.0F, Theme.TEXT, 7.0F);
            this.hits.add(new PanelScreen.Hit(f + 8.0F, f1 + 154.0F, 128.0F, 16.0F, () -> {
                applyPickedColor(panelscreen$colorpick, j1 | 0xFF000000);
                this.colorPick = null;
            }));
            this.hits
                .add(
                    new PanelScreen.Hit(
                        f2 - f4, f3 - f4, f4 * 2.0F, f4 * 2.0F, () -> this.pickHsvCircle(panelscreen$colorpick, f2, f3, f4, this.mx, this.my), true
                    )
                );
            this.hits.add(new PanelScreen.Hit(f + 8.0F, f1 + 138.0F, 120.0F, 10.0F, () -> {
                panelscreen$colorpick.v = Math.max(0.0F, Math.min(1.0F, (this.mx - (f + 8.0F)) / 120.0F));
                applyPickedColor(panelscreen$colorpick, hsvToRgb(panelscreen$colorpick.h, panelscreen$colorpick.s, panelscreen$colorpick.v));
            }, true));
        }
    }

    private void pickHsvCircle(PanelScreen.ColorPick picker, float cx, float cy, float rad, float px, float py) {
        float f = px - cx;
        float f1 = py - cy;
        float f2 = (float)Math.sqrt(f * f + f1 * f1);
        float f3 = (float)Math.atan2(f1, f);
        if (f3 < 0.0F) {
            f3 += (float) (Math.PI * 2);
        }

        picker.h = f3 / (float) (Math.PI * 2);
        picker.s = Math.max(0.0F, Math.min(1.0F, f2 / rad));
        applyPickedColor(picker, hsvToRgb(picker.h, picker.s, picker.v));
    }

    private void drawBannerPick(DrawContext g) {
        float f = this.uiScale;
        float f1 = 240.0F * f;
        float f2 = 280.0F * f;
        float f3 = this.navRect[0] + this.navRect[2] + 12.0F * f;
        float f4 = this.navRect[1] + 40.0F * f;
        Paint.shadow(f3, f4, f1, f2, 16.0F, 12.0F);
        Paint.chromeGui(g, f3, f4, f1, f2, 12.0F);
        Paint.text(g, "GIF / \u0431\u0430\u043d\u043d\u0435\u0440", f3 + 14.0F * f, f4 + 12.0F * f, Theme.TEXT, 9.0F * f);
        Paint.textR(g, "\u2715", f3 + f1 - 16.0F * f, f4 + 12.0F * f, Theme.MUTED, 10.0F * f);
        this.hits.add(new PanelScreen.Hit(f3 + f1 - 32.0F * f, f4 + 8.0F * f, 24.0F * f, 22.0F * f, () -> this.bannerPick = false));
        float f5 = f4 + 34.0F * f + this.bannerPickScroll;
        float f6 = (f1 - 36.0F * f) / 2.0F;
        float f7 = 52.0F * f;
        int i = 0;
        Nvg.scissor(f3, f4 + 32.0F * f, f1, f2 - 70.0F * f);

        for (BannerLibrary.Entry bannerlibrary$entry : BannerLibrary.entries()) {
            float f8 = f3 + 12.0F * f + i * (f6 + 8.0F * f);
            boolean flag = bannerlibrary$entry.id().equals(BannerLibrary.selectedId());
            Paint.box(g, f8, f5, f6, f7, Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 40 : 10), 8.0F);
            Image image = BannerLibrary.preview(bannerlibrary$entry);
            if (image != null) {
                Nvg.imageCover(image, f8 + 4.0F * f, f5 + 4.0F * f, f6 - 8.0F * f, f7 - 18.0F * f, 6.0F);
            }

            Paint.text(
                g, fit(bannerlibrary$entry.label(), f6 - 10.0F * f, 5.4F * f), f8 + 6.0F * f, f5 + f7 - 12.0F * f, flag ? Theme.TEXT : Theme.MUTED, 5.4F * f
            );
            this.hits.add(new PanelScreen.Hit(f8, f5, f6, f7, () -> {
                BannerLibrary.select(bannerlibrary$entry.id());
                if (App.live()) {
                    App.save().defer();
                }
            }));
            if (++i >= 2) {
                i = 0;
                f5 += f7 + 8.0F * f;
            }
        }

        Nvg.unscissor();
        float f9 = f4 + f2 - 28.0F * f;
        Paint.box(g, f3 + 12.0F * f, f9, f1 - 24.0F * f, 18.0F * f, Theme.alpha(Theme.ACCENT, 40), 7.0F);
        Paint.textC(g, "Add PNG / GIF / MP4", f3 + f1 * 0.5F, f9 + 4.0F * f, Theme.ACCENT_HOT, 6.2F * f);
        this.hits
            .add(
                new PanelScreen.Hit(
                    f3 + 12.0F * f,
                    f9,
                    f1 - 24.0F * f,
                    18.0F * f,
                    () -> Folders.pickImage(
                        path -> MinecraftClient.getInstance()
                            .execute(
                                () -> {
                                    boolean flag1 = BannerLibrary.importFile(path);
                                    App.features()
                                        .find(HudFeature.class)
                                        .ifPresent(
                                            hud -> hud.notify(
                                                "\u0411\u0430\u043d\u043d\u0435\u0440\u044b",
                                                flag1
                                                    ? "\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e: " + path.getFileName()
                                                    : "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0438\u043c\u043f\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c"
                                            )
                                        );
                                }
                            ),
                        () -> MinecraftClient.getInstance()
                            .execute(
                                () -> {
                                    Folders.ensure(BannerLibrary.dir());
                                    Folders.open(BannerLibrary.dir());
                                    BannerLibrary.reload();
                                    App.features()
                                        .find(HudFeature.class)
                                        .ifPresent(
                                            hud -> hud.notify(
                                                "\u0411\u0430\u043d\u043d\u0435\u0440\u044b",
                                                "\u041f\u043e\u043b\u043e\u0436\u0438 GIF \u0432 \u043f\u0430\u043f\u043a\u0443"
                                            )
                                        );
                                }
                            )
                    )
                )
            );
    }

    private static void applyPickedColor(PanelScreen.ColorPick picker, int color) {
        picker.tint.set(color | 0xFF000000);
        if (picker.owner instanceof ThemeFeature themefeature) {
            themefeature.setChannelColor(themefeature.editChannel(), color);
        } else {
            picker.owner.poke();
        }
    }

    private void applyDrag(double mouseX) {
        if (this.dragNum != null) {
            float f = (float)((mouseX - this.dragNum.x) / this.dragNum.w);
            f = Math.max(0.0F, Math.min(1.0F, f));
            this.dragNum.num.set(this.dragNum.num.min() + (this.dragNum.num.max() - this.dragNum.num.min()) * f);
            this.dragNum.owner.poke();
        }
    }

    private void persistLayout() {
        if (App.live()) {
            App.save().defer();
        }
    }

    private static void toastModule(Feature feature) {
        App.features()
            .find(HudFeature.class)
            .ifPresent(
                hud -> hud.notify(
                    feature.name(),
                    feature.on() ? "\u0432\u043a\u043b\u044e\u0447\u0435\u043d\u043e" : "\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043e",
                    feature.on() ? HudFeature.NoteKind.OK : HudFeature.NoteKind.WARN
                )
            );
    }

    private void clickUi() {
        GuiSfx.click();
    }

    private void drawBridge(DrawContext g) {
        if (!(this.appearBody.value() < 0.25F) && !(this.bodyRect[2] < 8.0F)) {
            float f = this.bodyRect[0] - (this.navRect[0] + this.navRect[2]);
            if (!(f <= 1.0F) && !(f > 40.0F) && !(Math.abs(this.navRect[1] - this.bodyRect[1]) > 28.0F)) {
                float f1 = this.appearBody.value();
                float f2 = this.navRect[1] + 76.0F * this.uiScale;
                Paint.box(g, this.navRect[0] + this.navRect[2] - 2.0F, f2, f + 4.0F, 2.2F * this.uiScale, Theme.alpha(Theme.ACCENT, (int)(50.0F * f1)), 1.0F);
            }
        }
    }

    private void drawModuleTip(DrawContext g, Feature hovered) {
        if (hovered != null && hovered.about() != null && !hovered.about().isBlank()) {
            if (!hovered.id().equals(this.tipId)) {
                this.tipId = hovered.id();
                this.tipSince = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - this.tipSince >= 380L) {
                String s1 = hovered.about();
                float f6 = this.uiScale;
                float f7 = 6.2F * f6;
                float f8 = Paint.tw(s1, f7) + 16.0F * f6;
                float f9 = 18.0F * f6;
                float f10 = Math.min(this.width - f8 - 8.0F, this.mx + 14.0F);
                float f11 = Math.max(8.0F, this.my - f9 - 10.0F);
                Paint.chromeGui(g, f10, f11, f8, f9, 7.0F * f6);
                Paint.text(g, s1, f10 + 8.0F * f6, f11 + 5.0F * f6, Theme.TEXT, f7);
            }
        } else {
            if (!this.optTip.isEmpty() && System.currentTimeMillis() - this.optTipSince < 800L) {
                String s = this.optTip;
                float f = this.uiScale;
                float f1 = 6.2F * f;
                float f2 = Paint.tw(s, f1) + 16.0F * f;
                float f3 = 18.0F * f;
                float f4 = Math.min(this.width - f2 - 8.0F, this.mx + 14.0F);
                float f5 = Math.max(8.0F, this.my - f3 - 10.0F);
                Paint.chromeGui(g, f4, f5, f2, f3, 7.0F * f);
                Paint.text(g, s, f4 + 8.0F * f, f5 + 5.0F * f, Theme.TEXT, f1);
            }

            this.tipId = "";
        }
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "";
        } else {
            return value.length() <= max ? value : value.substring(0, max - 1) + "\u2026";
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

    private static boolean blink() {
        return System.currentTimeMillis() / 420L % 2L == 0L;
    }

    private static boolean leftDown() {
        return GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 1;
    }

    private boolean overNav(double x, double y) {
        return Paint.hit(x, y, this.navRect[0], this.navRect[1], this.navRect[2], this.navRect[3]);
    }

    private boolean overBody(double x, double y) {
        return Paint.hit(x, y, this.bodyRect[0], this.bodyRect[1], this.bodyRect[2], this.bodyRect[3]);
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        boolean flag = event.button() == 1;
        boolean flag1 = event.button() == 0;
        if (!flag1 && !flag) {
            return super.mouseClicked(event, doubled);
        } else {
            if (this.overNav(event.x(), event.y())) {
                this.navFront = true;
            } else if (this.overBody(event.x(), event.y())) {
                this.navFront = false;
            }

            for (int i = this.hits.size() - 1; i >= 0; i--) {
                PanelScreen.Hit panelscreen$hit = this.hits.get(i);
                if (Paint.hit(event.x(), event.y(), panelscreen$hit.x, panelscreen$hit.y, panelscreen$hit.w, panelscreen$hit.h)) {
                    if (flag && panelscreen$hit.right != null) {
                        panelscreen$hit.right.run();
                        GuiSfx.click();
                    } else {
                        panelscreen$hit.action.run();
                        if (flag1 && panelscreen$hit.sfx) {
                            GuiSfx.click();
                        }
                    }

                    return true;
                }
            }

            if (!this.overBody(event.x(), event.y()) && !this.overNav(event.x(), event.y())) {
                this.searchFocus = false;
                this.themeNameFocus = false;
                this.configNameFocus = false;
            }

            if (this.drop != null) {
                this.drop = null;
            }

            if (this.colorPick != null && !Paint.hit(event.x(), event.y(), this.colorPick.x - 8.0F, this.colorPick.y - 8.0F, 156.0F, 158.0F)) {
                this.colorPick = null;
            }

            return super.mouseClicked(event, doubled);
        }
    }

    public boolean mouseDragged(Click event, double dx, double dy) {
        if (PreviewPane.mouseDragged(event.x(), event.y(), dx, dy)) {
            return true;
        } else if (this.cosmeticsOpen && CosmeticsDrawer.mouseDragged(event.x(), event.y(), dx, dy)) {
            return true;
        } else {
            if (this.dockHold != null) {
                this.tickDockDrag();
                if (this.dockDragging) {
                    return true;
                }
            }

            if (this.bannerDown && !this.draggingNav) {
                float f = (float)event.x() - this.bannerStartX;
                float f1 = (float)event.y() - this.bannerStartY;
                if (f * f + f1 * f1 > 36.0F) {
                    this.bannerDragged = true;
                    this.draggingNav = true;
                    this.navFront = true;
                }
            }

            if (this.draggingNav) {
                GuiLayout.navX = this.clampX((float)event.x() - this.dragOffX, 200.0F * this.uiScale);
                GuiLayout.navY = this.clampY((float)event.y() - this.dragOffY, 470.0F * this.uiScale);
                return true;
            } else if (this.draggingBody) {
                GuiLayout.bodyX = this.clampX((float)event.x() - this.dragOffX, 470.0F * this.uiScale);
                GuiLayout.bodyY = this.clampY((float)event.y() - this.dragOffY, 470.0F * this.uiScale);
                return true;
            } else if (this.dragNum != null) {
                this.applyDrag(event.x());
                return true;
            } else {
                if (this.colorPick != null && leftDown()) {
                    float f4 = this.colorPick.x;
                    float f5 = this.colorPick.y;
                    float f2 = f4 + 64.0F;
                    float f3 = f5 + 78.0F;
                    if (Paint.hit(event.x(), event.y(), f2 - 52.0F, f3 - 52.0F, 104.0F, 104.0F)) {
                        this.pickHsvCircle(this.colorPick, f2, f3, 52.0F, (float)event.x(), (float)event.y());
                        return true;
                    }

                    if (Paint.hit(event.x(), event.y(), f4 + 8.0F, f5 + 138.0F, 120.0F, 10.0F)) {
                        this.colorPick.v = Math.max(0.0F, Math.min(1.0F, ((float)event.x() - (f4 + 8.0F)) / 120.0F));
                        applyPickedColor(this.colorPick, hsvToRgb(this.colorPick.h, this.colorPick.s, this.colorPick.v));
                        return true;
                    }
                }

                return super.mouseDragged(event, dx, dy);
            }
        }
    }

    public boolean mouseReleased(Click event) {
        PreviewPane.endYawDrag();
        CosmeticsDrawer.endYawDrag();
        this.bannerDown = false;
        this.bannerDragged = false;
        if (this.dockHold != null) {
            this.finishDock();
        }

        if (this.draggingNav || this.draggingBody) {
            this.finishPanelDrag(this.draggingNav);
        }

        this.dragNum = null;
        return super.mouseReleased(event);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float f = (float)v * 28.0F * this.uiScale;
        if (this.drop != null && Paint.hit(mouseX, mouseY, this.drop.x, this.drop.y, this.drop.w, 160.0F * this.uiScale)) {
            this.drop.scroll += f;
            return true;
        } else if (this.bannerPick) {
            this.bannerPickScroll += f;
            return true;
        } else if (this.overBody(mouseX, mouseY)) {
            if (this.settingsOpen) {
                ThemeConfigUi.scrollTheme(f);
            } else if (this.bindsOpen) {
                this.inspectScrollVel += f * 0.65F;
                this.inspectScroll += f * 0.35F;
            } else if (GuiLayout.selected() == Category.CONFIG) {
                ThemeConfigUi.scrollConfig(f);
            } else if (Paint.hit(mouseX, mouseY, this.inspectRect[0], this.inspectRect[1], this.inspectRect[2], this.inspectRect[3])) {
                this.inspectScrollVel += f * 0.65F;
                this.inspectScroll += f * 0.35F;
            } else {
                this.scrollVel += f * 0.65F;
                GuiLayout.get(GuiLayout.selected()).scroll += f * 0.35F;
            }

            return true;
        } else {
            return true;
        }
    }

    public boolean charTyped(CharInput event) {
        char c0 = (char)event.codepoint();
        if (this.numEdit == null || !Character.isDigit(c0) && c0 != '.' && c0 != '-' && c0 != ',') {
            if (this.themeNameFocus) {
                App.features().find(ThemeFeature.class).ifPresent(tf -> tf.setDraftName(tf.draftName() + c0));
                return true;
            } else if (this.configNameFocus) {
                App.features().find(ConfigFeature.class).ifPresent(cf -> cf.setDraftName(cf.draftName() + c0));
                return true;
            } else if (this.searchFocus) {
                if (GuiLayout.selected() == Category.CONFIG) {
                    App.features().find(ConfigFeature.class).ifPresent(cf -> cf.setFilter(cf.filter() + c0));
                } else {
                    this.moduleSearch = this.moduleSearch + c0;
                }

                return true;
            } else {
                return super.charTyped(event);
            }
        } else {
            this.numEdit.buf = this.numEdit.buf + c0;
            return true;
        }
    }

    public boolean keyPressed(KeyInput event) {
        if (this.numEdit != null) {
            if (event.key() == 257 || event.key() == 335) {
                this.numEdit.commit();
                this.numEdit = null;
                return true;
            }

            if (event.key() == 256) {
                this.numEdit = null;
                return true;
            }

            if (event.key() == 259 && !this.numEdit.buf.isEmpty()) {
                this.numEdit.buf = this.numEdit.buf.substring(0, this.numEdit.buf.length() - 1);
                return true;
            }
        }

        if (this.capture != null) {
            this.capture.set(event.key() == 256 ? -1 : event.key());
            if (this.captureOwner != null) {
                this.captureOwner.poke();
            }

            this.capture = null;
            this.captureOwner = null;
            return true;
        } else {
            boolean flag = (event.modifiers() & 2) != 0 || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 341) == 1;
            if (flag && event.key() == 70) {
                this.searchFocus = true;
                this.themeNameFocus = false;
                this.configNameFocus = false;
                return true;
            } else {
                if (event.key() == 259) {
                    if (this.themeNameFocus) {
                        App.features().find(ThemeFeature.class).ifPresent(tf -> {
                            String s = tf.draftName();
                            if (!s.isEmpty()) {
                                tf.setDraftName(s.substring(0, s.length() - 1));
                            }
                        });
                        return true;
                    }

                    if (this.configNameFocus) {
                        App.features().find(ConfigFeature.class).ifPresent(cf -> {
                            String s = cf.draftName();
                            if (!s.isEmpty()) {
                                cf.setDraftName(s.substring(0, s.length() - 1));
                            }
                        });
                        return true;
                    }

                    if (this.searchFocus) {
                        if (GuiLayout.selected() == Category.CONFIG) {
                            App.features().find(ConfigFeature.class).ifPresent(cf -> {
                                String s = cf.filter();
                                if (!s.isEmpty()) {
                                    cf.setFilter(s.substring(0, s.length() - 1));
                                }
                            });
                        } else if (!this.moduleSearch.isEmpty()) {
                            this.moduleSearch = this.moduleSearch.substring(0, this.moduleSearch.length() - 1);
                        }

                        return true;
                    }
                }

                if (event.key() != 256) {
                    return super.keyPressed(event);
                } else if (this.bannerPick) {
                    this.bannerPick = false;
                    return true;
                } else if (this.numEdit != null) {
                    this.numEdit = null;
                    return true;
                } else if (this.colorPick != null) {
                    this.colorPick = null;
                    return true;
                } else if (this.drop != null) {
                    this.drop = null;
                    return true;
                } else if (!this.themeNameFocus && !this.configNameFocus && !this.searchFocus) {
                    this.beginClose();
                    return true;
                } else {
                    this.themeNameFocus = false;
                    this.configNameFocus = false;
                    this.searchFocus = false;
                    return true;
                }
            }
        }
    }

    public void onFilesDropped(List<Path> paths) {
        if (paths != null && !paths.isEmpty() && App.live()) {
            int i = 0;
            int j = 0;

            for (Path path : paths) {
                String s = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (s.endsWith(".cfg")) {
                    if (JsonImport.importConfig(path)) {
                        i++;
                    }
                } else if (BannerLibrary.importFile(path)) {
                    j++;
                }
            }

            if (i <= 0) {
                if (j > 0) {
                    int k = j;
                    App.features()
                        .find(HudFeature.class)
                        .ifPresent(
                            hud -> hud.notify("\u0411\u0430\u043d\u043d\u0435\u0440\u044b", "\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e: " + k)
                        );
                } else {
                    App.features()
                        .find(HudFeature.class)
                        .ifPresent(hud -> hud.notify("\u0418\u043c\u043f\u043e\u0440\u0442", "\u041d\u0443\u0436\u0435\u043d .cfg / PNG / GIF"));
                }
            }
        }
    }

    public void requestClose() {
        this.beginClose();
    }

    private void beginClose() {
        if (!this.closing) {
            this.closing = true;
            if (!this.closeSfx) {
                this.closeSfx = true;
                GuiSfx.close();
            }
        }
    }

    private void finishClose() {
        if (!this.finished) {
            this.finished = true;
            this.persistLayout();
            if (this.client != null && this.client.currentScreen == this) {
                this.client.setScreen(null);
            }

            if (App.live()) {
                App.save().flush();
            }
        }
    }

    public void close() {
        if (!this.finished) {
            if (!this.closing) {
                this.beginClose();
            } else {
                this.finishClose();
            }
        }
    }

    public void removed() {
        BannerLibrary.unload();
        CosmeticsDrawer.unload();
        super.removed();
    }

    public boolean shouldPause() {
        return false;
    }

    private static float[] rgbToHsv(int argb) {
        float f = (argb >> 16 & 0xFF) / 255.0F;
        float f1 = (argb >> 8 & 0xFF) / 255.0F;
        float f2 = (argb & 0xFF) / 255.0F;
        float f3 = Math.max(f, Math.max(f1, f2));
        float f4 = Math.min(f, Math.min(f1, f2));
        float f5 = f3 - f4;
        float f6;
        if (f5 < 1.0E-5F) {
            f6 = 0.0F;
        } else if (f3 == f) {
            f6 = (f1 - f2) / f5 % 6.0F;
        } else if (f3 == f1) {
            f6 = (f2 - f) / f5 + 2.0F;
        } else {
            f6 = (f - f1) / f5 + 4.0F;
        }

        f6 /= 6.0F;
        if (f6 < 0.0F) {
            f6++;
        }

        float f7 = f3 < 1.0E-5F ? 0.0F : f5 / f3;
        return new float[]{f6, f7, f3};
    }

    private static int hsvToRgb(float h, float s, float v) {
        float f = v * s;
        float f1 = f * (1.0F - Math.abs(h * 6.0F % 2.0F - 1.0F));
        float f2 = v - f;
        float f3 = 0.0F;
        float f4 = 0.0F;
        float f5 = 0.0F;
        float f6 = h * 6.0F;
        if (f6 < 1.0F) {
            f3 = f;
            f4 = f1;
        } else if (f6 < 2.0F) {
            f3 = f1;
            f4 = f;
        } else if (f6 < 3.0F) {
            f4 = f;
            f5 = f1;
        } else if (f6 < 4.0F) {
            f4 = f1;
            f5 = f;
        } else if (f6 < 5.0F) {
            f3 = f1;
            f5 = f;
        } else {
            f3 = f;
            f5 = f1;
        }

        int i = Math.round((f3 + f2) * 255.0F);
        int j = Math.round((f4 + f2) * 255.0F);
        int k = Math.round((f5 + f2) * 255.0F);
        return 0xFF000000 | i << 16 | j << 8 | k;
    }

    private static final class ColorPick {
        final Opt.Tint tint;
        final Feature owner;
        final float x;
        final float y;
        float h;
        float s;
        float v;

        ColorPick(Opt.Tint tint, Feature owner, float x, float y, float h, float s, float v) {
            this.tint = tint;
            this.owner = owner;
            this.x = x;
            this.y = y;
            this.h = h;
            this.s = s;
            this.v = v;
        }
    }

    private static final class DragNum {
        final Opt.Num num;
        final Feature owner;
        final float x;
        final float w;

        DragNum(Opt.Num num, Feature owner, float x, float w) {
            this.num = num;
            this.owner = owner;
            this.x = x;
            this.w = w;
        }
    }

    private static final class DropMenu {
        final Opt.Pick pick;
        final Feature owner;
        final float x;
        final float y;
        final float w;
        float scroll;
        float maxScroll;

        DropMenu(Opt.Pick pick, Feature owner, float x, float y, float w) {
            this.pick = pick;
            this.owner = owner;
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }

    private static final class Hit {
        final float x;
        final float y;
        final float w;
        final float h;
        final Runnable action;
        final Runnable right;
        final boolean sfx;

        Hit(float x, float y, float w, float h, Runnable action) {
            this(x, y, w, h, action, null, false);
        }

        Hit(float x, float y, float w, float h, Runnable action, boolean quiet) {
            this(x, y, w, h, action, null, quiet);
        }

        Hit(float x, float y, float w, float h, Runnable action, Runnable right) {
            this(x, y, w, h, action, right, false);
        }

        Hit(float x, float y, float w, float h, Runnable action, Runnable right, boolean quiet) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
            this.right = right;
            this.sfx = !quiet;
        }
    }

    private static final class NumEdit {
        final Opt.Num num;
        final Feature owner;
        String buf;

        NumEdit(Opt.Num num, Feature owner, String buf) {
            this.num = num;
            this.owner = owner;
            this.buf = buf == null ? "" : buf;
        }

        void commit() {
            try {
                this.num.set(Double.parseDouble(this.buf.replace(',', '.')));
                this.owner.poke();
            } catch (Exception exception) {
            }
        }
    }
}
