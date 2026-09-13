package dev.doorevisuals.data;

import com.github.noamm9.nvgrenderer.nvg.Image;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.doorevisuals.draw.Nvg;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Account {
    private static final Logger LOG = LoggerFactory.getLogger("DooreVisuals/Account");
    private static final String OWNER_HWID = "2a5b8e64298a03fbdba06d5aaa8387fb";
    private static volatile String uid = "000000";
    private static volatile String username = "";
    private static volatile String avatarPath = "";
    private static volatile String hwid = "";
    private static volatile boolean ready;
    private static Image avatarNvg;
    private static String avatarNvgPath = "";

    private Account() {
    }

    public static synchronized void ensure() {
        if (!ready) {
            ready = true;
            Path path = ClientPaths.accountFile();
            if (!Files.isRegularFile(path)) {
                LOG.info("No launcher account");
            } else {
                try {
                    JsonObject jsonobject = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    if (jsonobject.has("username") && jsonobject.get("username").isJsonPrimitive()) {
                        username = jsonobject.get("username").getAsString();
                    }

                    if (jsonobject.has("uid") && jsonobject.get("uid").isJsonPrimitive()) {
                        String s = jsonobject.get("uid").getAsString();
                        if (s != null && s.matches("\\d{6}")) {
                            uid = s;
                        }
                    }

                    if (jsonobject.has("avatar") && jsonobject.get("avatar").isJsonPrimitive()) {
                        avatarPath = jsonobject.get("avatar").getAsString();
                    }

                    if (jsonobject.has("hwid") && jsonobject.get("hwid").isJsonPrimitive()) {
                        hwid = jsonobject.get("hwid").getAsString();
                    }
                } catch (Exception exception) {
                    LOG.debug("account read: {}", exception.toString());
                }

                LOG.info("Account {} #{} role={}", new Object[]{nick(), uid, roleId()});
            }
        }
    }

    public static String uid() {
        ensure();
        return uid;
    }

    public static boolean owner() {
        ensure();
        return "000001".equals(uid) && "2a5b8e64298a03fbdba06d5aaa8387fb".equalsIgnoreCase(hwid);
    }

    public static String nick() {
        ensure();
        if (username != null && !username.isBlank()) {
            return username;
        } else {
            try {
                MinecraftClient minecraftclient = MinecraftClient.getInstance();
                if (minecraftclient != null && minecraftclient.getSession() != null && minecraftclient.getSession().getUsername() != null) {
                    return minecraftclient.getSession().getUsername();
                }
            } catch (Throwable throwable) {
            }

            return "player";
        }
    }

    public static String roleId() {
        ensure();
        return owner() ? "developer" : Privileges.roleOf(uid);
    }

    public static String roleLabel() {
        return Privileges.labelOf(roleId());
    }

    public static String planLabel() {
        String s = roleId();

        return switch (s) {
            case "developer" -> "Doore";
            case "user" -> "Free";
            default -> Privileges.labelOf(roleId());
        };
    }

    public static boolean hasAvatar() {
        return avatarImage() != null;
    }

    public static Image avatarImage() {
        ensure();
        if (avatarPath == null || avatarPath.isBlank() || !Files.isRegularFile(Path.of(avatarPath))) {
            return null;
        } else if (avatarPath.equals(avatarNvgPath)) {
            return avatarNvg;
        } else {
            if (avatarNvg != null) {
                Nvg.deleteImage(avatarNvg);
                avatarNvg = null;
            }

            avatarNvg = Nvg.createImage(avatarPath);
            avatarNvgPath = avatarPath;
            return avatarNvg;
        }
    }

    public static boolean privileged() {
        String s = roleId();
        return s != null && !s.isBlank() && !"user".equals(s);
    }
}
