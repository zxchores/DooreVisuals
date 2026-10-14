package dev.doorevisuals.mix;

import dev.doorevisuals.tools.CommandFixFeature;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatScreen.class)
public abstract class CommandFixMixin {
    @ModifyVariable(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String doore$layout(String chatText) {
        return CommandFixFeature.rewrite(chatText);
    }
}
