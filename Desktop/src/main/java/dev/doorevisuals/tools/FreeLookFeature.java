package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class FreeLookFeature extends Feature implements Tick {
    private static volatile boolean looking;
    private static volatile float yaw;
    private static volatile float pitch;
    private final Opt.Key key = this.opt(new Opt.Key("key", "\u041a\u043b\u0430\u0432\u0438\u0448\u0430", 342));
    private final Opt.Flag thirdPerson = this.opt(new Opt.Flag("third", "3-\u0435 \u043b\u0438\u0446\u043e", true));
    private boolean held;
    private Perspective savedPerspective = Perspective.FIRST_PERSON;

    public FreeLookFeature() {
        super(
            "free_look",
            "Free Look",
            "\u041a\u0430\u043c\u0435\u0440\u0430 \u043e\u0442\u0434\u0435\u043b\u044c\u043d\u043e \u043e\u0442 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u044f \u2014 \u0443\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0439 \u043a\u043b\u0430\u0432\u0438\u0448\u0443",
            Category.TOOLS,
            true
        );
    }

    public static boolean active() {
        return looking;
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    public static void turn(double dx, double dy) {
        if (looking) {
            yaw += (float)dx * 0.15F;
            pitch = MathHelper.clamp(pitch + (float)dy * 0.15F, -90.0F, 90.0F);
        }
    }

    @Override
    protected void disable() {
        this.release(MinecraftClient.getInstance());
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.player != null && mc.currentScreen == null && this.on()) {
            int i = (Integer)this.key.get();
            boolean flag = i > 0 && GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
            if (flag && !this.held) {
                yaw = mc.player.getYaw();
                pitch = mc.player.getPitch();
                looking = true;
                this.held = true;
                if ((Boolean)this.thirdPerson.get()) {
                    this.savedPerspective = mc.options.getPerspective();
                    if (this.savedPerspective == Perspective.FIRST_PERSON) {
                        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                    }
                }
            } else if (!flag && this.held) {
                this.release(mc);
            }
        } else {
            this.release(mc);
        }
    }

    private void release(MinecraftClient mc) {
        if (this.held && mc != null && (Boolean)this.thirdPerson.get() && mc.options != null) {
            try {
                mc.options.setPerspective(this.savedPerspective);
            } catch (Throwable throwable) {
            }
        }

        this.held = false;
        looking = false;
    }
}
