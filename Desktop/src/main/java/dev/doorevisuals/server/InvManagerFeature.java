package dev.doorevisuals.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.tools.AuctionLore;
import dev.doorevisuals.tools.InvClicks;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

public final class InvManagerFeature extends Feature implements Tick {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Opt.Pick preset = this.opt(new Opt.Pick("preset", "Пресет", "pvp", "pvp", "farm"));
    private final Opt.Key apply = this.opt(new Opt.Key("apply", "Применить", -1));
    private final Opt.Key save = this.opt(new Opt.Key("save", "Сохранить раскладку", -1));
    private final Opt.Key search = this.opt(new Opt.Key("ah_search", "AH поиск из руки", -1));
    private final Opt.Flag auto = this.opt(new Opt.Flag("auto", "Автораскладка", false));
    private boolean applyWas;
    private boolean saveWas;
    private boolean searchWas;
    private int autoCool;

    public InvManagerFeature() {
        super("inv_manager", "Inventory Manager", "Пресеты хотбара и автораскладка меча/сферы/еды", Category.TOOLS, false);
        this.refreshPresets();
    }

    @Override
    public void tick(MinecraftClient mc) {
        if (this.on() && mc.player != null) {
            this.poll(mc, this.apply, this.applyWas, v -> this.applyWas = v, () -> this.applyPreset(mc));
            this.poll(mc, this.save, this.saveWas, v -> this.saveWas = v, () -> this.savePreset(mc));
            this.poll(mc, this.search, this.searchWas, v -> this.searchWas = v, () -> AhHelperFeature.searchHeld(mc));
            if ((Boolean)this.auto.get() && mc.currentScreen == null && !InvClicks.busy()) {
                if (this.autoCool > 0) {
                    this.autoCool--;
                } else {
                    this.autoLayout(mc);
                    this.autoCool = 40;
                }
            }
        }
    }

    private void applyPreset(MinecraftClient mc) {
        if (mc.player != null && !InvClicks.busy()) {
            List<String> list = this.load((String)this.preset.get());
            if (!list.isEmpty()) {
                PlayerScreenHandler playerscreenhandler = mc.player.playerScreenHandler;

                for (int i = 0; i < 9 && i < list.size(); i++) {
                    String s = list.get(i);
                    if (s != null && !s.isBlank()) {
                        int j = 36 + i;
                        int k = this.find(playerscreenhandler, s, j);
                        if (k >= 0 && k != j) {
                            InvClicks.swap(0, k, j);
                        }
                    }
                }
            }
        }
    }

    private void savePreset(MinecraftClient mc) {
        if (mc.player != null) {
            JsonArray jsonarray = new JsonArray();
            PlayerScreenHandler playerscreenhandler = mc.player.playerScreenHandler;

            for (int i = 0; i < 9; i++) {
                ItemStack itemstack = ((Slot)playerscreenhandler.slots.get(36 + i)).getStack();
                jsonarray.add(AuctionLore.displayName(itemstack));
            }

            JsonObject jsonobject = this.root();
            jsonobject.add((String)this.preset.get(), jsonarray);
            this.write(jsonobject);
            this.refreshPresets();
        }
    }

    private void autoLayout(MinecraftClient mc) {
        PlayerScreenHandler playerscreenhandler = mc.player.playerScreenHandler;
        this.place(playerscreenhandler, 36, "меч", "sword", "клинок");
        this.place(playerscreenhandler, 37, "сфера", "шар", "orb");
        this.place(playerscreenhandler, 44, "яблок", "еда", "apple", "gapple", "хлеб", "стейк");
    }

    private void place(PlayerScreenHandler menu, int dest, String... keys) {
        ItemStack itemstack = ((Slot)menu.slots.get(dest)).getStack();
        if (!AuctionLore.nameHas(itemstack, keys)) {
            int i = this.find(menu, String.join(",", keys), dest);
            if (i >= 0) {
                InvClicks.swap(0, i, dest);
            }
        }
    }

    private int find(PlayerScreenHandler menu, String filter, int skip) {
        String[] astring = filter.toLowerCase(Locale.ROOT).split("[,;]");

        for (int i = 9; i < menu.slots.size(); i++) {
            if (i != skip && AuctionLore.nameHas(((Slot)menu.slots.get(i)).getStack(), astring)) {
                return i;
            }
        }

        return -1;
    }

    private List<String> load(String id) {
        List<String> list = new ArrayList<>();
        JsonObject jsonobject = this.root();
        if (jsonobject.has(id) && jsonobject.get(id).isJsonArray()) {
            JsonArray jsonarray = jsonobject.getAsJsonArray(id);

            for (int i = 0; i < jsonarray.size(); i++) {
                list.add(jsonarray.get(i).getAsString());
            }
        }

        return list;
    }

    private JsonObject root() {
        Path path = file();
        if (Files.isRegularFile(path)) {
            try {
                JsonObject jsonobject = (JsonObject)GSON.fromJson(Files.readString(path), JsonObject.class);
                if (jsonobject != null) {
                    return jsonobject;
                }
            } catch (Exception exception) {
            }
        }

        JsonObject jsonobject1 = new JsonObject();
        jsonobject1.add("pvp", defaults("меч", "сфера", "", "", "", "", "", "", "яблоко"));
        jsonobject1.add("farm", defaults("кирка", "лопата", "", "", "", "", "", "", "еда"));
        return jsonobject1;
    }

    private static JsonArray defaults(String... names) {
        JsonArray jsonarray = new JsonArray();

        for (String s : names) {
            jsonarray.add(s);
        }

        return jsonarray;
    }

    private void write(JsonObject json) {
        try {
            Files.writeString(file(), GSON.toJson(json));
        } catch (Exception exception) {
        }
    }

    private void refreshPresets() {
        JsonObject jsonobject = this.root();
        List<String> list = new ArrayList<>();

        for (String s : jsonobject.keySet()) {
            list.add(s);
        }

        if (!list.isEmpty()) {
            this.preset.replaceOptions(list);
        }
    }

    private static Path file() {
        return ClientPaths.invPresets();
    }

    private void poll(MinecraftClient mc, Opt.Key key, boolean was, java.util.function.Consumer<Boolean> setWas, Runnable run) {
        int i = (Integer)key.get();
        if (i > 0 && i != -1 && mc.getWindow() != null) {
            boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
            if (flag && !was && !InvClicks.busy()) {
                run.run();
            }

            setWas.accept(flag);
        } else {
            setWas.accept(false);
        }
    }
}
