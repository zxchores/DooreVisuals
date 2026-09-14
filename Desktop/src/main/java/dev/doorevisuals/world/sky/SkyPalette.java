package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Theme;

/**
 * The colours the sky is currently painted with. A preset supplies the target, this is what actually
 * reaches the shaders after the switch between presets has been smoothed out.
 */
public record SkyPalette(
    int zenith, int horizon, int nadir, int cloud, int auroraA, int auroraB, float turbidity, float coverage
) {
    private static final int NIGHT_ZENITH = -16315620;
    private static final int NIGHT_HORIZON = -15195064;
    private static final int NIGHT_CLOUD = -7692072;

    public static SkyPalette of(SkyPreset preset, int accent) {
        return new SkyPalette(
            preset.zenith(),
            preset.horizon(),
            preset.nadir(),
            preset.cloudTint(accent),
            preset.auroraA(),
            preset.auroraB(),
            preset.turbidity(),
            preset.coverage()
        );
    }

    /**
     * The same sky after sundown. Presets that follow the world clock keep their daylight colours,
     * so without this the shell stays bright blue at midnight while the moon is already up.
     */
    public SkyPalette afterDark(float t) {
        if (t <= 0.001F) {
            return this;
        } else {
            float f = Math.min(1.0F, t);
            return new SkyPalette(
                Theme.lerp(this.zenith, NIGHT_ZENITH, f),
                Theme.lerp(this.horizon, NIGHT_HORIZON, f),
                this.nadir,
                Theme.lerp(this.cloud, NIGHT_CLOUD, f),
                this.auroraA,
                this.auroraB,
                this.turbidity * (1.0F - 0.55F * f),
                this.coverage
            );
        }
    }

    public SkyPalette lerp(SkyPalette to, float t) {
        return new SkyPalette(
            Theme.lerp(this.zenith, to.zenith, t),
            Theme.lerp(this.horizon, to.horizon, t),
            Theme.lerp(this.nadir, to.nadir, t),
            Theme.lerp(this.cloud, to.cloud, t),
            Theme.lerp(this.auroraA, to.auroraA, t),
            Theme.lerp(this.auroraB, to.auroraB, t),
            this.turbidity + (to.turbidity - this.turbidity) * t,
            this.coverage + (to.coverage - this.coverage) * t
        );
    }

    /**
     * Colour of the shell at {@code t}, zero overhead through one at the horizon and two straight
     * down. Turbidity milks out the horizon stop, which is where haze reads strongest.
     */
    public int band(float t) {
        int i = Theme.lerp(this.horizon, -1, this.turbidity * 0.22F);
        return t < 0.5F ? Theme.lerp(this.zenith, i, t * 2.0F) : Theme.lerp(i, this.nadir, (t - 0.5F) * 2.0F);
    }
}
