package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Theme;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public final class BoreHelperFeature extends Feature implements Tick {
    private final Opt.Num radius = this.opt(new Opt.Num("radius", "Радиус", 2.0, 1.0, 8.0, 0.5));
    private volatile Box zone;

    public BoreHelperFeature() {
        super("bore_helper", "Bore Helper", "Зона копания бульдозера. Сама не копает", Category.WORLD, false);
    }

    @Override
    public void tick(MinecraftClient mc) {
        this.zone = this.on() && this.holding(mc) ? this.box(mc) : null;
    }

    public void draw(WorldRenderContext ctx) {
        Box box = this.zone;
        if (box != null) {
            Mesh.box(ctx, box, Mesh.alpha(Theme.ACCENT, 0.12F), Theme.ACCENT);
            Mesh.cage(ctx, box, Mesh.alpha(Theme.ACCENT_HOT, 0.08F), Theme.ACCENT_HOT, 0.15F);
        }
    }

    private boolean holding(MinecraftClient mc) {
        if (mc.player == null) {
            return false;
        } else {
            return match(mc.player.getMainHandStack()) || match(mc.player.getOffHandStack());
        }
    }

    private static boolean match(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            boolean flag = stack.isOf(Items.DIAMOND_PICKAXE)
                || stack.isOf(Items.NETHERITE_PICKAXE)
                || stack.isOf(Items.IRON_PICKAXE)
                || stack.isOf(Items.GOLDEN_PICKAXE)
                || AuctionLore.nameHas(stack, "кирк", "pickaxe");
            String s = AuctionLore.blob(stack).toLowerCase(Locale.ROOT);
            return flag && (s.contains("бульдозер") || s.contains("bulldozer") || s.contains("bore"));
        } else {
            return false;
        }
    }

    private Box box(MinecraftClient mc) {
        Double double1 = AuctionLore.loreRadius(held(mc));
        double d0 = double1 != null ? double1 : (Double)this.radius.get();
        HitResult hitresult = mc.crosshairTarget;
        if (hitresult instanceof BlockHitResult blockhitresult && hitresult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockpos = blockhitresult.getBlockPos();
            return Box.of(blockpos.toCenterPos(), d0 * 2.0, d0 * 2.0, d0 * 2.0);
        } else if (mc.player != null) {
            return Box.of(mc.player.getLerpedPos(1.0F).add(mc.player.getRotationVec(1.0F).multiply(3.0)), d0 * 2.0, 2.0, d0 * 2.0);
        } else {
            return null;
        }
    }

    private static ItemStack held(MinecraftClient mc) {
        if (mc.player == null) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack = mc.player.getMainHandStack();
            return match(itemstack) ? itemstack : mc.player.getOffHandStack();
        }
    }
}
