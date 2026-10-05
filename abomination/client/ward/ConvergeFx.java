package com.seafle.abomination.client.ward;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.AbomMesh;
import com.seafle.abomination.client.mesh.AbomMeshLoader;
import com.seafle.abomination.client.mesh.CoreMeshes;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
public final class ConvergeFx {
    private ConvergeFx() {}
    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (!ConvergeState.active) {
                return;
            }
            ConvergeState.fuse();
            if (!ConvergeState.active) {
                return;
            }
            render(ctx.matrixStack(), ctx.consumers(), ctx.camera().getPos());
        });
    }
    private static void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d cam) {
        if (consumers == null) {
            return;
        }
        float lift = ConvergeState.lift();
        float draw = ConvergeState.draw();
        float fuse = ConvergeState.fuse();
        Vec3d c = ConvergeState.centre;
        float time = (com.seafle.abomination.client.fx.AbomFx.now() / 20.0f) % 100000.0f;
        var list = ConvergeState.sources;
        for (int i = 0; i < list.size(); i++) {
            ConvergeState.Source src = list.get(i);
            AbomMesh.Chunk mesh = CoreMeshes.cpu(src.coreId());
            if (mesh == null) {
                continue;
            }
            Vec3d from = src.from().add(0.0, 1.0 + lift * 2.6, 0.0);
            float d = draw * draw;
            Vec3d flat = new Vec3d(from.x - c.x, 0.0, from.z - c.z);
            double r0 = flat.length();
            double ang0 = Math.atan2(flat.z, flat.x);
            double r = r0 * (1.0 - d);
            double ang = ang0 + d * 7.5 + time * 0.35 * d;
            double y = MathHelper.lerp(d, from.y, c.y);
            y += MathHelper.sin((float) (d * Math.PI)) * 3.5;
            double px = c.x + Math.cos(ang) * r;
            double pz = c.z + Math.sin(ang) * r;
            if (fuse > 0.0f) {
                float churn = MathHelper.sin(fuse * (float) Math.PI);
                float f2 = fuse * fuse;
                double orbit = 1.9 * (1.0 - f2) * (0.6 + 0.4 * churn);
                double oa = time * (4.0 + 9.0 * fuse) + i * 2.4;
                px += Math.cos(oa) * orbit;
                pz += Math.sin(oa) * orbit;
                y += Math.sin(oa * 1.7 + i) * orbit * 0.8;
                px += MathHelper.sin(time * 47f + i * 3.1f) * 0.5f * churn;
                y += MathHelper.sin(time * 53f + i * 1.7f) * 0.5f * churn;
                pz += MathHelper.cos(time * 41f + i * 2.3f) * 0.5f * churn;
            }
            float grow = 0.5f + draw * 0.35f + MathHelper.sin(fuse * (float) Math.PI) * 1.5f;
            float scale = grow * (1.0f - fuse * fuse * 0.85f);
            matrices.push();
            matrices.translate(px - cam.x, y - cam.y, pz - cam.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                    time * (60f + 240f * d + 900f * fuse) + i * 51f));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    time * (40f + 600f * fuse) + i * 33f));
            float sq = 1.0f + MathHelper.sin(time * 31f + i * 2.0f) * 0.35f * fuse;
            matrices.scale(scale * sq, scale / Math.max(0.2f, sq), scale * sq);
            float cc = Math.min(1.0f, 1.0f + fuse * 3.0f);
            drawMesh(consumers, matrices.peek(), mesh, src.coreId(), cc);
            matrices.pop();
        }
    }
    private static void drawMesh(VertexConsumerProvider consumers, MatrixStack.Entry entry,
                                 AbomMesh.Chunk mesh, String coreId, float shade) {
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(
                new Identifier(Abomination.MOD_ID,
                        com.seafle.abomination.client.render.CoreItemRenderer.folderOf(coreId)
                                + coreId + ".png")));
        Matrix4f pm = entry.getPositionMatrix();
        Matrix3f nm = entry.getNormalMatrix();
        float[] v = mesh.verts;
        int fpv = AbomMeshLoader.FLOATS_PER_VERTEX;
        int[] ix = mesh.indices;
        for (int t = 0; t + 2 < ix.length; t += 3) {
            push(vc, pm, nm, v, ix[t] * fpv, shade);
            push(vc, pm, nm, v, ix[t + 1] * fpv, shade);
            push(vc, pm, nm, v, ix[t + 2] * fpv, shade);
            push(vc, pm, nm, v, ix[t + 2] * fpv, shade);
        }
    }
    private static void push(VertexConsumer vc, Matrix4f pm, Matrix3f nm,
                             float[] v, int o, float shade) {
        vc.vertex(pm, v[o], v[o + 1], v[o + 2])
                .color(shade, shade, shade, 1.0f)
                .texture(v[o + 6], v[o + 7])
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(0x00F000F0)
                .normal(nm, v[o + 3], v[o + 4], v[o + 5])
                .next();
    }
}
