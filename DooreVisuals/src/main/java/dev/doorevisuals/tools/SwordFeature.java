package dev.doorevisuals.tools;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Custom Sword: replaces the look of swords. Each material has its own choice from the library of blades; the model is a
 * normal item model built from cubes ({@code tools/knife_models.py} generates them with their textures), so Minecraft
 * draws it itself, with the hand, the lighting and the glint. In first person the knife also gets the animations of
 * {@link KnifeAnim}: a flourish on draw, a slight sway, and an inspect routine of its own on a key.
 */
public final class SwordFeature extends Feature implements Tick {
    private static final String NONE = "нет";
    private static final String OWN_STYLE = "Как у ножа";
    /** Shape names in the order the pickers list them (saved choices refer to the names). */
    private static final List<String> NAMES = List.of(
            "Арканум", "Пламенный", "Демонический", "Карамбит", "Бабочка", "Штык-нож", "Охотничий", "М9",
            "Бабочка (3D)", "М9 (3D)", "Стилет (3D)");
    private static final Map<String, Identifier> MODEL_IDS = Map.ofEntries(
            Map.entry("Арканум", Identifier.of("doorevisuals", "arcanum")),
            Map.entry("Пламенный", Identifier.of("doorevisuals", "flame")),
            Map.entry("Демонический", Identifier.of("doorevisuals", "demon")),
            Map.entry("Карамбит", Identifier.of("doorevisuals", "karambit")),
            Map.entry("Бабочка", Identifier.of("doorevisuals", "butterfly")),
            Map.entry("Штык-нож", Identifier.of("doorevisuals", "bayonet")),
            Map.entry("Охотничий", Identifier.of("doorevisuals", "hunter")),
            Map.entry("М9", Identifier.of("doorevisuals", "m9")),
            // Converted from Sketchfab models (CC BY 4.0), see CREDITS-knives.txt
            Map.entry("Бабочка (3D)", Identifier.of("doorevisuals", "sk_butterfly")),
            Map.entry("М9 (3D)", Identifier.of("doorevisuals", "sk_m9")),
            Map.entry("Стилет (3D)", Identifier.of("doorevisuals", "sk_stiletto")));

    /** How many knife models there are to preview. */
    public static int previewCount() {
        return NAMES.size();
    }

    public static String previewName(int index) {
        return NAMES.get(Math.floorMod(index, NAMES.size()));
    }

    /** A sword with the model of the knife at that index, for drawing in a menu. */
    public static ItemStack previewStack(int index) {
        ItemStack stack = new ItemStack(net.minecraft.item.Items.NETHERITE_SWORD);
        stack.set(DataComponentTypes.ITEM_MODEL, MODEL_IDS.get(previewName(index)));
        return stack;
    }

    private static volatile boolean live;
    private static volatile boolean drawOn = true;
    private static volatile boolean idleOn = true;
    private static volatile boolean inspectOn = true;
    private static volatile String inspectStyle = OWN_STYLE;
    private static volatile float inspectSpeed = 1.0F;
    private static volatile int inspectKey = -1;
    private static long inspectStartedAt;
    private static boolean inspectWasDown;
    private static final Map<String, String> CHOSEN = new LinkedHashMap<>();

    private final Map<String, Opt.Pick> picks = new LinkedHashMap<>();
    private final Opt.Flag drawAnim = this.opt(new Opt.Flag("draw_anim", "Анимация доставания", true));
    private final Opt.Flag idleAnim = this.opt(new Opt.Flag("idle_anim", "Покачивание в руке", true));
    private final Opt.Flag inspectFlag = this.opt(new Opt.Flag("inspect", "Анимация осмотра", true));
    private final Opt.Pick inspectStyleOpt = this.opt(
            new Opt.Pick("inspect_style", "Стиль осмотра", OWN_STYLE, OWN_STYLE, "Прокрут", "Переворот", "Восьмёрка"));
    private final Opt.Num inspectSpeedOpt = this.opt(new Opt.Num("inspect_speed", "Скорость осмотра", 1.0, 0.5, 2.5, 0.1));
    private final Opt.Key inspectKeyOpt = this.opt(new Opt.Key("inspect_key", "Клавиша осмотра", GLFW.GLFW_KEY_Y));

    public SwordFeature() {
        super(
                // New id: the module is on from the first launch, which a saved "off" under the old id would have overridden.
                "knives",
                "Custom Sword",
                "Свои модели клинков: выберите, какой меч во что превращается",
                Category.COSMETICS,
                true
        );
        this.badge(Badge.BETA);
        // Every material has a knife from the start, so the module works as soon as it is enabled.
        this.pick("wooden", "Деревянный", "Охотничий");
        this.pick("stone", "Каменный", "Штык-нож");
        this.pick("iron", "Железный", "М9");
        this.pick("golden", "Золотой", "Бабочка");
        this.pick("diamond", "Алмазный", "Карамбит");
        this.pick("netherite", "Незеритовый", "Демонический");
    }

    private void pick(String id, String label, String preset) {
        String[] values = new String[NAMES.size() + 1];
        values[0] = NONE;
        for (int i = 0; i < NAMES.size(); i++) {
            values[i + 1] = NAMES.get(i);
        }
        this.picks.put(id, this.opt(new Opt.Pick("k_" + id, label, preset, values)));
    }

    @Override
    protected void enable() {
        this.sync();
    }

    @Override
    protected void disable() {
        live = false;
        inspectStartedAt = 0L;
    }

    @Override
    public void poke() {
        super.poke();
        this.sync();
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            this.sync();
            pollInspect(mc);
        } else {
            live = false;
            inspectStartedAt = 0L;
        }
    }

    private void sync() {
        CHOSEN.clear();
        for (Map.Entry<String, Opt.Pick> entry : this.picks.entrySet()) {
            String value = entry.getValue().get();
            if (value != null && !NONE.equals(value)) {
                CHOSEN.put(entry.getKey(), value);
            }
        }
        drawOn = this.drawAnim.get();
        idleOn = this.idleAnim.get();
        inspectOn = this.inspectFlag.get();
        inspectStyle = this.inspectStyleOpt.get();
        inspectSpeed = this.inspectSpeedOpt.f();
        inspectKey = this.inspectKeyOpt.get();
        live = this.on();
    }

    public static boolean active() {
        return live;
    }

    // ------------------------------------------------------------------ models

    /** Name of the chosen shape for an item, or an empty string when it is not replaced. */
    public static String modelFor(Item item) {
        if (!live || item == null) {
            return "";
        }
        String key = key(item);
        return key.isEmpty() ? "" : CHOSEN.getOrDefault(key, "");
    }

    public static String modelFor(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "" : modelFor(stack.getItem());
    }

    private static String key(Item item) {
        if (item == Items.WOODEN_SWORD) {
            return "wooden";
        }
        if (item == Items.STONE_SWORD) {
            return "stone";
        }
        if (item == Items.IRON_SWORD) {
            return "iron";
        }
        if (item == Items.GOLDEN_SWORD) {
            return "golden";
        }
        if (item == Items.DIAMOND_SWORD) {
            return "diamond";
        }
        if (item == Items.NETHERITE_SWORD) {
            return "netherite";
        }
        return "";
    }

    /** Item model of a shape, for the preview in the settings panel. */
    public static Identifier modelId(String shape) {
        return shape == null ? null : MODEL_IDS.get(shape);
    }

    public static boolean isSwordModule(Feature feature) {
        return feature instanceof SwordFeature;
    }

    /** First shape chosen for any material, used by the default preview. */
    public static String currentShape() {
        for (String shape : CHOSEN.values()) {
            if (shape != null && !shape.isEmpty() && MODEL_IDS.containsKey(shape)) {
                return shape;
            }
        }
        return null;
    }

    /**
     * The stack to draw. For a held sword with a chosen shape it is a copy that carries the custom item model; the real
     * stack is never touched, so the inventory and everything the server sees stay exactly as they are. The inventory
     * icon is left vanilla.
     */
    public static ItemStack visualStack(ItemStack stack, ItemDisplayContext ctx) {
        if (!live || stack == null || stack.isEmpty() || ctx == ItemDisplayContext.GUI) {
            return stack;
        }
        String shape = modelFor(stack.getItem());
        Identifier want = shape.isEmpty() ? null : MODEL_IDS.get(shape);
        if (want == null || want.equals(stack.get(DataComponentTypes.ITEM_MODEL))) {
            return stack;
        }
        ItemStack copy = stack.copy();
        copy.set(DataComponentTypes.ITEM_MODEL, want);
        return copy;
    }

    // ------------------------------------------------------------------ animation

    /** Inspect is started by its own key, and only while a sword with a chosen shape is in the main hand. */
    private static void pollInspect(MinecraftClient mc) {
        if (!inspectOn || mc == null || mc.getWindow() == null || mc.player == null) {
            return;
        }
        String shape = modelFor(mc.player.getMainHandStack());
        int code = inspectKey;
        boolean down = !shape.isEmpty() && code > 0 && GLFW.glfwGetKey(mc.getWindow().getHandle(), code) == GLFW.GLFW_PRESS;
        if (down && !inspectWasDown && !inspectPlaying()) {
            inspectStartedAt = System.currentTimeMillis();
        }
        inspectWasDown = down;
    }

    private static boolean ownStyle(String shape) {
        return OWN_STYLE.equals(inspectStyle) && KnifeAnim.hasInspect(shape);
    }

    private static float inspectDuration(String shape) {
        float speed = Math.max(0.4F, inspectSpeed);
        return ownStyle(shape) ? Math.max(0.6F, 2.2F / speed) : Math.max(0.3F, 1.2F / speed);
    }

    /** Whether an inspect is running right now. */
    public static boolean inspectPlaying() {
        if (!live || !inspectOn || inspectStartedAt <= 0L) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String shape = mc.player == null ? "" : modelFor(mc.player.getMainHandStack());
        return (System.currentTimeMillis() - inspectStartedAt) < inspectDuration(shape) * 1000.0F;
    }

    /**
     * Pose of the hand for the stack being shown in the main hand: draw flourish while it is raised, then either the
     * inspect routine or the idle sway. Six numbers (x, y, z in blocks; Y, X, Z rotation in degrees), or null when this
     * is not a custom blade and nothing should be added.
     */
    public static float[] handPose(ItemStack stack, float equipProgress) {
        String shape = modelFor(stack);
        if (shape.isEmpty()) {
            return null;
        }
        float[] pose = new float[6];
        if (drawOn && equipProgress > 0.001F) {
            add(pose, KnifeAnim.draw(shape, equipProgress));
        }
        if (inspectPlaying()) {
            float raw = Math.max(0.0F, Math.min(1.0F,
                    (System.currentTimeMillis() - inspectStartedAt) / (inspectDuration(shape) * 1000.0F)));
            if (ownStyle(shape)) {
                add(pose, KnifeAnim.inspect(shape, raw));
            } else {
                add(pose, KnifeAnim.generic(inspectStyle, raw * raw * (3.0F - 2.0F * raw)));
            }
        } else if (idleOn) {
            float[] sway = KnifeAnim.idle(System.currentTimeMillis() / 1000.0F);
            // The sway returns gradually after an inspect instead of jumping in on the last frame.
            float k = settle(shape);
            for (int i = 0; i < 6; i++) {
                sway[i] *= k;
            }
            add(pose, sway);
        }
        return pose;
    }

    /** 0 right after an inspect ends and 1 once the hand has settled (smoothstep over 0.8 s). */
    private static float settle(String shape) {
        if (inspectStartedAt <= 0L) {
            return 1.0F;
        }
        float since = System.currentTimeMillis() - inspectStartedAt - inspectDuration(shape) * 1000.0F;
        float x = Math.max(0.0F, Math.min(1.0F, since / 800.0F));
        return x * x * (3.0F - 2.0F * x);
    }

    private static void add(float[] into, float[] more) {
        for (int i = 0; i < 6; i++) {
            into[i] += more[i];
        }
    }
}
