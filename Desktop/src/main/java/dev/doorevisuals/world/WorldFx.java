package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import net.minecraft.entity.LivingEntity;

public final class WorldFx {
    private WorldFx() {
    }

    public static boolean espOn() {
        return App.features().find(TargetEspFeature.class).filter(Feature::on).isPresent();
    }

    public static LivingEntity espTarget() {
        return App.features().find(TargetEspFeature.class).filter(Feature::on).map(TargetEspFeature::current).orElse(null);
    }

    public static boolean espOwns(LivingEntity entity) {
        return entity != null && entity == espTarget();
    }
}
