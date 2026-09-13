package dev.doorevisuals.media;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import org.endlesssource.mediainterface.SystemMediaFactory;
import org.endlesssource.mediainterface.api.MediaSession;
import org.endlesssource.mediainterface.api.MediaTransportControls;
import org.endlesssource.mediainterface.api.NowPlaying;
import org.endlesssource.mediainterface.api.PlaybackState;
import org.endlesssource.mediainterface.api.SystemMediaInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaHub {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Media");
    private static final MediaHub I = new MediaHub();
    private SystemMediaInterface api;
    private volatile MediaHub.Snap snap = MediaHub.Snap.EMPTY;
    private long next;

    private MediaHub() {
    }

    public static MediaHub get() {
        return I;
    }

    public void start() {
        try {
            if (!SystemMediaFactory.isPlatformSupported()) {
                LOG.warn("Media platform not supported ({})", SystemMediaFactory.getPlatformName());
                return;
            }

            this.api = SystemMediaFactory.createSystemInterface();
            LOG.info("Media interface ready ({})", this.api.getClass().getSimpleName());
        } catch (Throwable throwable) {
            LOG.warn("Media interface unavailable: {}", throwable.toString());
            this.api = null;
        }
    }

    public void stop() {
        if (this.api != null) {
            try {
                this.api.close();
            } catch (Throwable throwable) {
            }

            this.api = null;
        }

        CoverArt.sync("");
    }

    public MediaHub.Snap snapshot() {
        long i = System.currentTimeMillis();
        if (i < this.next) {
            return this.snap;
        } else {
            this.next = i + 400L;
            if (this.api == null) {
                this.snap = MediaHub.Snap.EMPTY;
                CoverArt.sync("");
                return this.snap;
            } else {
                try {
                    MediaSession mediasession = (MediaSession)this.api.getActiveSession().orElse(null);
                    if (mediasession == null) {
                        for (MediaSession mediasession1 : this.api.getAllSessions()) {
                            if (mediasession1.getNowPlaying().isPresent()) {
                                mediasession = mediasession1;
                                break;
                            }
                        }
                    }

                    if (mediasession == null) {
                        this.snap = MediaHub.Snap.EMPTY;
                        CoverArt.sync("");
                        return this.snap;
                    }

                    Optional<NowPlaying> optional = mediasession.getNowPlaying();
                    if (optional.isEmpty()) {
                        this.snap = MediaHub.Snap.EMPTY;
                        CoverArt.sync("");
                        return this.snap;
                    }

                    NowPlaying nowplaying = optional.get();
                    String s2 = nowplaying.getTitle().orElse("").trim();
                    String s = nowplaying.getArtist().orElse("").trim();
                    boolean flag = !s2.isEmpty() || !s.isEmpty();
                    if (!flag) {
                        this.snap = MediaHub.Snap.EMPTY;
                        CoverArt.sync("");
                        return this.snap;
                    }

                    boolean flag1 = false;

                    try {
                        MediaTransportControls mediatransportcontrols = mediasession.getControls();
                        if (mediatransportcontrols != null) {
                            flag1 = mediatransportcontrols.getPlaybackState() == PlaybackState.PLAYING;
                        }
                    } catch (Throwable throwable) {
                    }

                    long k = nowplaying.getPosition().map(MediaHub::toMs).orElse(0L);
                    long j = nowplaying.getDuration().map(MediaHub::toMs).orElse(0L);
                    double d0 = j > 0L ? Math.max(0.0, Math.min(1.0, (double)k / j)) : 0.0;
                    String s1 = nowplaying.getArtwork().orElse("");
                    CoverArt.sync(s1);
                    this.snap = new MediaHub.Snap(true, flag1, s2.isEmpty() ? "Unknown" : s2, s.isEmpty() ? "Media" : s, d0, k, j, !s1.isBlank());
                } catch (Throwable throwable1) {
                    LOG.debug("Media snapshot failed: {}", throwable1.toString());
                    this.snap = MediaHub.Snap.EMPTY;
                }

                return this.snap;
            }
        }
    }

    public boolean nextTrack() {
        return this.control(MediaTransportControls::next);
    }

    public boolean prevTrack() {
        return this.control(MediaTransportControls::previous);
    }

    public boolean togglePlay() {
        return this.control(MediaTransportControls::togglePlayPause);
    }

    public boolean seek(double progress) {
        double d0 = Math.max(0.0, Math.min(1.0, progress));
        return this.control(c -> {
            try {
                long i = this.snap.durationMs();
                if (i <= 0L) {
                    return false;
                }

                Duration duration = Duration.ofMillis((long)(i * d0));

                try {
                    c.getClass().getMethod("seek", Duration.class).invoke(c, duration);
                    return true;
                } catch (Throwable throwable1) {
                    try {
                        c.getClass().getMethod("setPlaybackPosition", Duration.class).invoke(c, duration);
                        return true;
                    } catch (Throwable throwable) {
                    }
                }
            } catch (Throwable throwable2) {
            }

            return false;
        });
    }

    public boolean nudgeVolume(double wheel) {
        return this.control(c -> {
            try {
                double d0 = c.getClass().getMethod("getVolume").invoke(c) instanceof Number number ? number.doubleValue() : 0.5;
                double d1 = Math.max(0.0, Math.min(1.0, d0 + wheel * 0.06));

                try {
                    c.getClass().getMethod("setVolume", double.class).invoke(c, d1);
                    return true;
                } catch (Throwable throwable) {
                    c.getClass().getMethod("setVolume", float.class).invoke(c, (float)d1);
                    return true;
                }
            } catch (Throwable throwable1) {
                return false;
            }
        });
    }

    private boolean control(Function<MediaTransportControls, Boolean> op) {
        if (this.api == null) {
            return false;
        } else {
            try {
                MediaSession mediasession = (MediaSession)this.api.getActiveSession().orElse(null);
                if (mediasession == null) {
                    return false;
                } else {
                    MediaTransportControls mediatransportcontrols = mediasession.getControls();
                    if (mediatransportcontrols == null) {
                        return false;
                    } else {
                        boolean flag = Boolean.TRUE.equals(op.apply(mediatransportcontrols));
                        if (flag) {
                            this.next = 0L;
                        }

                        return flag;
                    }
                }
            } catch (Throwable throwable) {
                LOG.debug("Media control failed: {}", throwable.toString());
                return false;
            }
        }
    }

    private static long toMs(Duration d) {
        return d == null ? 0L : Math.max(0L, d.toMillis());
    }

    public record Snap(boolean available, boolean playing, String title, String artist, double progress, long positionMs, long durationMs, boolean hasArt) {
        static final MediaHub.Snap EMPTY = new MediaHub.Snap(false, false, "", "", 0.0, 0L, 0L, false);
    }
}
