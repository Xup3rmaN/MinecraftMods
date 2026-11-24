package com.splegg.trajectory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Рисует траекторию в виде голограммы: светящаяся лента с бегущими импульсами и кольцо в точке падения. */
public final class HologramRenderer {
    private static final float CORE_WIDTH = 0.035f;
    private static final float GLOW_WIDTH = 0.12f;
    /** На каком расстоянии (в блоках) лента «сходится» от руки к реальной траектории. */
    private static final double HAND_BLEND_DISTANCE = 3.0;

    private HologramRenderer() {}

    public static void render(Matrix4f matrix, Camera camera, ClientPlayerEntity player, boolean shovelInOffhand,
                              TrajectorySimulator.Result result, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<Vec3d> raw = result.points();
        if (raw.size() < 2 || client.world == null) return;

        Vec3d cam = camera.getPos();
        float time = (client.world.getTime() + tickDelta) / 20f;
        List<Vec3d> pts = shiftStartToHand(raw, player, shovelInOffhand, tickDelta);

        HitResult hit = result.hit();
        int color = hit != null && hit.getType() == HitResult.Type.ENTITY
                ? TrajectoryConfig.entityHitColor : TrajectoryConfig.hologramColor;
        float r = ((color >> 16) & 0xFF) / 255f, g = ((color >> 8) & 0xFF) / 255f, b = (color & 0xFF) / 255f;
        // Лёгкое мерцание, как у голограммы
        float flicker = 0.85f + 0.15f * MathHelper.sin(time * 37f) * MathHelper.sin(time * 13f);

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buf = consumers.getBuffer(RenderLayer.getDebugQuads());

        double travelled = 0;
        for (int i = 0; i < pts.size() - 1; i++) {
            Vec3d a = pts.get(i), c = pts.get(i + 1);
            double segLen = a.distanceTo(c);
            // Плавное появление ленты у руки, чтобы она не закрывала обзор
            float fadeIn = (float) MathHelper.clamp(travelled / 1.2, 0, 1);
            // Бегущий импульс вдоль траектории
            float pulse = 0.5f + 0.5f * MathHelper.sin((float) (travelled * 1.6 - time * 12f));
            pulse = pulse * pulse * pulse;
            float coreA = (0.45f + 0.5f * pulse) * flicker * fadeIn;
            float glowA = (0.10f + 0.18f * pulse) * flicker * fadeIn;

            ribbon(buf, matrix, cam, a, c, GLOW_WIDTH, r, g, b, glowA);
            ribbon(buf, matrix, cam, a, c, CORE_WIDTH, lerp(r, 1f, 0.5f), lerp(g, 1f, 0.5f), lerp(b, 1f, 0.5f), coreA);
            travelled += segLen;
        }

        if (hit != null) {
            Vec3d normal = new Vec3d(0, 1, 0);
            Vec3d center = markerPosition(client.world, hit).add(0, 0.02, 0);
            float pulseR = 0.35f + 0.08f * MathHelper.sin(time * 6f);
            disc(buf, matrix, cam, center, normal, pulseR, r, g, b, 0.18f * flicker);
            ring(buf, matrix, cam, center, normal, pulseR, 0.05f, time * 2.5f, r, g, b, 0.85f * flicker, true);
            ring(buf, matrix, cam, center, normal, pulseR * 1.6f, 0.03f, -time * 1.5f, r, g, b, 0.5f * flicker, false);
        }

        consumers.draw(RenderLayer.getDebugQuads());
    }

    /** Маркер всегда лежит на верхней грани блока, даже если яйцо попадает в его бок. */
    private static Vec3d markerPosition(World world, HitResult hit) {
        Vec3d p = hit.getPos();
        if (!(hit instanceof BlockHitResult bhr)) return p;
        BlockPos bp = bhr.getBlockPos();
        VoxelShape shape = world.getBlockState(bp).getCollisionShape(world, bp);
        double top = shape.isEmpty() ? 1.0 : shape.getMax(Direction.Axis.Y);
        double x = MathHelper.clamp(p.x, bp.getX(), bp.getX() + 1.0);
        double z = MathHelper.clamp(p.z, bp.getZ(), bp.getZ() + 1.0);
        return new Vec3d(x, bp.getY() + top, z);
    }

    /** Сдвигает начало траектории из глаз в руку и плавно сводит к реальному пути. */
    private static List<Vec3d> shiftStartToHand(List<Vec3d> raw, ClientPlayerEntity player, boolean offhand, float tickDelta) {
        Vec3d forward = player.getRotationVec(tickDelta);
        Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0));
        if (right.lengthSquared() < 1e-6) right = new Vec3d(1, 0, 0);
        right = right.normalize();
        Vec3d up = right.crossProduct(forward).normalize();

        boolean rightHand = (player.getMainArm() == Arm.RIGHT) != offhand;
        double side = rightHand ? 1 : -1;
        Vec3d handOffset = right.multiply(0.35 * side).add(up.multiply(-0.2)).add(forward.multiply(0.3));

        List<Vec3d> out = new ArrayList<>(raw.size() + 8);
        out.add(raw.get(0).add(handOffset));
        double dist = 0;
        for (int i = 1; i < raw.size(); i++) {
            Vec3d a = raw.get(i - 1), c = raw.get(i);
            double len = a.distanceTo(c);
            // Дробим первые отрезки, чтобы изгиб от руки был плавным
            int sub = dist < HAND_BLEND_DISTANCE ? 6 : 1;
            for (int s = 1; s <= sub; s++) {
                double t = (double) s / sub;
                double d = dist + len * t;
                double k = Math.max(0, 1 - d / HAND_BLEND_DISTANCE);
                k = k * k * (3 - 2 * k);
                out.add(a.lerp(c, t).add(handOffset.multiply(k)));
            }
            dist += len;
        }
        return out;
    }

    /** Полоса между a и c, всегда повёрнутая к камере. */
    private static void ribbon(VertexConsumer buf, Matrix4f m, Vec3d cam, Vec3d a, Vec3d c, float width,
                               float r, float g, float b, float alpha) {
        Vec3d dir = c.subtract(a);
        Vec3d toCam = cam.subtract(a.add(c).multiply(0.5));
        Vec3d side = dir.crossProduct(toCam);
        if (side.lengthSquared() < 1e-9) return;
        side = side.normalize().multiply(width / 2);
        quad(buf, m, cam, a.add(side), a.subtract(side), c.subtract(side), c.add(side), r, g, b, alpha);
    }

    private static void disc(VertexConsumer buf, Matrix4f m, Vec3d cam, Vec3d center, Vec3d normal, float radius,
                             float r, float g, float b, float alpha) {
        Vec3d[] basis = basis(normal);
        int seg = 32;
        for (int i = 0; i < seg; i++) {
            Vec3d p1 = circle(center, basis, radius, (float) (i * Math.PI * 2 / seg));
            Vec3d p2 = circle(center, basis, radius, (float) ((i + 1) * Math.PI * 2 / seg));
            quad(buf, m, cam, center, p1, p2, center, r, g, b, alpha);
        }
    }

    private static void ring(VertexConsumer buf, Matrix4f m, Vec3d cam, Vec3d center, Vec3d normal, float radius,
                             float thickness, float rotation, float r, float g, float b, float alpha, boolean dashed) {
        Vec3d[] basis = basis(normal);
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            if (dashed && (i / 4) % 2 == 1) continue;
            float a1 = rotation + (float) (i * Math.PI * 2 / seg);
            float a2 = rotation + (float) ((i + 1) * Math.PI * 2 / seg);
            Vec3d o1 = circle(center, basis, radius + thickness, a1), o2 = circle(center, basis, radius + thickness, a2);
            Vec3d i1 = circle(center, basis, radius - thickness, a1), i2 = circle(center, basis, radius - thickness, a2);
            quad(buf, m, cam, i1, o1, o2, i2, r, g, b, alpha);
        }
    }

    private static Vec3d[] basis(Vec3d normal) {
        Vec3d ref = Math.abs(normal.y) > 0.9 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d u = normal.crossProduct(ref).normalize();
        Vec3d v = normal.crossProduct(u).normalize();
        return new Vec3d[]{u, v};
    }

    private static Vec3d circle(Vec3d center, Vec3d[] basis, float radius, float angle) {
        return center.add(basis[0].multiply(MathHelper.cos(angle) * radius))
                .add(basis[1].multiply(MathHelper.sin(angle) * radius));
    }

    private static void quad(VertexConsumer buf, Matrix4f m, Vec3d cam, Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4,
                             float r, float g, float b, float a) {
        vertex(buf, m, cam, p1, r, g, b, a);
        vertex(buf, m, cam, p2, r, g, b, a);
        vertex(buf, m, cam, p3, r, g, b, a);
        vertex(buf, m, cam, p4, r, g, b, a);
    }

    private static void vertex(VertexConsumer buf, Matrix4f m, Vec3d cam, Vec3d p, float r, float g, float b, float a) {
        buf.vertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z)).color(r, g, b, a);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
