package com.bigbro.chickenbomber.client.mixin;

import com.bigbro.chickenbomber.client.AirplaneRenderer;
import com.bigbro.chickenbomber.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Добавляем свои рендереры сразу после ванильных. */
@Mixin(EntityRenderers.class)
public abstract class EntityRenderersMixin {
	@Shadow
	private static <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
		throw new AssertionError();
	}

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void chickenbomber$registerRenderers(CallbackInfo ci) {
		register(ModEntities.AIRPLANE, AirplaneRenderer::new);
		// Яйцо-бомба выглядит как обычное яйцо, только крупнее
		register(ModEntities.EGG_BOMB, context -> new ThrownItemRenderer<>(context, 1.75F, false));
	}
}
