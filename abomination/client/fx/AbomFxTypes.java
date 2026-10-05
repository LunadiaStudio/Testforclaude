package com.seafle.abomination.client.fx;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
public final class AbomFxTypes {
    private AbomFxTypes() {}
    public static final class Beam extends AbomFx.Fx {
        private final Vec3d from;
        private final Vec3d to;
        public Beam(Vec3d from, Vec3d to) {
            super(34.0f);
            this.from = from;
            this.to = to;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            final Vec3d src = origin(this.from);
            final float t = age();
            final double grow = Math.min(1.0, t / 3.0);
            final float env = t < 2.0f
                    ? t / 2.0f
                    : (float) Math.max(0.0, Math.pow(1.0 - (t - 2.0f) / (this.life - 2.0f), 2.4));
            final float flick = 0.82f + 0.18f * MathHelper.sin(t * 3.1f);
            AbomFx.tube(bb, m, cam, src, this.to, 26, 14, grow, new AbomFx.Profile() {
                @Override
                public double radius(double f) {
                    double horn = 0.4 + 3.6 * Math.pow(f, 1.7);
                    double ripple = 1.0 + 0.26 * Math.sin(f * 34.0 - t * 2.4);
                    return horn * ripple * (0.55 + 0.45 * env);
                }
                @Override
                public void colour(double f, float[] o) {
                    o[0] = 0.20f;
                    o[1] = 0.86f;
                    o[2] = 0.98f;
                    o[3] = (float) (0.26 * env * flick * (0.3 + 0.7 * f));
                }
            });
            AbomFx.tube(bb, m, cam, src, this.to, 20, 10, grow, new AbomFx.Profile() {
                @Override
                public double radius(double f) {
                    return (0.12 + 0.95 * Math.pow(f, 1.5))
                            * (1.0 + 0.18 * Math.sin(f * 50.0 - t * 4.0));
                }
                @Override
                public void colour(double f, float[] o) {
                    o[0] = 0.85f;
                    o[1] = 1.0f;
                    o[2] = 1.0f;
                    o[3] = 0.9f * env * flick;
                }
            });
            Vec3d dir = this.to.subtract(src);
            Vec3d n = dir.normalize();
            for (int k = 0; k < 7; k++) {
                double fr = ((t * 0.085) + k / 7.0) % 1.0;
                if (fr > grow) {
                    continue;
                }
                Vec3d c = src.add(dir.multiply(fr));
                double rr = 0.6 + 5.0 * Math.pow(fr, 1.4);
                AbomFx.annulus(bb, m, cam, c, n, rr * 0.72, rr * 1.55, 24,
                        (float) (t * 0.05 + k), 0.55f, 1.0f, 1.0f,
                        0.45f * env * (1.0f - (float) fr * 0.45f));
            }
            AbomFx.flare(bb, m, cam, src, 5.0 * env, 0.65f, 1.0f, 1.0f, 0.85f * env);
            if (grow >= 1.0) {
                float it = t - 3.0f;
                AbomFx.flare(bb, m, cam, this.to, 3.5 + it * 1.4, 0.85f, 1.0f, 1.0f,
                        Math.max(0.0f, env));
                AbomFx.annulus(bb, m, cam, this.to.add(0.0, 0.1, 0.0), new Vec3d(0, 1, 0),
                        it * 1.6, it * 1.6 + 2.4, 40, t * 0.04f,
                        0.4f, 0.95f, 1.0f, 0.55f * env);
            }
        }
    }
    public static final class Charge extends AbomFx.Fx {
        private final Vec3d at;
        private final float radius;
        public Charge(Vec3d at, float radius, int ticks) {
            super(ticks);
            this.at = at;
            this.radius = radius;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            Vec3d here = origin(this.at);
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float pull = f * f;
            float shake = f > 0.7f ? (f - 0.7f) / 0.3f : 0.0f;
            float jitter = shake * MathHelper.sin(t * 2.7f) * 0.35f;
            for (int i = 0; i < 64; i++) {
                float h0 = AbomFx.hash(i, 11);
                float h1 = AbomFx.hash(i, 27);
                float h2 = AbomFx.hash(i, 53);
                float local = (t * (0.045f + 0.05f * f) + h2) % 1.0f;
                double dist = this.radius * (1.0 - local) * (1.0 - pull * 0.3);
                double theta = h0 * Math.PI * 2 + local * 4.2;
                double phi = (h1 - 0.5) * Math.PI * 0.95;
                Vec3d[] pts = new Vec3d[6];
                for (int s2 = 0; s2 < pts.length; s2++) {
                    double dd = dist * (1.0 - s2 * 0.15);
                    double th = theta + s2 * 0.26;
                    pts[s2] = here.add(
                            Math.cos(th) * Math.cos(phi) * dd,
                            Math.sin(phi) * dd,
                            Math.sin(th) * Math.cos(phi) * dd);
                }
                float near = 1.0f - (float) (dist / Math.max(0.01, this.radius));
                float a = (0.35f + 0.65f * near) * (0.3f + 0.7f * f);
                AbomFx.ribbon(bb, m, cam, pts, 0.18 + 0.55 * near,
                        0.35f + 0.4f * near, 0.95f, 1.0f, a);
            }
            Vec3d n = AbomFx.camRight.crossProduct(AbomFx.camUp);
            for (int k = 0; k < 4; k++) {
                double fr = ((t * 0.05) + k * 0.25) % 1.0;
                double rr = this.radius * (1.0 - fr);
                AbomFx.ring(bb, m, cam, here, n, rr * 0.94, rr, 40,
                        0.4f, 0.95f, 1.0f, (float) (0.55 * fr) * (0.3f + 0.7f * f));
            }
            double core = (1.2 + 6.5 * pull) * (1.0 + jitter);
            AbomFx.flare(bb, m, cam, here, core, 0.75f, 1.0f, 1.0f,
                    0.4f + 0.6f * pull);
            AbomFx.flare(bb, m, cam, here, core * 0.45, 1.0f, 1.0f, 1.0f,
                    0.5f + 0.5f * pull);
            if (shake > 0.0f) {
                int seed = (int) (t * 0.5f);
                for (int i = 0; i < 10; i++) {
                    double th = AbomFx.hash(seed * 37 + i, 71) * Math.PI * 2;
                    double ph = (AbomFx.hash(seed * 37 + i, 97) - 0.5) * Math.PI;
                    double reach = core * (0.8 + 1.4 * AbomFx.hash(seed * 37 + i, 13));
                    Vec3d d = new Vec3d(Math.cos(th) * Math.cos(ph), Math.sin(ph),
                            Math.sin(th) * Math.cos(ph));
                    Vec3d[] pts = new Vec3d[4];
                    for (int s2 = 0; s2 < pts.length; s2++) {
                        double dd = reach * s2 / (pts.length - 1.0);
                        double wob = (AbomFx.hash(seed * 91 + i * 7 + s2, 5) - 0.5) * 0.5;
                        pts[s2] = here.add(d.multiply(dd))
                                .add(AbomFx.camRight.multiply(wob));
                    }
                    AbomFx.ribbon(bb, m, cam, pts, 0.22, 0.85f, 1.0f, 1.0f, 0.8f * shake);
                }
            }
        }
    }
    public static final class Rune extends AbomFx.Fx {
        private final Vec3d at;
        private final float radius;
        public Rune(Vec3d at, float radius, int ticks) {
            super(ticks);
            this.at = at.add(0.0, 0.05, 0.0);
            this.radius = radius;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            double r = this.radius * 1.45;
            float roll = t * 0.004f;
            float hot = MathHelper.clamp((t - (this.life - 5.0f)) / 5.0f, 0.0f, 1.0f);
            float cr = 0.40f + 0.60f * hot;
            float cg = 0.90f + 0.10f * hot;
            float cb = 0.95f;
            float fade = MathHelper.clamp(t / 4.0f, 0.0f, 1.0f);
            AbomFx.brand(bb, m, cam, this.at, r, 0.0, Math.PI * 2, 40,
                    cr, cg, cb, fade * 0.16f);
            double from = -Math.PI * 0.5;
            AbomFx.brand(bb, m, cam, this.at, r, from, from + Math.PI * 2 * f, 44,
                    cr, cg, cb, fade * (0.55f + 0.45f * hot));
            if (f > 0.02f && f < 0.99f) {
                double th = from + Math.PI * 2 * f;
                Vec3d head = this.at.add(Math.cos(th) * r * 0.78, 0.15,
                        Math.sin(th) * r * 0.78);
                AbomFx.flare(bb, m, cam, head, this.radius * 0.55,
                        0.85f, 1.0f, 1.0f, fade * 0.85f);
            }
            if (hot > 0.0f) {
                AbomFx.flare(bb, m, cam, this.at.add(0.0, 0.5, 0.0),
                        this.radius * (1.2 + 2.0 * hot), 0.8f, 1.0f, 1.0f, hot * 0.8f);
                AbomFx.shock(bb, m, cam, this.at, this.radius * (1.0 + 1.4 * hot), roll,
                        0.7f, 1.0f, 1.0f, hot * (1.0f - hot * 0.6f));
            }
        }
    }
    public static final class Impact extends AbomFx.Fx {
        private final Vec3d at;
        private final float power;
        public Impact(Vec3d at, float power) {
            super(30.0f);
            this.at = at.add(0.0, 0.05, 0.0);
            this.power = power;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            double p = this.power;
            if (t < 3.5f) {
                float k = 1.0f - t / 3.5f;
                AbomFx.flare(bb, m, cam, this.at.add(0.0, 0.4 * p, 0.0),
                        p * (1.4 + 2.6 * (1.0 - k)), 1.0f, 0.96f, 0.86f, k * 0.95f);
                AbomFx.flare(bb, m, cam, this.at.add(0.0, 0.4 * p, 0.0),
                        p * (0.5 + 1.0 * (1.0 - k)), 1.0f, 1.0f, 1.0f, k);
            }
            shockAt(bb, m, cam, p, t, 0.0f);
            shockAt(bb, m, cam, p * 0.68, t, 3.5f);
            float dust = (1.0f - f) * (1.0f - f);
            for (int i = 0; i < 9; i++) {
                float h0 = AbomFx.hash(i, 3);
                float h1 = AbomFx.hash(i, 17);
                double a = h0 * Math.PI * 2;
                double rr = p * (0.7 + 2.4 * (float) Math.pow(f, 0.5)) * (0.6 + 0.6 * h1);
                double hh = p * (0.4 + 1.8 * (float) Math.pow(f, 0.45)) * (0.4 + h0);
                Vec3d q = this.at.add(Math.cos(a) * rr, hh, Math.sin(a) * rr);
                AbomFx.puff(bb, m, cam, q, p * (0.9 + 1.6 * f) * (0.6 + 0.7 * h1),
                        h1 * 6.28f + t * 0.02f,
                        0.40f, 0.36f, 0.38f, 0.55f * dust);
            }
        }
        private void shockAt(BufferBuilder bb, Matrix4f m, Vec3d cam,
                             double p, float t, float delay) {
            float lt = t - delay;
            if (lt < 0.0f) {
                return;
            }
            float k = MathHelper.clamp(lt / 10.0f, 0.0f, 1.0f);
            float a = (1.0f - k) * (1.0f - k);
            double r = p * (0.5 + 4.6 * (float) Math.pow(k, 0.55));
            AbomFx.shock(bb, m, cam, this.at, r, delay * 0.3f,
                    1.0f, 0.86f + 0.14f * (1.0f - k), 0.62f, 0.95f * a);
        }
    }
    public static final class Burst extends AbomFx.Fx {
        private final Vec3d at;
        private final float power;
        public Burst(Vec3d at, float power) {
            super(20.0f);
            this.at = at;
            this.power = power;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            Vec3d here = origin(this.at);
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float fade = (1.0f - f) * (1.0f - f);
            Vec3d n = AbomFx.camRight.crossProduct(AbomFx.camUp);
            double r = this.power * (0.2 + 1.8 * (float) Math.pow(f, 0.5));
            AbomFx.annulus(bb, m, cam, here, n, r * 0.6, r, 40, t * 0.03f,
                    0.75f, 0.30f, 0.95f, 0.7f * fade);
            AbomFx.flare(bb, m, cam, here, this.power * (0.8 + 1.2 * f),
                    0.55f, 0.85f, 1.0f, 0.7f * fade);
            for (int i = 0; i < 26; i++) {
                float h0 = AbomFx.hash(i, 71);
                float h1 = AbomFx.hash(i, 97);
                double th = h0 * Math.PI * 2;
                double ph = (h1 - 0.5) * Math.PI;
                double reach = this.power * (0.8 + 2.2 * h1) * f;
                Vec3d d = new Vec3d(Math.cos(th) * Math.cos(ph), Math.sin(ph),
                        Math.sin(th) * Math.cos(ph));
                Vec3d[] pts = new Vec3d[4];
                for (int s = 0; s < pts.length; s++) {
                    pts[s] = here.add(d.multiply(reach * s / (pts.length - 1.0)));
                }
                AbomFx.ribbon(bb, m, cam, pts, 0.35 + 0.5 * h0,
                        0.7f, 0.45f, 1.0f, 0.6f * fade);
            }
        }
    }
    public static final class Tether extends AbomFx.Fx {
        private final Vec3d from;
        private final Vec3d to;
        public Tether(Vec3d from, Vec3d to, int ticks) {
            super(ticks);
            this.from = from;
            this.to = to;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            Vec3d src = origin(this.from);
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float a = (1.0f - f * f) * 0.8f;
            int seed = (int) (t * 0.5f);
            int n = 12;
            Vec3d[] pts = new Vec3d[n];
            Vec3d dir = this.to.subtract(src);
            Vec3d[] uv = AbomFx.basis(dir.normalize());
            for (int i = 0; i < n; i++) {
                double p = i / (n - 1.0);
                double amp = Math.sin(p * Math.PI) * dir.length() * 0.10;
                double ox = (AbomFx.hash(seed * 131 + i, 3) - 0.5) * 2.0 * amp;
                double oy = (AbomFx.hash(seed * 131 + i, 9) - 0.5) * 2.0 * amp;
                pts[i] = src.add(dir.multiply(p))
                        .add(uv[0].multiply(ox)).add(uv[1].multiply(oy));
            }
            AbomFx.ribbon(bb, m, cam, pts, 0.30, 0.72f, 0.38f, 1.0f, a);
            double pulse = (t * 0.09) % 1.0;
            Vec3d pp = src.add(dir.multiply(pulse));
            AbomFx.flare(bb, m, cam, pp, 0.9, 0.85f, 0.6f, 1.0f, a);
            AbomFx.flare(bb, m, cam, this.to, 1.3, 0.7f, 0.35f, 1.0f, a * 0.8f);
        }
    }
    public static final class Spike extends AbomFx.Fx {
        private final Vec3d root;
        private final Vec3d tip;
        private final float girth;
        public Spike(Vec3d root, Vec3d tip, float girth, int ticks) {
            super(ticks);
            this.root = root;
            this.tip = tip;
            this.girth = girth;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            float t = age();
            float grow = MathHelper.clamp(t / 4.0f, 0.0f, 1.0f);
            grow = 1.0f - (1.0f - grow) * (1.0f - grow);
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float sink = f > 0.8f ? (f - 0.8f) / 0.2f : 0.0f;
            float a = 1.0f - sink;
            if (a <= 0.0f) {
                return;
            }
            Vec3d end = this.root.add(this.tip.subtract(this.root).multiply(grow * (1.0f - sink)));
            final float gg = this.girth;
            AbomFx.tube(bb, m, cam, this.root.subtract(new Vec3d(0, 0.5, 0)), end,
                    6, 7, 1.0, new AbomFx.Profile() {
                        @Override
                        public double radius(double x) {
                            return gg * (1.0 - x) * (1.0 - x * 0.35);
                        }
                        @Override
                        public void colour(double x, float[] o) {
                            o[0] = 0.20f + 0.55f * (float) x;
                            o[1] = 0.55f + 0.42f * (float) x;
                            o[2] = 0.52f + 0.45f * (float) x;
                            o[3] = a * (0.55f + 0.45f * (float) x);
                        }
                    });
            if (t < 6.0f) {
                float k = 1.0f - t / 6.0f;
                AbomFx.annulus(bb, m, cam, this.root.add(0.0, 0.06, 0.0), new Vec3d(0, 1, 0),
                        gg * (1.0 + 3.0 * (1.0 - k)), gg * (2.2 + 4.0 * (1.0 - k)), 20, 0.0f,
                        0.45f, 0.42f, 0.40f, 0.5f * k);
            }
        }
    }
    public static final class AirBurst extends AbomFx.Fx {
        private final Vec3d at;
        private final float power;
        public AirBurst(Vec3d at, float power) {
            super(18.0f);
            this.at = at;
            this.power = power;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float fade = (1.0f - f) * (1.0f - f);
            AbomFx.flare(bb, m, cam, this.at, this.power * (0.9 + 2.2 * f),
                    1.0f, 0.94f, 0.80f, 0.9f * fade);
            AbomFx.flare(bb, m, cam, this.at, this.power * (0.4 + 0.8 * f),
                    1.0f, 1.0f, 1.0f, fade);
            for (int i = 0; i < 10; i++) {
                float h0 = AbomFx.hash(i, 131);
                float h1 = AbomFx.hash(i, 197);
                double th = h0 * Math.PI * 2;
                double ph = (h1 - 0.5) * Math.PI;
                double reach = this.power * (1.0 + 2.6 * h1) * (float) Math.pow(f, 0.55);
                Vec3d d = new Vec3d(Math.cos(th) * Math.cos(ph), Math.sin(ph),
                        Math.sin(th) * Math.cos(ph));
                AbomFx.puff(bb, m, cam, this.at.add(d.multiply(reach)),
                        this.power * (0.5 + 0.9 * h0) * (0.6 + f),
                        h0 * 6.28f, 0.45f, 0.40f, 0.42f, 0.6f * fade);
            }
        }
    }
    public static final class Crack extends AbomFx.Fx {
        private final Vec3d at;
        private final float radius;
        public Crack(Vec3d at, float radius) {
            super(40.0f);
            this.at = at.add(0.0, 0.04, 0.0);
            this.radius = radius;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            float t = age();
            float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float creep = MathHelper.clamp(t / 8.0f, 0.0f, 1.0f);
            creep = 1.0f - (1.0f - creep) * (1.0f - creep);
            float a = (1.0f - f) * (1.0f - f);
            AbomFx.brand(bb, m, cam, this.at, this.radius * creep,
                    0.0, Math.PI * 2, 36, 1.0f, 0.52f, 0.26f, a * 0.85f);
            AbomFx.brand(bb, m, cam, this.at, this.radius * creep * 0.62,
                    0.6, 0.6 + Math.PI * 2, 30, 1.0f, 0.72f, 0.38f, a * 0.55f);
            if (creep < 1.0f) {
                for (int i = 0; i < 6; i++) {
                    double th = AbomFx.hash(i, 41) * Math.PI * 2;
                    double rr = this.radius * creep * (0.75 + 0.25 * AbomFx.hash(i, 61));
                    AbomFx.flare(bb, m, cam,
                            this.at.add(Math.cos(th) * rr, 0.25, Math.sin(th) * rr),
                            this.radius * 0.12, 1.0f, 0.7f, 0.35f, a * (1.0f - creep));
                }
            }
        }
    }
    public static final class RamPath extends AbomFx.Fx {
        private final Vec3d at;
        private final Vec3d dir;
        private final float radius;
        public RamPath(Vec3d at, Vec3d aim, float radius, int ticks) {
            super(ticks);
            this.at = at.add(0.0, 0.06, 0.0);
            Vec3d d = aim.subtract(at).multiply(1.0, 0.0, 1.0);
            this.dir = d.lengthSquared() < 1.0E-4 ? new Vec3d(0, 0, 1) : d.normalize();
            this.radius = radius;
        }
        @Override
        void build(BufferBuilder bb, Matrix4f m, Vec3d cam) {
            final float t = age();
            final float f = MathHelper.clamp(t / this.life, 0.0f, 1.0f);
            float rate = 3.0f + 9.0f * f;
            boolean warm = ((int) (t / Math.max(1.0f, 20.0f / rate))) % 2 == 0;
            float cr = 1.0f;
            float cg = warm ? 0.85f : 0.10f;
            float cb = 0.06f;
            float fade = MathHelper.clamp(t / 3.0f, 0.0f, 1.0f);
            float full = MathHelper.TAU;
            AbomFx.brand(bb, m, cam, this.at, this.radius, 0.0, full, 48,
                    cr, cg, cb, fade * 0.30f);
            AbomFx.brand(bb, m, cam, this.at, this.radius * 0.66, 0.0, full, 40,
                    cr, cg, cb, fade * 0.42f);
            Vec3d up = this.dir;
            Vec3d side = new Vec3d(-up.z, 0.0, up.x);
            double r = this.radius;
            double halfW = r * 0.085;
            bar(bb, m, cam, this.at, up, side, r * 0.10, r * 0.44, halfW,
                    cr, cg, cb, fade * 0.95f);
            bar(bb, m, cam, this.at, up, side, -r * 0.34, -r * 0.14, halfW,
                    cr, cg, cb, fade * 0.95f);
        }
        private static void bar(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c,
                                Vec3d up, Vec3d side, double a, double b, double halfW,
                                float r, float g, float bl, float al) {
            Vec3d p0 = c.add(up.multiply(a)).add(side.multiply(-halfW));
            Vec3d p1 = c.add(up.multiply(a)).add(side.multiply(halfW));
            Vec3d p2 = c.add(up.multiply(b)).add(side.multiply(halfW));
            Vec3d p3 = c.add(up.multiply(b)).add(side.multiply(-halfW));
            AbomFx.quad(bb, m, cam, p0, p1, p2, p3, r, g, bl, al);
        }
    }
}
