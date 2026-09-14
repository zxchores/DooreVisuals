package dev.doorevisuals.overlay;

import com.google.gson.JsonObject;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.server.FunHelperFeature;
import dev.doorevisuals.tools.CooldownsFeature;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class HudFeature extends Feature {
    private static final int LAYOUT_VER = 9;
    private static final int NOTE_CAP = 3;
    private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private final Opt.Flag watermark = this.opt(new Opt.Flag("watermark", "\u0412\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a", true));
    private final Opt.Flag coords = this.opt(new Opt.Flag("coords", "TPS / XYZ", true));
    private final Opt.Flag keys = this.opt(new Opt.Flag("keys", "\u041a\u043b\u0430\u0432\u0438\u0448\u0438", false));
    private final Opt.Flag mouseKeys = this.opt(new Opt.Flag("mouse_keys", "\u041b\u041a\u041c / \u041f\u041a\u041c", true).visibleWhen(this.keys::get));
    private final Opt.Flag armor = this.opt(new Opt.Flag("armor", "\u0411\u0440\u043e\u043d\u044f", true));
    private final Opt.Flag inventory = this.opt(new Opt.Flag("inventory", "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c", false));
    private final Opt.Flag effects = this.opt(new Opt.Flag("effects", "\u042d\u0444\u0444\u0435\u043a\u0442\u044b", true));
    private final Opt.Flag cooldowns = this.opt(new Opt.Flag("cooldowns", "\u041a\u0443\u043b\u0434\u0430\u0443\u043d\u044b", true));
    private final Opt.Flag island = this.opt(new Opt.Flag("island", "\u041e\u0441\u0442\u0440\u043e\u0432", true));
    private final Opt.Flag targetHud = this.opt(new Opt.Flag("target_hud", "\u0426\u0435\u043b\u044c", true));
    private final Opt.Flag notifications = this.opt(new Opt.Flag("notifications", "\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f", true));
    private final Opt.Pick hpMode = this.opt(
        new Opt.Pick(
                "hp_mode",
                "HP Target",
                "\u041f\u043e\u043b\u043e\u0441\u043a\u0430",
                "\u041f\u043e\u043b\u043e\u0441\u043a\u0430",
                "\u0421\u0435\u0440\u0434\u0446\u0430"
            )
            .visibleWhen(this.targetHud::get)
    );
    private final Opt.Flag useThemeAccent = this.opt(new Opt.Flag("theme_accent", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint accent = this.opt(
        new Opt.Tint("accent", "\u0410\u043a\u0446\u0435\u043d\u0442 HUD", -12533600).visibleWhen(() -> !(Boolean)this.useThemeAccent.get())
    );
    private final Opt.Pick hudKit = this.opt(
        new Opt.Pick(
            "hud_kit",
            "\u0421\u0431\u043e\u0440\u043a\u0430",
            "\u041f\u043e\u043b\u043d\u044b\u0439",
            "\u0411\u043e\u0439",
            "\u0418\u043d\u0444\u043e",
            "\u041c\u0435\u0434\u0438\u0430",
            "\u041f\u043e\u043b\u043d\u044b\u0439"
        )
    );
    private final Opt.Pick layoutPreset = this.opt(
        new Opt.Pick(
            "layout_preset",
            "\u0420\u0430\u0441\u043a\u043b\u0430\u0434\u043a\u0430",
            "\u041f\u043e\u043b\u043d\u044b\u0439",
            "\u0423\u0433\u043b\u044b",
            "\u041c\u0438\u043d\u0438",
            "\u041f\u043e\u043b\u043d\u044b\u0439"
        )
    );
    private final Opt.Flag sbOn = this.opt(new Opt.Flag("sb_enabled", "\u0421\u043a\u043e\u0440\u0431\u043e\u0440\u0434", true));
    private final Opt.Num sbScaleOpt = this.opt(
        new Opt.Num("sb_scale", "SB \u043c\u0430\u0441\u0448\u0442\u0430\u0431", 1.0, 0.5, 1.5, 0.05).visibleWhen(this.sbOn::get)
    );
    private final Opt.Num sbOpacityOpt = this.opt(
        new Opt.Num("sb_opacity", "SB \u043d\u0435\u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 255.0, 0.0, 255.0, 5.0)
            .visibleWhen(this.sbOn::get)
    );
    private final Opt.Flag sbHideScoresOpt = this.opt(
        new Opt.Flag("sb_hide_scores", "SB \u0441\u043a\u0440\u044b\u0442\u044c \u043e\u0447\u043a\u0438", false).visibleWhen(this.sbOn::get)
    );
    private final Opt.Num sbOx = this.opt(new Opt.Num("sb_ox", "SB \u0441\u0434\u0432\u0438\u0433 X", 0.0, -200.0, 200.0, 1.0).visibleWhen(this.sbOn::get));
    private final Opt.Num sbOy = this.opt(new Opt.Num("sb_oy", "SB \u0441\u0434\u0432\u0438\u0433 Y", 0.0, -200.0, 200.0, 1.0).visibleWhen(this.sbOn::get));
    private final Opt.Flag bbOn = this.opt(new Opt.Flag("bb_enabled", "\u041f\u043e\u043b\u043e\u0441\u0430 \u0431\u043e\u0441\u0441\u0430", true));
    private final Opt.Num bbScaleOpt = this.opt(
        new Opt.Num("bb_scale", "BB \u043c\u0430\u0441\u0448\u0442\u0430\u0431", 1.0, 0.5, 2.0, 0.05).visibleWhen(this.bbOn::get)
    );
    private final Opt.Pick bbStyleOpt = this.opt(
        new Opt.Pick("bb_style", "BB \u0441\u0442\u0438\u043b\u044c", "default", "default", "thin", "segmented", "text_only").visibleWhen(this.bbOn::get)
    );
    private final Opt.Flag bbHideNameOpt = this.opt(
        new Opt.Flag("bb_hide_name", "BB \u0441\u043a\u0440\u044b\u0442\u044c \u0438\u043c\u044f", false).visibleWhen(this.bbOn::get)
    );
    private final Opt.Num bbOx = this.opt(new Opt.Num("bb_ox", "BB \u0441\u0434\u0432\u0438\u0433 X", 0.0, -400.0, 400.0, 1.0).visibleWhen(this.bbOn::get));
    private final Opt.Num bbOy = this.opt(new Opt.Num("bb_oy", "BB \u0441\u0434\u0432\u0438\u0433 Y", 0.0, -200.0, 200.0, 1.0).visibleWhen(this.bbOn::get));
    private final Map<String, HudSlot> slots = new LinkedHashMap<>();
    private final Anim targetShow = new Anim(0.0F);
    private final Anim targetHp = new Anim(1.0F);
    private final Anim editorChrome = new Anim(0.0F);
    private long lastNanos;
    private float frameDt = 0.016666668F;
    private final Anim[] keyAnims = new Anim[]{new Anim(0.0F), new Anim(0.0F), new Anim(0.0F), new Anim(0.0F), new Anim(0.0F), new Anim(0.0F)};
    private final float[] slotScratch = new float[2];
    private final List<HudFeature.Note> notes = new ArrayList<>();
    private final ItemStack[] armorStacks = new ItemStack[4];
    private final ItemStack[] invStacks = new ItemStack[36];
    private int watermarkIconX;
    private int watermarkIconY;
    private int watermarkIconS;
    private boolean watermarkIconReady;
    private int targetHeadX;
    private int targetHeadY;
    private int targetHeadS;
    private boolean targetHeadReady;
    private final IslandHud islandHud = new IslandHud();
    private String lastLayout = "";
    private int lastTotems = -1;
    private long armorNext;
    private long invNext;
    private long effectsNext;
    private final List<HudFeature.EffectRow> effectRows = new ArrayList<>();
    private final Map<String, Integer> effectMax = new HashMap<>();
    private final List<HudFeature.IconBlit> effectIcons = new ArrayList<>();
    private int cdIconX;
    private int cdIconY;
    private float cdIconS;
    private boolean cdIconsReady;

    public void islandEvent(String title, String body) {
        this.islandHud.push(title, body);
        this.notify(title == null ? "" : title, body == null ? "" : body, HudFeature.NoteKind.OK);
    }

    public HudFeature() {
        super(
            "hud",
            "HUD",
            "\u0412\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a, \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b, target hud. \u0420\u0430\u0441\u043a\u043b\u0430\u0434\u043a\u0430 \u0432 \u0447\u0430\u0442\u0435",
            Category.OVERLAY,
            true
        );
        this.applyDooreLayout();

        for (int i = 0; i < this.armorStacks.length; i++) {
            this.armorStacks[i] = ItemStack.EMPTY;
        }

        for (int j = 0; j < this.invStacks.length; j++) {
            this.invStacks[j] = ItemStack.EMPTY;
        }
    }

    public void resetLayout() {
        this.applyLayoutPreset((String)this.layoutPreset.get());
        this.poke();
    }

    public void resetSlot(HudSlot slot) {
        HudSlot hudslot = defaultSlot(slot.id(), (String)this.layoutPreset.get());
        if (hudslot != null) {
            slot.move(hudslot.x(), hudslot.y());
            slot.setScale(1.0F);
            slot.size(hudslot.w(), hudslot.h());
            this.poke();
        }
    }

    private void applyLayoutPreset(String name) {
        Map<String, HudSlot> map = new LinkedHashMap<>();

        for (String s : new String[]{
            "watermark", "coords", "keys", "armor", "inventory", "effects", "cooldowns", "island", "trap", "target_hud", "notifications"
        }) {
            HudSlot hudslot = defaultSlot(s, name);
            if (hudslot != null) {
                map.put(s, hudslot);
            }
        }

        this.slots.clear();
        this.slots.putAll(map);
    }

    private static HudSlot defaultSlot(String id, String preset) {
        boolean flag = "\u041c\u0438\u043d\u0438".equals(preset);
        boolean flag1 = "\u0423\u0433\u043b\u044b".equals(preset);

        return switch (id) {
            case "watermark" -> new HudSlot("watermark", "\u0412\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a", 8.0F, 8.0F, 220.0F, 22.0F);
            case "coords" -> new HudSlot("coords", "TPS / XYZ", 8.0F, !flag1 && !flag ? -8.0F : -8.0F, 156.0F, 32.0F);
            case "keys" -> new HudSlot("keys", "\u041a\u043b\u0430\u0432\u0438\u0448\u0438", flag ? 8.0F : 8.0F, flag ? -40.0F : -56.0F, 56.0F, 44.0F);
            case "armor" -> new HudSlot("armor", "\u0411\u0440\u043e\u043d\u044f", -8.0F, flag1 ? -8.0F : -28.0F, 88.0F, 24.0F);
            case "inventory" -> new HudSlot("inventory", "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c", -8.0F, 190.0F, 176.0F, 80.0F);
            case "effects" -> new HudSlot("effects", "\u042d\u0444\u0444\u0435\u043a\u0442\u044b", -8.0F, flag1 ? 8.0F : 148.0F, 128.0F, 40.0F);
            case "cooldowns" -> new HudSlot("cooldowns", "\u041a\u0443\u043b\u0434\u0430\u0443\u043d\u044b", 8.0F, 50.0F, 96.0F, 26.0F);
            case "island" -> new HudSlot("island", "\u041e\u0441\u0442\u0440\u043e\u0432", 0.0F, 8.0F, 188.0F, 28.0F);
            case "trap" -> new HudSlot("trap", "\u0422\u0440\u0430\u043f\u043a\u0430", 8.0F, flag ? -58.0F : -80.0F, 110.0F, 20.0F);
            case "target_hud" -> new HudSlot("target_hud", "\u0426\u0435\u043b\u044c", 0.0F, 0.0F, 196.0F, 52.0F);
            case "notifications" -> new HudSlot(
                "notifications", "\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f", -8.0F, flag ? 8.0F : 280.0F, 176.0F, 48.0F
            );
            default -> null;
        };
    }

    private void applyDooreLayout() {
        this.slots.clear();
        this.slots.put("watermark", new HudSlot("watermark", "\u0412\u0430\u0442\u0435\u0440\u043c\u0430\u0440\u043a", 8.0F, 8.0F, 220.0F, 22.0F));
        this.slots.put("coords", new HudSlot("coords", "TPS / XYZ", 8.0F, -8.0F, 156.0F, 32.0F));
        this.slots.put("keys", new HudSlot("keys", "\u041a\u043b\u0430\u0432\u0438\u0448\u0438", 8.0F, -56.0F, 56.0F, 44.0F));
        this.slots.put("armor", new HudSlot("armor", "\u0411\u0440\u043e\u043d\u044f", -8.0F, -28.0F, 88.0F, 24.0F));
        this.slots.put("inventory", new HudSlot("inventory", "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c", -8.0F, 190.0F, 176.0F, 80.0F));
        this.slots.put("effects", new HudSlot("effects", "\u042d\u0444\u0444\u0435\u043a\u0442\u044b", -8.0F, 148.0F, 128.0F, 40.0F));
        this.slots.put("cooldowns", new HudSlot("cooldowns", "\u041a\u0443\u043b\u0434\u0430\u0443\u043d\u044b", 8.0F, 50.0F, 96.0F, 26.0F));
        this.slots.put("island", new HudSlot("island", "\u041e\u0441\u0442\u0440\u043e\u0432", 0.0F, 8.0F, 168.0F, 24.0F));
        this.slots.put("trap", new HudSlot("trap", "\u0422\u0440\u0430\u043f\u043a\u0430", 8.0F, -80.0F, 110.0F, 20.0F));
        this.slots.put("target_hud", new HudSlot("target_hud", "\u0426\u0435\u043b\u044c", 0.0F, 0.0F, 196.0F, 52.0F));
        this.slots
            .put(
                "notifications",
                new HudSlot("notifications", "\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f", -8.0F, 280.0F, 176.0F, 48.0F)
            );
    }

    private boolean show(Opt.Flag flag, String id) {
        return (Boolean)flag.get() && this.kit(id);
    }

    private boolean kit(String id) {
        String s = (String)this.hudKit.get();

        return switch (s) {
            case "\u0411\u043e\u0439" -> {
                switch (id) {
                    case "target_hud":
                    case "keys":
                    case "armor":
                    case "notifications":
                    case "cooldowns":
                        yield true;
                    default:
                        yield false;
                }
            }
            case "\u0418\u043d\u0444\u043e" -> {
                switch (id) {
                    case "watermark":
                    case "coords":
                    case "effects":
                    case "notifications":
                        yield true;
                    default:
                        yield false;
                }
            }
            case "\u041c\u0435\u0434\u0438\u0430" -> {
                switch (id) {
                    case "island":
                    case "watermark":
                    case "notifications":
                        yield true;
                    default:
                        yield false;
                }
            }
            default -> true;
        };
    }

    public void notify(String title, String body) {
        this.notify(title, body, HudFeature.NoteKind.INFO);
    }

    public void notify(String title, String body, HudFeature.NoteKind kind) {
        this.notes.add(0, new HudFeature.Note(title, body, System.currentTimeMillis(), kind == null ? HudFeature.NoteKind.INFO : kind));

        while (this.notes.size() > 3) {
            this.notes.remove(this.notes.size() - 1);
        }
    }

    public boolean hideVanillaEffects() {
        return this.on() && this.show(this.effects, "effects");
    }

    public boolean tryIslandClick(double mx, double my) {
        return this.on() && (Boolean)this.island.get() && this.islandHud.click(mx, my, true);
    }

    public boolean tryIslandScroll(double mx, double my, double dy) {
        return this.on() && (Boolean)this.island.get() && this.islandHud.scroll(mx, my, dy, true);
    }

    HudSlot slot(String id) {
        return this.slots.get(id);
    }

    int hudAccent() {
        return this.useThemeAccent.get() ? ThemeFeature.hudTint() : (Integer)this.accent.get();
    }

    float[] slotScreen(HudSlot s, float w, float h) {
        return this.slotPos(s, w, h, MinecraftClient.getInstance());
    }

    public boolean islandOn() {
        return this.on() && (Boolean)this.island.get();
    }

    private static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public Collection<HudSlot> activeSlots() {
        List<HudSlot> list = new ArrayList<>();
        if (this.show(this.watermark, "watermark")) {
            list.add(this.slots.get("watermark"));
        }

        if (this.show(this.coords, "coords")) {
            list.add(this.slots.get("coords"));
        }

        if (this.show(this.keys, "keys")) {
            list.add(this.slots.get("keys"));
        }

        if (this.show(this.armor, "armor") && (this.hasArmor() || this.editingLayout())) {
            list.add(this.slots.get("armor"));
        }

        if (this.show(this.inventory, "inventory")) {
            list.add(this.slots.get("inventory"));
        }

        if (this.show(this.effects, "effects")) {
            list.add(this.slots.get("effects"));
        }

        if (this.show(this.cooldowns, "cooldowns")) {
            list.add(this.slots.get("cooldowns"));
        }

        if (this.show(this.island, "island")) {
            list.add(this.slots.get("island"));
        } else if (this.trapSlotVisible()) {
            list.add(this.slots.get("trap"));
        }

        if (this.show(this.targetHud, "target_hud")) {
            list.add(this.slots.get("target_hud"));
        }

        if (this.show(this.notifications, "notifications")) {
            list.add(this.slots.get("notifications"));
        }

        return list;
    }

    public void writeLayouts(JsonObject root) {
        root.addProperty("ver", 9);

        for (HudSlot hudslot : this.slots.values()) {
            JsonObject jsonobject = new JsonObject();
            hudslot.write(jsonobject);
            root.add(hudslot.id(), jsonobject);
        }
    }

    public void readLayouts(JsonObject root) {
        if (root != null) {
            int i = root.has("ver") ? root.get("ver").getAsInt() : 0;
            if (i < 8) {
                this.applyDooreLayout();
            } else {
                for (HudSlot hudslot : this.slots.values()) {
                    if (root.has(hudslot.id())) {
                        hudslot.read(root.getAsJsonObject(hudslot.id()));
                    }
                }

                this.migrate(root, "brand", "watermark");
                this.migrate(root, "pos", "coords");
                this.migrate(root, "aim_hud", "target_hud");
                this.migrate(root, "dynamic_island", "island");
                this.migrate(root, "ping", "watermark");
                if (i < 6) {
                    HudSlot hudslot1 = this.slots.get("armor");
                    if (hudslot1 != null) {
                        hudslot1.setScale(1.0F);
                    }
                }
            }
        }
    }

    private float[] slotPos(HudSlot s, float w, float h, MinecraftClient mc) {
        float f = Math.max(0.01F, s.scale());
        float f1 = mc.getWindow().getScaledWidth();
        float f2 = mc.getWindow().getScaledHeight();
        float f3 = s.x();
        float f4 = s.y();
        if (f3 < 0.0F) {
            f3 = f1 + f3 - w * f;
            if (f3 < 4.0F) {
                f3 = f1 - w * f - 6.0F;
            }
        }

        if (f4 < 0.0F) {
            f4 = f2 + f4 - h * f;
            if (f4 < 4.0F) {
                f4 = f2 - h * f - 6.0F;
            }
        }

        this.slotScratch[0] = f3;
        this.slotScratch[1] = f4;
        return this.slotScratch;
    }

    private float[] resolveSlot(HudSlot s, float w, float h, MinecraftClient mc) {
        if (Math.abs(s.x()) < 0.01F && Math.abs(s.y()) < 0.01F) {
            float f = Math.max(0.01F, s.scale());
            this.slotScratch[0] = (mc.getWindow().getScaledWidth() - w * f) * 0.5F;
            this.slotScratch[1] = mc.getWindow().getScaledHeight() * 0.62F;
            return this.slotScratch;
        } else {
            return this.slotPos(s, w, h, mc);
        }
    }

    public float[] editorRect(HudSlot s) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        float[] afloat;
        if ("target_hud".equals(s.id())) {
            afloat = this.resolveSlot(s, s.w(), s.h(), minecraftclient);
        } else if ("island".equals(s.id()) && Math.abs(s.x()) < 0.01F) {
            float f = Math.max(0.01F, s.scale());
            this.slotScratch[0] = (minecraftclient.getWindow().getScaledWidth() - s.w() * f) * 0.5F;
            this.slotScratch[1] = this.slotPos(s, s.w(), s.h(), minecraftclient)[1];
            afloat = this.slotScratch;
        } else {
            afloat = this.slotPos(s, s.w(), s.h(), minecraftclient);
        }

        float f1 = Math.max(0.01F, s.scale());
        return new float[]{afloat[0], afloat[1], s.w() * f1, s.h() * f1};
    }

    public boolean hitSlot(HudSlot s, double mx, double my) {
        float[] afloat = this.editorRect(s);
        return mx >= afloat[0] && mx <= afloat[0] + afloat[2] && my >= afloat[1] && my <= afloat[1] + afloat[3];
    }

    public void moveSlotScreen(HudSlot s, float screenX, float screenY) {
        s.move(screenX, screenY);
    }

    private static float screen(float origin, float local, float sc) {
        return origin + local * sc;
    }

    private void migrate(JsonObject root, String from, String to) {
        if (root.has(from) && this.slots.containsKey(to) && !root.has(to)) {
            this.slots.get(to).read(root.getAsJsonObject(from));
        }
    }

    public void paint(DrawContext g, RenderTickCounter dt) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null) {
            HudTweaksFeature.applyFromHud(
                this.on(),
                (Boolean)this.sbOn.get(),
                this.sbScaleOpt.f(),
                this.sbOpacityOpt.i(),
                (Boolean)this.sbHideScoresOpt.get(),
                this.sbOx.f(),
                this.sbOy.f(),
                (Boolean)this.bbOn.get(),
                this.bbScaleOpt.f(),
                (String)this.bbStyleOpt.get(),
                this.bbOx.f(),
                this.bbOy.f(),
                (Boolean)this.bbHideNameOpt.get()
            );
            if (!((String)this.layoutPreset.get()).equals(this.lastLayout)) {
                if (!this.lastLayout.isEmpty()) {
                    this.applyLayoutPreset((String)this.layoutPreset.get());
                }

                this.lastLayout = (String)this.layoutPreset.get();
            }

            this.trackTotem(minecraftclient);
            boolean flag = this.editingLayout();
            long i = System.nanoTime();
            this.frameDt = this.lastNanos == 0L ? 0.016666668F : Math.max(0.004166667F, Math.min(0.033333335F, (float)(i - this.lastNanos) / 1.0E9F));
            this.lastNanos = i;
            this.editorChrome.springOpen(flag ? 1.0F : 0.0F, this.frameDt);
            if (this.show(this.watermark, "watermark")) {
                this.paintWatermark(g, minecraftclient);
            }

            if (this.show(this.coords, "coords")) {
                this.paintCoords(g, minecraftclient);
            }

            if (this.show(this.keys, "keys")) {
                this.paintKeys(g, minecraftclient);
            }

            if (this.show(this.effects, "effects")) {
                this.paintEffects(g, minecraftclient, flag);
            }

            if (this.show(this.cooldowns, "cooldowns")) {
                this.paintCooldowns(g, minecraftclient, flag);
            }

            if (this.show(this.island, "island")) {
                this.islandHud.paint(g, this, flag, this.frameDt);
            } else if (this.trapSlotVisible()) {
                this.paintTrap(g, minecraftclient, flag);
            }

            if (this.show(this.targetHud, "target_hud")) {
                this.paintTarget(g, minecraftclient);
            }

            if (this.show(this.notifications, "notifications")) {
                this.paintNotes(g, flag);
            }

            if (this.show(this.armor, "armor")) {
                this.paintArmorPlate(g);
            }

            if (this.show(this.inventory, "inventory")) {
                this.paintInventoryPlate(g);
            }
        }
    }

    public void paintItems(DrawContext g) {
        if (this.on()) {
            if (this.show(this.watermark, "watermark") && this.watermarkIconReady) {
                try {
                    g.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        Sprites.LOGO,
                        this.watermarkIconX,
                        this.watermarkIconY,
                        0.0F,
                        0.0F,
                        this.watermarkIconS,
                        this.watermarkIconS,
                        this.watermarkIconS,
                        this.watermarkIconS
                    );
                } catch (Throwable throwable2) {
                    try {
                        g.drawTexture(RenderPipelines.GUI_TEXTURED, Sprites.ICON, this.watermarkIconX, this.watermarkIconY, 0.0F, 0.0F, 11, 11, 11, 11);
                    } catch (Throwable throwable1) {
                    }
                }
            }

            if (this.show(this.targetHud, "target_hud") && this.targetHeadReady) {
                LivingEntity livingentity = App.aim().get();
                if (livingentity != null) {
                    this.paintTargetHead(g, livingentity, this.targetHeadX, this.targetHeadY, this.targetHeadS);
                }
            }

            if (this.show(this.effects, "effects")) {
                for (HudFeature.IconBlit hudfeature$iconblit : this.effectIcons) {
                    try {
                        g.drawTexture(
                            RenderPipelines.GUI_TEXTURED,
                            hudfeature$iconblit.tex,
                            hudfeature$iconblit.x,
                            hudfeature$iconblit.y,
                            0.0F,
                            0.0F,
                            hudfeature$iconblit.s,
                            hudfeature$iconblit.s,
                            hudfeature$iconblit.s,
                            hudfeature$iconblit.s
                        );
                    } catch (Throwable throwable) {
                    }
                }
            }

            if (this.show(this.armor, "armor") && (this.armorPacked() > 0 || this.editingLayout())) {
                this.refreshArmor();
                HudSlot hudslot = this.slots.get("armor");
                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), minecraftclient);
                g.getMatrices().pushMatrix();

                try {
                    g.getMatrices().translate(afloat[0], afloat[1]);
                    g.getMatrices().scale(hudslot.scale(), hudslot.scale());
                    boolean flag = this.armorPacked() > 0;
                    int i = 0;

                    for (int j = 0; j < ARMOR.length; j++) {
                        ItemStack itemstack = this.armorStacks[j];
                        if (!flag || !itemstack.isEmpty()) {
                            if (!itemstack.isEmpty()) {
                                g.drawItem(itemstack, 6 + i * 20, 3);
                            }

                            i++;
                        }
                    }
                } finally {
                    g.getMatrices().popMatrix();
                }
            }

            if (this.show(this.cooldowns, "cooldowns") && this.cdIconsReady) {
                HudSlot hudslot1 = this.slots.get("cooldowns");
                MinecraftClient minecraftclient1 = MinecraftClient.getInstance();
                float[] afloat1 = this.slotPos(hudslot1, hudslot1.w(), hudslot1.h(), minecraftclient1);
                g.getMatrices().pushMatrix();

                try {
                    g.getMatrices().translate(afloat1[0], afloat1[1]);
                    g.getMatrices().scale(hudslot1.scale(), hudslot1.scale());
                    List<CooldownsFeature.Cd> list = CooldownsFeature.active();
                    if (list.isEmpty()) {
                        g.drawItem(new ItemStack(Items.ENDER_PEARL), 6, 3);
                        g.drawItem(new ItemStack(Items.GOLDEN_APPLE), 28, 3);
                        g.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), 50, 3);
                        g.drawItem(new ItemStack(Items.SHIELD), 72, 3);
                    } else {
                        for (int k = 0; k < list.size(); k++) {
                            g.drawItem(list.get(k).stack(), 6 + k * 22, 3);
                        }
                    }
                } finally {
                    g.getMatrices().popMatrix();
                }
            }

            if (this.show(this.inventory, "inventory")) {
                this.refreshInventory();
                HudSlot hudslot2 = this.slots.get("inventory");
                MinecraftClient minecraftclient2 = MinecraftClient.getInstance();
                float[] afloat2 = this.slotPos(hudslot2, hudslot2.w(), hudslot2.h(), minecraftclient2);
                g.getMatrices().pushMatrix();

                try {
                    g.getMatrices().translate(afloat2[0], afloat2[1]);
                    g.getMatrices().scale(hudslot2.scale(), hudslot2.scale());

                    for (int k = 0; k < 27; k++) {
                        int i1 = k % 9;
                        int j1 = k / 9;
                        ItemStack itemstack2 = this.invStacks[9 + k];
                        if (!itemstack2.isEmpty()) {
                            g.drawItem(itemstack2, 4 + i1 * 18, 4 + j1 * 18);
                        }
                    }

                    for (int l = 0; l < 9; l++) {
                        ItemStack itemstack1 = this.invStacks[l];
                        if (!itemstack1.isEmpty()) {
                            g.drawItem(itemstack1, 4 + l * 18, 62);
                        }
                    }
                } finally {
                    g.getMatrices().popMatrix();
                }
            }
        }
    }

    private void paintTargetHead(DrawContext g, LivingEntity t, int x, int y, int s) {
        try {
            if (t instanceof AbstractClientPlayerEntity abstractclientplayerentity) {
                PlayerSkinDrawer.draw(g, abstractclientplayerentity.getSkin(), x, y, s);
                return;
            }

            if (t instanceof PlayerEntity playerentity) {
                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                if (minecraftclient.getNetworkHandler() != null) {
                    PlayerListEntry playerlistentry = minecraftclient.getNetworkHandler().getPlayerListEntry(playerentity.getUuid());
                    if (playerlistentry != null) {
                        PlayerSkinDrawer.draw(g, playerlistentry.getSkinTextures(), x, y, s);
                        return;
                    }
                }
            }

            InventoryScreen.drawEntity(g, x, y, x + s, y + s, Math.max(12, s), 0.0625F, x + s * 0.5F, y + s * 0.35F, t);
        } catch (Throwable throwable) {
        }
    }

    private void hudPlate(DrawContext g, float w, float h, int ac) {
        Paint.hudPlate(g, 0.0F, 0.0F, w, h, ac);
    }

    public float editorChromeAlpha() {
        return Math.max(0.0F, Math.min(1.0F, this.editorChrome.value()));
    }

    private void paintWatermark(DrawContext g, MinecraftClient mc) {
        HudSlot hudslot = this.slots.get("watermark");
        String s = mc.player.getGameProfile().name();
        String s1 = mc.getCurrentFps() + "fps";
        int i = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry playerlistentry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (playerlistentry != null) {
                i = Math.max(0, playerlistentry.getLatency());
            }
        }

        String s2 = i + "ms";
        float f = 22.0F;
        float f1 = f * 0.5F;
        float f2 = 8.0F;
        float f3 = 12.0F;
        float f4 = 7.2F;
        float f5 = 28.0F;
        float f6 = f2 + 10.0F + Paint.tw(s, f4) + f2;
        float f7 = f2 + 10.0F + Paint.tw(s1, f4) + f2;
        float f8 = f2 + 10.0F + Paint.tw(s2, f4) + f2;
        float f9 = f5 + f6 + f7 + f8;
        hudslot.size(f9, f);
        float[] afloat = this.slotPos(hudslot, f9, f, mc);
        float f10 = Math.max(0.01F, hudslot.scale());
        int j = this.hudAccent();
        Nvg.push();
        Nvg.move(afloat[0], afloat[1]);
        Nvg.scale(f10, f10);
        Paint.box(g, 0.0F, 0.0F, f9, f, Theme.alpha(526862, 236), f1);
        Paint.outline(g, 0.0F, 0.0F, f9, f, Theme.alpha(16777215, 16), f1);
        float f11 = f5 + f6;
        float f12 = f11 + f7;
        Paint.box(g, f5, 5.0F, 1.0F, f - 10.0F, Theme.alpha(16777215, 22), 0.0F);
        Paint.box(g, f11, 5.0F, 1.0F, f - 10.0F, Theme.alpha(16777215, 22), 0.0F);
        Paint.box(g, f12, 5.0F, 1.0F, f - 10.0F, Theme.alpha(16777215, 22), 0.0F);
        Paint.box(g, 8.0F, 5.0F, f3, f3, Theme.alpha(329483, 200), 3.5F);
        Nvg.circle(f5 + 14.0F, f * 0.38F, 3.1F, Theme.alpha(16777215, 210));
        Paint.box(g, f5 + 10.2F, f * 0.52F, 7.6F, 4.4F, Theme.alpha(16777215, 210), 2.2F);
        Paint.text(g, s, f5 + 22.0F, (f - f4) * 0.5F, Theme.TEXT, f4);
        Paint.box(g, f11 + 10.0F, 7.2F, 8.0F, 2.0F, Theme.alpha(16777215, 200), 0.8F);
        Paint.box(g, f11 + 11.2F, 10.2F, 8.0F, 2.0F, Theme.alpha(16777215, 160), 0.8F);
        Paint.box(g, f11 + 12.4F, 13.2F, 8.0F, 2.0F, Theme.alpha(16777215, 120), 0.8F);
        Paint.text(g, s1, f11 + 22.0F, (f - f4) * 0.5F, Theme.TEXT, f4);
        Nvg.circle(f12 + 14.0F, f * 0.5F, 4.2F, Theme.alpha(j, 40));
        Nvg.ring(f12 + 10.2F, f * 0.5F - 3.8F, 7.6F, 7.6F, 1.15F, Theme.alpha(16777215, 210), 3.8F);
        Paint.text(g, s2, f12 + 22.0F, (f - f4) * 0.5F, Theme.TEXT, f4);
        Nvg.pop();
        this.watermarkIconS = Math.max(8, Math.round((f3 - 1.2F) * f10));
        this.watermarkIconX = Math.round(screen(afloat[0], 8.4F, f10));
        this.watermarkIconY = Math.round(screen(afloat[1], 5.4F, f10));
        this.watermarkIconReady = true;
    }

    private void paintCoords(DrawContext g, MinecraftClient mc) {
        HudSlot hudslot = this.slots.get("coords");
        ClientPlayerEntity clientplayerentity = mc.player;
        float f = TpsMeter.tps();
        String s = String.format(Locale.ROOT, "%.1f TPS", f);
        String s1 = (int)Math.floor(clientplayerentity.getX())
            + "  "
            + (int)Math.floor(clientplayerentity.getY())
            + "  "
            + (int)Math.floor(clientplayerentity.getZ());

        String s2 = switch (Direction.fromHorizontalDegrees(clientplayerentity.getYaw())) {
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST -> "E";
            case WEST -> "W";
            default -> "";
        };
        String s3 = worldClock(mc);
        float f1 = Math.max(Paint.tw(s, 8.0F) + 18.0F + Paint.tw(s3, 6.4F) + Paint.tw(s2, 6.4F), Paint.tw(s1, 7.2F) + 16.0F) + 16.0F;
        float f2 = 32.0F;
        hudslot.size(f1, f2);
        float[] afloat = this.slotPos(hudslot, f1, f2, mc);
        int i = this.hudAccent();
        int j = f >= 19.4F ? Theme.OK : (f >= 15.0F ? Theme.WARN : Theme.BAD);
        Nvg.push();
        Nvg.move(afloat[0], afloat[1]);
        Nvg.scale(hudslot.scale(), hudslot.scale());
        this.hudPlate(g, f1, f2, i);
        Nvg.circle(12.0F, 11.0F, 2.4F, j);
        Paint.text(g, s, 18.0F, 6.5F, j, 8.0F);
        Paint.textR(g, s2 + "  " + s3, f1 - 8.0F, 7.2F, Theme.MUTED, 6.4F);
        Paint.text(g, s1, 10.0F, 18.5F, Theme.alpha(16777215, 190), 7.2F);
        Nvg.pop();
    }

    private static String worldClock(MinecraftClient mc) {
        World world = mc.world;
        if (world == null) {
            return "--:--";
        } else {
            long i = world.getTimeOfDay() % 24000L;
            int j = (int)((i / 1000L + 6L) % 24L);
            int k = (int)(i % 1000L * 60L / 1000L);
            return String.format(Locale.ROOT, "%02d:%02d", j, k);
        }
    }

    private void paintKeys(DrawContext g, MinecraftClient mc) {
        HudSlot hudslot = this.slots.get("keys");
        boolean flag = (Boolean)this.mouseKeys.get();
        float f = 13.0F;
        float f1 = 3.0F;
        float f2 = 8.0F + f * 3.0F + f1 * 2.0F;
        float f3 = flag ? 8.0F + f * 3.0F + f1 * 2.0F : 8.0F + f * 2.0F + f1;
        hudslot.size(f2, f3);
        float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), mc);
        GameOptions gameoptions = mc.options;
        int i = this.hudAccent();
        Nvg.push();
        Nvg.move(afloat[0], afloat[1]);
        Nvg.scale(hudslot.scale(), hudslot.scale());
        this.hudPlate(g, f2, f3, i);
        float f4 = 5.0F;
        float f5 = 4.0F;
        cap(g, f4 + f + f1, f5, "W", gameoptions.forwardKey.isPressed(), this.keyAnims[0], i, f);
        cap(g, f4, f5 + f + f1, "A", gameoptions.leftKey.isPressed(), this.keyAnims[1], i, f);
        cap(g, f4 + f + f1, f5 + f + f1, "S", gameoptions.backKey.isPressed(), this.keyAnims[2], i, f);
        cap(g, f4 + (f + f1) * 2.0F, f5 + f + f1, "D", gameoptions.rightKey.isPressed(), this.keyAnims[3], i, f);
        if (flag) {
            cap(g, f4, f5 + (f + f1) * 2.0F, "L", gameoptions.attackKey.isPressed(), this.keyAnims[4], i, f);
            cap(g, f4 + (f + f1) * 2.0F, f5 + (f + f1) * 2.0F, "R", gameoptions.useKey.isPressed(), this.keyAnims[5], i, f);
        }

        Nvg.pop();
    }

    private static void cap(DrawContext g, float x, float y, String label, boolean down, Anim anim, int accent, float size) {
        float f = anim.to(down ? 1.0F : 0.0F, 36.0F);
        Paint.keycap(g, x, y, size, size, label, f, accent);
    }

    public boolean editingLayout() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        return minecraftclient.currentScreen instanceof ChatScreen;
    }

    private boolean hasArmor() {
        this.refreshArmor();

        for (ItemStack itemstack : this.armorStacks) {
            if (itemstack != null && !itemstack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private int armorPacked() {
        int i = 0;

        for (ItemStack itemstack : this.armorStacks) {
            if (itemstack != null && !itemstack.isEmpty()) {
                i++;
            }
        }

        return i;
    }

    private void paintArmorPlate(DrawContext g) {
        this.refreshArmor();
        HudSlot hudslot = this.slots.get("armor");
        boolean flag = this.editingLayout();
        int i = this.armorPacked();
        if (i == 0 && !flag) {
            hudslot.size(28.0F, 24.0F);
        } else {
            boolean flag1 = i > 0;
            int j = flag1 ? i : 4;
            float f = 8.0F + j * 20.0F;
            float f1 = 24.0F;
            hudslot.size(f, f1);
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), minecraftclient);
            int k = this.hudAccent();
            Nvg.push();
            Nvg.move(afloat[0], afloat[1]);
            Nvg.scale(hudslot.scale(), hudslot.scale());
            this.hudPlate(g, hudslot.w(), hudslot.h(), k);
            int l = 0;

            for (int i1 = 0; i1 < ARMOR.length; i1++) {
                ItemStack itemstack = this.armorStacks[i1];
                if (!flag1 || !itemstack.isEmpty()) {
                    float f2 = 6 + l * 20;
                    l++;
                    boolean flag2 = armorLow(itemstack);
                    if (flag2) {
                        float f3 = 0.55F + 0.45F * (float)Math.sin(Anim.timeSec() * 5.5555F);
                        Paint.box(g, f2 - 1.0F, 2.0F, 20.0F, 20.0F, Theme.alpha(16730698, (int)(90.0F + 80.0F * f3)), 5.0F);
                    }

                    Paint.box(g, f2, 3.0F, 18.0F, 18.0F, Theme.alpha(0, 90), 4.0F);
                    if (!itemstack.isEmpty() && itemstack.isDamageable()) {
                        float f4 = (float)(itemstack.getMaxDamage() - itemstack.getDamage()) / itemstack.getMaxDamage();
                        int j1 = flag2 ? Theme.BAD : (f4 < 0.55F ? Theme.WARN : Theme.OK);
                        Paint.bar(g, f2 + 2.0F, 18.0F, 14.0F, 2.2F, f4, j1);
                    }
                }
            }

            Nvg.pop();
        }
    }

    private void paintEffects(DrawContext g, MinecraftClient mc, boolean editor) {
        this.refreshEffects(mc);
        HudSlot hudslot = this.slots.get("effects");
        this.effectIcons.clear();
        if (!this.effectRows.isEmpty() || editor) {
            float f = 14.0F;
            float f1 = 20.0F;
            float f2 = 118.0F;

            for (HudFeature.EffectRow hudfeature$effectrow : this.effectRows) {
                f2 = Math.max(f2, f + 10.0F + Paint.tw(hudfeature$effectrow.name, 7.2F) + Paint.tw(hudfeature$effectrow.time, 6.6F) + 14.0F);
            }

            float f5 = Math.max(22.0F, 6.0F + Math.max(1, this.effectRows.size()) * f1);
            hudslot.size(f2, f5);
            float[] afloat = this.slotPos(hudslot, f2, f5, mc);
            int i = this.hudAccent();
            float f3 = Math.max(0.01F, hudslot.scale());
            Nvg.push();
            Nvg.move(afloat[0], afloat[1]);
            Nvg.scale(f3, f3);
            this.hudPlate(g, f2, f5, i);
            float f4 = 4.0F;
            if (this.effectRows.isEmpty()) {
                Paint.text(g, "\u043d\u0435\u0442 \u044d\u0444\u0444\u0435\u043a\u0442\u043e\u0432", 8.0F, 7.0F, Theme.MUTED, 7.0F);
            } else {
                for (HudFeature.EffectRow hudfeature$effectrow1 : this.effectRows) {
                    Paint.box(g, 8.0F, f4, f, f, Theme.alpha(0, 90), 3.5F);
                    this.effectIcons
                        .add(
                            new HudFeature.IconBlit(
                                hudfeature$effectrow1.icon,
                                Math.round(screen(afloat[0], 8.0F, f3)),
                                Math.round(screen(afloat[1], f4, f3)),
                                Math.max(8, Math.round(f * f3))
                            )
                        );
                    Paint.text(g, hudfeature$effectrow1.name, 8.0F + f + 5.0F, f4, Theme.TEXT, 7.2F);
                    int j = hudfeature$effectrow1.warn ? Theme.WARN : Theme.MUTED;
                    Paint.textR(g, hudfeature$effectrow1.time, f2 - 6.0F, f4, j, 6.6F);
                    Paint.bar(g, 8.0F + f + 5.0F, f4 + 11.0F, f2 - f - 22.0F, 3.2F, hudfeature$effectrow1.progress, hudfeature$effectrow1.warn ? Theme.WARN : i);
                    f4 += f1;
                }
            }

            Nvg.pop();
        }
    }

    private void paintCooldowns(DrawContext g, MinecraftClient mc, boolean editor) {
        HudSlot hudslot = this.slots.get("cooldowns");
        List<CooldownsFeature.Cd> list = CooldownsFeature.active();
        int i = Math.max(1, editor && list.isEmpty() ? 4 : list.size());
        float f = Math.max(96.0F, 10.0F + i * 22.0F);
        float f1 = 24.0F;
        hudslot.size(f, f1);
        float[] afloat = this.slotPos(hudslot, f, f1, mc);
        Nvg.push();
        Nvg.move(afloat[0], afloat[1]);
        Nvg.scale(hudslot.scale(), hudslot.scale());
        this.hudPlate(g, f, f1, this.hudAccent());

        for (int j = 0; j < i; j++) {
            float f2 = 6 + j * 22;
            Paint.box(g, f2, 3.0F, 18.0F, 18.0F, Theme.alpha(0, 80), 4.0F);
            if (j < list.size()) {
                CooldownsFeature.Cd cooldownsfeature$cd = list.get(j);
                float f3 = cooldownsfeature$cd.progress();
                if (f3 > 0.02F) {
                    Paint.bar(g, f2 + 1.0F, 18.0F, 16.0F, 2.2F, 1.0F - f3, Theme.ACCENT_HOT);
                }

                Paint.textC(g, String.format("%.0f", cooldownsfeature$cd.seconds()), f2 + 9.0F, 8.0F, Theme.TEXT, 5.4F);
            }
        }

        Nvg.pop();
        this.cdIconX = Math.round(afloat[0]);
        this.cdIconY = Math.round(afloat[1]);
        this.cdIconS = hudslot.scale();
        this.cdIconsReady = true;
    }

    private void trackTotem(MinecraftClient mc) {
        if (mc.player != null) {
            int i = 0;

            try {
                PlayerInventory playerinventory = mc.player.getInventory();

                for (int j = 0; j < 46; j++) {
                    ItemStack itemstack = playerinventory.getStack(j);
                    if (itemstack.isOf(Items.TOTEM_OF_UNDYING)) {
                        i += itemstack.getCount();
                    }
                }
            } catch (Throwable throwable) {
            }

            if (this.lastTotems >= 0 && i < this.lastTotems) {
                this.islandEvent("\u0422\u043e\u0442\u0435\u043c", "\u0441\u0440\u0430\u0431\u043e\u0442\u0430\u043b");
            }

            this.lastTotems = i;
        }
    }

    private boolean trapSlotVisible() {
        if ((Boolean)this.island.get()) {
            return false;
        } else {
            String s = FunHelperFeature.trapText();
            return this.editingLayout() || s != null && !s.isBlank();
        }
    }

    private void paintTrap(DrawContext g, MinecraftClient mc, boolean editor) {
        HudSlot hudslot = this.slots.get("trap");
        if (hudslot != null) {
            String s = FunHelperFeature.trapText();
            if ((s == null || s.isBlank()) && !editor) {
                hudslot.size(0.0F, 0.0F);
            } else {
                if (s == null || s.isBlank()) {
                    s = "\u0442\u0440\u0430\u043f\u043a\u0430";
                }

                float f = Math.max(88.0F, Paint.tw(s, 6.6F) + 20.0F);
                float f1 = 18.0F;
                hudslot.size(f, f1);
                float[] afloat = this.slotPos(hudslot, f, f1, mc);
                int i = this.hudAccent();
                Nvg.push();
                Nvg.move(afloat[0], afloat[1]);
                Nvg.scale(hudslot.scale(), hudslot.scale());
                this.hudPlate(g, f, f1, i);
                Paint.text(g, s, 8.0F, 5.2F, Theme.ACCENT_HOT, 6.6F);
                Nvg.pop();
            }
        }
    }

    private static boolean armorLow(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
            int i = stack.getMaxDamage();
            int j = i - stack.getDamage();
            return j < 20 || i > 0 && j * 100 < i * 15;
        } else {
            return false;
        }
    }

    private void paintTarget(DrawContext g, MinecraftClient mc) {
        LivingEntity livingentity = App.aim().get();
        boolean flag = App.aim().hot() && livingentity != null;
        float f = this.targetShow.to(flag ? 1.0F : 0.0F, 28.0F);
        if (!(f < 0.03F) && livingentity != null) {
            float f1 = Math.max(0.0F, Math.min(1.0F, livingentity.getHealth() / Math.max(1.0F, livingentity.getMaxHealth())));
            float f2 = this.targetHp.to(f1, 22.0F);
            String s = livingentity.getName().getString();
            if (s.length() > 16) {
                s = s.substring(0, 15) + "\u0432\u0402\u00a6";
            }

            String s1 = String.format(Locale.ROOT, "%.1f / %.0f", livingentity.getHealth(), livingentity.getMaxHealth());
            String s2 = mc.player == null ? "" : String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(livingentity));
            HudSlot hudslot = this.slots.get("target_hud");
            float f3 = 36.0F;
            float f4 = 188.0F;
            float f5 = 48.0F;
            hudslot.size(f4, f5);
            float[] afloat = this.resolveSlot(hudslot, f4, f5, mc);
            float f6 = afloat[0];
            float f7 = afloat[1];
            int i = f1 < 0.3F ? Theme.BAD : (f1 < 0.55F ? Theme.WARN : this.hudAccent());
            boolean flag1 = "\u0421\u0435\u0440\u0434\u0446\u0430".equals(this.hpMode.get());
            float f8 = Math.max(0.01F, hudslot.scale());
            Nvg.push();

            try {
                Nvg.alpha(f);
                Nvg.move(f6, f7);
                Nvg.scale(f8, f8);
                this.hudPlate(g, f4, f5, i);
                Paint.box(g, 6.0F, 6.0F, f3, f3, Theme.alpha(329483, 240), 8.0F);
                Paint.text(g, s, f3 + 14.0F, 7.0F, Theme.TEXT, 8.6F);
                Paint.textR(g, s1, f4 - 10.0F, 8.0F, Theme.alpha(i, 230), 7.4F);
                if (flag1) {
                    int j = Math.min(10, Math.max(1, Math.round(livingentity.getMaxHealth() / 2.0F)));
                    int k = Math.max(0, Math.min(j, Math.round(livingentity.getHealth() / 2.0F)));

                    for (int l = 0; l < j; l++) {
                        int i1 = l < k ? i : Theme.alpha(16777215, 35);
                        Paint.heart(g, f3 + 14.0F + l * 9, 24.0F, 8.0F, i1);
                    }
                } else {
                    Paint.bar(g, f3 + 14.0F, 26.0F, f4 - f3 - 24.0F, 5.2F, f2, i);
                }

                if (!s2.isEmpty()) {
                    Paint.text(g, s2, f3 + 14.0F, 35.0F, Theme.MUTED, 6.2F);
                }
            } finally {
                Nvg.pop();
            }

            this.targetHeadS = Math.round(f3 * f8);
            this.targetHeadX = Math.round(screen(f6, 6.0F, f8));
            this.targetHeadY = Math.round(screen(f7, 6.0F, f8));
            this.targetHeadReady = true;
        } else {
            this.targetHeadReady = false;
        }
    }

    private void paintInventoryPlate(DrawContext g) {
        this.refreshInventory();
        HudSlot hudslot = this.slots.get("inventory");
        hudslot.size(176.0F, 80.0F);
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), minecraftclient);
        int i = this.hudAccent();
        Nvg.push();

        try {
            Nvg.move(afloat[0], afloat[1]);
            Nvg.scale(hudslot.scale(), hudslot.scale());
            this.hudPlate(g, hudslot.w(), hudslot.h(), i);

            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 9; k++) {
                    Paint.box(g, 4 + k * 18, 4 + j * 18, 16, 16, Theme.alpha(16777215, 10), 3.0F);
                }
            }

            for (int l = 0; l < 9; l++) {
                Paint.box(g, 4 + l * 18, 62, 16, 16, Theme.alpha(i, 18), 3.0F);
            }
        } finally {
            Nvg.pop();
        }
    }

    private static String formatMs(long ms) {
        if (ms < 0L) {
            ms = 0L;
        }

        long i = ms / 1000L;
        long j = i / 60L;
        long k = i % 60L;
        return j + ":" + String.format(Locale.ROOT, "%02d", k);
    }

    private void paintNotes(DrawContext g, boolean editor) {
        HudSlot hudslot = this.slots.get("notifications");
        long i = System.currentTimeMillis();
        long j = 4800L;
        this.notes.removeIf(nx -> i - nx.at > 4800L);

        while (this.notes.size() > 3) {
            this.notes.remove(this.notes.size() - 1);
        }

        if (!this.notes.isEmpty() || editor) {
            int k = Math.max(1, Math.min(3, this.notes.size()));
            float f = 30.0F;
            float f1 = Math.max(34.0F, 8.0F + k * f);
            hudslot.size(180.0F, f1);
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), minecraftclient);
            int l = this.hudAccent();
            Nvg.push();
            Nvg.move(afloat[0], afloat[1]);
            Nvg.scale(hudslot.scale(), hudslot.scale());
            float f2 = 0.0F;
            if (this.notes.isEmpty()) {
                this.hudPlate(g, 180.0F, 32.0F, l);
                Paint.text(g, "\u0443\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f", 12.0F, 11.0F, Theme.MUTED, 8.0F);
            } else {
                int i1 = Math.min(3, this.notes.size());

                for (int j1 = 0; j1 < i1; j1++) {
                    HudFeature.Note hudfeature$note = this.notes.get(j1);
                    long k1 = i - hudfeature$note.at;
                    float f3 = 1.0F - (float)k1 / 4800.0F;
                    float f4 = Math.min(1.0F, (float)k1 / 280.0F);
                    float f5 = (1.0F - Anim.easeOut(f4)) * 16.0F;
                    float f6 = f3 < 0.18F ? f3 / 0.18F : 1.0F;
                    float f7 = Math.max(0.08F, f4 * f6);

                    int l1 = switch (hudfeature$note.kind) {
                        case OK -> Theme.OK;
                        case WARN -> Theme.WARN;
                        case BAD -> Theme.BAD;
                        default -> l;
                    };
                    Nvg.push();
                    Nvg.alpha(f7);
                    Nvg.move(f5, 0.0F);
                    Paint.box(g, 0.0F, f2, 180.0F, 26.0F, Theme.alpha(461583, 230), 8.0F);
                    Paint.outline(g, 0.0F, f2, 180.0F, 26.0F, Theme.alpha(16777215, 18), 8.0F);
                    Paint.box(g, 0.0F, f2 + 5.0F, 2.4F, 16.0F, l1, 1.2F);
                    float f8 = 180.0F * Math.max(0.0F, f3);
                    Paint.box(g, 0.0F, f2 + 24.4F, f8, 1.6F, Theme.alpha(l1, 180), 0.0F);
                    Paint.text(g, hudfeature$note.title, 12.0F, f2 + 4.0F, Theme.TEXT, 7.6F);
                    Paint.text(g, hudfeature$note.body, 12.0F, f2 + 14.0F, Theme.MUTED, 6.4F);
                    Nvg.pop();
                    f2 += f;
                }
            }

            Nvg.pop();
        }
    }

    private void refreshArmor() {
        long i = System.currentTimeMillis();
        if (i >= this.armorNext) {
            this.armorNext = i + 150L;
            ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
            if (clientplayerentity != null) {
                for (int j = 0; j < ARMOR.length; j++) {
                    this.armorStacks[j] = clientplayerentity.getEquippedStack(ARMOR[j]).copy();
                }
            }
        }
    }

    private void refreshInventory() {
        long i = System.currentTimeMillis();
        if (i >= this.invNext) {
            this.invNext = i + 200L;
            ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
            if (clientplayerentity != null) {
                for (int j = 0; j < 36; j++) {
                    ItemStack itemstack = clientplayerentity.getInventory().getStack(j);
                    ItemStack itemstack1 = this.invStacks[j];
                    if (itemstack1 == null || !ItemStack.areItemsAndComponentsEqual(itemstack1, itemstack) || itemstack1.getCount() != itemstack.getCount()) {
                        this.invStacks[j] = itemstack.isEmpty() ? ItemStack.EMPTY : itemstack.copy();
                    }
                }
            }
        }
    }

    private void refreshEffects(MinecraftClient mc) {
        long i = System.currentTimeMillis();
        if (i >= this.effectsNext) {
            this.effectsNext = i + 200L;
            this.effectRows.clear();
            if (mc.player != null) {
                HashSet<String> hashset = new HashSet<>();

                for (StatusEffectInstance statuseffectinstance : mc.player.getStatusEffects()) {
                    String s = effectPath(statuseffectinstance);
                    hashset.add(s);
                    int j = statuseffectinstance.getDuration();
                    int k = Math.max(j, this.effectMax.getOrDefault(s, j));
                    if (j > k) {
                        k = j;
                    }

                    this.effectMax.put(s, k);
                    int l = Math.max(0, j / 20);
                    String s1 = j <= 0 ? "\u0432\u20ac\u045b" : l / 60 + ":" + String.format(Locale.ROOT, "%02d", l % 60);
                    String s2 = ((StatusEffect)statuseffectinstance.getEffectType().value()).getName().getString();
                    if (statuseffectinstance.getAmplifier() > 0) {
                        s2 = s2 + " " + (statuseffectinstance.getAmplifier() + 1);
                    }

                    if (s2.length() > 14) {
                        s2 = s2.substring(0, 13) + "\u0432\u0402\u00a6";
                    }

                    float f = j <= 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)j / Math.max(1, k)));
                    boolean flag = j > 0 && j <= 200;
                    this.effectRows.add(new HudFeature.EffectRow(effectIcon(s), s2, s1, f, flag));
                }

                this.effectMax.keySet().removeIf(kx -> !hashset.contains(kx));
            }
        }
    }

    private static String effectPath(StatusEffectInstance e) {
        return e.getEffectType().getKey().map(k -> k.getValue().getPath()).orElseGet(() -> {
            String s = ((StatusEffect)e.getEffectType().value()).getTranslationKey();
            int i = s.lastIndexOf(46);
            return i >= 0 ? s.substring(i + 1) : s;
        });
    }

    private static Identifier effectIcon(String path) {
        return Identifier.of("minecraft", "textures/mob_effect/" + path + ".png");
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        } else {
            return s.length() <= max ? s : s.substring(0, max - 1) + "\u0432\u0402\u00a6";
        }
    }

    private record EffectRow(Identifier icon, String name, String time, float progress, boolean warn) {
    }

    private record IconBlit(Identifier tex, int x, int y, int s) {
    }

    private record Note(String title, String body, long at, HudFeature.NoteKind kind) {
    }

    public static enum NoteKind {
        INFO,
        OK,
        WARN,
        BAD;
    }
}
