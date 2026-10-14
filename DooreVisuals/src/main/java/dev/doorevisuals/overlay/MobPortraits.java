package dev.doorevisuals.overlay;

import dev.doorevisuals.draw.Sprites;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Static 2D portraits for living mobs. No entity/model rendering is used.
 * The mob is detected by its vanilla entity id and its vanilla texture is
 * cropped to a face-sized region.
 */
public final class MobPortraits {
    private static final Map<String, Face> FACES = new HashMap<>();

    static {
        // 64x64 humanoid textures: standard 8x8 head/front region.
        register64("zombie", "textures/entity/zombie/zombie.png");
        register64("zombie_villager", "textures/entity/zombie_villager/zombie_villager.png");
        register64("husk", "textures/entity/zombie/husk.png");
        register64("drowned", "textures/entity/zombie/drowned.png");
        register64("skeleton", "textures/entity/skeleton/skeleton.png");
        register64("stray", "textures/entity/skeleton/stray.png");
        register64("wither_skeleton", "textures/entity/skeleton/wither_skeleton.png");
        register64("bogged", "textures/entity/skeleton/bogged.png");
        register64("piglin", "textures/entity/piglin/piglin.png");
        register64("piglin_brute", "textures/entity/piglin/piglin_brute.png");
        register64("zombified_piglin", "textures/entity/piglin/zombified_piglin.png");
        register64("villager", "textures/entity/villager/villager.png");
        register64("wandering_trader", "textures/entity/wandering_trader.png");
        register64("witch", "textures/entity/witch.png");
        register64("evoker", "textures/entity/illager/evoker.png");
        register64("vindicator", "textures/entity/illager/vindicator.png");
        register64("pillager", "textures/entity/illager/pillager.png");
        register64("illusioner", "textures/entity/illager/illusioner.png");

        // 64x32 classic mob textures. Their head front is also 8x8 at 8,8.
        register32("creeper", "textures/entity/creeper/creeper.png");
        register32("enderman", "textures/entity/enderman/enderman.png");
        register32("cow", "textures/entity/cow/cow.png");
        register32("mooshroom", "textures/entity/cow/mooshroom.png");
        register32("pig", "textures/entity/pig/pig.png");
        register32("sheep", "textures/entity/sheep/sheep.png");
        register32("chicken", "textures/entity/chicken.png");
        register32("rabbit", "textures/entity/rabbit/brown.png");
        register32("goat", "textures/entity/goat.png");
        register32("iron_golem", "textures/entity/iron_golem/iron_golem.png");
    }

    private MobPortraits() {
    }

    private static void register64(String id, String path) {
        FACES.put(id, new Face(
            Identifier.of("minecraft", path),
            64, 64,
            8, 8, 8, 8
        ));
    }

    private static void register32(String id, String path) {
        FACES.put(id, new Face(
            Identifier.of("minecraft", path),
            64, 32,
            8, 8, 8, 8
        ));
    }

    public static void draw(DrawContext context, LivingEntity entity, int x, int y, int size, int argb) {
        String id = Registries.ENTITY_TYPE
            .getId(entity.getType())
            .getPath();

        Face face = FACES.get(id);
        if (face == null) {
            // No known face crop: use the mob's spawn egg as a static icon, the client icon only as a last resort.
            Identifier egg = Identifier.of("minecraft", id + "_spawn_egg");
            if ((argb >>> 24) > 200 && Registries.ITEM.containsId(egg)) {
                context.getMatrices().pushMatrix();
                try {
                    context.getMatrices().translate(x, y);
                    context.getMatrices().scale(size / 16.0F, size / 16.0F);
                    context.drawItem(new ItemStack(Registries.ITEM.get(egg)), 0, 0);
                } finally {
                    context.getMatrices().popMatrix();
                }
                return;
            }
            drawFallback(context, x, y, size);
            return;
        }

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            face.texture,
            x,
            y,
            face.u,
            face.v,
            size,
            size,
            face.regionWidth,
            face.regionHeight,
            face.textureWidth,
            face.textureHeight,
            argb
        );
    }

    private static void drawFallback(DrawContext context, int x, int y, int size) {
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            Sprites.ICON,
            x,
            y,
            0.0F,
            0.0F,
            size,
            size,
            size,
            size
        );
    }

    private record Face(
        Identifier texture,
        int textureWidth,
        int textureHeight,
        int u,
        int v,
        int regionWidth,
        int regionHeight
    ) {
    }
}
