package com.bigbro.chickenbomber;

import com.bigbro.chickenbomber.entity.Airplane;
import com.bigbro.chickenbomber.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ChickenBomber {
	/** Насколько дальше и выше игрока появляется самолёт, в блоках. */
	private static final double DISTANCE = 40.0;
	private static final double HEIGHT = 18.0;
	/** Если самолёт уже летает в этом радиусе, второй не появляется. */
	private static final double ALREADY_HERE_RADIUS = 128.0;

	private ChickenBomber() {
	}

	public static void spawnAirplane(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();

		AABB area = player.getBoundingBox().inflate(ALREADY_HERE_RADIUS);
		if (!level.getEntitiesOfClass(Airplane.class, area, Airplane::isAlive).isEmpty()) {
			return;
		}

		Vec3 look = player.getLookAngle();
		Vec3 forward = new Vec3(look.x, 0, look.z);
		forward = forward.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : forward.normalize();
		Vec3 pos = player.position().add(forward.scale(DISTANCE)).add(0, HEIGHT, 0);

		Airplane airplane = ModEntities.AIRPLANE.create(level, EntitySpawnReason.EVENT);
		if (airplane == null) {
			return;
		}
		airplane.snapTo(pos.x, pos.y, pos.z, player.getYRot() + 180.0F, 0.0F);
		// Здесь же в кабину садится курица
		airplane.finalizeSpawn(level, level.getCurrentDifficultyAt(airplane.blockPosition()), EntitySpawnReason.EVENT, null);
		level.addFreshEntityWithPassengers(airplane);
	}
}
