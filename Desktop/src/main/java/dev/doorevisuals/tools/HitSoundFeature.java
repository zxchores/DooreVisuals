package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.audio.ModSounds;
import dev.doorevisuals.audio.Sfx;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public final class HitSoundFeature extends Feature {
    private final Opt.Pick kind = this.opt(
        new Opt.Pick(
            "kind",
            "\u0417\u0432\u0443\u043a",
            "\u041a\u043b\u0438\u043a",
            "\u041a\u043b\u0438\u043a",
            "\u0422\u0443\u043f\u043e\u0439",
            "\u041a\u0440\u0438\u0441\u0442\u0430\u043b\u043b",
            "\u0411\u043b\u0438\u043f",
            "\u041f\u043e\u043f",
            "\u0421\u043d\u044d\u043f",
            "\u0421\u0442\u0443\u043a",
            "\u0422\u0438\u043a",
            "\u041c\u0435\u043b\u0441\u0442\u0440\u043e\u0439"
        )
    );
    private final Opt.Num volume = this.opt(new Opt.Num("volume", "\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c", 0.7, 0.1, 1.5, 0.05));
    private final Opt.Num pitch = this.opt(new Opt.Num("pitch", "\u0422\u043e\u043d", 1.0, 0.5, 2.0, 0.05));
    private final Opt.Num variance = this.opt(
        new Opt.Num("variance", "\u0420\u0430\u0437\u0431\u0440\u043e\u0441 \u0442\u043e\u043d\u0430", 0.12, 0.0, 0.4, 0.01)
    );
    private final Opt.Flag totem = this.opt(new Opt.Flag("totem", "\u0422\u043e\u0442\u0435\u043c", true));
    private boolean bound;
    private long last;
    private long lastTotem;

    public HitSoundFeature() {
        super(
            "hit_sound",
            "\u0417\u0432\u0443\u043a \u0443\u0434\u0430\u0440\u0430",
            "\u0421\u0432\u043e\u0439 \u0437\u0432\u0443\u043a \u0443\u0434\u0430\u0440\u0430 \u2014 \u0444\u0430\u0439\u043b\u044b \u0443\u0436\u0435 \u0432\u043d\u0443\u0442\u0440\u0438 \u043c\u043e\u0434\u0430",
            Category.TOOLS,
            true
        );
    }

    public void bind() {
        if (!this.bound) {
            this.bound = true;
            AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
                if (level.isClient() && this.on() && entity instanceof LivingEntity) {
                    long i = System.currentTimeMillis();
                    if (i - this.last < 80L) {
                        return ActionResult.PASS;
                    } else {
                        this.last = i;
                        float f = this.pitch.f() + (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * this.variance.f();
                        Sfx.play(soundOf((String)this.kind.get()), this.volume.f(), Math.max(0.5F, f));
                        return ActionResult.PASS;
                    }
                } else {
                    return ActionResult.PASS;
                }
            });
        }
    }

    public static boolean muteVanillaHits() {
        return App.features().find(HitSoundFeature.class).map(Feature::on).orElse(false);
    }

    public static boolean muteVanillaTotem() {
        return App.features().find(HitSoundFeature.class).map(f -> f.on() && (Boolean)f.totem.get()).orElse(false);
    }

    public static void playTotem() {
        App.features().find(HitSoundFeature.class).ifPresent(HitSoundFeature::playTotemNow);
    }

    private void playTotemNow() {
        if (this.on() && (Boolean)this.totem.get()) {
            long i = System.currentTimeMillis();
            if (i - this.lastTotem >= 200L) {
                this.lastTotem = i;
                Sfx.play(ModSounds.TOTEM_MELL, this.volume.f(), 1.0F);
            }
        }
    }

    private static SoundEvent soundOf(String kind) {
        return switch (kind) {
            case "\u0422\u0443\u043f\u043e\u0439" -> ModSounds.HIT_THUD;
            case "\u041a\u0440\u0438\u0441\u0442\u0430\u043b\u043b" -> ModSounds.HIT_CRYSTAL;
            case "\u0411\u043b\u0438\u043f" -> ModSounds.HIT_BLIP;
            case "\u041f\u043e\u043f" -> ModSounds.HIT_POP;
            case "\u0421\u043d\u044d\u043f" -> ModSounds.HIT_SNAP;
            case "\u0421\u0442\u0443\u043a" -> ModSounds.HIT_KNOCK;
            case "\u0422\u0438\u043a" -> ModSounds.HIT_TICK;
            case "\u041c\u0435\u043b\u0441\u0442\u0440\u043e\u0439" -> ModSounds.HIT_BEM;
            default -> ModSounds.HIT_CLICK;
        };
    }
}
