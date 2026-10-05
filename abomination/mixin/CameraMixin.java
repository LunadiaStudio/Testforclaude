package com.seafle.abomination.mixin;
import com.seafle.abomination.client.fx.CameraShake;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    @Shadow
    public abstract float getYaw();
    @Shadow
    public abstract float getPitch();
    @Inject(method = "update", at = @At("TAIL"))
    private void abomination$shake(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                   boolean inverseView, float tickDelta, CallbackInfo ci) {
        float[] s = CameraShake.offset(tickDelta);
        if (s != null) {
            this.setRotation(this.getYaw() + s[1], this.getPitch() + s[0]);
        }
    }
}
