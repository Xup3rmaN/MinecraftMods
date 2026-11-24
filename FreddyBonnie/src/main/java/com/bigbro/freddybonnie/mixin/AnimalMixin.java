package com.bigbro.freddybonnie.mixin;

import com.bigbro.freddybonnie.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Свиньи несут яйца Фредди и Бонни, как куры — обычные яйца. */
@Mixin(Animal.class)
public abstract class AnimalMixin {
	/** Раз в сколько тиков свинья несёт яйцо: от 30 до 90 секунд. */
	@Unique
	private static final int MIN_EGG_TIME = 600;
	@Unique
	private static final int EGG_TIME_SPREAD = 1200;

	@Unique
	private int freddybonnie$eggTime = -1;

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	private void freddybonnie$layEgg(ServerLevel level, CallbackInfo ci) {
		Animal self = (Animal) (Object) this;
		if (!(self instanceof Pig) || self.isBaby() || !self.isAlive()) {
			return;
		}
		if (this.freddybonnie$eggTime < 0) {
			this.freddybonnie$eggTime = MIN_EGG_TIME + self.getRandom().nextInt(EGG_TIME_SPREAD);
		}
		if (--this.freddybonnie$eggTime > 0) {
			return;
		}
		this.freddybonnie$eggTime = MIN_EGG_TIME + self.getRandom().nextInt(EGG_TIME_SPREAD);
		self.playSound(SoundEvents.CHICKEN_EGG, 1.0F, 0.6F);
		self.spawnAtLocation(level, ModEntities.createEgg());
	}
}
