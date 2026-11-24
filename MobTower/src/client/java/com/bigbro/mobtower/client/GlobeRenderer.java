package com.bigbro.mobtower.client;

import com.bigbro.mobtower.entity.GlobeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Вращающийся кубический земной шар. */
public class GlobeRenderer extends EntityRenderer<GlobeEntity, EntityRenderState> {
	private static final Identifier TEXTURE = ModTextures.GLOBE;
	private final GlobeModel model;

	public GlobeRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new GlobeModel(GlobeModel.createLayer().bakeRoot());
		this.shadowRadius = 0.3F;
	}

	@Override
	public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		ModTextures.ensureLoaded();
		poseStack.pushPose();
		poseStack.translate(0.0F, 0.375F, 0.0F);
		poseStack.mulPose(Axis.ZP.rotationDegrees(23.5F));
		poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 12.0F));
		poseStack.scale(-1.5F, -1.5F, 1.5F);
		collector.submitModel(this.model, state, poseStack, this.model.renderType(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	static class GlobeModel extends EntityModel<EntityRenderState> {
		GlobeModel(ModelPart root) {
			super(root);
		}

		static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			mesh.getRoot().addOrReplaceChild("globe",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.ZERO);
			return LayerDefinition.create(mesh, 32, 16);
		}
	}
}
