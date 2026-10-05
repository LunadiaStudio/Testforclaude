package com.seafle.abomination.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.client.mesh.GpuMesh;
import com.seafle.abomination.config.AbomConfig;
import com.seafle.abomination.entity.SplitEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import java.util.Map;
import java.util.WeakHashMap;
public class SplitRenderer extends EntityRenderer<SplitEntity> {
    private static final String[] MESHES = { "mobheart", "maelstrom", "amalgam" };
    private static final Identifier AURA =
            new Identifier(Abomination.MOD_ID, "textures/entity/shield_aura.png");
    private final Map<SplitEntity, Float> shieldFade = new WeakHashMap<>();
    public SplitRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }
    private float shieldAmount(SplitEntity entity) {
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
    public Identifier getTexture(SplitEntity entity) {
        return new Identifier(Abomination.MOD_ID,
                "textures/entity/" + MESHES[clamp(entity.getVariant())] + ".png");
    }
    private static int clamp(int v) {
        return v < 0 ? 0 : (v >= MESHES.length ? MESHES.length - 1 : v);
    }
    @Override
    public void render(SplitEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light) {
        GpuMesh mesh = CoreMeshes.get(MESHES[clamp(entity.getVariant())]);
        if (mesh == null) {
            return;
        }
        float time = entity.age + tickDelta;
        Identifier tex = this.getTexture(entity);
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
        float s = AbomConfig.splitScale;
        matrices.scale(s, s, s);
        ramPose(matrices, entity, tickDelta);
        int mg = entity.getMerge();
        if (mg > 0) {
            float f = MathHelper.clamp(mg / (float) AbomConfig.refuseTicks, 0.0f, 1.0f);
            float stretch = 1.0f + 0.30f * f;
            matrices.translate(0.0, 0.5, 0.0);
            matrices.scale(1.0f / stretch, stretch, 1.0f / stretch);
            matrices.translate(0.0, -0.5, 0.0);
            if (f > 0.75f) {
                float k = (f - 0.75f) / 0.25f;
                float shrink = 1.0f - 0.45f * k * k;
                matrices.scale(shrink, shrink, shrink);
            }
        }
        float ph = entity.getId() * 1.7f;
        float idle = mg > 0 ? 0.0f : 1.0f;
        matrices.translate(0.0, 0.5, 0.0);
        if (idle > 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(time * 0.05f + ph) * 5.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    MathHelper.cos(time * 0.04f + ph) * 5.0f));
        }
        float breath = MathHelper.sin(time * 0.09f + ph) * 0.05f;
        matrices.scale(1.0f + breath, 1.0f - breath * 0.6f, 1.0f + breath);
        matrices.translate(0.0, -0.5, 0.0);
        float b = brightness(light);
        if (entity.hurtTime > 0) {
            RenderSystem.setShaderColor(b, b * 0.45f, b * 0.45f, 1.0f);
        } else {
            RenderSystem.setShaderColor(b, b, b, 1.0f);
        }
        MeshDraw.solid(mesh, matrices, tex, null);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        float shield = shieldAmount(entity);
        if (shield > 0.005f) {
            float pulse = 0.55f + 0.45f * MathHelper.sin(time * 0.09f);
            shell(mesh, matrices, 1.04f,
                    (time * 0.010f) % 1.0f, (time * 0.006f) % 1.0f,
                    0.55f, 1.0f, 1.0f, shield * (0.75f + 0.25f * pulse));
            float breathe = 1.14f + 0.035f * MathHelper.sin(time * 0.055f);
            shell(mesh, matrices, breathe,
                    (-time * 0.014f) % 1.0f, (time * 0.019f) % 1.0f,
                    0.30f, 0.95f, 0.92f, shield * (0.40f + 0.22f * pulse));
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
    private void shell(GpuMesh mesh, MatrixStack matrices, float inflate,
                       float u, float v, float r, float g, float b, float a) {
        matrices.push();
        matrices.translate(0.0, 0.5, 0.0);
        matrices.scale(inflate, inflate, inflate);
        matrices.translate(0.0, -0.5, 0.0);
        MeshDraw.swirl(mesh, matrices, AURA, u, v, r, g, b, a, null);
        matrices.pop();
    }
    static float brightness(int light) {
        int block = net.minecraft.client.render.LightmapTextureManager
                .getBlockLightCoordinates(light);
        int sky = net.minecraft.client.render.LightmapTextureManager
                .getSkyLightCoordinates(light);
        float level = Math.max(block, sky) / 15.0f;
        float floor = AbomConfig.minBrightness;
        return floor + (1.0f - floor) * level;
    }
    private static void ramPose(MatrixStack matrices,
                                com.seafle.abomination.entity.SplitEntity entity,
                                float tickDelta) {
        int rt = entity.getRamTick();
        if (rt < 0) {
            return;
        }
        float t = rt + tickDelta;
        float warn = AbomConfig.ramWarnTicks;
        float hitAt = warn + com.seafle.abomination.pattern.RamPattern.CHARGE_LEN;
        matrices.translate(0.0, 0.5, 0.0);
        if (t < warn) {
            float w = MathHelper.clamp(t / warn, 0.0f, 1.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-22.0f * w));
            float squash = 1.0f - 0.07f * w;
            matrices.scale(1.0f / squash, squash, 1.0f / squash);
        } else if (t < hitAt) {
            float go = MathHelper.clamp((t - warn) / 6.0f, 0.0f, 1.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-22.0f + 46.0f * go));
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
            double sinkBlocks = AbomConfig.ramModelSink
                    * (AbomConfig.splitScale / AbomConfig.modelScale);
            matrices.translate(0.0, -sinkBlocks / AbomConfig.splitScale * k, 0.0);
            float dive = 24.0f * in * (1.0f - out) + 34.0f * k;
            float shudder = MathHelper.sin((t - hitAt) * 0.45f) * 2.4f * k;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dive + shudder));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(9.0f * k));
            float squash = 1.0f - 0.06f * k;
            matrices.scale(1.0f / squash, squash, 1.0f / squash);
        }
        matrices.translate(0.0, -0.5, 0.0);
    }
}
