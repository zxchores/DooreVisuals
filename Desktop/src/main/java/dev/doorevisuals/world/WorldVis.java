package dev.doorevisuals.world;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class WorldVis {
    private WorldVis() {
    }

    public static boolean canSee(ClientPlayerEntity self, Entity target) {
        if (self == null || target == null || !target.isAlive()) {
            return false;
        } else if (target == self) {
            return true;
        } else if (self.canSee(target)) {
            return true;
        } else {
            Vec3d vec3d = self.getEyePos();
            Vec3d vec3d1 = target.getEntityPos().add(0.0, target.getHeight() * 0.6, 0.0);
            BlockHitResult blockhitresult = self.getEntityWorld()
                .raycast(new RaycastContext(vec3d, vec3d1, ShapeType.COLLIDER, FluidHandling.NONE, self));
            return blockhitresult.getType() == Type.MISS;
        }
    }

    public static boolean inFrustumRange(ClientPlayerEntity self, LivingEntity e, double rangeSq) {
        return self != null && e != null && e.squaredDistanceTo(self) <= rangeSq;
    }
}
