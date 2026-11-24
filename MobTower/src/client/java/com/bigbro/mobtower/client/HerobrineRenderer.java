package com.bigbro.mobtower.client;

import com.bigbro.mobtower.entity.Herobrine;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class HerobrineRenderer extends HumanoidMobRenderer<Herobrine, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	private static final Identifier TEXTURE = ModTextures.HEROBRINE;
	private static final RenderType EYES = RenderTypes.eyes(ModTextures.HEROBRINE_EYES);

	public HerobrineRenderer(EntityRendererProvider.Context context) {
		super(context, createModel(), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	private static HumanoidModel<HumanoidRenderState> createModel() {
		return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64).bakeRoot());
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		ModTextures.ensureLoaded();
		return TEXTURE;
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public void extractRenderState(Herobrine herobrine, HumanoidRenderState state, float partialTick) {
		super.extractRenderState(herobrine, state, partialTick);
		// Он едет на Визершторме, но стоит в полный рост, а не сидит
		state.isPassenger = false;
	}
}
