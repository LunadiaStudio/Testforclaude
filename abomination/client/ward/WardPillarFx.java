package com.seafle.abomination.client.ward;
import com.seafle.abomination.init.AbomBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
public final class WardPillarFx {
    private WardPillarFx() {}
    private static final float PILLAR_WAVE = 0.55f;
    private static final float SEGMENT_SPACING = 5.0f;
    private static final int MAX_SEGMENTS = 34;
    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (!WardState.active) {
                return;
            }
            float rise = WardState.rise();
            if (rise <= 0.0f) {
                return;
            }
            if (WardState.fade() <= 0.0f) {
                return;
            }
            render(ctx.matrixStack(), ctx.consumers(), ctx.camera().getPos(), rise);
        });
    }
    private static void render(MatrixStack matrices, VertexConsumerProvider consumers,
                               Vec3d cam, float rise) {
        if (consumers == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        var blockRenderer = mc.getBlockRenderManager();
        var state = AbomBlocks.WARD_PILLAR.getDefaultState();
        int n = Math.max(1, WardState.pillars);
        float time = (com.seafle.abomination.client.fx.AbomFx.now() / 20.0f) % 100000.0f;
        float targetR = WardState.radius;
        float height = WardState.height;
        int segments = Math.min(MAX_SEGMENTS, Math.max(2, (int) (height / SEGMENT_SPACING)));
        float spacing = height / segments;
        for (int i = 0; i < n; i++) {
            float wave = (float) i / n * PILLAR_WAVE;
            float pr = MathHelper.clamp((rise - wave) / (1.0f - PILLAR_WAVE), 0.0f, 1.0f);
            if (pr <= 0.0f) {
                continue;
            }
            float e = 1.0f - (1.0f - pr) * (1.0f - pr) * (1.0f - pr);
            float base = (float) (i * (2.0 * Math.PI) / n);
            float ang = base + e * (float) (Math.PI * 0.5) + time * 0.06f;
            float r = targetR * e;
            float px = (float) WardState.x + MathHelper.cos(ang) * r;
            float pz = (float) WardState.z + MathHelper.sin(ang) * r;
            for (int s = 0; s < segments; s++) {
                float segDelay = (float) s / segments * 0.45f;
                float segRise = MathHelper.clamp((pr - segDelay) / (1.0f - segDelay), 0.0f, 1.0f);
                if (segRise <= 0.0f) {
                    continue;
                }
                float se = 1.0f - (1.0f - segRise) * (1.0f - segRise) * (1.0f - segRise);
                float targetY = (float) WardState.y + s * spacing;
                float py = (float) WardState.y + (targetY - (float) WardState.y) * se;
                py += MathHelper.sin(time * 0.9f + s * 0.7f + i) * 0.18f;
                float dir = (s % 2 == 0) ? 1.0f : -1.0f;
                float spin = time * 34.0f * dir + s * 27.0f + i * 13.0f;
                matrices.push();
                matrices.translate(px - cam.x, py - cam.y, pz - cam.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                        MathHelper.sin(time * 1.1f + s * 0.5f + i) * 4.0f * dir));
                matrices.translate(-0.5, -0.5, -0.5);
                blockRenderer.renderBlockAsEntity(state, matrices, consumers,
                        0x00F000F0, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }
        }
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }
}
