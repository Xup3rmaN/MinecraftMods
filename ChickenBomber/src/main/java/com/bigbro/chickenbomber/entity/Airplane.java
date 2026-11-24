package com.bigbro.chickenbomber.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Самолёт с курицей в кабине. Кружит в небе, а заметив игрока, пикирует прямо на него,
 * сбрасывает яйца-бомбы и уходит вверх на новый заход.
 */
public class Airplane extends Monster {
	private static final double SIGHT_RANGE = 64.0;
	private static final double CRUISE_HEIGHT = 18.0;
	private static final double CRUISE_SPEED = 0.45;
	private static final double DIVE_SPEED = 0.9;
	private static final double CLIMB_SPEED = 0.6;
	/** Ближе этого самолёт выходит из пике, чтобы не врезаться в игрока и землю. */
	private static final double PULL_UP_DISTANCE = 7.0;
	private static final int EGG_INTERVAL = 6;
	private static final int MAX_DIVE_TICKS = 120;
	private static final int CLIMB_TICKS = 60;

	private enum Phase { CRUISE, DIVE, CLIMB }

	private Phase phase = Phase.CRUISE;
	private int phaseTicks;
	private float circleAngle;
	private @Nullable Vec3 patrolCenter;

	public Airplane(EntityType<? extends Airplane> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.xpReward = 20;
		this.circleAngle = this.random.nextFloat() * Mth.TWO_PI;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.ARMOR, 4.0)
			.add(Attributes.FOLLOW_RANGE, SIGHT_RANGE)
			.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	/** Самолёт всегда появляется с курицей-пилотом — и при входе в мир, и через /summon. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		data = super.finalizeSpawn(level, difficulty, reason, data);
		Chicken chicken = EntityType.CHICKEN.create(this.level(), EntitySpawnReason.JOCKEY);
		if (chicken != null) {
			chicken.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
			chicken.setPersistenceRequired();
			chicken.startRiding(this, true, false);
		}
		this.setPersistenceRequired();
		return data;
	}

	@Override
	public void tick() {
		// /summon и загрузка мира сбрасывают NoGravity из сохранения, поэтому держим его каждый тик
		this.setNoGravity(true);
		super.tick();
	}

	/**
	 * Летим ровно с той скоростью, что задал ИИ. Обычное «воздушное» движение моба
	 * гасит скорость трением и тянет вниз, и самолёт просто проседал бы на землю.
	 */
	@Override
	public void travel(Vec3 input) {
		this.move(MoverType.SELF, this.getDeltaMovement());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.phaseTicks++;
		if (this.patrolCenter == null) {
			this.patrolCenter = this.position();
		}

		Player player = level.getNearestPlayer(this, SIGHT_RANGE);
		if (player != null && (player.isSpectator() || !player.isAlive())) {
			player = null;
		}

		Vec3 wanted = switch (this.phase) {
			case CRUISE -> this.cruise(player);
			case DIVE -> this.dive(level, player);
			case CLIMB -> this.climb(player);
		};

		// Плавно поворачиваем к нужному курсу, как настоящий самолёт, а не дёргаемся
		Vec3 velocity = this.getDeltaMovement().lerp(wanted, this.phase == Phase.DIVE ? 0.15 : 0.08);
		if (this.horizontalCollision) {
			velocity = new Vec3(velocity.x, Math.max(velocity.y, CLIMB_SPEED), velocity.z);
		}
		this.setDeltaMovement(velocity);

		double horizontal = velocity.horizontalDistance();
		if (horizontal > 1.0E-3) {
			float yaw = (float) (Mth.atan2(velocity.z, velocity.x) * Mth.RAD_TO_DEG) - 90.0F;
			this.setYRot(yaw);
			this.yBodyRot = yaw;
			this.yHeadRot = yaw;
		}
		// Нос вниз в пике, вверх при наборе высоты
		this.setXRot(Mth.clamp((float) (-Mth.atan2(velocity.y, Math.max(horizontal, 0.05)) * Mth.RAD_TO_DEG), -45.0F, 70.0F));
	}

	private Vec3 cruise(@Nullable Player player) {
		if (player != null && this.hasLineOfSight(player) && this.phaseTicks > 20) {
			this.setPhase(Phase.DIVE);
			this.playSound(SoundEvents.PHANTOM_SWOOP, 3.0F, 0.8F);
			return this.getDeltaMovement();
		}

		Vec3 center = player != null ? player.position() : this.patrolCenter;
		this.circleAngle += 0.02F;
		Vec3 target = new Vec3(
			center.x + Mth.cos(this.circleAngle) * 24.0,
			center.y + CRUISE_HEIGHT,
			center.z + Mth.sin(this.circleAngle) * 24.0
		);
		return toward(target, CRUISE_SPEED);
	}

	private Vec3 dive(ServerLevel level, @Nullable Player player) {
		if (player == null) {
			this.setPhase(Phase.CLIMB);
			return this.getDeltaMovement();
		}

		if (this.phaseTicks % EGG_INTERVAL == 0) {
			EggBomb egg = new EggBomb(level, this);
			Vec3 v = this.getDeltaMovement();
			egg.setDeltaMovement(v.x, Math.min(v.y, 0.0) - 0.2, v.z);
			level.addFreshEntity(egg);
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.HOSTILE, 1.5F, 0.8F);
		}

		double distance = this.distanceTo(player);
		if (distance < PULL_UP_DISTANCE || this.getY() < player.getY() + 2.0 || this.phaseTicks > MAX_DIVE_TICKS) {
			this.setPhase(Phase.CLIMB);
		}
		return toward(player.position().add(0.0, 1.0, 0.0), DIVE_SPEED);
	}

	private Vec3 climb(@Nullable Player player) {
		double groundY = player != null ? player.getY() : this.patrolCenter.y;
		if (this.phaseTicks > CLIMB_TICKS && this.getY() > groundY + CRUISE_HEIGHT - 3.0) {
			this.setPhase(Phase.CRUISE);
		}
		// Уходим вперёд по курсу и круто вверх
		Vec3 forward = this.getDeltaMovement().horizontal();
		forward = forward.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : forward.normalize();
		return new Vec3(forward.x * CLIMB_SPEED, CLIMB_SPEED * 0.8, forward.z * CLIMB_SPEED);
	}

	private Vec3 toward(Vec3 target, double speed) {
		Vec3 offset = target.subtract(this.position());
		double length = offset.length();
		return length < 1.0E-3 ? Vec3.ZERO : offset.scale(speed / length);
	}

	private void setPhase(Phase phase) {
		this.phase = phase;
		this.phaseTicks = 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getDirectEntity() instanceof EggBomb || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL)) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Курица сидит в кабине пассажиром: штурвал у самолёта, а не у неё. */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	/** Без Fabric API lang-файлы мода не грузятся, поэтому имя задано и запасным текстом. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback("entity.chickenbomber.airplane", "Самолёт");
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}
}
