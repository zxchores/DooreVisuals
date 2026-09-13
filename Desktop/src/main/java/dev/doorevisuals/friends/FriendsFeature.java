package dev.doorevisuals.friends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.doorevisuals.App;
import dev.doorevisuals.core.Category;
import dev.doorevisuals.core.Feature;
import dev.doorevisuals.core.Opt;
import dev.doorevisuals.core.Tick;
import dev.doorevisuals.data.ClientPaths;
import dev.doorevisuals.draw.Mesh;
import dev.doorevisuals.draw.Types;
import dev.doorevisuals.overlay.HudFeature;
import dev.doorevisuals.world.WorldVis;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class FriendsFeature extends Feature implements Tick {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile FriendsFeature live;
    private final Opt.Key addKey = this.opt(
        new Opt.Key("add", "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043f\u043e\u0434 \u043f\u0440\u0438\u0446\u0435\u043b\u043e\u043c", 46)
    );
    private final Opt.Flag badges = this.opt(new Opt.Flag("badges", "\u0411\u0435\u0439\u0434\u0436 \u0434\u0440\u0443\u0437\u0435\u0439", true));
    private final Opt.Flag tabGlass = this.opt(new Opt.Flag("tab_glass", "\u0421\u0442\u0435\u043a\u043b\u043e Tab", true));
    private final Opt.Flag pingBar = this.opt(new Opt.Flag("ping_bar", "\u041f\u043e\u043b\u043e\u0441\u043a\u0430 \u043f\u0438\u043d\u0433\u0430", true));
    private final Opt.Tint color = this.opt(new Opt.Tint("color", "\u0426\u0432\u0435\u0442 \u043d\u0438\u043a\u0430", -8458284));
    private final Opt.Flag worldMark = this.opt(new Opt.Flag("world_mark", "\u041c\u0435\u0442\u043a\u0430 \u0432 \u043c\u0438\u0440\u0435", true));
    private final Map<UUID, String> friends = new LinkedHashMap<>();
    private volatile List<FriendsFeature.Mark> marks = List.of();
    private boolean addWasDown;

    public FriendsFeature() {
        super(
            "friends",
            "Friends",
            "\u0421\u043f\u0438\u0441\u043e\u043a, \u0446\u0432\u0435\u0442 \u043d\u0438\u043a\u0430, \u0441\u0442\u0435\u043a\u043b\u043e Tab \u0438 \u043f\u043e\u043b\u043e\u0441\u043a\u0430 \u043f\u0438\u043d\u0433\u0430",
            Category.FRIENDS,
            true
        );
        live = this;
        this.load();
    }

    public static boolean isFriend(UUID id) {
        FriendsFeature friendsfeature = live;
        return friendsfeature != null && friendsfeature.on() && id != null && friendsfeature.friends.containsKey(id);
    }

    public static boolean showBadge() {
        FriendsFeature friendsfeature = live;
        return friendsfeature != null && friendsfeature.on() && (Boolean)friendsfeature.badges.get();
    }

    public static int nickColor() {
        FriendsFeature friendsfeature = live;
        return friendsfeature == null ? -8458284 : (Integer)friendsfeature.color.get();
    }

    public static boolean worldMark() {
        FriendsFeature friendsfeature = live;
        return friendsfeature != null && friendsfeature.on() && (Boolean)friendsfeature.worldMark.get();
    }

    public static boolean tabGlass() {
        FriendsFeature friendsfeature = live;
        return friendsfeature == null || (Boolean)friendsfeature.tabGlass.get();
    }

    public static boolean pingBar() {
        FriendsFeature friendsfeature = live;
        return friendsfeature == null || (Boolean)friendsfeature.pingBar.get();
    }

    public Map<UUID, String> friends() {
        return Map.copyOf(this.friends);
    }

    public void remove(UUID id) {
        if (id != null) {
            this.friends.remove(id);
            this.save();
            this.poke();
        }
    }

    public void addLooked() {
        MinecraftClient minecraftclient = MinecraftClient.getInstance();
        if (minecraftclient.player != null) {
            if (App.aim().get() instanceof PlayerEntity playerentity && playerentity != minecraftclient.player) {
                UUID uuid = playerentity.getUuid();
                if (this.friends.containsKey(uuid)) {
                    this.friends.remove(uuid);
                    this.toast("\u0414\u0440\u0443\u0433 \u0443\u0431\u0440\u0430\u043d", playerentity.getName().getString());
                } else {
                    this.friends.put(uuid, playerentity.getName().getString());
                    this.toast("\u0414\u0440\u0443\u0433 \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d", playerentity.getName().getString());
                }

                this.save();
                this.poke();
            }
        }
    }

    @Override
    public void tick(MinecraftClient mc) {
        live = this;
        if (this.on() && mc.player != null && mc.currentScreen == null) {
            boolean flag = GLFW.glfwGetKey(mc.getWindow().getHandle(), (Integer)this.addKey.get()) == 1;
            if (flag && !this.addWasDown) {
                this.addLooked();
            }

            this.addWasDown = flag;
        } else {
            this.addWasDown = false;
        }
    }

    private void toast(String title, String body) {
        App.features().find(HudFeature.class).ifPresent(h -> h.notify(title, body, HudFeature.NoteKind.OK));
        App.features().find(HudFeature.class).ifPresent(h -> h.islandEvent(title, body));
    }

    private Path file() {
        return ClientPaths.root().resolve("friends.json");
    }

    private void load() {
        this.friends.clear();
        Path path = this.file();
        if (Files.isRegularFile(path)) {
            try {
                JsonObject jsonobject = (JsonObject)GSON.fromJson(Files.readString(path), JsonObject.class);
                if (jsonobject == null || !jsonobject.has("friends")) {
                    return;
                }

                JsonArray jsonarray = jsonobject.getAsJsonArray("friends");

                for (int i = 0; i < jsonarray.size(); i++) {
                    JsonObject jsonobject1 = jsonarray.get(i).getAsJsonObject();
                    UUID uuid = UUID.fromString(jsonobject1.get("id").getAsString());
                    String s = jsonobject1.has("name") ? jsonobject1.get("name").getAsString() : uuid.toString();
                    this.friends.put(uuid, s);
                }
            } catch (Throwable throwable) {
            }
        }
    }

    private void save() {
        try {
            JsonObject jsonobject = new JsonObject();
            JsonArray jsonarray = new JsonArray();

            for (Entry<UUID, String> entry : this.friends.entrySet()) {
                JsonObject jsonobject1 = new JsonObject();
                jsonobject1.addProperty("id", entry.getKey().toString());
                jsonobject1.addProperty("name", entry.getValue());
                jsonarray.add(jsonobject1);
            }

            jsonobject.add("friends", jsonarray);
            Files.writeString(this.file(), GSON.toJson(jsonobject));
        } catch (Throwable throwable) {
        }
    }

    public void extract(float td) {
        if (this.on() && (Boolean)this.worldMark.get()) {
            MinecraftClient minecraftclient = MinecraftClient.getInstance();
            ClientPlayerEntity clientplayerentity = minecraftclient.player;
            if (clientplayerentity != null && minecraftclient.world != null) {
                int i = (Integer)this.color.get();
                List<FriendsFeature.Mark> list = new ArrayList<>();

                for (PlayerEntity playerentity : minecraftclient.world.getPlayers()) {
                    if (playerentity != clientplayerentity
                        && isFriend(playerentity.getUuid())
                        && playerentity.isAlive()
                        && WorldVis.inFrustumRange(clientplayerentity, playerentity, 6400.0)) {
                        double d0 = MathHelper.lerp(td, playerentity.lastX, playerentity.getX());
                        double d1 = MathHelper.lerp(td, playerentity.lastY, playerentity.getY());
                        double d2 = MathHelper.lerp(td, playerentity.lastZ, playerentity.getZ());
                        list.add(new FriendsFeature.Mark(d0, d1, d2, i));
                        if (list.size() >= 24) {
                            break;
                        }
                    }
                }

                this.marks = list;
            } else {
                this.marks = List.of();
            }
        } else {
            this.marks = List.of();
        }
    }

    public void draw(WorldRenderContext ctx) {
        for (FriendsFeature.Mark friendsfeature$mark : this.marks) {
            int i = Mesh.alpha(friendsfeature$mark.color, 0.55F);
            int j = Mesh.alpha(friendsfeature$mark.color, 0.22F);
            Mesh.disc(ctx, friendsfeature$mark.x, friendsfeature$mark.y + 0.04, friendsfeature$mark.z, 0.22, 0.55, 28, i, Types.holo());
            Mesh.ring(ctx, friendsfeature$mark.x, friendsfeature$mark.y + 0.05, friendsfeature$mark.z, 0.58, 24, i);
            Mesh.cylinder(ctx, friendsfeature$mark.x, friendsfeature$mark.y + 0.02, friendsfeature$mark.z, 0.12, 1.55F, 16, j, Types.holo());
        }
    }

    private record Mark(double x, double y, double z, int color) {
    }
}
