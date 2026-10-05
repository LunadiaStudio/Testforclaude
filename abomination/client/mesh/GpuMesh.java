package com.seafle.abomination.client.mesh;
import net.minecraft.client.gl.VertexBuffer;
import java.util.List;
public final class GpuMesh {
    public static final class Part {
        public final VertexBuffer buffer;
        public final float pivotX;
        public final float pivotY;
        public final float pivotZ;
        public final AbomMesh.Kind kind;
        public final float safeDegrees;
        Part(VertexBuffer buffer, float pivotX, float pivotY, float pivotZ,
             AbomMesh.Kind kind, float safeDegrees) {
            this.buffer = buffer;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.kind = kind;
            this.safeDegrees = safeDegrees;
        }
    }
    public final List<Part> parts;
    public final int triangles;
    public final long vramBytes;
    GpuMesh(List<Part> parts, int triangles, long vramBytes) {
        this.parts = parts;
        this.triangles = triangles;
        this.vramBytes = vramBytes;
    }
    public void close() {
        for (Part p : parts) {
            if (!p.buffer.isClosed()) {
                p.buffer.close();
            }
        }
        parts.clear();
    }
}
