package com.bigbro.mobtower.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Без Fabric API ресурсы мода не попадают в ресурс-паки игры, поэтому текстуры
 * читаем прямо из jar и регистрируем вручную. Такие текстуры переживают F3+T.
 */
public final class ModTextures {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Identifier WITHER_STORM = id("wither_storm");
	public static final Identifier WITHER_STORM_EYES = id("wither_storm_eyes");
	public static final Identifier HEROBRINE = id("herobrine");
	public static final Identifier HEROBRINE_EYES = id("herobrine_eyes");
	public static final Identifier GLOBE = id("globe");

	private static final Identifier[] ALL = {WITHER_STORM, WITHER_STORM_EYES, HEROBRINE, HEROBRINE_EYES, GLOBE};
	private static boolean loaded;

	private ModTextures() {
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath("mobtower", "textures/entity/" + name + ".png");
	}

	/** Вызывать из потока отрисовки: текстуры сразу загружаются в видеопамять. */
	public static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;

		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		for (Identifier texture : ALL) {
			String path = "/assets/" + texture.getNamespace() + "/" + texture.getPath();
			try (InputStream in = ModTextures.class.getResourceAsStream(path)) {
				if (in == null) {
					LOGGER.error("Mob Tower: texture {} is missing from the mod jar", path);
					continue;
				}
				NativeImage image = NativeImage.read(in);
				textureManager.register(texture, new DynamicTexture(texture::toString, image));
			} catch (IOException e) {
				LOGGER.error("Mob Tower: failed to load texture {}", path, e);
			}
		}
	}
}
