package com.seafle.abomination.client.render;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.AbomMesh;
import com.seafle.abomination.client.mesh.AbomMeshLoader;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.config.AbomConfig;
import com.seafle.abomination.entity.ClotEntity;
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
public class ClotRenderer extends EntityRenderer<ClotEntity> {
    private static final Identifier TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/clot.png");
    private static final String MESH = "clot";
    public ClotRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6f;
    }
    @Override
    public Identifier getTexture(ClotEntity entity) {
        return TEXTURE;
    }
    @Override
    public void render(ClotEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        AbomMesh mesh = CoreMeshes.full(MESH);
        if (mesh == null || consumers == null) {
            return;
        }
        float t = entity.age + tickDelta;
        float walk = entity.limbAnimator.getPos(tickDelta);
        float speed = entity.limbAnimator.getSpeed(tickDelta);
        float step = walk * 1.1f + t * 0.035f * (1.0f - Math.min(1.0f, speed * 3.0f));
        float dying = entity.deathTime > 0
                ? MathHelper.clamp((entity.deathTime + tickDelta) / ClotEntity.WRITHE_TICKS,
                        0.0f, 1.0f)
                : 0.0f;
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        float s = AbomConfig.clotScale;
        matrices.scale(s, s, s);
        matrices.push();
        if (dying > 0.0f) {
            float rage = dying * dying;
            float f = (entity.deathTime + tickDelta);
            matrices.translate(
                    MathHelper.sin(f * 1.7f) * 0.10f * rage,
                    Math.abs(MathHelper.sin(f * 2.3f)) * 0.22f * rage,
                    MathHelper.cos(f * 1.3f) * 0.10f * rage);
            matrices.translate(0.0, 0.5, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(f * 1.9f) * 26.0f * rage));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(f * 1.5f) * 26.0f * rage));
            float swell = 1.0f + rage * 0.35f;
            matrices.scale(swell, swell, swell);
            matrices.translate(0.0, -0.5, 0.0);
        } else {
            float pull = MathHelper.sin(step);
            float lurch = MathHelper.clamp(pull, 0.0f, 1.0f);
            lurch = lurch * lurch;
            matrices.translate(0.0, 0.0, -lurch * 0.16f);
            matrices.translate(0.0, 0.18, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    -9.0f + pull * 6.0f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                    MathHelper.cos(step) * 7.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(step) * 5.0f));
            float flat = 1.0f - lurch * 0.07f;
            matrices.scale(1.0f / flat, flat, 1.0f);
            matrices.translate(0.0, -0.18, 0.0);
        }
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        int arm = 0;
        for (AbomMesh.Chunk c : mesh.chunks) {
            matrices.push();
            if (c.kind != AbomMesh.Kind.BODY) {
                crawl(matrices, c, arm++, step, dying, entity.deathTime + tickDelta);
            }
            draw(vc, matrices, c, light);
            matrices.pop();
        }
        matrices.pop();
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
    private static void crawl(MatrixStack matrices, AbomMesh.Chunk c, int index,
                              float step, float dying, float dt) {
        float p = step + index * MathHelper.PI;
        float amp = Math.min(c.safeDegrees, 20.0f);
        float swingF;
        float outF;
        if (dying > 0.0f) {
            float rage = dying * dying;
            swingF = MathHelper.sin(dt * 2.1f + index * 2.0f) * (1.0f + rage);
            outF = 0.6f + MathHelper.cos(dt * 1.7f + index) * (0.8f + rage);
        } else {
            float w = MathHelper.cos(p);
            swingF = w >= 0.0f
                    ? (float) Math.pow(w, 0.6)
                    : -(float) Math.pow(-w, 1.6);
            outF = 0.45f + 0.55f * MathHelper.sin(p);
        }
        matrices.translate(c.pivotX, c.pivotY, c.pivotZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swingF * amp));
        float side = index == 0 ? 1.0f : -1.0f;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * outF * amp * 0.9f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                side * swingF * amp * 0.35f));
        matrices.translate(-c.pivotX, -c.pivotY, -c.pivotZ);
    }
    private static void draw(VertexConsumer vc, MatrixStack matrices,
                             AbomMesh.Chunk c, int light) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pm = e.getPositionMatrix();
        Matrix3f nm = e.getNormalMatrix();
        float[] v = c.verts;
        int fpv = AbomMeshLoader.FLOATS_PER_VERTEX;
        int[] ix = c.indices;
        for (int i = 0; i + 2 < ix.length; i += 3) {
            push(vc, pm, nm, v, ix[i] * fpv, light);
            push(vc, pm, nm, v, ix[i + 1] * fpv, light);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light);
        }
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
