package com.bigbro.freddybonnie.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Мишка Фредди: широкая голова с ушами и мордой, цилиндр, бабочка, пузо.
 * Части названы как у гуманоида, поэтому ходьба и замах рукой работают по-ванильному.
 * Раскладка текстуры повторяется в tools/textures.py — меняйте их вместе.
 */
public class FreddyModel extends HumanoidModel<HumanoidRenderState> {
	public FreddyModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 9.0F, 10.0F),
			PartPose.ZERO);
		// «Шляпа» гуманоида у Фредди — настоящий цилиндр
		head.addOrReplaceChild("hat",
			CubeListBuilder.create()
				.texOffs(40, 0).addBox(-3.5F, -1.0F, -3.5F, 7.0F, 1.0F, 7.0F)
				.texOffs(68, 0).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 5.0F, 5.0F),
			PartPose.offset(0.0F, -9.0F, 0.0F));
		CubeListBuilder ear = CubeListBuilder.create().texOffs(88, 0).addBox(-1.5F, -3.0F, -0.5F, 3.0F, 3.0F, 1.0F);
		head.addOrReplaceChild("right_ear", ear, PartPose.offset(-4.0F, -8.5F, 0.0F));
		head.addOrReplaceChild("left_ear", ear, PartPose.offset(4.0F, -8.5F, 0.0F));
		head.addOrReplaceChild("snout",
			CubeListBuilder.create()
				.texOffs(96, 0).addBox(-2.5F, -3.0F, -2.0F, 5.0F, 3.0F, 2.0F)
				.texOffs(110, 0).addBox(-1.0F, -3.3F, -2.4F, 2.0F, 1.0F, 1.0F),
			PartPose.offset(0.0F, -1.0F, -5.0F));

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 19).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F),
			PartPose.ZERO);
		body.addOrReplaceChild("bow_tie",
			CubeListBuilder.create().texOffs(32, 19).addBox(-2.5F, 0.0F, -1.0F, 5.0F, 2.0F, 1.0F),
			PartPose.offset(0.0F, 0.5F, -3.0F));

		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(44, 19).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(-7.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(44, 19).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
			PartPose.offset(7.0F, 2.0F, 0.0F));
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(60, 19).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
			PartPose.offset(-2.6F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(60, 19).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
			PartPose.offset(2.6F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(HumanoidRenderState state) {
		super.setupAnim(state);
		// Левой рукой держит поводок с драконом — чуть вперёд и вверх
		this.leftArm.xRot = -0.9F;
		this.leftArm.zRot = 0.15F;
	}
}
