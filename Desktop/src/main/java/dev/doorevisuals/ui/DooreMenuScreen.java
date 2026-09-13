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

public final class DooreMenuScreen extends Screen {
    private final List<DooreMenuScreen.Hit> hits = new ArrayList<>();
    private final List<Changelog.Entry> news = Changelog.load();
    private int mx;
    private int my;

    public DooreMenuScreen() {
        super(Text.literal("DooreVisuals"));
    }

    @Override
    protected void init() {
        super.init();
        AltsStore.boot();
        if (FirstRun.needsWizard() && this.client != null) {
            this.client.setScreen(new FirstRunScreen(this));
        }
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        MenuAtmosphere.paintBackground(g, this.width, this.height);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        this.mx = mouseX;
        this.my = mouseY;
        this.hits.clear();
        this.renderBackground(g, mouseX, mouseY, partialTick);
        this.paintBrand(g);
        this.paintButtons(g);
        this.paintFooter(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void paintBrand(DrawContext g) {
        float f = this.width * 0.5F;
        float f1 = this.height * 0.22F;
        int i = Math.round(Math.min(96.0F, Math.max(64.0F, this.height * 0.12F)));

        try {
            g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.CLIENT_ICON, Math.round(f) - i / 2, Math.round(f1) - i / 2, 0.0F, 0.0F, i, i, i, i);
        } catch (Throwable throwable) {
            try {
                g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.LOGO, Math.round(f) - i / 2, Math.round(f1) - i / 2, 0.0F, 0.0F, i, i, i, i);
            } catch (Throwable ignored) {
            }
        }

        Paint.textC(g, "DOORE Visuals", f, f1 + i * 0.55F + 8.0F, Theme.TEXT, 18.0F);
        Account.ensure();
        String s = Account.nick();
        Paint.textC(
            g,
            (s == null || s.isBlank() ? "visual client" : s) + "  ·  v" + DooreClient.VERSION,
            f,
            f1 + i * 0.55F + 30.0F,
            Theme.MUTED,
            9.0F
        );
        if (!this.news.isEmpty() && ChangelogSeen.hasNew()) {
            Changelog.Entry changelog$entry = this.news.getFirst();
            String s1 = "NEW  ·  v" + changelog$entry.version() + "  ·  " + changelog$entry.title();
            float f2 = Math.min(320.0F, Math.max(180.0F, Paint.tw(s1, 7.5F) + 28.0F));
            float f3 = f - f2 * 0.5F;
            float f4 = f1 + i * 0.55F + 48.0F;
            Paint.box(g, f3, f4, f2, 20.0F, Theme.alpha(Theme.ACCENT, 40), 8.0F);
            Paint.textC(g, s1, f, f4 + 5.0F, Theme.ACCENT_HOT, 7.5F);
            this.hits.add(new DooreMenuScreen.Hit(f3, f4, f2, 20.0F, () -> this.client.setScreen(new ChangelogScreen(this))));
        }
    }

    private void paintButtons(DrawContext g) {
        float f = Math.min(280.0F, Math.max(220.0F, this.width * 0.32F));
        float f1 = (this.width - f) * 0.5F;
        float f2 = this.height * 0.48F;
        this.button(g, f1, f2, f, 36.0F, "Одиночная игра", true, () -> this.client.setScreen(new SelectWorldScreen(this)));
        this.button(g, f1, f2 + 44.0F, f, 36.0F, "Мультиплеер", false, () -> this.client.setScreen(new MultiplayerScreen(this)));
        DooreMenuScreen.LastServer dooremenuscreen$lastserver = this.resolveLastServer();
        float f3 = f2 + 88.0F;
        if (dooremenuscreen$lastserver != null) {
            this.button(g, f1, f3, f, 36.0F, dooremenuscreen$lastserver.label, false, () -> this.connectLast(dooremenuscreen$lastserver));
            f3 += 44.0F;
        }

        float f4 = (f - 12.0F) / 2.0F;
        this.button(g, f1, f3, f4, 32.0F, "Аккаунты", false, () -> this.client.setScreen(new AltManagerScreen(this)));
        this.button(g, f1 + f4 + 12.0F, f3, f4, 32.0F, "Настройки", false, () -> this.client.setScreen(new OptionsScreen(this, this.client.options)));
        this.button(g, f1, f3 + 40.0F, f, 32.0F, "Выход", false, () -> this.client.scheduleStop());
    }

    private void paintFooter(DrawContext g) {
        if (!this.news.isEmpty() && !ChangelogSeen.hasNew()) {
            Changelog.Entry changelog$entry = this.news.getFirst();
            Paint.textC(g, "v" + changelog$entry.version() + "  ·  " + changelog$entry.title(), this.width * 0.5F, this.height - 22.0F, Theme.GHOST, 7.0F);
        }
    }

    private void button(DrawContext g, float x, float y, float w, float h, String label, boolean primary, Runnable action) {
        boolean flag = Paint.hit((double)this.mx, (double)this.my, x, y, w, h);
        int i = Theme.alpha(primary || flag ? Theme.ACCENT : 16777215, primary || flag ? 70 : 16);
        Paint.box(g, x, y, w, h, i, 8.0F);
        Paint.outline(g, x, y, w, h, Theme.alpha(primary || flag ? Theme.ACCENT : 16777215, flag ? 180 : 50), 8.0F);
        if (primary) {
            Paint.box(g, x, y, 3.0F, h, Theme.ACCENT, 0.0F);
        }

        Paint.textC(g, label, x + w * 0.5F, y + h * 0.32F, flag || primary ? Theme.ACCENT_HOT : Theme.TEXT, 11.0F);
        this.hits.add(new DooreMenuScreen.Hit(x, y, w, h, action));
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
    public boolean mouseClicked(Click event, boolean doubled) {
        if (event.button() == 0) {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                DooreMenuScreen.Hit dooremenuscreen$hit = this.hits.get(i);
                if (Paint.hit(event.x(), event.y(), dooremenuscreen$hit.x, dooremenuscreen$hit.y, dooremenuscreen$hit.w, dooremenuscreen$hit.h)) {
                    dooremenuscreen$hit.action.run();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private record Hit(float x, float y, float w, float h, Runnable action) {
    }

    private record LastServer(String label, String ip) {
    }
}
