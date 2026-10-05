package com.seafle.abomination.client.ward;
public final class WardState {
    private WardState() {}
    public static volatile boolean active = false;
    public static volatile double x, y, z;
    public static volatile float radius = 56f;
    public static volatile float height = 170f;
    public static volatile int pillars = 20;
    public static volatile boolean gloomOn = true;
    public static volatile long chargeMs = 1600L;
    public static volatile long riseMs = 2600L;
    public static volatile long linkMs = 900L;
    public static volatile long expandMs = 1800L;
    public static volatile long holdMs = 22000L;
    public static volatile long collapseMs = 1800L;
    public static void begin(double x, double y, double z, float radius, float height,
                             int pillars, int chargeMs, int riseMs, int linkMs,
                             int expandMs, int holdMs, int collapseMs, boolean gloom) {
        WardState.x = x;
        WardState.y = y;
        WardState.z = z;
        WardState.radius = radius;
        WardState.height = height;
        WardState.pillars = pillars;
        WardState.gloomOn = gloom;
        WardState.chargeMs = chargeMs;
        WardState.riseMs = riseMs;
        WardState.linkMs = linkMs;
        WardState.expandMs = expandMs;
        WardState.holdMs = holdMs;
        WardState.collapseMs = collapseMs;
        WardState.elapsedMs = 0L;
        WardState.active = true;
    }
    public static void breakNow() {
        if (!active) {
            return;
        }
        elapsedMs = chargeMs + riseMs + linkMs + expandMs + holdMs;
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
    private static float phase(long from, long len) {
        if (!active) return 0f;
        long e = elapsed() - from;
        if (e <= 0) return 0f;
        return e >= len ? 1f : e / (float) len;
    }
    public static float charge() {
        return phase(0, chargeMs);
    }
    public static float rise() {
        return phase(chargeMs, riseMs);
    }
    public static float link() {
        return phase(chargeMs + riseMs, linkMs);
    }
    public static float progress() {
        return phase(chargeMs + riseMs + linkMs, expandMs);
    }
    public static float fade() {
        if (!active) return 1f;
        long collapseStart = chargeMs + riseMs + linkMs + expandMs + holdMs;
        long e = elapsed();
        if (e < collapseStart) return 1f;
        long total = collapseStart + collapseMs;
        if (e >= total) {
            active = false;
            return 0f;
        }
        return 1f - (e - collapseStart) / (float) collapseMs;
    }
    public static float gloom() {
        if (!active || !gloomOn) return 0f;
        return Math.min(charge(), fade());
    }
}
