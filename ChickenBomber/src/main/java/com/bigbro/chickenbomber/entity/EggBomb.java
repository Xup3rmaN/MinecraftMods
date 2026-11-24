package com.bigbro.chickenbomber.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Яйцо, которое сбрасывает самолёт. Взрывается, как только во что-нибудь врежется. */
public class EggBomb extends ThrowableItemProjectile {
	private static final float EXPLOSION_POWER = 1.8F;
	/** MOB — ломает блоки, как крипер (если не выключено правило mobGriefing). NONE — не ломает. */
	private static final Level.ExplosionInteraction EXPLOSION_BLOCKS = Level.ExplosionInteraction.MOB;

	public EggBomb(EntityType<? extends EggBomb> type, Level level) {
		super(type, level);
	}

	public EggBomb(Level level, LivingEntity owner) {
		super(ModEntities.EGG_BOMB, owner, level, new ItemStack(Items.EGG));
		this.setPos(owner.getX(), owner.getY() - 0.3, owner.getZ());
	}

	@Override
	protected Item getDefaultItem() {
		return Items.EGG;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		// Не взрываемся о собственный самолёт и курицу-пилота
		return !(entity instanceof Airplane) && !(entity.getVehicle() instanceof Airplane) && super.canHitEntity(entity);
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
