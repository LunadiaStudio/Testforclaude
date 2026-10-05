package com.seafle.abomination.client.fx;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;
public final class AbomFx {
    private AbomFx() {}
    private static volatile long clock;
    public static void tick() {
        clock++;
    }
    public static float now() {
        return clock + net.minecraft.client.MinecraftClient.getInstance().getTickDelta();
    }
    public abstract static class Fx {
        private final float startTick = now();
        final float life;
        int anchorId = -1;
        Vec3d anchorOffset = Vec3d.ZERO;
        boolean dieWithAnchor;
        protected Fx(float lifeTicks) {
            this.life = lifeTicks;
        }
        public Fx anchor(int id, Vec3d offset) {
            this.anchorId = id;
            this.anchorOffset = offset;
            return this;
        }
        public Fx dieWithAnchor() {
            this.dieWithAnchor = true;
            return this;
        }
        protected final Vec3d origin(Vec3d fallback) {
            if (this.anchorId < 0) {
                return fallback;
            }
            var world = net.minecraft.client.MinecraftClient.getInstance().world;
            if (world == null) {
                return fallback;
            }
            var e = entity();
            if (e == null) {
                return fallback;
            }
            float td = net.minecraft.client.MinecraftClient.getInstance()
                    .getTickDelta();
            return new Vec3d(
                    net.minecraft.util.math.MathHelper.lerp(td, e.prevX, e.getX()),
                    net.minecraft.util.math.MathHelper.lerp(td, e.prevY, e.getY()),
                    net.minecraft.util.math.MathHelper.lerp(td, e.prevZ, e.getZ()))
                    .add(this.anchorOffset);
        }
        protected final float age() {
            return now() - this.startTick;
        }
        final boolean dead() {
            if (this.age() > this.life) {
                return true;
            }
            return this.dieWithAnchor && this.anchorId >= 0 && entity() == null;
        }
        private net.minecraft.entity.Entity entity() {
            var world = net.minecraft.client.MinecraftClient.getInstance().world;
            return world == null ? null : world.getEntityById(this.anchorId);
        }
        abstract void build(BufferBuilder bb, Matrix4f m, Vec3d cam);
    }
    private static final List<Fx> LIST = new ArrayList<>();
    private static final String T = "textures/entity/";
    static final net.minecraft.util.Identifier GLOW =
            new net.minecraft.util.Identifier(com.seafle.abomination.Abomination.MOD_ID,
                    T + "fx_glow.png");
    static final net.minecraft.util.Identifier RING =
            new net.minecraft.util.Identifier(com.seafle.abomination.Abomination.MOD_ID,
                    T + "fx_ring.png");
    static final net.minecraft.util.Identifier SMOKE =
            new net.minecraft.util.Identifier(com.seafle.abomination.Abomination.MOD_ID,
                    T + "fx_smoke.png");
    static final net.minecraft.util.Identifier BRAND =
            new net.minecraft.util.Identifier(com.seafle.abomination.Abomination.MOD_ID,
                    T + "rune_mark.png");
    private static final net.minecraft.util.Identifier[] PASSES =
            { null, GLOW, RING, SMOKE, BRAND };
    static net.minecraft.util.Identifier pass;
    private static boolean solid() {
        return pass == null;
    }
    private static boolean on(net.minecraft.util.Identifier tex) {
        return tex.equals(pass);
    }
    static Vec3d camRight = new Vec3d(1, 0, 0);
    static Vec3d camUp = new Vec3d(0, 1, 0);
    public static void add(Fx fx) {
        if (LIST.size() > 512) {
            LIST.remove(0);
        }
        LIST.add(fx);
    }
    public static void clear() {
        LIST.clear();
    }
    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ctx -> {
            LIST.removeIf(Fx::dead);
            if (LIST.isEmpty()) {
                return;
            }
            Vec3d cam = ctx.camera().getPos();
            var rv = ctx.camera().getHorizontalPlane();
            var uv = ctx.camera().getVerticalPlane();
            Vec3d fwd = new Vec3d(rv.x(), rv.y(), rv.z());
            camUp = new Vec3d(uv.x(), uv.y(), uv.z());
            camRight = fwd.crossProduct(camUp).normalize();
            Matrix4f m = new Matrix4f(ctx.matrixStack().peek().getPositionMatrix());
            net.minecraft.client.MinecraftClient.getInstance()
                    .getFramebuffer().beginWrite(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(
                    com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder bb = tess.getBuffer();
            for (net.minecraft.util.Identifier tex : PASSES) {
                pass = tex;
                if (tex == null) {
                    RenderSystem.setShader(GameRenderer::getPositionColorProgram);
                    bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                } else {
                    RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
                    RenderSystem.setShaderTexture(0, tex);
                    bb.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                }
                for (Fx f : LIST) {
                    try {
                        f.build(bb, m, cam);
                    } catch (Exception ignored) {
                    }
                }
                BufferBuilder.BuiltBuffer built = bb.endNullable();
                if (built != null) {
                    BufferRenderer.drawWithGlobalProgram(built);
                }
            }
            pass = null;
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        });
    }
    static void v(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d p,
                  float r, float g, float b, float a) {
        bb.vertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .color(r, g, b, a).next();
    }
    static void tv(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d p, float u, float v,
                   float r, float g, float b, float a) {
        bb.vertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .texture(u, v).color(r, g, b, a).next();
    }
    static void texSector(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, Vec3d n,
                          double radius, double from, double to, int segs,
                          float r, float g, float b, float a) {
        if (a <= 0.002f || radius <= 0.0 || to <= from) {
            return;
        }
        Vec3d[] uv = basis(n);
        for (int i = 0; i < segs; i++) {
            double a0 = from + (to - from) * i / segs;
            double a1 = from + (to - from) * (i + 1) / segs;
            Vec3d d0 = uv[0].multiply(Math.cos(a0)).add(uv[1].multiply(Math.sin(a0)));
            Vec3d d1 = uv[0].multiply(Math.cos(a1)).add(uv[1].multiply(Math.sin(a1)));
            Vec3d p0 = c.add(d0.multiply(radius));
            Vec3d p1 = c.add(d1.multiply(radius));
            tv(bb, m, cam, c, 0.5f, 0.5f, r, g, b, a);
            tv(bb, m, cam, p0, (float) (0.5 + 0.5 * Math.cos(a0)),
                    (float) (0.5 + 0.5 * Math.sin(a0)), r, g, b, a);
            tv(bb, m, cam, p1, (float) (0.5 + 0.5 * Math.cos(a1)),
                    (float) (0.5 + 0.5 * Math.sin(a1)), r, g, b, a);
            tv(bb, m, cam, c, 0.5f, 0.5f, r, g, b, a);
        }
    }
    static void quad(BufferBuilder bb, Matrix4f m, Vec3d cam,
                     Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3,
                     float r, float g, float b, float a) {
        if (a <= 0.001f || !solid()) {
            return;
        }
        v(bb, m, cam, p0, r, g, b, a);
        v(bb, m, cam, p1, r, g, b, a);
        v(bb, m, cam, p2, r, g, b, a);
        v(bb, m, cam, p3, r, g, b, a);
    }
    static void quad2(BufferBuilder bb, Matrix4f m, Vec3d cam,
                      Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3,
                      float r0, float g0, float b0, float a0,
                      float r1, float g1, float b1, float a1) {
        if (!solid()) {
            return;
        }
        v(bb, m, cam, p0, r0, g0, b0, a0);
        v(bb, m, cam, p1, r1, g1, b1, a1);
        v(bb, m, cam, p2, r1, g1, b1, a1);
        v(bb, m, cam, p3, r0, g0, b0, a0);
    }
    static Vec3d[] basis(Vec3d n) {
        Vec3d up = Math.abs(n.y) > 0.95 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d u = n.crossProduct(up).normalize();
        Vec3d w = n.crossProduct(u).normalize();
        return new Vec3d[] { u, w };
    }
    static void sector(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, Vec3d n,
                       double inner, double outer, double from, double to, int sides,
                       float r, float g, float b, float a) {
        if (a <= 0.001f || outer <= 0.0 || to <= from || !solid()) {
            return;
        }
        Vec3d[] uv = basis(n);
        for (int i = 0; i < sides; i++) {
            double a0 = from + (to - from) * i / sides;
            double a1 = from + (to - from) * (i + 1) / sides;
            Vec3d d0 = uv[0].multiply(Math.cos(a0)).add(uv[1].multiply(Math.sin(a0)));
            Vec3d d1 = uv[0].multiply(Math.cos(a1)).add(uv[1].multiply(Math.sin(a1)));
            quad(bb, m, cam,
                    c.add(d0.multiply(inner)), c.add(d0.multiply(outer)),
                    c.add(d1.multiply(outer)), c.add(d1.multiply(inner)),
                    r, g, b, a);
        }
    }
    static void ring(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, Vec3d n,
                     double inner, double outer, int sides,
                     float r, float g, float b, float a) {
        sector(bb, m, cam, c, n, inner, outer, 0.0, Math.PI * 2, sides, r, g, b, a);
    }
    static void annulus(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, Vec3d n,
                        double inner, double outer, int sides, float roll,
                        float r, float g, float b, float a) {
        if (a <= 0.001f || outer <= 0.0 || !solid()) {
            return;
        }
        Vec3d[] uv = basis(n);
        for (int i = 0; i < sides; i++) {
            double a0 = i * Math.PI * 2 / sides + roll;
            double a1 = (i + 1) * Math.PI * 2 / sides + roll;
            Vec3d d0 = uv[0].multiply(Math.cos(a0)).add(uv[1].multiply(Math.sin(a0)));
            Vec3d d1 = uv[0].multiply(Math.cos(a1)).add(uv[1].multiply(Math.sin(a1)));
            quad2(bb, m, cam,
                    c.add(d0.multiply(inner)), c.add(d0.multiply(outer)),
                    c.add(d1.multiply(outer)), c.add(d1.multiply(inner)),
                    r, g, b, a, r, g, b, 0.0f);
        }
    }
    interface Profile {
        double radius(double f);
        void colour(double f, float[] out);
    }
    static void tube(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d from, Vec3d to,
                     int segs, int sides, double until, Profile p) {
        if (!solid()) {
            return;
        }
        Vec3d dir = to.subtract(from);
        double len = dir.length();
        if (len < 1.0E-4) {
            return;
        }
        Vec3d n = dir.multiply(1.0 / len);
        Vec3d[] uv = basis(n);
        float[] c0 = new float[4];
        float[] c1 = new float[4];
        for (int s = 0; s < segs; s++) {
            double f0 = s / (double) segs;
            double f1 = (s + 1) / (double) segs;
            if (f0 > until) {
                break;
            }
            if (f1 > until) {
                f1 = until;
            }
            double r0 = p.radius(f0);
            double r1 = p.radius(f1);
            p.colour(f0, c0);
            p.colour(f1, c1);
            Vec3d a0 = from.add(dir.multiply(f0));
            Vec3d a1 = from.add(dir.multiply(f1));
            for (int k = 0; k < sides; k++) {
                double t0 = k * Math.PI * 2 / sides;
                double t1 = (k + 1) * Math.PI * 2 / sides;
                Vec3d d0 = uv[0].multiply(Math.cos(t0)).add(uv[1].multiply(Math.sin(t0)));
                Vec3d d1 = uv[0].multiply(Math.cos(t1)).add(uv[1].multiply(Math.sin(t1)));
                v(bb, m, cam, a0.add(d0.multiply(r0)), c0[0], c0[1], c0[2], c0[3]);
                v(bb, m, cam, a1.add(d0.multiply(r1)), c1[0], c1[1], c1[2], c1[3]);
                v(bb, m, cam, a1.add(d1.multiply(r1)), c1[0], c1[1], c1[2], c1[3]);
                v(bb, m, cam, a0.add(d1.multiply(r0)), c0[0], c0[1], c0[2], c0[3]);
            }
        }
    }
    static void flare(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, double size,
                      float r, float g, float b, float a) {
        if (!on(GLOW) || a <= 0.002f || size <= 0.0) {
            return;
        }
        sprite(bb, m, cam, c, size, 0.0f, r, g, b, a);
    }
    static void sprite(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, double size,
                       float roll, float r, float g, float b, float a) {
        if (a <= 0.002f || size <= 0.0) {
            return;
        }
        double cs = Math.cos(roll);
        double sn = Math.sin(roll);
        Vec3d ax = camRight.multiply(cs).add(camUp.multiply(sn)).multiply(size);
        Vec3d ay = camRight.multiply(-sn).add(camUp.multiply(cs)).multiply(size);
        tv(bb, m, cam, c.subtract(ax).subtract(ay), 0.0f, 0.0f, r, g, b, a);
        tv(bb, m, cam, c.subtract(ax).add(ay), 0.0f, 1.0f, r, g, b, a);
        tv(bb, m, cam, c.add(ax).add(ay), 1.0f, 1.0f, r, g, b, a);
        tv(bb, m, cam, c.add(ax).subtract(ay), 1.0f, 0.0f, r, g, b, a);
    }
    static void puff(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, double size,
                     float roll, float r, float g, float b, float a) {
        if (!on(SMOKE)) {
            return;
        }
        sprite(bb, m, cam, c, size, roll, r, g, b, a);
    }
    static void decal(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, double radius,
                      float roll, float r, float g, float b, float a) {
        if (a <= 0.002f || radius <= 0.0) {
            return;
        }
        double cs = Math.cos(roll) * radius;
        double sn = Math.sin(roll) * radius;
        Vec3d ax = new Vec3d(cs, 0.0, sn);
        Vec3d az = new Vec3d(-sn, 0.0, cs);
        tv(bb, m, cam, c.subtract(ax).subtract(az), 0.0f, 0.0f, r, g, b, a);
        tv(bb, m, cam, c.subtract(ax).add(az), 0.0f, 1.0f, r, g, b, a);
        tv(bb, m, cam, c.add(ax).add(az), 1.0f, 1.0f, r, g, b, a);
        tv(bb, m, cam, c.add(ax).subtract(az), 1.0f, 0.0f, r, g, b, a);
    }
    static void shock(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, double radius,
                      float roll, float r, float g, float b, float a) {
        if (!on(RING)) {
            return;
        }
        decal(bb, m, cam, c, radius, roll, r, g, b, a);
    }
    static void brand(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d c, double radius,
                      double from, double to, int segs,
                      float r, float g, float b, float a) {
        if (!on(BRAND) || a <= 0.002f || radius <= 0.0 || to <= from) {
            return;
        }
        texSector(bb, m, cam, c, new Vec3d(0, 1, 0), radius, from, to, segs, r, g, b, a);
    }
    static void ribbon(BufferBuilder bb, Matrix4f m, Vec3d cam, Vec3d[] pts,
                       double width, float r, float g, float b, float a) {
        if (!on(GLOW)) {
            return;
        }
        for (int i = 0; i + 1 < pts.length; i++) {
            Vec3d p0 = pts[i];
            Vec3d p1 = pts[i + 1];
            Vec3d seg = p1.subtract(p0);
            if (seg.lengthSquared() < 1.0E-8) {
                continue;
            }
            Vec3d view = p0.subtract(cam);
            Vec3d side = seg.crossProduct(view);
            if (side.lengthSquared() < 1.0E-8) {
                continue;
            }
            side = side.normalize().multiply(width * 0.5);
            float f0 = 1.0f - i / (float) pts.length;
            float f1 = 1.0f - (i + 1) / (float) pts.length;
            tv(bb, m, cam, p0.subtract(side), 0.5f, 0.02f, r, g, b, a * f0);
            tv(bb, m, cam, p0.add(side), 0.5f, 0.98f, r, g, b, a * f0);
            tv(bb, m, cam, p1.add(side), 0.5f, 0.98f, r, g, b, a * f1);
            tv(bb, m, cam, p1.subtract(side), 0.5f, 0.02f, r, g, b, a * f1);
        }
    }
    static float hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0f;
    }
    static float ease(float x) {
        return MathHelper.clamp(x, 0.0f, 1.0f);
    }
}
