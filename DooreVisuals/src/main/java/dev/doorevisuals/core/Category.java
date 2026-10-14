package dev.doorevisuals.core;

public enum Category {
    OVERLAY("HUD"),
    WORLD("World / FX"),
    COSMETICS("Cosmetics"),
    TOOLS("Player"),
    FRIENDS("Friends"),
    SYSTEM("Server / Misc"),
    THEME("Theme"),
    CONFIG("Config");

    private final String label;

    private Category(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
