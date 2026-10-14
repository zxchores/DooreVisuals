package dev.doorevisuals.world;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.compat.IrisCompat;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Anim;
import dev.doorevisuals.draw.Theme;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

public final class BlockHighlightFeature extends Feature {
    private final Opt.Pick style = this.opt(new Opt.Pick("style", "Стиль", "Перелив", "Перелив", "Классика", "Неон", "Пульс", "Углы"));
    private final Opt.Flag useTheme = this.opt(new Opt.Flag("theme", "\u0426\u0432\u0435\u0442 \u0442\u0435\u043c\u044b", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442", -12533600).visibleWhen(() -> !(Boolean)this.useTheme.get()));
    private final Opt.Num alpha = this.opt(
        new Opt.Num("alpha", "\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", 0.55, 0.15, 1.0, 0.05)
    );
    private final Opt.Flag fill = this.opt(new Opt.Flag("fill", "\u0417\u0430\u043b\u0438\u0432\u043a\u0430", true));

    private final Opt.Num speed = this.opt(new Opt.Num("speed", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u043f\u0435\u0440\u0435\u043b\u0438\u0432\u0430", 1.0, 0.2, 2.5, 0.1));
    private final Opt.Num thickness = this.opt(new Opt.Num("thickness", "\u0422\u043e\u043b\u0449\u0438\u043d\u0430", 2.2, 1.0, 4.0, 0.2));

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
        BlockPos blockpos = outline.pos();
        VoxelShape voxelshape = outline.shape();
        if (voxelshape != null && !voxelshape.isEmpty()) {
            int i = this.useTheme.get() ? Theme.EFFECT : (Integer)this.color.get();
            float f = this.alpha.f();
            int j = Mesh.alpha(i, 0.95F * f);
            int k = this.fill.get() ? Mesh.alpha(i, 0.14F * f) : Mesh.alpha(i, 0.02F);
            double d0 = blockpos.getX();
            double d1 = blockpos.getY();
            double d2 = blockpos.getZ();
            boolean shimmer = "\u041f\u0435\u0440\u0435\u043b\u0438\u0432".equals(this.style.get()) && !IrisCompat.shaderPackInUse();
            float pulse = "\u041f\u0443\u043b\u044c\u0441".equals(this.style.get()) ? 0.65F + 0.35F * Anim.breathe(1.6F) : 1.0F;
            int fillColor = this.fill.get() ? Mesh.alpha(i, shimmer ? 0.18F * f : 0.14F * f) : 0;

            // Keep the volume fill, but take the outline from VoxelShape's merged edge set. Drawing
            // the boxes individually creates visible seams where stairs and multipart blocks join.
            if (this.fill.get()) {
                for (Box box : voxelshape.getBoundingBoxes()) {
                    Mesh.box(ctx, box.offset(d0, d1, d2).expand(0.001), fillColor, 0);
                }
            }
            voxelshape.forEachEdge((x1, y1, z1, x2, y2, z2) -> {
                double ax = d0 + x1, ay = d1 + y1, az = d2 + z1;
                double bx = d0 + x2, by = d1 + y2, bz = d2 + z2;
                int edge = Mesh.alpha(j, pulse);
                if (shimmer) {
                    Mesh.shimmerEdge(ctx, ax, ay, az, bx, by, bz, edge, this.thickness.f(), this.speed.f());
                } else {
                    Mesh.outline(ctx, ax, ay, az, bx, by, bz, edge, "\u041d\u0435\u043e\u043d".equals(this.style.get()) ? this.thickness.f() + 1.0F : this.thickness.f());
                }
            });
        }
    }
}
