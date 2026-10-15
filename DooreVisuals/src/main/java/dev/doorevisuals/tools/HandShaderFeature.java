package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.draw.Theme;
import dev.doorevisuals.draw.Types;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Hand Shader: translucent waves in the colours of the theme flowing over the first-person hand.
 *
 * <p>The geometry is the very {@code ModelPart} the game has just drawn (the mixin passes it in), drawn a second time
 * with the wave pipeline; the skin texture is the mask, so the waves lie on the skin and the sleeve and nowhere else.
 * The settings reach the shader in the vertices of that model part: the tint colour is the primary colour and the
 * opacity, the overlay value holds the speed and the wave size, the light value holds the secondary colour. A theme
 * change or a slider is therefore visible on the next frame, and no texture or uniform has to be kept in sync.</p>
 *
 * <p>The pass goes through batch 1 of the render queue. The model parts of one batch are drawn in the order of a hash
 * map, so in batch 0 the waves could land under the arm; a later batch is always drawn after the arm.</p>
 */
public final class HandShaderFeature extends Feature {
    private static final String WAVES = "Волны";
    private static final String RIPPLES = "Рябь";
    private static final String PULSE = "Пульс";
    private static final String OUTLINE = "Контур";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("DooreVisuals/HandShader");
    private static HandShaderFeature self;

    private boolean logged;

    private final Opt.Pick style = this.opt(new Opt.Pick("style", "Стиль", WAVES, WAVES, RIPPLES, PULSE, OUTLINE).chips());
    private final Opt.Num alpha = this.opt(new Opt.Num("alpha", "Прозрачность", 0.75, 0.1, 1.0, 0.05));
    private final Opt.Num speed = this.opt(new Opt.Num("speed", "Скорость", 1.0, 0.2, 3.0, 0.1));
    private final Opt.Num scale = this.opt(new Opt.Num("scale", "Размер волн", 1.0, 0.4, 3.0, 0.1));
    private final Opt.Flag onItem = this.opt(new Opt.Flag("on_item", "Волны на предмете в руке", true));
    private final Opt.Flag themeColor = this.opt(new Opt.Flag("theme_color", "Цвет из темы", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "Цвет", 0xFF8A5CF6).visibleWhen(() -> !this.themeColor.get()));

    public HandShaderFeature() {
        super(
                "hand_shader",
                "Hand Shader",
                "Полупрозрачные волны цвета темы по руке от первого лица",
                Category.TOOLS,
                false
        );
        self = this;
    }

    private static net.minecraft.item.ItemDisplayContext itemContext;

    /** The game starts drawing the item of a hand; the waves go on it only when it is the first-person hand. */
    public static void beginItem(net.minecraft.item.ItemDisplayContext context) {
        itemContext = context;
    }

    public static void endItem() {
        itemContext = null;
    }

    /**
     * Waves on the item in the first-person hand: the same quads, drawn again in the wave layer. The atlas the quads come from
     * is the mask (only the pixels of the item get waves); the colour travels in the light value.
     */
    public static void submitItemWaves(MatrixStack pose, OrderedRenderCommandQueue queue, java.util.List<net.minecraft.client.render.model.BakedQuad> quads,
                                       int[] tints, int outline) {
        HandShaderFeature f = self;
        net.minecraft.item.ItemDisplayContext context = itemContext;
        if (f == null || !f.on() || !f.onItem.get() || context == null || !context.isFirstPerson() || quads == null || quads.isEmpty()) {
            return;
        }
        Identifier atlas = quads.get(0).sprite().getAtlasId();
        Identifier mask = Identifier.of(atlas.getNamespace(), "textures/atlas/" + atlas.getPath() + ".png");
        int primary = f.themeColor.get() ? Theme.EFFECT & 0xFFFFFF : f.color.get() & 0xFFFFFF;
        int alpha = Math.round(MathHelper.clamp(f.alpha.f(), 0.0F, 1.0F) * 255.0F);
        int packed = ((primary >> 16) & 0xFF) | (((primary >> 8) & 0xFF) << 8) | ((primary & 0xFF) << 16) | (alpha << 24);
        int settings = (Math.round(f.speed.f() * 1000.0F) & 0xFFFF) | (Math.round(f.scale.f() * 1000.0F) << 16);
        queue.getBatchingQueue(1).submitItem(pose, context, packed, settings, outline, tints, quads,
                Types.handWave(styleIndex(f.style.get()), mask, true), net.minecraft.client.render.item.ItemRenderState.Glint.NONE);
    }

    private static int styleIndex(String name) {
        return switch (name) {
            case RIPPLES -> 1;
            case PULSE -> 2;
            case OUTLINE -> 3;
            default -> 0;
        };
    }

    /**
     * Called from {@code PlayerEntityRendererMixin} at the end of {@code PlayerEntityRenderer.renderArm}: the pose and the
     * model part are the ones of the vanilla pass, so the waves follow the hand and its animation exactly.
     */
    public static void renderArm(MatrixStack pose, OrderedRenderCommandQueue queue, int light, Identifier skin, ModelPart arm) {
        HandShaderFeature f = self;
        if (f == null || !f.on() || pose == null || queue == null || arm == null || skin == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null || mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            return;
        }

        int primary;
        int secondary;
        if (f.themeColor.get()) {
            primary = Theme.EFFECT & 0xFFFFFF;
            secondary = Theme.ACCENT_HOT & 0xFFFFFF;
        } else {
            primary = f.color.get() & 0xFFFFFF;
            secondary = lighten(primary, 0.35F);
        }
        int tint = (Math.round(MathHelper.clamp(f.alpha.f(), 0.0F, 1.0F) * 255.0F) << 24) | primary;
        // Overlay: speed and size in thousandths (low and high 16 bits). Light: the secondary colour, r | g << 8 and b << 16.
        int settings = (Math.round(f.speed.f() * 1000.0F) & 0xFFFF) | (Math.round(f.scale.f() * 1000.0F) << 16);
        int secondaryPacked = ((secondary >> 16) & 0xFF) | (((secondary >> 8) & 0xFF) << 8) | ((secondary & 0xFF) << 16);

        queue.getBatchingQueue(1).submitModelPart(arm, pose, Types.handWave(styleIndex(f.style.get()), skin), secondaryPacked, settings, null, tint, null);
        if (!f.logged) {
            f.logged = true;
            LOG.info("Hand Shader: waves submitted (style {}, skin {})", styleIndex(f.style.get()), skin);
        }
    }

    private static int lighten(int rgb, float t) {
        int r = (int)(((rgb >> 16) & 0xFF) * (1.0F - t) + 255 * t);
        int g = (int)(((rgb >> 8) & 0xFF) * (1.0F - t) + 255 * t);
        int b = (int)((rgb & 0xFF) * (1.0F - t) + 255 * t);
        return (r << 16) | (g << 8) | b;
    }
}
