package dev.doorevisuals.world.sky;

import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * A layer of snow drawn over the ground around the player. It is purely visual: the real blocks are not touched, so
 * collisions stay as they are and the snow only exists on this client.
 *
 * <p>The ground is sampled column by column from the heightmap into a small wrapping grid that follows the player. The
 * amount of snow grows while it snows and melts slowly afterwards. Columns do not fill at the same moment (every column
 * has its own threshold, so the first snow lies in patches), and the layer gets thicker in the lee of walls and steps,
 * which is what makes drifts. Corners are averaged between neighbours at the same level, so the surface is smooth on flat
 * ground and still breaks cleanly at a cliff.</p>
 */
final class SnowCover {
    private static final int R = 20;
    private static final int S = 2 * R + 3;
    private static final int FILL_BUDGET = 160;
    private static final int REFRESH_PER_TICK = 40;
    private static final float MAX_HEIGHT = 0.5F;

    private final int[] gx = new int[S * S];
    private final int[] gz = new int[S * S];
    private final int[] top = new int[S * S];
    private final boolean[] ground = new boolean[S * S];
    private final float[] noise = new float[S * S];
    private final float[] heights = new float[S * S];

    private World world;
    private float amount;
    private int refreshAt;
    private int centerX;
    private int centerZ;

    SnowCover() {
        this.clear();
    }

    void clear() {
        java.util.Arrays.fill(this.gx, Integer.MIN_VALUE);
        java.util.Arrays.fill(this.ground, false);
        this.amount = 0.0F;
        this.world = null;
    }

    float amount() {
        return this.amount;
    }

    /** @param snowing whether it snows now; @param speed how fast the layer builds up, 1 is about two and a half minutes */
    void tick(World world, ClientPlayerEntity player, boolean snowing, float dt, float speed) {
        if (this.world != world) {
            this.clear();
            this.world = world;
        }
        if (snowing) {
            this.amount = Math.min(1.0F, this.amount + dt * speed / 150.0F);
        } else {
            this.amount = Math.max(0.0F, this.amount - dt / 200.0F);
        }

        this.centerX = MathHelper.floor(player.getX());
        this.centerZ = MathHelper.floor(player.getZ());
        if (this.amount <= 0.001F && !snowing) {
            return;
        }

        // Blocks change under the snow: re-read a rolling slice of the grid each tick.
        for (int k = 0; k < REFRESH_PER_TICK; k++) {
            this.refreshAt = (this.refreshAt + 1) % (S * S);
            this.gx[this.refreshAt] = Integer.MIN_VALUE;
        }

        int budget = FILL_BUDGET;
        for (int dx = -R; dx <= R && budget > 0; dx++) {
            for (int dz = -R; dz <= R && budget > 0; dz++) {
                int x = this.centerX + dx;
                int z = this.centerZ + dz;
                int i = index(x, z);
                if (this.gx[i] == x && this.gz[i] == z) {
                    continue;
                }
                if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                this.sample(world, i, x, z);
                budget--;
            }
        }
    }

    private void sample(World world, int i, int x, int z) {
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, topY - 1, z);
        BlockState state = world.getBlockState(pos);
        this.gx[i] = x;
        this.gz[i] = z;
        this.top[i] = topY;
        // Only full, dry blocks hold snow; water, lava, slabs and partial shapes are left alone.
        this.ground[i] = !state.isAir()
            && state.getFluidState().isEmpty()
            && state.isFullCube(world, pos)
            && topY > world.getBottomY() + 1;
        this.noise[i] = hash01(x, z);
    }

    private static int index(int x, int z) {
        return Math.floorMod(x, S) * S + Math.floorMod(z, S);
    }

    private boolean valid(int i, int x, int z) {
        return this.gx[i] == x && this.gz[i] == z && this.ground[i];
    }

    /** Thickness of the snow on one column, in blocks. Zero where nothing lies yet. */
    private float thickness(int x, int z) {
        int i = index(x, z);
        if (!this.valid(i, x, z)) {
            return 0.0F;
        }
        float n = this.noise[i];
        float cover = smooth(n * 0.55F, n * 0.55F + 0.30F, this.amount * 1.25F);
        if (cover <= 0.02F) {
            return 0.0F;
        }
        float base = cover * (0.04F + 0.19F * this.amount);

        // Drifts: snow piles up in the lee of anything higher than the column.
        int higher = 0;
        int level = this.top[i];
        higher += this.higherNeighbour(x + 1, z, level);
        higher += this.higherNeighbour(x - 1, z, level);
        higher += this.higherNeighbour(x, z + 1, level);
        higher += this.higherNeighbour(x, z - 1, level);
        float drift = this.amount * 0.07F * higher;
        return Math.min(MAX_HEIGHT, base + drift * cover);
    }

    private int higherNeighbour(int x, int z, int level) {
        int i = index(x, z);
        return this.gx[i] == x && this.gz[i] == z && this.top[i] > level ? 1 : 0;
    }

    /** Height of the snow surface above the block position, or a negative number when there is no snow there. */
    double surfaceY(int x, int z) {
        int i = index(x, z);
        float h = this.thickness(x, z);
        return h > 0.0F && this.valid(i, x, z) ? this.top[i] + h : -1.0;
    }

    void draw(WorldRenderContext ctx, Vec3d cam, float light) {
        MatrixStack matrices = ctx.matrices();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null || this.amount <= 0.01F || this.world == null) {
            return;
        }
        int radius = switch (VisualQuality.level()) {
            case LOW -> 10;
            case MEDIUM -> 15;
            case HIGH -> R;
        };

        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                this.heights[index(this.centerX + dx, this.centerZ + dz)] = this.thickness(this.centerX + dx, this.centerZ + dz);
            }
        }

        MatrixStack.Entry pose = matrices.peek();
        VertexConsumer buf = consumers.getBuffer(Types.fill());
        float lit = MathHelper.clamp(light, 0.2F, 1.0F);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = this.centerX + dx;
                int z = this.centerZ + dz;
                int i = index(x, z);
                if (this.heights[i] <= 0.0F || !this.valid(i, x, z)) {
                    continue;
                }
                float dist = (float)Math.sqrt(dx * dx + dz * dz);
                float fade = MathHelper.clamp((radius - dist) / 5.0F, 0.0F, 1.0F);
                if (fade <= 0.01F) {
                    continue;
                }

                int level = this.top[i];
                float h00 = this.corner(x, z, level);
                float h10 = this.corner(x + 1, z, level);
                float h11 = this.corner(x + 1, z + 1, level);
                float h01 = this.corner(x, z + 1, level);
                int color = this.color(this.noise[i], lit, fade);

                float x0 = (float)(x - cam.x);
                float x1 = (float)(x + 1 - cam.x);
                float z0 = (float)(z - cam.z);
                float z1 = (float)(z + 1 - cam.z);
                float y = level + 0.012F - (float)cam.y;
                buf.vertex(pose, x0, y + h00, z0).color(color);
                buf.vertex(pose, x0, y + h01, z1).color(color);
                buf.vertex(pose, x1, y + h11, z1).color(color);
                buf.vertex(pose, x1, y + h10, z0).color(color);
            }
        }
    }

    /**
     * Height of one grid corner: the mean over the columns that touch it and lie at the same level as the column being
     * drawn. Columns at another level are ignored, so snow does not slope across a step.
     */
    private float corner(int cx, int cz, int level) {
        float sum = 0.0F;
        int n = 0;
        for (int ox = -1; ox <= 0; ox++) {
            for (int oz = -1; oz <= 0; oz++) {
                int x = cx + ox;
                int z = cz + oz;
                int i = index(x, z);
                if (this.gx[i] == x && this.gz[i] == z && this.top[i] == level) {
                    sum += this.heights[i];
                    n++;
                }
            }
        }
        return n == 0 ? 0.0F : sum / n;
    }

    private int color(float n, float light, float fade) {
        float mixv = 0.35F + 0.65F * n;
        float r = (0.90F + 0.08F * mixv) * light;
        float g = (0.93F + 0.06F * mixv) * light;
        float b = (0.98F + 0.02F * mixv) * light;
        int a = Math.round(250.0F * fade);
        return (a << 24) | (Math.round(r * 255.0F) << 16) | (Math.round(g * 255.0F) << 8) | Math.round(b * 255.0F);
    }

    private static float smooth(float e0, float e1, float x) {
        float t = MathHelper.clamp((x - e0) / (e1 - e0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float hash01(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0F;
    }
}
