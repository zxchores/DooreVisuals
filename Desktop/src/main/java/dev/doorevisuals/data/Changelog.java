package dev.doorevisuals.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class Changelog {
    private Changelog() {
    }

    public static List<Changelog.Entry> load() {
        List<Changelog.Entry> list = new ArrayList<>();

        try {
            Identifier identifier = Identifier.of("doorevisuals", "changelog.json");
            Optional<Resource> optional = MinecraftClient.getInstance().getResourceManager().getResource(identifier);
            if (optional.isEmpty()) {
                return list;
            } else {
                Object object;
                try (BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(optional.get().getInputStream(), StandardCharsets.UTF_8))) {
                    JsonObject jsonobject = JsonParser.parseReader(bufferedreader).getAsJsonObject();
                    JsonArray jsonarray = jsonobject.getAsJsonArray("entries");
                    if (jsonarray != null) {
                        for (JsonElement jsonelement : jsonarray) {
                            JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                            String s = jsonobject1.has("version") ? jsonobject1.get("version").getAsString() : "";
                            String s1 = jsonobject1.has("title") ? jsonobject1.get("title").getAsString() : s;
                            List<String> list1 = new ArrayList<>();
                            if (jsonobject1.has("bullets") && jsonobject1.get("bullets").isJsonArray()) {
                                for (JsonElement jsonelement1 : jsonobject1.getAsJsonArray("bullets")) {
                                    list1.add(jsonelement1.getAsString());
                                }
                            }

                            list.add(new Changelog.Entry(s, s1, list1));
                        }

                        return list;
                    }

                    object = list;
                }

                return (List<Changelog.Entry>)object;
            }
        } catch (Exception exception) {
            return list;
        }
    }

    public static boolean isCurrent(String version) {
        return "3.31.0".equals(version);
    }

    public record Entry(String version, String title, List<String> bullets) {
    }
}
