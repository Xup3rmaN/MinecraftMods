package com.bigbro.freddybonnie.mixin;

import com.bigbro.freddybonnie.entity.Bonnie;
import com.bigbro.freddybonnie.entity.Freddy;
import com.bigbro.freddybonnie.entity.ModEntities;
import com.bigbro.freddybonnie.entity.PetDragon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/** Ванильная таблица атрибутов неизменяемая, поэтому подсовываем свои атрибуты при запросе. */
@Mixin(DefaultAttributes.class)
public abstract class DefaultAttributesMixin {
	@Unique
	private static final Map<EntityType<?>, AttributeSupplier> freddybonnie$suppliers = new HashMap<>();

	@Unique
	private static AttributeSupplier freddybonnie$get(EntityType<?> type) {
		if (type == ModEntities.FREDDY) {
			return freddybonnie$suppliers.computeIfAbsent(type, t -> Freddy.createAttributes().build());
		} else if (type == ModEntities.BONNIE) {
			return freddybonnie$suppliers.computeIfAbsent(type, t -> Bonnie.createAttributes().build());
		} else if (type == ModEntities.PET_DRAGON) {
			return freddybonnie$suppliers.computeIfAbsent(type, t -> PetDragon.createAttributes().build());
		}
		return null;
	}

	@Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
	private static void freddybonnie$getSupplier(EntityType<? extends LivingEntity> type, CallbackInfoReturnable<AttributeSupplier> cir) {
		AttributeSupplier supplier = freddybonnie$get(type);
		if (supplier != null) {
			cir.setReturnValue(supplier);
		}
	}

	@Inject(method = "hasSupplier", at = @At("HEAD"), cancellable = true)
	private static void freddybonnie$hasSupplier(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
		if (type == ModEntities.FREDDY || type == ModEntities.BONNIE || type == ModEntities.PET_DRAGON) {
			cir.setReturnValue(true);
		}
	}
}
