package com.seafle.abomination.client.ward;
import net.minecraft.util.math.MathHelper;
public final class SummonState {
    private SummonState() {}
    public static volatile boolean active = false;
    public static volatile double x, y, z;
    public static volatile float radius = 26f;
    public static volatile long gatherMs = 3500L;
    public static volatile long holdMs = 1200L;
    public static volatile long revealMs = 3600L;
    public static void begin(double x, double y, double z, float radius,
                             int gatherMs, int holdMs, int revealMs) {
        SummonState.x = x;
        SummonState.y = y;
        SummonState.z = z;
        SummonState.radius = radius;
        SummonState.gatherMs = gatherMs;
        SummonState.holdMs = holdMs;
        SummonState.revealMs = revealMs;
        SummonState.elapsedMs = 0L;
        SummonState.active = true;
    }
    public static void stop() {
        active = false;
        elapsedMs = 0L;
    }
    private static volatile long elapsedMs;
    public static void tick() {
        if (active) {
            elapsedMs += 50L;
        }
    }
    private static long elapsed() {
        float d = net.minecraft.client.MinecraftClient.getInstance().getTickDelta();
        return elapsedMs + (long) (d * 50.0f);
    }
    public static float progress() {
        if (!active) return 0f;
        long e = elapsed();
        if (e < gatherMs) {
            return MathHelper.clamp(e / (float) gatherMs, 0f, 1f);
        }
        long fadeStart = gatherMs + holdMs + revealMs;
        if (e >= fadeStart) {
            active = false;
            return 0f;
        }
        long r = e - gatherMs - holdMs;
        if (r <= 0) return 1f;
        return 1f - MathHelper.clamp(r / (float) revealMs, 0f, 1f);
    }
    public static float reveal() {
        if (!active) return 0f;
        long r = elapsed() - gatherMs - holdMs;
        if (r <= 0) return 0f;
        return MathHelper.clamp(r / (float) revealMs, 0f, 1f);
    }
    public static float radiusNow() {
        float g = MathHelper.clamp(elapsed() / (float) gatherMs, 0f, 1f);
        return radius * (0.25f + 0.75f * g) * (1.0f + reveal() * 0.55f);
    }
}
