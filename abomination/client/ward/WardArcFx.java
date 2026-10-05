package com.seafle.abomination.client.ward;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
public final class WardArcFx {
    private WardArcFx() {}
    private static final int RING_SEGMENTS = 160;
    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (!WardState.active || WardState.fade() <= 0.0f) {
                return;
            }
            float link = WardState.link();
            if (link <= 0.0f) {
                return;
            }
            Matrix4f m = ctx.matrixStack().peek().getPositionMatrix();
            Vec3d cam = ctx.camera().getPos();
            render(m, cam, link, WardState.progress());
        });
    }
    private static void render(Matrix4f m, Vec3d cam, float link, float progress) {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        float r = WardState.radius;
        double cx = WardState.x - cam.x;
        double cy = WardState.y - cam.y;
        double cz = WardState.z - cam.z;
        float time = (com.seafle.abomination.client.fx.AbomFx.now() / 20.0f) % 100000.0f;
        float ringY = (float) cy + WardState.height * 0.22f;
        float shock = MathHelper.clamp((link - 0.55f) / 0.45f, 0.0f, 1.0f);
        if (shock > 0.0f) {
            float ringR = r * (0.15f + shock * 1.25f);
            float thick = r * 0.09f * (1.0f - shock * 0.5f);
            float alpha = (1.0f - shock) * 0.85f;
            float gy = (float) cy + 0.15f;
            buf.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                float th = (float) (Math.PI * 2 * i / RING_SEGMENTS);
                float ux = MathHelper.cos(th);
                float uz = MathHelper.sin(th);
                buf.vertex(m, (float) cx + ux * (ringR - thick), gy,
                                (float) cz + uz * (ringR - thick))
                        .color(0.35f, 1.0f, 0.92f, alpha).next();
                buf.vertex(m, (float) cx + ux * ringR, gy, (float) cz + uz * ringR)
                        .color(0.55f, 0.35f, 1.0f, 0.0f).next();
            }
            tess.draw();
        }
        if (progress > 0.0f && progress < 1.0f) {
            float lift = WardState.height * progress;
            float alpha = (1.0f - progress) * 0.5f;
            buf.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= RING_SEGMENTS; i++) {
                float th = (float) (Math.PI * 2 * i / RING_SEGMENTS);
                float ux = MathHelper.cos(th) * r;
                float uz = MathHelper.sin(th) * r;
                buf.vertex(m, (float) cx + ux, (float) cy + lift - 3.5f, (float) cz + uz)
                        .color(0.5f, 1.0f, 0.95f, 0.0f).next();
                buf.vertex(m, (float) cx + ux, (float) cy + lift, (float) cz + uz)
                        .color(0.85f, 1.0f, 1.0f, alpha).next();
            }
            tess.draw();
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
