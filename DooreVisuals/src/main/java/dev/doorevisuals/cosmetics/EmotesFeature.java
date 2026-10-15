package dev.doorevisuals.cosmetics;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Emotes and gestures: a key per emote (wave, cheer, bow, dance, think). While one plays, the body of your own character is
 * posed by it; it is visible from the third person (the camera switches there for the emote if you let it, and returns).
 */
public final class EmotesFeature extends Feature implements Tick {
    private static final String[] NAMES = {"Помахать", "Победа", "Поклон", "Танец", "Задуматься"};
    private static final long[] DURATION_MS = {2800L, 2400L, 2000L, 4200L, 3000L};

    private static EmotesFeature self;

    private final Opt.Key[] keys = new Opt.Key[NAMES.length];
    private final Opt.Flag thirdPerson = this.opt(new Opt.Flag("third_person", "Показывать от третьего лица", true));
    private final boolean[] down = new boolean[NAMES.length];

    private int playing = -1;
    private long startedAt;
    private Perspective restore;

    public EmotesFeature() {
        super(
                "emotes",
                "Emotes",
                "Эмоции и жесты по клавише: помахать, победа, поклон, танец, задуматься",
                Category.COSMETICS,
                true
        );
        for (int i = 0; i < NAMES.length; i++) {
            this.keys[i] = this.opt(new Opt.Key("emote_" + i, "Клавиша: " + NAMES[i], -1));
        }
        this.badge(Badge.NEW);
        self = this;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player == null) {
            this.stop(mc);
            return;
        }
        if (this.playing >= 0 && System.currentTimeMillis() - this.startedAt > DURATION_MS[this.playing]) {
            this.stop(mc);
        }
        for (int i = 0; i < NAMES.length; i++) {
            int key = this.keys[i].get();
            boolean pressed = key > 0 && mc.currentScreen == null && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
            if (pressed && !this.down[i]) {
                if (this.playing == i) {
                    this.stop(mc);
                } else {
                    this.start(mc, i);
                }
            }
            this.down[i] = pressed;
        }
    }

    private void start(MinecraftClient mc, int index) {
        this.playing = index;
        this.startedAt = System.currentTimeMillis();
        if (this.thirdPerson.get() && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            this.restore = Perspective.FIRST_PERSON;
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    private void stop(MinecraftClient mc) {
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
        if (f == null || !f.on() || f.playing < 0 || mc.player == null || state.id != mc.player.getId()) {
            return;
        }
        long elapsed = System.currentTimeMillis() - f.startedAt;
        float t = MathHelper.clamp(elapsed / (float)DURATION_MS[f.playing], 0.0F, 1.0F);
        // Fades in over the first eighth and out over the last eighth.
        float w = Math.min(1.0F, Math.min(t / 0.125F, (1.0F - t) / 0.125F));
        w = w * w * (3.0F - 2.0F * w);
        float s = elapsed / 1000.0F;
        switch (f.playing) {
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
                aim(model.body, w, 0.95F, 0.0F, 0.0F);
                aim(model.head, w, 0.35F, 0.0F, 0.0F);
                aim(model.rightArm, w, 0.55F, 0.0F, -0.1F);
                aim(model.leftArm, w, 0.55F, 0.0F, 0.1F);
            }
            case 3 -> {
                float beat = s * 9.0F;
                aim(model.body, w, 0.0F, MathHelper.sin(beat * 0.5F) * 0.45F, 0.0F);
                aim(model.head, w, 0.0F, 0.0F, MathHelper.sin(beat * 0.5F + 1.0F) * 0.22F);
                aim(model.rightArm, w, -2.1F + MathHelper.sin(beat) * 0.55F, 0.0F, -0.3F);
                aim(model.leftArm, w, -2.1F + MathHelper.cos(beat) * 0.55F, 0.0F, 0.3F);
                aim(model.rightLeg, w, MathHelper.sin(beat) * 0.55F, 0.0F, 0.0F);
                aim(model.leftLeg, w, -MathHelper.sin(beat) * 0.55F, 0.0F, 0.0F);
            }
            default -> {
                aim(model.rightArm, w, -1.15F, -0.55F, 0.0F);
                aim(model.head, w, 0.28F, MathHelper.sin(s * 1.6F) * 0.35F, 0.0F);
            }
        }
    }

    /** Blends one part from its current pose towards the target angles by the weight w. */
    private static void aim(ModelPart part, float w, float pitch, float yaw, float roll) {
        part.pitch = MathHelper.lerp(w, part.pitch, pitch);
        part.yaw = MathHelper.lerp(w, part.yaw, yaw);
        part.roll = MathHelper.lerp(w, part.roll, roll);
    }
}
