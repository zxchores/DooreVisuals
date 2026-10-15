package dev.doorevisuals.tools;

import dev.doorevisuals.core.Badge;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.Paint;
import dev.doorevisuals.draw.Theme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Container Peek: the client remembers what was in each chest, barrel and shulker box you opened. Later, looking at one of
 * them without opening it, a small panel under the crosshair shows what was inside when you last saw it, and how long ago.
 * Nothing is read from the server: it is only what the screen showed you.
 */
public final class ContainerPeekFeature extends Feature implements Tick {
    private static final int LIMIT = 96;

    private static ContainerPeekFeature self;

    private final Opt.Flag age = this.opt(new Opt.Flag("show_age", "Показывать, как давно открывали", true));

    private final Map<BlockPos, Snap> cache = new HashMap<>();
    private World world;
    private BlockPos looked;
    private BlockPos open;
    /** Where the item icons of the panel go this frame; filled in paint(), used in paintItems(). */
    private final List<Object[]> icons = new ArrayList<>();

    private record Snap(List<ItemStack> stacks, long at) {
    }

    public ContainerPeekFeature() {
        super(
                "container_peek",
                "Container Peek",
                "Запоминает содержимое сундуков, бочек и шалкеров и показывает его, когда смотришь на них",
                Category.TOOLS,
                true
        );
        this.badge(Badge.NEW);
        self = this;
    }

    private static boolean container(Block block) {
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock;
    }

    private static boolean storageHandler(ScreenHandler handler) {
        return handler instanceof GenericContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (mc.world == null) {
            this.cache.clear();
            this.world = null;
            return;
        }
        if (this.world != mc.world) {
            this.cache.clear();
            this.world = mc.world;
        }
        if (mc.currentScreen == null) {
            this.open = null;
            HitResult hit = mc.crosshairTarget;
            this.looked = hit instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK ? b.getBlockPos() : null;
            return;
        }
        if (mc.currentScreen instanceof HandledScreen<?> screen && storageHandler(screen.getScreenHandler())) {
            // The block that was under the crosshair when the screen opened is the one being shown.
            if (this.open == null) {
                this.open = this.looked;
            }
            if (this.open != null && container(mc.world.getBlockState(this.open).getBlock())) {
                List<ItemStack> stacks = new ArrayList<>();
                for (Slot slot : screen.getScreenHandler().slots) {
                    if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
                        stacks.add(slot.getStack().copy());
                    }
                }
                this.cache.put(this.open, new Snap(stacks, System.currentTimeMillis()));
                if (this.cache.size() > LIMIT) {
                    Iterator<Map.Entry<BlockPos, Snap>> it = this.cache.entrySet().iterator();
                    Map.Entry<BlockPos, Snap> oldest = null;
                    while (it.hasNext()) {
                        Map.Entry<BlockPos, Snap> e = it.next();
                        if (oldest == null || e.getValue().at() < oldest.getValue().at()) {
                            oldest = e;
                        }
                    }
                    if (oldest != null) {
                        this.cache.remove(oldest.getKey());
                    }
                }
            }
        }
    }

    private Snap current(MinecraftClient mc) {
        if (!this.on() || mc.currentScreen != null || mc.world == null || this.looked == null) {
            return null;
        }
        if (!container(mc.world.getBlockState(this.looked).getBlock())) {
            return null;
        }
        return this.cache.get(this.looked);
    }

    /** The panel: called inside the NanoVG frame of the overlay. */
    public static void paint(DrawContext g) {
        ContainerPeekFeature f = self;
        if (f != null) {
            f.paintPanel(g);
        }
    }

    /** The item icons of the panel: called after the frame, with the game's own item renderer. */
    public static void paintItems(DrawContext g) {
        ContainerPeekFeature f = self;
        if (f != null) {
            f.drawIcons(g);
        }
    }

    private void paintPanel(DrawContext g) {
        this.icons.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        Snap snap = this.current(mc);
        if (snap == null || snap.stacks().isEmpty()) {
            return;
        }
        int total = snap.stacks().size();
        int rows = (total + 8) / 9;
        float w = 9 * 18.0F + 12.0F;
        float h = rows * 18.0F + 28.0F;
        float x = g.getScaledWindowWidth() / 2.0F - w / 2.0F;
        float y = g.getScaledWindowHeight() / 2.0F + 22.0F;
        Paint.hudPlate(g, x, y, w, h, Theme.HUD);
        long secs = Math.max(0L, (System.currentTimeMillis() - snap.at()) / 1000L);
        String when = !this.age.get() ? "" : secs < 90 ? secs + " с назад" : secs < 5400 ? (secs / 60) + " мин назад" : (secs / 3600) + " ч назад";
        Paint.text(g, "Содержимое", x + 7.0F, y + 6.0F, Theme.TEXT, 6.4F);
        if (!when.isEmpty()) {
            Paint.textR(g, when, x + w - 7.0F, y + 6.0F, Theme.MUTED, 5.6F);
        }
        int filled = 0;
        for (int i = 0; i < total; i++) {
            float sx = x + 6.0F + (i % 9) * 18.0F;
            float sy = y + 20.0F + (i / 9) * 18.0F;
            Paint.box(g, sx, sy, 17.0F, 17.0F, Theme.alpha(0x000000, 90), 3.0F);
            ItemStack stack = snap.stacks().get(i);
            if (!stack.isEmpty()) {
                filled++;
                this.icons.add(new Object[] {stack, (int)sx + 1, (int)sy + 1});
            }
        }
        if (filled == 0) {
            Paint.textC(g, "пусто", x + w / 2.0F, y + h - 12.0F, Theme.MUTED, 5.6F);
        }
    }

    private void drawIcons(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Object[] icon : this.icons) {
            ItemStack stack = (ItemStack)icon[0];
            int x = (Integer)icon[1];
            int y = (Integer)icon[2];
            g.drawItem(stack, x, y);
            g.drawStackOverlay(mc.textRenderer, stack, x, y);
        }
    }
}
