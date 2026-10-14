package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/** Client-only surface rain, snowfall, puddles, impacts and footsteps. */
final class AtmosphereEffectsRenderer {
    private static final long WET_MILLIS = 14_000L;
    private static final int MAX_IMPACTS = 56;
    private static final double STRIDE = 0.66;

    private final ArrayDeque<RainDrop> drops = new ArrayDeque<>();
    private final ArrayDeque<Impact> impacts = new ArrayDeque<>();
    private final ArrayDeque<FootMark> marks = new ArrayDeque<>();
    private final LinkedHashMap<BlockPos, Long> wet = new LinkedHashMap<>();
    private final List<Flake> flakes = new ArrayList<>();
    private final SnowCover snowCover = new SnowCover();
    private float snowIntensity = 1.0F;

    private World world;
    private Vec3d lastPlayerPos;
    private long nextDropAt;
    private long lastStepAt;
    private long lastTickAt;
    private long stormStartedAt;
    private boolean wasThundering;
    private float wind;
    private double snowCarry;
    private Vec3d stepAnchor;
    private boolean rightFoot;
    private int reflection = 0x78BFD0;

    void clear() {
        this.drops.clear();
        this.impacts.clear();
        this.marks.clear();
        this.wet.clear();
        this.flakes.clear();
        this.snowCover.clear();
        this.stepAnchor = null;
        this.world = null;
        this.lastPlayerPos = null;
        this.lastTickAt = 0L;
        this.stormStartedAt = 0L;
        this.wasThundering = false;
        this.wind = 0.0F;
    }

    private static int maxDrops() {
        return switch (VisualQuality.level()) {
            case LOW -> 40;
            case MEDIUM -> 90;
            case HIGH -> 170;
        };
    }

    private static int maxPuddles() {
        return switch (VisualQuality.level()) {
            case LOW -> 24;
            case MEDIUM -> 52;
            case HIGH -> 96;
        };
    }

    private static int maxMarks() {
        return switch (VisualQuality.level()) {
            case LOW -> 40;
            case MEDIUM -> 80;
            case HIGH -> 120;
        };
    }

    private int maxFlakes() {
        int base = switch (VisualQuality.level()) {
            case LOW -> 160;
            case MEDIUM -> 340;
            case HIGH -> 620;
        };
        return Math.round(base * this.snowIntensity);
    }

    float windStrength() {
        return this.wind;
    }

    void tick(
        MinecraftClient mc,
        boolean rainEnabled,
        boolean stormEnabled,
        boolean footstepsEnabled,
        boolean snowEnabled,
        float snowSpeed,
        float intensity
    ) {
        if (mc.world == null || mc.player == null) {
            clear();
            return;
        }

        if (this.world != mc.world) {
            clear();
            this.world = mc.world;
        }

        long now = System.currentTimeMillis();
        float dt = this.lastTickAt == 0L
            ? 0.05F
            : MathHelper.clamp(
                (now - this.lastTickAt) / 1000.0F,
                0.0F,
                0.1F
            );
        this.lastTickAt = now;

        // Entirely client-side: this renderer never changes world weather state.
        boolean raining =
            rainEnabled
            && this.world.getRegistryKey() == World.OVERWORLD;

        boolean thundering = raining && stormEnabled;
        boolean snowing = snowEnabled && this.world.getRegistryKey() == World.OVERWORLD;
        this.snowIntensity = MathHelper.clamp(intensity, 0.3F, 3.5F);

        if (thundering && !this.wasThundering) {
            this.stormStartedAt = now;
        }
        if (!thundering) {
            this.stormStartedAt = 0L;
        }
        this.wasThundering = thundering;

        float targetWind;
        if (!raining) {
            targetWind = snowing ? MathHelper.clamp(0.10F + 0.14F * (this.snowIntensity - 1.0F), 0.06F, 0.55F) : 0.0F;
        } else if (thundering) {
            float buildup = Math.min(
                1.0F,
                (now - this.stormStartedAt) / 8_000.0F
            );
            targetWind = 0.28F + 0.52F * buildup;
        } else {
            targetWind = 0.20F;
        }

        float response = targetWind > this.wind ? 0.75F : 0.28F;
        this.wind +=
            (targetWind - this.wind)
            * (1.0F - (float)Math.exp(-dt * response));

        if (raining && now >= this.nextDropAt) {
            int burst = switch (VisualQuality.level()) {
                case LOW -> 1;
                case MEDIUM -> 2;
                case HIGH -> 4;
            };
            for (int i = 0; i < burst; i++) {
                spawnRain(mc.player, now);
            }
            this.nextDropAt = now + 90L;
        }

        if (snowing) {
            updateSnow(mc.player, dt, now);
        } else {
            // The flakes already in the air finish their fall instead of vanishing when the snow stops.
            updateSnow(mc.player, dt, now, false);
        }
        this.snowCover.tick(this.world, mc.player, snowing, dt, snowSpeed);

        advanceDrops(now);
        expire(now);

        Vec3d pos = mc.player.getEntityPos();
        if (footstepsEnabled) {
            updateFootsteps(mc.player, pos, now);
        } else {
            this.marks.clear();
        }
        this.lastPlayerPos = pos;
    }

    void draw(
        WorldRenderContext ctx,
        boolean rainEnabled,
        boolean footstepsEnabled,
        boolean snowEnabled,
        int reflection,
        boolean driftsEnabled,
        float light
    ) {
        if (
            this.world == null
            || (!rainEnabled && !footstepsEnabled && !snowEnabled && !driftsEnabled && this.flakes.isEmpty())
        ) {
            return;
        }

        this.reflection = reflection & 0xFFFFFF;

        long now = System.currentTimeMillis();

        if (rainEnabled) {
            drawRain(ctx, now);
            drawImpacts(ctx, now);
            drawPuddles(ctx, now);
        }

        if (driftsEnabled) {
            this.snowCover.draw(ctx, MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos(), light);
        }

        if (footstepsEnabled) {
            drawFootsteps(ctx, now);
        }

        if (!this.flakes.isEmpty()) {
            drawSnow(ctx, now);
        }
    }

    /**
     * Flakes are round soft discs that face the camera. They fade in as they appear high up, twinkle a little, and fade
     * out right above the ground, so none of them pops in or out.
     */
    private void drawSnow(WorldRenderContext ctx, long now) {
        int segs = Math.max(8, VisualQuality.orbSegs());
        for (Flake flake : this.flakes) {
            float twinkle = 0.78F + 0.22F * (float)Math.sin(now * 0.004 + flake.phase * 3.0F);
            float near = MathHelper.clamp((float)(flake.y - flake.ground) / 0.8F, 0.0F, 1.0F);
            float fadeIn = MathHelper.clamp(flake.age / 1.2F, 0.0F, 1.0F);
            int alpha = Math.round(210.0F * twinkle * near * fadeIn);
            if (alpha <= 4) continue;
            Mesh.softOrb(ctx, flake.x, flake.y, flake.z, flake.size, segs, (alpha << 24) | 0xF0F6FF);
        }
    }

    private void updateSnow(ClientPlayerEntity player, float dt, long now) {
        updateSnow(player, dt, now, true);
    }

    /**
     * Flakes spawn high above the player over a wide area and fall slowly, so the snow is already in the air all around
     * and comes down across the whole visible world. Each one drifts on the shared wind plus its own slow sway, and
     * falls at its own speed.
     */
    private void updateSnow(ClientPlayerEntity player, float dt, long now, boolean spawnMore) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int cap = maxFlakes();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        double reach = 28.0;

        if (spawnMore) {
            this.snowCarry += cap / 9.0 * dt;
            int spawn = (int)this.snowCarry;
            this.snowCarry -= spawn;
            for (int i = 0; i < spawn && this.flakes.size() < cap; i++) {
                double x = px + random.nextDouble(-reach, reach);
                double z = pz + random.nextDouble(-reach, reach);
                int ground = this.world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
                double y = Math.max(py, ground) + random.nextDouble(7.0, 22.0);
                // Under a roof or a cliff the column is closed above the spawn height: nothing falls there.
                if (ground > py + 24.0) continue;
                double bigness = random.nextDouble();
                this.flakes.add(new Flake(
                    x, y, z, ground,
                    (float)(0.75 + random.nextDouble() * 0.75),
                    (float)(random.nextDouble() * 6.2831853),
                    // mostly small flakes, a few larger ones
                    0.020F + (float)(bigness * bigness) * 0.050F
                ));
            }
        }

        for (Iterator<Flake> it = this.flakes.iterator(); it.hasNext();) {
            Flake flake = it.next();
            flake.age += dt;
            flake.y -= flake.fall * dt;
            double gust = 0.75 + 0.25 * Math.sin(now * 0.00031 + flake.phase * 0.2);
            flake.x += (this.wind * 2.0 * gust + Math.sin(now * 0.00085 + flake.phase) * 0.30) * dt;
            flake.z += (this.wind * 0.35 + Math.cos(now * 0.00065 + flake.phase * 1.3) * 0.26) * dt;
            double ox = flake.x - px;
            double oz = flake.z - pz;
            if (flake.y <= flake.ground + 0.04 || ox * ox + oz * oz > (reach + 4.0) * (reach + 4.0)) {
                it.remove();
            }
        }
    }

    private void drawRain(WorldRenderContext ctx, long now) {
        int drawn = 0;
        int cap = Math.max(4, VisualQuality.frameParticleCap() / 3);

        for (RainDrop drop : this.drops) {
            if (drawn >= cap) break;

            float age = MathHelper.clamp((now - drop.born) / 460.0F, 0.0F, 1.0F);

            double drift = this.wind * age * 0.28;

            double x = drop.x + drift;
            double z = drop.z + this.wind * age * 0.08;
            double y = drop.y + 6.4 * (1.0 - age);

            int alpha = Math.round(96.0F + 50.0F * (1.0F - age));

            int color = (alpha << 24) | 0xB8D7EE;

            Mesh.outline(ctx, x, y, z, x + this.wind * 0.055, y - 0.30, z, color, 1.1F);

            drawn++;
        }
    }

    private void drawImpacts(WorldRenderContext ctx, long now) {
        for (Impact impact : this.impacts) {
            if (!VisualQuality.takeParticle()) break;

            float age = MathHelper.clamp((now - impact.born) / 620.0F, 0.0F, 1.0F);

            float ease = smooth(age);
            float radius = impact.water ? 0.035F + ease * 0.27F : 0.035F + ease * 0.18F;

            int alpha = Math.round(130.0F * (1.0F - age));

            int ringColor = (alpha << 24) | (impact.water ? 0x8ADCF0 : 0xD7ECF9);

            Mesh.ring(ctx, impact.x, impact.y + 0.016, impact.z, radius, 14, ringColor);

            if (impact.water) {
                float inner = radius * 0.58F;
                int innerAlpha = Math.round(52.0F * (1.0F - age));
                Mesh.ring(ctx, impact.x, impact.y + 0.018, impact.z, inner, 12, (innerAlpha << 24) | 0xC0F1FF);
            }
        }
    }

    private void drawPuddles(WorldRenderContext ctx, long now) {
        for (Map.Entry<BlockPos, Long> entry : this.wet.entrySet()) {
            BlockPos p = entry.getKey();

            float fade = MathHelper.clamp((entry.getValue() - now) / (float)WET_MILLIS, 0.0F, 1.0F);

            if (fade <= 0.005F) continue;
            if (!VisualQuality.takeParticle()) break;

            long seedLong = p.asLong();
            float seed = (float)((seedLong & 4095L) * 0.024543693);

            float wobble = (float)Math.sin(now * 0.00018 + seed);

            float yaw = seed + wobble * 0.08F;

            double cx = p.getX() + 0.5;
            double cy = p.getY() + 1.0125;
            double cz = p.getZ() + 0.5;

            float size = 0.18F + 0.26F * fade;

            float narrow = 0.058F + 0.11F * fade;

            // Very dark, broad base: the eye reads this as a thin wet film, not as a blue decal floating on the block.
            int darkAlpha = Math.round(28.0F * fade);
            Mesh.ellipse(ctx, cx, cy, cz, size, narrow, yaw, 22, (darkAlpha << 24) | 0x101C23, Types.fill());

            // Broken secondary reflection, tinted by the sky.
            int edgeAlpha = Math.round(28.0F + 34.0F * fade);

            Mesh.ellipse(
                ctx,
                cx + 0.012,
                cy + 0.0010,
                cz - 0.008,
                size * 0.82F,
                narrow * 0.72F,
                yaw + 0.31F,
                20,
                (edgeAlpha << 24) | this.reflection,
                Types.fill()
            );

            // Thin highlight line; intentionally not centered.
            float gloss = 0.06F + 0.13F * fade;

            int glossAlpha = Math.round(34.0F * fade);

            Mesh.ellipse(
                ctx,
                cx - Math.cos(yaw) * gloss,
                cy + 0.0019,
                cz - Math.sin(yaw) * gloss,
                size * 0.38F,
                narrow * 0.24F,
                yaw + 0.52F,
                14,
                (glossAlpha << 24) | 0xD9F8FF,
                Types.fill()
            );

            // Real rain ripple: not every puddle gets one at every frame.
            if (fade > 0.22F) {
                float phase = (float)((now * 0.00011 + seed * 1.7) % 1.0);

                float rippleRadius = 0.025F + phase * size * 0.72F;

                int rippleAlpha = Math.round(22.0F * (1.0F - phase) * fade);

                Mesh.ring(ctx, cx, cy + 0.003F, cz, rippleRadius, 16, (rippleAlpha << 24) | 0xB4EFF7);
            }
        }
    }

    private void drawFootsteps(WorldRenderContext ctx, long now) {
        for (FootMark mark : this.marks) {
            if (!VisualQuality.takeParticle()) break;

            float fade = MathHelper.clamp(1.0F - (now - mark.born) / (float)mark.life, 0.0F, 1.0F);

            int color = (Math.round(mark.alpha * fade) << 24) | (mark.rgb & 0xFFFFFF);

            if (mark.water) {
                Mesh.ring(ctx, mark.x, mark.y + 0.015, mark.z, 0.08 + (1.0F - fade) * 0.17, 12, color);
            } else {
                Mesh.ellipse(ctx, mark.x, mark.y + 0.012, mark.z, 0.105, 0.18, mark.yaw, 12, color, Types.fill());

                Mesh.ellipse(
                    ctx,
                    mark.x,
                    mark.y + 0.013,
                    mark.z,
                    0.052,
                    0.085,
                    mark.yaw,
                    10,
                    (Math.round(mark.alpha * fade * 0.42F) << 24) | (mark.rgb & 0xFFFFFF),
                    Types.fill()
                );
            }
        }
    }

    private void spawnRain(
        ClientPlayerEntity player,
        long now
    ) {
        ThreadLocalRandom random =
            ThreadLocalRandom.current();

        int x = MathHelper.floor(
            player.getX()
                + random.nextInt(-10, 11)
        );

        int z = MathHelper.floor(
            player.getZ()
                + random.nextInt(-10, 11)
        );

        int y = this.world.getTopY(
            Heightmap.Type.MOTION_BLOCKING,
            x,
            z
        ) - 1;

        BlockPos surface =
            new BlockPos(x, y, z);

        if (!this.world.isSkyVisible(surface.up())) {
            return;
        }

        BlockState state =
            this.world.getBlockState(surface);

        boolean water =
            state.getFluidState()
                .isIn(FluidTags.WATER);

        if (
            !water
            && !state.isSolidBlock(
                this.world,
                surface
            )
        ) {
            return;
        }

        double cx = x + 0.5;
        double cz = z + 0.5;
        double top =
            y + (water ? 0.88 : 1.0);

        this.drops.addLast(
            new RainDrop(
                cx,
                top,
                cz,
                now
            )
        );

        int dropCap = maxDrops();
        while (this.drops.size() > dropCap) {
            this.drops.removeFirst();
        }

        if (water) {
            this.impacts.addLast(
                new Impact(
                    cx,
                    y + 0.89,
                    cz,
                    now,
                    true
                )
            );
        } else {
            this.wet.put(
                surface.toImmutable(),
                now + WET_MILLIS
            );

            int puddles = maxPuddles();
            while (this.wet.size() > puddles) {
                this.wet.remove(
                    this.wet.keySet()
                        .iterator()
                        .next()
                );
            }

            this.impacts.addLast(
                new Impact(
                    cx,
                    y + 1.005,
                    cz,
                    now,
                    false
                )
            );
        }

        while (this.impacts.size() > MAX_IMPACTS) {
            this.impacts.removeFirst();
        }
    }

    private void advanceDrops(long now) {
        Iterator<RainDrop> iterator =
            this.drops.iterator();

        while (iterator.hasNext()) {
            RainDrop drop = iterator.next();
            if (now - drop.born >= 460L) {
                iterator.remove();
            }
        }
    }

    private void expire(long now) {
        this.impacts.removeIf(
            value -> now - value.born > 620L
        );

        this.marks.removeIf(
            value -> now - value.born > value.life
        );

        this.wet.entrySet().removeIf(
            entry -> entry.getValue() <= now
        );
    }

    private void updateFootsteps(
        ClientPlayerEntity player,
        Vec3d pos,
        long now
    ) {
        if (!player.isOnGround()) {
            return;
        }
        if (this.stepAnchor == null) {
            this.stepAnchor = pos;
            return;
        }

        double dx = pos.x - this.stepAnchor.x;
        double dz = pos.z - this.stepAnchor.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 6.0) {
            // teleported: start over from here
            this.stepAnchor = pos;
            return;
        }
        // One print per stride, measured from the previous print, so the pace of walking and sprinting both leave marks.
        if (dist < STRIDE) {
            return;
        }

        BlockPos feet = BlockPos.ofFloored(pos.x, pos.y + 0.02, pos.z);
        BlockState atFeet = this.world.getBlockState(feet);
        boolean water = atFeet.getFluidState().isIn(FluidTags.WATER) && pos.y - feet.getY() < 0.45;

        // The snow layer drawn over the ground counts as snow too: prints are pressed into it.
        double coverY = this.snowCover.surfaceY(MathHelper.floor(pos.x), MathHelper.floor(pos.z));
        boolean onCover = !water && coverY > 0.0 && Math.abs(pos.y - coverY) < 1.6;

        BlockPos surface = null;
        BlockState state = null;
        String id = "";
        if (water) {
            surface = feet;
            state = atFeet;
        } else {
            // Snow layers are the block the player stands inside of; snow blocks and sand are the block below.
            for (BlockPos candidate : new BlockPos[] {feet, feet.down()}) {
                BlockState st = this.world.getBlockState(candidate);
                String name = net.minecraft.registry.Registries.BLOCK.getId(st.getBlock()).getPath();
                if (name.equals("snow") || name.equals("snow_block") || name.equals("powder_snow") || name.endsWith("_snow")
                    || name.equals("sand") || name.endsWith("_sand")) {
                    surface = candidate;
                    state = st;
                    id = name;
                    break;
                }
            }
        }
        if (surface == null && !onCover) {
            // Nothing printable underfoot: restart the stride so the next print is not placed a long way behind.
            this.stepAnchor = pos;
            return;
        }

        boolean snow = !water && (onCover || !id.contains("sand"));
        float yaw = (float)Math.atan2(-dx, dz);
        double side = this.rightFoot ? 0.1 : -0.1;
        this.rightFoot = !this.rightFoot;
        double x = pos.x + Math.cos(yaw) * side;
        double z = pos.z + Math.sin(yaw) * side;

        double y;
        if (water) {
            y = surface.getY() + 0.88;
        } else if (onCover) {
            y = coverY;
        } else if (id.equals("snow") && state != null && state.contains(SnowBlock.LAYERS)) {
            y = surface.getY() + state.get(SnowBlock.LAYERS) * 0.125;
        } else {
            y = surface.getY() + 1.0;
        }

        int rgb = water ? 0x70DFFF : snow ? 0xAFC8DC : 0xC9A16C;
        int alpha = water ? 155 : snow ? 125 : 92;
        long life = water ? 1_600L : snow ? 24_000L : 14_000L;

        this.marks.addLast(new FootMark(x, y, z, yaw, rgb, alpha, water, now, life));
        int markCap = maxMarks();
        while (this.marks.size() > markCap) {
            this.marks.removeFirst();
        }

        this.stepAnchor = pos;
        this.lastStepAt = now;
    }

    private static float smooth(float x) {
        x = MathHelper.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static final class Flake {
        double x;
        double y;
        double z;
        final double ground;
        final float fall;
        final float phase;
        final float size;
        float age;

        Flake(double x, double y, double z, double ground, float fall, float phase, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ground = ground;
            this.fall = fall;
            this.phase = phase;
            this.size = size;
        }
    }

    private record RainDrop(
        double x,
        double y,
        double z,
        long born
    ) { }

    private record Impact(
        double x,
        double y,
        double z,
        long born,
        boolean water
    ) { }

    private record FootMark(
        double x,
        double y,
        double z,
        float yaw,
        int rgb,
        int alpha,
        boolean water,
        long born,
        long life
    ) { }
}
