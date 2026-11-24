package com.bigbro.oldmechanics.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla computes {@code crouching} before polling the keyboard, so it lags one tick behind the sneak key:
 * after releasing shift you still move (and jump) at sneak speed for a tick and cannot start sprinting.
 * Pre-1.14 applied the sneak slowdown from the key state of the same tick. Recompute it right after
 * the input is read, using the old rules (no forced crouch under low ceilings).
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Shadow
	private boolean crouching;

	@Inject(
		method = "aiStep",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER)
	)
	private void oldmechanics$sameTickSneak(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		this.crouching = self.isShiftKeyDown()
			&& !self.getAbilities().flying
			&& !self.isSwimming()
			&& !self.isPassenger()
			&& !self.isSleeping();
	}
}
