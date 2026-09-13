package dev.doorevisuals.models;

import java.util.List;
import net.minecraft.util.Identifier;

public final class ModelPack {
    public final String id;
    public final Identifier texture;
    public final float texW;
    public final float texH;
    public final List<ModelCube> cubes;
    public final boolean walk;
    public final boolean idle;

    public ModelPack(String id, Identifier texture, float texW, float texH, List<ModelCube> cubes, boolean idle, boolean walk) {
        this.id = id;
        this.texture = texture;
        this.texW = texW;
        this.texH = texH;
        this.cubes = cubes;
        this.idle = idle;
        this.walk = walk;
    }
}
