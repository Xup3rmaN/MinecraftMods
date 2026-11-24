package com.bigbro.freddybonnie.client;

import com.bigbro.freddybonnie.entity.PetDragon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Рисует ручного дракона ванильной моделью и текстурой — так же, как EnderDragonRenderer, только без кристаллов и лучей смерти. */
public class PetDragonRenderer extends EntityRenderer<PetDragon, EnderDragonRenderState> {
	private static final Identifier DRAGON_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
	private static final Identifier DRAGON_EYES_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
	private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(DRAGON_LOCATION);
	private static final RenderType EYES = RenderTypes.eyes(DRAGON_EYES_LOCATION);
	private final EnderDragonModel model;

	public PetDragonRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 2.0F;
		this.model = new EnderDragonModel(context.bakeLayer(ModelLayers.ENDER_DRAGON));
	}

	@Override
	public void submit(EnderDragonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		float yaw = state.getHistoricalPos(7).yRot();
		float climb = (float) (state.getHistoricalPos(5).y() - state.getHistoricalPos(10).y());
		poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
		poseStack.mulPose(Axis.XP.rotationDegrees(climb * 10.0F));
		poseStack.translate(0.0F, 0.0F, 1.0F);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		int overlay = OverlayTexture.pack(0.0F, state.hasRedOverlay);
		collector.order(0).submitModel(this.model, state, poseStack, RENDER_TYPE, state.lightCoords, overlay, -1, null, state.outlineColor, null);
		collector.submitModel(this.model, state, poseStack, EYES, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public EnderDragonRenderState createRenderState() {
		return new EnderDragonRenderState();
	}

	@Override
	public void extractRenderState(PetDragon dragon, EnderDragonRenderState state, float partialTick) {
		super.extractRenderState(dragon, state, partialTick);
		state.flapTime = Mth.lerp(partialTick, dragon.oFlapTime, dragon.flapTime);
		state.deathTime = 0.0F;
		state.hasRedOverlay = dragon.hurtTime > 0 || dragon.deathTime > 0;
		state.beamOffset = null;
		state.isLandingOrTakingOff = false;
		state.isSitting = false;
		state.distanceToEgg = Double.MAX_VALUE;
		state.partialTicks = partialTick;
		state.flightHistory.copyFrom(dragon.flightHistory);
	}
}
