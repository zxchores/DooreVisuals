package dev.doorevisuals.ui;

import dev.doorevisuals.DooreClient;
import dev.doorevisuals.data.Account;
import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.data.Changelog;
import dev.doorevisuals.data.ChangelogSeen;
import dev.doorevisuals.data.FirstRun;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;

public final class DooreMenuScreen extends Screen {
    private final List<Changelog.Entry> news = Changelog.load();
    private int stackX;
    private int stackY;
    private int stackW;
    private int stackH;

    public DooreMenuScreen() {
        super(Text.literal("DooreVisuals"));
    }

    @Override
    protected void init() {
        super.init();
        AltsStore.boot();
        if (FirstRun.needsWizard() && this.client != null) {
            this.client.setScreen(new FirstRunScreen(this));
        } else {
            this.buildButtons();
        }
    }

    private void buildButtons() {
        int i = Math.min(252, Math.max(196, this.width - 56));
        int j = this.height < 270 ? 20 : 26;
        int k = this.height < 270 ? 5 : 8;
        DooreMenuScreen.LastServer dooremenuscreen$lastserver = this.resolveLastServer();
        int l = 4 + (dooremenuscreen$lastserver != null ? 1 : 0);
        this.stackW = i;
        this.stackH = l * j + (l - 1) * k + 18;
        this.stackX = (this.width - i) / 2;
        this.stackY = Math.max(this.height / 2 - 4, this.height - this.stackH - Math.max(16, this.height / 16));
        int i1 = this.stackY + 9;
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(
                this.stackX, i1, i, j, Text.literal("Одиночная игра"), DooreMenuScreen.Kind.PRIMARY, () -> this.client.setScreen(new SelectWorldScreen(this))
            )
        );
        i1 += j + k;
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(
                this.stackX, i1, i, j, Text.literal("Мультиплеер"), DooreMenuScreen.Kind.NORMAL, () -> this.client.setScreen(new MultiplayerScreen(this))
            )
        );
        i1 += j + k;
        if (dooremenuscreen$lastserver != null) {
            this.addDrawableChild(
                new DooreMenuScreen.MenuButton(
                    this.stackX, i1, i, j, Text.literal(dooremenuscreen$lastserver.label), DooreMenuScreen.Kind.NORMAL, () -> this.connectLast(dooremenuscreen$lastserver)
                )
            );
            i1 += j + k;
        }

        int j1 = (i - 8) / 2;
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(
                this.stackX, i1, j1, j, Text.literal("Аккаунты"), DooreMenuScreen.Kind.NORMAL, () -> this.client.setScreen(new AltManagerScreen(this))
            )
        );
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(
                this.stackX + j1 + 8,
                i1,
                j1,
                j,
                Text.literal("Настройки"),
                DooreMenuScreen.Kind.NORMAL,
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))
            )
        );
        i1 += j + k;
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(this.stackX, i1, i, j, Text.literal("Выход"), DooreMenuScreen.Kind.GHOST, () -> this.client.scheduleStop())
        );
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        MenuAtmosphere.paintBackground(g, this.width, this.height);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.paintBrand(g);
        if (this.stackW > 0) {
            Paint.box(g, this.stackX - 10, this.stackY, this.stackW + 20, this.stackH, Theme.alpha(10, 168), 14.0F);
            Paint.outline(g, this.stackX - 10, this.stackY, this.stackW + 20, this.stackH, Theme.alpha(Theme.ACCENT, 55), 14.0F);
            Paint.box(g, this.stackX - 10, this.stackY, this.stackW + 20, 1.2F, Theme.alpha(16777215, 28), 0.0F);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void paintBrand(DrawContext g) {
        float f = this.width * 0.5F;
        int i = this.stackY > 0 ? this.stackY : this.height / 2;
        int j = Math.round(Math.min(72.0F, Math.max(32.0F, i * 0.28F)));
        if (this.height < 260) {
            j = 28;
        }

        float f1 = Math.max(22.0F, i * 0.38F);
        try {
            g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.CLIENT_ICON, Math.round(f) - j / 2, Math.round(f1) - j / 2, 0.0F, 0.0F, j, j, j, j);
        } catch (Throwable throwable) {
            try {
                g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.LOGO, Math.round(f) - j / 2, Math.round(f1) - j / 2, 0.0F, 0.0F, j, j, j, j);
            } catch (Throwable ignored) {
            }
        }

        int k = Math.round(f1 + j * 0.52F + 8.0F);
        this.shadowText(g, "DOORE Visuals", Math.round(f), k, Theme.TEXT);
        Account.ensure();
        String s = Account.nick();
        String s1 = (s == null || s.isBlank() ? "visual client" : s) + "  ·  v" + DooreClient.VERSION;
        this.shadowText(g, s1, Math.round(f), k + 12, Theme.MUTED);
        if (this.height >= 300 && !this.news.isEmpty() && ChangelogSeen.hasNew() && k + 40 < this.stackY - 8) {
            Changelog.Entry changelog$entry = this.news.getFirst();
            String s2 = "NEW  ·  v" + changelog$entry.version();
            int l = this.textRenderer.getWidth(s2) + 16;
            int i1 = Math.round(f) - l / 2;
            int j1 = k + 28;
            Paint.box(g, i1, j1, l, 14.0F, Theme.alpha(Theme.ACCENT, 70), 7.0F);
            Paint.outline(g, i1, j1, l, 14.0F, Theme.alpha(Theme.ACCENT_HOT, 90), 7.0F);
            this.shadowText(g, s2, Math.round(f), j1 + 3, Theme.ACCENT_HOT);
        }
    }

    private void shadowText(DrawContext g, String s, int cx, int y, int color) {
        g.drawTextWithShadow(this.textRenderer, s, cx - this.textRenderer.getWidth(s) / 2, y, color);
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
                    }
                } catch (Throwable throwable) {
                }

                return new DooreMenuScreen.LastServer(s1, s);
            }
        }

        return null;
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

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private enum Kind {
        PRIMARY,
        NORMAL,
        GHOST
    }

    private static final class MenuButton extends ClickableWidget {
        private final DooreMenuScreen.Kind kind;
        private final Runnable action;
        private final Anim hover = new Anim(0.0F);

        MenuButton(int x, int y, int w, int h, Text label, DooreMenuScreen.Kind kind, Runnable action) {
            super(x, y, w, h, label);
            this.kind = kind;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
            float f = this.hover.springHover(this.isHovered() ? 1.0F : 0.0F, 0.016666668F);
            int i = this.getX();
            int j = this.getY();
            int k = this.getWidth();
            int l = this.getHeight();
            boolean flag = this.kind == DooreMenuScreen.Kind.PRIMARY;
            boolean flag1 = this.kind == DooreMenuScreen.Kind.GHOST;
            int i1;
            int j1;
            if (flag) {
                i1 = Theme.lerp(Theme.alpha(Theme.ACCENT, 200), Theme.alpha(Theme.ACCENT_HOT, 230), f);
                j1 = Theme.alpha(Theme.ACCENT_HOT, 140 + Math.round(80.0F * f));
            } else if (flag1) {
                i1 = Theme.alpha(16777215, 10 + Math.round(18.0F * f));
                j1 = Theme.alpha(16777215, 40 + Math.round(50.0F * f));
            } else {
                i1 = Theme.lerp(Theme.alpha(16777215, 14), Theme.alpha(Theme.ACCENT, 70), f);
                j1 = Theme.alpha(flag ? Theme.ACCENT : 16777215, 50 + Math.round(110.0F * f));
            }

            Paint.box(g, i, j, k, l, i1, 8.0F);
            Paint.outline(g, i, j, k, l, j1, 8.0F);
            Paint.box(g, i + 8, j + 1, k - 16, 1.0F, Theme.alpha(16777215, 18 + Math.round(22.0F * f)), 0.0F);
            float f1 = 2.4F + 2.2F * (flag ? 1.0F : f);
            Paint.box(g, i, j + 3, f1, l - 6, Theme.alpha(flag || f > 0.2F ? Theme.ACCENT_HOT : Theme.ACCENT, 180 + Math.round(60.0F * f)), 1.2F);
            TextRenderer textrenderer = MinecraftClient.getInstance().textRenderer;
            int k1 = flag || f > 0.45F ? Theme.TEXT : (flag1 ? Theme.MUTED : -1);
            String s = this.getMessage().getString();
            if (flag || f > 0.55F) {
                s = "›  " + s;
            }

            int l1 = textrenderer.getWidth(s);
            g.drawTextWithShadow(textrenderer, s, i + (k - l1) / 2, j + (l - 8) / 2, k1);
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            this.action.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }
    }

    private record LastServer(String label, String ip) {
    }
}
