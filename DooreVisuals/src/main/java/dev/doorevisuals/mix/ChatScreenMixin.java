package dev.doorevisuals.mix;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    /*
     * width и textRenderer объявлены не в ChatScreen, а в его родителе Screen,
     * а @Shadow наследованные поля не находит — именно это давало
     * "@Shadow field width was not located in the target class" и валило запуск.
     * Поэтому берём их через Screen и клиент.
     */
    private int doore$width() {
        return ((Screen) (Object) this).width;
    }

    private TextRenderer doore$text() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private int doore$cfgSuggestionIndex = 0;
    private String doore$cfgLastText = "";
    private boolean doore$cfgGhost = false;

    private static final String[] CFG_COMMANDS = {
        ".cfg load <имя>",
        ".cfg dir",
        ".cfg delete <имя>",
        ".cfg list",
        ".cfg reload",
        ".cfg save [имя]",
        ".cfg help"
    };

    @Inject(method = "render", at = @At("TAIL"))
    private void doore$renderCfgSuggestions(
        DrawContext context,
        int mouseX,
        int mouseY,
        float delta,
        CallbackInfo ci
    ) {
        String text = this.chatField.getText();

        if (!text.toLowerCase(Locale.ROOT).startsWith(".cfg")) {
            this.doore$cfgSuggestionIndex = 0;
            this.doore$cfgLastText = text;
            if (this.doore$cfgGhost) {
                this.doore$cfgGhost = false;
                this.chatField.setSuggestion(null);
            }
            return;
        }

        if (!text.equals(this.doore$cfgLastText)) {
            this.doore$cfgSuggestionIndex = 0;
            this.doore$cfgLastText = text;
        }

        List<String> suggestions = this.doore$getSuggestions(text);
        if (suggestions.isEmpty()) {
            return;
        }
        this.doore$cfgSuggestionIndex = Math.min(this.doore$cfgSuggestionIndex, suggestions.size() - 1);
        // The rest of the chosen command is shown in the field in grey, like the game's own completion.
        String chosen = suggestions.get(this.doore$cfgSuggestionIndex);
        this.chatField.setSuggestion(chosen.regionMatches(true, 0, text, 0, text.length()) ? chosen.substring(text.length()) : null);
        this.doore$cfgGhost = true;

        // The suggestions look like the game's own: a black translucent list above the field, the chosen one in yellow,
        // the rest in grey, the first match on top. No title, no frame, no accent colours.
        int rowHeight = 12;
        int width = 0;
        for (String s : suggestions) {
            width = Math.max(width, this.doore$text().getWidth(s));
        }
        width += 2;
        int rows = Math.min(suggestions.size(), 10);
        int height = rows * rowHeight;
        int x = Math.max(0, Math.min(this.chatField.getCharacterX(0) - 1, this.doore$width() - width));
        int y = this.chatField.getY() - 3 - height;
        if (y < 0) {
            y = this.chatField.getY() + this.chatField.getHeight() + 3;
        }
        int first = Math.max(0, Math.min(this.doore$cfgSuggestionIndex - rows / 2, suggestions.size() - rows));
        context.fill(x, y, x + width, y + height, 0xD0000000);
        for (int i = 0; i < rows; i++) {
            int index = first + i;
            int rowY = y + i * rowHeight;
            context.drawTextWithShadow(
                this.doore$text(),
                suggestions.get(index),
                x + 1,
                rowY + 2,
                index == this.doore$cfgSuggestionIndex ? 0xFFFFFF00 : 0xFFAAAAAA
            );
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void doore$cfgSuggestionKeys(
        KeyInput input,
        CallbackInfoReturnable<Boolean> cir
    ) {
        String text = this.chatField.getText();

        if (!text.toLowerCase(Locale.ROOT).startsWith(".cfg")) {
            return;
        }

        List<String> suggestions = this.doore$getSuggestions(text);
        if (suggestions.isEmpty()) {
            return;
        }

        int key = input.key();

        if (key == GLFW.GLFW_KEY_DOWN) {
            this.doore$cfgSuggestionIndex =
                (this.doore$cfgSuggestionIndex + 1)
                % suggestions.size();
            cir.setReturnValue(true);
            return;
        }

        if (key == GLFW.GLFW_KEY_UP) {
            this.doore$cfgSuggestionIndex =
                (this.doore$cfgSuggestionIndex - 1 + suggestions.size())
                % suggestions.size();
            cir.setReturnValue(true);
            return;
        }

        if (key == GLFW.GLFW_KEY_TAB) {
            String selected =
                suggestions.get(this.doore$cfgSuggestionIndex);

            String replacement;
            if (text.equalsIgnoreCase(".cfg")) {
                replacement = selected + " ";
            } else if (text.trim().equalsIgnoreCase(".cfg")) {
                replacement = selected + " ";
            } else {
                replacement = this.doore$completePartial(
                    text,
                    selected
                );
            }

            this.chatField.setText(replacement);
            this.chatField.setCursorToEnd(false);
            cir.setReturnValue(true);
        }
    }

    private List<String> doore$getSuggestions(String text) {
        String lower =
            text.toLowerCase(Locale.ROOT);

        if (lower.equals(".cfg")) {
            return this.doore$commandsOnly();
        }

        if (!lower.startsWith(".cfg ")) {
            return this.doore$commandsOnly().stream()
                .filter(command ->
                    command.startsWith(text.toLowerCase(Locale.ROOT))
                    || command.startsWith(".cfg " + text.substring(5))
                )
                .toList();
        }

        String partial =
            text.substring(5)
                .toLowerCase(Locale.ROOT)
                .trim();

        if (partial.isEmpty()) {
            return this.doore$commandsOnly();
        }

        List<String> result = new ArrayList<>();

        for (String command : CFG_COMMANDS) {
            String commandPart =
                command.substring(5).toLowerCase(Locale.ROOT);

            if (
                commandPart.startsWith(partial)
                || commandPart.contains(partial)
            ) {
                result.add(command);
            }
        }

        return result;
    }

    private List<String> doore$commandsOnly() {
        return List.of(CFG_COMMANDS);
    }

    private String doore$completePartial(
        String original,
        String selected
    ) {
        if (!original.toLowerCase(Locale.ROOT).startsWith(".cfg")) {
            return original;
        }

        if (original.startsWith(".cfg ")) {
            String typed = original.substring(5).trim();
            String command = selected.substring(5);

            int space = command.indexOf(' ');
            if (space >= 0) {
                command = command.substring(0, space);
            }

            if (command.startsWith(typed)) {
                return ".cfg " + command + " ";
            }

            return ".cfg " + command + " ";
        }

        return selected + " ";
    }
}
