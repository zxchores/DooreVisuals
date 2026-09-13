package dev.doorevisuals.data;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public final class Aim {
    private LivingEntity target;
    private long seen;

    public void bind() {
        AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
            if (level.isClient() && entity instanceof LivingEntity livingentity && livingentity != player) {
                this.lock(livingentity);
            }

            return ActionResult.PASS;
        });
    }

    public void pulse(MinecraftClient mc) {
        if (mc.player != null && mc.world != null) {
            if (mc.crosshairTarget instanceof EntityHitResult entityhitresult
                && entityhitresult.getEntity() instanceof LivingEntity livingentity
                && livingentity != mc.player
                && livingentity.isAlive()) {
                this.lock(livingentity);
            }

            if (this.target != null) {
                boolean flag = !this.target.isAlive()
                    || this.target.isRemoved()
                    || this.target.getEntityWorld() != mc.world
                    || mc.player.squaredDistanceTo(this.target) > 4096.0
                    || System.currentTimeMillis() - this.seen > 8000L;
                if (flag) {
                    this.target = null;
                }
            }
        } else {
            this.target = null;
        }
    }

    public LivingEntity get() {
        return this.target;
    }

    public boolean hot() {
        if (this.target != null && this.target.isAlive()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.player == null) {
                return false;
            } else {
                return minecraftclient.player.canSee(this.target) ? true : System.currentTimeMillis() - this.seen < 2800L;
            }
        } else {
            return false;
        }
    }

    private void lock(LivingEntity living) {
        this.target = living;
        this.seen = System.currentTimeMillis();
    }
}
