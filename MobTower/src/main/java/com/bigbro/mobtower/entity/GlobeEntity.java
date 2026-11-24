package com.bigbro.mobtower.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Земной шар, которым кидается Херобрин. Взрывается от удара обо что угодно. */
public class GlobeEntity extends ThrowableProjectile {
	private static final float EXPLOSION_POWER = 2.0F;
	/** NONE — взрыв ранит и отбрасывает, но не ломает блоки. BLOCK — ломает, как крипер. */
	private static final Level.ExplosionInteraction EXPLOSION_BLOCKS = Level.ExplosionInteraction.NONE;

	public GlobeEntity(EntityType<? extends GlobeEntity> type, Level level) {
		super(type, level);
	}

	public GlobeEntity(Level level, LivingEntity owner) {
		super(ModEntities.GLOBE, level);
		this.setOwner(owner);
		this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// Иначе шар врежется в Визершторм, с которого его и бросили
		return !(entity instanceof WitherStorm) && !(entity instanceof Herobrine) && super.canHitEntity(entity);
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!this.level().isClientSide()) {
			this.level().explode(this, this.getX(), this.getY(), this.getZ(), EXPLOSION_POWER, EXPLOSION_BLOCKS);
			this.discard();
		}
	}
}
