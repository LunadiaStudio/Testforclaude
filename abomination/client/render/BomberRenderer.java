package com.seafle.abomination.client.render;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.AbomMesh;
import com.seafle.abomination.client.mesh.AbomMeshLoader;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.config.AbomConfig;
import com.seafle.abomination.entity.BomberEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
public class BomberRenderer extends EntityRenderer<BomberEntity> {
    private static final Identifier TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/bomber.png");
    public BomberRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }
    @Override
    public Identifier getTexture(BomberEntity entity) {
        return TEXTURE;
    }
    @Override
    public void render(BomberEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        AbomMesh.Chunk mesh = CoreMeshes.cpu("bomber");
        if (mesh == null || consumers == null) {
            return;
        }
        float t = entity.age + tickDelta;
        float s = AbomConfig.bomberScale;
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        matrices.scale(s, s, s);
        matrices.translate(0.0, 0.5, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
        int fuse = entity.getFuse();
        if (fuse < 0) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 3.5f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.sin(t * 0.18f) * 11.0f));
        } else {
            float left = fuse / (float) entity.maxFuse();
            float rate = 0.7f + (1.0f - left) * 2.2f;
            float pulse = Math.abs(MathHelper.sin(t * rate));
            float swell = 1.0f + pulse * (0.10f + 0.30f * (1.0f - left));
            matrices.scale(swell, 1.0f / swell, swell);
            float shake = (1.0f - left) * (1.0f - left);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.sin(t * 4.3f) * 9.0f * shake));
        }
        matrices.translate(0.0, -0.5, 0.0);
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pm = e.getPositionMatrix();
        Matrix3f nm = e.getNormalMatrix();
        float[] v = mesh.verts;
        int fpv = AbomMeshLoader.FLOATS_PER_VERTEX;
        int[] ix = mesh.indices;
        float hot = fuse < 0 ? 0.0f
                : (1.0f - fuse / (float) entity.maxFuse())
                        * (Math.abs(MathHelper.sin(t * 2.5f)) * 0.5f + 0.5f);
        float cr = 1.0f;
        float cg = 1.0f - hot * 0.35f;
        float cb = 1.0f - hot * 0.55f;
        for (int i = 0; i + 2 < ix.length; i += 3) {
            push(vc, pm, nm, v, ix[i] * fpv, light, cr, cg, cb);
            push(vc, pm, nm, v, ix[i + 1] * fpv, light, cr, cg, cb);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light, cr, cg, cb);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light, cr, cg, cb);
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
    private static void push(VertexConsumer vc, Matrix4f pm, Matrix3f nm,
                             float[] v, int o, int light, float r, float g, float b) {
        vc.vertex(pm, v[o], v[o + 1], v[o + 2])
                .color(r, g, b, 1.0f)
                .texture(v[o + 6], v[o + 7])
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nm, v[o + 3], v[o + 4], v[o + 5])
                .next();
    }
}
