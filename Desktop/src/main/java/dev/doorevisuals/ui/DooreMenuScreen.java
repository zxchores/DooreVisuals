package dev.doorevisuals.ui;

import com.github.noamm9.nvgrenderer.nvg.Image;
import dev.doorevisuals.data.Account;
import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.data.Changelog;
import dev.doorevisuals.data.ChangelogSeen;
import dev.doorevisuals.data.FirstRun;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

public final class DooreMenuScreen extends Screen {
    private static final float BTN_H = 44.0F;
    private static final float BTN_GAP = 12.0F;
    private static final float BTN_R = 12.0F;
    private static final float ICON = 40.0F;
    private static final float ICON_GAP = 12.0F;
    private final List<DooreMenuScreen.MenuBtn> buttons = new ArrayList<>();
    private final List<DooreMenuScreen.IconBtn> icons = new ArrayList<>();
    private final List<Changelog.Entry> news = Changelog.load();
    private final List<DooreMenuScreen.Hit> hits = new ArrayList<>();
    private float btnW;
    private int mx;
    private int my;
    private int laidOutForW = -1;
    private int laidOutForH = -1;
    private int markBottomY;
    private float brandCx;
    private float brandCy;
    private float brandSize;
    private final Anim brandYaw = new Anim(0.0F);
    private final Anim brandPitch = new Anim(0.0F);
    private final Anim brandHover = new Anim(0.0F);
    private float newsX;
    private float newsY;
    private float newsW;
    private float newsH;

    public DooreMenuScreen() {
        super(Text.literal("DooreVisuals"));
    }

    protected void init() {
        AltsStore.boot();
        this.rebuildButtons();
        if (FirstRun.needsWizard()) {
            this.client.setScreen(new FirstRunScreen(this));
        }
    }

    public void resize(int w, int h) {
        super.resize(w, h);
        this.rebuildButtons();
    }

    private void rebuildButtons() {
        this.buttons.clear();
        this.icons.clear();
        this.laidOutForW = this.width;
        this.laidOutForH = this.height;
        if (this.width > 0 && this.height > 0) {
            this.btnW = Math.min(236.0F, Math.max(188.0F, this.width * 0.26F));
            float f = this.width - this.btnW - Math.max(36.0F, this.width * 0.07F);
            float f1 = this.height * 0.42F;
            this.buttons
                .add(
                    new DooreMenuScreen.MenuBtn(
                        "\u041e\u0434\u0438\u043d\u043e\u0447\u043d\u0430\u044f \u0438\u0433\u0440\u0430",
                        f,
                        f1,
                        true,
                        () -> this.client.setScreen(new SelectWorldScreen(this))
                    )
                );
            this.buttons
                .add(
                    new DooreMenuScreen.MenuBtn(
                        "\u041c\u0443\u043b\u044c\u0442\u0438\u043f\u043b\u0435\u0435\u0440",
                        f,
                        f1 + 44.0F + 12.0F,
                        false,
                        () -> this.client.setScreen(new MultiplayerScreen(this))
                    )
                );
            DooreMenuScreen.LastServer dooremenuscreen$lastserver = this.resolveLastServer();
            List<DooreMenuScreen.IconBtn> list = new ArrayList<>();
            if (dooremenuscreen$lastserver != null) {
                list.add(new DooreMenuScreen.IconBtn("last", "\u0421\u0435\u0440\u0432\u0435\u0440", () -> this.connectLast(dooremenuscreen$lastserver)));
            }

            list.add(
                new DooreMenuScreen.IconBtn(
                    "alts", "\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b", () -> this.client.setScreen(new AltManagerScreen(this))
                )
            );
            list.add(
                new DooreMenuScreen.IconBtn(
                    "opts",
                    "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438",
                    () -> this.client.setScreen(new OptionsScreen(this, this.client.options))
                )
            );
            list.add(
                new DooreMenuScreen.IconBtn(
                    "news",
                    ChangelogSeen.hasNew()
                        ? "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f \u00b7 NEW"
                        : "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f",
                    () -> this.client.setScreen(new ChangelogScreen(this))
                )
            );
            list.add(new DooreMenuScreen.IconBtn("quit", "\u0412\u044b\u0445\u043e\u0434", () -> this.client.scheduleStop()));
            float f2 = list.size() * 40.0F + (list.size() - 1) * 12.0F;
            float f3 = (this.width - f2) * 0.5F;
            float f4 = this.height - 40.0F - 28.0F;

            for (int i = 0; i < list.size(); i++) {
                DooreMenuScreen.IconBtn dooremenuscreen$iconbtn = list.get(i);
                this.icons
                    .add(
                        new DooreMenuScreen.IconBtn(dooremenuscreen$iconbtn.id, dooremenuscreen$iconbtn.tip, f3 + i * 52.0F, f4, dooremenuscreen$iconbtn.action)
                    );
            }
        }
    }

    private DooreMenuScreen.LastServer resolveLastServer() {
        if (this.client != null && this.client.options != null) {
            String s = this.client.options.lastServer;
            if (s != null && !s.isBlank()) {
                s = s.trim();
                String s1 = s;

                try {
                    ServerList serverlist = new ServerList(this.client);
                    serverlist.loadFile();
                    ServerInfo serverinfo = serverlist.get(s);
                    if (serverinfo != null && serverinfo.name != null && !serverinfo.name.isBlank()) {
                        s1 = serverinfo.name;
                    } else {
                        for (int i = 0; i < serverlist.size(); i++) {
                            ServerInfo serverinfo1 = serverlist.get(i);
                            if (serverinfo1 != null && s.equalsIgnoreCase(serverinfo1.address)) {
                                s1 = serverinfo1.name != null && !serverinfo1.name.isBlank() ? serverinfo1.name : s;
                                break;
                            }
                        }
                    }
                } catch (Throwable throwable) {
                }

                return new DooreMenuScreen.LastServer(s1, s);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private void connectLast(DooreMenuScreen.LastServer last) {
        try {
            ServerAddress serveraddress = ServerAddress.parse(last.ip);
            ServerInfo serverinfo = new ServerInfo(last.label, last.ip, ServerType.OTHER);
            ConnectScreen.connect(this, this.client, serveraddress, serverinfo, false, null);
        } catch (Throwable throwable) {
            this.client.setScreen(new MultiplayerScreen(this));
        }
    }

    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        MenuAtmosphere.paintBackground(g, this.width, this.height);
    }

    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        if (this.width != this.laidOutForW || this.height != this.laidOutForH || this.buttons.isEmpty()) {
            this.rebuildButtons();
        }

        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        this.paintBrandMark(g);
        Nvg.run(g, () -> {
            this.paintCinematicAtmosphere(g);
            this.paintBrandText(g);
            this.paintNewsCard(g);

            for (DooreMenuScreen.MenuBtn dooremenuscreen$menubtn : this.buttons) {
                this.paintButton(g, dooremenuscreen$menubtn);
            }

            String s = null;

            for (DooreMenuScreen.IconBtn dooremenuscreen$iconbtn : this.icons) {
                if (this.paintIcon(g, dooremenuscreen$iconbtn)) {
                    s = dooremenuscreen$iconbtn.tip;
                }
            }

            if (s != null) {
                Paint.textC(g, s, this.width * 0.5F, this.height - 18.0F, Theme.MUTED, 7.2F);
            }

            Paint.textR(g, "v3.30.2", this.width - 18, 16.0F, Theme.alpha(Theme.MUTED, 170), 8.0F);
        });
    }

    private void paintCinematicAtmosphere(DrawContext g) {
        float f = Anim.timeSec();
        float f1 = (float)Math.sin(f * 0.22F);
        Nvg.circle(this.width * 0.22F + f1 * 22.0F, this.height * 0.28F, Math.max(90.0F, this.height * 0.28F), Theme.alpha(Theme.ACCENT, 12));
        Nvg.circle(
            this.width * 0.68F - f1 * 18.0F, this.height * 0.72F, Math.max(110.0F, this.height * 0.34F), Theme.alpha(Theme.ACCENT_DIM, 10)
        );

        for (int i = 0; i < 14; i++) {
            float f2 = (i * 113.0F + f * (3.0F + i % 4)) % Math.max(1, this.width);
            float f3 = (i * 71.0F + (float)Math.sin(f * 0.4F + i) * 18.0F) % Math.max(1, this.height);
            Nvg.circle(f2, f3, 0.8F + i % 3 * 0.35F, Theme.alpha(Theme.ACCENT_HOT, 35 + i % 4 * 10));
        }
    }

    private void paintNewsCard(DrawContext g) {
        if (!this.news.isEmpty()) {
            Changelog.Entry changelog$entry = this.news.getFirst();
            boolean flag = ChangelogSeen.hasNew();
            float f = flag ? 268.0F : 238.0F;
            float f1 = flag ? 102.0F : 82.0F;
            float f2 = Math.max(24.0F, this.brandCx - f * 0.5F);
            float f3 = Math.min(this.height - f1 - 72.0F, this.markBottomY + 62.0F);
            this.newsX = f2;
            this.newsY = f3;
            this.newsW = f;
            this.newsH = f1;
            boolean flag1 = Paint.hit((double)this.mx, (double)this.my, f2, f3, f, f1);
            int i = Theme.alpha(593939, flag ? 230 : 188);
            Paint.box(g, f2, f3, f, f1, i, 14.0F);
            Paint.grad(g, f2, f3, f, f1, Theme.alpha(Theme.ACCENT_DEEP, flag ? 140 : 80), Theme.alpha(526862, 0), 14.0F, false);
            Paint.outline(g, f2, f3, f, f1, Theme.alpha(Theme.ACCENT, flag1 ? 180 : (flag ? 120 : 55)), 14.0F);
            if (flag) {
                Paint.box(g, f2 + 12.0F, f3 + 10.0F, 36.0F, 14.0F, Theme.alpha(Theme.ACCENT, 70), 7.0F);
                Paint.text(g, "NEW", f2 + 19.0F, f3 + 13.0F, Theme.ACCENT_HOT, 6.6F);
                Paint.text(g, "v" + changelog$entry.version(), f2 + 56.0F, f3 + 12.0F, Theme.TEXT, 7.4F);
            } else {
                Paint.text(g, "WHAT'S NEW  \u00b7  v" + changelog$entry.version(), f2 + 13.0F, f3 + 11.0F, Theme.ACCENT_HOT, 6.4F);
            }

            Paint.text(g, changelog$entry.title(), f2 + 13.0F, f3 + (flag ? 32 : 29), Theme.TEXT, flag ? 9.2F : 8.5F);
            int j = Math.min(flag ? 3 : 2, changelog$entry.bullets().size());

            for (int k = 0; k < j; k++) {
                String s = changelog$entry.bullets().get(k);
                if (s.length() > 38) {
                    s = s.substring(0, 37) + "\u2026";
                }

                Paint.text(g, "\u00b7 " + s, f2 + 13.0F, f3 + (flag ? 52 : 47) + k * 13, Theme.MUTED, 6.2F);
            }

            this.hits.add(new DooreMenuScreen.Hit(f2, f3, f, f1, () -> this.client.setScreen(new ChangelogScreen(this))));
        }
    }

    private void paintBrandMark(DrawContext g) {
        this.brandCx = this.width * 0.3F;
        this.brandCy = this.height * 0.38F;
        this.brandSize = Math.min(176.0F, Math.max(118.0F, this.height / 3.8F));
        float f = this.brandSize * 0.5F;
        boolean flag = Paint.hit(
            (double)this.mx, (double)this.my, this.brandCx - f * 1.15F, this.brandCy - f * 1.15F, this.brandSize * 1.3F, this.brandSize * 1.3F
        );
        this.brandHover.to(flag ? 1.0F : 0.0F, 10.0F);
        float f1 = (float)System.currentTimeMillis() / 1000.0F;
        float f2;
        float f3;
        if (flag) {
            f2 = MathHelper.clamp((this.mx - this.brandCx) / f, -1.0F, 1.0F) * 58.0F;
            f3 = MathHelper.clamp((this.my - this.brandCy) / f, -1.0F, 1.0F) * 28.0F;
        } else {
            f2 = MathHelper.sin(f1 * 0.85F) * 12.0F;
            f3 = MathHelper.cos(f1 * 0.55F) * 6.0F;
        }

        float f4 = this.brandYaw.to(f2, 7.0F);
        float f5 = this.brandPitch.to(f3, 7.0F);
        float f6 = (float)Math.toRadians(f4);
        float f7 = (float)Math.toRadians(f5);
        float f8 = Math.max(0.22F, (float)Math.cos(f6));
        float f9 = Math.max(0.82F, (float)Math.cos(f7 * 0.85F));
        float f10 = 1.0F + 0.05F * this.brandHover.value();
        float f11 = (float)Math.sin(f6) * this.brandSize * 0.1F;
        float f12 = (float)Math.sin(f7) * this.brandSize * 0.06F;
        this.markBottomY = Math.round(this.brandCy + f * f9 * f10 + 6.0F);
        int i = Math.round(this.brandSize);
        Matrix3x2fStack matrix3x2fstack = g.getMatrices();
        matrix3x2fstack.pushMatrix();
        matrix3x2fstack.translate(this.brandCx + f11, this.brandCy + f12);
        matrix3x2fstack.scale(f8 * f10, f9 * f10);
        matrix3x2fstack.rotate((float)Math.toRadians(f4 * 0.1F));
        matrix3x2fstack.translate(-f, -f);

        try {
            g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.CLIENT_ICON, 0, 0, 0.0F, 0.0F, i, i, i, i);
        } catch (Throwable throwable1) {
            try {
                g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.LOGO, 0, 0, 0.0F, 0.0F, i, i, i, i);
            } catch (Throwable throwable) {
            }
        }

        matrix3x2fstack.popMatrix();
    }

    private void paintBrandText(DrawContext g) {
        float f = this.markBottomY + 14.0F;
        float f1 = this.brandHover.value() * 3.0F;
        Paint.textC(g, "DOORE Visuals", this.brandCx, f - f1, Theme.TEXT, 18.0F);
        Paint.textC(g, "visual client", this.brandCx, f + 22.0F - f1, Theme.alpha(16777215, 200), 10.0F);
        this.paintProfileChip(g, f + 42.0F - f1);
    }

    private void paintProfileChip(DrawContext g, float y) {
        Account.ensure();
        String s = Account.nick();
        String s1 = "#" + Account.uid();
        String s2 = Account.roleLabel();
        float f = 168.0F;
        float f1 = 36.0F;
        float f2 = this.brandCx - f * 0.5F;
        Paint.box(g, f2, y, f, f1, Theme.alpha(593939, 200), 10.0F);
        Paint.outline(g, f2, y, f, f1, Theme.alpha(Theme.ACCENT, 40), 10.0F);
        Image image = Account.avatarImage();
        if (image != null) {
            Nvg.imageCover(image, f2 + 8.0F, y + 6.0F, 24.0F, 24.0F, 12.0F);
        } else {
            Nvg.circle(f2 + 20.0F, y + 18.0F, 10.0F, Theme.alpha(Theme.ACCENT, 50));
        }

        Paint.text(g, s, f2 + 40.0F, y + 7.0F, Theme.TEXT, 7.4F);
        Paint.text(g, s1 + "  \u00b7  " + s2, f2 + 40.0F, y + 20.0F, Theme.MUTED, 5.6F);
    }

    private void paintButton(DrawContext g, DooreMenuScreen.MenuBtn b) {
        boolean flag = Paint.hit((double)this.mx, (double)this.my, b.x, b.y, this.btnW, 44.0F);
        float f = b.hover.spring(flag ? 1.0F : 0.0F, 150.0F, 0.72F, 0.016666668F);
        boolean flag1 = flag || b.primary;
        float f1 = f * 2.0F;
        if (flag1) {
            Paint.box(g, b.x, b.y - f1, this.btnW, 44.0F, Theme.alpha(924184, 230), 12.0F);
            Paint.outline(g, b.x, b.y - f1, this.btnW, 44.0F, Theme.alpha(Theme.ACCENT, flag ? 200 : 120), 12.0F);
            Paint.box(g, b.x, b.y - f1, 3.5F, 44.0F, Theme.ACCENT, 0.0F);
        } else {
            Paint.box(g, b.x, b.y, this.btnW, 44.0F, Theme.alpha(16777215, 10), 12.0F);
            Paint.outline(g, b.x, b.y, this.btnW, 44.0F, Theme.alpha(16777215, 70), 12.0F);
        }

        String s = b.label;
        if (flag || b.primary) {
            s = "[>] " + b.label;
        }

        Paint.textC(g, s, b.x + this.btnW * 0.5F, b.y + 13.0F - (flag1 ? f1 : 0.0F), flag1 ? Theme.ACCENT_HOT : Theme.TEXT, 12.0F);
        this.hits.add(new DooreMenuScreen.Hit(b.x, b.y, this.btnW, 44.0F, b.action));
    }

    private boolean paintIcon(DrawContext g, DooreMenuScreen.IconBtn ic) {
        boolean flag = Paint.hit((double)this.mx, (double)this.my, ic.x, ic.y, 40.0F, 40.0F);
        float f = ic.hover.spring(flag ? 1.0F : 0.0F, 140.0F, 0.75F, 0.016666668F);
        float f1 = 11.0F;
        int i = Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 38 : 12);
        Paint.box(g, ic.x, ic.y, 40.0F, 40.0F, i, f1);
        Paint.outline(g, ic.x, ic.y, 40.0F, 40.0F, Theme.alpha(flag ? Theme.ACCENT : 16777215, flag ? 160 : 50), f1);
        int j = flag ? Theme.ACCENT_HOT : Theme.TEXT;
        float f2 = ic.x + 20.0F;
        float f3 = ic.y + 20.0F;
        drawGlyph(ic.id, f2, f3, j, 1.0F + 0.06F * f);
        if ("news".equals(ic.id) && ChangelogSeen.hasNew()) {
            Nvg.circle(ic.x + 40.0F - 8.0F, ic.y + 8.0F, 3.4F, Theme.ACCENT_HOT);
        }

        this.hits.add(new DooreMenuScreen.Hit(ic.x, ic.y, 40.0F, 40.0F, ic.action));
        return flag;
    }

    private static void drawGlyph(String id, float cx, float cy, int col, float s) {
        switch (id) {
            case "last":
                Nvg.circle(cx, cy, 8.2F * s, Theme.alpha(col, 0));
                Nvg.ring(cx - 8.0F * s, cy - 8.0F * s, 16.0F * s, 16.0F * s, 1.6F, col, 8.0F * s);
                Nvg.line(cx + 1.0F, cy - 7.5F * s, cx + 1.0F, cy + 1.0F, 1.7F, col);
                Nvg.line(cx + 1.0F, cy + 1.0F, cx + 5.5F * s, cy + 1.0F, 1.7F, col);
                break;
            case "alts":
                Nvg.circle(cx, cy - 4.2F * s, 4.2F * s, col);
                Nvg.rect(cx - 7.5F * s, cy + 2.2F * s, 15.0F * s, 8.0F * s, Theme.alpha(col, 200), 5.0F);
                break;
            case "opts":
                Nvg.circle(cx, cy, 4.4F * s, col);

                for (int i = 0; i < 6; i++) {
                    float f = (float)(i * Math.PI / 3.0);
                    float f1 = cx + (float)Math.cos(f) * 6.2F * s;
                    float f2 = cy + (float)Math.sin(f) * 6.2F * s;
                    float f3 = cx + (float)Math.cos(f) * 9.2F * s;
                    float f4 = cy + (float)Math.sin(f) * 9.2F * s;
                    Nvg.line(f1, f2, f3, f4, 2.1F, col);
                }
                break;
            case "news":
                Nvg.rect(cx - 7.0F * s, cy - 8.0F * s, 14.0F * s, 16.0F * s, Theme.alpha(col, 40), 3.0F);
                Nvg.line(cx - 4.5F * s, cy - 3.5F * s, cx + 4.5F * s, cy - 3.5F * s, 1.5F, col);
                Nvg.line(cx - 4.5F * s, cy, cx + 3.2F * s, cy, 1.5F, col);
                Nvg.line(cx - 4.5F * s, cy + 3.5F * s, cx + 1.8F * s, cy + 3.5F * s, 1.5F, col);
                break;
            default:
                Nvg.line(cx - 6.0F * s, cy - 6.0F * s, cx + 6.0F * s, cy + 6.0F * s, 1.8F, col);
                Nvg.line(cx + 6.0F * s, cy - 6.0F * s, cx - 6.0F * s, cy + 6.0F * s, 1.8F, col);
        }
    }

    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubled);
        } else {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                DooreMenuScreen.Hit dooremenuscreen$hit = this.hits.get(i);
                if (Paint.hit(event.comp_4798(), event.comp_4799(), dooremenuscreen$hit.x, dooremenuscreen$hit.y, dooremenuscreen$hit.w, dooremenuscreen$hit.h)
                    )
                 {
                    dooremenuscreen$hit.action.run();
                    return true;
                }
            }

            return super.mouseClicked(event, doubled);
        }
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    private record Hit(float x, float y, float w, float h, Runnable action) {
    }

    private static final class IconBtn {
        final String id;
        final String tip;
        final float x;
        final float y;
        final Runnable action;
        final Anim hover = new Anim(0.0F);

        IconBtn(String id, String tip, Runnable action) {
            this(id, tip, 0.0F, 0.0F, action);
        }

        IconBtn(String id, String tip, float x, float y, Runnable action) {
            this.id = id;
            this.tip = tip;
            this.x = x;
            this.y = y;
            this.action = action;
        }
    }

    private record LastServer(String label, String ip) {
    }

    private static final class MenuBtn {
        final String label;
        final float x;
        final float y;
        final boolean primary;
        final Runnable action;
        final Anim hover = new Anim(0.0F);

        MenuBtn(String label, float x, float y, boolean primary, Runnable action) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.primary = primary;
            this.action = action;
        }
    }
}
