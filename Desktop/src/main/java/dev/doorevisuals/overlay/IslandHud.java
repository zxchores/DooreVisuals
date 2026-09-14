package dev.doorevisuals.overlay;

import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.media.CoverArt;
import dev.doorevisuals.media.MediaHub;
import dev.doorevisuals.server.FunHelperFeature;
import java.util.ArrayDeque;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

public final class IslandHud {
    private static final long EVENT_MS = 2500L;
    private static final int QUEUE_CAP = 6;
    private static final int BANDS = 4;
    private final Anim reveal = new Anim(0.0F);
    private final Anim expand = new Anim(0.0F);
    private final Anim[] bands = bands();
    private final ArrayDeque<IslandHud.Event> queue = new ArrayDeque<>();
    private boolean pinned;
    private boolean hitsReady;
    private float bodyX;
    private float bodyY;
    private float bodyW;
    private float bodyH;
    private float prevX;
    private float prevY;
    private float nextX;
    private float nextY;
    private float playX;
    private float playY;
    private float btnW;
    private float btnH;
    private float barX;
    private float barY;
    private float barW;
    private float barH;
    private String lastTrackKey = "";
    private String lastTrap = "";

    public void push(String title, String body) {
        String s = title == null ? "" : title;
        String s1 = body == null ? "" : body;
        if (!s.isBlank() || !s1.isBlank()) {
            while (this.queue.size() >= 6) {
                this.queue.removeFirst();
            }

            this.queue.addLast(new IslandHud.Event(s, s1, System.currentTimeMillis() + 2500L));
        }
    }

    public boolean click(double mx, double my, boolean islandOn) {
        if (islandOn && this.hitsReady) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.currentScreen != null && !(minecraftclient.currentScreen instanceof ChatScreen)) {
                return false;
            } else {
                boolean flag = this.expand.value() > 0.55F;
                if (flag) {
                    if (hit(mx, my, this.prevX, this.prevY, this.btnW, this.btnH)) {
                        return MediaHub.get().prevTrack();
                    }

                    if (hit(mx, my, this.nextX, this.nextY, this.btnW, this.btnH)) {
                        return MediaHub.get().nextTrack();
                    }

                    if (hit(mx, my, this.playX, this.playY, this.btnW, this.btnH)) {
                        return MediaHub.get().togglePlay();
                    }

                    if (hit(mx, my, this.barX, this.barY, this.barW, this.barH) && this.barW > 4.0F) {
                        return MediaHub.get().seek((mx - this.barX) / this.barW);
                    }
                }

                if (hit(mx, my, this.bodyX, this.bodyY, this.bodyW, this.bodyH)) {
                    this.pinned = !this.pinned;
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean scroll(double mx, double my, double dy, boolean islandOn) {
        if (islandOn && this.hitsReady && !(this.expand.value() < 0.55F)) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.currentScreen != null && !(minecraftclient.currentScreen instanceof ChatScreen)) {
                return false;
            } else {
                return hit(mx, my, this.bodyX, this.bodyY, this.bodyW, this.bodyH) ? MediaHub.get().nudgeVolume(dy) : false;
            }
        } else {
            return false;
        }
    }

    public void paint(DrawContext g, HudFeature hud, boolean editor, float dt) {
        HudSlot hudslot = hud.slot("island");
        if (hudslot == null) {
            this.hitsReady = false;
        } else {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            this.prune();
            this.pulseMedia();
            String s = FunHelperFeature.trapText();
            boolean flag = s != null && !s.isBlank();
            if (flag && !s.equals(this.lastTrap)) {
                this.push("FunTime", s);
            }

            this.lastTrap = flag ? s : "";
            IslandHud.Event islandhud$event = this.queue.peekFirst();
            boolean flag1 = islandhud$event != null;
            MediaHub.Snap mediahub$snap = MediaHub.get().snapshot();
            boolean flag2 = mediahub$snap.available();
            boolean flag3 = flag2 || flag1 || flag || editor || this.pinned;
            this.hitsReady = false;
            this.driveBands(flag2 && mediahub$snap.playing(), (float)mediahub$snap.progress(), dt);
            float f = this.reveal.spring(flag3 ? 1.0F : 0.0F, 190.0F, 28.0F, dt);
            if (!flag3) {
                this.expand.spring(0.0F, 165.0F, 28.0F, dt);
            }

            if (f < 0.03F && !flag3) {
                hudslot.size(188.0F, 28.0F);
            } else {
                float f1 = guiMouseX(minecraftclient);
                float f2 = guiMouseY(minecraftclient);
                float f3 = 26.0F;
                float f4 = 168.0F;
                float f5 = 76.0F;
                float f6 = 228.0F;
                float f7 = Math.max(0.01F, hudslot.scale());
                hudslot.size(f6, f5);
                float[] afloat = hud.slotScreen(hudslot, f6, f5);
                float f8 = afloat[1];
                float f9 = Math.abs(hudslot.x()) < 0.01F ? (minecraftclient.getWindow().getScaledWidth() - f4 * f7) * 0.5F : afloat[0];
                boolean flag4 = (editor || minecraftclient.currentScreen == null) && hit(f1, f2, f9, f8, f4 * f7, f3 * f7);
                float f10 = this.expand.spring(!flag4 && !flag1 && !flag && !this.pinned ? 0.0F : 1.0F, 165.0F, 28.0F, dt);
                float f11 = Anim.easeInOut(f10);
                float f12 = f3 + (f5 - f3) * f11;
                float f13 = f4 + (f6 - f4) * f11;
                hudslot.size(f13, f12);
                float f14 = Math.abs(hudslot.x()) < 0.01F
                    ? (minecraftclient.getWindow().getScaledWidth() - f13 * f7) * 0.5F
                    : hud.slotScreen(hudslot, f13, f12)[0];
                int i = CoverArt.ready() ? CoverArt.accent() : hud.hudAccent();
                boolean flag5 = flag2 && mediahub$snap.playing();
                String s1 = flag2
                    ? mediahub$snap.title()
                    : (flag1 ? islandhud$event.title : (flag ? "FunTime" : "\u043d\u0435\u0442 \u0442\u0440\u0435\u043a\u0430"));
                String s2 = flag2 ? mediahub$snap.artist() : (flag1 ? islandhud$event.body : (flag ? s : ""));
                float f15 = flag2 ? (float)mediahub$snap.progress() : 0.0F;
                String s3 = flag2 ? formatMs(mediahub$snap.positionMs()) : "0:00";
                String s4 = flag2 ? formatMs(mediahub$snap.durationMs()) : "0:00";
                String s5 = s2.isEmpty() ? s1 : s1 + " \u2014 " + s2;
                if (flag1) {
                    s5 = islandhud$event.title + "  " + islandhud$event.body;
                } else if (flag && !flag2) {
                    s5 = s;
                }

                float f16 = Anim.timeSec();
                float f17 = 0.25F + 0.75F * f;
                float f18 = 1.0F - Anim.easeInOut(Math.min(1.0F, f11 * 1.55F));
                float f19 = Anim.easeInOut(Math.max(0.0F, (f11 - 0.18F) / 0.82F));
                float f20 = f12 * 0.5F;
                Ui.push();
                Ui.alpha(f17);
                Ui.move(f14, f8);
                Ui.scale(f7, f7);
                Paint.glass(g, 0.0F, 0.0F, f13, f12, f20);
                Paint.outline(g, 0.0F, 0.0F, f13, f12, Theme.alpha(i, 90 + (int)(70.0F * f11)), f20);
                float f21 = 16.0F + 14.0F * f11;
                float f22 = 5.0F + 3.0F * f11;
                float f23 = (f3 - 16.0F) * 0.5F * (1.0F - f11) + f22 * f11;
                float f24 = f21 * 0.5F;
                Paint.box(g, f22, f23, f21, f21, Theme.alpha(1710622, 255), f24);
                if (CoverArt.nvg() != null) {
                    Nvg.image(CoverArt.nvg(), f22, f23, f21, f21, f24);
                } else if (flag1) {
                    Nvg.circle(f22 + f24, f23 + f24, 4.2F, i);
                } else if (flag5) {
                    float f25 = 0.85F + 0.15F * (0.5F + 0.5F * (float)Math.sin(f16 * 8.0F));
                    Nvg.circle(f22 + f24, f23 + f24, 3.2F * f25, i);
                } else {
                    Nvg.circle(f22 + f24, f23 + f24, 3.0F, Theme.MUTED);
                }

                if (f18 > 0.04F) {
                    Ui.alpha(f17 * f18);
                    float f36 = f22 + f21 + 7.0F;
                    float f26 = f13 - f36 - 28.0F;
                    float f27 = (f3 - 8.0F) * 0.5F;
                    Ui.scissor(f36, 3.0F, Math.max(8.0F, f26), f3 - 6.0F);

                    try {
                        float f28 = Nvg.width(s5, 6.8F);
                        if (f28 > f26) {
                            float f29 = f28 + 24.0F;
                            float f30 = f16 * 35.7F % f29;
                            Paint.text(g, s5, f36 - f30, f27, Theme.TEXT, 6.8F);
                            Paint.text(g, s5, f36 - f30 + f29, f27, Theme.TEXT, 6.8F);
                        } else {
                            Paint.text(g, s5, f36, f27, Theme.TEXT, 6.8F);
                        }
                    } finally {
                        Ui.unscissor();
                    }

                    this.paintBands(g, f13 - 24.0F, 18.0F, 2.2F, 2.4F, 11.0F, Theme.alpha(i, 200));
                    Ui.alpha(f17);
                }

                if (f19 > 0.04F) {
                    Ui.alpha(f17 * f19);
                    float f46 = f22 + f21 + 7.0F;
                    this.paintBands(g, f46, f23 + f21, 3.0F, 3.0F, f21 * 0.72F, Theme.alpha(i, 215));
                    float f37 = f46 + bandsWidth(3.0F, 3.0F) + 8.0F;
                    Paint.text(g, trim(s1, 22), f37, 8.0F, Theme.TEXT, 7.6F);
                    if (!s2.isEmpty()) {
                        Paint.text(g, trim(s2, 26), f37, 20.0F, Theme.MUTED, 6.3F);
                    }

                    float f38 = 12.0F;
                    float f39 = 42.0F;
                    float f41 = f13 - 24.0F;
                    Paint.bar(g, f38, f39, f41, 2.4F, !flag1 && (!flag || flag2) ? f15 : 1.0F, i);
                    Nvg.circle(f38 + f41 * Math.max(0.0F, Math.min(1.0F, !flag1 && (!flag || flag2) ? f15 : 1.0F)), f39 + 1.2F, 2.8F, i);
                    this.barX = screen(f14, f38, f7);
                    this.barY = screen(f8, f39 - 4.0F, f7);
                    this.barW = f41 * f7;
                    this.barH = 10.0F * f7;
                    Paint.text(g, s3, f38, 50.0F, Theme.MUTED, 5.8F);
                    Paint.textR(g, s4, f38 + f41, 50.0F, Theme.MUTED, 5.8F);
                    float f42 = 58.0F;
                    float f44 = 18.0F;
                    float f45 = 13.0F;
                    float f32 = f13 * 0.5F;
                    float f33 = f32 - 34.0F;
                    float f34 = f32 - 9.0F;
                    float f35 = f32 + 16.0F;
                    Paint.box(g, f33, f42, f44, f45, Theme.alpha(16777215, 12), 6.0F);
                    Paint.box(g, f34, f42, f44, f45, Theme.alpha(i, 40), 6.0F);
                    Paint.box(g, f35, f42, f44, f45, Theme.alpha(16777215, 12), 6.0F);
                    Paint.textC(g, "\u2039", f33 + f44 * 0.5F, f42 + 1.2F, Theme.TEXT, 10.0F);
                    Paint.textC(g, flag5 ? "\u275a\u275a" : "\u25b6", f34 + f44 * 0.5F, f42 + 1.8F, Theme.TEXT, 7.4F);
                    Paint.textC(g, "\u203a", f35 + f44 * 0.5F, f42 + 1.2F, Theme.TEXT, 10.0F);
                    this.prevX = screen(f14, f33, f7);
                    this.prevY = screen(f8, f42, f7);
                    this.playX = screen(f14, f34, f7);
                    this.playY = screen(f8, f42, f7);
                    this.nextX = screen(f14, f35, f7);
                    this.nextY = screen(f8, f42, f7);
                    this.btnW = f44 * f7;
                    this.btnH = f45 * f7;
                    Ui.alpha(f17);
                }

                Ui.pop();
                this.bodyX = f14;
                this.bodyY = f8;
                this.bodyW = f13 * f7;
                this.bodyH = f12 * f7;
                if (f11 < 0.55F) {
                    this.prevX = this.nextX = this.playX = -999.0F;
                    this.prevY = this.nextY = this.playY = -999.0F;
                    this.btnW = this.btnH = 0.0F;
                }

                this.hitsReady = editor || minecraftclient.currentScreen == null;
            }
        }
    }

    /**
     * The system media API gives no spectrum, so each band gets its own wave seeded by track
     * progress and pulled by a spring. Low bands swing wider and slower than high ones, which is
     * what makes the stack read as an equalizer rather than four sines in a row.
     */
    private void driveBands(boolean playing, float progress, float dt) {
        float time = Anim.timeSec();

        for (int band = 0; band < BANDS; band++) {
            float target;
            if (playing) {
                float beat = 0.5F + 0.5F * (float)Math.sin(time * (6.4F + band * 2.3F) + band * 1.9F + progress * 24.0F);
                float sway = 0.5F + 0.5F * (float)Math.sin(time * (1.6F + band * 0.5F) + progress * 7.0F);
                target = (0.2F + 0.8F * beat) * (0.62F + 0.38F * sway) * (1.0F - 0.16F * band);
            } else {
                target = 0.1F + 0.04F * band;
            }

            this.bands[band].spring(target, playing ? 300.0F : 130.0F, 24.0F, dt);
        }
    }

    private void paintBands(DrawContext g, float x, float bottom, float barW, float gap, float maxH, int color) {
        for (int band = 0; band < BANDS; band++) {
            float h = Math.max(barW, maxH * Math.max(0.0F, this.bands[band].value()));
            Paint.box(g, x + band * (barW + gap), bottom - h, barW, h, color, barW * 0.5F);
        }
    }

    private static float bandsWidth(float barW, float gap) {
        return BANDS * barW + (BANDS - 1) * gap;
    }

    private static Anim[] bands() {
        Anim[] anim = new Anim[4];

        for (int i = 0; i < 4; i++) {
            anim[i] = new Anim(0.1F);
        }

        return anim;
    }

    private void pulseMedia() {
        MediaHub.Snap mediahub$snap = MediaHub.get().snapshot();
        if (!mediahub$snap.available()) {
            this.lastTrackKey = "";
        } else {
            String s = mediahub$snap.title() + "\u0000" + mediahub$snap.artist();
            if (!s.equals(this.lastTrackKey) && !this.lastTrackKey.isEmpty()) {
                this.push("\u0422\u0440\u0435\u043a", mediahub$snap.title());
            }

            this.lastTrackKey = s;
        }
    }

    private void prune() {
        long i = System.currentTimeMillis();

        while (!this.queue.isEmpty() && this.queue.peekFirst().until < i) {
            this.queue.removeFirst();
        }
    }

    private static float screen(float origin, float local, float sc) {
        return origin + local * sc;
    }

    private static float guiMouseX(MinecraftClient mc) {
        return (float)(mc.mouse.getX() * mc.getWindow().getScaledWidth() / Math.max(1, mc.getWindow().getWidth()));
    }

    private static float guiMouseY(MinecraftClient mc) {
        return (float)(mc.mouse.getY() * mc.getWindow().getScaledHeight() / Math.max(1, mc.getWindow().getHeight()));
    }

    private static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private static String formatMs(long ms) {
        if (ms < 0L) {
            ms = 0L;
        }

        long i = ms / 1000L;
        return i / 60L + ":" + String.format(Locale.ROOT, "%02d", i % 60L);
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        } else {
            return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
        }
    }

    private record Event(String title, String body, long until) {
    }
}
