package dev.doorevisuals.tools;

import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.data.Favorites;
import dev.doorevisuals.data.Folders;
import dev.doorevisuals.data.Profiles;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.ui.ConfigApplyScreen;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents.AllowChat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class ConfigFeature extends Feature {
    private final Opt.Text name = this.opt(new Opt.Text("name", "\u0418\u043c\u044f \u043a\u043e\u043d\u0444\u0438\u0433\u0430", "default"));
    private final Opt.Text search = this.opt(new Opt.Text("search", "\u041f\u043e\u0438\u0441\u043a", ""));
    private final Opt.Flag serverHint = this.opt(
        new Opt.Flag("server_hint", "\u0418\u043c\u044f \u043f\u043e \u0441\u0435\u0440\u0432\u0435\u0440\u0443", false)
    );
    private final Opt.Text note = this.opt(new Opt.Text("note", "\u0417\u0430\u043c\u0435\u0442\u043a\u0430", ""));
    private final Opt.Text tags = this.opt(new Opt.Text("tags", "\u0422\u0435\u0433\u0438", ""));
    private String draftName = "default";
    private String filter = "";
    private boolean chatBound;

    public ConfigFeature() {
        super(
            "config",
            "Config",
            "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c, \u043f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c, \u043a\u043e\u043f\u0438\u044f, \u0431\u044d\u043a\u0430\u043f \u0438 \u043f\u043e\u0438\u0441\u043a. .cfg help / load / save / list / dir / del",
            Category.CONFIG,
            true
        );
    }

    public void bind() {
        if (!this.chatBound) {
            this.chatBound = true;
            ClientSendMessageEvents.ALLOW_CHAT.register((AllowChat)message -> !this.handleChat(message));
            ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> client.execute(this::onJoined));
        }
    }

    private void onJoined() {
        if (App.live()) {
            String s = Profiles.currentServerKey();
            if (!s.isEmpty()) {
                String s1 = App.save().profiles().suggestForServer(s);
                if (s1 != null && !s1.isBlank()) {
                    if (!s1.equalsIgnoreCase(App.save().profiles().active())) {
                        if (!App.save().profiles().skipped(s, s1)) {
                            ConfigApplyScreen.offer(s, s1);
                        }
                    }
                }
            }
        }
    }

    public String filter() {
        return this.filter;
    }

    public void setFilter(String f) {
        this.filter = f == null ? "" : f.trim().toLowerCase();
        this.search.set(this.filter);
    }

    public boolean serverHint() {
        return (Boolean)this.serverHint.get();
    }

    public String draftName() {
        return this.draftName;
    }

    public void setDraftName(String n) {
        this.draftName = n == null ? "" : n;
        this.name.set(this.draftName.isBlank() ? "default" : this.draftName);
    }

    @Override
    public String about() {
        String s = App.live() ? App.save().profiles().active() : "default";
        return "\u041a\u043e\u043d\u0444\u0438\u0433\u0438 \u00b7 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0439: " + s;
    }

    public List<String> list() {
        if (!App.live()) {
            return List.of();
        } else {
            List<String> list = Favorites.sortConfigs(App.save().profiles().list());
            return this.filter.isEmpty() ? list : list.stream().filter(n -> n.toLowerCase().contains(this.filter)).toList();
        }
    }

    public boolean renameDraft() {
        if (!App.live()) {
            return false;
        } else {
            String s = App.save().profiles().active();
            String s1 = this.draftName.isBlank() ? s : this.draftName.trim();
            boolean flag = App.save().profiles().rename(s, s1);
            toast(flag ? "Rename \u2192 " + s1 : App.save().profiles().lastError());
            if (flag) {
                this.syncName(s1);
            }

            return flag;
        }
    }

    public boolean duplicateDraft() {
        if (!App.live()) {
            return false;
        } else {
            String s = App.save().profiles().active();
            String s1 = this.draftName.isBlank() ? s + "-copy" : this.draftName.trim();
            boolean flag = App.save().profiles().duplicate(s, s1);
            toast(flag ? "Dup \u2192 " + s1 : App.save().profiles().lastError());
            return flag;
        }
    }

    public void suggestServerName() {
        if ((Boolean)this.serverHint.get()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (minecraftclient.getCurrentServerEntry() != null && minecraftclient.getCurrentServerEntry().address != null) {
                String s = minecraftclient.getCurrentServerEntry().address.replace(':', '-').replace('.', '-');
                this.setDraftName(Profiles.clean(s));
            }
        }
    }

    public boolean saveDraft() {
        if (!App.live()) {
            return false;
        } else {
            String s = this.draftName.isBlank() ? "default" : this.draftName.trim();
            boolean flag = App.save().profiles().saveAs(s, (String)this.note.get(), (String)this.tags.get());
            if (flag) {
                this.syncName(App.save().profiles().active());
                this.bindCurrentServer();
                toast("\u0421\u043e\u0445\u0440\u0430\u043d\u0451\u043d: " + App.save().profiles().active());
            } else {
                String s1 = App.save().profiles().lastError();
                toast(s1.isEmpty() ? "\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f" : s1);
            }

            return flag;
        }
    }

    public boolean setDraftDefault() {
        if (!App.live()) {
            return false;
        } else {
            String s = this.draftName.isBlank() ? App.save().profiles().active() : this.draftName.trim();
            boolean flag = App.save().profiles().setDefault(s);
            toast(flag ? "Default \u2192 " + s : App.save().profiles().lastError());
            return flag;
        }
    }

    public String activeMetadata() {
        if (!App.live()) {
            return "";
        } else {
            Profiles.Metadata profiles$metadata = App.save().profiles().metadata(App.save().profiles().active());
            String s = profiles$metadata.updatedAt() <= 0L
                ? "\u2014"
                : Instant.ofEpochMilli(profiles$metadata.updatedAt()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
            String s1 = profiles$metadata.tags().isBlank() ? "" : " \u00b7 #" + profiles$metadata.tags().replace(" ", " #");
            return s + s1;
        }
    }

    public boolean applyNamed(String profileName) {
        if (!App.live()) {
            return false;
        } else {
            Profiles profiles = App.save().profiles();
            String s = profiles.active();
            if (profileName != null && !profileName.isBlank() && !s.equalsIgnoreCase(profileName.trim())) {
                profiles.saveAs(s);
            }

            boolean flag = profiles.load(profileName);
            if (flag) {
                this.syncName(App.save().profiles().active());
                this.bindCurrentServer();
                toast("\u041f\u0440\u0438\u043c\u0435\u043d\u0451\u043d: " + App.save().profiles().active());
            } else {
                toast("\u041d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + profileName);
            }

            return flag;
        }
    }

    public boolean deleteNamed(String profileName) {
        if (!App.live()) {
            return false;
        } else {
            boolean flag = App.save().profiles().delete(profileName);
            if (flag) {
                Favorites.removeConfig(profileName);
                this.syncName(App.save().profiles().active());
                toast("\u0423\u0434\u0430\u043b\u0451\u043d: " + profileName);
            }

            return flag;
        }
    }

    public boolean openConfigsFolder() {
        if (!App.live()) {
            Folders.ensure(ClientPaths.configs());
            return Folders.open(ClientPaths.configs());
        } else {
            boolean flag = App.save().profiles().openFolder();
            toast(Folders.pretty(App.save().profiles().dir()));
            return flag;
        }
    }

    public boolean openNamedFolder(String profileName) {
        return this.openConfigsFolder();
    }

    public static boolean run(String act, String profileName) {
        if (!App.live()) {
            return false;
        } else {
            Profiles profiles = App.save().profiles();
            String s = profileName != null && !profileName.isBlank() ? profileName.trim() : profiles.active();

            boolean flag = switch (act) {
                case "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c" -> profiles.saveAs(s);
                case "\u0417\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c" -> profiles.load(s);
                case "\u0421\u043e\u0437\u0434\u0430\u0442\u044c", "\u041d\u043e\u0432\u044b\u0439" -> profiles.saveAs(s.isEmpty() ? "profile" : s);
                case "\u0423\u0434\u0430\u043b\u0438\u0442\u044c" -> profiles.delete(s);
                case "\u041f\u0430\u043f\u043a\u0430 \u043a\u043b\u0438\u0435\u043d\u0442\u0430", "\u041f\u0430\u043f\u043a\u0430", "\u041f\u0430\u043f\u043a\u0430 \u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432" -> profiles.openFolder(
                    profiles.active()
                );
                default -> false;
            };
            if (flag) {
                App.features().find(ConfigFeature.class).ifPresent(c -> c.syncName(profiles.active()));
                App.features().find(HudFeature.class).ifPresent(h -> h.notify("\u041a\u043e\u043d\u0444\u0438\u0433", act + " \u00b7 " + profiles.active()));
            }

            return flag;
        }
    }

    public void syncName(String active) {
        this.name.set(active);
        this.draftName = active;
    }

    public static String activeName() {
        return !App.live() ? "default" : App.save().profiles().active();
    }

    private void bindCurrentServer() {
        String s = Profiles.currentServerKey();
        if (!s.isEmpty()) {
            App.save().profiles().bindServer(s, App.save().profiles().active());
        }
    }

    private boolean handleChat(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String raw = message.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // Local client command. Never send .cfg commands to the server.
        if (!lower.startsWith(".cfg")) {
            return false;
        }

        List<String> parts = tokenize(raw);
        MinecraftClient mc = MinecraftClient.getInstance();

        if (parts.isEmpty()) {
            printHelp(mc);
            return true;
        }

        String root = parts.get(0).toLowerCase(Locale.ROOT);
        if (!root.equals(".cfg")) {
            return false;
        }

        if (parts.size() == 1) {
            printHelp(mc);
            return true;
        }

        String action = parts.get(1).toLowerCase(Locale.ROOT);

        if (action.equals("help")) {
            printHelp(mc);
            return true;
        }

        if (!App.live()) {
            chatError(mc, "Клиент ещё не готов.");
            return true;
        }

        Profiles profiles = App.save().profiles();
        String rest = parts.size() >= 3 ? joinFrom(parts, 2) : "";

        try {
            switch (action) {
                case "load" -> {
                    if (rest.isBlank()) {
                        chatUsage(mc, ".cfg load <имя>");
                        return true;
                    }

                    String target = cleanCommandName(rest);
                    if (target.isBlank()) {
                        chatUsage(mc, ".cfg load <имя>");
                        return true;
                    }

                    boolean ok = this.applyNamed(target);
                    if (ok) {
                        chatSuccess(
                            mc,
                            "Конфиг загружен",
                            profiles.active() + ".cfg"
                        );
                    } else {
                        chatError(
                            mc,
                            errorOr(
                                profiles.lastError(),
                                "Конфиг не найден: " + target
                            )
                        );
                    }
                    return true;
                }

                case "save" -> {
                    String target = rest.isBlank()
                        ? profiles.active()
                        : cleanCommandName(rest);

                    if (target.isBlank()) {
                        target = "default";
                    }

                    this.setDraftName(target);
                    boolean ok = this.saveDraft();

                    if (ok) {
                        chatSuccess(
                            mc,
                            "Конфиг сохранён",
                            profiles.active() + ".cfg"
                        );
                    } else {
                        chatError(
                            mc,
                            errorOr(
                                profiles.lastError(),
                                "Не удалось сохранить конфиг"
                            )
                        );
                    }
                    return true;
                }

                case "list" -> {
                    String query = rest.isBlank()
                        ? ""
                        : rest.toLowerCase(Locale.ROOT);

                    List<String> configs =
                        Favorites.sortConfigs(profiles.list());

                    if (!query.isBlank()) {
                        configs = configs.stream()
                            .filter(name -> name.toLowerCase(Locale.ROOT).contains(query))
                            .toList();
                    }

                    printConfigList(
                        mc,
                        profiles,
                        configs,
                        query
                    );
                    return true;
                }

                case "delete" -> {
                    if (rest.isBlank()) {
                        chatUsage(mc, ".cfg delete <имя>");
                        return true;
                    }

                    String target = cleanCommandName(rest);
                    if (target.isBlank()) {
                        chatUsage(mc, ".cfg delete <имя>");
                        return true;
                    }

                    boolean deletingActive =
                        profiles.active().equalsIgnoreCase(target);

                    boolean ok = this.deleteNamed(target);

                    if (ok) {
                        chatSuccess(
                            mc,
                            "Конфиг удалён",
                            target + ".cfg"
                        );

                        if (deletingActive) {
                            chatMuted(
                                mc,
                                "Активным стал: "
                                    + profiles.active()
                                    + ".cfg"
                            );
                        }
                    } else {
                        chatError(
                            mc,
                            errorOr(
                                profiles.lastError(),
                                "Не удалось удалить конфиг"
                            )
                        );
                    }
                    return true;
                }

                case "dir" -> {
                    boolean ok = this.openConfigsFolder();

                    if (ok) {
                        chatSuccess(
                            mc,
                            "Папка конфигов открыта",
                            Folders.pretty(profiles.dir())
                        );
                    } else {
                        chatError(
                            mc,
                            "Не удалось открыть папку конфигов"
                        );
                    }
                    return true;
                }

                case "reload" -> {
                    String active = profiles.active();
                    boolean ok = profiles.load(active);

                    if (ok) {
                        this.syncName(profiles.active());
                        this.bindCurrentServer();

                        chatSuccess(
                            mc,
                            "Конфиг перезагружен",
                            profiles.active() + ".cfg"
                        );
                    } else {
                        chatError(
                            mc,
                            errorOr(
                                profiles.lastError(),
                                "Не удалось перезагрузить конфиг"
                            )
                        );
                    }
                    return true;
                }

                default -> {
                    chatError(
                        mc,
                        "Неизвестная команда: .cfg " + action
                    );
                    chatMuted(
                        mc,
                        "Используй .cfg help"
                    );
                    return true;
                }
            }
        } catch (Throwable error) {
            chatError(
                mc,
                "Ошибка выполнения команды"
            );
            App.features()
                .find(HudFeature.class)
                .ifPresent(h -> h.notify(
                    "Config",
                    error.getClass().getSimpleName(),
                    HudFeature.NoteKind.BAD
                ));
            return true;
        }
    }

    private void printHelp(MinecraftClient mc) {
        chatHeader(
            mc,
            "CONFIG",
            "локальные конфиги клиента"
        );

        chatCommand(
            mc,
            ".cfg load <имя>",
            "загрузить конфиг"
        );
        chatCommand(
            mc,
            ".cfg dir",
            "открыть папку конфигов"
        );
        chatCommand(
            mc,
            ".cfg delete <имя>",
            "удалить конфиг"
        );
        chatCommand(
            mc,
            ".cfg list",
            "показать список конфигов"
        );
        chatCommand(
            mc,
            ".cfg reload",
            "перезагрузить активный конфиг"
        );
        chatCommand(
            mc,
            ".cfg save [имя]",
            "сохранить текущий конфиг"
        );
        chatCommand(
            mc,
            ".cfg help",
            "показать эту справку"
        );

        chatMuted(
            mc,
            "Все команды локальные и не отправляются на сервер."
        );
    }

    private void printConfigList(
        MinecraftClient mc,
        Profiles profiles,
        List<String> configs,
        String query
    ) {
        if (configs.isEmpty()) {
            chatHeader(
                mc,
                "CONFIG",
                query == null || query.isBlank()
                    ? "конфигов пока нет"
                    : "ничего не найдено: " + query
            );
            chatMuted(
                mc,
                "Используй .cfg new <имя> чтобы создать конфиг."
            );
            return;
        }

        chatHeader(
            mc,
            "CONFIGS",
            configs.size() + " шт."
        );

        String active = profiles.active();

        int shown = 0;
        for (String name : configs) {
            if (shown >= 18) {
                chatMuted(
                    mc,
                    "… ещё " + (configs.size() - shown) + " конфигов"
                );
                break;
            }

            Profiles.Metadata meta = profiles.metadata(name);
            boolean isActive = active.equalsIgnoreCase(name);

            String suffix = "";
            if (meta.tags() != null && !meta.tags().isBlank()) {
                suffix = "  ·  #" + meta.tags().trim().replace(" ", " #");
            }

            if (isActive) {
                chatSuccess(
                    mc,
                    "● " + name + ".cfg",
                    "активный" + suffix
                );
            } else {
                chatInfo(
                    mc,
                    "○ " + name + ".cfg",
                    suffix.isBlank() ? "" : suffix.substring(3)
                );
            }

            shown++;
        }
    }

    private static String cleanCommandName(String value) {
        if (value == null) {
            return "";
        }

        String result = value.trim();

        if (result.toLowerCase(Locale.ROOT).endsWith(".cfg")) {
            result = result.substring(0, result.length() - 4);
        }

        return result.trim();
    }

    private static String errorOr(
        String error,
        String fallback
    ) {
        return error == null || error.isBlank()
            ? fallback
            : error;
    }

    private static void chatHeader(
        MinecraftClient mc,
        String title,
        String subtitle
    ) {
        if (mc.player == null) {
            return;
        }

        MutableText message =
            Text.literal("[ ")
                .append(plain("DOORE", C_LAVENDER_BRIGHT))
                .append(plain(" / " + title + " ] ", C_MUTED))
                .append(plain(subtitle, C_MUTED));

        mc.player.sendMessage(message, false);
    }

    private static final int C_LAVENDER = 0xC9B8FF;
    private static final int C_LAVENDER_BRIGHT = 0xE7DFFF;
    private static final int C_LAVENDER_DIM = 0x9B8AC9;
    private static final int C_MUTED = 0x8A829B;
    private static final int C_GREEN = 0xA7E7C0;
    private static final int C_RED = 0xFF8FA8;
    private static final int C_YELLOW = 0xE8D89B;

    private static TextColor color(int rgb) {
        return TextColor.fromRgb(rgb);
    }

    private static MutableText plain(String text, int rgb) {
        return Text.literal(text).styled(style -> style.withColor(color(rgb)));
    }

    private static void chatCommand(
        MinecraftClient mc,
        String command,
        String description
    ) {
        if (mc.player == null) {
            return;
        }

        MutableText message =
            Text.literal("  ")
                .append(plain(command, C_LAVENDER_BRIGHT))
                .append(plain("  —  ", C_MUTED))
                .append(plain(description, C_MUTED));

        mc.player.sendMessage(message, false);
    }

    private static void chatSuccess(
        MinecraftClient mc,
        String title,
        String detail
    ) {
        sendStatus(
            mc,
            "✓",
            C_GREEN,
            title,
            detail
        );
    }

    private static void chatInfo(
        MinecraftClient mc,
        String title,
        String detail
    ) {
        sendStatus(
            mc,
            "•",
            C_LAVENDER,
            title,
            detail
        );
    }

    private static void chatError(
        MinecraftClient mc,
        String text
    ) {
        sendStatus(
            mc,
            "×",
            C_RED,
            "Ошибка",
            text
        );
    }

    private static void chatMuted(
        MinecraftClient mc,
        String text
    ) {
        if (mc.player == null) {
            return;
        }

        MutableText message =
            Text.literal("  ·  ")
                .append(plain(text, C_MUTED));

        mc.player.sendMessage(message, false);
    }

    private static void chatUsage(
        MinecraftClient mc,
        String usage
    ) {
        if (mc.player == null) {
            return;
        }

        MutableText message =
            Text.literal("  ↳ ")
                .append(plain(usage, C_YELLOW));

        mc.player.sendMessage(message, false);
    }

    private static void sendStatus(
        MinecraftClient mc,
        String icon,
        int iconColor,
        String title,
        String detail
    ) {
        if (mc.player == null) {
            return;
        }

        MutableText message =
            Text.literal("[ ")
                .append(plain(icon, iconColor))
                .append(plain(" ] ", C_MUTED))
                .append(plain(title, C_LAVENDER_BRIGHT));

        if (detail != null && !detail.isBlank()) {
            message = message.append(
                plain("  ·  " + detail, C_MUTED)
            );
        }

        mc.player.sendMessage(message, false);
    }

    private static List<String> tokenize(String raw) {
        List<String> list = new ArrayList<>();
        StringBuilder stringbuilder = new StringBuilder();
        boolean flag = false;

        for (int i = 0; i < raw.length(); i++) {
            char c0 = raw.charAt(i);
            if (c0 == '"') {
                flag = !flag;
            } else if (flag || !Character.isWhitespace(c0)) {
                stringbuilder.append(c0);
            } else if (!stringbuilder.isEmpty()) {
                list.add(stringbuilder.toString());
                stringbuilder.setLength(0);
            }
        }

        if (!stringbuilder.isEmpty()) {
            list.add(stringbuilder.toString());
        }

        return list;
    }

    private static String joinFrom(List<String> parts, int from) {
        StringBuilder stringbuilder = new StringBuilder();

        for (int i = from; i < parts.size(); i++) {
            if (!stringbuilder.isEmpty()) {
                stringbuilder.append(' ');
            }

            stringbuilder.append(parts.get(i));
        }

        return stringbuilder.toString();
    }

    private static void chat(MinecraftClient mc, String text) {
        if (mc.player != null) {
            mc.player.sendMessage(plain("[CFG] " + text, C_LAVENDER), false);
        }
    }

    private static void toast(String body) {
        App.features().find(HudFeature.class).ifPresent(h -> h.notify("\u041a\u043e\u043d\u0444\u0438\u0433", body));
    }
}
