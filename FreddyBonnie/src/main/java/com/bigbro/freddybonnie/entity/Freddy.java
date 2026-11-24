package com.bigbro.freddybonnie.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** Мишка Фредди: гуляет, держит Эндер Дракона на поводке и кидает в него снежки. */
public class Freddy extends PathfinderMob {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int THROW_INTERVAL = 15;
	private static final double DRAGON_SEARCH_RADIUS = 48.0;

	/** Дракона создаём на первом тике после появления, когда Фредди уже в мире и поводку есть за что держаться. */
	private boolean needsDragon;
	private @Nullable PetDragon dragon;

	public Freddy(EntityType<? extends Freddy> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 60.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.FOLLOW_RANGE, DRAGON_SEARCH_RADIUS);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
	}

	/** Фредди всегда появляется с Бонни на плечах — и из яйца, и через /summon. */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		data = super.finalizeSpawn(level, difficulty, reason, data);
		Bonnie bonnie = ModEntities.BONNIE.create(this.level(), EntitySpawnReason.JOCKEY);
		if (bonnie != null) {
			bonnie.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
			bonnie.setPersistenceRequired();
			bonnie.startRiding(this, true, false);
		}
		this.setPersistenceRequired();
		this.needsDragon = true;
		return data;
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

		if (this.needsDragon) {
			this.needsDragon = false;
			this.spawnDragon(level);
		}

		if (this.dragon == null || !this.dragon.isAlive() || this.dragon.getLeashHolder() != this) {
			this.dragon = this.findDragon(level);
		}
		if (this.dragon == null) {
			return;
		}

		this.getLookControl().setLookAt(this.dragon.getX(), this.dragon.getY() + 2.0, this.dragon.getZ(), 30.0F, 30.0F);
		if (this.tickCount % THROW_INTERVAL == 0) {
			this.throwSnowball(level, this.dragon);
		}
	}

	private void spawnDragon(ServerLevel level) {
		PetDragon dragon = ModEntities.PET_DRAGON.create(level, EntitySpawnReason.EVENT);
		if (dragon == null) {
			LOGGER.warn("Freddy & Bonnie: could not create the pet dragon");
			return;
		}
		dragon.snapTo(this.getX() + 8.0, this.getY() + PetDragon.HOVER_HEIGHT, this.getZ(), 0.0F, 0.0F);
		dragon.setPersistenceRequired();
		if (!level.addFreshEntity(dragon)) {
			LOGGER.warn("Freddy & Bonnie: the level refused the pet dragon at {}", dragon.position());
			return;
		}
		dragon.setLeashedTo(this, true);
		this.dragon = dragon;
	}

	private @Nullable PetDragon findDragon(ServerLevel level) {
		AABB area = this.getBoundingBox().inflate(DRAGON_SEARCH_RADIUS);
		return level.getEntitiesOfClass(PetDragon.class, area, d -> d.isAlive() && d.getLeashHolder() == this)
			.stream().findFirst().orElse(null);
	}

	private void throwSnowball(ServerLevel level, PetDragon dragon) {
		Snowball snowball = new Snowball(level, this, new ItemStack(Items.SNOWBALL));
		double dx = dragon.getX() - snowball.getX();
		double dy = dragon.getY() + 2.0 - snowball.getY();
		double dz = dragon.getZ() - snowball.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		snowball.shoot(dx, dy + horizontal * 0.1, dz, 1.6F, 1.0F);
		level.addFreshEntity(snowball);
		this.swing(InteractionHand.MAIN_HAND);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 1.0F, 0.6F);
	}

	/** Бонни сидит пассажиром и только бьёт балалайкой — ходит Фредди сам. */
	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return null;
	}

	/** Без Fabric API lang-файлы мода не грузятся, поэтому имя задано и запасным текстом. */
	@Override
	protected Component getTypeName() {
		return Component.translatableWithFallback("entity.freddybonnie.freddy", "Фредди");
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}
}
