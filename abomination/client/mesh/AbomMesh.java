package com.seafle.abomination.client.mesh;
import java.util.List;
public final class AbomMesh {
    public enum Kind {
        BODY, ARM, LEG, NUB;
        static Kind of(int raw) {
            return switch (raw) {
                case 1 -> ARM;
                case 2 -> LEG;
                case 3 -> NUB;
                default -> BODY;
            };
        }
        public boolean isLimb() {
            return this != BODY;
        }
    }
    public static final class Chunk {
        public final float[] verts;
        public final int[] indices;
        public final float pivotX;
        public final float pivotY;
        public final float pivotZ;
        public final Kind kind;
        public final float safeDegrees;
        Chunk(float[] verts, int[] indices,
              float pivotX, float pivotY, float pivotZ, Kind kind, float safeDegrees) {
            this.verts = verts;
            this.indices = indices;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.kind = kind;
            this.safeDegrees = safeDegrees;
        }
        public int triangleCount() {
            return indices.length / 3;
        }
    }
    public final List<Chunk> chunks;
    public final int totalTriangles;
    public final int totalVertices;
    AbomMesh(List<Chunk> chunks) {
        this.chunks = chunks;
        int t = 0;
        int v = 0;
        for (Chunk c : chunks) {
            t += c.triangleCount();
            v += c.verts.length / AbomMeshLoader.FLOATS_PER_VERTEX;
        }
        this.totalTriangles = t;
        this.totalVertices = v;
    }
}
