package dev.doorevisuals.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Hand poses for knives in first person, in the spirit of CS2: a flourish when the knife is drawn, a slight sway while it
 * is held, and a different inspect routine for every knife.
 *
 * <p>A pose is six numbers: translation in blocks (x, y, z) and rotation in degrees (around Y, X, Z, in the order the hand
 * is turned). Inspect routines are keyframes, and the pose between them is a smooth cubic curve, so the motion has no
 * corners. Every routine starts and ends at the neutral pose; angles may end on a full turn, which is the same pose.</p>
 */
public final class KnifeAnim {
    /** Rows are {time 0..1, dx, dy, dz, ry, rx, rz}. */
    private static final Map<String, float[][]> INSPECT = new HashMap<>();
    private static final float[][] MAGIC = {
        {0.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
        {0.15F, 0.00F, 0.05F, 0.00F, 40.0F, -12.0F, 0.0F},
        {0.35F, 0.00F, 0.10F, 0.00F, 160.0F, -30.0F, 14.0F},
        {0.55F, 0.00F, 0.12F, 0.00F, 300.0F, -18.0F, -12.0F},
        {0.78F, 0.00F, 0.04F, 0.00F, 350.0F, -6.0F, 4.0F},
        {1.00F, 0.00F, 0.00F, 0.00F, 360.0F, 0.0F, 0.0F},
    };

    static {
        // M9: the knife is tossed up, turns over in the air and is caught.
        INSPECT.put("М9", new float[][] {
            {0.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
            {0.10F, 0.00F, 0.02F, 0.00F, 0.0F, -18.0F, 6.0F},
            {0.28F, 0.01F, 0.12F, -0.05F, 10.0F, -190.0F, 12.0F},
            {0.48F, 0.00F, 0.09F, -0.03F, 60.0F, -360.0F, 0.0F},
            {0.68F, 0.00F, 0.03F, 0.00F, 200.0F, -360.0F, -8.0F},
            {0.86F, 0.00F, 0.005F, 0.00F, 340.0F, -360.0F, 3.0F},
            {1.00F, 0.00F, 0.00F, 0.00F, 360.0F, -360.0F, 0.0F},
        });
        // Karambit: spun around the finger ring, two full turns.
        INSPECT.put("Карамбит", new float[][] {
            {0.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
            {0.12F, 0.02F, 0.03F, 0.00F, 0.0F, -10.0F, 40.0F},
            {0.35F, 0.06F, 0.05F, -0.02F, 0.0F, -15.0F, 270.0F},
            {0.58F, -0.04F, 0.04F, -0.02F, 0.0F, -10.0F, 520.0F},
            {0.80F, 0.02F, 0.02F, 0.00F, 0.0F, -4.0F, 690.0F},
            {1.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 720.0F},
        });
        // Butterfly: two quick flips with a hop.
        INSPECT.put("Бабочка", new float[][] {
            {0.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
            {0.10F, 0.00F, 0.02F, 0.00F, 0.0F, -30.0F, 0.0F},
            {0.30F, 0.00F, 0.10F, -0.02F, 0.0F, -300.0F, 8.0F},
            {0.50F, 0.00F, 0.06F, -0.02F, 0.0F, -560.0F, -6.0F},
            {0.72F, 0.00F, 0.02F, 0.00F, 0.0F, -700.0F, 0.0F},
            {1.00F, 0.00F, 0.00F, 0.00F, 0.0F, -720.0F, 0.0F},
        });
        // Bayonet: a slow, heavy turn that shows both sides of the blade.
        INSPECT.put("Штык-нож", new float[][] {
            {0.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
            {0.15F, 0.00F, 0.03F, 0.00F, -40.0F, 0.0F, 8.0F},
            {0.40F, 0.00F, 0.05F, 0.00F, -150.0F, 0.0F, 22.0F},
            {0.65F, 0.00F, 0.04F, 0.00F, -250.0F, 12.0F, 18.0F},
            {0.85F, 0.00F, 0.01F, 0.00F, -340.0F, 0.0F, 4.0F},
            {1.00F, 0.00F, 0.00F, 0.00F, -360.0F, 0.0F, 0.0F},
        });
        // Hunter: raised to the light, held for a moment, lowered.
        INSPECT.put("Охотничий", new float[][] {
            {0.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
            {0.18F, 0.00F, 0.02F, 0.00F, -50.0F, 0.0F, 12.0F},
            {0.40F, 0.00F, 0.04F, 0.00F, -85.0F, 18.0F, 30.0F},
            {0.62F, 0.00F, 0.04F, 0.00F, -85.0F, 18.0F, 30.0F},
            {0.85F, 0.00F, 0.01F, 0.00F, -20.0F, 4.0F, 6.0F},
            {1.00F, 0.00F, 0.00F, 0.00F, 0.0F, 0.0F, 0.0F},
        });
        INSPECT.put("Арканум", MAGIC);
        INSPECT.put("Пламенный", MAGIC);
        INSPECT.put("Демонический", MAGIC);
    }

    private KnifeAnim() {
    }

    /** True when the model has its own inspect routine. */
    public static boolean hasInspect(String model) {
        return INSPECT.containsKey(model);
    }

    /** Pose of the model's own inspect routine at linear progress t, 0..1. */
    public static float[] inspect(String model, float t) {
        float[][] rows = INSPECT.get(model);
        return rows == null ? new float[6] : sample(rows, t);
    }

    /** The three generic styles, as they were before: only rotations. */
    public static float[] generic(String style, float t) {
        float wave = (float)Math.sin(t * Math.PI);
        return switch (style) {
            case "Переворот" -> new float[] {0.0F, 0.0F, 0.0F, 0.0F, -360.0F * t, 24.0F * wave};
            case "Восьмёрка" -> new float[] {0.0F, 0.0F, 0.0F, 120.0F * wave, 62.0F * (float)Math.sin(t * Math.PI * 2.0), -28.0F * wave};
            default -> new float[] {0.0F, 0.0F, 0.0F, 350.0F * t, 18.0F * wave, -42.0F * wave};
        };
    }

    /**
     * The flourish while the knife is raised: equipProgress is 1 when it is still lowered and 0 when it is in place, so
     * the pose is neutral at the end. Squared, so most of the turn happens early and it settles gently.
     */
    public static float[] draw(String model, float equipProgress) {
        float e = Math.max(0.0F, Math.min(1.0F, equipProgress));
        float s = e * e;
        return switch (model) {
            case "Бабочка" -> new float[] {0.0F, 0.0F, 0.0F, 0.0F, -360.0F * s, 10.0F * s};
            case "Карамбит" -> new float[] {0.0F, 0.0F, 0.0F, 0.0F, -20.0F * s, 540.0F * s};
            case "М9" -> new float[] {0.0F, 0.0F, 0.0F, 180.0F * s, -60.0F * s, -14.0F * s};
            case "Штык-нож", "Охотничий" -> new float[] {0.0F, 0.0F, 0.0F, -90.0F * s, -30.0F * s, 20.0F * s};
            default -> new float[] {0.0F, 0.03F * s, 0.0F, 120.0F * s, -24.0F * s, 12.0F * s};
        };
    }

    /** A slight breathing sway of the held knife; the arguments are seconds. */
    public static float[] idle(float seconds) {
        float t = seconds;
        return new float[] {
            (float)Math.sin(t * 0.6) * 0.002F,
            (float)Math.sin(t * 0.9) * 0.003F,
            0.0F,
            (float)Math.sin(t * 0.5 + 2.0) * 0.8F,
            (float)Math.sin(t * 0.8 + 1.0) * 0.7F,
            (float)Math.sin(t * 1.1) * 0.9F,
        };
    }

    /** Cubic Hermite through the keyframes, with tangents from the neighbouring keys and zero speed at both ends. */
    private static float[] sample(float[][] rows, float t) {
        float x = Math.max(0.0F, Math.min(1.0F, t));
        int i = 0;
        while (i < rows.length - 2 && x > rows[i + 1][0]) {
            i++;
        }
        float t0 = rows[i][0];
        float t1 = rows[i + 1][0];
        float h = Math.max(1.0E-4F, t1 - t0);
        float u = (x - t0) / h;
        float u2 = u * u;
        float u3 = u2 * u;
        float h00 = 2 * u3 - 3 * u2 + 1;
        float h10 = u3 - 2 * u2 + u;
        float h01 = -2 * u3 + 3 * u2;
        float h11 = u3 - u2;
        float[] out = new float[6];
        for (int c = 1; c <= 6; c++) {
            float m0 = tangent(rows, i, c);
            float m1 = tangent(rows, i + 1, c);
            out[c - 1] = h00 * rows[i][c] + h10 * h * m0 + h01 * rows[i + 1][c] + h11 * h * m1;
        }
        return out;
    }

    private static float tangent(float[][] rows, int k, int c) {
        if (k <= 0 || k >= rows.length - 1) {
            return 0.0F;
        }
        return (rows[k + 1][c] - rows[k - 1][c]) / (rows[k + 1][0] - rows[k - 1][0]);
    }
}
