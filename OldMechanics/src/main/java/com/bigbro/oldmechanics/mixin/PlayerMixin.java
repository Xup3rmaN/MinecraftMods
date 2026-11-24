package com.bigbro.oldmechanics.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pre-1.14 edge protection: sneaking only keeps you on a block while you are actually standing on the ground,
 * not while already falling a little below the edge.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "isAboveGround", at = @At("HEAD"), cancellable = true)
	private void oldmechanics$onlyOnGround(float maxUpStep, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(((Player) (Object) this).onGround());
	}
}
