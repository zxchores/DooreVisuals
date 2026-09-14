package dev.doorevisuals.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class ModelLoader {
    private ModelLoader() {
    }

    public static void ensureExample(Path root) {
        Path path = root.resolve("example");
        if (!Files.isDirectory(path) || !Files.isRegularFile(path.resolve("model.json"))) {
            try {
                Files.createDirectories(path);
                Files.writeString(
                    path.resolve("model.json"),
                    "{\n  \"minecraft:geometry\": [{\n    \"description\": {\"identifier\": \"geometry.example\", \"texture_width\": 64, \"texture_height\": 64},\n    \"bones\": [\n      {\"name\": \"body\", \"pivot\": [0, 24, 0], \"cubes\": [{\"origin\": [-4, 12, -2], \"size\": [8, 12, 4], \"uv\": [16, 16]}]},\n      {\"name\": \"head\", \"pivot\": [0, 24, 0], \"cubes\": [{\"origin\": [-4, 24, -4], \"size\": [8, 8, 8], \"uv\": [0, 0]}]},\n      {\"name\": \"rightArm\", \"pivot\": [-5, 22, 0], \"cubes\": [{\"origin\": [-8, 12, -2], \"size\": [4, 12, 4], \"uv\": [40, 16]}]},\n      {\"name\": \"leftArm\", \"pivot\": [5, 22, 0], \"cubes\": [{\"origin\": [4, 12, -2], \"size\": [4, 12, 4], \"uv\": [32, 48]}]},\n      {\"name\": \"rightLeg\", \"pivot\": [-2, 12, 0], \"cubes\": [{\"origin\": [-4, 0, -2], \"size\": [4, 12, 4], \"uv\": [0, 16]}]},\n      {\"name\": \"leftLeg\", \"pivot\": [2, 12, 0], \"cubes\": [{\"origin\": [0, 0, -2], \"size\": [4, 12, 4], \"uv\": [16, 48]}]}\n    ]\n  }]\n}\n"
                );
                Files.writeString(path.resolve("animation.json"), "{\n  \"idle\": true,\n  \"walk\": true\n}\n");
            } catch (Exception exception) {
            }
        }
    }

    public static ModelPack load(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return null;
        } else {
            Path path = dir.resolve("model.json");
            if (!Files.isRegularFile(path)) {
                return null;
            } else {
                try {
                    JsonObject jsonobject = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    float[] afloat = texSize(jsonobject);
                    List<ModelCube> list = cubes(jsonobject);
                    if (list.isEmpty()) {
                        return null;
                    } else {
                        Identifier identifier = texture(dir, dir.getFileName().toString());
                        boolean flag = false;
                        boolean flag1 = false;
                        Path path1 = dir.resolve("animation.json");
                        if (Files.isRegularFile(path1)) {
                            String s = Files.readString(path1).toLowerCase(Locale.ROOT);
                            flag = s.contains("idle") || s.contains("walk");
                            flag1 = s.contains("walk") || s.contains("run");
                        }

                        return new ModelPack(dir.getFileName().toString(), identifier, afloat[0], afloat[1], list, flag, flag1);
                    }
                } catch (Exception exception) {
                    return null;
                }
            }
        }
    }

    private static Identifier texture(Path dir, String id) {
        Path path = Files.isRegularFile(dir.resolve("texture.png")) ? dir.resolve("texture.png") : firstPng(dir);
        if (path == null) {
            return Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
        } else {
            try {
                InputStream inputstream = Files.newInputStream(path);

                Identifier identifier;
                try {
                    NativeImage nativeimage = NativeImage.read(inputstream);
                    Identifier identifier1 = Identifier.of("doorevisuals", "models/" + slug(id));
                    NativeImageBackedTexture nativeimagebackedtexture = new NativeImageBackedTexture(() -> "doore_model_" + slug(id), nativeimage);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier1, nativeimagebackedtexture);
                    identifier = identifier1;
                } catch (Throwable throwable) {
                    if (inputstream != null) {
                        try {
                            inputstream.close();
                        } catch (Throwable throwable1) {
                            throwable.addSuppressed(throwable1);
                        }
                    }

                    throw throwable;
                }

                if (inputstream != null) {
                    inputstream.close();
                }

                return identifier;
            } catch (Exception exception) {
                return Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
            }
        }
    }

    private static Path firstPng(Path dir) {
        try {
            return Files.list(dir).filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).findFirst().orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private static float[] texSize(JsonObject json) {
        if (json.has("minecraft:geometry") && json.get("minecraft:geometry").isJsonArray()) {
            JsonArray jsonarray = json.getAsJsonArray("minecraft:geometry");
            if (!jsonarray.isEmpty() && jsonarray.get(0).isJsonObject()) {
                JsonObject jsonobject = jsonarray.get(0).getAsJsonObject();
                if (jsonobject.has("description") && jsonobject.get("description").isJsonObject()) {
                    JsonObject jsonobject1 = jsonobject.getAsJsonObject("description");
                    return new float[]{num(jsonobject1, "texture_width", 64.0F), num(jsonobject1, "texture_height", 64.0F)};
                }
            }
        }

        if (json.has("textureSize") && json.get("textureSize").isJsonArray()) {
            JsonArray jsonarray1 = json.getAsJsonArray("textureSize");
            return new float[]{jsonarray1.size() > 0 ? jsonarray1.get(0).getAsFloat() : 64.0F, jsonarray1.size() > 1 ? jsonarray1.get(1).getAsFloat() : 64.0F};
        } else {
            return new float[]{num(json, "textureWidth", 64.0F), num(json, "textureHeight", 64.0F)};
        }
    }

    private static List<ModelCube> cubes(JsonObject json) {
        List<ModelCube> list = new ArrayList<>();
        if (json.has("minecraft:geometry") && json.get("minecraft:geometry").isJsonArray()) {
            for (JsonElement jsonelement : json.getAsJsonArray("minecraft:geometry")) {
                if (jsonelement.isJsonObject() && jsonelement.getAsJsonObject().has("bones")) {
                    bones(jsonelement.getAsJsonObject().getAsJsonArray("bones"), list);
                }
            }
        } else if (json.has("bones") && json.get("bones").isJsonArray()) {
            bones(json.getAsJsonArray("bones"), list);
        } else if (json.has("elements") && json.get("elements").isJsonArray()) {
            elements(json.getAsJsonArray("elements"), list);
        } else if (json.has("parts") && json.get("parts").isJsonObject()) {
            for (var entry : json.getAsJsonObject("parts").entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    bone(entry.getKey(), entry.getValue().getAsJsonObject(), new float[3], list);
                }
            }
        }

        return list;
    }

    private static void bones(JsonArray bones, List<ModelCube> out) {
        for (JsonElement jsonelement : bones) {
            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                String s = jsonobject.has("name") ? jsonobject.get("name").getAsString() : "bone";
                float[] afloat = vec(jsonobject, "pivot");
                bone(s, jsonobject, afloat, out);
            }
        }
    }

    private static void bone(String name, JsonObject bone, float[] pivot, List<ModelCube> out) {
        if (bone.has("cubes") && bone.get("cubes").isJsonArray()) {
            for (JsonElement jsonelement : bone.getAsJsonArray("cubes")) {
                if (jsonelement.isJsonObject()) {
                    JsonObject jsonobject = jsonelement.getAsJsonObject();
                    float[] afloat = vec(jsonobject, "origin");
                    float[] afloat1 = vec(jsonobject, "size");
                    float[] afloat2 = uv(jsonobject);
                    if (afloat1[0] > 0.0F || afloat1[1] > 0.0F || afloat1[2] > 0.0F) {
                        out.add(new ModelCube(name, afloat[0], afloat[1], afloat[2], afloat1[0], afloat1[1], afloat1[2], afloat2[0], afloat2[1], pivot[0], pivot[1], pivot[2]));
                    }
                }
            }
        }
    }

    private static void elements(JsonArray elements, List<ModelCube> out) {
        for (JsonElement jsonelement : elements) {
            if (jsonelement.isJsonObject()) {
                JsonObject jsonobject = jsonelement.getAsJsonObject();
                float[] afloat = vec(jsonobject, "from");
                float[] afloat1 = vec(jsonobject, "to");
                float[] afloat2 = uv(jsonobject);
                out.add(
                    new ModelCube(
                        "elem",
                        afloat[0],
                        afloat[1],
                        afloat[2],
                        Math.max(0.1F, afloat1[0] - afloat[0]),
                        Math.max(0.1F, afloat1[1] - afloat[1]),
                        Math.max(0.1F, afloat1[2] - afloat[2]),
                        afloat2[0],
                        afloat2[1],
                        0.0F,
                        0.0F,
                        0.0F
                    )
                );
            }
        }
    }

    private static float[] vec(JsonObject json, String key) {
        float[] afloat = new float[3];
        if (json.has(key) && json.get(key).isJsonArray()) {
            JsonArray jsonarray = json.getAsJsonArray(key);

            for (int i = 0; i < 3 && i < jsonarray.size(); i++) {
                afloat[i] = jsonarray.get(i).getAsFloat();
            }
        }

        return afloat;
    }

    private static float[] uv(JsonObject json) {
        if (json.has("uv") && json.get("uv").isJsonArray()) {
            JsonArray jsonarray = json.getAsJsonArray("uv");
            return new float[]{jsonarray.size() > 0 ? jsonarray.get(0).getAsFloat() : 0.0F, jsonarray.size() > 1 ? jsonarray.get(1).getAsFloat() : 0.0F};
        } else {
            return new float[2];
        }
    }

    private static float num(JsonObject json, String key, float fallback) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsFloat() : fallback;
    }

    private static String slug(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        return s.isBlank() ? "model" : s;
    }
}
