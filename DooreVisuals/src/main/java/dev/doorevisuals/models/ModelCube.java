package dev.doorevisuals.models;

public record ModelCube(String bone, float ox, float oy, float oz, float w, float h, float d, float u, float v, float pivotX, float pivotY, float pivotZ) {
    public float cx() {
        return this.ox + this.w * 0.5F;
    }

    public float cy() {
        return this.oy + this.h * 0.5F;
    }

    public float cz() {
        return this.oz + this.d * 0.5F;
    }
}
