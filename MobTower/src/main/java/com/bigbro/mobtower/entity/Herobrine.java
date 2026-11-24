package com.bigbro.mobtower.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Стоит на Визершторме, следит за игроком и раскидывает в него земные шары. */
public class Herobrine extends Monster {
	private static final double THROW_RANGE = 48.0;
	private static final int THROW_COOLDOWN = 40;
	/** Сколько шаров за бросок летит в разные стороны, кроме прицельного. */
	private static final int SCATTER_GLOBES = 9;
	/** Во сколько раз Херобрин больше обычного игрока. */
	private static final double GIANT_SCALE = 4.0;

	private int throwCooldown = THROW_COOLDOWN;

	public Herobrine(EntityType<? extends Herobrine> type, Level level) {
		super(type, level);
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 100.0)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.SCALE, GIANT_SCALE)
			.add(Attributes.FOLLOW_RANGE, THROW_RANGE)
			.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);

		Player player = level.getNearestPlayer(this, THROW_RANGE);
		if (player == null || player.isSpectator()) {
			return;
		}
		this.getLookControl().setLookAt(player, 30.0F, 30.0F);

		if (--this.throwCooldown > 0) {
			return;
		}
		this.throwCooldown = THROW_COOLDOWN + this.random.nextInt(15);

		// Один шар прицельно в игрока
		GlobeEntity aimed = new GlobeEntity(level, this);
		double dx = player.getX() - aimed.getX();
		double dy = player.getEyeY() - 0.5 - aimed.getY();
		double dz = player.getZ() - aimed.getZ();
		// Чуть выше цели: шар летит по дуге
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		aimed.shoot(dx, dy + horizontal * 0.12, dz, 1.4F, 3.0F);
		level.addFreshEntity(aimed);

		// Остальные веером во все стороны, немного вверх, чтобы разлетались подальше
		for (int i = 0; i < SCATTER_GLOBES; i++) {
			GlobeEntity globe = new GlobeEntity(level, this);
			double angle = (i + this.random.nextDouble() * 0.6) * (Math.PI * 2.0 / SCATTER_GLOBES);
			globe.shoot(Math.cos(angle), 0.2 + this.random.nextDouble() * 0.4, Math.sin(angle), 0.8F + this.random.nextFloat() * 0.6F, 5.0F);
			level.addFreshEntity(globe);
		}

		this.swing(InteractionHand.MAIN_HAND);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 2.0F, 0.5F);
	}

	/** На Визершторме он стоит лицом к игроку, а не туда, куда летит Визершторм. */
	@Override
	public void rideTick() {
		super.rideTick();
		this.yBodyRot = this.yHeadRot;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FALL)) {
			return false;
		}
		if (source.getEntity() instanceof WitherStorm || source.getDirectEntity() instanceof GlobeEntity) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Без Fabric API lang-файлы мода не грузятся, поэтому имя есть и запасным текстом. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback("entity.mobtower.herobrine", "Херобрин");
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
