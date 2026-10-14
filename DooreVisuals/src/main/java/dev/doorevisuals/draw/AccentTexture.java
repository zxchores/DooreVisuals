package dev.doorevisuals.draw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Акцент темы в виде текстуры 1×1. Пайплайны умеют передавать шейдеру только константы, а цвет темы
 * меняется на ходу, поэтому он приезжает сэмплером: слой руки подвязывает эту текстуру как Sampler1,
 * а когда акцент меняется, пиксель просто перезаливается — пересобирать пайплайны не нужно.
 */
public final class AccentTexture {
    public static final Identifier ID = Identifier.of("doorevisuals", "dynamic/accent");

    private static NativeImageBackedTexture texture;
    private static int current = -1;

    private AccentTexture() {
    }

    /** Обновляет цвет акцента; повторные вызовы с тем же цветом ничего не делают. */
    public static void update(int argb) {
        int rgb = argb & 0xFFFFFF;
        if (rgb == current) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getTextureManager() == null) {
            return;
        }

        current = rgb;

        try {
            if (texture == null) {
                NativeImage image = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
                image.setColor(0, 0, 0xFF000000 | rgb);
                texture = new NativeImageBackedTexture(() -> "doorevisuals accent", image);
                mc.getTextureManager().registerTexture(ID, texture);
            } else {
                texture.getImage().setColor(0, 0, 0xFF000000 | rgb);
                texture.upload();
            }
        } catch (Throwable ignored) {
            // Если текстура не завелась, шейдер останется на своей палитре — это не повод падать.
            texture = null;
        }
    }
}
