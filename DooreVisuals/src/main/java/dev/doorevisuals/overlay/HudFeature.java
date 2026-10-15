package dev.doorevisuals.overlay;

import com.google.gson.JsonObject;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Keys;
import dev.doorevisuals.draw.Nvg;
import dev.doorevisuals.draw.Ui;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Sprites;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.overlay.MobPortraits;
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
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.client.gui.screen.ChatScreen;
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
    private final Opt.Pick coordsStyle = this.opt(
        new Opt.Pick(
                "coords_style",
                "\u0421\u0442\u0438\u043b\u044c \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442",
                "\u041f\u043e\u043b\u043d\u044b\u0439",
                "\u041f\u043e\u043b\u043d\u044b\u0439",
                "X Y Z"
            )
            .visibleWhen(this.coords::get)
    );
    private final Opt.Flag watermarkName = this.opt(new Opt.Flag("wm_name", "Watermark · имя", true).visibleWhen(this.watermark::get));
    private final Opt.Flag watermarkFps = this.opt(new Opt.Flag("wm_fps", "Watermark · FPS", true).visibleWhen(this.watermark::get));
    private final Opt.Flag watermarkPing = this.opt(new Opt.Flag("wm_ping", "Watermark · Ping", true).visibleWhen(this.watermark::get));
    private final Opt.Flag watermarkIcons = this.opt(new Opt.Flag("wm_icons", "Watermark · подписи", true).visibleWhen(this.watermark::get));
    private final Opt.Flag watermarkTps = this.opt(new Opt.Flag("wm_tps", "Watermark · TPS", true).visibleWhen(this.watermark::get));
    private final Opt.Flag coordsTps = this.opt(new Opt.Flag("coords_tps", "Координаты · TPS", false).visibleWhen(this.coords::get));
    private final Opt.Flag coordsDirection = this.opt(new Opt.Flag("coords_direction", "Координаты · направление", false).visibleWhen(this.coords::get));
    private final Opt.Flag coordsClock = this.opt(new Opt.Flag("coords_clock", "Координаты · часы", false).visibleWhen(this.coords::get));
    private final Opt.Flag keys = this.opt(new Opt.Flag("keys", "\u041a\u043b\u0430\u0432\u0438\u0448\u0438", false));
    private final Opt.Flag mouseKeys = this.opt(new Opt.Flag("mouse_keys", "\u041b\u041a\u041c / \u041f\u041a\u041c", true).visibleWhen(this.keys::get));
    private final Opt.Flag armor = this.opt(new Opt.Flag("armor", "\u0411\u0440\u043e\u043d\u044f", true));
    private final Opt.Flag inventory = this.opt(new Opt.Flag("inventory", "\u0418\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c", false));
    private final Opt.Flag effects = this.opt(new Opt.Flag("effects", "\u042d\u0444\u0444\u0435\u043a\u0442\u044b", true));
    private final Opt.Flag cooldowns = this.opt(new Opt.Flag("cooldowns", "\u041a\u0443\u043b\u0434\u0430\u0443\u043d\u044b", true));
    private final Opt.Flag hotkeys = this.opt(new Opt.Flag("hotkeys", "HotKeys", true));
    private final Opt.Flag island = this.opt(new Opt.Flag("island", "\u041e\u0441\u0442\u0440\u043e\u0432", true));
    private final Opt.Flag targetHud = this.opt(new Opt.Flag("target_hud", "\u0426\u0435\u043b\u044c", true));
    private final Opt.Flag notifications = this.opt(new Opt.Flag("notifications", "\u0423\u0432\u0435\u0434\u043e\u043c\u043b\u0435\u043d\u0438\u044f", true));
    private final Opt.Flag clockOn = this.opt(new Opt.Flag("clock", "Время", false));
    private final Opt.Flag clockSeconds = this.opt(new Opt.Flag("clock_seconds", "Время · секунды", false).visibleWhen(this.clockOn::get));
    private final Opt.Flag watermarkTime = this.opt(new Opt.Flag("wm_time", "Watermark · время", false).visibleWhen(this.watermark::get));
    private final Opt.Flag vitals = this.opt(new Opt.Flag("vitals", "Предупреждения: здоровье, голод, броня", true));
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
    private final List<HudFeature.PlateIcon> watermarkIconBlits = new ArrayList<>();
    private int targetHeadX;
    private int targetHeadY;
    private int targetHeadS;
    private boolean targetHeadReady;
    private float targetAlpha;
    private final Anim hotAppear = new Anim(0.0F);
    private List<String[]> hotLast = new ArrayList<>();
    private List<String[]> hotCache = new ArrayList<>();
    private long hotCacheAt;
    private LivingEntity targetEntity;
    private final ItemStack[] targetArmor = new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private float targetIconX;
    private float targetIconY;
    private float targetIconS;
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
            "watermark", "coords", "keys", "armor", "inventory", "effects", "cooldowns", "clock", "hotkeys", "island", "target_hud", "notifications"
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
            case "clock" -> new HudSlot("clock", "Время", 8.0F, 80.0F, 70.0F, 20.0F);
            case "hotkeys" -> new HudSlot("hotkeys", "HotKeys", -8.0F, 8.0F, 100.0F, 20.0F);
            case "island" -> new HudSlot("island", "\u041e\u0441\u0442\u0440\u043e\u0432", 0.0F, 8.0F, 188.0F, 28.0F);
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
        this.slots.put("clock", new HudSlot("clock", "Время", 8.0F, 80.0F, 70.0F, 20.0F));
        this.slots.put("hotkeys", new HudSlot("hotkeys", "HotKeys", -8.0F, 8.0F, 100.0F, 20.0F));
        this.slots.put("island", new HudSlot("island", "\u041e\u0441\u0442\u0440\u043e\u0432", 0.0F, 8.0F, 168.0F, 24.0F));
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
                    case "hotkeys":
                        yield true;
                    default:
                        yield false;
                }
            }
            case "\u0418\u043d\u0444\u043e" -> {
                switch (id) {
                    case "watermark":
                    case "clock":
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

    public boolean chatHudPartOn(String id) {
        return switch (id) {
            case "watermark" -> watermark.get();
            case "coords" -> coords.get();
            case "keys" -> keys.get();
            case "armor" -> armor.get();
            case "inventory" -> inventory.get();
            case "effects" -> effects.get();
            case "cooldowns" -> cooldowns.get();
            case "clock" -> clockOn.get();
            case "island" -> island.get();
            case "target_hud" -> targetHud.get();
            case "notifications" -> notifications.get();
            default -> false;
        };
    }

    public void toggleChatHudPart(String id) {
        switch (id) {
            case "watermark" -> watermark.set(!watermark.get());
            case "coords" -> coords.set(!coords.get());
            case "keys" -> keys.set(!keys.get());
            case "armor" -> armor.set(!armor.get());
            case "inventory" -> inventory.set(!inventory.get());
            case "effects" -> effects.set(!effects.get());
            case "cooldowns" -> cooldowns.set(!cooldowns.get());
            case "clock" -> clockOn.set(!clockOn.get());
            case "island" -> island.set(!island.get());
            case "target_hud" -> targetHud.set(!targetHud.get());
            case "notifications" -> notifications.set(!notifications.get());
        }
        this.poke();
        App.save().flush();
    }

    public boolean chatHudSubOn(String id) {
        return switch (id) {
            case "wm_name" -> watermarkName.get();
            case "wm_fps" -> watermarkFps.get();
            case "wm_ping" -> watermarkPing.get();
            case "wm_icons" -> watermarkIcons.get();
            case "coords_tps" -> coordsTps.get();
            case "coords_direction" -> coordsDirection.get();
            case "coords_clock" -> coordsClock.get();
            default -> false;
        };
    }

    public void toggleChatHudSub(String id) {
        switch (id) {
            case "wm_name" -> watermarkName.set(!watermarkName.get());
            case "wm_fps" -> watermarkFps.set(!watermarkFps.get());
            case "wm_ping" -> watermarkPing.set(!watermarkPing.get());
            case "wm_icons" -> watermarkIcons.set(!watermarkIcons.get());
            case "coords_tps" -> coordsTps.set(!coordsTps.get());
            case "coords_direction" -> coordsDirection.set(!coordsDirection.get());
            case "coords_clock" -> coordsClock.set(!coordsClock.get());
        }
        this.poke();
        App.save().flush();
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

        if (this.show(this.clockOn, "clock")) {
            list.add(this.slots.get("clock"));
        }

        if (this.show(this.hotkeys, "hotkeys") && (this.editingLayout() || this.hotAppear.value() > 0.02F || !this.hotkeyEntries().isEmpty())) {
            list.add(this.slots.get("hotkeys"));
        }

        if (this.show(this.island, "island")) {
            list.add(this.slots.get("island"));
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

        float maxX = Math.max(2.0F, f1 - w * f - 2.0F);
        float maxY = Math.max(2.0F, f2 - h * f - 2.0F);
        f3 = Math.max(2.0F, Math.min(maxX, f3));
        f4 = Math.max(2.0F, Math.min(maxY, f4));

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
            this.trackVitals(minecraftclient);
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

            if (this.show(this.clockOn, "clock")) {
                this.paintClock(g, minecraftclient);
            }

            if (this.show(this.hotkeys, "hotkeys")) {
                this.paintHotkeys(g, minecraftclient, flag);
            }

            if (this.show(this.island, "island")) {
                this.islandHud.paint(g, this, flag, this.frameDt);
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

                for (HudFeature.PlateIcon hudfeature$plateicon : this.watermarkIconBlits) {
                    try {
                        g.drawTexture(
                            RenderPipelines.GUI_TEXTURED,
                            hudfeature$plateicon.tex,
                            hudfeature$plateicon.x,
                            hudfeature$plateicon.y,
                            0.0F,
                            0.0F,
                            hudfeature$plateicon.s,
                            hudfeature$plateicon.s,
                            hudfeature$plateicon.s,
                            hudfeature$plateicon.s,
                            hudfeature$plateicon.color
                        );
                    } catch (Throwable throwable) {
                    }
                }
            }

            if (this.show(this.targetHud, "target_hud") && this.targetHeadReady && this.targetEntity != null) {
                this.paintTargetHead(g, this.targetEntity, this.targetHeadX, this.targetHeadY, this.targetHeadS, this.targetAlpha);
                for (int ti = 0; ti < this.targetArmor.length && this.targetAlpha > 0.7F; ti++) {
                    ItemStack piece = this.targetArmor[ti];
                    if (piece == null || piece.isEmpty()) {
                        continue;
                    }
                    g.getMatrices().pushMatrix();
                    try {
                        g.getMatrices().translate(this.targetIconX + ti * 14.0F * this.targetIconS / 0.75F, this.targetIconY);
                        g.getMatrices().scale(this.targetIconS, this.targetIconS);
                        g.drawItem(piece, 0, 0);
                    } catch (Throwable ignored) {
                    } finally {
                        g.getMatrices().popMatrix();
                    }
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

    /** The face is blitted separately from the plate, so it carries the plate's fade in its colour. */
    private void paintTargetHead(DrawContext g, LivingEntity target, int x, int y, int size, float alpha) {
        int argb = (Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, alpha))) << 24) | 0xFFFFFF;
        try {
            if (target instanceof PlayerEntity player) {
                SkinTextures skin = null;
                if (player instanceof AbstractClientPlayerEntity client) {
                    skin = client.getSkin();
                }
                if (skin == null) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.getNetworkHandler() != null) {
                        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                        if (entry != null) {
                            skin = entry.getSkinTextures();
                        }
                    }
                }
                if (skin == null) {
                    skin = DefaultSkinHelper.getSkinTextures(player.getUuid());
                }
                // face and hat layer of the 64x64 skin
                Identifier tex = skin.body().texturePath();
                g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 8.0F, 8.0F, size, size, 8, 8, 64, 64, argb);
                g.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 40.0F, 8.0F, size, size, 8, 8, 64, 64, argb);
                return;
            }
            MobPortraits.draw(g, target, x, y, size, argb);
        } catch (Throwable ignored) {
            // Keep the HUD alive even if a skin or resource is unavailable.
        }
    }

    /**
     * The common entrance of a HUD window: it fades in, grows a little from 92 percent and slides up a few pixels. The
     * caller has already done Ui.push(); k is the animation value.
     */
    private void popIn(float k, float[] pos, float w, float h, float scale) {
        float a = Math.max(0.0F, Math.min(1.0F, k));
        float sc = scale * (0.92F + 0.08F * a);
        Ui.alpha(a);
        Ui.move(pos[0] + w * (scale - sc) * 0.5F, pos[1] + h * (scale - sc) * 0.5F + (1.0F - a) * 8.0F);
        Ui.scale(sc, sc);
    }

    private void hudPlate(DrawContext g, float w, float h, int ac) {
        Paint.hudPlate(g, 0.0F, 0.0F, w, h, ac);
    }

    public float editorChromeAlpha() {
        return Math.max(0.0F, Math.min(1.0F, this.editorChrome.value()));
    }

    /** Small vector glyphs for the HUD plates: 0 speed, 1 globe, 2 sparkle, 3 key, anything else a timer ring. */
    private static void glyph(DrawContext g, int kind, float cx, float cy, int c) {
        switch (kind) {
            case 0 -> {
                Paint.box(g, cx - 5.0F, cy - 3.2F, 6.0F, 1.5F, c, 0.7F);
                Paint.box(g, cx - 3.0F, cy - 0.7F, 8.0F, 1.5F, c, 0.7F);
                Paint.box(g, cx - 5.0F, cy + 1.8F, 6.0F, 1.5F, c, 0.7F);
            }
            case 1 -> {
                Paint.ring(g, cx, cy, 4.6F, 1.2F, c);
                Paint.box(g, cx - 4.6F, cy - 0.5F, 9.2F, 1.0F, c, 0.5F);
                Paint.box(g, cx - 0.5F, cy - 4.6F, 1.0F, 9.2F, c, 0.5F);
            }
            case 2 -> {
                Paint.box(g, cx - 0.7F, cy - 4.5F, 1.4F, 9.0F, c, 0.7F);
                Paint.box(g, cx - 4.5F, cy - 0.7F, 9.0F, 1.4F, c, 0.7F);
                Paint.circle(g, cx + 3.8F, cy - 3.8F, 1.1F, c);
            }
            case 3 -> {
                Paint.outline(g, cx - 4.5F, cy - 3.5F, 9.0F, 7.0F, c, 2.0F);
                Paint.box(g, cx - 2.0F, cy - 0.7F, 4.0F, 1.4F, c, 0.7F);
            }
            default -> Paint.ring(g, cx, cy, 3.6F, 1.2F, c);
        }
    }

    private void paintWatermark(DrawContext g, MinecraftClient mc) {
        HudSlot slot = this.slots.get("watermark");
        String nick = mc.player.getGameProfile().name();
        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping = Math.max(0, e.getLatency());
        }
        boolean labels = watermarkIcons.get();
        String title = watermarkName.get() ? nick : "DooreVisuals";
        List<String> parts = new ArrayList<>();
        if (watermarkFps.get()) parts.add((labels ? "Fps " : "") + mc.getCurrentFps());
        if (watermarkPing.get()) parts.add((labels ? "Ping " : "") + ping);
        if (watermarkTps.get()) parts.add((labels ? "Tps " : "") + String.format(Locale.ROOT, "%.1f", TpsMeter.tps()));
        if (watermarkTime.get()) parts.add(java.time.LocalTime.now().format(CLOCK_HM));

        float size = 7.2F;
        float h = 20.0F;
        float pad = 9.0F;
        float gap = 8.0F;
        float w = pad * 2.0F + Paint.tw(title, size);
        for (String part : parts) w += gap * 2.0F + 1.0F + Paint.tw(part, size);
        slot.size(w, h);
        float[] pos = this.slotPos(slot, w, h, mc);
        int accent = this.hudAccent();
        Ui.push();
        Ui.move(pos[0], pos[1]);
        Ui.scale(Math.max(0.01F, slot.scale()), Math.max(0.01F, slot.scale()));
        this.hudPlate(g, w, h, accent);
        float ty = (h - size) * 0.5F;
        float x = pad;
        Paint.text(g, title, x, ty, accent, size);
        x += Paint.tw(title, size);
        for (String part : parts) {
            x += gap;
            Paint.box(g, x, 5.0F, 1.0F, h - 10.0F, Theme.alpha(0xFFFFFF, 34), 0.0F);
            x += 1.0F + gap;
            Paint.text(g, part, x, ty, Theme.TEXT, size);
            x += Paint.tw(part, size);
        }
        Ui.pop();
        this.watermarkIconBlits.clear();
        this.watermarkIconReady = false;
    }

    private static final java.time.format.DateTimeFormatter CLOCK_HM = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter CLOCK_HMS = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    /** A plain clock: the real time of the computer, with or without seconds. */
    private void paintClock(DrawContext g, MinecraftClient mc) {
        HudSlot slot = this.slots.get("clock");
        String text = java.time.LocalTime.now().format(this.clockSeconds.get() ? CLOCK_HMS : CLOCK_HM);
        float size = 7.2F;
        float h = 20.0F;
        float pad = 9.0F;
        float w = pad * 2.0F + 12.0F + Paint.tw(text, size);
        slot.size(w, h);
        float[] pos = this.slotPos(slot, w, h, mc);
        int accent = this.hudAccent();
        Ui.push();
        Ui.move(pos[0], pos[1]);
        Ui.scale(Math.max(0.01F, slot.scale()), Math.max(0.01F, slot.scale()));
        this.hudPlate(g, w, h, accent);
        glyph(g, 4, pad + 3.6F, h * 0.5F, accent);
        Paint.text(g, text, pad + 12.0F, (h - size) * 0.5F, Theme.TEXT, size);
        Ui.pop();
    }

    private boolean warnHealth;
    private boolean warnFood;
    private final boolean[] warnArmor = new boolean[4];

    /**
     * Warnings in the notifications: low health, hunger (with an estimate of how long the saturation lasts) and armor that
     * is about to break. Each fires once and is re-armed when the value recovers, so nothing repeats every frame.
     */
    private void trackVitals(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (!this.vitals.get() || p == null || p.isCreative() || p.isSpectator()) {
            return;
        }
        float hp = p.getHealth();
        if (hp <= 6.0F && !this.warnHealth) {
            this.warnHealth = true;
            this.notify("Мало здоровья", String.format(Locale.ROOT, "%.1f из %.0f", hp, p.getMaxHealth()), HudFeature.NoteKind.BAD);
        } else if (hp > 10.0F) {
            this.warnHealth = false;
        }

        int food = p.getHungerManager().getFoodLevel();
        if (food <= 6 && !this.warnFood) {
            this.warnFood = true;
            float sat = p.getHungerManager().getSaturationLevel();
            // One saturation point is four units of exhaustion; sprinting spends about 0.56 per second, anything else far less.
            float rate = p.isSprinting() ? 0.56F : 0.12F;
            int secs = Math.round((sat * 4.0F + 4.0F) / rate);
            this.notify("Голод", "еда " + food + "/20, насыщение " + String.format(Locale.ROOT, "%.1f", sat) + ", ещё ~" + secs + " с", HudFeature.NoteKind.WARN);
        } else if (food > 10) {
            this.warnFood = false;
        }

        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack stack = p.getEquippedStack(ARMOR[i]);
            boolean low = armorLow(stack);
            if (low && !this.warnArmor[i]) {
                this.warnArmor[i] = true;
                this.notify("Броня ломается", stack.getName().getString(), HudFeature.NoteKind.WARN);
            } else if (!low) {
                this.warnArmor[i] = false;
            }
        }
    }

    private void paintCoords(DrawContext g, MinecraftClient mc) {
        HudSlot slot = this.slots.get("coords");
        ClientPlayerEntity player = mc.player;
        if ("X Y Z".equals(coordsStyle.get())) {
            paintCoordsPlain(g, mc, slot, player);
            return;
        }
        double speed = Math.hypot(player.getVelocity().x, player.getVelocity().z) * 20.0;
        List<String> texts = new ArrayList<>();
        List<Integer> glyphs = new ArrayList<>();
        texts.add(String.format(Locale.ROOT, "%.2f b/s", speed));
        glyphs.add(0);
        texts.add((int)Math.floor(player.getX()) + "x " + (int)Math.floor(player.getY()) + "y " + (int)Math.floor(player.getZ()) + "z");
        glyphs.add(1);
        if (coordsDirection.get()) {
            String dir = switch (Direction.fromHorizontalDegrees(player.getYaw())) {
                case NORTH -> "N"; case SOUTH -> "S"; case EAST -> "E"; case WEST -> "W"; default -> "";
            };
            texts.add(dir);
            glyphs.add(-1);
        }
        if (coordsClock.get()) {
            texts.add(worldClock(mc));
            glyphs.add(-1);
        }
        if (coordsTps.get()) {
            texts.add(String.format(Locale.ROOT, "%.1f tps", TpsMeter.tps()));
            glyphs.add(-1);
        }

        float size = 7.2F;
        float h = 20.0F;
        float pad = 9.0F;
        float gap = 12.0F;
        float w = pad * 2.0F - gap;
        for (int i = 0; i < texts.size(); i++) {
            w += gap + (glyphs.get(i) >= 0 ? 14.0F : 0.0F) + Paint.tw(texts.get(i), size);
        }
        slot.size(w, h);
        float[] pos = this.slotPos(slot, w, h, mc);
        int accent = this.hudAccent();
        Ui.push();
        Ui.move(pos[0], pos[1]);
        Ui.scale(slot.scale(), slot.scale());
        // Same plate, tinted towards the accent so the bottom strip reads as a status bar.
        Paint.box(g, 0.0F, 0.0F, w, h, Theme.alpha(Theme.lerp(0x0A0910, accent, 0.28F), 228), 8.0F);
        Paint.outline(g, 0.0F, 0.0F, w, h, Theme.alpha(accent, 46), 8.0F);
        float x = pad;
        float ty = (h - size) * 0.5F;
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) x += gap;
            if (glyphs.get(i) >= 0) {
                glyph(g, glyphs.get(i), x + 5.0F, h * 0.5F, Theme.alpha(Theme.ACCENT_HOT, 235));
                x += 14.0F;
            }
            Paint.text(g, texts.get(i), x, ty, Theme.TEXT, size);
            x += Paint.tw(texts.get(i), size);
        }
        Ui.pop();
    }

    private void paintCoordsPlain(DrawContext g, MinecraftClient mc, HudSlot slot, ClientPlayerEntity player) {
        String[] labels = {"X", "Y", "Z"};
        String[] values = {Integer.toString((int)Math.floor(player.getX())), Integer.toString((int)Math.floor(player.getY())), Integer.toString((int)Math.floor(player.getZ()))};
        float size = 7.6F, pad = 9.0F, gap = 9.0F, lead = 4.0F;
        float w = pad * 2.0F - gap;
        for (int i = 0; i < labels.length; i++) w += Paint.tw(labels[i], size) + lead + Paint.tw(values[i], size) + gap;
        float h = 18.0F;
        slot.size(w, h);
        float[] pos = this.slotPos(slot, w, h, mc);
        int accent = this.hudAccent();
        Ui.push();
        Ui.move(pos[0], pos[1]);
        Ui.scale(slot.scale(), slot.scale());
        this.hudPlate(g, w, h, accent);
        float x = pad, y = (h - size) * 0.5F;
        for (int i = 0; i < labels.length; i++) {
            Paint.text(g, labels[i], x, y, Theme.alpha(accent, 215), size);
            x += Paint.tw(labels[i], size) + lead;
            Paint.text(g, values[i], x, y, Theme.TEXT, size);
            x += Paint.tw(values[i], size) + gap;
        }
        Ui.pop();
    }

    private static String worldClock(MinecraftClient mc) {
        World world = mc.world;
        if (world == null) return "--:--";
        long time = world.getTimeOfDay() % 24000L;
        int hh = (int)((time / 1000L + 6L) % 24L);
        int mm = (int)(time % 1000L * 60L / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d", hh, mm);
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
        Ui.push();
        Ui.move(afloat[0], afloat[1]);
        Ui.scale(hudslot.scale(), hudslot.scale());
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

        Ui.pop();
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
        if (i == 0 && !flag && this.potionCount == 0) {
            hudslot.size(28.0F, 24.0F);
        } else {
            boolean flag1 = i > 0;
            int j = flag1 ? i : 4;
            float f = 8.0F + j * 20.0F + (this.potionCount > 0 ? 34.0F : 0.0F);
            float f1 = 24.0F;
            hudslot.size(f, f1);
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), minecraftclient);
            int k = this.hudAccent();
            Ui.push();
            Ui.move(afloat[0], afloat[1]);
            Ui.scale(hudslot.scale(), hudslot.scale());
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

            if (this.potionCount > 0) {
                // A flask and the number of potions in the inventory.
                float px = 6 + l * 20 + 4.0F;
                Paint.circle(g, px + 4.0F, 14.0F, 4.2F, Theme.alpha(0xB06CFF, 210));
                Paint.box(g, px + 2.6F, 5.0F, 2.8F, 5.0F, Theme.alpha(0xFFFFFF, 190), 0.8F);
                Paint.text(g, "x" + this.potionCount, px + 11.0F, 8.5F, Theme.TEXT, 7.0F);
            }

            Ui.pop();
        }
    }

    private void paintEffects(DrawContext g, MinecraftClient mc, boolean editor) {
        this.refreshEffects(mc);
        HudSlot slot = this.slots.get("effects");
        this.effectIcons.clear();
        if (this.effectRows.isEmpty() && !editor) {
            return;
        }
        float head = 20.0F;
        float rowH = 18.0F;
        float ic = 12.0F;
        float w = 150.0F;
        for (HudFeature.EffectRow r : this.effectRows) {
            float need = 8.0F + ic + 6.0F + Paint.tw(r.name(), 7.2F) + 14.0F + Paint.tw(r.time(), 6.6F) + 18.0F;
            if (!r.level().isEmpty()) need += 8.0F + Paint.tw("LVL. " + r.level(), 6.4F);
            w = Math.max(w, need);
        }
        float h = head + Math.max(1, this.effectRows.size()) * rowH + 4.0F;
        slot.size(w, h);
        float[] pos = this.slotPos(slot, w, h, mc);
        int accent = this.hudAccent();
        float scale = Math.max(0.01F, slot.scale());
        Ui.push();
        Ui.move(pos[0], pos[1]);
        Ui.scale(scale, scale);
        this.hudPlate(g, w, h, accent);
        Paint.text(g, "Effects", 9.0F, (head - 8.0F) * 0.5F, Theme.TEXT, 8.0F);
        glyph(g, 2, w - 12.0F, head * 0.5F, accent);
        float y = head;
        if (this.effectRows.isEmpty()) {
            Paint.text(g, "нет эффектов", 9.0F, y + 4.0F, Theme.MUTED, 7.0F);
        } else {
            for (HudFeature.EffectRow r : this.effectRows) {
                this.effectIcons.add(new HudFeature.IconBlit(r.icon(),
                    Math.round(screen(pos[0], 8.0F, scale)), Math.round(screen(pos[1], y + (rowH - ic) * 0.5F, scale)), Math.max(8, Math.round(ic * scale))));
                float tx = 8.0F + ic + 6.0F;
                Paint.text(g, r.name(), tx, y + (rowH - 7.2F) * 0.5F, Theme.TEXT, 7.2F);
                if (!r.level().isEmpty()) {
                    Paint.text(g, "LVL. " + r.level(), tx + Paint.tw(r.name(), 7.2F) + 8.0F, y + (rowH - 6.4F) * 0.5F, accent, 6.4F);
                }
                int tc = r.warn() ? Theme.WARN : Theme.TEXT;
                Paint.textR(g, r.time(), w - 9.0F, y + (rowH - 6.6F) * 0.5F, tc, 6.6F);
                glyph(g, 4, w - 9.0F - Paint.tw(r.time(), 6.6F) - 8.0F, y + rowH * 0.5F, accent);
                y += rowH;
            }
        }
        Ui.pop();
    }

    /** Enabled modules that have a key bound, name and key, sorted by name. Rebuilt a few times a second, not every frame. */
    private List<String[]> hotkeyEntries() {
        long now = System.currentTimeMillis();
        if (now - this.hotCacheAt < 200L) {
            return this.hotCache;
        }
        this.hotCacheAt = now;
        List<String[]> rows = new ArrayList<>();
        for (Feature f : App.features().all()) {
            Opt.Key bind = f.toggleBind();
            if (f.on() && bind != null && bind.get() != -1) {
                rows.add(new String[] {f.name(), Keys.name(bind.get())});
            }
        }
        rows.sort((a, b) -> a[0].compareToIgnoreCase(b[0]));
        this.hotCache = rows.size() > 8 ? new ArrayList<>(rows.subList(0, 8)) : rows;
        return this.hotCache;
    }

    private void paintHotkeys(DrawContext g, MinecraftClient mc, boolean editor) {
        HudSlot slot = this.slots.get("hotkeys");
        if (slot == null) {
            return;
        }
        List<String[]> current = this.hotkeyEntries();
        if (!current.isEmpty()) {
            this.hotLast = current;
        } else if (editor && this.hotLast.isEmpty()) {
            this.hotLast = new ArrayList<>();
            this.hotLast.add(new String[] {"Module", "R"});
        }
        // The window eases in when the first bound module is switched on and eases out after the last one is switched off;
        // the rows it showed last stay on screen for that fade.
        float k = this.hotAppear.springOpen(!current.isEmpty() || editor ? 1.0F : 0.0F, this.frameDt);
        if (k < 0.01F || this.hotLast.isEmpty()) {
            slot.size(0.0F, 0.0F);
            return;
        }
        List<String[]> rows = this.hotLast;
        float head = 20.0F;
        float rowH = 15.0F;
        float w = 96.0F;
        for (String[] r : rows) {
            w = Math.max(w, 9.0F + Paint.tw(r[0], 6.6F) + 14.0F + Paint.tw(r[1], 6.6F) + 9.0F);
        }
        float h = head + rows.size() * rowH + 3.0F;
        slot.size(w, h);
        float[] pos = this.slotPos(slot, w, h, mc);
        int accent = this.hudAccent();
        Ui.push();
        this.popIn(k, pos, w, h, Math.max(0.01F, slot.scale()));
        this.hudPlate(g, w, h, accent);
        Paint.text(g, "HotKeys", 9.0F, (head - 8.0F) * 0.5F, Theme.TEXT, 8.0F);
        glyph(g, 3, w - 12.0F, head * 0.5F, accent);
        float y = head;
        for (String[] r : rows) {
            float ty = y + (rowH - 6.6F) * 0.5F;
            Paint.text(g, r[0], 9.0F, ty, Theme.MUTED, 6.6F);
            Paint.textR(g, r[1], w - 9.0F, ty, accent, 6.6F);
            y += rowH;
        }
        Ui.pop();
    }

    private void paintCooldowns(DrawContext g, MinecraftClient mc, boolean editor) {
        HudSlot hudslot = this.slots.get("cooldowns");
        List<CooldownsFeature.Cd> list = CooldownsFeature.active();
        int i = Math.max(1, editor && list.isEmpty() ? 4 : list.size());
        float f = Math.max(96.0F, 10.0F + i * 22.0F);
        float f1 = 24.0F;
        hudslot.size(f, f1);
        float[] afloat = this.slotPos(hudslot, f, f1, mc);
        Ui.push();
        Ui.move(afloat[0], afloat[1]);
        Ui.scale(hudslot.scale(), hudslot.scale());
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

        Ui.pop();
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
        LivingEntity ent = App.aim().get();
        boolean live = App.aim().hot() && ent != null;
        // With the chat open the plate is shown for yourself, so it can be placed and scaled.
        boolean preview = !live && this.editingLayout() && mc.player != null;
        if (preview) {
            ent = mc.player;
        }
        float show = this.targetShow.springOpen(live || preview ? 1.0F : 0.0F, this.frameDt);
        if (show < 0.03F || ent == null) {
            this.targetHeadReady = false;
            return;
        }
        float hpFrac = Math.max(0.0F, Math.min(1.0F, ent.getHealth() / Math.max(1.0F, ent.getMaxHealth())));
        float hpShown = this.targetHp.to(hpFrac, 22.0F);
        String name = ent.getName().getString();
        if (name.length() > 16) {
            name = name.substring(0, 15) + "\u2026";
        }
        String hpText = String.format(Locale.ROOT, "%.1f", ent.getHealth());
        String dist = preview || mc.player == null ? "" : String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(ent));
        for (int i = 0; i < this.targetArmor.length; i++) {
            this.targetArmor[i] = ent.getEquippedStack(ARMOR[i]);
        }

        HudSlot slot = this.slots.get("target_hud");
        float w = 170.0F;
        float h = 46.0F;
        slot.size(w, h);
        float[] pos = this.resolveSlot(slot, w, h, mc);
        int accent = hpFrac < 0.3F ? Theme.BAD : (hpFrac < 0.55F ? Theme.WARN : this.hudAccent());
        boolean hearts = "\u0421\u0435\u0440\u0434\u0446\u0430".equals(this.hpMode.get());
        float scale = Math.max(0.01F, slot.scale());
        float appear = Math.max(0.0F, Math.min(1.0F, show));
        float eff = scale * (0.92F + 0.08F * appear);
        float ox = pos[0] + w * (scale - eff) * 0.5F;
        float oy = pos[1] + h * (scale - eff) * 0.5F + (1.0F - appear) * 8.0F;
        Ui.push();
        try {
            this.popIn(show, pos, w, h, scale);
            this.hudPlate(g, w, h, accent);
            // face frame; the skin itself is blitted afterwards in paintItems
            Paint.box(g, 5.0F, 5.0F, 36.0F, 36.0F, Theme.alpha(0xFFFFFF, 16), 7.0F);
            float tx = 48.0F;
            Paint.text(g, name, tx, 7.0F, Theme.TEXT, 8.2F);
            float hw = Paint.tw(hpText, 7.4F);
            Paint.textR(g, hpText, w - 8.0F, 7.6F, Theme.alpha(accent, 235), 7.4F);
            Paint.heart(g, w - 8.0F - hw - 10.0F, 8.2F, 7.0F, Theme.alpha(accent, 235));
            if (hearts) {
                int total = Math.min(10, Math.max(1, Math.round(ent.getMaxHealth() / 2.0F)));
                int full = Math.max(0, Math.min(total, Math.round(ent.getHealth() / 2.0F)));
                for (int i = 0; i < total; i++) {
                    Paint.heart(g, tx + i * 9, 20.0F, 8.0F, i < full ? accent : Theme.alpha(0xFFFFFF, 35));
                }
            } else {
                Paint.bar(g, tx, 21.0F, w - tx - 8.0F, 5.0F, hpShown, accent);
            }
            // armour row: slot backs and durability; the items are drawn in paintItems
            for (int i = 0; i < this.targetArmor.length; i++) {
                float ax = tx + i * 14.0F;
                Paint.box(g, ax, 29.0F, 12.0F, 12.0F, Theme.alpha(0xFFFFFF, 12), 3.0F);
                ItemStack piece = this.targetArmor[i];
                if (piece != null && !piece.isEmpty() && piece.isDamageable()) {
                    float left = 1.0F - (float)piece.getDamage() / Math.max(1, piece.getMaxDamage());
                    Paint.bar(g, ax + 1.0F, 40.0F, 10.0F, 1.6F, left, left < 0.25F ? Theme.BAD : Theme.OK);
                }
            }
            if (!dist.isEmpty()) {
                Paint.textR(g, dist, w - 8.0F, 31.5F, Theme.MUTED, 6.6F);
            }
        } finally {
            Ui.pop();
        }
        this.targetEntity = ent;
        this.targetAlpha = appear;
        this.targetHeadS = Math.round(34.0F * eff);
        this.targetHeadX = Math.round(screen(ox, 6.0F, eff));
        this.targetHeadY = Math.round(screen(oy, 6.0F, eff));
        this.targetIconS = 0.75F * eff;
        this.targetIconX = screen(ox, 48.0F, eff);
        this.targetIconY = screen(oy, 29.0F, eff);
        this.targetHeadReady = true;
    }

    private void paintInventoryPlate(DrawContext g) {
        this.refreshInventory();
        HudSlot hudslot = this.slots.get("inventory");
        hudslot.size(176.0F, 80.0F);
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        float[] afloat = this.slotPos(hudslot, hudslot.w(), hudslot.h(), minecraftclient);
        int i = this.hudAccent();
        Ui.push();

        try {
            Ui.move(afloat[0], afloat[1]);
            Ui.scale(hudslot.scale(), hudslot.scale());
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
            Ui.pop();
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
            Ui.push();
            Ui.move(afloat[0], afloat[1]);
            Ui.scale(hudslot.scale(), hudslot.scale());
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
                    Ui.push();
                    Ui.alpha(f7);
                    Ui.move(f5, 0.0F);
                    Paint.box(g, 0.0F, f2, 180.0F, 26.0F, Theme.alpha(461583, 230), 8.0F);
                    Paint.outline(g, 0.0F, f2, 180.0F, 26.0F, Theme.alpha(16777215, 18), 8.0F);
                    Paint.box(g, 0.0F, f2 + 5.0F, 2.4F, 16.0F, l1, 1.2F);
                    float f8 = 180.0F * Math.max(0.0F, f3);
                    Paint.box(g, 0.0F, f2 + 24.4F, f8, 1.6F, Theme.alpha(l1, 180), 0.0F);
                    Paint.text(g, hudfeature$note.title, 12.0F, f2 + 4.0F, Theme.TEXT, 7.6F);
                    Paint.text(g, hudfeature$note.body, 12.0F, f2 + 14.0F, Theme.MUTED, 6.4F);
                    Ui.pop();
                    f2 += f;
                }
            }

            Ui.pop();
        }
    }

    private int potionCount;

    private void refreshArmor() {
        long i = System.currentTimeMillis();
        if (i >= this.armorNext) {
            this.armorNext = i + 150L;
            ClientPlayerEntity clientplayerentity = MinecraftClient.getInstance().player;
            if (clientplayerentity != null) {
                int potions = 0;
                PlayerInventory inventory = clientplayerentity.getInventory();
                for (int k = 0; k < 41; k++) {
                    ItemStack st = inventory.getStack(k);
                    if (st.isOf(Items.POTION) || st.isOf(Items.SPLASH_POTION) || st.isOf(Items.LINGERING_POTION)) {
                        potions += st.getCount();
                    }
                }
                this.potionCount = potions;
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
                    String s1 = j <= 0 ? "\u221e" : l / 60 + ":" + String.format(Locale.ROOT, "%02d", l % 60);
                    String s2 = ((StatusEffect)statuseffectinstance.getEffectType().value()).getName().getString();
                    String level = statuseffectinstance.getAmplifier() > 0 ? String.valueOf(statuseffectinstance.getAmplifier() + 1) : "";

                    if (s2.length() > 14) {
                        s2 = s2.substring(0, 13) + "\u2026";
                    }

                    float f = j <= 0 ? 1.0F : Math.max(0.0F, Math.min(1.0F, (float)j / Math.max(1, k)));
                    boolean flag = j > 0 && j <= 200;
                    this.effectRows.add(new HudFeature.EffectRow(effectIcon(s), s2, s1, f, flag, level));
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
            return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
        }
    }

    private record EffectRow(Identifier icon, String name, String time, float progress, boolean warn, String level) {
    }

    private record IconBlit(Identifier tex, int x, int y, int s) {
    }

    private record PlateIcon(Identifier tex, int x, int y, int s, int color) {
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
