package dev.doorevisuals.tools;

import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;

public final class AutoSprintFeature extends Feature implements Tick {
    private final Opt.Flag forwardOnly = this.opt(new Opt.Flag("forward", "\u0422\u043e\u043b\u044c\u043a\u043e \u0432\u043f\u0435\u0440\u0451\u0434", true));
    private final Opt.Flag inWater = this.opt(new Opt.Flag("water", "\u0412 \u0432\u043e\u0434\u0435", false));
    private final Opt.Flag whileEating = this.opt(new Opt.Flag("eating", "\u041f\u0440\u0438 \u0435\u0434\u0435", false));

    public AutoSprintFeature() {
        super(
            "auto_sprint",
            "Auto Sprint",
            "\u0421\u043f\u0440\u0438\u043d\u0442 \u0431\u0435\u0437 \u0443\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u044f Ctrl",
            Category.TOOLS,
            false
        );
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on()) {
            ClientPlayerEntity clientplayerentity = mc.player;
            if (clientplayerentity != null && !clientplayerentity.isSpectator() && !clientplayerentity.hasVehicle()) {
                if ((Boolean)this.whileEating.get() || !clientplayerentity.isUsingItem()) {
                    if ((Boolean)this.inWater.get() || !clientplayerentity.isTouchingWater() && !clientplayerentity.isSubmergedInWater()) {
                        boolean flag = clientplayerentity.input.hasForwardMovement();

                        try {
                            PlayerInput playerinput = clientplayerentity.input.playerInput;
                            if ((Boolean)this.forwardOnly.get()) {
                                flag = playerinput.forward() && !playerinput.backward();
                            } else {
                                flag = playerinput.forward() || clientplayerentity.input.hasForwardMovement();
                            }
                        } catch (Throwable throwable) {
                        }

                        if (flag && clientplayerentity.getHungerManager().getFoodLevel() > 6 && !clientplayerentity.horizontalCollision) {
                            clientplayerentity.setSprinting(true);
                        }
                    }
                }
            }
        }
    }
}
