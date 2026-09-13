package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.discord.DiscordRpc;

public final class DiscordFeature extends Feature implements DiscordRpc.Settings {
    public static final long APP_ID = 1528451427788128439L;
    private final Opt.Flag elapsed = this.opt(new Opt.Flag("elapsed", "\u0412\u0440\u0435\u043c\u044f \u0441\u0435\u0441\u0441\u0438\u0438", true));
    private final Opt.Flag server = this.opt(
        new Opt.Flag("server", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0441\u0435\u0440\u0432\u0435\u0440", true)
    );
    private final Opt.Flag world = this.opt(new Opt.Flag("world", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u043c\u0438\u0440", true));
    private final Opt.Flag fps = this.opt(new Opt.Flag("fps", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c FPS", true));
    private final Opt.Flag streamer = this.opt(new Opt.Flag("streamer", "\u0421\u0442\u0440\u0438\u043c\u0435\u0440-\u0440\u0435\u0436\u0438\u043c", false));
    private final Opt.Flag useLogo = this.opt(new Opt.Flag("use_logo", "\u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0430 logo", true));
    private final Opt.Pick artPreset = this.opt(
        new Opt.Pick("art_preset", "\u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0430", "logo", "logo", "logo_anim", "\u0421\u0432\u043e\u0439")
    );
    private final Opt.Text artKey = this.opt(
        new Opt.Text("art_key", "Art Asset \u043a\u043b\u044e\u0447", "logo").visibleWhen(() -> "\u0421\u0432\u043e\u0439".equals(this.artPreset.get()))
    );
    private DiscordRpc rpc;

    public DiscordFeature() {
        super(
            "discord_rpc",
            "Discord",
            "\u0418\u0433\u0440\u043e\u0432\u0430\u044f \u0430\u043a\u0442\u0438\u0432\u043d\u043e\u0441\u0442\u044c \u0432 Discord",
            Category.SYSTEM,
            true
        );
    }

    public void bind(DiscordRpc rpc) {
        this.rpc = rpc;
    }

    @Override
    protected void enable() {
        if (this.rpc != null) {
            this.rpc.forceRefresh();
        }
    }

    @Override
    protected void disable() {
        if (this.rpc != null) {
            this.rpc.forceRefresh();
        }
    }

    @Override
    public String about() {
        String s = super.about();
        return this.rpc == null ? s : s + (this.rpc.connected() ? " \u00b7 \u2713" : " \u00b7 " + this.rpc.status());
    }

    @Override
    public boolean enabled() {
        return this.on();
    }

    @Override
    public long appId() {
        return 1528451427788128439L;
    }

    @Override
    public boolean showElapsed() {
        return (Boolean)this.elapsed.get();
    }

    @Override
    public boolean showServer() {
        return (Boolean)this.server.get();
    }

    @Override
    public boolean showWorld() {
        return (Boolean)this.world.get();
    }

    @Override
    public boolean showFps() {
        return (Boolean)this.fps.get();
    }

    @Override
    public boolean streamerSafe() {
        return (Boolean)this.streamer.get();
    }

    @Override
    public boolean useLogo() {
        return (Boolean)this.useLogo.get();
    }

    @Override
    public String artKey() {
        if (!"\u0421\u0432\u043e\u0439".equals(this.artPreset.get())) {
            return (String)this.artPreset.get();
        } else {
            String s = (String)this.artKey.get();
            return s != null && !s.isBlank() ? s.trim() : "logo";
        }
    }
}
