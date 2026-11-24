package com.bigbro.mobtower.mixin;

import com.bigbro.mobtower.entity.Herobrine;
import com.bigbro.mobtower.entity.ModEntities;
import com.bigbro.mobtower.entity.WitherStorm;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ванильная таблица атрибутов неизменяемая, поэтому подсовываем свои атрибуты при запросе. */
@Mixin(DefaultAttributes.class)
public abstract class DefaultAttributesMixin {
	@Unique
	private static AttributeSupplier mobtower$witherStorm;
	@Unique
	private static AttributeSupplier mobtower$herobrine;

	@Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
	private static void mobtower$getSupplier(EntityType<? extends LivingEntity> type, CallbackInfoReturnable<AttributeSupplier> cir) {
		if (type == ModEntities.WITHER_STORM) {
			if (mobtower$witherStorm == null) {
				mobtower$witherStorm = WitherStorm.createAttributes().build();
			}
			cir.setReturnValue(mobtower$witherStorm);
		} else if (type == ModEntities.HEROBRINE) {
			if (mobtower$herobrine == null) {
				mobtower$herobrine = Herobrine.createAttributes().build();
			}
			cir.setReturnValue(mobtower$herobrine);
		}
	}

	@Inject(method = "hasSupplier", at = @At("HEAD"), cancellable = true)
	private static void mobtower$hasSupplier(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
		if (type == ModEntities.WITHER_STORM || type == ModEntities.HEROBRINE) {
			cir.setReturnValue(true);
		}
	}
}
