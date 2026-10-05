package com.seafle.abomination.client.mesh;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
public final class AbomMeshLoader {
    private AbomMeshLoader() {}
    public static final int FLOATS_PER_VERTEX = 8;
    private static final int VERSION = 3;
    public static AbomMesh read(InputStream raw) throws IOException {
        byte[] all;
        try (DataInputStream in = new DataInputStream(raw)) {
            all = in.readAllBytes();
        }
        ByteBuffer buf = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        if (all.length < 12
                || all[0] != 'A' || all[1] != 'B' || all[2] != 'O' || all[3] != 'M') {
            throw new IOException("abmesh 매직이 아니다");
        }
        buf.position(4);
        int version = buf.getInt();
        if (version != VERSION) {
            throw new IOException("abmesh 버전 " + version + " 은 못 읽는다 (기대 " + VERSION + ")");
        }
        int chunkCount = buf.getInt();
        if (chunkCount <= 0 || chunkCount > 4096) {
            throw new IOException("청크 수가 이상하다: " + chunkCount);
        }
        List<AbomMesh.Chunk> chunks = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            int kind = buf.getInt();
            int vertCount = buf.getInt();
            int triCount = buf.getInt();
            if (vertCount <= 0 || triCount <= 0) {
                throw new IOException("청크 " + i + " 가 비었다");
            }
            float safeDegrees = buf.getFloat();
            float pivotX = buf.getFloat();
            float pivotY = buf.getFloat();
            float pivotZ = buf.getFloat();
            buf.position(buf.position() + 6 * Float.BYTES);
            float[] verts = new float[vertCount * FLOATS_PER_VERTEX];
            FloatBuffer fb = buf.asFloatBuffer();
            fb.get(verts);
            buf.position(buf.position() + verts.length * Float.BYTES);
            int[] indices = new int[triCount * 3];
            IntBuffer ib = buf.asIntBuffer();
            ib.get(indices);
            buf.position(buf.position() + indices.length * Integer.BYTES);
            chunks.add(new AbomMesh.Chunk(verts, indices, pivotX, pivotY, pivotZ,
                    AbomMesh.Kind.of(kind), safeDegrees));
        }
        return new AbomMesh(chunks);
    }
}
