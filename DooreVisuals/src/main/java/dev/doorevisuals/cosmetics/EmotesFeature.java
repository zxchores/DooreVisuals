package dev.doorevisuals.cosmetics;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.ui.EmoteWheelScreen;
import dev.doorevisuals.net.CosmeticsNet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Emotes and gestures: a key opens a wheel of eight emotes, in pages (scroll or the arrows turn the page); hold the key,
 * point at a slot and let go, or click one. While an emote plays, the body of your own character is posed by it; it is visible from
 * the third person (the camera switches there for the emote if you let it, and returns).
 */
public final class EmotesFeature extends Feature implements Tick {
    public static final String[] NAMES = {
        "Помахать", "Победа", "Поклон", "Танец", "Задуматься", "6 7", "Салют", "Т-поза", "Фейспалм",
        "Робот", "Бег", "Кивок", "Нет", "Хлопки", "Боксёр", "Флекс", "Сердце",
        "Дрожь", "Удивление", "Указать", "Стоп", "Зов", "Плечи", "Две руки"
    };
    private static final long[] DURATION_MS = {
        2800L, 2400L, 2200L, 4200L, 3000L, 4000L, 2200L, 3000L, 2400L,
        4200L, 3500L, 2400L, 2400L, 3000L, 3500L, 3000L, 3200L,
        2500L, 2500L, 2200L, 2200L, 3000L, 2400L, 3000L
    };
    public static final int SLOTS = 8;

    private static EmotesFeature self;

    private final Opt.Key wheelKey = this.opt(new Opt.Key("wheel_key", "Клавиша колеса эмоций (удерживай)", GLFW.GLFW_KEY_B));
    private final Opt.Flag thirdPerson = this.opt(new Opt.Flag("third_person", "Показывать от третьего лица", true));
    private boolean wheelDown;
    private final Map<UUID, Remote> remote = new HashMap<>();

    private int playing = -1;
    private long startedAt;
    private Perspective restore;

    public EmotesFeature() {
        super(
                "emotes",
                "Emotes",
                "Колесо эмоций по клавише: 24 жеста на трёх страницах, от поклона до «6 7»",
                Category.COSMETICS,
                true
        );
        this.badge(Badge.NEW);
        self = this;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player == null) {
            this.stop(mc);
            this.remote.clear();
            return;
        }
        long now = System.currentTimeMillis();
        this.remote.values().removeIf(r -> now - r.at() > DURATION_MS[r.index()] + 500L);
        if (this.playing >= 0 && System.currentTimeMillis() - this.startedAt > DURATION_MS[this.playing]) {
            this.stop(mc);
        }
        int key = this.wheelKey.get();
        boolean pressed = key > 0 && mc.currentScreen == null && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        if (pressed && !this.wheelDown && this.on()) {
            mc.setScreen(new EmoteWheelScreen(key));
        }
        this.wheelDown = pressed;
    }

    /** The number of pages of the wheel. */
    public static int pages() {
        return (NAMES.length + SLOTS - 1) / SLOTS;
    }

    /** The name in that slot of that page, or null when the page has no emote there. */
    public static String at(int page, int slot) {
        int index = page * SLOTS + slot;
        return index < NAMES.length ? NAMES[index] : null;
    }

    /** Plays the emote of that name; the one already playing is stopped instead. */
    public static void play(String name) {
        EmotesFeature f = self;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (f == null || mc.player == null) {
            return;
        }
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                if (f.playing == i) {
                    f.stop(mc);
                } else {
                    f.start(mc, i);
                }
                return;
            }
        }
    }

    /** What another player is playing, as their client told ours through the server. */
    private record Remote(int index, long at) {
    }

    /** An emote of another player began (index) or ended (negative). */
    public static void received(UUID id, int index) {
        EmotesFeature f = self;
        if (f == null || id == null) {
            return;
        }
        if (index < 0 || index >= NAMES.length) {
            f.remote.remove(id);
        } else {
            f.remote.put(id, new Remote(index, System.currentTimeMillis()));
        }
    }

    public static void forgetRemote() {
        EmotesFeature f = self;
        if (f != null) {
            f.remote.clear();
        }
    }

    private void start(MinecraftClient mc, int index) {
        this.playing = index;
        this.startedAt = System.currentTimeMillis();
        CosmeticsNet.sendEmote(mc.player.getUuid(), index);
        if (this.thirdPerson.get() && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            this.restore = Perspective.FIRST_PERSON;
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    private void stop(MinecraftClient mc) {
        if (this.playing >= 0 && mc.player != null) {
            CosmeticsNet.sendEmote(mc.player.getUuid(), -1);
        }
        this.playing = -1;
        if (this.restore != null && mc.options != null) {
            mc.options.setPerspective(this.restore);
        }
        this.restore = null;
    }

    /**
     * Called by the player-model mixin after the game has posed the model: when the model is the player's own and an emote is
     * playing, the parts are blended towards the pose of the emote (fading in and out at the ends, so nothing snaps).
     */
    public static void pose(PlayerEntityModel model, PlayerEntityRenderState state) {
        EmotesFeature f = self;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (f == null || !f.on() || mc.player == null || mc.world == null) {
            return;
        }
        int emote;
        long startedAt;
        if (state.id == mc.player.getId()) {
            emote = f.playing;
            startedAt = f.startedAt;
        } else {
            Entity other = mc.world.getEntityById(state.id);
            Remote remote = other == null ? null : f.remote.get(other.getUuid());
            if (remote == null) {
                return;
            }
            emote = remote.index();
            startedAt = remote.at();
        }
        if (emote < 0 || System.currentTimeMillis() - startedAt > DURATION_MS[emote]) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startedAt;
        float t = MathHelper.clamp(elapsed / (float)DURATION_MS[emote], 0.0F, 1.0F);
        // Fades in over the first eighth and out over the last eighth.
        float w = Math.min(1.0F, Math.min(t / 0.125F, (1.0F - t) / 0.125F));
        w = w * w * (3.0F - 2.0F * w);
        float s = elapsed / 1000.0F;
        float beat = s * 9.0F;
        switch (emote) {
            case 0 -> {
                aim(model.rightArm, w, -3.0F, 0.0F, -0.25F + MathHelper.sin(s * 14.0F) * 0.5F);
            }
            case 1 -> {
                float cheer = MathHelper.sin(s * 16.0F) * 0.12F;
                aim(model.rightArm, w, -3.0F, 0.0F, -0.35F + cheer);
                aim(model.leftArm, w, -3.0F, 0.0F, 0.35F - cheer);
                aim(model.head, w, -0.25F, 0.0F, 0.0F);
            }
            case 2 -> {
                // A bow: the whole upper body folds forward about the hips, the legs stay where they are.
                hinge(model, 0.95F * w);
                aim(model.body, w, 0.95F, 0.0F, 0.0F);
                aim(model.head, w, 1.2F, 0.0F, 0.0F);
                aim(model.rightArm, w, 1.1F, 0.0F, 0.08F);
                aim(model.leftArm, w, 1.1F, 0.0F, -0.08F);
            }
            case 3 -> {
                aim(model.body, w, 0.0F, MathHelper.sin(beat * 0.5F) * 0.45F, 0.0F);
                aim(model.head, w, 0.0F, 0.0F, MathHelper.sin(beat * 0.5F + 1.0F) * 0.22F);
                aim(model.rightArm, w, -2.1F + MathHelper.sin(beat) * 0.55F, 0.0F, -0.3F);
                aim(model.leftArm, w, -2.1F + MathHelper.cos(beat) * 0.55F, 0.0F, 0.3F);
                aim(model.rightLeg, w, MathHelper.sin(beat) * 0.55F, 0.0F, 0.0F);
                aim(model.leftLeg, w, -MathHelper.sin(beat) * 0.55F, 0.0F, 0.0F);
            }
            case 4 -> {
                aim(model.rightArm, w, -1.15F, -0.55F, 0.0F);
                aim(model.head, w, 0.28F, MathHelper.sin(s * 1.6F) * 0.35F, 0.0F);
            }
            case 5 -> {
                // 6 7: both hands out in front, palms up, weighing one against the other.
                float swing = MathHelper.sin(s * 10.0F) * 0.6F;
                aim(model.rightArm, w, -1.2F + swing, -0.2F, 0.0F);
                aim(model.leftArm, w, -1.2F - swing, 0.2F, 0.0F);
                aim(model.head, w, 0.05F, 0.0F, MathHelper.sin(s * 5.0F) * 0.12F);
                aim(model.body, w, 0.0F, MathHelper.sin(s * 5.0F) * 0.1F, 0.0F);
            }
            case 6 -> {
                aim(model.rightArm, w, -2.55F, -0.5F, 0.15F);
                aim(model.head, w, -0.08F, 0.0F, 0.0F);
            }
            case 7 -> {
                aim(model.rightArm, w, 0.0F, 0.0F, 1.5708F);
                aim(model.leftArm, w, 0.0F, 0.0F, -1.5708F);
            }
            case 8 -> {
                aim(model.rightArm, w, -1.8F, -0.4F, 0.0F);
                aim(model.head, w, 0.5F, 0.0F, 0.0F);
                aim(model.body, w, 0.12F, 0.0F, 0.0F);
            }
            case 9 -> {
                // A robot moves in steps: every angle holds, then jumps to the next.
                float a = ((int)Math.floor(s * 3.0F)) % 2 == 0 ? 0.7F : -0.7F;
                aim(model.rightArm, w, -1.57F + a * 0.5F, 0.0F, 0.0F);
                aim(model.leftArm, w, -1.57F - a * 0.5F, 0.0F, 0.0F);
                aim(model.head, w, 0.0F, a * 0.7F, 0.0F);
                aim(model.body, w, 0.0F, a * 0.25F, 0.0F);
            }
            case 10 -> {
                float run = s * 15.0F;
                aim(model.rightLeg, w, MathHelper.sin(run), 0.0F, 0.0F);
                aim(model.leftLeg, w, -MathHelper.sin(run), 0.0F, 0.0F);
                aim(model.rightArm, w, -MathHelper.sin(run) - 0.5F, 0.0F, 0.0F);
                aim(model.leftArm, w, MathHelper.sin(run) - 0.5F, 0.0F, 0.0F);
                aim(model.body, w, 0.12F, 0.0F, 0.0F);
            }
            case 11 -> {
                aim(model.head, w, 0.1F + MathHelper.sin(s * 8.0F) * 0.4F, 0.0F, 0.0F);
            }
            case 12 -> {
                aim(model.head, w, 0.0F, MathHelper.sin(s * 10.0F) * 0.6F, 0.0F);
                aim(model.rightArm, w, -1.4F, -0.25F, 0.0F);
            }
            case 13 -> {
                float clap = Math.abs(MathHelper.sin(s * 11.0F));
                aim(model.rightArm, w, -1.4F, -0.1F - clap * 0.5F, 0.0F);
                aim(model.leftArm, w, -1.4F, 0.1F + clap * 0.5F, 0.0F);
            }
            case 14 -> {
                float jab = MathHelper.sin(s * 11.0F);
                aim(model.rightArm, w, -1.3F - Math.max(0.0F, jab) * 0.5F, -0.3F, 0.0F);
                aim(model.leftArm, w, -1.3F - Math.max(0.0F, -jab) * 0.5F, 0.3F, 0.0F);
                aim(model.body, w, 0.1F, jab * 0.25F, 0.0F);
                aim(model.head, w, 0.15F, -jab * 0.15F, 0.0F);
            }
            case 15 -> {
                float pump = MathHelper.sin(s * 6.0F) * 0.12F;
                aim(model.rightArm, w, -0.3F, 0.0F, 2.0F + pump);
                aim(model.leftArm, w, -0.3F, 0.0F, -2.0F - pump);
                aim(model.head, w, -0.1F, 0.0F, 0.0F);
            }
            case 16 -> {
                float pulse = MathHelper.sin(s * 7.0F) * 0.06F;
                aim(model.rightArm, w, -2.9F, 0.0F, 0.55F + pulse);
                aim(model.leftArm, w, -2.9F, 0.0F, -0.55F - pulse);
                aim(model.head, w, -0.15F, 0.0F, MathHelper.sin(s * 3.5F) * 0.12F);
            }
            case 17 -> {
                float shake = MathHelper.sin(s * 38.0F);
                aim(model.body, w, 0.05F, shake * 0.05F, 0.0F);
                aim(model.head, w, 0.1F, 0.0F, shake * 0.08F);
                aim(model.rightArm, w, -0.7F + shake * 0.1F, -0.5F, 0.0F);
                aim(model.leftArm, w, -0.7F - shake * 0.1F, 0.5F, 0.0F);
                aim(model.rightLeg, w, shake * 0.12F, 0.0F, 0.0F);
                aim(model.leftLeg, w, -shake * 0.12F, 0.0F, 0.0F);
            }
            case 18 -> {
                aim(model.rightArm, w, -0.5F, 0.0F, 0.7F);
                aim(model.leftArm, w, -0.5F, 0.0F, -0.7F);
                aim(model.head, w, -0.3F, 0.0F, 0.0F);
                aim(model.body, w, -0.12F, 0.0F, 0.0F);
            }
            case 19 -> {
                aim(model.rightArm, w, -1.57F, 0.0F, 0.0F);
                aim(model.head, w, 0.0F, -0.2F, 0.0F);
            }
            case 20 -> {
                aim(model.rightArm, w, -1.57F, 0.0F, MathHelper.sin(s * 9.0F) * 0.08F);
            }
            case 21 -> {
                aim(model.rightArm, w, -1.4F + MathHelper.sin(s * 9.0F) * 0.45F, -0.2F, 0.0F);
                aim(model.head, w, 0.1F, 0.0F, 0.0F);
            }
            case 22 -> {
                float lift = (MathHelper.sin(s * 6.0F) * 0.5F + 0.5F) * 0.25F;
                aim(model.rightArm, w, -0.35F, 0.0F, 0.35F + lift);
                aim(model.leftArm, w, -0.35F, 0.0F, -0.35F - lift);
                aim(model.head, w, 0.0F, 0.0F, MathHelper.sin(s * 3.0F) * 0.12F);
            }
            default -> {
                float flap = MathHelper.sin(s * 14.0F) * 0.45F;
                aim(model.rightArm, w, -3.0F, 0.0F, -0.25F + flap);
                aim(model.leftArm, w, -3.0F, 0.0F, 0.25F - flap);
            }
        }
    }

    /**
     * Folds the upper body forward by the angle about the hips: body, head and arms turn with it and their pivots move so that
     * the hips stay where they were (turning about the shoulders alone swings the hips back behind the legs).
     */
    private static void hinge(PlayerEntityModel model, float angle) {
        float c = MathHelper.cos(angle);
        float sn = MathHelper.sin(angle);
        float torsoY = 12.0F * (1.0F - c);
        float torsoZ = -12.0F * sn;
        model.body.originY += torsoY;
        model.body.originZ += torsoZ;
        model.head.originY += torsoY;
        model.head.originZ += torsoZ;
        float armY = 10.0F * (1.0F - c);
        float armZ = -10.0F * sn;
        model.rightArm.originY += armY;
        model.rightArm.originZ += armZ;
        model.leftArm.originY += armY;
        model.leftArm.originZ += armZ;
    }

    /** Blends one part from its current pose towards the target angles by the weight w. */
    private static void aim(ModelPart part, float w, float pitch, float yaw, float roll) {
        part.pitch = MathHelper.lerp(w, part.pitch, pitch);
        part.yaw = MathHelper.lerp(w, part.yaw, yaw);
        part.roll = MathHelper.lerp(w, part.roll, roll);
    }
}
