package dev.doorevisuals.data;

import com.google.gson.JsonObject;
import dev.doorevisuals.core.Category;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;

public final class GuiLayout {
    private static final int PANEL_VER = 4;
    public static final Category[] MAIN_CATS = new Category[]{Category.OVERLAY, Category.WORLD, Category.TOOLS, Category.SYSTEM};
    private static final Category[] DEFAULT_DOCK = new Category[]{Category.COSMETICS, Category.FRIENDS, Category.CONFIG};
    private static final EnumMap<Category, GuiLayout.Panel> PANELS = new EnumMap<>(Category.class);
    public static boolean showTheme = true;
    public static boolean showConfig = true;
    public static boolean uniformPanels = false;
    public static boolean showProfileAvatar = true;
    public static boolean showProfileNick = true;
    public static boolean snapPanels = true;
    public static float navX = 36.0F;
    public static float navY = 36.0F;
    public static float bodyX = 248.0F;
    public static float bodyY = 36.0F;
    private static Category selected = Category.OVERLAY;
    private static float scale = 1.0F;
    private static String bannerId = "builtin:cycle";
    public static boolean bannerLabels = true;
    private static final List<Category> Z_ORDER = new ArrayList<>();
    private static final List<Category> DOCK = new ArrayList<>();

    private GuiLayout() {
    }

    public static GuiLayout.Panel get(Category c) {
        return PANELS.computeIfAbsent(c, cat -> defaultPos(cat));
    }

    public static void put(Category c, float x, float y, float scroll) {
        GuiLayout.Panel guilayout$panel = get(c);
        guilayout$panel.x = x;
        guilayout$panel.y = y;
        guilayout$panel.scroll = scroll;
    }

    public static Category selected() {
        return selected;
    }

    public static void select(Category category) {
        selected = category == null ? Category.OVERLAY : category;
        bringFront(selected);
    }

    public static void bringFront(Category category) {
        if (category != null) {
            Z_ORDER.remove(category);
            Z_ORDER.add(category);
            int i = 0;

            for (Category categoryx : Z_ORDER) {
                get(categoryx).z = i++;
            }
        }
    }

    public static List<Category> zOrder() {
        ensureZ();
        return List.copyOf(Z_ORDER);
    }

    public static List<Category> dockOrder() {
        ensureDock();
        return List.copyOf(DOCK);
    }

    public static List<Category> dockVisible() {
        List<Category> list = new ArrayList<>();

        for (Category category : dockOrder()) {
            if (category != Category.CONFIG || showConfig) {
                list.add(category);
            }
        }

        return list;
    }

    public static void moveDock(int from, int to) {
        ensureDock();
        if (from >= 0 && to >= 0 && from < DOCK.size() && to < DOCK.size() && from != to) {
            Category category = DOCK.remove(from);
            DOCK.add(to, category);
        }
    }

    public static float scale() {
        return scale;
    }

    public static void scale(float value) {
        scale = Math.max(0.7F, Math.min(1.5F, value));
    }

    public static String bannerId() {
        return bannerId;
    }

    public static void bannerId(String id) {
        bannerId = id != null && !id.isBlank() ? id : "builtin:cycle";
        BannerLibrary.read(bannerId);
    }

    public static void write(JsonObject into) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("panelVer", 4);
        jsonobject.addProperty("showTheme", showTheme);
        jsonobject.addProperty("showConfig", showConfig);
        jsonobject.addProperty("uniformPanels", uniformPanels);
        jsonobject.addProperty("showProfileAvatar", showProfileAvatar);
        jsonobject.addProperty("showProfileNick", showProfileNick);
        jsonobject.addProperty("snapPanels", snapPanels);
        jsonobject.addProperty("navX", navX);
        jsonobject.addProperty("navY", navY);
        jsonobject.addProperty("bodyX", bodyX);
        jsonobject.addProperty("bodyY", bodyY);
        jsonobject.addProperty("selected", selected.name());
        jsonobject.addProperty("scale", scale);
        jsonobject.addProperty("bannerId", bannerId);
        jsonobject.addProperty("bannerLabels", bannerLabels);
        StringBuilder stringbuilder = new StringBuilder();

        for (Category category : zOrder()) {
            if (!stringbuilder.isEmpty()) {
                stringbuilder.append(',');
            }

            stringbuilder.append(category.name());
        }

        jsonobject.addProperty("zOrder", stringbuilder.toString());
        StringBuilder stringbuilder1 = new StringBuilder();

        for (Category category1 : dockOrder()) {
            if (!stringbuilder1.isEmpty()) {
                stringbuilder1.append(',');
            }

            stringbuilder1.append(category1.name());
        }

        jsonobject.addProperty("dockOrder", stringbuilder1.toString());

        for (Entry<Category, GuiLayout.Panel> entry : PANELS.entrySet()) {
            JsonObject jsonobject1 = new JsonObject();
            jsonobject1.addProperty("x", entry.getValue().x);
            jsonobject1.addProperty("y", entry.getValue().y);
            jsonobject1.addProperty("scroll", entry.getValue().scroll);
            jsonobject1.addProperty("z", entry.getValue().z);
            jsonobject.add(entry.getKey().name(), jsonobject1);
        }

        into.add("clickgui", jsonobject);
    }

    public static void read(JsonObject layout) {
        if (layout != null && layout.has("clickgui")) {
            JsonObject jsonobject = layout.getAsJsonObject("clickgui");
            int i = jsonobject.has("panelVer") ? jsonobject.get("panelVer").getAsInt() : 0;
            if (jsonobject.has("showTheme")) {
                showTheme = jsonobject.get("showTheme").getAsBoolean();
            }

            if (jsonobject.has("showConfig")) {
                showConfig = jsonobject.get("showConfig").getAsBoolean();
            }

            if (jsonobject.has("uniformPanels")) {
                uniformPanels = jsonobject.get("uniformPanels").getAsBoolean();
            }

            if (jsonobject.has("showProfileAvatar")) {
                showProfileAvatar = jsonobject.get("showProfileAvatar").getAsBoolean();
            }

            if (jsonobject.has("showProfileNick")) {
                showProfileNick = jsonobject.get("showProfileNick").getAsBoolean();
            }

            if (jsonobject.has("snapPanels")) {
                snapPanels = jsonobject.get("snapPanels").getAsBoolean();
            }

            if (jsonobject.has("navX")) {
                navX = jsonobject.get("navX").getAsFloat();
            }

            if (jsonobject.has("navY")) {
                navY = jsonobject.get("navY").getAsFloat();
            }

            if (jsonobject.has("bodyX")) {
                bodyX = jsonobject.get("bodyX").getAsFloat();
            }

            if (jsonobject.has("bodyY")) {
                bodyY = jsonobject.get("bodyY").getAsFloat();
            }

            if (jsonobject.has("selected")) {
                try {
                    selected = Category.valueOf(jsonobject.get("selected").getAsString());
                } catch (IllegalArgumentException illegalargumentexception2) {
                    selected = Category.OVERLAY;
                }
            }

            if (jsonobject.has("scale")) {
                scale(jsonobject.get("scale").getAsFloat());
            }

            if (jsonobject.has("bannerId")) {
                bannerId(jsonobject.get("bannerId").getAsString());
            }

            if (jsonobject.has("bannerLabels")) {
                bannerLabels = jsonobject.get("bannerLabels").getAsBoolean();
            }

            if (i < 4) {
                navX = 36.0F;
                navY = 36.0F;
                bodyX = 248.0F;
                bodyY = 36.0F;
                selected = Category.OVERLAY;

                for (Category category : Category.values()) {
                    GuiLayout.Panel guilayout$panel = defaultPos(category);
                    GuiLayout.Panel guilayout$panel1 = get(category);
                    guilayout$panel1.x = guilayout$panel.x;
                    guilayout$panel1.y = guilayout$panel.y;
                }

                DOCK.clear();
            } else {
                for (Category category1 : Category.values()) {
                    if (jsonobject.has(category1.name())) {
                        JsonObject jsonobject1 = jsonobject.getAsJsonObject(category1.name());
                        GuiLayout.Panel guilayout$panel2 = get(category1);
                        if (jsonobject1.has("x")) {
                            guilayout$panel2.x = jsonobject1.get("x").getAsFloat();
                        }

                        if (jsonobject1.has("y")) {
                            guilayout$panel2.y = jsonobject1.get("y").getAsFloat();
                        }

                        if (jsonobject1.has("scroll")) {
                            guilayout$panel2.scroll = jsonobject1.get("scroll").getAsFloat();
                        }

                        if (jsonobject1.has("z")) {
                            guilayout$panel2.z = jsonobject1.get("z").getAsInt();
                        }
                    }
                }
            }

            Z_ORDER.clear();
            if (jsonobject.has("zOrder") && i >= 4) {
                for (String s : jsonobject.get("zOrder").getAsString().split(",")) {
                    try {
                        Category category2 = Category.valueOf(s.trim());
                        if (!Z_ORDER.contains(category2)) {
                            Z_ORDER.add(category2);
                        }
                    } catch (IllegalArgumentException illegalargumentexception1) {
                    }
                }
            }

            ensureZ();
            DOCK.clear();
            if (jsonobject.has("dockOrder") && i >= 4) {
                for (String s1 : jsonobject.get("dockOrder").getAsString().split(",")) {
                    try {
                        Category category3 = Category.valueOf(s1.trim());
                        if (isDockCat(category3) && !DOCK.contains(category3)) {
                            DOCK.add(category3);
                        }
                    } catch (IllegalArgumentException illegalargumentexception) {
                    }
                }
            }

            ensureDock();
        }
    }

    private static boolean isDockCat(Category c) {
        return c == Category.COSMETICS || c == Category.FRIENDS || c == Category.CONFIG;
    }

    private static void ensureDock() {
        for (Category category : DEFAULT_DOCK) {
            if (!DOCK.contains(category)) {
                DOCK.add(category);
            }
        }

        DOCK.removeIf(cx -> !isDockCat(cx));
    }

    private static void ensureZ() {
        for (Category category : Category.values()) {
            if (!Z_ORDER.contains(category)) {
                Z_ORDER.add(category);
            }
        }

        int i = 0;

        for (Category category1 : Z_ORDER) {
            get(category1).z = i++;
        }
    }

    private static GuiLayout.Panel defaultPos(Category c) {
        return switch (c) {
            case OVERLAY -> new GuiLayout.Panel(18.0F, 28.0F);
            case WORLD -> new GuiLayout.Panel(154.0F, 28.0F);
            case COSMETICS -> new GuiLayout.Panel(290.0F, 28.0F);
            case TOOLS -> new GuiLayout.Panel(426.0F, 28.0F);
            case FRIENDS -> new GuiLayout.Panel(562.0F, 28.0F);
            case SYSTEM -> new GuiLayout.Panel(18.0F, 340.0F);
            case THEME -> new GuiLayout.Panel(154.0F, 340.0F);
            case CONFIG -> new GuiLayout.Panel(290.0F, 340.0F);
        };
    }

    public static final class Panel {
        public float x;
        public float y;
        public float scroll;
        public boolean open = true;
        public int z;

        public Panel(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
