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
import net.minecraft.text.Text;

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
        if (message != null && message.toLowerCase(Locale.ROOT).startsWith(".cfg")) {
            List<String> list = tokenize(message.trim());
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            if (list.size() != 1 && !list.get(1).equalsIgnoreCase("help")) {
                String s = list.get(1).toLowerCase(Locale.ROOT);
                String s1 = list.size() >= 3 ? joinFrom(list, 2) : "";
                if (!App.live()) {
                    chat(minecraftclient, "\u043a\u043b\u0438\u0435\u043d\u0442 \u0435\u0449\u0451 \u043d\u0435 \u0433\u043e\u0442\u043e\u0432");
                    return true;
                } else {
                    Profiles profiles = App.save().profiles();
                    switch (s) {
                        case "load":
                            if (s1.isBlank()) {
                                chat(minecraftclient, ".cfg load <\u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435>");
                                return true;
                            }

                            boolean flag3 = this.applyNamed(s1);
                            chat(minecraftclient, flag3 ? "load \u2192 " + profiles.active() : "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + s1);
                            break;
                        case "save":
                            String s2 = s1.isBlank() ? profiles.active() : s1;
                            this.setDraftName(s2);
                            boolean flag1 = this.saveDraft();
                            chat(minecraftclient, flag1 ? "save \u2192 " + profiles.active() + ".cfg" : profiles.lastError());
                            break;
                        case "dir":
                            boolean flag2 = s1.isBlank() ? this.openConfigsFolder() : this.openNamedFolder(s1);
                            chat(
                                minecraftclient,
                                flag2
                                    ? "\u043f\u0430\u043f\u043a\u0430: "
                                        + Folders.pretty(profiles.dir())
                                        + "  \u2014 \u0441\u044e\u0434\u0430 \u043c\u043e\u0436\u043d\u043e \u043a\u0438\u043d\u0443\u0442\u044c .cfg"
                                    : "\u043d\u0435 \u043e\u0442\u043a\u0440\u044b\u043b\u043e\u0441\u044c"
                            );
                            break;
                        case "list":
                            List<String> list1 = profiles.list();
                            if (list1.isEmpty()) {
                                chat(
                                    minecraftclient,
                                    "\u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432 \u043d\u0435\u0442  \u00b7  .cfg dir \u0447\u0442\u043e\u0431\u044b \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0430\u043f\u043a\u0443"
                                );
                            } else {
                                chat(minecraftclient, "\u043a\u043e\u043d\u0444\u0438\u0433\u0438: " + String.join(", ", list1));
                            }
                            break;
                        case "del":
                        case "delete":
                        case "rm":
                            if (s1.isBlank()) {
                                chat(minecraftclient, ".cfg del <\u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435>");
                                return true;
                            }

                            boolean flag = this.deleteNamed(s1);
                            chat(minecraftclient, flag ? "del \u2192 " + s1 : profiles.lastError());
                            break;
                        default:
                            chat(
                                minecraftclient,
                                "\u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u0430\u044f \u043a\u043e\u043c\u0430\u043d\u0434\u0430. .cfg help"
                            );
                            chat(minecraftclient, "load / save / list / dir / del");
                    }

                    return true;
                }
            } else {
                chat(
                    minecraftclient,
                    ".cfg load <\u0438\u043c\u044f>  \u2014 \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433"
                );
                chat(
                    minecraftclient,
                    ".cfg save [\u0438\u043c\u044f]  \u2014 \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0442\u0435\u043a\u0443\u0449\u0438\u0439"
                );
                chat(minecraftclient, ".cfg list        \u2014 \u0441\u043f\u0438\u0441\u043e\u043a \u043a\u043e\u043d\u0444\u0438\u0433\u043e\u0432");
                chat(minecraftclient, ".cfg dir         \u2014 \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0430\u043f\u043a\u0443 configs");
                chat(minecraftclient, ".cfg del <\u0438\u043c\u044f>   \u2014 \u0443\u0434\u0430\u043b\u0438\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433");
                return true;
            }
        } else {
            return false;
        }
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
            mc.player.sendMessage(Text.literal("\u00a73[CFG]\u00a7r " + text), false);
        }
    }

    private static void toast(String body) {
        App.features().find(HudFeature.class).ifPresent(h -> h.notify("\u041a\u043e\u043d\u0444\u0438\u0433", body));
    }
}
