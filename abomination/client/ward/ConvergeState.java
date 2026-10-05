package com.seafle.abomination.client.ward;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
public final class ConvergeState {
    private ConvergeState() {}
    public record Source(Vec3d from, String coreId) {}
    public static volatile boolean active = false;
    public static volatile Vec3d centre = Vec3d.ZERO;
    public static volatile List<Source> sources = new ArrayList<>();
    public static volatile long liftMs = 1400L;
    public static volatile long drawMs = 2600L;
    public static volatile long fuseMs = 900L;
    public static void begin(Vec3d centre, List<Source> sources,
                             int liftMs, int drawMs, int fuseMs) {
        ConvergeState.centre = centre;
        ConvergeState.sources = sources;
        ConvergeState.liftMs = liftMs;
        ConvergeState.drawMs = drawMs;
        ConvergeState.fuseMs = fuseMs;
        ConvergeState.elapsedMs = 0L;
        ConvergeState.active = true;
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
    public static float lift() {
        if (!active) return 0f;
        return MathHelper.clamp(elapsed() / (float) liftMs, 0f, 1f);
    }
    public static float draw() {
        if (!active) return 0f;
        long e = elapsed() - liftMs;
        if (e <= 0) return 0f;
        return MathHelper.clamp(e / (float) drawMs, 0f, 1f);
    }
    public static float fuse() {
        if (!active) return 0f;
        long e = elapsed() - liftMs - drawMs;
        if (e <= 0) return 0f;
        if (e >= fuseMs) {
            active = false;
            return 1f;
        }
        return e / (float) fuseMs;
    }
}
