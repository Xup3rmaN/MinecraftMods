package com.bigbro.mobtower.mixin;

import com.bigbro.mobtower.entity.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Без Fabric API реестры замораживаются ещё до запуска модов,
 * поэтому сущности регистрируем в последний момент перед заморозкой.
 */
@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesMixin {
	@Inject(method = "freeze", at = @At("HEAD"))
	private static void mobtower$registerEntities(CallbackInfo ci) {
		ModEntities.init();
	}
}
