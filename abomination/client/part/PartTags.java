package com.seafle.abomination.client.part;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.AbomMesh;
import net.fabricmc.loader.api.FabricLoader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
public final class PartTags {
    private PartTags() {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "abomination_parts.json";
    private static final Map<Integer, AbomMesh.Kind> OVERRIDES = new HashMap<>();
    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }
    public static void load() {
        Path p = path();
        if (!Files.exists(p)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject o = GSON.fromJson(r, JsonObject.class);
            if (o == null) {
                return;
            }
            OVERRIDES.clear();
            for (String key : o.keySet()) {
                try {
                    OVERRIDES.put(Integer.parseInt(key),
                            AbomMesh.Kind.valueOf(o.get(key).getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
        }
    }
    public static AbomMesh.Kind kindOf(int partIndex, AbomMesh.Kind auto) {
        return OVERRIDES.getOrDefault(partIndex, auto);
    }
}
