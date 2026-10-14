package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.tools.ThemeFeature;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MotionTrailFeature extends Feature implements Tick {

    private static final long HIT_DURATION_MS = 650L;
    private static final double TELEPORT_DIST_SQ = 64.0;
    private static final double MIN_STEP_SQ = 0.0625;
    private static final double MIN_SPEED_SQ = 0.002;
    private static final float LIFT = 0.88F;
    private static final int SUBDIVISION = 4;
    private static final String VERTICAL = "\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u044c\u043d\u044b\u0439";

    // Triggers
    private final Opt.Flag sprint = this.opt(new Opt.Flag("sprint", "\u0421\u043f\u0440\u0438\u043d\u0442", true));
    private final Opt.Flag elytra = this.opt(new Opt.Flag("elytra", "\u042d\u043b\u0438\u0442\u0440\u044b", true));
    private final Opt.Flag swim = this.opt(new Opt.Flag("swim", "\u041f\u043b\u0430\u0432\u0430\u043d\u0438\u0435", false));
    private final Opt.Flag hit = this.opt(new Opt.Flag("hit", "\u041f\u043e\u0441\u043b\u0435 \u0443\u0434\u0430\u0440\u0430", true));

    // Shape
    private final Opt.Pick plane = this.opt(new Opt.Pick("plane", "\u041f\u043b\u043e\u0441\u043a\u043e\u0441\u0442\u044c", VERTICAL, VERTICAL, "\u0413\u043e\u0440\u0438\u0437\u043e\u043d\u0442\u0430\u043b\u044c\u043d\u044b\u0439"));
    private final Opt.Num length = this.opt(new Opt.Num("length", "\u0414\u043b\u0438\u043d\u0430", 48.0, 8.0, 80.0, 1.0));
    private final Opt.Num size = this.opt(new Opt.Num("size", "\u0428\u0438\u0440\u0438\u043d\u0430", 0.22, 0.06, 0.5, 0.01));

    // Color
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean) this.themeColor.get()));

    private final Deque<Vec3d> points = new ArrayDeque<>();
    private final List<Vec3d> ctrl = new ArrayList<>();
    private final List<Vec3d> curve = new ArrayList<>();
    private long hitUntil;
    private World lastWorld;

    public MotionTrailFeature() {
        super(
                "motion_trail",
                "Trails",
                "\u041b\u0435\u043d\u0442\u0430 \u043f\u0440\u0438 \u0441\u043f\u0440\u0438\u043d\u0442\u0435, \u044d\u043b\u0438\u0442\u0440\u0430\u0445, \u043f\u043b\u0430\u0432\u0430\u043d\u0438\u0438 \u0438 \u043f\u043e\u0441\u043b\u0435 \u0443\u0434\u0430\u0440\u0430",
                Category.WORLD,
                false
        );
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitRes) -> {
            if (level.isClient() && this.on() && (Boolean) this.hit.get()) {
                this.hitUntil = System.currentTimeMillis() + HIT_DURATION_MS;
            }
            return ActionResult.PASS;
        });
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (!this.on() || mc.player == null || mc.world == null) {
            this.points.clear();
            this.lastWorld = null;
            return;
        }
        if (this.lastWorld != mc.world) {
            this.points.clear();
            this.lastWorld = mc.world;
        }
        ClientPlayerEntity player = mc.player;
        Vec3d pos = player.getEntityPos();
        long now = System.currentTimeMillis();
        if (!this.points.isEmpty() && this.points.peekLast().squaredDistanceTo(pos) > TELEPORT_DIST_SQ) {
            this.points.clear();
        }
        boolean active = (Boolean) this.sprint.get() && player.isSprinting() && !player.isGliding()
                || (Boolean) this.elytra.get() && player.isGliding()
                || (Boolean) this.swim.get() && player.isSwimming()
                || (Boolean) this.hit.get() && now < this.hitUntil;
        if (active && player.getVelocity().lengthSquared() > MIN_SPEED_SQ) {
            if (this.points.isEmpty() || this.points.peekLast().squaredDistanceTo(pos) >= MIN_STEP_SQ) {
                this.points.addLast(pos);
            }
        } else if (!this.points.isEmpty()) {
            this.points.pollFirst();
        }
        while (this.points.size() > Math.max(this.length.i(), 2)) {
            this.points.pollFirst();
        }
    }

    public void draw(WorldRenderContext ctx) {
        if (!this.on() || this.points.size() < 2) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || WorldFx.espOwns(mc.player)) {
            return;
        }
        // Invisible in first person
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return;
        }

        this.buildControl(mc);
        this.buildCurve();
        int count = this.curve.size();
        if (count < 2) {
            return;
        }

        int argb = (Boolean) this.themeColor.get() ? ThemeFeature.effectTint() : (Integer) this.color.get();
        float w = this.size.f();
        boolean vertical = VERTICAL.equals(this.plane.get());

        // Лента собирается из трёх слоёв: широкая мягкая подложка, основная лента и яркая голова.
        // Вместе они дают сужение и затухание к хвосту, которого у одной ленты не бывает.
        Mesh.ribbon(ctx, this.curve, vertical, w * 2.2F, Mesh.alpha(argb, 0.16F));
        Mesh.ribbon(ctx, this.curve, vertical, w, Mesh.alpha(argb, 0.52F));

        int headFrom = Math.max(0, count - Math.max(2, count / 3));
        Mesh.ribbon(ctx, this.curve.subList(headFrom, count), vertical, w * 1.35F, argb);
    }

    private void buildControl(MinecraftClient mc) {
        this.ctrl.clear();
        for (Vec3d p : this.points) {
            this.ctrl.add(p);
        }
        Vec3d head = mc.player.getLerpedPos(mc.getRenderTickCounter().getTickProgress(false));
        if (this.ctrl.isEmpty()) {
            this.ctrl.add(head);
        } else if (this.ctrl.get(this.ctrl.size() - 1).squaredDistanceTo(head) > 1.0E-6) {
            this.ctrl.add(head);
        } else {
            this.ctrl.set(this.ctrl.size() - 1, head);
        }
        for (int i = 0; i < this.ctrl.size(); i++) {
            this.ctrl.set(i, this.ctrl.get(i).add(0.0, LIFT, 0.0));
        }
    }

    private void buildCurve() {
        this.curve.clear();
        int n = this.ctrl.size();
        if (n < 3) {
            this.curve.addAll(this.ctrl);
            return;
        }
        this.curve.add(this.ctrl.get(0));
        for (int i = 0; i < n - 1; i++) {
            Vec3d p0 = this.ctrl.get(Math.max(i - 1, 0));
            Vec3d p1 = this.ctrl.get(i);
            Vec3d p2 = this.ctrl.get(i + 1);
            Vec3d p3 = this.ctrl.get(Math.min(i + 2, n - 1));
            for (int s = 1; s <= SUBDIVISION; s++) {
                this.curve.add(catmullRom(p0, p1, p2, p3, (double) s / SUBDIVISION));
            }
        }
    }

    private static Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * (2.0 * p1.x + (p2.x - p0.x) * t
                + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2
                + (3.0 * p1.x - p0.x - 3.0 * p2.x + p3.x) * t3);
        double y = 0.5 * (2.0 * p1.y + (p2.y - p0.y) * t
                + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2
                + (3.0 * p1.y - p0.y - 3.0 * p2.y + p3.y) * t3);
        double z = 0.5 * (2.0 * p1.z + (p2.z - p0.z) * t
                + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2
                + (3.0 * p1.z - p0.z - 3.0 * p2.z + p3.z) * t3);
        return new Vec3d(x, y, z);
    }
}
