package com.bigbro.chickenbomber.client;

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
 * Винтовой самолётик: фюзеляж, крылья, хвост, лобовое стекло и пропеллер на носу (нос смотрит в -Z).
 * Раскладка текстуры повторяется в tools/textures.py — меняйте их вместе.
 */
public class AirplaneModel extends EntityModel<LivingEntityRenderState> {
	private final ModelPart propeller;

	public AirplaneModel(ModelPart root) {
		super(root);
		this.propeller = root.getChild("propeller");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("fuselage",
			CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -18.0F, 10.0F, 10.0F, 36.0F),
			PartPose.offset(0.0F, 16.0F, 0.0F));

		root.addOrReplaceChild("right_wing",
			CubeListBuilder.create().texOffs(0, 46).addBox(-30.0F, 0.0F, -6.0F, 30.0F, 2.0F, 12.0F),
			PartPose.offsetAndRotation(-5.0F, 17.0F, -4.0F, 0.0F, 0.0F, 0.06F));
		root.addOrReplaceChild("left_wing",
			CubeListBuilder.create().texOffs(0, 46).mirror().addBox(0.0F, 0.0F, -6.0F, 30.0F, 2.0F, 12.0F),
			PartPose.offsetAndRotation(5.0F, 17.0F, -4.0F, 0.0F, 0.0F, -0.06F));

		root.addOrReplaceChild("tailplane",
			CubeListBuilder.create().texOffs(0, 60).addBox(-12.0F, 0.0F, 0.0F, 24.0F, 2.0F, 8.0F),
			PartPose.offset(0.0F, 13.0F, 11.0F));
		root.addOrReplaceChild("fin",
			CubeListBuilder.create().texOffs(64, 60).addBox(-1.0F, -10.0F, 0.0F, 2.0F, 10.0F, 8.0F),
			PartPose.offset(0.0F, 12.0F, 10.0F));

		root.addOrReplaceChild("windshield",
			CubeListBuilder.create().texOffs(100, 10).addBox(-5.0F, -4.0F, 0.0F, 10.0F, 4.0F, 1.0F),
			PartPose.offsetAndRotation(0.0F, 11.0F, -7.0F, -0.35F, 0.0F, 0.0F));

		root.addOrReplaceChild("hub",
			CubeListBuilder.create().texOffs(100, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 3.0F),
			PartPose.offset(0.0F, 16.0F, -18.0F));
		CubeListBuilder blade = CubeListBuilder.create().texOffs(92, 0).addBox(-1.0F, -8.0F, -0.5F, 2.0F, 16.0F, 1.0F);
		PartDefinition propeller = root.addOrReplaceChild("propeller", blade, PartPose.offset(0.0F, 16.0F, -21.5F));
		propeller.addOrReplaceChild("blade_2", blade, PartPose.rotation(0.0F, 0.0F, Mth.HALF_PI));

		CubeListBuilder wheel = CubeListBuilder.create().texOffs(110, 0).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 3.0F, 3.0F);
		root.addOrReplaceChild("right_wheel", wheel, PartPose.offset(-4.0F, 21.0F, -8.0F));
		root.addOrReplaceChild("left_wheel", wheel, PartPose.offset(4.0F, 21.0F, -8.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		super.setupAnim(state);
		this.propeller.zRot = state.ageInTicks * 1.6F;
	}
}
