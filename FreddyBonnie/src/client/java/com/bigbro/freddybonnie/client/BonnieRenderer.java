package com.bigbro.freddybonnie.client;

import com.bigbro.freddybonnie.entity.Bonnie;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class BonnieRenderer extends HumanoidMobRenderer<Bonnie, HumanoidRenderState, BonnieModel> {
	private static final float SCALE = 0.9F;

	public BonnieRenderer(EntityRendererProvider.Context context) {
		super(context, new BonnieModel(BonnieModel.createBodyLayer().bakeRoot()), 0.4F);
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		ModTextures.ensureLoaded();
		return ModTextures.BONNIE;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	protected void scale(HumanoidRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}
}
