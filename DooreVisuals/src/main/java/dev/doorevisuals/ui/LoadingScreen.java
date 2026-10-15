package dev.doorevisuals.ui;

import dev.doorevisuals.draw.SmoothText;
import dev.doorevisuals.tools.MainMenuFeature;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The client's loading screen, drawn over the game's while the resources load: a dark gradient, the logo, a progress bar and a
 * tip that changes every few seconds. It uses only plain quads and textures (no client shaders, which may not be loaded yet),
 * and reads its logo straight from the mod's jar, because the resource manager is empty during the first load.
 */
public final class LoadingScreen {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Loading");
    private static final Identifier LOGO_ID = Identifier.of("doorevisuals", "dyn/splash_logo");
    private static final String[] TIPS = {
        "Right Shift открывает меню клиента",
        "Правый клик по модулю открывает его настройки",
        ".cfg save имя сохраняет конфиг, .cfg diff a b сравнивает два",
        "Liquid Glass меняет все панели на жидкое стекло",
        "Профиль Макс fps отключает самые тяжёлые эффекты",
        "Колесо мыши по колонке в меню прокручивает модули",
        "Перетащите картинку или видео в окно меню, чтобы сменить баннер",
        "В Emotes у каждого жеста своя клавиша",
        "Тема из меню копируется кодом DVT1 и вставляется у друга",
    };

    private static boolean logoTried;
    private static boolean logoReady;
    private static float shown;
    private static boolean failed;

    private LoadingScreen() {
    }

    /** Called at the end of the game's own splash drawing; progress is 0..1, completeTime is when loading finished (-1 before). */
    public static void draw(DrawContext g, float progress, long completeTime) {
        if (failed || !MainMenuFeature.loadingScreen()) {
            return;
        }
        try {
            long now = Util.getMeasuringTimeMs();
            float since = completeTime > -1L ? (now - completeTime) / 1000.0F : -1.0F;
            // The game keeps its splash a second after loading and then fades it out over a second: the same here.
            float a = since >= 1.0F ? 1.0F - Math.min(1.0F, since - 1.0F) : 1.0F;
            if (a <= 0.01F) {
                return;
            }
            int w = g.getScaledWindowWidth();
            int h = g.getScaledWindowHeight();
            int alpha = Math.round(255.0F * a);
            g.fillGradient(0, 0, w, h, argb(alpha, 0x07070C), argb(alpha, 0x17142A));

            shown += (Math.max(progress, completeTime > -1L ? 1.0F : 0.0F) - shown) * 0.08F;
            float t = now / 1000.0F;
            int size = Math.max(48, Math.min(96, h / 8));
            int cx = w / 2;
            int cy = h / 2 - size / 2 - 14;
            ensureLogo();
            if (logoReady) {
                float pulse = 0.88F + 0.12F * (float)Math.sin(t * 2.4F);
                g.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO_ID, cx - size / 2, cy - size / 2, 0.0F, 0.0F, size, size, size, size, argb(Math.round(alpha * pulse), 0xFFFFFF));
            }
            SmoothText.drawCentered(g, "DooreVisuals", cx, cy + size / 2.0F + 12.0F, argb(alpha, 0xFFFFFF), 15.0F);

            // Progress bar: a thin rounded track and a fill in the accent colour.
            int bw = Math.min(260, w - 60);
            int bx = cx - bw / 2;
            int by = cy + size / 2 + 48;
            g.fill(bx, by, bx + bw, by + 4, argb(Math.round(alpha * 0.18F), 0xFFFFFF));
            int fill = Math.round(bw * Math.max(0.02F, Math.min(1.0F, shown)));
            g.fill(bx, by, bx + fill, by + 4, argb(alpha, 0x34D399));
            g.fill(bx, by, bx + fill, by + 1, argb(Math.round(alpha * 0.6F), 0xFFFFFF));
            SmoothText.drawCentered(g, Math.round(shown * 100.0F) + "%", cx, by + 10.0F, argb(Math.round(alpha * 0.7F), 0xC8C6D4), 9.0F);

            String tip = TIPS[(int)((now / 3500L) % TIPS.length)];
            SmoothText.drawCentered(g, tip, cx, h - 36.0F, argb(Math.round(alpha * 0.65F), 0x9A96AA), 10.0F);
        } catch (Throwable error) {
            // The loading screen must never be what stops the game from starting: on any error it just stops drawing.
            failed = true;
            LOG.warn("Loading screen disabled after an error: {}", error.toString());
        }
    }

    private static void ensureLogo() {
        if (logoTried) {
            return;
        }
        logoTried = true;
        try (InputStream in = LoadingScreen.class.getResourceAsStream("/assets/doorevisuals/textures/gui/client_icon.png")) {
            if (in != null) {
                NativeImage image = NativeImage.read(in);
                MinecraftClient.getInstance().getTextureManager().registerTexture(LOGO_ID, new NativeImageBackedTexture(() -> "doorevisuals splash logo", image));
                logoReady = true;
            }
        } catch (Throwable error) {
            LOG.warn("Splash logo not loaded: {}", error.toString());
        }
    }

    private static int argb(int alpha, int rgb) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }
}
