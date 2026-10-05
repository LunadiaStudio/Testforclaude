package com.seafle.abomination.client.fx;
import net.minecraft.util.math.MathHelper;
import java.util.HashMap;
import java.util.Map;
public final class BodyPulse {
    private BodyPulse() {}
    private static final float LIFE = 14.0f;
    private static final Map<Integer, Long> LAST = new HashMap<>();
    public static void hit(int entityId) {
        LAST.put(entityId, (long) AbomFx.now());
    }
    public static void clear() {
        LAST.clear();
    }
    public static float phase(int entityId) {
        Long t = LAST.get(entityId);
        if (t == null) {
            return -1.0f;
        }
        float age = AbomFx.now() - t;
        if (age > LIFE) {
            LAST.remove(entityId);
            return -1.0f;
        }
        return age;
    }
    public static float amount(float phase) {
        if (phase < 0.0f) {
            return 0.0f;
        }
        float f = phase / LIFE;
        float rise = MathHelper.clamp(phase / 2.0f, 0.0f, 1.0f);
        return rise * (1.0f - f) * (1.0f - f);
    }
}
