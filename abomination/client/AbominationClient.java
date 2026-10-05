package com.seafle.abomination.client;
import com.seafle.abomination.client.mesh.CoreMeshes;
import com.seafle.abomination.client.mesh.MeshBakery;
import com.seafle.abomination.client.part.PartTags;
import com.seafle.abomination.client.ward.WardArcFx;
import com.seafle.abomination.client.ward.WardPillarFx;
import com.seafle.abomination.client.ward.ConvergeFx;
import com.seafle.abomination.client.ward.ConvergeState;
import com.seafle.abomination.client.ward.SummonShader;
import com.seafle.abomination.client.ward.SummonState;
import com.seafle.abomination.client.ward.WardShader;
import com.seafle.abomination.client.ward.WardState;
import com.seafle.abomination.net.WardPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.seafle.abomination.client.render.AbominationRenderer;
import com.seafle.abomination.client.render.AltarRenderer;
import com.seafle.abomination.client.render.CoreItemRenderer;
import com.seafle.abomination.init.AbomBlockEntities;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import com.seafle.abomination.init.AbomCores;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import com.seafle.abomination.init.AbomEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
public class AbominationClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(AbomEntities.ABOMINATION, AbominationRenderer::new);
        EntityRendererRegistry.register(AbomEntities.SPLIT,
                com.seafle.abomination.client.render.SplitRenderer::new);
        EntityRendererRegistry.register(AbomEntities.THROWN_BLOCK,
                com.seafle.abomination.client.render.ThrownBlockRenderer::new);
        EntityRendererRegistry.register(AbomEntities.SPIKE,
                com.seafle.abomination.client.render.SpikeRenderer::new);
        EntityRendererRegistry.register(AbomEntities.CLOT,
                com.seafle.abomination.client.render.ClotRenderer::new);
        EntityRendererRegistry.register(AbomEntities.BOMBER,
                com.seafle.abomination.client.render.BomberRenderer::new);
        EntityRendererRegistry.register(AbomEntities.FINAL,
                com.seafle.abomination.client.render.FinalRenderer::new);
        BlockEntityRendererRegistry.register(AbomBlockEntities.ALTAR, AltarRenderer::new);
        BlockEntityRendererRegistry.register(AbomBlockEntities.TROPHY,
                com.seafle.abomination.client.render.TrophyRenderer::new);
        BuiltinItemRendererRegistry.INSTANCE.register(
                Registries.ITEM.get(new Identifier(
                        com.seafle.abomination.Abomination.MOD_ID, "trophy")),
                new CoreItemRenderer("trophy"));
        net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT
                .register(ctx -> ctx.register(
                        new net.minecraft.util.Identifier(
                                com.seafle.abomination.Abomination.MOD_ID, "abom_dissolve"),
                        net.minecraft.client.render.VertexFormats
                                .POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                        program -> com.seafle.abomination.client.render.MeshDraw
                                .setDissolveProgram(program)));
        PartTags.load();
        for (String id : AbomCores.IDS) {
            BuiltinItemRendererRegistry.INSTANCE.register(
                    Registries.ITEM.get(new Identifier(com.seafle.abomination.Abomination.MOD_ID, id)),
                    new CoreItemRenderer(id));
        }
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            resetVisuals();
            MeshBakery.prebake();
            CoreMeshes.prebake(AbomCores.IDS);
            CoreMeshes.prebake(new String[]{"mobheart", "maelstrom", "amalgam",
                    "spike", "clot", "bomber", "final", "trophy"});
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MeshBakery.discard();
            CoreMeshes.discard();
            resetVisuals();
        });
        WardPillarFx.init();
        WardArcFx.init();
        WardShader.init();
        ConvergeFx.init();
        SummonShader.init();
        ClientPlayNetworking.registerGlobalReceiver(WardPackets.WARD_START,
                (client, handler, buf, sender) -> {
                    double wx = buf.readDouble();
                    double wy = buf.readDouble();
                    double wz = buf.readDouble();
                    float radius = buf.readFloat();
                    float height = buf.readFloat();
                    int pillars = buf.readVarInt();
                    int chargeMs = buf.readVarInt();
                    int riseMs = buf.readVarInt();
                    int linkMs = buf.readVarInt();
                    int expandMs = buf.readVarInt();
                    int holdMs = buf.readVarInt();
                    int collapseMs = buf.readVarInt();
                    boolean gloom = buf.readBoolean();
                    client.execute(() -> WardState.begin(wx, wy, wz, radius, height,
                            pillars, chargeMs, riseMs, linkMs, expandMs, holdMs,
                            collapseMs, gloom));
                });
        ClientPlayNetworking.registerGlobalReceiver(WardPackets.WARD_BREAK,
                (client, handler, buf, sender) -> client.execute(WardState::breakNow));
        ClientPlayNetworking.registerGlobalReceiver(WardPackets.SUMMON_START,
                (client, handler, buf, sender) -> {
                    double sx = buf.readDouble();
                    double sy = buf.readDouble();
                    double sz = buf.readDouble();
                    float sr = buf.readFloat();
                    int gather = buf.readVarInt();
                    int hold = buf.readVarInt();
                    int reveal = buf.readVarInt();
                    client.execute(() -> SummonState.begin(sx, sy, sz, sr, gather, hold, reveal));
                });
        ClientPlayNetworking.registerGlobalReceiver(WardPackets.CONVERGE_START,
                (client, handler, buf, sender) -> {
                    double gx = buf.readDouble();
                    double gy = buf.readDouble();
                    double gz = buf.readDouble();
                    int lift = buf.readVarInt();
                    int draw = buf.readVarInt();
                    int fuse = buf.readVarInt();
                    int n = buf.readVarInt();
                    java.util.List<ConvergeState.Source> src = new java.util.ArrayList<>(n);
                    for (int i = 0; i < n; i++) {
                        double sx = buf.readDouble();
                        double sy = buf.readDouble();
                        double sz = buf.readDouble();
                        String coreId = buf.readString();
                        src.add(new ConvergeState.Source(
                                new net.minecraft.util.math.Vec3d(sx, sy, sz), coreId));
                    }
                    client.execute(() -> ConvergeState.begin(
                            new net.minecraft.util.math.Vec3d(gx, gy, gz), src,
                            lift, draw, fuse));
                });
        com.seafle.abomination.client.fx.AbomFx.init();
        ClientPlayNetworking.registerGlobalReceiver(
                com.seafle.abomination.net.FxPackets.FX,
                (client, handler, buf, sender) -> {
                    int type = buf.readVarInt();
                    net.minecraft.util.math.Vec3d from = new net.minecraft.util.math.Vec3d(
                            buf.readDouble(), buf.readDouble(), buf.readDouble());
                    net.minecraft.util.math.Vec3d to = new net.minecraft.util.math.Vec3d(
                            buf.readDouble(), buf.readDouble(), buf.readDouble());
                    float p1 = buf.readFloat();
                    int p2 = buf.readVarInt();
                    int anchor = buf.readVarInt();
                    client.execute(() -> spawnFx(type, from, to, p1, p2, anchor));
                });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            watchWorld(client);
            WardState.tick();
            SummonState.tick();
            ConvergeState.tick();
            com.seafle.abomination.client.fx.AbomFx.tick();
            MeshBakery.tick();
            CoreMeshes.tick();
            com.seafle.abomination.client.fx.CameraShake.tick();
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT
                .register((handler, client) ->
                        com.seafle.abomination.client.fx.CameraShake.stop());
    }
    private static net.minecraft.client.world.ClientWorld lastWorld;
    private static void watchWorld(net.minecraft.client.MinecraftClient client) {
        net.minecraft.client.world.ClientWorld now = client.world;
        if (now != lastWorld) {
            lastWorld = now;
            resetVisuals();
        }
    }
    private static void resetVisuals() {
        WardState.stop();
        SummonState.stop();
        ConvergeState.stop();
        com.seafle.abomination.client.fx.AbomFx.clear();
        com.seafle.abomination.client.fx.BodyPulse.clear();
        com.seafle.abomination.client.fx.CameraShake.stop();
    }
    private static void spawnFx(int type, net.minecraft.util.math.Vec3d from,
                                net.minecraft.util.math.Vec3d to, float p1, int p2,
                                int anchor) {
        if (type == com.seafle.abomination.net.FxPackets.SHAKE) {
            com.seafle.abomination.client.fx.CameraShake.shakeAt(from, p1, p2);
            return;
        }
        if (type == com.seafle.abomination.net.FxPackets.BODY_RIPPLE) {
            com.seafle.abomination.client.fx.BodyPulse.hit(anchor);
            return;
        }
        com.seafle.abomination.client.fx.AbomFx.Fx fx = make(type, from, to, p1, p2);
        if (fx == null) {
            return;
        }
        if (anchor >= 0) {
            fx.anchor(anchor, from);
        }
        com.seafle.abomination.client.fx.AbomFx.add(fx);
    }
    private static com.seafle.abomination.client.fx.AbomFx.Fx make(
            int type, net.minecraft.util.math.Vec3d from,
            net.minecraft.util.math.Vec3d to, float p1, int p2) {
        return switch (type) {
            case com.seafle.abomination.net.FxPackets.SONIC_BEAM ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Beam(from, to);
            case com.seafle.abomination.net.FxPackets.SONIC_CHARGE ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Charge(from, p1, p2);
            case com.seafle.abomination.net.FxPackets.RUNE_MARK ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Rune(from, p1, p2);
            case com.seafle.abomination.net.FxPackets.IMPACT ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Impact(from, p1);
            case com.seafle.abomination.net.FxPackets.BODY_BURST ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Burst(from, p1);
            case com.seafle.abomination.net.FxPackets.TETHER ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Tether(from, to, p2);
            case com.seafle.abomination.net.FxPackets.SPIKE ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Spike(from, to, p1, p2);
            case com.seafle.abomination.net.FxPackets.GROUND_CRACK ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.Crack(from, p1);
            case com.seafle.abomination.net.FxPackets.RAM_PATH ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.RamPath(from, to, p1, p2);
            case com.seafle.abomination.net.FxPackets.AIR_BURST ->
                    new com.seafle.abomination.client.fx.AbomFxTypes.AirBurst(from, p1);
            default -> null;
        };
    }
}
