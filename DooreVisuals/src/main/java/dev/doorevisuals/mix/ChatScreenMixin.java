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

    private static final String[] CFG_COMMANDS = {
        ".cfg load <имя>",
        ".cfg dir",
        ".cfg delete <имя>",
        ".cfg list",
        ".cfg reload",
        ".cfg save [имя]",
        ".cfg help"
    };

    private static final int LAVENDER = 0xFFC9B8;
    private static final int LAVENDER_BRIGHT = 0xFFE7DF;
    private static final int LAVENDER_DIM = 0xFF9B8AC9;
    private static final int PANEL = 0xE50E0B14;
    private static final int PANEL_2 = 0xF0130E1A;
    private static final int BORDER = 0xA66E5CA3;

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

        int maxWidth = 0;
        for (String s : suggestions) {
            maxWidth = Math.max(
                maxWidth,
                this.doore$text().getWidth(s) + 22
            );
        }

        int width = Math.min(
            Math.max(190, maxWidth),
            Math.min(420, this.doore$width() - 24)
        );

        int rowHeight = 16;
        int height = suggestions.size() * rowHeight + 10;

        int x = Math.max(
            12,
            Math.min(
                this.chatField.getX(),
                this.doore$width() - width - 12
            )
        );

        int y = this.chatField.getY() - height - 6;
        if (y < 10) {
            y = this.chatField.getY() + this.chatField.getHeight() + 6;
        }

        context.fill(
            x - 1,
            y - 1,
            x + width + 1,
            y + height + 1,
            BORDER
        );

        context.fill(
            x,
            y,
            x + width,
            y + height,
            PANEL
        );

        context.fill(
            x + 1,
            y + 1,
            x + 3,
            y + height - 1,
            LAVENDER
        );

        context.drawTextWithShadow(
            this.doore$text(),
            "DOORE",
            x + 10,
            y + 5,
            LAVENDER_BRIGHT
        );

        context.drawTextWithShadow(
            this.doore$text(),
            "CONFIG",
            x + 47,
            y + 5,
            LAVENDER_DIM
        );

        int listY = y + 18;

        for (int i = 0; i < suggestions.size(); i++) {
            boolean selected =
                i == this.doore$cfgSuggestionIndex;

            if (selected) {
                context.fill(
                    x + 5,
                    listY - 1,
                    x + width - 5,
                    listY + rowHeight - 2,
                    PANEL_2
                );
            }

            context.drawTextWithShadow(
                this.doore$text(),
                suggestions.get(i),
                x + 10,
                listY + 1,
                selected
                    ? LAVENDER_BRIGHT
                    : LAVENDER_DIM
            );

            listY += rowHeight;
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
