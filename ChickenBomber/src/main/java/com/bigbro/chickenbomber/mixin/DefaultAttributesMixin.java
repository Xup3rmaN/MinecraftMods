package com.bigbro.chickenbomber.mixin;

import com.bigbro.chickenbomber.entity.Airplane;
import com.bigbro.chickenbomber.entity.ModEntities;
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
	private static AttributeSupplier chickenbomber$airplane;

	@Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
	private static void chickenbomber$getSupplier(EntityType<? extends LivingEntity> type, CallbackInfoReturnable<AttributeSupplier> cir) {
		if (type == ModEntities.AIRPLANE) {
			if (chickenbomber$airplane == null) {
				chickenbomber$airplane = Airplane.createAttributes().build();
			}
			cir.setReturnValue(chickenbomber$airplane);
		}
	}

	@Inject(method = "hasSupplier", at = @At("HEAD"), cancellable = true)
	private static void chickenbomber$hasSupplier(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
		if (type == ModEntities.AIRPLANE) {
			cir.setReturnValue(true);
		}
	}
}
