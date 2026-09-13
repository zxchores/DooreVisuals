package dev.doorevisuals.ui;

import dev.doorevisuals.DooreClient;
import dev.doorevisuals.data.Account;
import dev.doorevisuals.data.AltsStore;
import dev.doorevisuals.data.Changelog;
import dev.doorevisuals.data.ChangelogSeen;
import dev.doorevisuals.data.FirstRun;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import java.util.List;
import net.minecraft.client.MinecraftClient;
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
        int i = Math.min(280, Math.max(220, this.width * 32 / 100));
        int j = (this.width - i) / 2;
        int k = Math.round(this.height * 0.46F);
        this.addDrawableChild(new DooreMenuScreen.MenuButton(j, k, i, 28, Text.literal("Одиночная игра"), true, () -> this.client.setScreen(new SelectWorldScreen(this))));
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(j, k + 34, i, 28, Text.literal("Мультиплеер"), false, () -> this.client.setScreen(new MultiplayerScreen(this)))
        );
        DooreMenuScreen.LastServer dooremenuscreen$lastserver = this.resolveLastServer();
        int l = k + 68;
        if (dooremenuscreen$lastserver != null) {
            this.addDrawableChild(
                new DooreMenuScreen.MenuButton(j, l, i, 28, Text.literal(dooremenuscreen$lastserver.label), false, () -> this.connectLast(dooremenuscreen$lastserver))
            );
            l += 34;
        }

        int i1 = (i - 8) / 2;
        this.addDrawableChild(new DooreMenuScreen.MenuButton(j, l, i1, 26, Text.literal("Аккаунты"), false, () -> this.client.setScreen(new AltManagerScreen(this))));
        this.addDrawableChild(
            new DooreMenuScreen.MenuButton(
                j + i1 + 8, l, i1, 26, Text.literal("Настройки"), false, () -> this.client.setScreen(new OptionsScreen(this, this.client.options))
            )
        );
        this.addDrawableChild(new DooreMenuScreen.MenuButton(j, l + 32, i, 26, Text.literal("Выход"), false, () -> this.client.scheduleStop()));
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        MenuAtmosphere.paintBackground(g, this.width, this.height);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.paintBrand(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void paintBrand(DrawContext g) {
        float f = this.width * 0.5F;
        float f1 = Math.max(56.0F, this.height * 0.16F);
        int i = Math.round(Math.min(84.0F, Math.max(56.0F, this.height * 0.1F)));

        try {
            g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.CLIENT_ICON, Math.round(f) - i / 2, Math.round(f1) - i / 2, 0.0F, 0.0F, i, i, i, i);
        } catch (Throwable throwable) {
            try {
                g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.LOGO, Math.round(f) - i / 2, Math.round(f1) - i / 2, 0.0F, 0.0F, i, i, i, i);
            } catch (Throwable ignored) {
            }
        }

        g.drawTextWithShadow(this.textRenderer, "DOORE Visuals", Math.round(f) - this.textRenderer.getWidth("DOORE Visuals") / 2, Math.round(f1 + i * 0.55F + 6.0F), Theme.TEXT);
        Account.ensure();
        String s = Account.nick();
        String s1 = (s == null || s.isBlank() ? "visual client" : s) + "  ·  v" + DooreClient.VERSION;
        g.drawTextWithShadow(this.textRenderer, s1, Math.round(f) - this.textRenderer.getWidth(s1) / 2, Math.round(f1 + i * 0.55F + 20.0F), Theme.MUTED);
        if (!this.news.isEmpty() && ChangelogSeen.hasNew()) {
            Changelog.Entry changelog$entry = this.news.getFirst();
            String s2 = "NEW  ·  v" + changelog$entry.version() + "  ·  " + changelog$entry.title();
            int j = Math.min(340, Math.max(160, this.textRenderer.getWidth(s2) + 20));
            int k = Math.round(f) - j / 2;
            int l = Math.round(f1 + i * 0.55F + 36.0F);
            Paint.box(g, k, l, j, 16.0F, Theme.alpha(Theme.ACCENT, 50), 6.0F);
            g.drawTextWithShadow(this.textRenderer, s2, Math.round(f) - this.textRenderer.getWidth(s2) / 2, l + 4, Theme.ACCENT_HOT);
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

    private static final class MenuButton extends ClickableWidget {
        private final boolean primary;
        private final Runnable action;

        MenuButton(int x, int y, int w, int h, Text label, boolean primary, Runnable action) {
            super(x, y, w, h, label);
            this.primary = primary;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
            boolean flag = this.isHovered();
            int i = this.getX();
            int j = this.getY();
            int k = this.getWidth();
            int l = this.getHeight();
            int i1 = Theme.alpha(this.primary || flag ? Theme.ACCENT : 394758, this.primary || flag ? 210 : 200);
            Paint.box(g, i, j, k, l, i1, 6.0F);
            Paint.outline(g, i, j, k, l, Theme.alpha(this.primary || flag ? Theme.ACCENT_HOT : 16777215, flag ? 200 : 70), 6.0F);
            if (this.primary) {
                Paint.box(g, i, j, 3.0F, l, Theme.ACCENT_HOT, 0.0F);
            }

            int j1 = this.primary || flag ? Theme.TEXT : -1;
            int k1 = MinecraftClient.getInstance().textRenderer.getWidth(this.getMessage());
            g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, this.getMessage(), i + (k - k1) / 2, j + (l - 8) / 2, j1);
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
