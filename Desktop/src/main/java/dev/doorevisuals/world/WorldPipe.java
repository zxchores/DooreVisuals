package dev.doorevisuals.world;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.draw.VisualQuality;
import dev.doorevisuals.friends.FriendsFeature;
import dev.doorevisuals.models.ModelsFeature;
import dev.doorevisuals.server.FunHelperFeature;
import dev.doorevisuals.tools.BoreHelperFeature;
import dev.doorevisuals.tools.GpsFeature;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AfterEntities;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.BeforeBlockOutline;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.EndExtraction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.state.OutlineRenderState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                App.features().find(ChinaHatFeature.class).filter(Feature::on).ifPresent(fx -> fx.extract(f));
                App.features().find(FriendsFeature.class).filter(Feature::on).ifPresent(fx -> fx.extract(f));
            }));
            WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((BeforeBlockOutline)(ctx, outline) -> {
                try {
                    return App.features().find(BlockHighlightFeature.class).map(f -> f.handle(ctx, outline)).orElse(true);
                } catch (Throwable throwable) {
                    LOG.error("Block highlight failed", throwable);
                    return true;
                }
            });
            WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> safe(() -> {
                VisualQuality.beginFrame();
                float f = partial();
                App.features().find(AtmosphereFeature.class).filter(Feature::on).ifPresent(fx -> fx.drawSky(ctx, f));
            }));
            WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)ctx -> safe(() -> {
                float f = partial();
                App.features().find(TargetEspFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx, f));
                App.features().find(PredictionsFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx, f));
                App.features().find(JumpCirclesFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(ChinaHatFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(FriendsFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(HitFxFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(WorldParticlesFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(KillFxFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(GpsFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(MotionTrailFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(AtmosphereFeature.class).filter(Feature::on).ifPresent(fx -> fx.drawFx(ctx, f));
                App.features().find(CosmeticsFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx, f));
                App.features().find(FunHelperFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(BoreHelperFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx));
                App.features().find(ModelsFeature.class).filter(Feature::on).ifPresent(fx -> fx.draw(ctx, f));
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
}
