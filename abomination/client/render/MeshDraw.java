package com.seafle.abomination.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seafle.abomination.client.mesh.GpuMesh;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
public final class MeshDraw {
    private MeshDraw() {}
    public interface PartPose {
        void apply(MatrixStack matrices, GpuMesh.Part part, int index);
    }
    public static void solid(GpuMesh mesh, MatrixStack matrices, Identifier texture,
                             PartPose pose) {
        draw(mesh, matrices, RenderLayer.getEntityCutoutNoCull(texture),
                GameRenderer.getRenderTypeEntityCutoutNoNullProgram(), pose);
    }
    public static void swirl(GpuMesh mesh, MatrixStack matrices, Identifier texture,
                             float u, float v, float r, float g, float b, float a,
                             PartPose pose) {
        if (a <= 0.004f) {
            return;
        }
        RenderSystem.setShaderColor(r, g, b, a);
        draw(mesh, matrices, RenderLayer.getEnergySwirl(texture, u, v),
                GameRenderer.getRenderTypeEnergySwirlProgram(), pose);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    private static net.minecraft.client.gl.ShaderProgram dissolveProgram;
    public static void setDissolveProgram(net.minecraft.client.gl.ShaderProgram program) {
        dissolveProgram = program;
    }
    public static void dissolve(GpuMesh mesh, MatrixStack matrices, Identifier texture,
                                float progress, float er, float eg, float eb,
                                PartPose pose) {
        if (dissolveProgram == null || progress >= 1.0f) {
            return;
        }
        RenderSystem.setShaderColor(er, eg, eb, progress);
        draw(mesh, matrices, RenderLayer.getEntityCutoutNoCull(texture),
                dissolveProgram, pose);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    private static void draw(GpuMesh mesh, MatrixStack matrices, RenderLayer layer,
                             net.minecraft.client.gl.ShaderProgram shader, PartPose pose) {
        if (mesh == null || shader == null) {
            return;
        }
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f globalView = new Matrix4f(RenderSystem.getModelViewMatrix());
        layer.startDrawing();
        for (int i = 0; i < mesh.parts.size(); i++) {
            GpuMesh.Part part = mesh.parts.get(i);
            matrices.push();
            if (pose != null) {
                pose.apply(matrices, part, i);
            }
            part.buffer.bind();
            part.buffer.draw(new Matrix4f(globalView).mul(matrices.peek().getPositionMatrix()),
                    projection, shader);
            matrices.pop();
        }
        VertexBuffer.unbind();
        layer.endDrawing();
    }
}
