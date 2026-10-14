package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class ZoomFeature extends Feature implements Tick {
    private static volatile float override = Float.NaN;
    private static ZoomFeature live;
    private final Opt.Num fov = this.opt(new Opt.Num("fov", "FOV", 28.0, 10.0, 70.0, 1.0));
    private final Opt.Num speed = this.opt(new Opt.Num("speed", "\u041f\u043b\u0430\u0432\u043d\u043e\u0441\u0442\u044c", 0.35, 0.15, 0.8, 0.01));
    private final Opt.Key key = this.opt(new Opt.Key("key", "\u041a\u043b\u0430\u0432\u0438\u0448\u0430", 67));
    private boolean zooming;
    private float current;
    private float baseFov;
    private float targetFov;
    private long lastNanos;

    public ZoomFeature() {
        super(
            "zoom",
            "Zoom",
            "\u041f\u043b\u0430\u0432\u043d\u044b\u0439 \u0437\u0443\u043c \u043f\u043e \u0443\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u044e, \u043a\u043e\u043b\u0451\u0441\u0438\u043a\u043e \u043c\u0435\u043d\u044f\u0435\u0442 FOV",
            Category.TOOLS,
            true
        );
        live = this;
    }

    public static float fovOverride() {
        ZoomFeature zoomfeature = live;
        if (zoomfeature != null) {
            zoomfeature.advance();
        }

        return override;
    }

    public static boolean zooming() {
        return !Float.isNaN(override);
    }

    @Override
    protected void disable() {
        this.clear();
    }

    @Override
    public void tick(MinecraftClient mc) {
        live = this;
        if (mc.player != null && mc.currentScreen == null) {
            boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), (Integer)this.key.get()) == 1;
            float f = ((Integer)mc.options.getFov().getValue()).intValue();
            if (flag) {
                if (!this.zooming) {
                    this.baseFov = f;
                    this.current = f;
                    this.targetFov = this.fov.f();
                    this.zooming = true;
                    this.lastNanos = System.nanoTime();
                }
            } else if (this.zooming) {
                this.targetFov = this.baseFov;
            }
        } else {
            this.clear();
        }
    }

    private void advance() {
        if (!this.zooming && Float.isNaN(override)) {
            this.lastNanos = 0L;
        } else {
            long i = System.nanoTime();
            float f = this.lastNanos == 0L ? 0.016666668F : (float)(i - this.lastNanos) / 1.0E9F;
            this.lastNanos = i;
            f = MathHelper.clamp(f, 0.001F, 0.05F);
            float f1 = Math.max(0.12F, this.speed.f());
            float f2 = 1.0F - (float)Math.exp(-f / f1);
            this.current = this.current + (this.targetFov - this.current) * f2;
            if (!this.heldNow() && Math.abs(this.current - this.baseFov) < 0.12F) {
                this.clear();
            } else {
                override = this.current;
            }
        }
    }

    private boolean heldNow() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        return minecraftclient.player != null && minecraftclient.currentScreen == null
            ? GLFW.glfwGetKey(minecraftclient.getWindow().getHandle(), (Integer)this.key.get()) == 1
            : false;
    }

    private void clear() {
        this.zooming = false;
        override = Float.NaN;
        this.lastNanos = 0L;
    }

    public static boolean held() {
        return !Float.isNaN(override);
    }

    public static void wheel(double dy) {
        ZoomFeature zoomfeature = live;
        if (zoomfeature != null && zoomfeature.zooming) {
            zoomfeature.targetFov = (float)Math.max(zoomfeature.fov.min(), Math.min(zoomfeature.fov.max(), zoomfeature.targetFov - dy * 3.5));
        }
    }
}
