package dev.doorevisuals.draw;

import net.minecraft.client.gui.DrawContext;

/**
 * One transform stack for the whole interface.
 *
 * <p>Shapes live in {@link Sdf} and text still lives in NanoVG, so a translate or a scale that only
 * reached one of them would pull a plate away from its own label. Every call here is applied to the
 * coordinates {@code Sdf} bakes into its vertices and mirrored into NanoVG in the same breath.
 *
 * <p>Only translation, scale, global alpha and an axis aligned clip are offered, which is exactly
 * what the interface ever asked NanoVG for.
 */
public final class Ui {
    private static final int DEPTH = 32;
    private static final float FAR = 1.0E6F;
    private static final float[] MOVE_X = new float[DEPTH];
    private static final float[] MOVE_Y = new float[DEPTH];
    private static final float[] SCALE_X = new float[DEPTH];
    private static final float[] SCALE_Y = new float[DEPTH];
    private static final float[] ALPHA = new float[DEPTH];
    private static final float[] CLIP_X0 = new float[DEPTH];
    private static final float[] CLIP_Y0 = new float[DEPTH];
    private static final float[] CLIP_X1 = new float[DEPTH];
    private static final float[] CLIP_Y1 = new float[DEPTH];
    private static int depth;
    private static int clips;

    static {
        reset();
    }

    private Ui() {
    }

    /**
     * Opens one interface block: a shape batch first, then the NanoVG element that carries its text,
     * so the text of a block always sits above its own plates.
     */
    public static void frame(DrawContext g, Runnable draw) {
        if (g != null && draw != null) {
            Sdf.Batch sdf$batch = Sdf.open(g);
            Nvg.run(g, () -> {
                reset();
                Sdf.bind(sdf$batch);

                try {
                    draw.run();
                } finally {
                    Sdf.close();
                    reset();
                }
            });
        }
    }

    /**
     * Runs a block with shapes back on NanoVG. Menus and pickers are drawn last and have to cover the
     * text underneath them, which the batch cannot do because it is submitted before that text.
     */
    public static void nvgShapes(Runnable draw) {
        if (draw != null) {
            if (!Nvg.frame()) {
                draw.run();
            } else {
                Sdf.Batch sdf$batch = Sdf.detach();

                try {
                    draw.run();
                } finally {
                    Sdf.bind(sdf$batch);
                }
            }
        }
    }

    public static void push() {
        if (depth < DEPTH - 1) {
            MOVE_X[depth + 1] = MOVE_X[depth];
            MOVE_Y[depth + 1] = MOVE_Y[depth];
            SCALE_X[depth + 1] = SCALE_X[depth];
            SCALE_Y[depth + 1] = SCALE_Y[depth];
            ALPHA[depth + 1] = ALPHA[depth];
            depth++;
        }

        Nvg.push();
    }

    public static void pop() {
        if (depth > 0) {
            depth--;
        }

        Nvg.pop();
    }

    public static void move(float x, float y) {
        MOVE_X[depth] = MOVE_X[depth] + x * SCALE_X[depth];
        MOVE_Y[depth] = MOVE_Y[depth] + y * SCALE_Y[depth];
        Nvg.move(x, y);
    }

    public static void scale(float x, float y) {
        SCALE_X[depth] = SCALE_X[depth] * x;
        SCALE_Y[depth] = SCALE_Y[depth] * y;
        Nvg.scale(x, y);
    }

    /** Absolute, the way NanoVG treats its global alpha, so a push and pop restores the old value. */
    public static void alpha(float a) {
        ALPHA[depth] = Math.max(0.0F, Math.min(1.0F, a));
        Nvg.alpha(a);
    }

    public static void scissor(float x, float y, float w, float h) {
        if (clips < DEPTH - 1) {
            float f = x * SCALE_X[depth] + MOVE_X[depth];
            float f1 = y * SCALE_Y[depth] + MOVE_Y[depth];
            float f2 = (x + w) * SCALE_X[depth] + MOVE_X[depth];
            float f3 = (y + h) * SCALE_Y[depth] + MOVE_Y[depth];
            CLIP_X0[clips + 1] = Math.max(CLIP_X0[clips], Math.min(f, f2));
            CLIP_Y0[clips + 1] = Math.max(CLIP_Y0[clips], Math.min(f1, f3));
            CLIP_X1[clips + 1] = Math.min(CLIP_X1[clips], Math.max(f, f2));
            CLIP_Y1[clips + 1] = Math.min(CLIP_Y1[clips], Math.max(f1, f3));
            clips++;
        }

        Nvg.scissor(x, y, w, h);
    }

    public static void unscissor() {
        if (clips > 0) {
            clips--;
        }

        Nvg.unscissor();
    }

    static void reset() {
        depth = 0;
        clips = 0;
        MOVE_X[0] = 0.0F;
        MOVE_Y[0] = 0.0F;
        SCALE_X[0] = 1.0F;
        SCALE_Y[0] = 1.0F;
        ALPHA[0] = 1.0F;
        CLIP_X0[0] = -FAR;
        CLIP_Y0[0] = -FAR;
        CLIP_X1[0] = FAR;
        CLIP_Y1[0] = FAR;
    }

    static float moveX() {
        return MOVE_X[depth];
    }

    static float moveY() {
        return MOVE_Y[depth];
    }

    static float scaleX() {
        return SCALE_X[depth];
    }

    static float scaleY() {
        return SCALE_Y[depth];
    }

    static float alpha() {
        return ALPHA[depth];
    }

    static float clipLeft() {
        return CLIP_X0[clips];
    }

    static float clipTop() {
        return CLIP_Y0[clips];
    }

    static float clipRight() {
        return CLIP_X1[clips];
    }

    static float clipBottom() {
        return CLIP_Y1[clips];
    }
}
