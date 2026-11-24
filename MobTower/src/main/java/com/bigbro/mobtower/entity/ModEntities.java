package com.bigbro.mobtower.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final String MOD_ID = "mobtower";

	/** Высота, на которой стоит Херобрин, в блоках: ровно на макушке центральной головы. */
	public static final float STORM_TOP = 9.0F;

	public static final EntityType<WitherStorm> WITHER_STORM = register(
		"wither_storm",
		EntityType.Builder.of(WitherStorm::new, MobCategory.MONSTER)
			.sized(7.0F, STORM_TOP)
			.eyeHeight(7.5F)
			.passengerAttachments(STORM_TOP)
			.fireImmune()
			.clientTrackingRange(16)
			.noLootTable()
	);
	public static final EntityType<Herobrine> HEROBRINE = register(
		"herobrine",
		EntityType.Builder.of(Herobrine::new, MobCategory.MONSTER)
			.sized(0.6F, 1.95F)
			.eyeHeight(1.74F)
			.fireImmune()
			.clientTrackingRange(16)
			.noLootTable()
	);
	public static final EntityType<GlobeEntity> GLOBE = register(
		"globe",
		EntityType.Builder.<GlobeEntity>of(GlobeEntity::new, MobCategory.MISC)
			.sized(0.75F, 0.75F)
			.clientTrackingRange(8)
			.updateInterval(10)
			.noLootTable()
	);

	private ModEntities() {
	}

	/** Загружает класс, а вместе с ним регистрирует сущности. Повторный вызов ничего не делает. */
	public static void init() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
