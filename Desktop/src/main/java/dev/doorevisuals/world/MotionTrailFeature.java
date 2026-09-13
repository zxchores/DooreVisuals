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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class MotionTrailFeature extends Feature implements Tick {
    private final Opt.Flag sprint = this.opt(new Opt.Flag("sprint", "\u0421\u043f\u0440\u0438\u043d\u0442", true));
    private final Opt.Flag elytra = this.opt(new Opt.Flag("elytra", "\u042d\u043b\u0438\u0442\u0440\u044b", true));
    private final Opt.Flag hit = this.opt(new Opt.Flag("hit", "\u041f\u043e\u0441\u043b\u0435 \u0443\u0434\u0430\u0440\u0430", true));
    private final Opt.Pick plane = this.opt(
        new Opt.Pick(
            "plane",
            "\u041f\u043b\u043e\u0441\u043a\u043e\u0441\u0442\u044c",
            "\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u044c\u043d\u044b\u0439",
            "\u0412\u0435\u0440\u0442\u0438\u043a\u0430\u043b\u044c\u043d\u044b\u0439",
            "\u0413\u043e\u0440\u0438\u0437\u043e\u043d\u0442\u0430\u043b\u044c\u043d\u044b\u0439"
        )
    );
    private final Opt.Num length = this.opt(new Opt.Num("length", "\u0414\u043b\u0438\u043d\u0430", 18.0, 8.0, 40.0, 1.0));
    private final Opt.Num size = this.opt(new Opt.Num("size", "\u0428\u0438\u0440\u0438\u043d\u0430", 0.22, 0.08, 0.45, 0.01));
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "\u0426\u0432\u0435\u0442 \u0438\u0437 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.themeColor.get()));
    private final Deque<Vec3d> points = new ArrayDeque<>();
    private long hitUntil;

    public MotionTrailFeature() {
        super(
            "motion_trail",
            "Trails",
            "\u041b\u0435\u043d\u0442\u0430 \u043f\u0440\u0438 \u0441\u043f\u0440\u0438\u043d\u0442\u0435, \u044d\u043b\u0438\u0442\u0440\u0430\u0445 \u0438 \u043f\u043e\u0441\u043b\u0435 \u0443\u0434\u0430\u0440\u0430",
            Category.WORLD,
            false
        );
        AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hitRes) -> {
            if (level.isClient() && this.on() && (Boolean)this.hit.get()) {
                this.hitUntil = System.currentTimeMillis() + 650L;
            }

            return ActionResult.PASS;
        });
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null && mc.world != null) {
            ClientPlayerEntity clientplayerentity = mc.player;
            boolean flag = (Boolean)this.sprint.get() && clientplayerentity.isSprinting() && !clientplayerentity.isGliding()
                || (Boolean)this.elytra.get() && clientplayerentity.isGliding()
                || (Boolean)this.hit.get() && System.currentTimeMillis() < this.hitUntil;
            if (flag && !(clientplayerentity.getVelocity().lengthSquared() < 0.002)) {
                Vec3d vec3d = clientplayerentity.getEntityPos();
                if (this.points.isEmpty() || this.points.peekLast().squaredDistanceTo(vec3d) > 0.0025) {
                    this.points.addLast(vec3d);
                }

                while (this.points.size() > this.length.i()) {
                    this.points.pollFirst();
                }
            } else {
                if (this.points.size() > 1) {
                    this.points.pollFirst();
                }
            }
        } else {
            this.points.clear();
        }
    }

    public void draw(WorldRenderContext ctx) {
        if (this.on() && this.points.size() >= 2) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (!WorldFx.espOwns(minecraftclient.player)) {
                int i = this.themeColor.get() ? ThemeFeature.effectTint() : (Integer)this.color.get();
                List<Vec3d> list = new ArrayList<>(this.points);
                if (minecraftclient.player != null) {
                    float f = minecraftclient.getRenderTickCounter().getTickProgress(false);
                    Vec3d vec3d = minecraftclient.player.getLerpedPos(f);
                    if (!list.isEmpty() && !(list.get(list.size() - 1).squaredDistanceTo(vec3d) > 1.0E-6)) {
                        list.set(list.size() - 1, vec3d);
                    } else {
                        list.add(vec3d);
                    }
                }

                boolean flag = minecraftclient.options.getPerspective() == Perspective.FIRST_PERSON;
                float f5 = flag ? 0.72F : 0.88F;
                int j = list.size();
                float f1 = this.size.f();

                for (int k = 0; k < j; k++) {
                    float f2 = j <= 1 ? 1.0F : (float)k / (j - 1);
                    Vec3d vec3d1 = list.get(k);
                    float f3 = f1 * (0.22F + 0.78F * f2 * f2);
                    float f4 = 0.2F + 0.8F * f2;
                    Mesh.orb(ctx, vec3d1.x, vec3d1.y + f5, vec3d1.z, f3 * 1.45F, Mesh.alpha(i, f4 * 0.32F));
                    Mesh.orb(ctx, vec3d1.x, vec3d1.y + f5, vec3d1.z, f3, Mesh.alpha(i, f4 * 0.9F));
                }
            }
        }
    }
}
