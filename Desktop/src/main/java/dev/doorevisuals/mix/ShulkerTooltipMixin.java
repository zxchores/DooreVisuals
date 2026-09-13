package dev.doorevisuals.mix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ShulkerTooltipMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void doore$shulker(TooltipContext context, PlayerEntity player, TooltipType flag, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack itemstack = (ItemStack)this;
        if (itemstack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof ShulkerBoxBlock) {
            ContainerComponent containercomponent = (ContainerComponent)itemstack.get(DataComponentTypes.CONTAINER);
            if (containercomponent != null) {
                List<Text> list = new ArrayList<>();
                int i = 0;

                try {
                    for (ItemStack itemstack1 : containercomponent.iterateNonEmpty()) {
                        if (itemstack1 != null && !itemstack1.isEmpty()) {
                            list.add(
                                Text.literal("  " + itemstack1.getName().getString() + " \u00d7" + itemstack1.getCount())
                                    .styled(s -> s.withColor(10528944))
                            );
                            if (++i >= 12) {
                                list.add(Text.literal("  \u2026").styled(s -> s.withColor(6976120)));
                                break;
                            }
                        }
                    }
                } catch (Throwable throwable) {
                    return;
                }

                if (!list.isEmpty()) {
                    List<Text> list1 = new ArrayList<>((Collection<? extends Text>)cir.getReturnValue());
                    list1.add(Text.literal("\u0421\u043e\u0434\u0435\u0440\u0436\u0438\u043c\u043e\u0435").styled(s -> s.withColor(8318932)));
                    list1.addAll(list);
                    cir.setReturnValue(list1);
                }
            }
        }
    }
}
