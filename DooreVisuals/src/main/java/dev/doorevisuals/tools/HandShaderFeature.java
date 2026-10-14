package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.draw.PostFx;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.draw.VisualQuality;
import net.minecraft.client.MinecraftClient;

/**
 * Hand Shader — переливающийся слой на настоящей модели руки от первого лица.
 *
 * <p>Геометрия — та же самая {@code ModelPart}, которую рисует ванильный проход: эффект накладывается
 * вторым проходом по ней, а маской служит текстура скина, поэтому волны идут по коже и рукаву, а не по
 * кубоиду. Рисует пайплайн стиля ({@code Gpu.handSurfacePipe}), стиль приходит в шейдер дефайном.</p>
 */
public final class HandShaderFeature extends Feature implements Tick {
    private static volatile boolean live = false;
    private static volatile int styleIndex = 0;

    private final Opt.Pick style = this.opt(
            new Opt.Pick(
                    "style",
                    "Стиль волн",
                    "Аура",
                    "Аура",
                    "Неон",
                    "Пульс",
                    "Контур"
            )
    );

    public HandShaderFeature() {
        super(
                "hand_shader",
                "Hand Shader",
                "Переливающиеся волны по руке от первого лица",
                Category.TOOLS,
                false
        );
    }

    @Override
    protected void enable() {
        sync();
    }

    @Override
    protected void disable() {
        live = false;
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (on()) {
            sync();
        } else {
            live = false;
        }
    }

    private void sync() {
        styleIndex = switch (this.style.get()) {
            case "Неон" -> 1;
            case "Пульс" -> 2;
            case "Контур" -> 3;
            default -> 0;
        };
        live = on();
    }

    /** Держит акцент темы в текстуре: перелив должен идти цветами темы, а не только радугой. */
    public static void syncAccent() {
        dev.doorevisuals.draw.AccentTexture.update(
                dev.doorevisuals.tools.ThemeFeature.effectTint()
        );
    }

    /**
     * Вызывается из {@code PlayerEntityRendererMixin} в конце {@code PlayerEntityRenderer.renderArm}:
     * поза и часть руки — те же, что у ванильного прохода, поэтому волны совпадают с рукой и по
     * положению, и по анимации.
     */
    private static boolean logged;

    public static void renderArm(
            net.minecraft.client.util.math.MatrixStack pose,
            net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
            int light,
            net.minecraft.util.Identifier skin,
            net.minecraft.client.model.ModelPart arm
    ) {
        if (!live || pose == null || queue == null || arm == null || skin == null) {
            if (!live && !logged) {
                logged = true;
                org.slf4j.LoggerFactory.getLogger("DooreVisuals/HandShader")
                        .info("Hand Shader: вызов есть, но фича выключена или нет текстуры (live={}, skin={})", live, skin);
            }
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null || mc.options.getPerspective() != net.minecraft.client.option.Perspective.FIRST_PERSON) {
            return;
        }

        syncAccent();

        if (!logged) {
            logged = true;
            org.slf4j.LoggerFactory.getLogger("DooreVisuals/HandShader")
                    .info("Hand Shader: слой наложен (стиль {}, скин {})", styleIndex, skin);
        }

        queue.submitModelPart(
                arm,
                pose,
                Types.handSurface(styleIndex, skin),
                light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV,
                null
        );
    }
}
