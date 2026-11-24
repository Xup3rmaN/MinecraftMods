package dev.holoplane;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Состояние голограммы-плоскости: центр (cx, cz), уровень y, размеры по X и Z. */
public final class PlaneState {
	public static final int DEFAULT_SIZE = 20;
	public static final int MIN_SIZE = 1;
	public static final int MAX_SIZE = 128;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("holoplane.properties");

	public boolean visible = false;
	public boolean spawned = false;
	public int cx, y, cz;
	public int sizeX = DEFAULT_SIZE;
	public int sizeZ = DEFAULT_SIZE;
	public HoloBlock block = HoloBlock.REDSTONE;
	public int opacity = 60; // проценты
	/** Сдвиг оттенка текстуры, градусы 0..359. */
	public int hue = 180;
	/** Оттенок плавно бежит по кругу. */
	public boolean hueCycle = false;

	public void changeHue(int delta) {
		hue = Math.floorMod(hue + delta, 360);
	}

	public enum HoloBlock {
		REDSTONE("Блок редстоуна", Items.REDSTONE_BLOCK, "block/redstone_block", "block/redstone_block"),
		OAK_LOG("Дубовое бревно (log_oak_top)", Items.OAK_LOG, "block/oak_log_top", "block/oak_log"),
		NOTE_BLOCK("Нотный блок", Items.NOTE_BLOCK, "block/note_block", "block/note_block");

		public final String label;
		public final Item item;
		public final String topTexture;
		public final String sideTexture;

		HoloBlock(String label, Item item, String topTexture, String sideTexture) {
			this.label = label;
			this.item = item;
			this.topTexture = topTexture;
			this.sideTexture = sideTexture;
		}
	}

	public int minX() {
		return cx - sizeX / 2;
	}

	public int minZ() {
		return cz - sizeZ / 2;
	}

	public void spawnAt(double px, double py, double pz) {
		cx = (int) Math.floor(px);
		cz = (int) Math.floor(pz);
		y = (int) Math.floor(py + 1.0E-3);
		spawned = true;
		visible = true;
	}

	public void move(int dx, int dy, int dz) {
		cx += dx;
		y += dy;
		cz += dz;
	}

	public void resize(int dx, int dz) {
		sizeX = Math.clamp(sizeX + dx, MIN_SIZE, MAX_SIZE);
		sizeZ = Math.clamp(sizeZ + dz, MIN_SIZE, MAX_SIZE);
	}

	public void changeOpacity(int delta) {
		opacity = Math.clamp(opacity + delta, 10, 100);
	}

	public void load() {
		if (!Files.exists(FILE)) return;
		Properties p = new Properties();
		try (Reader r = Files.newBufferedReader(FILE)) {
			p.load(r);
			visible = Boolean.parseBoolean(p.getProperty("visible", "false"));
			spawned = Boolean.parseBoolean(p.getProperty("spawned", "false"));
			cx = Integer.parseInt(p.getProperty("cx", "0"));
			y = Integer.parseInt(p.getProperty("y", "0"));
			cz = Integer.parseInt(p.getProperty("cz", "0"));
			sizeX = Math.clamp(Integer.parseInt(p.getProperty("sizeX", "20")), MIN_SIZE, MAX_SIZE);
			sizeZ = Math.clamp(Integer.parseInt(p.getProperty("sizeZ", "20")), MIN_SIZE, MAX_SIZE);
			opacity = Math.clamp(Integer.parseInt(p.getProperty("opacity", "60")), 10, 100);
			hue = Math.floorMod(Integer.parseInt(p.getProperty("hue", "180")), 360);
			hueCycle = Boolean.parseBoolean(p.getProperty("hueCycle", "false"));
			try {
				block = HoloBlock.valueOf(p.getProperty("block", HoloBlock.REDSTONE.name()));
			} catch (IllegalArgumentException e) {
				block = HoloBlock.REDSTONE;
			}
		} catch (IOException | NumberFormatException e) {
			HoloPlaneClient.LOGGER.warn("Не удалось прочитать {}", FILE, e);
		}
	}

	public void save() {
		Properties p = new Properties();
		p.setProperty("visible", Boolean.toString(visible));
		p.setProperty("spawned", Boolean.toString(spawned));
		p.setProperty("cx", Integer.toString(cx));
		p.setProperty("y", Integer.toString(y));
		p.setProperty("cz", Integer.toString(cz));
		p.setProperty("sizeX", Integer.toString(sizeX));
		p.setProperty("sizeZ", Integer.toString(sizeZ));
		p.setProperty("opacity", Integer.toString(opacity));
		p.setProperty("hue", Integer.toString(hue));
		p.setProperty("hueCycle", Boolean.toString(hueCycle));
		p.setProperty("block", block.name());
		try (Writer w = Files.newBufferedWriter(FILE)) {
			p.store(w, "Holo Plane");
		} catch (IOException e) {
			HoloPlaneClient.LOGGER.warn("Не удалось сохранить {}", FILE, e);
		}
	}
}
