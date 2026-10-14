package dev.doorevisuals.friends;

/** Shared per-render tint state for the armor render hooks. */
public final class FriendsArmorTint {
    public static final ThreadLocal<Integer> TINT = new ThreadLocal<>();

    private FriendsArmorTint() {}
}
