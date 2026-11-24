package com.bigbro.oldmechanics.mixin;

import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Pre-1.14 sneaking: the hitbox stays 1.8 blocks tall and the eyes only drop by 0.08 (1.62 -> 1.54).
 */
@Mixin(Avatar.class)
public abstract class AvatarMixin {
	@Unique
	private static final EntityDimensions OLD_CROUCHING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F)
		.withEyeHeight(1.54F)
		.withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, Avatar.DEFAULT_VEHICLE_ATTACHMENT));

	@Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
	private void oldmechanics$oldCrouchDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (pose == Pose.CROUCHING && (Object) this instanceof Player) {
			cir.setReturnValue(OLD_CROUCHING_DIMENSIONS);
		}
	}
}
