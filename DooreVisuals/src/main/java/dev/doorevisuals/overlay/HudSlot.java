package dev.doorevisuals.overlay;

import com.google.gson.JsonObject;

public final class HudSlot {
    private final String id;
    private final String title;
    private float x;
    private float y;
    private float scale = 1.0F;
    private float w;
    private float h;
    private boolean locked;

    public HudSlot(String id, String title, float x, float y, float w, float h) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean locked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void toggleLock() {
        this.locked = !this.locked;
    }

    public String id() {
        return this.id;
    }

    public String title() {
        return this.title;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float scale() {
        return this.scale;
    }

    public float w() {
        return this.w;
    }

    public float h() {
        return this.h;
    }

    public void size(float w, float h) {
        this.w = w;
        this.h = h;
    }

    public void move(float x, float y) {
        if (!this.locked) {
            this.x = x;
            this.y = y;
        }
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.55F, Math.min(2.2F, scale));
    }

    public boolean contains(double mx, double my) {
        float f = this.w * this.scale;
        float f1 = this.h * this.scale;
        return mx >= this.x && mx <= this.x + f && my >= this.y && my <= this.y + f1;
    }

    public void write(JsonObject o) {
        o.addProperty("x", this.x);
        o.addProperty("y", this.y);
        o.addProperty("scale", this.scale);
        o.addProperty("locked", this.locked);
    }

    public void read(JsonObject o) {
        if (o != null) {
            if (o.has("x")) {
                this.x = o.get("x").getAsFloat();
            }

            if (o.has("y")) {
                this.y = o.get("y").getAsFloat();
            }

            if (o.has("scale")) {
                this.scale = Math.max(0.55F, Math.min(2.2F, o.get("scale").getAsFloat()));
            }

            if (o.has("locked")) {
                this.locked = o.get("locked").getAsBoolean();
            }
        }
    }
}
