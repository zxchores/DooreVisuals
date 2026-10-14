package dev.doorevisuals.draw;

public final class Spring {
    private float value;
    private float velocity;
    private float stiffness = 170.0F;
    private float damping = 16.0F;
    private float mass = 1.0F;
    private boolean clamp01;

    public Spring() {
    }

    public Spring(float initial) {
        this.value = initial;
    }

    public Spring(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public Spring clamp01() {
        this.clamp01 = true;
        return this;
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

    public void configure(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public float update(float target, float dt) {
        if (dt <= 0.0F) {
            return this.value;
        } else {
            dt = Math.min(dt, 0.033333335F);
            float f = (target - this.value) * this.stiffness;
            float f1 = this.velocity * this.damping;
            float f2 = (f - f1) / this.mass;
            this.velocity += f2 * dt;
            this.value = this.value + this.velocity * dt;
            if (this.clamp01) {
                this.value = Math.max(0.0F, Math.min(1.0F, this.value));
                if (this.value <= 0.0F && this.velocity < 0.0F) {
                    this.velocity = 0.0F;
                }

                if (this.value >= 1.0F && this.velocity > 0.0F) {
                    this.velocity = 0.0F;
                }
            }

            float f3 = Math.abs(target - this.value);
            if (f3 < 0.0015F && Math.abs(this.velocity) < 0.01F) {
                this.value = target;
                this.velocity = 0.0F;
            }

            return this.value;
        }
    }

    public float update60(float target) {
        return this.update(target, 0.016666668F);
    }

    public boolean settled() {
        return this.velocity == 0.0F;
    }
}
