package com.seafle.abomination.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seafle.abomination.Abomination;
import com.seafle.abomination.block.TrophyBlock;
import com.seafle.abomination.block.TrophyBlockEntity;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.client.mesh.GpuMesh;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
public class TrophyRenderer implements BlockEntityRenderer<TrophyBlockEntity> {
    private static final Identifier TEXTURE =
            new Identifier(Abomination.MOD_ID, "textures/entity/trophy.png");
    private static final Identifier AURA =
            new Identifier(Abomination.MOD_ID, "textures/entity/shield_aura.png");
    private static final float SCALE = 0.95f;
    public TrophyRenderer(BlockEntityRendererFactory.Context ctx) {
    }
    @Override
    public void render(TrophyBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider consumers, int light, int overlay) {
        GpuMesh mesh = CoreMeshes.get("trophy");
        if (mesh == null || be.getWorld() == null) {
            return;
        }
        float t = (be.getWorld().getTime() % 100000L) + tickDelta;
        matrices.push();
        matrices.translate(0.5, 0.02, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                -be.getCachedState().get(TrophyBlock.FACING).asRotation()));
        matrices.scale(SCALE, SCALE, SCALE);
        float breath = MathHelper.sin(t * 0.018f) * 0.012f;
        matrices.translate(0.0, 0.5, 0.0);
        matrices.scale(1.0f + breath, 1.0f - breath * 0.6f, 1.0f + breath);
        matrices.translate(0.0, -0.5, 0.0);
        float b = SplitRenderer.brightness(light);
        RenderSystem.setShaderColor(b, b, b, 1.0f);
        MeshDraw.solid(mesh, matrices, TEXTURE, null);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        MeshDraw.swirl(mesh, matrices, AURA,
                (t * 0.004f) % 1.0f, (t * 0.003f) % 1.0f,
                0.45f, 0.85f, 1.0f, 0.30f, null);
        matrices.pop();
    }
}
