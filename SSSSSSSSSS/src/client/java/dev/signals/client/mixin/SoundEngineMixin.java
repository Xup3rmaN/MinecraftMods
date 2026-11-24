package dev.signals.client.mixin;

import dev.signals.Signals;
import dev.signals.client.SignalsClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Глушит все звуки игры, кроме сигналов, когда включён режим «только сигналы». */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void signals$muteOthers(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (SignalsClient.isMuteOthers() && !Signals.MOD_ID.equals(sound.getIdentifier().getNamespace())) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}
