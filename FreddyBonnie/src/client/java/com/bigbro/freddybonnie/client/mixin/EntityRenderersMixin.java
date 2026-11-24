package com.bigbro.freddybonnie.client.mixin;

import com.bigbro.freddybonnie.client.BonnieRenderer;
import com.bigbro.freddybonnie.client.FreddyRenderer;
import com.bigbro.freddybonnie.client.PetDragonRenderer;
import com.bigbro.freddybonnie.entity.ModEntities;
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
	private static void freddybonnie$registerRenderers(CallbackInfo ci) {
		register(ModEntities.FREDDY, FreddyRenderer::new);
		register(ModEntities.BONNIE, BonnieRenderer::new);
		register(ModEntities.PET_DRAGON, PetDragonRenderer::new);
	}
}
