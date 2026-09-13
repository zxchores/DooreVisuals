package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public final class BlockHighlightFeature extends Feature {
    private final Opt.Flag useTheme = this.opt(new Opt.Flag("theme", "\u0426\u0432\u0435\u0442 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.useTheme.get()));
    private final Opt.Num alpha = this.opt(
        new Opt.Num("alpha", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 0.55, 0.15, 1.0, 0.05)
    );
    private final Opt.Flag fill = this.opt(new Opt.Flag("fill", "\u0417\u0430\u043b\u0438\u0432\u043a\u0430", true));

    public BlockHighlightFeature() {
        super(
            "block_highlight",
            "Block Highlight",
            "Outline \u0431\u043b\u043e\u043a\u0430 \u043f\u043e\u0434 \u043f\u0440\u0438\u0446\u0435\u043b\u043e\u043c",
            Category.WORLD,
            true
        );
    }

    public boolean handle(WorldRenderContext ctx, OutlineRenderState outline) {
        if (this.on() && outline != null) {
            this.draw(ctx, outline);
            return false;
        } else {
            return true;
        }
    }

    private void draw(WorldRenderContext ctx, OutlineRenderState outline) {
        BlockPos blockpos = outline.comp_4932();
        VoxelShape voxelshape = outline.comp_4935();
        if (voxelshape != null && !voxelshape.isEmpty()) {
            int i = this.useTheme.get() ? Theme.EFFECT : (Integer)this.color.get();
            float f = this.alpha.f();
            int j = Mesh.alpha(i, 0.95F * f);
            int k = this.fill.get() ? Mesh.alpha(i, 0.14F * f) : Mesh.alpha(i, 0.02F);
            double d0 = blockpos.getX();
            double d1 = blockpos.getY();
            double d2 = blockpos.getZ();

            for (Box box : voxelshape.getBoundingBoxes()) {
                Box box1 = box.offset(d0, d1, d2);
                Mesh.box(ctx, box1, k, j);
                double d3 = (box1.minX + box1.maxX) * 0.5;
                double d4 = (box1.minY + box1.maxY) * 0.5;
                double d5 = (box1.minZ + box1.maxZ) * 0.5;
                float f1 = (float)(Math.max(box1.maxX - box1.minX, box1.maxZ - box1.minZ) * 0.28);
                Mesh.orb(ctx, d3, d4, d5, Math.max(0.08F, f1), Mesh.alpha(i, 0.22F * f));
            }
        }
    }
}
