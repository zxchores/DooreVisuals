package dev.doorevisuals.draw;

public enum Skin {
    DOORE("Doore", -12533600, -8458284, -15046056, -16378350),
    WEX("Wex", -11885569, -7550721, -15054214, -16116448),
    ICE("\u041b\u0451\u0434", -10565377, -6627073, -15050118, -16379880),
    TOXIC("\u0422\u043e\u043a\u0441\u0438\u043a", -8585414, -4784262, -12944872, -16116730),
    SUNSET("\u0417\u0430\u043a\u0430\u0442", -34246, -20374, -7718376, -15070202),
    VIOLET("\u0424\u0438\u043e\u043b\u0435\u0442", -5215489, -3104513, -10868086, -15595494),
    MONO("\u041c\u043e\u043d\u043e", -1513236, -1, -9803150, -15724524),
    CUSTOM("\u0421\u0432\u043e\u044f", -12533600, -8458284, -15046056, -16378350);

    private final String label;
    private final int accent;
    private final int accentHot;
    private final int accentDim;
    private final int accentDeep;

    private Skin(String label, int accent, int accentHot, int accentDim, int accentDeep) {
        this.label = label;
        this.accent = accent;
        this.accentHot = accentHot;
        this.accentDim = accentDim;
        this.accentDeep = accentDeep;
    }

    public String label() {
        return this.label;
    }

    public int accent() {
        return this.accent;
    }

    public void apply() {
        Theme.applyAccent(this.accent, this.accentHot, this.accentDim, this.accentDeep);
    }

    public static Skin byLabel(String label) {
        for (Skin skin : values()) {
            if (skin.label.equals(label) || skin.name().equalsIgnoreCase(label)) {
                return skin;
            }
        }

        return DOORE;
    }

    public static String[] labels() {
        Skin[] askin = values();
        String[] astring = new String[askin.length];

        for (int i = 0; i < askin.length; i++) {
            astring[i] = askin[i].label;
        }

        return astring;
    }
}
