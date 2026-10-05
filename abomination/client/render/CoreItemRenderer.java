package com.seafle.abomination.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.client.mesh.GpuMesh;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
public class CoreItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private final String id;
    private final Identifier texture;
    public CoreItemRenderer(String id) {
        this.id = id;
        this.texture = new Identifier(Abomination.MOD_ID, folderOf(id) + id + ".png");
    }
    public static String folderOf(String id) {
        return id.endsWith("_core") ? "textures/item/" : "textures/entity/";
    }
    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        GpuMesh mesh = CoreMeshes.get(this.id);
        if (mesh == null || mesh.parts.isEmpty()) {
            return;
        }
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        RenderLayer layer = RenderLayer.getEntitySolid(this.texture);
        var shader = GameRenderer.getRenderTypeEntitySolidProgram();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
                .mul(matrices.peek().getPositionMatrix());
        float b = brightness(light);
        RenderSystem.setShaderColor(b, b, b, 1.0f);
        layer.startDrawing();
        GpuMesh.Part part = mesh.parts.get(0);
        part.buffer.bind();
        part.buffer.draw(modelView, projection, shader);
        VertexBuffer.unbind();
        layer.endDrawing();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        matrices.pop();
    }
    private static float brightness(int light) {
        int block = net.minecraft.client.render.LightmapTextureManager.getBlockLightCoordinates(light);
        int sky = net.minecraft.client.render.LightmapTextureManager.getSkyLightCoordinates(light);
        float level = Math.max(block, sky) / 15.0f;
        return 0.35f + 0.65f * level;
    }
}
