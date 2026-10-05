package com.seafle.abomination.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.GpuMesh;
import com.seafle.abomination.client.mesh.MeshBakery;
import com.seafle.abomination.client.part.PartTags;
import com.seafle.abomination.config.AbomConfig;
import com.seafle.abomination.entity.AbominationEntity;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import java.util.Map;
import java.util.WeakHashMap;
public class AbominationRenderer extends EntityRenderer<AbominationEntity> {
    private static final Identifier TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/abomination.png");
    private static final Identifier SHIELD_TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/shield_aura.png");
    public AbominationRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }
    @Override
    public Identifier getTexture(AbominationEntity entity) {
        return TEXTURE;
    }
    private final Map<AbominationEntity, MeshBakery.Lod> lastLod = new WeakHashMap<>();
    private final Map<AbominationEntity, Float> shieldFade = new WeakHashMap<>();
    private float shieldAmount(AbominationEntity entity) {
        float want = entity.isShielded() ? 1.0f : 0.0f;
        float cur = this.shieldFade.getOrDefault(entity, want);
        float rate = want > cur ? 0.10f : 0.16f;
        cur = cur + (want - cur) * rate;
        if (Math.abs(want - cur) < 0.002f) {
            cur = want;
        }
        this.shieldFade.put(entity, cur);
        return cur;
    }
    @Override
    public void render(AbominationEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (entity.isInvisible()) {
            return;
        }
        float split = entity.getPhase() == 1
                ? MathHelper.clamp(
                        (entity.getSplitTick() + tickDelta) / (float) AbomConfig.splitAnimTicks,
                        0.0f, 1.0f)
                : 0.0f;
        float tear = MathHelper.clamp((split - 0.62f) / 0.38f, 0.0f, 1.0f);
        int copies = tear > 0.0f ? 3 : 1;
        double distance = this.dispatcher.camera.getPos().distanceTo(entity.getPos());
        MeshBakery.Lod lod = MeshBakery.select(
                copies > 1 ? distance * 2.5 : distance, this.lastLod.get(entity));
        this.lastLod.put(entity, lod);
        GpuMesh mesh = MeshBakery.get(lod);
        if (mesh == null) {
            return;
        }
        float shield = shieldAmount(entity);
        for (int c = 0; c < copies; c++) {
            drawBody(entity, yaw, tickDelta, matrices, mesh, light, split, tear, c, shield);
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
    private void drawBody(AbominationEntity entity, float yaw, float tickDelta,
                          MatrixStack matrices, GpuMesh mesh, int light,
                          float split, float tear, int copy, float shield) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        float emerge = MathHelper.clamp(entity.age / (float) EMERGE_TICKS, 0.0f, 1.0f);
        float scale = AbomConfig.modelScale * (0.12f + 0.88f * emerge * emerge);
        scale *= 1.0f + 0.22f * split * (1.0f - tear) - 0.30f * tear;
        matrices.scale(scale, scale, scale);
        matrices.translate(0.0, AbomConfig.modelYOffset / scale, 0.0);
        if (tear > 0.0f) {
            double lane = copy * (Math.PI * 2 / 3.0);
            float push = tear * tear * entity.getHeight() * 0.55f;
            matrices.translate(Math.cos(lane) * push / scale,
                    tear * entity.getHeight() * 0.10f / scale,
                    Math.sin(lane) * push / scale);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(entity.age * 0.4f + copy * 2.1f) * 22.0f * tear));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(entity.age * 0.33f + copy * 1.4f) * 22.0f * tear));
        }
        pulsePhase = com.seafle.abomination.client.fx.BodyPulse.phase(entity.getId());
        pulseAmount = com.seafle.abomination.client.fx.BodyPulse.amount(pulsePhase);
        if (pulseAmount > 0.0f) {
            float k = MathHelper.sin(pulsePhase * 0.42f) * 0.30f * pulseAmount;
            float wide = 1.0f / (float) Math.sqrt(1.0f + k);
            matrices.scale(wide, 1.0f + k, wide);
        }
        idle(matrices, entity.age + tickDelta, scale);
        attackPose(matrices, entity, tickDelta);
        applyTint(entity, light, emerge, split);
        boolean noCull = AbomConfig.renderNoCull;
        RenderLayer layer = noCull
                ? RenderLayer.getEntityCutoutNoCull(TEXTURE)
                : RenderLayer.getEntitySolid(TEXTURE);
        var shader = noCull
                ? GameRenderer.getRenderTypeEntityCutoutNoNullProgram()
                : GameRenderer.getRenderTypeEntitySolidProgram();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f globalView = new Matrix4f(RenderSystem.getModelViewMatrix());
        float animTime = entity.age + tickDelta;
        limbBoost = entity.getAttackPose() == 1 ? 0.35f
                : entity.getAttackPose() == 2 ? 2.2f : 1.0f;
        limbRaise = 0.0f;
        limbThrust = 0.0f;
        if (entity.getAttackPose() == 1) {
            float pt = entity.getAttackPoseTick() + tickDelta;
            limbRaise = MathHelper.clamp(pt / 6.0f, 0.0f, 1.0f);
            limbRaise = 1.0f - (1.0f - limbRaise) * (1.0f - limbRaise);
            if (pt > 86.0f) {
                float since = (pt - 86.0f) % 2.0f;
                limbThrust = 1.0f - since / 2.0f;
                limbThrust *= limbThrust;
            }
        }
        if (split > 0.0f) {
            limbBoost = Math.max(limbBoost, 1.0f + 4.0f * split);
        }
        if (entity.isPoser()) {
            limbBoost = 3.2f;
        }
        layer.startDrawing();
        int limbIndex = -1;
        for (int i = 0; i < mesh.parts.size(); i++) {
            GpuMesh.Part part = mesh.parts.get(i);
            boolean baked = part.kind.isLimb();
            if (baked) {
                limbIndex++;
            }
            boolean moves = baked && PartTags.kindOf(limbIndex, part.kind).isLimb();
            matrices.push();
            if (moves) {
                limbSwing(matrices, part, limbIndex, animTime);
            }
            part.buffer.bind();
            part.buffer.draw(new Matrix4f(globalView).mul(matrices.peek().getPositionMatrix()),
                    projection, shader);
            matrices.pop();
        }
        VertexBuffer.unbind();
        layer.endDrawing();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (shield > 0.005f) {
            drawShield(mesh, matrices, globalView, projection, animTime, shield);
        }
        matrices.pop();
    }
    private void drawShield(GpuMesh mesh, MatrixStack matrices, Matrix4f globalView,
                            Matrix4f projection, float animTime, float shield) {
        float u0 = (animTime * 0.010f) % 1.0f;
        float v0 = (animTime * 0.006f) % 1.0f;
        float pulse = 0.55f + 0.45f * MathHelper.sin(animTime * 0.09f);
        shell(mesh, matrices, globalView, projection, animTime,
                1.035f, u0, v0, 0.55f, 1.0f, 1.0f, shield * (0.75f + 0.25f * pulse));
        float breathe = 1.13f + 0.035f * MathHelper.sin(animTime * 0.055f);
        shell(mesh, matrices, globalView, projection, animTime,
                breathe, (-animTime * 0.014f) % 1.0f, (animTime * 0.019f) % 1.0f,
                0.30f, 0.95f, 0.92f, shield * (0.38f + 0.22f * pulse));
    }
    private void shell(GpuMesh mesh, MatrixStack matrices, Matrix4f globalView,
                       Matrix4f projection, float animTime, float inflate,
                       float u, float v, float r, float g, float b, float a) {
        if (a <= 0.004f) {
            return;
        }
        RenderLayer layer = RenderLayer.getEnergySwirl(SHIELD_TEXTURE, u, v);
        var shader = GameRenderer.getRenderTypeEnergySwirlProgram();
        matrices.push();
        matrices.translate(0.0, 0.5, 0.0);
        matrices.scale(inflate, inflate, inflate);
        matrices.translate(0.0, -0.5, 0.0);
        RenderSystem.setShaderColor(r, g, b, a);
        layer.startDrawing();
        int limbIndex = -1;
        for (int i = 0; i < mesh.parts.size(); i++) {
            GpuMesh.Part part = mesh.parts.get(i);
            boolean baked = part.kind.isLimb();
            if (baked) {
                limbIndex++;
            }
            boolean moves = baked && PartTags.kindOf(limbIndex, part.kind).isLimb();
            matrices.push();
            if (moves) {
                limbSwing(matrices, part, limbIndex, animTime);
            }
            part.buffer.bind();
            part.buffer.draw(new Matrix4f(globalView).mul(matrices.peek().getPositionMatrix()),
                    projection, shader);
            matrices.pop();
        }
        VertexBuffer.unbind();
        layer.endDrawing();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrices.pop();
    }
    private static void idle(MatrixStack matrices, float time, float scale) {
        float bs = AbomConfig.idleBobSpeed;
        float bob = (MathHelper.sin(time * bs)
                + MathHelper.sin(time * bs * 1.7f + 0.9f) * 0.35f)
                * AbomConfig.idleBobHeight;
        matrices.translate(0.0, bob / scale, 0.0);
        float ss = AbomConfig.idleSwaySpeed;
        float amp = AbomConfig.idleSwayDegrees;
        float pitch = (MathHelper.sin(time * ss)
                + MathHelper.sin(time * ss * 2.3f + 1.7f) * 0.4f) * amp;
        float roll = (MathHelper.cos(time * ss * 0.61f)
                + MathHelper.sin(time * ss * 1.9f + 0.6f) * 0.4f) * amp;
        float twist = MathHelper.sin(time * ss * 0.43f + 2.1f) * AbomConfig.idleTwistDegrees;
        matrices.translate(0.0, 0.5, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(twist));
        float br = AbomConfig.idleBreathAmount;
        if (br != 0.0f) {
            float p = time * AbomConfig.idleBreathSpeed;
            float sx = MathHelper.sin(p);
            float sz = MathHelper.sin(p + 2.094f);
            matrices.scale(1.0f + br * sx,
                    1.0f - br * (sx + sz) * 0.5f * 1.2f,
                    1.0f + br * sz);
        }
        matrices.translate(0.0, -0.5, 0.0);
    }
    private static void attackPose(MatrixStack matrices, AbominationEntity entity, float tickDelta) {
        int pose = entity.getAttackPose();
        if (pose == 0) {
            return;
        }
        float t = entity.getAttackPoseTick() + tickDelta;
        matrices.translate(0.0, 0.5, 0.0);
        if (pose == 1) {
            float sweep = MathHelper.sin(t * 0.10f) * 16.0f;
            float lean = 8.0f + MathHelper.sin(t * 0.06f) * 6.0f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lean));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sweep));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                    MathHelper.cos(t * 0.075f) * 14.0f));
        } else if (pose == 2) {
            float wind = MathHelper.clamp(t / 26.0f, 0.0f, 1.0f);
            float amp = 3.0f + wind * 13.0f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(t * 0.16f) * amp));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.sin(t * 0.11f + 1.1f) * amp * 0.8f));
            float swell = 1.0f + MathHelper.sin(t * 0.13f) * 0.13f * wind;
            matrices.scale(swell, 1.0f / swell, swell);
        } else if (pose == 3) {
            float charge = MathHelper.clamp(t / 62.0f, 0.0f, 1.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    -16.0f * charge + MathHelper.sin(t * 0.14f) * 3.5f));
            float squash = 1.0f - 0.09f * charge;
            matrices.scale(1.0f / squash, squash, 1.0f / squash);
            if (t > 62.0f) {
                float snap = MathHelper.clamp((t - 62.0f) / 10.0f, 0.0f, 1.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                        34.0f * (1.0f - snap)));
            }
        } else if (pose == 4) {
            float warn = AbomConfig.ramWarnTicks;
            float hitAt = warn + com.seafle.abomination.pattern.RamPattern.CHARGE_LEN;
            if (t < warn) {
                float w = MathHelper.clamp(t / warn, 0.0f, 1.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-22.0f * w));
                float squash = 1.0f - 0.07f * w;
                matrices.scale(1.0f / squash, squash, 1.0f / squash);
            } else if (t < hitAt) {
                float go = MathHelper.clamp((t - warn) / 6.0f, 0.0f, 1.0f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                        -22.0f + 46.0f * go));
                float stretch = 1.0f + 0.10f * go;
                matrices.scale(1.0f / stretch, stretch, 1.0f / stretch);
            } else {
                float span = com.seafle.abomination.pattern.RamPattern.RECOVER;
                float p = MathHelper.clamp((t - hitAt) / span, 0.0f, 1.0f);
                float in = MathHelper.clamp(p / 0.12f, 0.0f, 1.0f);
                in = in * in * (3.0f - 2.0f * in);
                float out = MathHelper.clamp((p - 0.65f) / 0.35f, 0.0f, 1.0f);
                out = out * out * (3.0f - 2.0f * out);
                float k = in * (1.0f - out);
                matrices.translate(0.0,
                        -AbomConfig.ramModelSink / AbomConfig.modelScale * k, 0.0);
                float dive = 24.0f * in * (1.0f - out) + 34.0f * k;
                float shudder = MathHelper.sin((t - hitAt) * 0.45f) * 2.4f * k;
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dive + shudder));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(9.0f * k));
                float squash = 1.0f - 0.06f * k;
                matrices.scale(1.0f / squash, squash, 1.0f / squash);
            }
        }
        matrices.translate(0.0, -0.5, 0.0);
    }
    private static float limbBoost = 1.0f;
    private static float pulsePhase = -1.0f;
    private static float pulseAmount = 0.0f;
    private static float limbRaise = 0.0f;
    private static float limbThrust = 0.0f;
    private static void limbSwing(MatrixStack matrices, GpuMesh.Part part, int index, float time) {
        float amp = Math.min(part.safeDegrees * AbomConfig.idleLimbScale,
                AbomConfig.idleLimbMaxDegrees) * limbBoost;
        if (amp <= 0.0f) {
            return;
        }
        float speed = AbomConfig.idleLimbSpeed;
        float wave = part.pivotY * 8.0f;
        float phase = wave + index * 0.37f;
        float main = MathHelper.sin(time * speed - phase);
        float slow = MathHelper.sin(time * speed * 0.31f - phase * 0.5f);
        float a = main * amp;
        float b = slow * amp * 0.75f;
        float c = MathHelper.sin(time * speed * 0.19f - phase * 0.3f) * amp * 0.45f;
        float bump = 0.0f;
        if (pulseAmount > 0.0f) {
            float crest = pulsePhase * 0.11f;
            float d = Math.abs(part.pivotY - crest);
            bump = Math.max(0.0f, 1.0f - d * 5.0f) * pulseAmount;
        }
        float lift = 0.0f;
        float spread = 0.0f;
        if (limbRaise > 0.0f && part.kind == com.seafle.abomination.client.mesh
                .AbomMesh.Kind.ARM) {
            float vary = 0.75f + 0.5f * MathHelper.sin(index * 2.1f);
            lift = -limbRaise * amp * 2.4f * vary;
            spread = limbRaise * amp * 1.3f * (index % 2 == 0 ? 1.0f : -1.0f);
            lift += limbThrust * amp * 1.8f;
        }
        matrices.translate(part.pivotX, part.pivotY, part.pivotZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                a + bump * amp * 2.2f + lift));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(b + spread));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(c));
        float swell = 1.0f + main * 0.06f * Math.min(2.0f, limbBoost) + bump * 0.22f;
        matrices.scale(swell, 1.0f / swell, swell);
        matrices.translate(-part.pivotX, -part.pivotY, -part.pivotZ);
    }
    private static final int EMERGE_TICKS = 52;
    private static void applyTint(AbominationEntity entity, int light,
                                  float emerge, float split) {
        int block = LightmapTextureManager.getBlockLightCoordinates(light);
        int sky = LightmapTextureManager.getSkyLightCoordinates(light);
        float level = Math.max(block, sky) / 15.0f;
        float floor = AbomConfig.minBrightness;
        float b = floor + (1.0f - floor) * level;
        b *= 0.05f + 0.95f * emerge * emerge;
        b *= 1.0f - 0.88f * split;
        if (entity.hurtTime > 0) {
            RenderSystem.setShaderColor(b, b * 0.45f, b * 0.45f, 1.0f);
        } else {
            RenderSystem.setShaderColor(b, b, b, 1.0f);
        }
    }
}
