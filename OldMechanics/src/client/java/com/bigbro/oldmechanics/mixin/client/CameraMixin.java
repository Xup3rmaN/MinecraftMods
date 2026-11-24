package com.bigbro.oldmechanics.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pre-1.14 camera: the view sits exactly at the entity's current eye height every frame,
 * so sneaking and standing up are instant instead of being smoothed over several ticks.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private float eyeHeight;
	@Shadow
	private float eyeHeightOld;

	@Inject(method = "setup", at = @At("HEAD"))
	private void oldmechanics$instantEyeHeight(Level level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
		this.eyeHeight = entity.getEyeHeight();
		this.eyeHeightOld = this.eyeHeight;
	}
}
