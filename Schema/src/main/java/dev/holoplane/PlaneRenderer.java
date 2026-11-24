package dev.holoplane;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Рисует плоскость полупрозрачных блоков с текстурами из атласа блоков — как голограмма в Schematica.
 * Шейдер hologram.fsh сдвигает hue текстуры, поэтому голограмма не сливается с такими же настоящими блоками.
 * Клетки, где в мире уже стоит твёрдый блок, не рисуются.
 */
public final class PlaneRenderer {
	private static final RenderPipeline PIPELINE = RenderPipeline.builder()
			.withLocation(Identifier.of(HoloPlaneClient.MOD_ID, "pipeline/hologram"))
			.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
			.withUniform("Projection", UniformType.UNIFORM_BUFFER)
			.withVertexShader("core/position_tex_color")
			.withFragmentShader(Identifier.of(HoloPlaneClient.MOD_ID, "core/hologram"))
			.withSampler("Sampler0")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withDepthWrite(false)
			.withCull(false)
			.withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
			.withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
			.build();

	private static final RenderLayer LAYER = RenderLayer.of(HoloPlaneClient.MOD_ID + "_hologram",
			RenderSetup.builder(PIPELINE)
					.texture("Sampler0", SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
					.translucent()
					.build());

	private static final float INSET = 0.002f;
	/** Минимальная насыщенность — чтобы серые места текстуры тоже окрашивались. */
	private static final float MIN_SATURATION = 0.35f;
	/** Полный круг оттенков при переливании, мс. */
	private static final long CYCLE_MS = 6000;

	/** Затенение граней как у ванильных блоков: верх, низ, север/юг, запад/восток. */
	private static final float SHADE_UP = 1.0f, SHADE_DOWN = 0.5f, SHADE_NS = 0.8f, SHADE_WE = 0.6f;

	private boolean[] show = new boolean[0];

	public void render(WorldRenderContext context, PlaneState state) {
		if (!state.visible || !state.spawned) return;
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = client.world;
		if (world == null) return;

		SpriteAtlasTexture atlas = client.getAtlasManager().getAtlasTexture(Atlases.BLOCKS);
		Sprite top = atlas.getSprite(Identifier.ofVanilla(state.block.topTexture));
		Sprite side = atlas.getSprite(Identifier.ofVanilla(state.block.sideTexture));

		int sx = state.sizeX, sz = state.sizeZ;
		int minX = state.minX(), minZ = state.minZ(), y = state.y;
		if (show.length < sx * sz) show = new boolean[sx * sz];

		// Голограмма видна только там, куда можно поставить блок (воздух, вода, трава…)
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int i = 0; i < sx; i++) {
			for (int j = 0; j < sz; j++) {
				pos.set(minX + i, y, minZ + j);
				show[i * sz + j] = world.getBlockState(pos).isReplaceable();
			}
		}

		Vec3d cam = context.worldState().cameraRenderState.pos;
		Matrix4f m = context.matrices().peek().getPositionMatrix();
		VertexConsumerProvider consumers = context.consumers();
		VertexConsumer buf = consumers.getBuffer(LAYER);

		float hue = state.hue / 360f;
		if (state.hueCycle) hue += (Util.getMeasuringTimeMs() % CYCLE_MS) / (float) CYCLE_MS;
		hue -= (float) Math.floor(hue);
		int alpha = Math.round(state.opacity * 2.55f);
		int cUp = color(hue, SHADE_UP, alpha);
		int cDown = color(hue, SHADE_DOWN, alpha);
		int cNS = color(hue, SHADE_NS, alpha);
		int cWE = color(hue, SHADE_WE, alpha);

		float y0 = (float) (y - cam.y) + INSET, y1 = (float) (y + 1 - cam.y) - INSET;
		for (int i = 0; i < sx; i++) {
			for (int j = 0; j < sz; j++) {
				if (!show[i * sz + j]) continue;
				float x0 = (float) (minX + i - cam.x) + INSET, x1 = (float) (minX + i + 1 - cam.x) - INSET;
				float z0 = (float) (minZ + j - cam.z) + INSET, z1 = (float) (minZ + j + 1 - cam.z) - INSET;

				// верх и низ
				face(buf, m, cUp, top, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
				face(buf, m, cDown, top, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
				// боковые грани — только там, где рядом нет другой клетки голограммы
				if (!shown(i, j - 1, sx, sz)) face(buf, m, cNS, side, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
				if (!shown(i, j + 1, sx, sz)) face(buf, m, cNS, side, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
				if (!shown(i - 1, j, sx, sz)) face(buf, m, cWE, side, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
				if (!shown(i + 1, j, sx, sz)) face(buf, m, cWE, side, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
			}
		}

		if (consumers instanceof VertexConsumerProvider.Immediate immediate) immediate.draw(LAYER);
	}

	/** Цвет вершины — это параметры шейдера: r = сдвиг hue, g = мин. насыщенность, b = затенение, a = прозрачность. */
	private static int color(float hue, float shade, int alpha) {
		int r = Math.round(hue * 255), g = Math.round(MIN_SATURATION * 255), b = Math.round(shade * 255);
		return (alpha << 24) | (r << 16) | (g << 8) | b;
	}

	private boolean shown(int i, int j, int sx, int sz) {
		return i >= 0 && j >= 0 && i < sx && j < sz && show[i * sz + j];
	}

	/** Четырёхугольник: вершины по порядку верх-лево, низ-лево, низ-право, верх-право текстуры. */
	private static void face(VertexConsumer buf, Matrix4f m, int color, Sprite s,
							 float ax, float ay, float az, float bx, float by, float bz,
							 float cx, float cy, float cz, float dx, float dy, float dz) {
		float u0 = s.getMinU(), u1 = s.getMaxU(), v0 = s.getMinV(), v1 = s.getMaxV();
		buf.vertex(m, ax, ay, az).texture(u0, v0).color(color);
		buf.vertex(m, bx, by, bz).texture(u0, v1).color(color);
		buf.vertex(m, cx, cy, cz).texture(u1, v1).color(color);
		buf.vertex(m, dx, dy, dz).texture(u1, v0).color(color);
	}
}
