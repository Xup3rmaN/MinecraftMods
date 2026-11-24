package com.bigbro.mobtower.client.mixin;

import com.bigbro.mobtower.client.GlobeRenderer;
import com.bigbro.mobtower.client.HerobrineRenderer;
import com.bigbro.mobtower.client.WitherStormRenderer;
import com.bigbro.mobtower.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
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
	private static void mobtower$registerRenderers(CallbackInfo ci) {
		register(ModEntities.WITHER_STORM, WitherStormRenderer::new);
		register(ModEntities.HEROBRINE, HerobrineRenderer::new);
		register(ModEntities.GLOBE, GlobeRenderer::new);
	}
}
