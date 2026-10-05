package com.seafle.abomination.client.render;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.AbomMesh;
import com.seafle.abomination.client.mesh.AbomMeshLoader;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.entity.SpikeEntity;
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
public class SpikeRenderer extends EntityRenderer<SpikeEntity> {
    private static final Identifier TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/spike.png");
    private static final float RISE = 4.0f;
    private static final float SINK = 12.0f;
    private static final float BURY = 0.45f;
    public SpikeRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }
    @Override
    public Identifier getTexture(SpikeEntity entity) {
        return TEXTURE;
    }
    @Override
    public void render(SpikeEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        AbomMesh.Chunk mesh = CoreMeshes.cpu("spike");
        if (mesh == null || consumers == null) {
            return;
        }
        float t = entity.age + tickDelta;
        float h = entity.getScale();
        float rise = MathHelper.clamp(t / RISE, 0.0f, 1.0f);
        rise = 1.0f - (1.0f - rise) * (1.0f - rise) * (1.0f - rise);
        float left = entity.getLife() - t;
        float sink = left < SINK ? MathHelper.clamp(1.0f - left / SINK, 0.0f, 1.0f) : 0.0f;
        float out = rise - sink;
        if (out <= 0.0f) {
            return;
        }
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch()));
        float sunk = MathHelper.lerp(out, 1.0f, BURY);
        matrices.translate(0.0, -h * sunk, 0.0);
        matrices.scale(h, h, h);
        if (rise < 1.0f) {
            float over = 1.0f + MathHelper.sin(rise * (float) Math.PI) * 0.12f;
            matrices.scale(1.0f / over, over, 1.0f / over);
        }
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pm = e.getPositionMatrix();
        Matrix3f nm = e.getNormalMatrix();
        float[] v = mesh.verts;
        int fpv = AbomMeshLoader.FLOATS_PER_VERTEX;
        int[] ix = mesh.indices;
        for (int i = 0; i + 2 < ix.length; i += 3) {
            push(vc, pm, nm, v, ix[i] * fpv, light);
            push(vc, pm, nm, v, ix[i + 1] * fpv, light);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light);
        }
        matrices.pop();
    }
    private static void push(VertexConsumer vc, Matrix4f pm, Matrix3f nm,
                             float[] v, int o, int light) {
        vc.vertex(pm, v[o], v[o + 1], v[o + 2])
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .texture(v[o + 6], v[o + 7])
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nm, v[o + 3], v[o + 4], v[o + 5])
                .next();
    }
}
