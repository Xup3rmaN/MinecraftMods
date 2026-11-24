package com.bigbro.chickenbomber.client;

import com.bigbro.chickenbomber.entity.Airplane;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class AirplaneRenderer extends MobRenderer<Airplane, LivingEntityRenderState, AirplaneModel> {
	/** Высота центра фюзеляжа: вокруг неё самолёт наклоняет нос. */
	private static final float PITCH_PIVOT = 0.5F;

	public AirplaneRenderer(EntityRendererProvider.Context context) {
		super(context, new AirplaneModel(AirplaneModel.createBodyLayer().bakeRoot()), 1.5F);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		ModTextures.ensureLoaded();
		return ModTextures.AIRPLANE;
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public void extractRenderState(Airplane airplane, LivingEntityRenderState state, float partialTick) {
		super.extractRenderState(airplane, state, partialTick);
		// Корпус всегда смотрит по курсу, без запаздывания, как у ходячих мобов
		state.bodyRot = Mth.rotLerp(partialTick, airplane.yRotO, airplane.getYRot());
	}

	@Override
	protected void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale) {
		super.setupRotations(state, poseStack, bodyRot, scale);
		if (state.deathTime <= 0.0F) {
			poseStack.translate(0.0F, PITCH_PIVOT, 0.0F);
			poseStack.mulPose(Axis.XP.rotationDegrees(-state.xRot));
			poseStack.translate(0.0F, -PITCH_PIVOT, 0.0F);
		}
	}
}
