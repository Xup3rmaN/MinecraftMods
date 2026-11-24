package com.bigbro.chickenbomber.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final String MOD_ID = "chickenbomber";

	/** На какой высоте сидит курица: на верху фюзеляжа, в кабине. */
	public static final float COCKPIT_HEIGHT = 0.7F;

	public static final EntityType<Airplane> AIRPLANE = register(
		"airplane",
		EntityType.Builder.of(Airplane::new, MobCategory.MONSTER)
			.sized(3.0F, 1.4F)
			.eyeHeight(0.9F)
			.passengerAttachments(COCKPIT_HEIGHT)
			.clientTrackingRange(16)
			.noLootTable()
	);
	public static final EntityType<EggBomb> EGG_BOMB = register(
		"egg_bomb",
		EntityType.Builder.<EggBomb>of(EggBomb::new, MobCategory.MISC)
			.sized(0.4F, 0.4F)
			.clientTrackingRange(8)
			.updateInterval(10)
			.noLootTable()
	);

	private ModEntities() {
	}

	/** Загружает класс, а вместе с ним регистрирует сущности. Повторный вызов ничего не делает. */
	public static void init() {
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
