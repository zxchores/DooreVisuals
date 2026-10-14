package dev.doorevisuals.overlay;

public final class DeathRecap {
    private static volatile String attacker = "";
    private static volatile String weapon = "";
    private static volatile String kind = "";

    private DeathRecap() {
    }

    public static void record(String who, String with, String how) {
        attacker = who == null ? "" : who;
        weapon = with == null ? "" : with;
        kind = how == null ? "" : how;
    }

    public static String attacker() {
        return attacker;
    }

    public static String weapon() {
        return weapon;
    }

    public static String kind() {
        return kind;
    }

    public static boolean any() {
        return !attacker.isBlank() || !weapon.isBlank() || !kind.isBlank();
    }
}
