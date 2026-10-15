package dev.doorevisuals.tools;

import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * The poses of the first-person swing. Every mode is a function of the swing progress p (0 at the start of the swing,
 * 1 at its end) that is exactly zero at both ends, so the hand leaves the resting pose and comes back to it without a
 * jump. The angles are relative to the resting pose and scale with the strength; {@code i} is +1 for the right arm and
 * -1 for the left one, which mirrors the motion.
 *
 * <p>Axes in the view space: a negative X rotation pitches the blade away from the player (a chop), a positive Y
 * rotation sweeps it to the left, a positive Z rotation rolls its tip to the left.</p>
 */
public final class SwingAnimations {
    public static final String VANILLA = "Vanilla";
    public static final List<String> TYPES = List.of(
            VANILLA, "Mod 1", "Mod 2", "Mod 3", "Mod 4", "Mod 5", "Mod 6", "Mod 7", "Mod 8", "Mod 9", "Mod 10", "Mod 11");

    private SwingAnimations() {
    }

    /** Applies the pose of the given type to the matrix stack. Does nothing for Vanilla and for an unknown type. */
    public static void apply(MatrixStack pose, Arm arm, float progress, String type, float strength) {
        float p = MathHelper.clamp(progress, 0.0F, 1.0F);
        if (p <= 0.0F || p >= 1.0F || strength <= 0.0F) {
            return;
        }
        float i = arm == Arm.LEFT ? -1.0F : 1.0F;
        float k = strength / 3.0F;
        switch (type) {
            case "Mod 1" -> {
                // Quick horizontal slash: the blade whips across to the left and settles slowly.
                float s = snap(p);
                move(pose, i * 0.10F * k * s, 0.0F, -0.06F * k * s);
                turn(pose, i * 58.0F * k * s, -10.0F * k * s, i * 22.0F * k * s);
            }
            case "Mod 2" -> {
                // Diagonal cut from the upper right to the lower left.
                float a = arch(p);
                move(pose, i * -0.04F * k * a, 0.05F * k * a, -0.08F * k * a);
                turn(pose, i * 30.0F * k * a, -62.0F * k * a, i * -48.0F * k * a);
            }
            case "Mod 3" -> {
                // Overhead chop: the blade goes up first, then comes down hard.
                float w = phase(p, 0.0F, 0.34F);
                float d = phase(p, 0.28F, 1.0F);
                move(pose, 0.0F, (0.10F * w - 0.13F * d) * k, -0.14F * d * k);
                turn(pose, i * 8.0F * k * d, (38.0F * w - 96.0F * d) * k, i * -6.0F * k * d);
            }
            case "Mod 4" -> {
                // Wide side swing: a long wind-up to the right, then a sweep right across the screen.
                float w = phase(p, 0.0F, 0.32F);
                float d = phase(p, 0.26F, 1.0F);
                move(pose, i * (0.14F * w - 0.30F * d) * k, 0.02F * k * d, -0.10F * k * d);
                turn(pose, i * (-34.0F * w + 88.0F * d) * k, -22.0F * k * d, i * 18.0F * k * d);
            }
            case "Mod 5" -> {
                // Backhand: the blade swings the other way, from left to right, with a roll.
                float a = arch(p);
                move(pose, i * 0.13F * k * a, 0.0F, -0.08F * k * a);
                turn(pose, i * -66.0F * k * a, -34.0F * k * a, i * -34.0F * k * a);
            }
            case "Mod 6" -> {
                // Smooth arc: one long wave, the blade swings out and comes back along the same curve.
                float a = arch(p);
                float w = (float)Math.sin(p * Math.PI * 2.0);
                move(pose, i * 0.06F * k * w, 0.03F * k * a, -0.05F * k * a);
                turn(pose, i * 42.0F * k * w, -58.0F * k * a, i * -26.0F * k * a);
            }
            case "Mod 7" -> {
                // Short sharp jab: slow pull back, a sudden thrust forward at the end.
                float j = late(p);
                float b = phase(p, 0.0F, 0.4F) * (1.0F - phase(p, 0.4F, 0.55F));
                move(pose, 0.0F, 0.02F * k * j, (0.05F * b - 0.38F * j) * k);
                turn(pose, i * 4.0F * k * j, (-16.0F * j + 6.0F * b) * k, 0.0F);
            }
            case "Mod 8" -> {
                // Spin: the weapon rolls once around its own axis (a whole turn, so it ends where it started).
                float t = p * p * (3.0F - 2.0F * p);
                float a = arch(p);
                move(pose, 0.0F, 0.0F, -0.08F * k * a);
                turn(pose, 0.0F, -34.0F * k * a, i * -360.0F * t);
            }
            case "Mod 9" -> {
                // Low sweep: from under the hand up and across, like an uppercut.
                float a = arch(p);
                float s = snap(p);
                move(pose, i * -0.04F * k * s, -0.16F * k * a, -0.06F * k * s);
                turn(pose, i * 34.0F * k * s, 52.0F * k * a, i * 40.0F * k * s);
            }
            case "Mod 10" -> {
                // Heavy tilt: the weapon is laid over on its side and brought down with weight.
                float a = arch(p);
                float h = phase(p, 0.15F, 1.0F);
                move(pose, i * 0.06F * k * a, -0.05F * k * a, -0.12F * k * h);
                turn(pose, i * -52.0F * k * a, -38.0F * k * a - 20.0F * k * h, i * -92.0F * k * a);
            }
            case "Mod 11" -> {
                // Combination: a horizontal slash first, then a diagonal strike on the way back, with a jab at the end.
                float p1 = phase(p, 0.0F, 0.5F);
                float p2 = phase(p, 0.5F, 0.86F);
                float p3 = phase(p, 0.78F, 1.0F);
                move(pose, i * (0.07F * p1 - 0.05F * p2) * k, 0.03F * k * p2, -0.28F * k * p3 - 0.06F * k * p2);
                turn(pose, i * (52.0F * p1 - 40.0F * p2) * k, (-12.0F * p1 - 78.0F * p2 - 20.0F * p3) * k, i * (24.0F * p1 - 52.0F * p2) * k);
            }
            default -> {
            }
        }
    }

    /** The Mod types apply to the stack and the empty hand alike; Vanilla leaves the game's own swing alone. */
    public static boolean custom(String type) {
        return type != null && !VANILLA.equals(type) && TYPES.contains(type);
    }

    // ------------------------------------------------------------------ curves, all zero at 0 and 1

    /** A half sine: 0 → 1 → 0. */
    private static float arch(float p) {
        return (float)Math.sin(p * Math.PI);
    }

    /** Fast out, long settle. */
    private static float snap(float p) {
        return (float)Math.sin(Math.pow(p, 0.55) * Math.PI);
    }

    /** Slow start, sharp finish. */
    private static float late(float p) {
        return (float)Math.sin(Math.pow(p, 1.8) * Math.PI);
    }

    /** A bump that exists only between a and b (0 outside, 1 in the middle of it). */
    private static float phase(float p, float a, float b) {
        if (p <= a || p >= b) {
            return 0.0F;
        }
        return (float)Math.sin((p - a) / (b - a) * Math.PI);
    }

    private static void move(MatrixStack pose, float x, float y, float z) {
        pose.translate(x, y, z);
    }

    /** Yaw (Y), pitch (X) and roll (Z), in degrees, applied in that order. */
    private static void turn(MatrixStack pose, float yaw, float pitch, float roll) {
        if (yaw != 0.0F) {
            pose.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        }
        if (pitch != 0.0F) {
            pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        }
        if (roll != 0.0F) {
            pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        }
    }
}
