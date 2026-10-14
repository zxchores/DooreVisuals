package dev.doorevisuals.draw;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.doorevisuals.mix.DrawContextAccessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

/**
 * Rounded rectangles, strokes, gradients and shadows drawn from a signed distance field instead of
 * NanoVG.
 *
 * <p>Each shape is split into its four quadrants. Inside one quadrant {@code |p| - (half - radius)}
 * is a linear function of position, so it can ride along in the texture coordinate and the fragment
 * stage gets the exact distance to the rounded outline without needing a single per-shape uniform.
 * Corner radius and border width travel in the spare short pair, which is why the pipeline asks for
 * {@code POSITION_TEXTURE_COLOR_LIGHT}.
 *
 * <p>Shapes land in a {@link Batch}, itself a GUI render state. The vanilla GUI renderer merges
 * neighbouring elements that share a pipeline into one draw, so a whole HUD normally costs one call.
 */
public final class Sdf {
    private static final int SHORT_MAX = 32767;
    private static final float UNIT = 16.0F;
    private static final int VERTS_PER_SHAPE = 16;
    private static final int MAX_VERTS = 1 << 16;
    private static boolean dead;
    private static Sdf.Batch bound;

    private Sdf() {
    }

    public static boolean ok() {
        return !dead;
    }

    /** True while a batch collects shapes, which is the case inside every {@link Ui#frame} block. */
    public static boolean batching() {
        return !dead && bound != null;
    }

    /**
     * Registers a batch that gathers every shape until {@link #close()}. {@link Ui#frame} calls this
     * before the NanoVG element of the same block, which is what puts the shapes under its text.
     */
    static Sdf.Batch open(DrawContext g) {
        if (dead || g == null) {
            return null;
        } else {
            try {
                Sdf.Batch sdf$batch = new Sdf.Batch(fullScreen(g), 1024).capture(g.getMatrices());
                state(g).addSimpleElement(sdf$batch);
                return sdf$batch;
            } catch (Throwable throwable) {
                trip(throwable);
                return null;
            }
        }
    }

    static void bind(Sdf.Batch batch) {
        bound = batch;
    }

    static Sdf.Batch detach() {
        Sdf.Batch sdf$batch = bound;
        bound = null;
        return sdf$batch;
    }

    static void close() {
        bound = null;
    }

    public static void fill(DrawContext g, float x, float y, float w, float h, int color, float radius) {
        shape(g, x, y, w, h, color, color, true, radius, 0.0F, 0.0F);
    }

    public static void grad(DrawContext g, float x, float y, float w, float h, int a, int b, float radius, boolean vertical) {
        shape(g, x, y, w, h, a, b, vertical, radius, 0.0F, 0.0F);
    }

    public static void stroke(DrawContext g, float x, float y, float w, float h, int color, float radius, float thickness) {
        shape(g, x, y, w, h, color, color, true, radius, Math.max(0.35F, thickness), 0.0F);
    }

    public static void shadow(DrawContext g, float x, float y, float w, float h, int color, float radius, float blur) {
        shape(g, x, y, w, h, color, color, true, radius, 0.0F, Math.max(0.5F, blur));
    }

    public static void circle(DrawContext g, float cx, float cy, float r, int color) {
        if (!(r <= 0.0F)) {
            shape(g, cx - r, cy - r, r * 2.0F, r * 2.0F, color, color, true, r, 0.0F, 0.0F);
        }
    }

    public static void ring(DrawContext g, float cx, float cy, float r, float thickness, int color) {
        if (!(r <= 0.0F)) {
            shape(g, cx - r, cy - r, r * 2.0F, r * 2.0F, color, color, true, r, Math.max(0.35F, thickness), 0.0F);
        }
    }

    private static void shape(
        DrawContext g, float x, float y, float w, float h, int colorA, int colorB, boolean vertical, float radius, float border, float blur
    ) {
        if (!dead && (g != null || bound != null) && !(w <= 0.0F) && !(h <= 0.0F)) {
            float f = Ui.alpha();
            int i = fade(colorA, f);
            int j = colorB == colorA ? i : fade(colorB, f);
            if ((i & 0xFF000000) != 0 || (j & 0xFF000000) != 0) {
                try {
                    emit(g, x, y, w, h, i, j, vertical, radius, border, blur);
                } catch (Throwable throwable) {
                    trip(throwable);
                }
            }
        }
    }

    private static void emit(
        DrawContext g, float x, float y, float w, float h, int colorA, int colorB, boolean vertical, float radius, float border, float blur
    ) {
        Matrix3x2fc matrix3x2fc = pose(g);
        if (matrix3x2fc != null) {
            float f = Ui.scaleX() * matrix3x2fc.m00();
            float f1 = Ui.scaleY() * matrix3x2fc.m11();
            float f2 = Ui.moveX() * matrix3x2fc.m00() + matrix3x2fc.m20();
            float f3 = Ui.moveY() * matrix3x2fc.m11() + matrix3x2fc.m21();
            float f4 = Math.min(x * f + f2, (x + w) * f + f2);
            float f5 = Math.min(y * f1 + f3, (y + h) * f1 + f3);
            float f6 = Math.max(x * f + f2, (x + w) * f + f2);
            float f7 = Math.max(y * f1 + f3, (y + h) * f1 + f3);
            float f8 = Math.min(Math.abs(f), Math.abs(f1));
            if (!(f6 - f4 < 0.04F) && !(f7 - f5 < 0.04F)) {
                float f9 = (f4 + f6) * 0.5F;
                float f10 = (f5 + f7) * 0.5F;
                float f11 = (f6 - f4) * 0.5F;
                float f12 = (f7 - f5) * 0.5F;
                float f13 = Math.max(0.0F, Math.min(radius * f8, Math.min(f11, f12)));
                float f14 = blur > 0.0F ? blur * f8 : 0.0F;
                int i = clampShort(Math.round(f13 * UNIT));
                int j = f14 > 0.0F
                    ? -Math.max(1, clampShort(Math.round(f14 * UNIT)))
                    : (border > 0.0F ? Math.max(1, clampShort(Math.round(border * f8 * UNIT))) : 0);

                // The field reaches zero on the outline itself, so the quad has to run a pixel past it
                // for the antialiased edge, and past the blur radius when the shape is a shadow.
                float f15 = f14 + 1.0F;
                Sdf.Batch sdf$batch = target(g, f4 - f15, f5 - f15, f6 + f15, f7 + f15);
                if (sdf$batch != null) {
                    float f16 = f11 - f13;
                    float f17 = f12 - f13;
                    float f28 = Ui.clipLeft() * matrix3x2fc.m00() + matrix3x2fc.m20();
                    float f29 = Ui.clipRight() * matrix3x2fc.m00() + matrix3x2fc.m20();
                    float f30 = Ui.clipTop() * matrix3x2fc.m11() + matrix3x2fc.m21();
                    float f31 = Ui.clipBottom() * matrix3x2fc.m11() + matrix3x2fc.m21();
                    float f32 = Math.min(f28, f29);
                    float f33 = Math.max(f28, f29);
                    float f34 = Math.min(f30, f31);
                    float f35 = Math.max(f30, f31);

                    for (int k = 0; k < 4; k++) {
                        float f18 = (k & 1) == 0 ? -1.0F : 1.0F;
                        float f19 = (k & 2) == 0 ? -1.0F : 1.0F;
                        float f20 = Math.max(Math.min(f9, f9 + f18 * (f11 + f15)), f32);
                        float f21 = Math.max(Math.min(f10, f10 + f19 * (f12 + f15)), f34);
                        float f22 = Math.min(Math.max(f9, f9 + f18 * (f11 + f15)), f33);
                        float f23 = Math.min(Math.max(f10, f10 + f19 * (f12 + f15)), f35);
                        if (!(f22 - f20 <= 0.002F) && !(f23 - f21 <= 0.002F)) {
                            float f24 = f18 * (f20 - f9) - f16;
                            float f25 = f19 * (f21 - f10) - f17;
                            float f26 = f18 * (f22 - f9) - f16;
                            float f27 = f19 * (f23 - f10) - f17;
                            int l = tint(colorA, colorB, vertical, f20, f21, f4, f5, f6, f7);
                            int i1 = tint(colorA, colorB, vertical, f20, f23, f4, f5, f6, f7);
                            int j1 = tint(colorA, colorB, vertical, f22, f23, f4, f5, f6, f7);
                            int k1 = tint(colorA, colorB, vertical, f22, f21, f4, f5, f6, f7);
                            sdf$batch.vertex(f20, f21, f24, f25, l, i, j);
                            sdf$batch.vertex(f20, f23, f24, f27, i1, i, j);
                            sdf$batch.vertex(f22, f23, f26, f27, j1, i, j);
                            sdf$batch.vertex(f22, f21, f26, f25, k1, i, j);
                        }
                    }
                }
            }
        }
    }

    private static int tint(int a, int b, boolean vertical, float px, float py, float x0, float y0, float x1, float y1) {
        if (a == b) {
            return a;
        } else {
            float f = vertical ? y1 - y0 : x1 - x0;
            float f1 = f <= 0.002F ? 0.0F : Math.max(0.0F, Math.min(1.0F, ((vertical ? py - y0 : px - x0) / f)));
            return Theme.lerp(a, b, f1);
        }
    }

    private static int fade(int argb, float alpha) {
        return alpha >= 0.999F ? argb : Theme.alpha(argb, Math.round((argb >>> 24) * Math.max(0.0F, alpha)));
    }

    private static Sdf.Batch target(DrawContext g, float x0, float y0, float x1, float y1) {
        Sdf.Batch sdf$batch = bound;
        if (sdf$batch != null) {
            return sdf$batch.count + VERTS_PER_SHAPE > MAX_VERTS ? null : sdf$batch;
        } else {
            // Outside a batch the caller is drawing straight onto the context, where every other
            // element is one render state as well, so a shape becomes its own and keeps its place in
            // the layer order.
            Sdf.Batch sdf$batch1 = new Sdf.Batch(rect(x0, y0, x1, y1), VERTS_PER_SHAPE);
            state(g).addSimpleElement(sdf$batch1);
            return sdf$batch1;
        }
    }

    /**
     * The pose is folded into the vertices here instead of being sent to the GPU, because the field is
     * measured in screen pixels. A rotated pose would tilt it off the pixel grid, so those callers get
     * nothing back and fall through to the older backend.
     */
    private static Matrix3x2fc pose(DrawContext g) {
        Sdf.Batch sdf$batch = bound;
        Matrix3x2fc matrix3x2fc = sdf$batch != null ? sdf$batch.pose : g.getMatrices();
        return Math.abs(matrix3x2fc.m01()) > 1.0E-4F || Math.abs(matrix3x2fc.m10()) > 1.0E-4F ? null : matrix3x2fc;
    }

    private static GuiRenderState state(DrawContext g) {
        return ((DrawContextAccessor)g).doore$state();
    }

    private static ScreenRect fullScreen(DrawContext g) {
        return new ScreenRect(0, 0, Math.max(1, g.getScaledWindowWidth()), Math.max(1, g.getScaledWindowHeight()));
    }

    private static ScreenRect rect(float x0, float y0, float x1, float y1) {
        int i = (int)Math.floor(x0);
        int j = (int)Math.floor(y0);
        return new ScreenRect(i, j, Math.max(1, (int)Math.ceil(x1) - i), Math.max(1, (int)Math.ceil(y1) - j));
    }

    private static int clampShort(int v) {
        return Math.max(0, Math.min(SHORT_MAX, v));
    }

    private static void trip(Throwable t) {
        if (!dead) {
            dead = true;
            bound = null;
            t.printStackTrace();
        }
    }

    static final class Batch implements SimpleGuiElementRenderState {
        private final ScreenRect bounds;
        final Matrix3x2f pose = new Matrix3x2f();
        private float[] geometry;
        private int[] attributes;
        int count;

        Batch(ScreenRect bounds, int verts) {
            this.bounds = bounds;
            this.geometry = new float[verts * 4];
            this.attributes = new int[verts * 3];
        }

        Sdf.Batch capture(Matrix3x2fc from) {
            this.pose.set(from);
            return this;
        }

        void vertex(float x, float y, float u, float v, int color, int radius, int param) {
            if ((this.count + 1) * 4 > this.geometry.length) {
                this.grow();
            }

            int i = this.count * 4;
            this.geometry[i] = x;
            this.geometry[i + 1] = y;
            this.geometry[i + 2] = u;
            this.geometry[i + 3] = v;
            int j = this.count * 3;
            this.attributes[j] = color;
            this.attributes[j + 1] = radius;
            this.attributes[j + 2] = param;
            this.count++;
        }

        private void grow() {
            int i = Math.min(MAX_VERTS, Math.max(this.geometry.length / 4 * 2, this.count + VERTS_PER_SHAPE));
            float[] afloat = new float[i * 4];
            int[] aint = new int[i * 3];
            System.arraycopy(this.geometry, 0, afloat, 0, Math.min(this.geometry.length, afloat.length));
            System.arraycopy(this.attributes, 0, aint, 0, Math.min(this.attributes.length, aint.length));
            this.geometry = afloat;
            this.attributes = aint;
        }

        @Override
        public void setupVertices(VertexConsumer vertices) {
            for (int i = 0; i < this.count; i++) {
                int j = i * 4;
                int k = i * 3;
                vertices.vertex(this.geometry[j], this.geometry[j + 1], 0.0F)
                    .texture(this.geometry[j + 2], this.geometry[j + 3])
                    .color(this.attributes[k])
                    .light(this.attributes[k + 1], this.attributes[k + 2]);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return Gpu.UI_RECT_PIPE;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.empty();
        }

        @Override
        public ScreenRect scissorArea() {
            return null;
        }

        @Override
        public ScreenRect bounds() {
            return this.bounds;
        }
    }
}
