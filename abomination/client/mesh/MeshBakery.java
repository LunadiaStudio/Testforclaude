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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
public final class MeshBakery {
    private MeshBakery() {}
    public enum Lod {
        LOD0("abomination_lod0", 48.0),
        LOD1("abomination_lod1", Double.MAX_VALUE);
        public final String file;
        public final double maxDistance;
        Lod(String file, double maxDistance) {
            this.file = file;
            this.maxDistance = maxDistance;
        }
    }
    private static final Lod[] BAKE_ORDER = { Lod.LOD1, Lod.LOD0 };
    private static final double LOD_HYSTERESIS = 8.0;
    private static final int BYTES_PER_VERTEX = 36;
    private static final int UPLOADS_PER_TICK = 4;
    private static final int BAKED_LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
    private static final Map<Lod, GpuMesh> READY = new EnumMap<>(Lod.class);
    private static final ConcurrentLinkedQueue<Pending> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean BAKING = new AtomicBoolean(false);
    private record Pending(Lod lod, BufferBuilder.BuiltBuffer built, int vertexCount,
                           float pivotX, float pivotY, float pivotZ,
                           AbomMesh.Kind kind, float safeDegrees, boolean last) {}
    private static final Map<Lod, List<GpuMesh.Part>> BUILDING = new EnumMap<>(Lod.class);
    private static final Map<Lod, int[]> BUILDING_STATS = new EnumMap<>(Lod.class);
    private static long bakeStartMs;
    public static void prebake() {
        if (!READY.isEmpty() || !BAKING.compareAndSet(false, true)) {
            return;
        }
        bakeStartMs = System.currentTimeMillis();
        ResourceManager resources = MinecraftClient.getInstance().getResourceManager();
        Util.getMainWorkerExecutor().execute(() -> {
            try {
                for (Lod lod : BAKE_ORDER) {
                    prepare(resources, lod);
                }
            } catch (Throwable t) {
                BAKING.set(false);
            }
        });
    }
    private static void prepare(ResourceManager resources, Lod lod) throws Exception {
        Identifier id = new Identifier(Abomination.MOD_ID, "mesh/" + lod.file + ".abmesh");
        Optional<net.minecraft.resource.Resource> res = resources.getResource(id);
        if (res.isEmpty()) {
            throw new IllegalStateException("메시 리소스가 없다: " + id);
        }
        AbomMesh mesh;
        try (InputStream in = res.get().getInputStream()) {
            mesh = AbomMeshLoader.read(in);
        }
        int n = mesh.chunks.size();
        for (int i = 0; i < n; i++) {
            AbomMesh.Chunk chunk = mesh.chunks.get(i);
            int vertexCount = chunk.indices.length;
            BufferBuilder bb = new BufferBuilder(vertexCount * 6);
            bb.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            float[] v = chunk.verts;
            for (int idx : chunk.indices) {
                int o = idx * AbomMeshLoader.FLOATS_PER_VERTEX;
                bb.vertex(
                        v[o], v[o + 1], v[o + 2],
                        1.0f, 1.0f, 1.0f, 1.0f,
                        v[o + 6], v[o + 7],
                        OverlayTexture.DEFAULT_UV,
                        BAKED_LIGHT,
                        v[o + 3], v[o + 4], v[o + 5]
                );
            }
            QUEUE.add(new Pending(lod, bb.end(), vertexCount,
                    chunk.pivotX, chunk.pivotY, chunk.pivotZ,
                    chunk.kind, chunk.safeDegrees, i == n - 1));
        }
    }
    public static void tick() {
        for (int i = 0; i < UPLOADS_PER_TICK; i++) {
            Pending p = QUEUE.poll();
            if (p == null) {
                return;
            }
            VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vb.bind();
            vb.upload(p.built());
            VertexBuffer.unbind();
            BUILDING.computeIfAbsent(p.lod(), k -> new ArrayList<>())
                    .add(new GpuMesh.Part(vb, p.pivotX(), p.pivotY(), p.pivotZ(),
                            p.kind(), p.safeDegrees()));
            int[] stats = BUILDING_STATS.computeIfAbsent(p.lod(), k -> new int[2]);
            stats[0] += p.vertexCount() / 3;
            stats[1] += p.vertexCount();
            if (p.last()) {
                List<GpuMesh.Part> parts = BUILDING.remove(p.lod());
                int[] s = BUILDING_STATS.remove(p.lod());
                long vram = (long) s[1] * BYTES_PER_VERTEX;
                READY.put(p.lod(), new GpuMesh(parts, s[0], vram));
                if (READY.size() == Lod.values().length) {
                }
            }
        }
    }
    public static Lod select(double distance, Lod previous) {
        double edge = Lod.LOD0.maxDistance;
        if (previous == Lod.LOD0) {
            return distance > edge + LOD_HYSTERESIS ? Lod.LOD1 : Lod.LOD0;
        }
        if (previous == Lod.LOD1) {
            return distance < edge - LOD_HYSTERESIS ? Lod.LOD0 : Lod.LOD1;
        }
        return distance <= edge ? Lod.LOD0 : Lod.LOD1;
    }
    public static GpuMesh get(Lod wanted) {
        GpuMesh m = READY.get(wanted);
        if (m != null) {
            return m;
        }
        for (Lod lod : BAKE_ORDER) {
            GpuMesh other = READY.get(lod);
            if (other != null) {
                return other;
            }
        }
        return null;
    }
    public static void discard() {
        for (GpuMesh m : READY.values()) {
            m.close();
        }
        READY.clear();
        for (List<GpuMesh.Part> parts : BUILDING.values()) {
            for (GpuMesh.Part p : parts) {
                if (!p.buffer.isClosed()) {
                    p.buffer.close();
                }
            }
        }
        BUILDING.clear();
        BUILDING_STATS.clear();
        for (Pending p : QUEUE) {
            p.built().release();
        }
        QUEUE.clear();
        BAKING.set(false);
    }
}
