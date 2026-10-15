package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;

/**
 * Main Menu: the settings of the client's main menu and of its loading screen. The menu reads the values live, so a change
 * is visible the next time it is drawn.
 */
public final class MainMenuFeature extends Feature {
    public static final String BG_GRADIENT = "Градиент";
    public static final String BG_IMAGE = "Картинка";
    public static final String BG_PANORAMA = "Панорама";
    public static final String BG_VIDEO = "Видео";

    private static MainMenuFeature self;

    private final Opt.Pick background = this.opt(new Opt.Pick("background", "Фон меню", BG_GRADIENT, BG_GRADIENT, BG_IMAGE, BG_PANORAMA, BG_VIDEO).chips());
    private final Opt.Num dim = this.opt(new Opt.Num("dim", "Затемнение фона", 0.45, 0.0, 0.85, 0.05));
    private final Opt.Flag logo = this.opt(new Opt.Flag("logo", "Логотип", true));
    private final Opt.Text logoFile = this.opt(new Opt.Text("logo_file", "Свой логотип (путь к PNG, из буфера)", "").visibleWhen(() -> this.logo.get()));
    private final Opt.Flag profile = this.opt(new Opt.Flag("profile", "Баннер профиля", true));
    private final Opt.Text profileText = this.opt(new Opt.Text("profile_text", "Текст профиля (из буфера)", "").visibleWhen(() -> this.profile.get()));
    private final Opt.Text profileImage = this.opt(new Opt.Text("profile_image", "Картинка профиля (путь к PNG, из буфера)", "").visibleWhen(() -> this.profile.get()));
    private final Opt.Flag news = this.opt(new Opt.Flag("news", "Что нового", true));
    private final Opt.Flag player = this.opt(new Opt.Flag("player", "Плеер", true));
    private final Opt.Flag farewell = this.opt(new Opt.Flag("farewell", "Прощание при выходе", true));
    private final Opt.Flag loading = this.opt(new Opt.Flag("loading", "Свой экран загрузки", true));

    public MainMenuFeature() {
        super(
                "main_menu",
                "Main Menu",
                "Фон, логотип, профиль, новости, плеер главного меню и экран загрузки",
                Category.SYSTEM,
                true
        );
        this.badge(Badge.NEW);
        self = this;
    }

    private static MainMenuFeature active() {
        MainMenuFeature f = self;
        return f != null && f.on() ? f : null;
    }

    public static String background() {
        MainMenuFeature f = active();
        return f == null ? BG_GRADIENT : f.background.get();
    }

    public static float dim() {
        MainMenuFeature f = active();
        return f == null ? 0.45F : f.dim.f();
    }

    public static boolean logo() {
        MainMenuFeature f = active();
        return f == null || f.logo.get();
    }

    public static String logoFile() {
        MainMenuFeature f = active();
        return f == null ? "" : f.logoFile.get();
    }

    public static boolean profile() {
        MainMenuFeature f = active();
        return f != null && f.profile.get();
    }

    public static String profileText() {
        MainMenuFeature f = active();
        return f == null ? "" : f.profileText.get();
    }

    public static String profileImage() {
        MainMenuFeature f = active();
        return f == null ? "" : f.profileImage.get();
    }

    public static boolean news() {
        MainMenuFeature f = active();
        return f != null && f.news.get();
    }

    public static boolean player() {
        MainMenuFeature f = active();
        return f != null && f.player.get();
    }

    public static boolean farewell() {
        MainMenuFeature f = active();
        return f != null && f.farewell.get();
    }

    /** Whether the client's loading screen replaces the game's (read at start, so it uses the saved setting). */
    public static boolean loadingScreen() {
        MainMenuFeature f = self;
        return f == null || (f.on() && f.loading.get());
    }

    public static boolean live() {
        return App.live();
    }
}
