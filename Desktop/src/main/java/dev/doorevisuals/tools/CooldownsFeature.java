package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public final class CooldownsFeature extends Feature implements Tick {
    private final Opt.Flag hotbar = this.opt(new Opt.Flag("hotbar", "Секунды на хотбаре", true));
    private volatile List<CooldownsFeature.Cd> active = List.of();

    public CooldownsFeature() {
        super("cooldowns", "Cooldowns", "Секунды на иконках: ванильный КД и число из лора", Category.OVERLAY, true);
    }

    public static List<CooldownsFeature.Cd> active() {
        return App.features().find(CooldownsFeature.class).filter(Feature::on).map(f -> f.active).orElse(List.of());
    }

    public static boolean hotbarSeconds() {
        return App.features().find(CooldownsFeature.class).filter(Feature::on).map(f -> (Boolean)f.hotbar.get()).orElse(false);
    }

    public static float seconds(ItemStack stack, MinecraftClient mc) {
        if (stack == null || stack.isEmpty() || mc.player == null) {
            return 0.0F;
        } else {
            float f = 0.0F;

            try {
                float f1 = mc.player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
                if (f1 > 0.02F) {
                    f = Math.max(f, f1 * 20.0F);
                }
            } catch (Throwable throwable) {
            }

            Float float1 = AuctionLore.loreSeconds(stack);
            if (float1 != null) {
                f = Math.max(f, float1);
            }

            return f;
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null) {
            List<CooldownsFeature.Cd> list = new ArrayList<>();
            this.add(list, mc.player.getOffHandStack(), mc);

            for (int i = 0; i < 9; i++) {
                this.add(list, mc.player.getInventory().getStack(i), mc);
            }

            this.active = list;
        } else {
            this.active = List.of();
        }
    }

    private void add(List<CooldownsFeature.Cd> out, ItemStack stack, MinecraftClient mc) {
        float f = seconds(stack, mc);
        if (f > 0.15F) {
            float f1 = 0.0F;

            try {
                f1 = mc.player.getItemCooldownManager().getCooldownProgress(stack, 0.0F);
            } catch (Throwable throwable) {
            }

            out.add(new CooldownsFeature.Cd(stack.copy(), f, f1));
        }
    }

    public static void paintHotbar(DrawContext g) {
        if (hotbarSeconds()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.player != null && !minecraftclient.options.hudHidden) {
                int i = g.getScaledWindowWidth() / 2 - 90;
                int j = g.getScaledWindowHeight() - 19;

                for (int k = 0; k < 9; k++) {
                    float f = seconds(minecraftclient.player.getInventory().getStack(k), minecraftclient);
                    if (f > 0.15F) {
                        String s = f >= 10.0F ? String.valueOf(Math.round(f)) : String.format("%.1f", f);
                        g.drawText(minecraftclient.textRenderer, s, i + k * 20 + 2, j - 8, 0xFFFFFF, true);
                    }
                }

                float f1 = seconds(minecraftclient.player.getOffHandStack(), minecraftclient);
                if (f1 > 0.15F) {
                    g.drawText(
                        minecraftclient.textRenderer,
                        f1 >= 10.0F ? String.valueOf(Math.round(f1)) : String.format("%.1f", f1),
                        i - 28,
                        j - 8,
                        0xFFFFFF,
                        true
                    );
                }
            }
        }
    }

    public record Cd(ItemStack stack, float seconds, float progress) {
    }
}
