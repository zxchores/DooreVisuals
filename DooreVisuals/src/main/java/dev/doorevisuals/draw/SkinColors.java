package dev.doorevisuals.draw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class SkinColors {
    private SkinColors() {
    }

    public static int ofVictim(LivingEntity e, Identifier skin) {
        int i = averageFace(skin);
        if (i != 0) {
            return warm(i);
        } else {
            int j = 0;
            if (e != null) {
                j = e.getUuid().hashCode();
                if (e instanceof PlayerEntity playerentity) {
                    j ^= playerentity.getGameProfile().name().hashCode() * 31;
                }
            } else if (skin != null) {
                j = skin.toString().hashCode();
            }

            float f = (j & 65535) / 65535.0F;
            return hsv(f * 0.15F + 0.02F, 0.35F + Math.abs(j >> 8) % 40 / 100.0F, 0.75F + Math.abs(j >> 16) % 25 / 100.0F);
        }
    }

    public static int averageFace(Identifier skin) {
        if (skin == null) {
            return 0;
        } else {
            try {
                if (MinecraftClient.getInstance().getTextureManager().getTexture(skin) instanceof NativeImageBackedTexture nativeimagebackedtexture) {
                    NativeImage nativeimage = nativeimagebackedtexture.getImage();
                    if (nativeimage != null && nativeimage.getWidth() >= 16 && nativeimage.getHeight() >= 16) {
                        return avg(nativeimage, 8, 8, 8, 8);
                    }
                }
            } catch (Throwable throwable) {
            }

            return 0;
        }
    }

    private static int warm(int argb) {
        int i = argb >> 16 & 0xFF;
        int j = argb >> 8 & 0xFF;
        int k = argb & 0xFF;
        i = MathHelper.clamp((int)(i * 0.85F + 40.0F), 40, 255);
        j = MathHelper.clamp((int)(j * 0.9F + 20.0F), 30, 255);
        k = MathHelper.clamp((int)(k * 0.95F + 10.0F), 20, 255);
        return 0xFF000000 | i << 16 | j << 8 | k;
    }

    private static int avg(NativeImage img, int x0, int y0, int w, int h) {
        long i = 0L;
        long j = 0L;
        long k = 0L;
        int l = 0;

        for (int i1 = y0; i1 < y0 + h; i1++) {
            for (int j1 = x0; j1 < x0 + w; j1++) {
                int k1 = img.getColorArgb(j1, i1);
                int l1 = k1 >>> 24 & 0xFF;
                if (l1 >= 20) {
                    i += k1 >> 16 & 0xFF;
                    j += k1 >> 8 & 0xFF;
                    k += k1 & 0xFF;
                    l++;
                }
            }
        }

        return l == 0 ? 0 : 0xFF000000 | (int)(i / l) << 16 | (int)(j / l) << 8 | (int)(k / l);
    }

    private static int hsv(float h, float s, float v) {
        h -= (float)Math.floor(h);
        float f = v * s;
        float f1 = f * (1.0F - Math.abs(h * 6.0F % 2.0F - 1.0F));
        float f2 = v - f;
        float f3 = 0.0F;
        float f4 = 0.0F;
        float f5 = 0.0F;
        int i = (int)(h * 6.0F);
        switch (i) {
            case 0:
                f3 = f;
                f4 = f1;
                break;
            case 1:
                f3 = f1;
                f4 = f;
                break;
            case 2:
                f4 = f;
                f5 = f1;
                break;
            case 3:
                f4 = f1;
                f5 = f;
                break;
            case 4:
                f3 = f1;
                f5 = f;
                break;
            default:
                f3 = f;
                f5 = f1;
        }

        return 0xFF000000
            | MathHelper.clamp((int)((f3 + f2) * 255.0F), 0, 255) << 16
            | MathHelper.clamp((int)((f4 + f2) * 255.0F), 0, 255) << 8
            | MathHelper.clamp((int)((f5 + f2) * 255.0F), 0, 255);
    }
}
