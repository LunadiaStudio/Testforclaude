package com.seafle.abomination.client.ward;
import com.seafle.abomination.Abomination;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.JsonEffectShaderProgram;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.Uniform;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
public final class SummonShader {
    private SummonShader() {}
    private static final Identifier CHAIN =
            new Identifier("minecraft", "shaders/post/abom_summon.json");
    private static PostEffectProcessor chain;
    private static int builtWidth = -1;
    private static int builtHeight = -1;
    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            if (!SummonState.active) {
                dispose();
                return;
            }
            SummonState.progress();
            if (!SummonState.active) {
                dispose();
                return;
            }
            build();
            if (chain == null) {
                return;
            }
            applyUniforms(ctx);
            try {
                chain.render(ctx.tickDelta());
                MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                com.mojang.blaze3d.systems.RenderSystem.depthFunc(
                        org.lwjgl.opengl.GL11.GL_LEQUAL);
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            } catch (Exception e) {
                dispose();
            }
        });
    }
    private static void build() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (chain != null && (w != builtWidth || h != builtHeight)) {
            dispose();
        }
        if (chain != null) {
            return;
        }
        try {
            chain = new PostEffectProcessor(mc.getTextureManager(), mc.getResourceManager(),
                    mc.getFramebuffer(), CHAIN);
            chain.setupDimensions(w, h);
            builtWidth = w;
            builtHeight = h;
        } catch (Exception e) {
            chain = null;
        }
    }
    private static void dispose() {
        if (chain == null) {
            return;
        }
        try {
            chain.close();
        } catch (Exception ignored) {
        }
        chain = null;
        builtWidth = builtHeight = -1;
    }
    private static void applyUniforms(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer fb = mc.getFramebuffer();
        Matrix4f projection = new Matrix4f(ctx.projectionMatrix());
        Matrix4f modelView = new Matrix4f(ctx.matrixStack().peek().getPositionMatrix());
        Matrix4f inverse = new Matrix4f(projection).mul(modelView).invert();
        var cam = ctx.camera().getPos();
        float outW = fb.textureWidth > 0 ? fb.textureWidth : fb.viewportWidth;
        float outH = fb.textureHeight > 0 ? fb.textureHeight : fb.viewportHeight;
        for (PostEffectPass pass : chain.passes) {
            JsonEffectShaderProgram shader = pass.program;
            if (shader == null) {
                continue;
            }
            mat4(shader, "ProjMat", projection);
            mat4(shader, "InverseTransformMatrix", inverse);
            vec2(shader, "OutSize", outW, outH);
            vec3(shader, "CameraPosition", (float) cam.x, (float) cam.y, (float) cam.z);
            vec3(shader, "SummonCentre",
                    (float) SummonState.x, (float) SummonState.y, (float) SummonState.z);
            f(shader, "SummonRadius", SummonState.radiusNow());
            f(shader, "SummonProgress", SummonState.progress());
            f(shader, "SummonReveal", SummonState.reveal());
            f(shader, "iTime", (com.seafle.abomination.client.fx.AbomFx.now() / 20.0f) % 100000.0f);
            f(shader, "SummonActive", 1f);
        }
    }
    private static void mat4(JsonEffectShaderProgram s, String name, Matrix4f v) {
        Uniform u = s.getUniformByName(name);
        if (u != null) {
            u.set(v);
        }
    }
    private static void vec3(JsonEffectShaderProgram s, String name, float a, float b, float c) {
        Uniform u = s.getUniformByName(name);
        if (u != null) {
            u.set(a, b, c);
        }
    }
    private static void vec2(JsonEffectShaderProgram s, String name, float a, float b) {
        Uniform u = s.getUniformByName(name);
        if (u != null) {
            u.set(a, b);
        }
    }
    private static void f(JsonEffectShaderProgram s, String name, float v) {
        Uniform u = s.getUniformByName(name);
        if (u != null) {
            u.set(v);
        }
    }
}
