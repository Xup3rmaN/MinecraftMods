package com.bigbro.freddybonnie.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Кролик Бонни: сидит у Фредди на плечах и время от времени бьёт его балалайкой по голове. */
public class Bonnie extends PathfinderMob {
	private int bonkCooldown = 20;

	public Bonnie(EntityType<? extends Bonnie> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25);
	}

	/** Ванильные мирные мобы не двигают таймер взмаха рукой (это делают только монстры),
	 * и без этого удар балалайкой и бросок снежка не анимировались бы. */
	@Override
	public void aiStep() {
		super.aiStep();
		this.updateSwingTime();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!(this.getVehicle() instanceof Freddy freddy)) {
			return;
		}
		// Смотрит туда же, куда Фредди, — на дракона
		this.setYHeadRot(freddy.getYHeadRot());
		this.setXRot(freddy.getXRot());

		if (--this.bonkCooldown > 0) {
			return;
		}
		this.bonkCooldown = 18 + this.random.nextInt(14);

		this.swing(InteractionHand.MAIN_HAND);
		// Звенит струнами, а Фредди вздрагивает, как от удара, но без урона
		level.playSound(null, freddy.getX(), freddy.getEyeY(), freddy.getZ(), SoundEvents.NOTE_BLOCK_BANJO.value(), SoundSource.NEUTRAL, 1.5F, 0.5F + this.random.nextFloat() * 1.0F);
		level.playSound(null, freddy.getX(), freddy.getEyeY(), freddy.getZ(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 1.0F, 1.2F);
		level.broadcastDamageEvent(freddy, this.damageSources().mobAttack(this));
		level.sendParticles(ParticleTypes.CRIT, freddy.getX(), freddy.getEyeY() + 0.6, freddy.getZ(), 8, 0.3, 0.1, 0.3, 0.2);
		level.sendParticles(ParticleTypes.NOTE, freddy.getX(), freddy.getEyeY() + 1.0, freddy.getZ(), 1, 0.2, 0.2, 0.2, this.random.nextDouble());
	}

	/** Без Fabric API lang-файлы мода не грузятся, поэтому имя задано и запасным текстом. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback("entity.freddybonnie.bonnie", "Бонни");
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
