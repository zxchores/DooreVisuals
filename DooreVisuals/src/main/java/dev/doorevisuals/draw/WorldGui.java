package dev.doorevisuals.draw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class WorldGui {
    private WorldGui() {
    }

    public static float[] project(double x, double y, double z) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.gameRenderer == null) {
            return null;
        } else {
            Camera camera = minecraftclient.gameRenderer.getCamera();
            Vec3d vec3d = camera.getCameraPos();
            Vector3f vector3f = new Vector3f((float)(x - vec3d.x), (float)(y - vec3d.y), (float)(z - vec3d.z));
            new Quaternionf(camera.getRotation()).conjugate().transform(vector3f);
            if (vector3f.z > -0.08F) {
                return null;
            } else {
                float f = -vector3f.z;
                int i = minecraftclient.getWindow().getScaledWidth();
                int j = minecraftclient.getWindow().getScaledHeight();
                if (i > 0 && j > 0) {
                    float f1 = (float)i / j;
                    double d0 = 70.0;

                    try {
                        d0 = ((Integer)minecraftclient.options.getFov().getValue()).intValue();
                    } catch (Throwable throwable) {
                    }

                    float f2 = (float)Math.tan(Math.toRadians(d0) * 0.5);
                    float f3 = vector3f.x / (f * f2 * f1);
                    float f4 = vector3f.y / (f * f2);
                    float f5 = (f3 * 0.5F + 0.5F) * i;
                    float f6 = (0.5F - f4 * 0.5F) * j;
                    return !(f5 < -120.0F) && !(f6 < -80.0F) && !(f5 > i + 120) && !(f6 > j + 80) ? new float[]{f5, f6} : null;
                } else {
                    return null;
                }
            }
        }
    }
}
