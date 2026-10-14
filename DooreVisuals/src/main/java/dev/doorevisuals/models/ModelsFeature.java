package dev.doorevisuals.models;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.draw.Mesh;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.math.MathHelper;

public final class ModelsFeature extends Feature implements Tick {
    private final Opt.Flag hideVanilla = this.opt(new Opt.Flag("hide_vanilla", "Скрыть ваниль", true));
    private final Opt.Num scale = this.opt(new Opt.Num("scale", "Масштаб", 1.0, 0.4, 2.5, 0.05));
    private final Opt.Pick model = this.opt(new Opt.Pick("model", "Модель", "нет", "нет"));
    private final Opt.Key reload = this.opt(new Opt.Key("reload", "Пересканировать", -1));
    private final Map<String, ModelPack> packs = new LinkedHashMap<>();
    private boolean reloadWas;
    private int scanCool;

    public ModelsFeature() {
        super("models", "Models", "Кастомная модель локального игрока из папки models", Category.COSMETICS, false);
        this.scan();
    }

    public static boolean hideVanilla(LivingEntityRenderState state) {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player == null || !(state instanceof PlayerEntityRenderState playerentityrenderstate)) {
            return false;
        } else {
            return AppModels() != null
                && AppModels().on()
                && (Boolean)AppModels().hideVanilla.get()
                && AppModels().pack() != null
                && playerentityrenderstate.id == minecraftclient.player.getId();
        }
    }

    private static ModelsFeature AppModels() {
        return dev.doorevisuals.App.features().find(ModelsFeature.class).orElse(null);
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.scanCool > 0) {
            this.scanCool--;
        }

        int i = (Integer)this.reload.get();
        if (i > 0 && i != -1 && mc.getWindow() != null) {
            boolean flag = org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
            if (flag && !this.reloadWas) {
                this.scan();
            }

            this.reloadWas = flag;
        } else {
            this.reloadWas = false;
        }

        if (this.on() && this.scanCool == 0 && "нет".equals(this.model.get()) && this.packs.size() > 0) {
            this.scanCool = 80;
        }
    }

    public void draw(WorldRenderContext ctx, float tickDelta) {
        if (this.on()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            ModelPack modelpack = this.pack();
            if (clientplayerentity != null && modelpack != null) {
                if (minecraftclient.options.getPerspective() != Perspective.FIRST_PERSON) {
                    double d0 = MathHelper.lerp(tickDelta, clientplayerentity.lastX, clientplayerentity.getX());
                    double d1 = MathHelper.lerp(tickDelta, clientplayerentity.lastY, clientplayerentity.getY());
                    double d2 = MathHelper.lerp(tickDelta, clientplayerentity.lastZ, clientplayerentity.getZ());
                    float f = MathHelper.lerpAngleDegrees(tickDelta, clientplayerentity.lastBodyYaw, clientplayerentity.bodyYaw);
                    float f1 = this.scale.f();
                    float f2 = clientplayerentity.limbAnimator.getAmplitude(tickDelta);
                    float f3 = clientplayerentity.limbAnimator.getAnimationProgress(tickDelta);
                    float f4 = modelpack.idle ? MathHelper.sin(clientplayerentity.age * 0.08F) * 0.6F : 0.0F;
                    int j = -1;

                    for (ModelCube modelcube : modelpack.cubes) {
                        float f5 = 0.0F;
                        String s = modelcube.bone().toLowerCase(Locale.ROOT);
                        if (modelpack.walk && f2 > 0.05F) {
                            if (s.contains("left") && (s.contains("arm") || s.contains("leg") || s.contains("рука") || s.contains("нога"))) {
                                f5 = MathHelper.cos(f3 * 0.6662F) * 12.0F * f2;
                            } else if (s.contains("right") && (s.contains("arm") || s.contains("leg") || s.contains("рука") || s.contains("нога"))) {
                                f5 = MathHelper.cos(f3 * 0.6662F + (float)Math.PI) * 12.0F * f2;
                            }
                        }

                        Mesh.modelCube(
                            ctx,
                            modelpack.texture,
                            d0,
                            d1,
                            d2,
                            f + f5,
                            f1,
                            modelcube.cx(),
                            modelcube.cy() + f4,
                            modelcube.cz(),
                            modelcube.w(),
                            modelcube.h(),
                            modelcube.d(),
                            modelcube.u(),
                            modelcube.v(),
                            modelpack.texW,
                            modelpack.texH,
                            j
                        );
                    }
                }
            }
        }
    }

    private ModelPack pack() {
        String s = (String)this.model.get();
        return s == null || "нет".equals(s) ? null : this.packs.get(s);
    }

    private void scan() {
        this.packs.clear();
        Path path = ClientPaths.models();
        ModelLoader.ensureExample(path);
        List<String> list = new ArrayList<>();
        list.add("нет");

        try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path)) {
            for (Path path1 : directorystream) {
                if (Files.isDirectory(path1)) {
                    ModelPack modelpack = ModelLoader.load(path1);
                    if (modelpack != null) {
                        this.packs.put(modelpack.id, modelpack);
                        list.add(modelpack.id);
                    }
                }
            }
        } catch (Exception exception) {
        }

        this.model.replaceOptions(list);
    }
}
