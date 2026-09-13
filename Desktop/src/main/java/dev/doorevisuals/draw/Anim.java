package dev.doorevisuals.draw;

public final class Anim {
    private static volatile Anim.Motion motion = Anim.Motion.NORMAL;
    private float value;
    private float velocity;

    public Anim(float value) {
        this.value = value;
    }

    public static void setMotion(Anim.Motion m) {
        motion = m == null ? Anim.Motion.NORMAL : m;
    }

    public static Anim.Motion motion() {
        return motion;
    }

    public float value() {
        return this.value;
    }

    public float velocity() {
        return this.velocity;
    }

    public void snap(float v) {
        this.value = v;
        this.velocity = 0.0F;
    }

    public void nudge(float target, float amount) {
        float f = clamp01(amount);
        this.value = this.value + (target - this.value) * f;
        this.velocity *= 1.0F - f;
    }

    public float spring(float target, float stiffness, float damping, float dt) {
        if (dt <= 0.0F) {
            return this.value;
        } else {
            float f = Math.min(dt, 0.022222223F);
            Anim.Motion anim$motion = motion;
            float f1 = Math.max(30.0F, stiffness * anim$motion.springMul);
            float f2 = Math.max(8.0F, damping);
            int i = f1 > 500.0F ? 2 : 1;
            float f3 = f / i;

            for (int j = 0; j < i; j++) {
                float f4 = (target - this.value) * f1;
                float f5 = this.velocity * f2;
                this.velocity += (f4 - f5) * f3;
                this.value = this.value + this.velocity * f3;
            }

            if (Math.abs(target - this.value) < 0.002F && Math.abs(this.velocity) < 0.05F) {
                this.value = target;
                this.velocity = 0.0F;
            }

            return this.value;
        }
    }

    public float springHover(float target, float dt) {
        return this.spring(target, 520.0F, 34.0F, dt);
    }

    public float springToggle(float target, float dt) {
        return this.spring(target, 780.0F, 40.0F, dt);
    }

    public float springExpand(float target, float dt) {
        return this.spring(target, 360.0F, 30.0F, dt);
    }

    public float springPanel(float target, float dt) {
        return this.spring(target, 420.0F, 32.0F, dt);
    }

    public float springOpen(float target, float dt) {
        return this.spring(target, 260.0F, 16.0F, dt);
    }

    public float springPop(float target, float dt) {
        return this.spring(target, 205.0F, 13.2F, dt);
    }

    public float to(float target, float speed) {
        return this.to(target, speed, 0.016666668F);
    }

    public float to(float target, float speed, float dt) {
        if (dt <= 0.0F) {
            return this.value;
        } else {
            Anim.Motion anim$motion = motion;
            float f = Math.max(0.01F, speed * anim$motion.lerpMul);
            float f1 = 1.0F - (float)Math.exp(-f * Math.min(dt, 0.033333335F) * 2.6F);
            this.value = this.value + (target - this.value) * Math.min(1.0F, f1);
            if (Math.abs(target - this.value) < 0.0015F) {
                this.value = target;
                this.velocity = 0.0F;
            }

            return this.value;
        }
    }

    public static float timeSec() {
        return (float)System.currentTimeMillis() * 0.001F;
    }

    public static float easeOpen(float t) {
        return switch (motion) {
            case SNAPPY -> easeOutQuint(t);
            case SOFT -> easeInOut(t);
            default -> easeOut(t);
        };
    }

    public static float easeOut(float t) {
        t = clamp01(t);
        float f = 1.0F - t;
        return 1.0F - f * f * f;
    }

    public static float easeOutQuad(float t) {
        t = clamp01(t);
        float f = 1.0F - t;
        return 1.0F - f * f;
    }

    public static float easeOutQuint(float t) {
        t = clamp01(t);
        float f = 1.0F - t;
        return 1.0F - f * f * f * f * f;
    }

    public static float easeInOut(float t) {
        t = clamp01(t);
        return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
    }

    public static float easeBack(float t) {
        t = clamp01(t);
        float f = 1.70158F;
        float f1 = f + 1.0F;
        return 1.0F + f1 * (float)Math.pow(t - 1.0F, 3.0) + f * (float)Math.pow(t - 1.0F, 2.0);
    }

    public static float easeElastic(float t) {
        t = clamp01(t);
        if (t != 0.0F && t != 1.0F) {
            float f = (float) (Math.PI * 2.0 / 3.0);
            return (float)Math.pow(2.0, -10.0F * t) * (float)Math.sin((t * 10.0F - 0.75F) * f) + 1.0F;
        } else {
            return t;
        }
    }

    public static float easeLife(float life) {
        life = clamp01(life);
        return life < 0.12F ? life / 0.12F : Math.max(0.0F, (1.0F - life) / 0.88F);
    }

    public static float pulse(float hz) {
        return 0.5F + 0.5F * (float)Math.sin(timeSec() * hz * Math.PI * 2.0);
    }

    public static float breathe(float periodSec) {
        float f = Math.max(0.2F, periodSec);
        return 0.55F + 0.45F * (0.5F + 0.5F * (float)Math.sin(timeSec() / f * Math.PI * 2.0));
    }

    public static float openDurationMs() {
        return switch (motion) {
            case SNAPPY -> 90.0F;
            case SOFT -> 160.0F;
            default -> 120.0F;
        };
    }

    private static float clamp01(float t) {
        return Math.max(0.0F, Math.min(1.0F, t));
    }

    public static enum Motion {
        NORMAL(1.1F, 1.0F),
        SNAPPY(1.45F, 1.25F),
        SOFT(0.78F, 0.82F);

        final float lerpMul;
        final float springMul;

        private Motion(float lerpMul, float springMul) {
            this.lerpMul = lerpMul;
            this.springMul = springMul;
        }
    }
}
