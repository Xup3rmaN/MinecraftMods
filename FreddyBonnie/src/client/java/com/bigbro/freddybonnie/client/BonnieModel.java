package com.bigbro.freddybonnie.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;

/**
 * Кролик Бонни с длинными ушами и балалайкой в правой руке.
 * Раскладка текстуры повторяется в tools/textures.py — меняйте их вместе.
 */
public class BonnieModel extends HumanoidModel<HumanoidRenderState> {
	/** Наклон правой руки: занесена над головой и опущена после удара. */
	private static final float RAISED = -2.8F;
	private static final float SLAMMED = -0.7F;

	public BonnieModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
			PartPose.ZERO);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
		CubeListBuilder ear = CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -10.0F, -0.5F, 2.0F, 10.0F, 1.0F);
		head.addOrReplaceChild("right_ear", ear, PartPose.offsetAndRotation(-2.0F, -7.5F, 0.0F, -0.1F, 0.0F, -0.15F));
		head.addOrReplaceChild("left_ear", ear, PartPose.offsetAndRotation(2.0F, -7.5F, 0.0F, -0.1F, 0.0F, 0.15F));
		head.addOrReplaceChild("snout",
			CubeListBuilder.create().texOffs(38, 0).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, -1.5F, -4.0F));

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
			PartPose.ZERO);
		body.addOrReplaceChild("bow_tie",
			CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 0.5F, -2.0F));

		PartDefinition rightArm = root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-5.0F, 2.0F, 0.0F));
		// Балалайка: гриф в кулаке, треугольный корпус на конце
		rightArm.addOrReplaceChild("balalaika",
			CubeListBuilder.create()
				.texOffs(72, 16).addBox(-0.5F, -0.5F, -8.0F, 1.0F, 1.0F, 8.0F)
				.texOffs(90, 16).addBox(-0.5F, -3.5F, -14.0F, 1.0F, 7.0F, 6.0F),
			PartPose.offset(-1.0F, 9.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(5.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(56, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(56, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(HumanoidRenderState state) {
		super.setupAnim(state);
		if (state.isPassenger) {
			// Сидит на плечах: ноги вперёд и широко в стороны, чтобы обхватить голову Фредди
			this.rightLeg.xRot = -1.45F;
			this.rightLeg.yRot = 0.6F;
			this.leftLeg.xRot = -1.45F;
			this.leftLeg.yRot = -0.6F;
			// Левой рукой держится за голову Фредди
			this.leftArm.xRot = -1.1F;
		}
		// Балалайка занесена над головой, а при ударе с размаху опускается Фредди на макушку.
		// Ванильный «тычок» рукой заменяем целиком, поэтому сбрасываем и его сдвиги
		float slam = state.attackTime > 0.0F ? Mth.sin(state.attackTime * Mth.PI) : 0.0F;
		this.body.yRot = 0.0F;
		this.rightArm.x = -5.0F;
		this.rightArm.z = 0.0F;
		this.rightArm.xRot = Mth.lerp(slam, RAISED, SLAMMED);
		this.rightArm.yRot = 0.0F;
		this.rightArm.zRot = 0.1F;
	}
}
