package dev.doorevisuals.mix;

import dev.doorevisuals.App;
import dev.doorevisuals.cosmetics.CosmeticsFeature;
import dev.doorevisuals.cosmetics.CosmeticsRegistry;
import dev.doorevisuals.friends.FriendsFeature;
import net.minecraft.entity.Entity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityNameTagMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void doore$badge(CallbackInfoReturnable<Text> cir) {
        if (App.live()) {
            Text text = (Text)cir.getReturnValue();
            Entity entity = (Entity)this;
            if (FriendsFeature.isFriend(entity.getUuid())) {
                int i = FriendsFeature.nickColor();
                Text text2 = text == null ? Text.empty() : text.copy().styled(s -> s.withColor(i));
                String s = text == null ? "" : text.getString();
                if (FriendsFeature.showBadge() && !s.startsWith("\u2605")) {
                    cir.setReturnValue(Text.literal("\u2605 ").styled(sx -> sx.withColor(i)).append(text2));
                } else {
                    cir.setReturnValue(text2);
                }
            } else {
                Text text1 = App.features()
                    .find(CosmeticsFeature.class)
                    .filter(CosmeticsFeature::showBadge)
                    .map(
                        f -> {
                            if (!CosmeticsRegistry.isDoore(entity.getUuid())) {
                                return text;
                            } else {
                                String s1 = text == null ? "" : text.getString();
                                return (Text)(s1.startsWith("\u25c6")
                                    ? text
                                    : Text.literal("\u25c6 ").append((Text)(text == null ? Text.empty() : text)));
                            }
                        }
                    )
                    .orElse(text);
                cir.setReturnValue(text1);
            }
        }
    }
}
