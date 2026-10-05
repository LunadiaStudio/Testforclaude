package com.seafle.abomination.client.mesh;
import com.seafle.abomination.Abomination;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
public final class CoreMeshes {
    private CoreMeshes() {}
    private static final Map<String, GpuMesh> READY = new HashMap<>();
    private static final Map<String, AbomMesh.Chunk> CPU = new HashMap<>();
    private static final Map<String, AbomMesh> FULL = new HashMap<>();
    private static final ConcurrentLinkedQueue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean BAKING = new AtomicBoolean(false);
    private record Pending(String id, BufferBuilder.BuiltBuffer built, int vertexCount,
                           AbomMesh.Chunk chunk, boolean last) {}
    private static final int BYTES_PER_VERTEX = 36;
    private static final int BAKED_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    public static void prebake(String[] ids) {
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
        Util.getMainWorkerExecutor().execute(() -> {
            for (String id : ids) {
                if (CPU.containsKey(id)) {
                    continue;
                }
                try {
                    prepare(resources, id);
                } catch (Exception e) {
        }
            }
        });
    }
    private static void prepare(ResourceManager resources, String id) throws Exception {
        Identifier path = new Identifier(Abomination.MOD_ID, "mesh/" + id + ".abmesh");
        Optional<net.minecraft.resource.Resource> res = resources.getResource(path);
        if (res.isEmpty()) {
            throw new IllegalStateException("메시 리소스가 없다: " + path);
        }
        AbomMesh mesh;
        try (InputStream in = res.get().getInputStream()) {
            mesh = AbomMeshLoader.read(in);
        }
        CPU.put(id, mesh.chunks.get(0));
        FULL.put(id, mesh);
        for (int i = 0; i < mesh.chunks.size(); i++) {
            AbomMesh.Chunk chunk = mesh.chunks.get(i);
            int vertexCount = chunk.indices.length;
            BufferBuilder bb = new BufferBuilder(vertexCount * 6);
            bb.begin(VertexFormat.DrawMode.TRIANGLES,
                    VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            float[] v = chunk.verts;
            for (int idx : chunk.indices) {
                int o = idx * AbomMeshLoader.FLOATS_PER_VERTEX;
                bb.vertex(v[o], v[o + 1], v[o + 2],
                        1.0f, 1.0f, 1.0f, 1.0f,
                        v[o + 6], v[o + 7],
                        OverlayTexture.DEFAULT_UV, BAKED_LIGHT,
                        v[o + 3], v[o + 4], v[o + 5]);
            }
            QUEUE.add(new Pending(id, bb.end(), vertexCount, chunk,
                    i == mesh.chunks.size() - 1));
        }
    }
    private static final Map<String, List<GpuMesh.Part>> BUILDING = new HashMap<>();
    private static final Map<String, int[]> BUILDING_STATS = new HashMap<>();
    public static void tick() {
        Pending p;
        while ((p = QUEUE.poll()) != null) {
            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(p.built());
            VertexBuffer.unbind();
            AbomMesh.Chunk c = p.chunk();
            BUILDING.computeIfAbsent(p.id(), k -> new ArrayList<>())
                    .add(new GpuMesh.Part(vb, c.pivotX, c.pivotY, c.pivotZ,
                            c.kind, c.safeDegrees));
            int[] st = BUILDING_STATS.computeIfAbsent(p.id(), k -> new int[1]);
            st[0] += p.vertexCount();
            if (p.last()) {
                List<GpuMesh.Part> parts = BUILDING.remove(p.id());
                int n = BUILDING_STATS.remove(p.id())[0];
                READY.put(p.id(), new GpuMesh(parts, n / 3,
                        (long) n * BYTES_PER_VERTEX));
            }
        }
    }
    public static GpuMesh get(String id) {
        return READY.get(id);
    }
    public static AbomMesh.Chunk cpu(String id) {
        return CPU.get(id);
    }
    public static AbomMesh full(String id) {
        return FULL.get(id);
    }
    public static java.util.Set<String> loadedIds() {
        return CPU.keySet();
    }
    public static void discard() {
        for (GpuMesh m : READY.values()) {
            m.close();
        }
        READY.clear();
        CPU.clear();
        for (Pending p : QUEUE) {
            p.built().release();
        }
        QUEUE.clear();
        BAKING.set(false);
    }
}
