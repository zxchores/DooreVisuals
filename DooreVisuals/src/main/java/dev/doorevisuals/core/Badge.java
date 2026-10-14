package dev.doorevisuals.core;

/** Small tag drawn next to a module name in the ClickGUI. */
public enum Badge {
    NEW("NEW", 0x34D399),
    BETA("BETA", 0xFFC94D),
    SERVER("SERVER", 0x60A5FA);

    private final String label;
    private final int rgb;

    private Badge(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
    }

    public String label() {
        return this.label;
    }

    public int rgb() {
        return this.rgb;
    }
}
