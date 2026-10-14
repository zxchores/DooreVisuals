package dev.doorevisuals.mix;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setRotation")
    void doore$setRotation(float var1, float var2);

    @Invoker("setPos")
    void doore$setPosition(double var1, double var3, double var5);
}
