package com.seafle.abomination.client.fx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
public final class CameraShake {
    private CameraShake() {}
    private static float x;
    private static float y;
    private static float prevX;
    private static float prevY;
    private static float power;
    private static int left;
    private static int total;
    private static final double RANGE = 70.0;
    public static void shake(float p, int ticks) {
        if (p <= power * remaining()) {
            return;
        }
        power = p;
        left = ticks;
        total = Math.max(1, ticks);
    }
    public static void shakeAt(Vec3d at, float p, int ticks) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        double d2 = mc.player.getPos().squaredDistanceTo(at);
        if (d2 > RANGE * RANGE) {
            return;
        }
        double f = 1.0 - Math.sqrt(d2) / RANGE;
        shake(p * (float) (f * f), ticks);
    }
    private static float remaining() {
        return total <= 0 ? 0.0f : left / (float) total;
    }
    public static void tick() {
        prevX = x;
        prevY = y;
        if (left <= 0) {
            x = 0.0f;
            y = 0.0f;
            power = 0.0f;
            return;
        }
        left--;
        float amp = power * remaining();
        java.util.Random r = java.util.concurrent.ThreadLocalRandom.current();
        x = (r.nextFloat() * 2.0f - 1.0f) * amp;
        y = (r.nextFloat() * 2.0f - 1.0f) * amp;
    }
    public static float[] offset(float tickDelta) {
        if (left <= 0 && Math.abs(x) < 0.001f && Math.abs(prevX) < 0.001f) {
            return null;
        }
        return new float[] {
                MathHelper.lerp(tickDelta, prevX, x),
                MathHelper.lerp(tickDelta, prevY, y)
        };
    }
    public static void stop() {
        x = 0.0f;
        y = 0.0f;
        prevX = 0.0f;
        prevY = 0.0f;
        left = 0;
        power = 0.0f;
    }
}
