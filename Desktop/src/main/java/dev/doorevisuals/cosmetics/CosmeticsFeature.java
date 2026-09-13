package dev.doorevisuals.cosmetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.net.CosmeticsNet;
import dev.doorevisuals.overlay.HudFeature;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CosmeticsFeature extends Feature implements Tick {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Cosmetics");
    private static final Identifier CAPE_DOORE = Identifier.of("doorevisuals", "textures/cosmetics/cape_doore.png");
    private static final Identifier CAPE_DARK = Identifier.of("doorevisuals", "textures/cosmetics/cape_dark.png");
    private static final Identifier CAPE_BROS = Identifier.of("doorevisuals", "textures/cosmetics/cape_bros.png");
    private static final Identifier CAPE_WEDDING = Identifier.of("doorevisuals", "textures/cosmetics/cape_wedding.png");
    private static final Identifier CAPE_SQUIRREL = Identifier.of("doorevisuals", "textures/cosmetics/cape_squirrel.png");
    private static final Identifier WINGS_DOORE = Identifier.of("doorevisuals", "textures/cosmetics/wings_doore.png");
    private static final Identifier WINGS_GHOST = Identifier.of("doorevisuals", "textures/cosmetics/wings_ghost.png");
    private final Opt.Pick cape = this.opt(
        new Opt.Pick(
            "cape",
            "\u041f\u043b\u0430\u0449",
            "\u041d\u0435\u0442",
            "\u041d\u0435\u0442",
            "Doore",
            "\u0422\u0451\u043c\u043d\u044b\u0439",
            "\u0411\u0440\u0430\u0442\u044c\u044f",
            "\u0421\u0432\u0430\u0434\u044c\u0431\u0430",
            "\u0411\u0435\u043b\u043a\u0430",
            "\u041d\u0435\u043e\u043d"
        )
    );
    private final Opt.Pick wings = this.opt(
        new Opt.Pick(
            "wings",
            "\u041a\u0440\u044b\u043b\u044c\u044f",
            "\u041d\u0435\u0442",
            "\u041d\u0435\u0442",
            "Doore",
            "\u041f\u0440\u0438\u0437\u0440\u0430\u043a",
            "\u0418\u0441\u043a\u0440\u0430"
        )
    );
    private final Opt.Pick hat = this.opt(
        new Opt.Pick(
            "hat",
            "\u0428\u043b\u044f\u043f\u0430",
            "\u041d\u0435\u0442",
            "\u041d\u0435\u0442",
            "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f",
            "\u0410\u043d\u0433\u0435\u043b",
            "67"
        )
    );
    private final Opt.Pick accessory = this.opt(
        new Opt.Pick(
            "accessory",
            "\u0410\u043a\u0441\u0435\u0441\u0441\u0443\u0430\u0440",
            "\u041d\u0435\u0442",
            "\u041d\u0435\u0442",
            "\u0423\u0448\u0438",
            "\u0420\u043e\u0433\u0430",
            "\u0411\u0430\u043d\u0434\u0430\u043d\u0430"
        )
    );
    private final Opt.Flag badge = this.opt(new Opt.Flag("badge", "\u0417\u043d\u0430\u0447\u043e\u043a Doore \u0443 \u043d\u0438\u043a\u0430", true));
    private final Opt.Flag trails = this.opt(
        new Opt.Flag("trails", "\u0421\u043b\u0435\u0434 \u043f\u043b\u0430\u0449\u0430 / \u043a\u0440\u044b\u043b\u044c\u0435\u0432", true)
    );
    private final Opt.Flag animated = this.opt(
        new Opt.Flag("animated", "\u0410\u043d\u0438\u043c\u0438\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0439 \u043f\u043b\u0430\u0449", false)
    );
    private final Opt.Pick profile = this.opt(new Opt.Pick("profile", "\u041f\u0440\u043e\u0444\u0438\u043b\u044c", "1", "1", "2", "3"));
    private final Opt.Text slotData = this.opt(new Opt.Text("slot_data", "\u041f\u0440\u043e\u0444\u0438\u043b\u0438", "{}").visibleWhen(() -> false));
    private final String[] savedCape = new String[]{"\u041d\u0435\u0442", "\u041d\u0435\u0442", "\u041d\u0435\u0442"};
    private final String[] savedWings = new String[]{"\u041d\u0435\u0442", "\u041d\u0435\u0442", "\u041d\u0435\u0442"};
    private final boolean[] savedBadge = new boolean[]{true, true, true};
    private final boolean[] savedTrails = new boolean[]{true, true, true};
    private final boolean[] savedAnim = new boolean[]{false, false, false};
    private String lastProfile = "1";
    private String lastCape = "";
    private String lastWings = "";
    private String lastHat = "";
    private String lastAcc = "";
    private String lastSlotJson = "";
    private boolean lastAnimated;
    private long lastErrorAt;

    public CosmeticsFeature() {
        super(
            "cosmetics",
            "Cosmetics",
            "\u041f\u043b\u0430\u0449, \u043a\u0440\u044b\u043b\u044c\u044f, \u0448\u043b\u044f\u043f\u0430, \u0443\u0448\u0438. \u0414\u0440\u0443\u0433\u0438\u043c \u0432\u0438\u0434\u043d\u043e \u0442\u043e\u043b\u044c\u043a\u043e \u0441 Doore",
            Category.COSMETICS,
            true
        );
    }

    public boolean showBadge() {
        return (Boolean)this.badge.get();
    }

    public void setBadge(boolean on) {
        this.badge.set(on);
    }

    public String capeLabel() {
        return (String)this.cape.get();
    }

    public String wingsLabel() {
        return (String)this.wings.get();
    }

    public String hatLabel() {
        return (String)this.hat.get();
    }

    public String accessoryLabel() {
        return (String)this.accessory.get();
    }

    public void setHatLabel(String label) {
        this.hat.set(label);
        this.pushLook();
    }

    public void setAccessoryLabel(String label) {
        this.accessory.set(label);
        this.pushLook();
    }

    public void setCapeLabel(String label) {
        this.cape.set(label);
        this.pushLook();
    }

    public void setWingsLabel(String label) {
        this.wings.set(label);
        this.pushLook();
    }

    public String selectionSummary() {
        return "\u041f\u043b\u0430\u0449: "
            + (String)this.cape.get()
            + "  \u00b7  \u041a\u0440\u044b\u043b\u044c\u044f: "
            + (String)this.wings.get()
            + "  \u00b7  "
            + (String)this.hat.get()
            + " / "
            + (String)this.accessory.get();
    }

    public static Identifier capeTexture(PlayerEntityRenderState state) {
        if (state != null && App.live()) {
            CosmeticsFeature cosmeticsfeature = App.features().find(CosmeticsFeature.class).orElse(null);
            if (cosmeticsfeature != null && cosmeticsfeature.on()) {
                CapeFiles.scan();
                if ((Boolean)cosmeticsfeature.animated.get()) {
                    Identifier identifier = CapeAnim.current();
                    if (identifier != null) {
                        return identifier;
                    }
                }

                UUID uuid = uuidOf(state);
                if (uuid == null) {
                    return null;
                } else {
                    String s = CosmeticsRegistry.look(uuid).capeId();
                    return s != null && !s.isEmpty() ? textureOf(s) : null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static Identifier textureOf(String id) {
        Identifier identifier = CapeFiles.texture(id);
        if (identifier != null) {
            return identifier;
        } else {
            return switch (id) {
                case "dark" -> CAPE_DARK;
                case "bros" -> CAPE_BROS;
                case "wedding" -> CAPE_WEDDING;
                case "squirrel" -> CAPE_SQUIRREL;
                case "neon" -> CAPE_DOORE;
                case "doore" -> CAPE_DOORE;
                default -> null;
            };
        }
    }

    private static UUID uuidOf(PlayerEntityRenderState state) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null && minecraftclient.player.getId() == state.id) {
            return minecraftclient.player.getUuid();
        } else {
            if (minecraftclient.world != null) {
                Entity entity = minecraftclient.world.getEntityById(state.id);
                if (entity != null) {
                    return entity.getUuid();
                }
            }

            return null;
        }
    }

    public void draw(WorldRenderContext ctx, float tickDelta) {
        if (this.on()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.world != null) {
                try {
                    for (AbstractClientPlayerEntity abstractclientplayerentity : minecraftclient.world.getPlayers()) {
                        CosmeticsRegistry.Look cosmeticsregistry$look = CosmeticsRegistry.look(abstractclientplayerentity.getUuid());
                        if ((!cosmeticsregistry$look.wingsId().isEmpty() || !cosmeticsregistry$look.accessoryId().isEmpty())
                            && VisualQuality.inFrustumDist(
                                abstractclientplayerentity.getX(),
                                abstractclientplayerentity.getY() + abstractclientplayerentity.getHeight() * 0.5,
                                abstractclientplayerentity.getZ()
                            )) {
                            float f = MathHelper.lerpAngleDegrees(tickDelta, abstractclientplayerentity.lastBodyYaw, abstractclientplayerentity.bodyYaw);
                            Vec3d vec3d = abstractclientplayerentity.getLerpedPos(tickDelta);
                            CosmeticsFeature.Anchor cosmeticsfeature$anchor = torsoAnchor(abstractclientplayerentity, vec3d, f, tickDelta);
                            CosmeticsFeature.Motion cosmeticsfeature$motion = motion(abstractclientplayerentity, tickDelta);
                            boolean flag = abstractclientplayerentity instanceof ClientPlayerEntity
                                && minecraftclient.options.getPerspective() == Perspective.FIRST_PERSON;
                            if (!flag && !cosmeticsregistry$look.wingsId().isEmpty()) {
                                Identifier identifier = wingsTex(cosmeticsregistry$look.wingsId());
                                int j = "ghost".equals(cosmeticsregistry$look.wingsId())
                                    ? -1427572993
                                    : ("spark".equals(cosmeticsregistry$look.wingsId()) ? -285226934 : -285212673);
                                drawVolWing(
                                    ctx,
                                    identifier,
                                    j,
                                    cosmeticsfeature$anchor.x - cosmeticsfeature$anchor.bx * cosmeticsfeature$anchor.spread,
                                    cosmeticsfeature$anchor.y,
                                    cosmeticsfeature$anchor.z - cosmeticsfeature$anchor.bz * cosmeticsfeature$anchor.spread,
                                    f + 10.0F + cosmeticsfeature$motion.flap * 18.0F,
                                    true,
                                    cosmeticsfeature$motion.flap
                                );
                                drawVolWing(
                                    ctx,
                                    identifier,
                                    j,
                                    cosmeticsfeature$anchor.x + cosmeticsfeature$anchor.bx * cosmeticsfeature$anchor.spread,
                                    cosmeticsfeature$anchor.y,
                                    cosmeticsfeature$anchor.z + cosmeticsfeature$anchor.bz * cosmeticsfeature$anchor.spread,
                                    f - 10.0F - cosmeticsfeature$motion.flap * 18.0F,
                                    false,
                                    cosmeticsfeature$motion.flap
                                );
                            }

                            drawAccessory(ctx, abstractclientplayerentity, cosmeticsregistry$look.accessoryId(), vec3d, f, tickDelta);
                            if ((Boolean)this.trails.get()) {
                                int k = Theme.alpha(Theme.ACCENT_HOT, 90);
                                Mesh.softBillboard(
                                    ctx,
                                    cosmeticsfeature$anchor.x,
                                    cosmeticsfeature$anchor.y + 0.1,
                                    cosmeticsfeature$anchor.z,
                                    0.12F + Math.abs(cosmeticsfeature$motion.flap) * 0.08F,
                                    k
                                );
                                Mesh.softBillboard(
                                    ctx,
                                    cosmeticsfeature$anchor.x - cosmeticsfeature$anchor.bx * cosmeticsfeature$anchor.spread * 1.4,
                                    cosmeticsfeature$anchor.y - 0.08,
                                    cosmeticsfeature$anchor.z - cosmeticsfeature$anchor.bz * cosmeticsfeature$anchor.spread * 1.4,
                                    0.08F,
                                    Theme.alpha(Theme.ACCENT, 70)
                                );
                                Mesh.softBillboard(
                                    ctx,
                                    cosmeticsfeature$anchor.x + cosmeticsfeature$anchor.bx * cosmeticsfeature$anchor.spread * 1.4,
                                    cosmeticsfeature$anchor.y - 0.08,
                                    cosmeticsfeature$anchor.z + cosmeticsfeature$anchor.bz * cosmeticsfeature$anchor.spread * 1.4,
                                    0.08F,
                                    Theme.alpha(Theme.ACCENT, 70)
                                );
                            }
                        }
                    }
                } catch (Throwable throwable) {
                    long i = System.currentTimeMillis();
                    if (i - this.lastErrorAt > 4000L) {
                        this.lastErrorAt = i;
                        LOG.warn("Cosmetics draw failed", throwable);
                    }
                }
            }
        }
    }

    private static CosmeticsFeature.Motion motion(AbstractClientPlayerEntity p, float tickDelta) {
        Vec3d vec3d = p.getVelocity();
        float f = (float)Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        boolean flag = p.isGliding();
        boolean flag1 = p.isSprinting();
        float f1 = flag ? 0.22F : (flag1 ? 0.14F : 0.06F);
        float f2 = flag ? 0.42F : (flag1 ? 0.28F : 0.14F);
        f1 *= 0.55F + Math.min(1.2F, f * 6.0F);
        float f3 = (p.age + tickDelta) * f2;
        float f4 = (float)Math.sin(f3) * f1;
        return new CosmeticsFeature.Motion(f4);
    }

    private static CosmeticsFeature.Anchor torsoAnchor(AbstractClientPlayerEntity p, Vec3d pos, float bodyYaw, float tickDelta) {
        double d0 = Math.toRadians(bodyYaw);
        double d1 = -Math.sin(d0);
        double d2 = Math.cos(d0);
        double d3 = Math.cos(d0);
        double d4 = Math.sin(d0);
        float f = p.getHeight();
        float f1 = p.getScale();
        double d5 = 0.22 * f1;
        float f2 = 0.2F * f1;
        double d6 = pos.y + f * 0.64;
        EntityPose entitypose = p.getPose();
        if (p.isInSneakingPose() || entitypose == EntityPose.CROUCHING) {
            d6 = pos.y + f * 0.52;
            d5 = 0.3 * f1;
        }

        if (p.hasVehicle()) {
            d6 -= 0.12 * f1;
            d5 += 0.04 * f1;
        }

        if (entitypose == EntityPose.SWIMMING || entitypose == EntityPose.SPIN_ATTACK || p.isInSwimmingPose() || p.isGliding()) {
            float f3 = MathHelper.lerp(tickDelta, p.lastPitch, p.getPitch());
            double d7 = Math.toRadians(f3);
            d6 = pos.y + f * 0.42 + Math.sin(-d7) * 0.18 * f1;
            d5 = 0.34 * f1 + Math.cos(d7) * 0.08 * f1;
            f2 = 0.18F * f1;
        }

        return new CosmeticsFeature.Anchor(pos.x - d1 * d5, d6, pos.z - d2 * d5, d3, d4, f2);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null) {
            this.cape.replaceOptions(CapeFiles.allCapeLabels());
            if ((Boolean)this.animated.get()) {
                if (!this.lastAnimated) {
                    CapeAnim.invalidate();
                }

                this.lastAnimated = true;
                CapeAnim.tick();
            } else {
                this.lastAnimated = false;
            }

            this.hydrateSlots();
            String s = (String)this.profile.get();
            if (!s.equals(this.lastProfile)) {
                this.storeSlot(this.lastProfile);
                this.loadSlot(s);
                this.lastProfile = s;
                App.features()
                    .find(HudFeature.class)
                    .ifPresent(
                        hx -> hx.islandEvent("\u041f\u0440\u043e\u0444\u0438\u043b\u044c", "\u041a\u043e\u0441\u043c\u0435\u0442\u0438\u043a\u0430 " + s)
                    );
            } else {
                this.storeSlot(s);
            }

            String s1 = mapCape((String)this.cape.get());
            String s2 = mapWings((String)this.wings.get());
            String s3 = mapHat((String)this.hat.get());
            String s4 = mapAcc((String)this.accessory.get());
            if (!s1.equals(this.lastCape) || !s2.equals(this.lastWings) || !s3.equals(this.lastHat) || !s4.equals(this.lastAcc)) {
                this.lastCape = s1;
                this.lastWings = s2;
                this.lastHat = s3;
                this.lastAcc = s4;
                CosmeticsNet.sendCosmetics(mc.player.getUuid(), s1, s2, s3, s4);
            }
        }
    }

    private void pushLook() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null) {
            String s = mapCape((String)this.cape.get());
            String s1 = mapWings((String)this.wings.get());
            String s2 = mapHat((String)this.hat.get());
            String s3 = mapAcc((String)this.accessory.get());
            this.lastCape = s;
            this.lastWings = s1;
            this.lastHat = s2;
            this.lastAcc = s3;
            CosmeticsNet.sendCosmetics(minecraftclient.player.getUuid(), s, s1, s2, s3);
        }
    }

    private void storeSlot(String slot) {
        int i = index(slot);
        this.savedCape[i] = (String)this.cape.get();
        this.savedWings[i] = (String)this.wings.get();
        this.savedBadge[i] = (Boolean)this.badge.get();
        this.savedTrails[i] = (Boolean)this.trails.get();
        this.savedAnim[i] = (Boolean)this.animated.get();
        String s = this.encodeSlots();
        if (!s.equals(this.slotData.get())) {
            this.lastSlotJson = s;
            this.slotData.set(s);
        }
    }

    private void loadSlot(String slot) {
        int i = index(slot);
        this.cape.set(this.savedCape[i]);
        this.wings.set(this.savedWings[i]);
        this.badge.set(this.savedBadge[i]);
        this.trails.set(this.savedTrails[i]);
        this.animated.set(this.savedAnim[i]);
        if ((Boolean)this.animated.get()) {
            CapeAnim.invalidate();
        }

        this.pushLook();
    }

    private void hydrateSlots() {
        String s = (String)this.slotData.get();
        if (s == null) {
            s = "";
        }

        if (!s.equals(this.lastSlotJson)) {
            this.lastSlotJson = s;
            this.readSlots(s);
            this.lastProfile = (String)this.profile.get();
        }
    }

    private String encodeSlots() {
        JsonObject jsonobject = new JsonObject();

        for (int i = 0; i < 3; i++) {
            JsonObject jsonobject1 = new JsonObject();
            jsonobject1.addProperty("cape", this.savedCape[i]);
            jsonobject1.addProperty("wings", this.savedWings[i]);
            jsonobject1.addProperty("badge", this.savedBadge[i]);
            jsonobject1.addProperty("trails", this.savedTrails[i]);
            jsonobject1.addProperty("anim", this.savedAnim[i]);
            jsonobject.add(String.valueOf(i + 1), jsonobject1);
        }

        return jsonobject.toString();
    }

    private void readSlots(String raw) {
        if (raw != null && !raw.isBlank() && !"{}".equals(raw.trim())) {
            try {
                JsonObject jsonobject = JsonParser.parseString(raw).getAsJsonObject();

                for (int i = 0; i < 3; i++) {
                    String s = String.valueOf(i + 1);
                    if (jsonobject.has(s) && jsonobject.get(s).isJsonObject()) {
                        JsonObject jsonobject1 = jsonobject.getAsJsonObject(s);
                        if (jsonobject1.has("cape")) {
                            this.savedCape[i] = jsonobject1.get("cape").getAsString();
                        }

                        if (jsonobject1.has("wings")) {
                            this.savedWings[i] = jsonobject1.get("wings").getAsString();
                        }

                        if (jsonobject1.has("badge")) {
                            this.savedBadge[i] = jsonobject1.get("badge").getAsBoolean();
                        }

                        if (jsonobject1.has("trails")) {
                            this.savedTrails[i] = jsonobject1.get("trails").getAsBoolean();
                        }

                        if (jsonobject1.has("anim")) {
                            this.savedAnim[i] = jsonobject1.get("anim").getAsBoolean();
                        }
                    }
                }
            } catch (Exception exception) {
            }
        }
    }

    private static int index(String slot) {
        return switch (slot) {
            case "2" -> 1;
            case "3" -> 2;
            default -> 0;
        };
    }

    private static String mapCape(String label) {
        if (CapeFiles.isFileLabel(label)) {
            return label;
        } else {
            return switch (label) {
                case "Doore" -> "doore";
                case "\u0422\u0451\u043c\u043d\u044b\u0439" -> "dark";
                case "\u0411\u0440\u0430\u0442\u044c\u044f" -> "bros";
                case "\u0421\u0432\u0430\u0434\u044c\u0431\u0430" -> "wedding";
                case "\u0411\u0435\u043b\u043a\u0430" -> "squirrel";
                case "\u041d\u0435\u043e\u043d" -> "neon";
                default -> "";
            };
        }
    }

    private static String mapWings(String label) {
        if (CapeFiles.isFileLabel(label)) {
            return label;
        } else {
            return switch (label) {
                case "Doore" -> "doore";
                case "\u041f\u0440\u0438\u0437\u0440\u0430\u043a" -> "ghost";
                case "\u0418\u0441\u043a\u0440\u0430" -> "spark";
                default -> "";
            };
        }
    }

    private static String mapHat(String label) {
        return switch (label) {
            case "\u041a\u0438\u0442\u0430\u0439\u0441\u043a\u0430\u044f" -> "china";
            case "\u0410\u043d\u0433\u0435\u043b" -> "halo";
            case "67" -> "67";
            default -> "";
        };
    }

    private static String mapAcc(String label) {
        return switch (label) {
            case "\u0423\u0448\u0438" -> "ears";
            case "\u0420\u043e\u0433\u0430" -> "horns";
            case "\u0411\u0430\u043d\u0434\u0430\u043d\u0430" -> "bandana";
            default -> "";
        };
    }

    private static Identifier wingsTex(String id) {
        Identifier identifier = CapeFiles.texture(id);
        if (identifier != null) {
            return identifier;
        } else {
            return "ghost".equals(id) ? WINGS_GHOST : WINGS_DOORE;
        }
    }

    private static void drawAccessory(WorldRenderContext ctx, AbstractClientPlayerEntity p, String acc, Vec3d pos, float bodyYaw, float tickDelta) {
        if (acc != null && !acc.isEmpty()) {
            if (!(p instanceof ClientPlayerEntity) || MinecraftClient.getInstance().options.getPerspective() != Perspective.FIRST_PERSON) {
                double d0 = pos.x;
                double d1 = pos.y + p.getStandingEyeHeight() + 0.08;
                double d2 = pos.z;
                int i = Theme.EFFECT;
                switch (acc) {
                    case "ears":
                        double d6 = Math.toRadians(bodyYaw);
                        double d7 = Math.cos(d6) * 0.16;
                        double d8 = Math.sin(d6) * 0.16;
                        Mesh.orb(ctx, d0 - d7, d1 + 0.12, d2 - d8, 0.09F, Mesh.alpha(i, 0.92F));
                        Mesh.orb(ctx, d0 + d7, d1 + 0.12, d2 + d8, 0.09F, Mesh.alpha(i, 0.92F));
                        break;
                    case "horns":
                        double d3 = Math.toRadians(bodyYaw);
                        double d4 = Math.cos(d3) * 0.12;
                        double d5 = Math.sin(d3) * 0.12;
                        Mesh.capsule(ctx, d0 - d4, d1 + 0.18, d2 - d5, 0.035F, 0.18F, 8, 3, Mesh.alpha(i, 0.95F), Types.fill());
                        Mesh.capsule(ctx, d0 + d4, d1 + 0.18, d2 + d5, 0.035F, 0.18F, 8, 3, Mesh.alpha(i, 0.95F), Types.fill());
                        break;
                    case "bandana":
                        Mesh.softBillboard(ctx, d0, d1 - 0.06, d2, 0.28F, Mesh.alpha(i, 0.55F));
                }
            }
        }
    }

    private static void drawVolWing(WorldRenderContext ctx, Identifier tex, int tint, double x, double y, double z, float yaw, boolean left, float flap) {
        float f = 1.25F;
        float f1 = left ? 1.0F : -1.0F;
        float f2 = left ? 0.14F : 0.56F;
        float f3 = left ? 0.44F : 0.86F;
        double d0 = Math.toRadians(yaw);
        double d1 = Math.sin(d0);
        double d2 = -Math.cos(d0);
        Mesh.yawQuad(ctx, tex, x, y + 0.025, z, yaw, 0.39999998F, 0.45000002F, 0.05F + flap * 0.08F, f2, 0.18F, f3, 0.84F, tint);
        Mesh.yawQuad(
            ctx,
            tex,
            x + d1 * 0.04 * 1.25 + f1 * 0.015 * 1.25,
            y - 0.075,
            z + d2 * 0.04 * 1.25,
            yaw + f1 * (8.0F + flap * 10.0F),
            0.29999998F,
            0.35F,
            0.07F + flap * 0.09F,
            f2 + (f3 - f2) * 0.1F,
            0.3F,
            f3,
            0.88F,
            Theme.alpha(tint, 190)
        );
    }

    private record Anchor(double x, double y, double z, double bx, double bz, float spread) {
    }

    private record Motion(float flap) {
    }
}
