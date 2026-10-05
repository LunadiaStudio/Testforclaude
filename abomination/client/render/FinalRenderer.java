package com.seafle.abomination.client.render;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.AbomMesh;
import com.seafle.abomination.client.mesh.AbomMeshLoader;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.config.AbomConfig;
import com.seafle.abomination.entity.FinalEntity;
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
public class FinalRenderer extends EntityRenderer<FinalEntity> {
    private static final Identifier TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/final.png");
    private static final String MESH = "final";
    private static final Identifier CRACKS =
            new Identifier(Abomination.MOD_ID, "textures/entity/death_cracks.png");
    private static final Identifier AURA =
            new Identifier(Abomination.MOD_ID, "textures/entity/shield_aura.png");
    public FinalRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.2f;
    }
    @Override
    public Identifier getTexture(FinalEntity entity) {
        return TEXTURE;
    }
    @Override
    public void render(FinalEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        AbomMesh mesh = CoreMeshes.full(MESH);
        if (mesh == null || consumers == null) {
            return;
        }
        float t = entity.age + tickDelta;
        float walk = entity.limbAnimator.getPos(tickDelta);
        int sw = entity.getSwing();
        float swing = sw > 0 ? sw + tickDelta : 0.0f;
        float dying = entity.deathTime > 0 ? entity.deathTime + tickDelta : 0.0f;
        int em = entity.getEmerge();
        float out = MathHelper.clamp((em + tickDelta) / FinalEntity.EMERGE_TICKS,
                0.0f, 1.0f);
        float over = out >= 1.0f ? 0.0f
                : MathHelper.sin(out * (float) Math.PI) * 0.06f;
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        float born = entity.birth();
        float grow = born >= 1.0f ? 1.0f : 0.35f + 0.65f * born * born;
        float s = AbomConfig.finalScale * grow;
        matrices.scale(s, s, s);
        if (out < 1.0f) {
            matrices.translate(0.0, -(1.0f - out) * 1.05f + over, 0.0);
        }
        matrices.push();
        if (dying > 0.0f) {
            float leak = MathHelper.clamp(
                    (dying - FinalEntity.CRACK_END)
                            / (float) (FinalEntity.LEAK_END - FinalEntity.CRACK_END),
                    0.0f, 1.0f);
            float burst = MathHelper.clamp(
                    (dying - FinalEntity.LEAK_END)
                            / (float) (FinalEntity.DYING_TICKS - FinalEntity.LEAK_END),
                    0.0f, 1.0f);
            matrices.translate(0.0, 0.45, 0.0);
            float shake = leak * (1.0f - burst) * 4.5f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(dying * 1.9f) * shake));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(dying * 2.3f) * shake));
            float throb = leak * (1.0f - burst)
                    * Math.max(0.0f, MathHelper.sin(dying * 0.55f)) * 0.14f;
            float swell = 1.0f + throb + burst * burst * 0.70f;
            matrices.scale(swell, swell, swell);
            matrices.translate(0.0, -0.45, 0.0);
        }
        if (out < 1.0f) {
            float rage = 1.0f - out;
            matrices.translate(0.0, 0.45, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(t * 0.35f) * 14.0f * rage));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(t * 0.29f) * 14.0f * rage));
            float swell = 1.0f + MathHelper.sin(t * 0.22f) * 0.10f * rage;
            matrices.scale(swell, 1.0f / swell, swell);
            matrices.translate(0.0, -0.45, 0.0);
        }
        float lurch = MathHelper.clamp(MathHelper.sin(walk), 0.0f, 1.0f);
        lurch = lurch * lurch;
        matrices.translate(0.0, 0.0, -lurch * 0.10f);
        matrices.translate(0.0, 0.45, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                MathHelper.cos(walk) * 6.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                -6.0f + MathHelper.sin(t * 0.055f) * 4.5f + lurch * 5.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                MathHelper.cos(t * 0.041f) * 5.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                MathHelper.sin(t * 0.033f) * 3.5f));
        float bx = 1.0f + MathHelper.sin(t * 0.071f) * 0.055f;
        float by = 1.0f + MathHelper.sin(t * 0.049f + 2.1f) * 0.048f;
        float bz = 1.0f + MathHelper.sin(t * 0.063f + 4.2f) * 0.055f;
        matrices.scale(bx, by, bz);
        if (swing > 0.0f) {
            int wu = entity.windup();
            int rc = entity.recover();
            boolean sweepKind = entity.getAttack() == FinalEntity.SWEEP;
            if (swing < wu) {
                float w = swing / (float) wu;
                if (sweepKind) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-30.0f * w));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0f * w));
                } else {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-16.0f * w));
                }
            } else {
                float r = MathHelper.clamp((swing - wu) / (float) rc, 0.0f, 1.0f);
                float k = (1.0f - r) * (1.0f - r);
                if (sweepKind) {
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(34.0f * k));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-12.0f * k));
                } else {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(22.0f * k));
                }
            }
        }
        matrices.translate(0.0, -0.45, 0.0);
        float eaten = dying > FinalEntity.LEAK_END
                ? MathHelper.clamp((dying - FinalEntity.LEAK_END)
                        / (float) (FinalEntity.DYING_TICKS - FinalEntity.LEAK_END),
                        0.0f, 1.0f)
                : 0.0f;
        if (eaten > 0.0f) {
            com.seafle.abomination.client.mesh.GpuMesh gpu = CoreMeshes.get(MESH);
            if (gpu != null) {
                MeshDraw.dissolve(gpu, matrices, TEXTURE,
                        Math.min(0.999f, eaten * 1.05f), 1.0f, 0.72f, 0.30f, null);
            }
        } else {
            VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
            int nub = 0;
            for (AbomMesh.Chunk c : mesh.chunks) {
                matrices.push();
                if (c.kind == AbomMesh.Kind.ARM) {
                    bigHand(matrices, c, t, swing, entity.getAttack(),
                            entity.windup(), entity.recover());
                } else if (c.kind != AbomMesh.Kind.BODY) {
                    dangle(matrices, c, nub++, t, walk);
                }
                draw(vc, matrices, c, light, dying);
                matrices.pop();
            }
        }
        if (dying > 0.0f) {
            float f = MathHelper.clamp(dying / FinalEntity.DYING_TICKS, 0.0f, 1.0f);
            float glow = f * f * 4.2f;
            float white = MathHelper.clamp(
                    (dying - FinalEntity.LEAK_END)
                            / (float) (FinalEntity.DYING_TICKS - FinalEntity.LEAK_END),
                    0.0f, 1.0f);
            float cr = 1.0f;
            float cg = 0.55f + 0.45f * white;
            float cb = 0.28f + 0.72f * white;
            float u = (dying * 0.0018f) % 1.0f;
            float v = (dying * 0.0011f) % 1.0f;
            matrices.push();
            matrices.translate(0.0, 0.5, 0.0);
            matrices.scale(1.006f, 1.006f, 1.006f);
            matrices.translate(0.0, -0.5, 0.0);
            com.seafle.abomination.client.mesh.GpuMesh gpu = CoreMeshes.get(MESH);
            if (gpu != null) {
                MeshDraw.swirl(gpu, matrices, CRACKS, u, v, cr, cg, cb,
                        Math.min(1.0f, glow) * (0.55f + 0.45f * white), null);
                MeshDraw.swirl(gpu, matrices, CRACKS,
                        (u * 2.7f + 0.37f) % 1.0f, (v * 1.9f + 0.61f) % 1.0f,
                        cr, cg, cb, Math.min(1.0f, glow * 0.7f) * (0.3f + 0.7f * white),
                        null);
            }
            matrices.pop();
        }
        matrices.pop();
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
    private static void bigHand(MatrixStack matrices, AbomMesh.Chunk c, float t,
                                float swing, int kind, int windup, int recover) {
        float amp = Math.max(c.safeDegrees, 15.0f);
        if (swing <= 0.0f) {
            matrices.translate(c.pivotX, c.pivotY, c.pivotZ);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(t * 0.06f) * amp * 0.35f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(t * 0.045f) * amp * 0.4f));
            matrices.translate(-c.pivotX, -c.pivotY, -c.pivotZ);
            return;
        }
        float pitch;
        float turn;
        float roll;
        if (kind == FinalEntity.SWEEP) {
            if (swing < windup) {
                float w = swing / (float) windup;
                float e = 1.0f - (1.0f - w) * (1.0f - w);
                turn = -e * 5.2f;
                pitch = -e * 1.1f;
                roll = e * 1.4f;
            } else {
                float r = MathHelper.clamp((swing - windup) / 2.0f, 0.0f, 1.0f);
                float rest = MathHelper.clamp(
                        (swing - windup) / (float) recover, 0.0f, 1.0f);
                float e = 1.0f - (1.0f - r) * (1.0f - r) * (1.0f - r);
                turn = MathHelper.lerp(e, -5.2f, 4.6f) * (1.0f - rest * 0.55f);
                pitch = MathHelper.lerp(e, -1.1f, 0.5f);
                roll = MathHelper.lerp(e, 1.4f, -1.8f);
            }
        } else {
            if (swing < windup) {
                float w = swing / (float) windup;
                float e = 1.0f - (1.0f - w) * (1.0f - w);
                pitch = -e * 6.0f;
                turn = e * 0.8f;
                roll = -e * 0.9f;
            } else {
                float r = MathHelper.clamp((swing - windup) / 3.0f, 0.0f, 1.0f);
                float rest = MathHelper.clamp(
                        (swing - windup) / (float) recover, 0.0f, 1.0f);
                float e = 1.0f - (1.0f - r) * (1.0f - r) * (1.0f - r);
                pitch = MathHelper.lerp(e, -6.0f, 2.4f) * (1.0f - rest * 0.6f);
                turn = MathHelper.lerp(e, 0.8f, -0.6f);
                roll = MathHelper.lerp(e, -0.9f, 1.2f);
            }
        }
        matrices.translate(c.pivotX, c.pivotY, c.pivotZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * amp));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(turn * amp));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll * amp));
        if (swing >= windup) {
            float rest = MathHelper.clamp(
                    1.0f - (swing - windup) / (float) recover, 0.0f, 1.0f);
            float out = 1.0f + 0.12f * rest;
            matrices.scale(out, out, out);
        }
        matrices.translate(-c.pivotX, -c.pivotY, -c.pivotZ);
    }
    private static void dangle(MatrixStack matrices, AbomMesh.Chunk c, int index,
                               float t, float walk) {
        float amp = c.safeDegrees;
        if (amp <= 0.0f) {
            return;
        }
        float a = t * (0.085f + index * 0.023f) + index * 2.2f;
        float b = t * (0.061f + index * 0.017f) + index * 4.1f;
        float gain = 1.0f + Math.abs(MathHelper.sin(walk)) * 0.5f;
        matrices.translate(c.pivotX, c.pivotY, c.pivotZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                MathHelper.sin(a) * amp * gain));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                MathHelper.cos(b) * amp * gain));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                MathHelper.sin(a * 0.6f) * amp * 0.5f));
        float grip = 1.0f + MathHelper.sin(b * 1.3f) * 0.06f;
        matrices.scale(grip, 1.0f / grip, grip);
        matrices.translate(-c.pivotX, -c.pivotY, -c.pivotZ);
    }
    private static void draw(VertexConsumer vc, MatrixStack matrices,
                             AbomMesh.Chunk c, int light, float dying) {
        MatrixStack.Entry e = matrices.peek();
        Matrix4f pm = e.getPositionMatrix();
        Matrix3f nm = e.getNormalMatrix();
        float[] v = c.verts;
        int fpv = AbomMeshLoader.FLOATS_PER_VERTEX;
        int[] ix = c.indices;
        float k = dying > 0.0f
                ? 1.0f - MathHelper.clamp(dying / (FinalEntity.CRACK_END * 1.4f),
                        0.0f, 1.0f) * 0.93f
                : 1.0f;
        for (int i = 0; i + 2 < ix.length; i += 3) {
            push(vc, pm, nm, v, ix[i] * fpv, light, k);
            push(vc, pm, nm, v, ix[i + 1] * fpv, light, k);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light, k);
            push(vc, pm, nm, v, ix[i + 2] * fpv, light, k);
        }
    }
    private static void push(VertexConsumer vc, Matrix4f pm, Matrix3f nm,
                             float[] v, int o, int light, float k) {
        vc.vertex(pm, v[o], v[o + 1], v[o + 2])
                .color(k, k, k, 1.0f)
                .texture(v[o + 6], v[o + 7])
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nm, v[o + 3], v[o + 4], v[o + 5])
                .next();
    }
}
