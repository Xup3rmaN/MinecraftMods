package com.bigbro.freddybonnie.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Эндер Дракон на поводке у Фредди. Рисуется ванильной моделью дракона, но это отдельная сущность:
 * ванильный EnderDragon жёстко прописывает себе тип minecraft:ender_dragon, и после перезахода
 * в мир наследник превратился бы в настоящего дракона, который крушит всё вокруг.
 * Этот только машет крыльями и кружит рядом с тем, кто держит поводок.
 */
public class PetDragon extends Mob {
	public static final double HOVER_HEIGHT = 7.0;
	private static final double ORBIT_RADIUS = 9.0;
	private static final double MAX_SPEED = 0.6;

	/** То же, что у ванильного дракона: по этой истории модель изгибает шею и хвост. */
	public final DragonFlightHistory flightHistory = new DragonFlightHistory();
	public float oFlapTime;
	public float flapTime;
	private float orbitAngle;

	public PetDragon(EntityType<? extends PetDragon> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
		this.xpReward = 50;
		this.orbitAngle = this.random.nextFloat() * Mth.TWO_PI;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	public void tick() {
		// /summon и загрузка мира сбрасывают NoGravity из сохранения, поэтому держим его каждый тик
		this.noPhysics = true;
		this.setNoGravity(true);
		super.tick();

		this.oFlapTime = this.flapTime;
		this.flapTime += 0.08F;
		this.flightHistory.record(this.getY(), this.getYRot());
		if (this.level().isClientSide()) {
			// Хлопок крыльями, как у ванильного дракона
			float now = Mth.cos(this.flapTime * Mth.TWO_PI);
			float before = Mth.cos(this.oFlapTime * Mth.TWO_PI);
			if (before <= -0.3F && now >= -0.3F && !this.isSilent()) {
				this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false);
			}
		}
	}

	/** Летим ровно туда, куда задал ИИ: без гравитации, трения и столкновений с блоками. */
	@Override
	public void travel(Vec3 input) {
		this.move(MoverType.SELF, this.getDeltaMovement());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		Entity holder = this.getLeashHolder();
		if (holder == null) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
			return;
		}

		this.orbitAngle += 0.008F;
		Vec3 target = holder.position().add(Mth.cos(this.orbitAngle) * ORBIT_RADIUS, HOVER_HEIGHT, Mth.sin(this.orbitAngle) * ORBIT_RADIUS);
		Vec3 step = target.subtract(this.position()).scale(0.05);
		if (step.length() > MAX_SPEED) {
			step = step.normalize().scale(MAX_SPEED);
		}
		this.setDeltaMovement(step);

		// Головой к Фредди. Модель дракона смотрит в -Z, поэтому курс на 180° отличается от обычных мобов
		double dx = holder.getX() - this.getX();
		double dz = holder.getZ() - this.getZ();
		float yaw = 180.0F - (float) (Mth.atan2(dx, dz) * Mth.RAD_TO_DEG);
		this.setYRot(Mth.rotLerp(0.08F, this.getYRot(), yaw));
		this.yBodyRot = this.getYRot();
		this.yHeadRot = this.getYRot();
	}

	/** Снежки не ранят, но дракон вздрагивает и рычит от обиды. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getDirectEntity() instanceof Snowball) {
			level.broadcastDamageEvent(this, source);
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Поводок не рвётся и не тянет: дракон сам держится рядом с Фредди. */
	@Override
	public double leashSnapDistance() {
		return 64.0;
	}

	@Override
	public double leashElasticDistance() {
		return 64.0;
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
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDER_DRAGON_GROWL;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENDER_DRAGON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENDER_DRAGON_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 2.5F;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 300;
	}

	@Override
	protected Component getTypeName() {
		return Component.translatable("entity.minecraft.ender_dragon");
	}
}
