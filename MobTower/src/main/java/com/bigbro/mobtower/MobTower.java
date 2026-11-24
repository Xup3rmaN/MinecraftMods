package com.bigbro.mobtower;

import com.bigbro.mobtower.entity.Herobrine;
import com.bigbro.mobtower.entity.ModEntities;
import com.bigbro.mobtower.entity.WitherStorm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MobTower {
	/** На каком расстоянии перед игроком появляется Визершторм, в блоках. */
	private static final double DISTANCE = 20.0;
	private static final double HEIGHT = 6.0;
	/** Если Визершторм уже есть в этом радиусе, второй не появляется. */
	private static final double ALREADY_HERE_RADIUS = 128.0;

	private MobTower() {
	}

	public static void spawnStorm(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();

		AABB area = player.getBoundingBox().inflate(ALREADY_HERE_RADIUS);
		if (!level.getEntitiesOfClass(WitherStorm.class, area, storm -> storm.isAlive()).isEmpty()) {
			return;
		}

		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();
		Vec3 pos = player.position().add(forward.scale(DISTANCE)).add(0, HEIGHT, 0);
		// Лицом к игроку
		float yaw = player.getYRot() + 180.0F;

		WitherStorm storm = ModEntities.WITHER_STORM.create(level, EntitySpawnReason.EVENT);
		Herobrine herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);
		if (storm == null || herobrine == null) {
			return;
		}

		storm.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		storm.setYHeadRot(yaw);
		storm.setYBodyRot(yaw);
		storm.setPersistenceRequired();

		herobrine.snapTo(pos.x, pos.y + ModEntities.STORM_TOP, pos.z, yaw, 0.0F);
		herobrine.setYHeadRot(yaw);
		herobrine.setPersistenceRequired();
		herobrine.startRiding(storm);

		level.addFreshEntityWithPassengers(storm);
	}
}
