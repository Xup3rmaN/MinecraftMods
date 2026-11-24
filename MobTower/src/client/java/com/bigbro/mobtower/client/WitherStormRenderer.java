package com.bigbro.mobtower.client;

import com.bigbro.mobtower.entity.WitherStorm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public class WitherStormRenderer extends MobRenderer<WitherStorm, LivingEntityRenderState, WitherStormModel> {
	/** Во сколько раз модель больше своих «пикселей». Связано с ModEntities.STORM_TOP. */
	private static final float SCALE = 2.5F;
	private static final Identifier TEXTURE = ModTextures.WITHER_STORM;
	private static final RenderType EYES = RenderTypes.eyes(ModTextures.WITHER_STORM_EYES);

	public WitherStormRenderer(EntityRendererProvider.Context context) {
		super(context, new WitherStormModel(WitherStormModel.createBodyLayer().bakeRoot()), 3.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		ModTextures.ensureLoaded();
		return TEXTURE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	protected int getBlockLightLevel(WitherStorm storm, BlockPos pos) {
		return Math.max(8, super.getBlockLightLevel(storm, pos));
	}
}
