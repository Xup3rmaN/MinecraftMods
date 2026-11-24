package com.bigbro.mobtower.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Огромный летающий босс. Медленно кружит над ближайшим игроком, проходит сквозь блоки
 * и возит на макушке Херобрина.
 */
public class WitherStorm extends Monster {
	private static final double ORBIT_RADIUS = 16.0;
	private static final double HOVER_HEIGHT = 9.0;
	private static final double MAX_SPEED = 0.35;

	private final ServerBossEvent bossEvent = new ServerBossEvent(
		this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10
	);
	private float orbitAngle;

	public WitherStorm(EntityType<? extends WitherStorm> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
		this.xpReward = 100;
		this.orbitAngle = this.random.nextFloat() * Mth.TWO_PI;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 1000.0)
			.add(Attributes.ARMOR, 12.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 96.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	public void tick() {
		// Как у вредины: летает сквозь блоки, иначе такая громадина застрянет в первом же холме
		this.noPhysics = true;
		super.tick();
		this.noPhysics = false;
		this.setNoGravity(true);

		if (this.level().isClientSide()) {
			for (int i = 0; i < 3; i++) {
				this.level().addParticle(
					ParticleTypes.PORTAL,
					this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0),
					(this.random.nextDouble() - 0.5) * 2.0, -this.random.nextDouble(), (this.random.nextDouble() - 0.5) * 2.0
				);
			}
			if (this.random.nextInt(3) == 0) {
				this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.8), this.getY() + this.random.nextDouble() * 3.0, this.getRandomZ(0.8), 0.0, -0.05, 0.0);
			}
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

		Player player = level.getNearestPlayer(this, 96.0);
		if (player == null || player.isSpectator()) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
			return;
		}

		this.orbitAngle += 0.006F;
		Vec3 wanted = new Vec3(
			player.getX() + Mth.cos(this.orbitAngle) * ORBIT_RADIUS,
			player.getY() + HOVER_HEIGHT,
			player.getZ() + Mth.sin(this.orbitAngle) * ORBIT_RADIUS
		);
		Vec3 move = wanted.subtract(this.position()).scale(0.03);
		if (move.length() > MAX_SPEED) {
			move = move.normalize().scale(MAX_SPEED);
		}
		this.setDeltaMovement(move);

		double dx = player.getX() - this.getX();
		double dz = player.getZ() - this.getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
		this.setYRot(Mth.rotLerp(0.05F, this.getYRot(), yaw));
		this.yBodyRot = this.getYRot();
		this.yHeadRot = this.getYRot();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		if (source.getEntity() instanceof Herobrine || source.getDirectEntity() instanceof GlobeEntity) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Сам он никем не управляет, и Херобрин на нём только едет. */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	/** Без Fabric API lang-файлы мода не грузятся, поэтому имя есть и запасным текстом. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback("entity.mobtower.wither_storm", "Визершторм");
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean force) {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITHER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 4.0F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}
}
