package com.splegg.trajectory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Повторяет физику ванильного ThrownEntity (яйцо) по тикам. */
public final class TrajectorySimulator {
    private static final int MAX_TICKS = 400;
    private static final double EGG_HALF_SIZE = 0.125;

    public record Result(List<Vec3d> points, HitResult hit) {}

    private TrajectorySimulator() {}

    /**
     * Параметры броска.
     *
     * @param speed           начальная скорость снаряда
     * @param inheritMovement добавлять смещение игрока за тик (так делает сервер в ProjectileEntity#setVelocity)
     * @param spawnYOffset    смещение точки спавна по Y относительно глаз (ванилла = -0.1)
     * @param spawnForward    смещение точки спавна вперёд по взгляду (ванилла = 0)
     * @param liftY           добавка к начальной вертикальной скорости (ванилла = 0)
     */
    public record Params(double speed, boolean inheritMovement, double spawnYOffset, double spawnForward, double liftY) {}

    public static Result simulate(PlayerEntity player, float tickDelta, Params params) {
        World world = player.getWorld();
        Vec3d dir = launchDirection(player.getPitch(tickDelta), player.getYaw(tickDelta));
        Vec3d pos = player.getCameraPosVec(tickDelta).add(0, params.spawnYOffset(), 0).add(dir.multiply(params.spawnForward()));
        Vec3d vel = dir.multiply(params.speed()).add(0, params.liftY(), 0);
        if (params.inheritMovement()) {
            Vec3d m = playerMovement(player);
            vel = vel.add(m.x, player.isOnGround() ? 0.0 : m.y, m.z);
        }
        return simulate(world, player, pos, vel);
    }

    /** Направление как в ProjectileEntity#setVelocity(shooter, pitch, yaw, roll, speed, divergence). */
    public static Vec3d launchDirection(float pitch, float yaw) {
        float rad = MathHelper.RADIANS_PER_DEGREE;
        double vx = -MathHelper.sin(yaw * rad) * MathHelper.cos(pitch * rad);
        double vy = -MathHelper.sin(pitch * rad);
        double vz = MathHelper.cos(yaw * rad) * MathHelper.cos(pitch * rad);
        return new Vec3d(vx, vy, vz).normalize();
    }

    /**
     * Реальное смещение игрока за последний тик. Именно его сервер (ServerPlayerEntity#getMovement)
     * добавляет к снаряду; клиентский getVelocity() после трения почти вдвое меньше и не подходит.
     */
    public static Vec3d playerMovement(PlayerEntity player) {
        return new Vec3d(player.getX() - player.prevX, player.getY() - player.prevY, player.getZ() - player.prevZ);
    }

    public static Result simulate(World world, Entity owner, Vec3d pos, Vec3d vel) {
        List<Vec3d> points = new ArrayList<>();
        points.add(pos);
        HitResult hit = null;
        double minY = world.getBottomY() - 64;

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            Vec3d next = pos.add(vel);

            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
            Vec3d end = next;
            if (blockHit.getType() != HitResult.Type.MISS) {
                end = blockHit.getPos();
                hit = blockHit;
            }

            Box eggBox = new Box(pos.x - EGG_HALF_SIZE, pos.y, pos.z - EGG_HALF_SIZE,
                    pos.x + EGG_HALF_SIZE, pos.y + EGG_HALF_SIZE * 2, pos.z + EGG_HALF_SIZE);
            EntityHitResult entityHit = ProjectileUtil.getEntityCollision(world, owner, pos, end,
                    eggBox.stretch(vel).expand(1.0), TrajectorySimulator::canHit);
            if (entityHit != null) {
                hit = entityHit;
                end = entityHit.getPos();
            }

            points.add(end);
            if (hit != null || end.y < minY) break;

            float drag = world.getFluidState(BlockPos.ofFloored(pos)).isIn(FluidTags.WATER) ? 0.8f : 0.99f;
            vel = vel.multiply(drag).subtract(0, TrajectoryConfig.gravity, 0);
            pos = next;
        }
        return new Result(points, hit);
    }

    private static boolean canHit(Entity e) {
        return e.isAlive() && e.canHit() && !e.isSpectator();
    }
}
