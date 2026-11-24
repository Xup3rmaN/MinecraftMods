package com.bigbro.mobtower.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Трёхголовая туша из поглощённых блоков с качающимися щупальцами.
 * Раскладка текстуры повторяется в tools/textures.py — меняйте их вместе.
 */
public class WitherStormModel extends EntityModel<LivingEntityRenderState> {
	private static final int TENTACLES = 4;
	private static final int SEGMENTS = 3;
	private static final float SEGMENT_LENGTH = 8.0F;
	private static final float[][] TENTACLE_ROOTS = {{-4.0F, -2.0F}, {4.0F, -2.0F}, {-4.0F, 2.5F}, {4.0F, 2.5F}};

	private final ModelPart body;
	private final ModelPart rightHead;
	private final ModelPart leftHead;
	private final ModelPart[][] tentacles = new ModelPart[TENTACLES][SEGMENTS];

	public WitherStormModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.rightHead = root.getChild("right_head");
		this.leftHead = root.getChild("left_head");
		for (int i = 0; i < TENTACLES; i++) {
			ModelPart part = this.body;
			for (int j = 0; j < SEGMENTS; j++) {
				part = part.getChild(j == 0 ? "tentacle_" + i : "segment");
				this.tentacles[i][j] = part;
			}
		}
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Herobrine стоит на макушке этой головы: её верх на y = -34, то есть ровно ModEntities.STORM_TOP после масштаба
		root.addOrReplaceChild("center_head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F),
			PartPose.offset(0.0F, -18.0F, 0.0F));

		CubeListBuilder sideHead = CubeListBuilder.create().texOffs(64, 0).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F);
		root.addOrReplaceChild("right_head", sideHead, PartPose.offset(-19.0F, -16.0F, 0.0F));
		root.addOrReplaceChild("left_head", sideHead, PartPose.offset(19.0F, -16.0F, 0.0F));

		root.addOrReplaceChild("shoulders",
			CubeListBuilder.create().texOffs(0, 32).addBox(-24.0F, -3.0F, -3.0F, 48.0F, 6.0F, 6.0F),
			PartPose.offset(0.0F, -11.0F, 0.0F));

		CubeListBuilder debris = CubeListBuilder.create().texOffs(56, 44).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F);
		root.addOrReplaceChild("debris_0", debris, PartPose.offsetAndRotation(-14.0F, -6.0F, 3.0F, 0.4F, 0.3F, 0.2F));
		root.addOrReplaceChild("debris_1", debris, PartPose.offsetAndRotation(13.0F, -5.0F, -3.0F, -0.3F, 0.5F, 0.1F));
		root.addOrReplaceChild("debris_2", debris, PartPose.offsetAndRotation(0.0F, -13.0F, 6.0F, 0.2F, -0.4F, 0.6F));
		root.addOrReplaceChild("debris_3", debris, PartPose.offsetAndRotation(-22.0F, -15.0F, -2.0F, 0.7F, 0.2F, 0.3F));
		root.addOrReplaceChild("debris_4", debris, PartPose.offsetAndRotation(23.0F, -14.0F, 2.0F, -0.5F, 0.1F, -0.4F));

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 44).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 18.0F, 8.0F),
			PartPose.offset(0.0F, -8.0F, 0.0F));
		CubeListBuilder rib = CubeListBuilder.create().texOffs(80, 44).addBox(-10.0F, -1.5F, -1.5F, 20.0F, 3.0F, 3.0F);
		body.addOrReplaceChild("rib_0", rib, PartPose.offset(0.0F, 3.0F, -3.0F));
		body.addOrReplaceChild("rib_1", rib, PartPose.offset(0.0F, 7.5F, -3.0F));
		body.addOrReplaceChild("rib_2", rib, PartPose.offset(0.0F, 12.0F, -3.0F));
		body.addOrReplaceChild("debris_5", debris, PartPose.offsetAndRotation(-6.0F, 10.0F, 4.0F, 0.3F, 0.6F, 0.2F));
		body.addOrReplaceChild("debris_6", debris, PartPose.offsetAndRotation(6.0F, 5.0F, 3.0F, -0.2F, 0.3F, 0.5F));

		CubeListBuilder segment = CubeListBuilder.create().texOffs(40, 44).addBox(-2.0F, 0.0F, -2.0F, 4.0F, SEGMENT_LENGTH, 4.0F);
		for (int i = 0; i < TENTACLES; i++) {
			PartDefinition part = body.addOrReplaceChild("tentacle_" + i, segment,
				PartPose.offset(TENTACLE_ROOTS[i][0], 17.0F, TENTACLE_ROOTS[i][1]));
			for (int j = 1; j < SEGMENTS; j++) {
				part = part.addOrReplaceChild("segment", segment, PartPose.offset(0.0F, SEGMENT_LENGTH - 0.5F, 0.0F));
			}
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;

		this.body.xRot = 0.08F + Mth.sin(t * 0.05F) * 0.05F;

		this.rightHead.yRot = -0.3F + Mth.sin(t * 0.04F) * 0.35F;
		this.rightHead.xRot = Mth.sin(t * 0.07F) * 0.15F;
		this.leftHead.yRot = 0.3F + Mth.sin(t * 0.04F + 2.0F) * 0.35F;
		this.leftHead.xRot = Mth.sin(t * 0.07F + 1.0F) * 0.15F;

		for (int i = 0; i < TENTACLES; i++) {
			float phase = i * 1.7F;
			float side = TENTACLE_ROOTS[i][0] < 0 ? -1.0F : 1.0F;
			for (int j = 0; j < SEGMENTS; j++) {
				ModelPart part = this.tentacles[i][j];
				float wave = t * 0.09F + phase + j * 0.8F;
				part.xRot = 0.15F + Mth.sin(wave) * (0.25F + j * 0.1F);
				part.zRot = side * (j == 0 ? 0.25F : 0.0F) + Mth.cos(wave * 0.8F) * (0.15F + j * 0.08F);
			}
		}
	}
}
