package com.seafle.abomination.client.render;
import com.seafle.abomination.entity.ThrownBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import com.seafle.abomination.Abomination;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.RenderLayers;
public class ThrownBlockRenderer extends EntityRenderer<ThrownBlockEntity> {
    private final BlockRenderManager blocks;
    private static final Identifier AURA =
            new Identifier(Abomination.MOD_ID, "textures/entity/shield_aura.png");
    public ThrownBlockRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.blocks = ctx.getBlockRenderManager();
        this.shadowRadius = 0.4f;
    }
    @Override
    public Identifier getTexture(ThrownBlockEntity entity) {
        return net.minecraft.client.texture.SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
    @Override
    public void render(ThrownBlockEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        BlockState state = entity.getBlockState();
        if (state.getRenderType() != BlockRenderType.MODEL) {
            return;
        }
        int id = entity.getId();
        float sx = 0.7f + (id % 7) * 0.31f;
        float sy = 0.9f + (id % 5) * 0.43f;
        float sz = 0.5f + (id % 11) * 0.19f;
        float rate = entity.isThrown() ? 4.5f : 1.15f;
        float t = entity.age + tickDelta;
        matrices.push();
        matrices.translate(0.0, 0.49, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * sy * rate));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * sx * rate));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(t * sz * rate));
        matrices.translate(-0.5, -0.5, -0.5);
        BakedModel model = this.blocks.getModel(state);
        this.blocks.getModelRenderer().render(entity.getWorld(), model, state,
                entity.getBlockPos(), matrices,
                consumers.getBuffer(RenderLayers.getMovingBlockLayer(state)),
                false, net.minecraft.util.math.random.Random.create(),
                state.getRenderingSeed(entity.getBlockPos()),
                OverlayTexture.DEFAULT_UV);
        float u = (t * 0.012f) % 1.0f;
        float v = (t * 0.008f) % 1.0f;
        float heat = entity.isThrown() ? 1.0f : 0.55f;
        matrices.push();
        float s2 = 1.03f;
        matrices.translate(0.5 - 0.5 * s2, 0.5 - 0.5 * s2, 0.5 - 0.5 * s2);
        matrices.scale(s2, s2, s2);
        VertexConsumer swirl = consumers.getBuffer(
                RenderLayer.getEnergySwirl(AURA, u, v));
        this.blocks.getModelRenderer().render(matrices.peek(), swirl, state, model,
                0.62f * heat, 0.34f * heat, 1.0f * heat, 15728640,
                OverlayTexture.DEFAULT_UV);
        matrices.pop();
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }
}
