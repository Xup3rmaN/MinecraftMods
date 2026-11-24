package com.bigbro.freddybonnie.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.Vec3;

public final class ModEntities {
	public static final String MOD_ID = "freddybonnie";

	public static final EntityType<Freddy> FREDDY = register(
		"freddy",
		EntityType.Builder.of(Freddy::new, MobCategory.CREATURE)
			.sized(0.9F, 2.6F)
			.eyeHeight(2.2F)
			// Бонни сидит на плечах: чуть позади головы, чтобы не проваливаться в неё
			.passengerAttachments(new Vec3(0.0, 1.2, -0.45))
			.clientTrackingRange(10)
			.noLootTable()
	);
	public static final EntityType<Bonnie> BONNIE = register(
		"bonnie",
		EntityType.Builder.of(Bonnie::new, MobCategory.CREATURE)
			.sized(0.6F, 1.8F)
			.eyeHeight(1.6F)
			.clientTrackingRange(10)
			.noLootTable()
	);
	public static final EntityType<PetDragon> PET_DRAGON = register(
		"pet_dragon",
		EntityType.Builder.of(PetDragon::new, MobCategory.MONSTER)
			.fireImmune()
			.sized(5.0F, 3.0F)
			.clientTrackingRange(10)
			.noLootTable()
	);

	private ModEntities() {
	}

	/** Загружает класс, а вместе с ним регистрирует сущности. Повторный вызов ничего не делает. */
	public static void init() {
	}

	/**
	 * Яйцо Фредди и Бонни. Это ванильное яйцо призыва свиньи с Фредди внутри:
	 * свой предмет без Fabric API остался бы без иконки.
	 */
	public static ItemStack createEgg() {
		ItemStack egg = new ItemStack(Items.PIG_SPAWN_EGG);
		egg.set(DataComponents.ENTITY_DATA, TypedEntityData.of(FREDDY, new CompoundTag()));
		egg.set(DataComponents.ITEM_NAME, Component.literal("Яйцо Фредди и Бонни"));
		return egg;
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
