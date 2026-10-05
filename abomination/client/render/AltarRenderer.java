package com.seafle.abomination.client.render;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.block.AltarBlockEntity;
import com.seafle.abomination.client.mesh.CoreMeshes;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
    private static float[] tint(String id) {
        return switch (id) {
            case "warden_core" -> new float[]{0.18f, 0.85f, 0.80f};
            case "enderman_core" -> new float[]{0.58f, 0.30f, 1.00f};
            case "creeper_core" -> new float[]{0.35f, 0.95f, 0.35f};
            case "drowned_core" -> new float[]{0.25f, 0.80f, 0.78f};
            case "skeleton_core" -> new float[]{0.85f, 0.85f, 0.80f};
            case "villager_core" -> new float[]{0.95f, 0.45f, 0.40f};
            case "zombie_core" -> new float[]{0.55f, 0.80f, 0.35f};
            default -> new float[]{0.45f, 1.00f, 0.92f};
        };
    }
    private static final java.util.Set<String> MISSING =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final float BURST_TICKS = 26f;
    public AltarRenderer(net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context ctx) {
    }
    @Override
    public void render(AltarBlockEntity altar, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light, int overlay) {
        if (!altar.hasCore() || altar.getWorld() == null) {
            return;
        }
        ItemStack stack = altar.getCore();
        Identifier id = Registries.ITEM.getId(stack.getItem());
        com.seafle.abomination.client.mesh.AbomMesh.Chunk mesh = CoreMeshes.cpu(id.getPath());
        if (altar.isPlain()) {
            if (mesh != null) {
                matrices.push();
                matrices.translate(0.5, 0.96, 0.5);
                matrices.scale(0.42f, 0.42f, 0.42f);
                drawMesh(consumers, matrices, mesh, id, 1.0f, light, overlay);
                matrices.pop();
            }
            return;
        }
        float time = (altar.getWorld().getTime() + tickDelta);
        long age = altar.ageSincePlaced(altar.getWorld().getTime());
        float burst = age >= BURST_TICKS ? 0f
                : 1f - MathHelper.clamp((age + tickDelta) / BURST_TICKS, 0f, 1f);
        float hover = 1.02f + MathHelper.sin(time * 0.08f) * 0.055f;
        hover += burst * burst * 0.55f;
        matrices.push();
        matrices.translate(0.5, hover, 0.5);
        drawAura(matrices, consumers, time, burst, tint(id.getPath()));
        if (mesh == null) {
            if (MISSING.add(id.getPath())) {
            }
        }
        if (mesh != null) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 1.6f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(time * 0.05f) * 9.0f));
            float sc = 0.42f + burst * 0.22f;
            matrices.scale(sc, sc, sc);
            float glow = MathHelper.clamp(1.0f + burst * 1.6f, 0f, 2.0f);
            float cr = Math.min(1f, glow), cg = Math.min(1f, glow), cb = Math.min(1f, glow);
            drawMesh(consumers, matrices, mesh, id, cr, 0x00F000F0, overlay);
            matrices.pop();
        }
        matrices.pop();
    }
    private static void drawMesh(VertexConsumerProvider consumers, MatrixStack matrices,
                                 com.seafle.abomination.client.mesh.AbomMesh.Chunk mesh,
                                 Identifier id, float shade, int light, int overlay) {
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(
                new Identifier(Abomination.MOD_ID,
                        CoreItemRenderer.folderOf(id.getPath()) + id.getPath() + ".png")));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pm = entry.getPositionMatrix();
        org.joml.Matrix3f nm = entry.getNormalMatrix();
        float[] v = mesh.verts;
        int fpv = com.seafle.abomination.client.mesh.AbomMeshLoader.FLOATS_PER_VERTEX;
        int[] ix = mesh.indices;
        for (int t = 0; t + 2 < ix.length; t += 3) {
            int a0 = ix[t] * fpv, a1 = ix[t + 1] * fpv, a2 = ix[t + 2] * fpv;
            push(vc, pm, nm, v, a0, shade, shade, shade, overlay, light);
            push(vc, pm, nm, v, a1, shade, shade, shade, overlay, light);
            push(vc, pm, nm, v, a2, shade, shade, shade, overlay, light);
            push(vc, pm, nm, v, a2, shade, shade, shade, overlay, light);
        }
    }
    private static void push(VertexConsumer vc, Matrix4f pm, org.joml.Matrix3f nm,
                             float[] v, int o, float r, float g, float b,
                             int overlay, int light) {
        vc.vertex(pm, v[o], v[o + 1], v[o + 2])
                .color(r, g, b, 1.0f)
                .texture(v[o + 6], v[o + 7])
                .overlay(overlay)
                .light(light)
                .normal(nm, v[o + 3], v[o + 4], v[o + 5])
                .next();
    }
    private static void drawAura(MatrixStack matrices, VertexConsumerProvider consumers,
                                 float time, float burst, float[] c) {
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
        Matrix4f m = matrices.peek().getPositionMatrix();
        for (int i = 0; i < 5; i++) {
            matrices.push();
            float spin = time * (0.9f + i * 0.55f) * ((i % 2 == 0) ? 1f : -1f);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28f + i * 37f));
            float r = 0.34f + i * 0.075f + burst * 0.5f;
            ring(consumers, matrices.peek().getPositionMatrix(), r, 0.045f,
                    c[0], c[1], c[2], 0.5f + burst * 0.5f);
            matrices.pop();
        }
        matrices.push();
        matrices.translate(0.0, -0.30, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-time * 0.5f));
        float gr = 0.42f + MathHelper.sin(time * 0.09f) * 0.03f + burst * 0.9f;
        ring(consumers, matrices.peek().getPositionMatrix(), gr, 0.05f,
                c[0], c[1], c[2], 0.30f + burst * 0.5f);
        matrices.pop();
        helix(consumers, m, time, burst, c);
        float y0 = 0.34f;
        float beamH = y0 + 1.1f + burst * 16.0f;
        float beamA = 0.20f + burst * 0.6f;
        float bw = 0.13f + burst * 0.2f;
        for (int i = 0; i < 6; i++) {
            float a = (float) (Math.PI * i / 6.0) + time * 0.015f;
            float dx = MathHelper.cos(a) * bw;
            float dz = MathHelper.sin(a) * bw;
            vc.vertex(m, -dx, y0, -dz).color(c[0], c[1], c[2], beamA).next();
            vc.vertex(m, dx, y0, dz).color(c[0], c[1], c[2], beamA).next();
            vc.vertex(m, dx, beamH, dz).color(c[0] * 0.8f, c[1] * 0.5f, 1.0f, 0f).next();
            vc.vertex(m, -dx, beamH, -dz).color(c[0] * 0.8f, c[1] * 0.5f, 1.0f, 0f).next();
        }
        for (int i = 0; i < 6; i++) {
            float a = (float) (Math.PI * i / 6.0) - time * 0.015f;
            float dx = MathHelper.cos(a) * bw * 0.8f;
            float dz = MathHelper.sin(a) * bw * 0.8f;
            vc.vertex(m, -dx, -0.34f, -dz).color(c[0], c[1], c[2], 0f).next();
            vc.vertex(m, dx, -0.34f, dz).color(c[0], c[1], c[2], 0f).next();
            vc.vertex(m, dx, -0.02f, dz).color(c[0], c[1], c[2], beamA * 0.8f).next();
            vc.vertex(m, -dx, -0.02f, -dz).color(c[0], c[1], c[2], beamA * 0.8f).next();
        }
    }
    private static void helix(VertexConsumerProvider consumers, Matrix4f m,
                              float time, float burst, float[] c) {
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
        int steps = 54;
        for (int strand = 0; strand < 2; strand++) {
            float dir = strand == 0 ? 1f : -1f;
            float phase = strand * 3.14159f;
            for (int i = 0; i < steps; i++) {
                float t0 = (float) i / steps;
                float t1 = (float) (i + 1) / steps;
                float a0 = t0 * 12.0f * dir + time * 0.11f * dir + phase;
                float a1 = t1 * 12.0f * dir + time * 0.11f * dir + phase;
                float rad0 = (0.30f + MathHelper.sin(t0 * 3.14159f) * 0.16f) * (1f + burst);
                float rad1 = (0.30f + MathHelper.sin(t1 * 3.14159f) * 0.16f) * (1f + burst);
                float y0 = -0.38f + t0 * 0.78f;
                float y1 = -0.38f + t1 * 0.78f;
                float a = (0.22f + burst * 0.5f) * MathHelper.sin(t0 * 3.14159f);
                float w = 0.028f;
                vc.vertex(m, MathHelper.cos(a0) * rad0, y0 - w, MathHelper.sin(a0) * rad0)
                        .color(c[0], c[1], c[2], a).next();
                vc.vertex(m, MathHelper.cos(a1) * rad1, y1 - w, MathHelper.sin(a1) * rad1)
                        .color(c[0], c[1], c[2], a).next();
                vc.vertex(m, MathHelper.cos(a1) * rad1, y1 + w, MathHelper.sin(a1) * rad1)
                        .color(1f, 1f, 1f, a * 0.8f).next();
                vc.vertex(m, MathHelper.cos(a0) * rad0, y0 + w, MathHelper.sin(a0) * rad0)
                        .color(1f, 1f, 1f, a * 0.8f).next();
            }
        }
    }
    private static void ring(VertexConsumerProvider consumers, Matrix4f m,
                             float radius, float half, float r, float g, float b, float a) {
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLightning());
        int segs = 72;
        for (int i = 0; i < segs; i++) {
            float t0 = (float) (Math.PI * 2 * i / segs);
            float t1 = (float) (Math.PI * 2 * (i + 1) / segs);
            float x0 = MathHelper.cos(t0) * radius, z0 = MathHelper.sin(t0) * radius;
            float x1 = MathHelper.cos(t1) * radius, z1 = MathHelper.sin(t1) * radius;
            vc.vertex(m, x0, -half, z0).color(r, g, b, a).next();
            vc.vertex(m, x1, -half, z1).color(r, g, b, a).next();
            vc.vertex(m, x1, half, z1).color(r, g, b, a * 0.35f).next();
            vc.vertex(m, x0, half, z0).color(r, g, b, a * 0.35f).next();
        }
    }
    @Override
    public int getRenderDistance() {
        return 96;
    }
}
