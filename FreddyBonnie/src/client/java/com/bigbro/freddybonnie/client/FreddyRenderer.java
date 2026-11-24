package com.bigbro.freddybonnie.client;

import com.bigbro.freddybonnie.entity.Freddy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class FreddyRenderer extends HumanoidMobRenderer<Freddy, HumanoidRenderState, FreddyModel> {
	private static final float SCALE = 1.25F;

	public FreddyRenderer(EntityRendererProvider.Context context) {
		super(context, new FreddyModel(FreddyModel.createBodyLayer().bakeRoot()), 0.7F);
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		ModTextures.ensureLoaded();
		return ModTextures.FREDDY;
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
