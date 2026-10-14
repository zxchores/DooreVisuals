package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.FeatureGuard;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.friends.FriendsFeature;
import dev.doorevisuals.models.ModelsFeature;
import dev.doorevisuals.world.sky.AtmosphereFeature;
import dev.doorevisuals.tools.BoreHelperFeature;
import dev.doorevisuals.tools.GpsFeature;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AfterEntities;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.BeforeBlockOutline;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.EndExtraction;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

public final class WorldPipe {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/World");
    private static boolean bound;

    private WorldPipe() {
    }

    public static void bind() {
        if (!bound) {
            bound = true;
            WorldRenderEvents.END_EXTRACTION.register((EndExtraction)ctx -> safe(() -> {
                float f = ctx.tickCounter().getTickProgress(false);
                run(ChinaHatFeature.class, "извлечение шляпы", fx -> fx.extract(f));
                run(FriendsFeature.class, "извлечение друзей", fx -> fx.extract(f));
            }));
            WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((BeforeBlockOutline)(ctx, outline) -> {
                return App.features().find(BlockHighlightFeature.class)
                    .map(f -> FeatureGuard.call(f, "обводка блока", () -> f.on() && f.handle(ctx, outline), true))
                    .orElse(true);
            });
            WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> safe(() -> {
                VisualQuality.beginFrame();
                float f = partial();
                run(AtmosphereFeature.class, "небо атмосферы", fx -> fx.drawSky(ctx, f));
            }));
            WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)ctx -> safe(() -> {
                float f = partial();
                run(TargetEspFeature.class, "Target ESP", fx -> fx.draw(ctx, f));
                run(WaterReflectionsFeature.class, "отражения воды", fx -> fx.draw(ctx));
                run(BloomFeature.class, "bloom", fx -> fx.draw(ctx));
                run(ShadowEnhancerFeature.class, "контактные тени", fx -> fx.draw(ctx));
                run(PredictionsFeature.class, "предсказания", fx -> fx.draw(ctx, f));
                run(JumpCirclesFeature.class, "круги прыжка", fx -> fx.draw(ctx));
                run(ChinaHatFeature.class, "шляпа", fx -> fx.draw(ctx));
                run(FriendsFeature.class, "рендер друзей", fx -> fx.draw(ctx));
                run(HitFxFeature.class, "эффекты удара", fx -> fx.draw(ctx));
                run(WorldParticlesFeature.class, "частицы мира", fx -> fx.draw(ctx));
                run(KillFxFeature.class, "эффекты убийства", fx -> fx.draw(ctx));
                run(GpsFeature.class, "GPS", fx -> fx.draw(ctx));
                run(MotionTrailFeature.class, "трейлы", fx -> fx.draw(ctx));
                run(AtmosphereFeature.class, "слои атмосферы", fx -> fx.drawFx(ctx, f));
                run(CosmeticsFeature.class, "косметика", fx -> fx.draw(ctx, f));
                run(BoreHelperFeature.class, "помощник руды", fx -> fx.draw(ctx));
                run(ModelsFeature.class, "модели", fx -> fx.draw(ctx, f));
            }));
        }
    }

    private static float partial() {
        try {
            return MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        } catch (Throwable throwable) {
            return 0.0F;
        }
    }

    private static void safe(Runnable r) {
        try {
            r.run();
        } catch (Throwable throwable) {
            LOG.error("World draw failed", throwable);
        }
    }

    private static <T extends Feature> void run(Class<T> type, String stage, Consumer<T> action) {
        App.features().find(type).ifPresent(feature -> FeatureGuard.run(feature, stage, action));
    }
}
