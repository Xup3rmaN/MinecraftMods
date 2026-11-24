package com.bigbro.chickenbomber.client;

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

	public static final Identifier AIRPLANE = id("airplane");

	private static final Identifier[] ALL = {AIRPLANE};
	private static boolean loaded;

	private ModTextures() {
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath("chickenbomber", "textures/entity/" + name + ".png");
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
					LOGGER.error("Chicken Bomber: texture {} is missing from the mod jar", path);
					continue;
				}
				NativeImage image = NativeImage.read(in);
				textureManager.register(texture, new DynamicTexture(texture::toString, image));
			} catch (IOException e) {
				LOGGER.error("Chicken Bomber: failed to load texture {}", path, e);
			}
		}
	}
}
