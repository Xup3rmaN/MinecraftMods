package dev.holoplane;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HoloPlaneClient implements ClientModInitializer {
	public static final String MOD_ID = "holoplane";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));

	public static final PlaneState STATE = new PlaneState();
	private final PlaneRenderer renderer = new PlaneRenderer();

	private static boolean dirty;
	private static int saveTimer;

	private KeyBinding menu, toggle, spawn, blend;
	private HeldKey up, down, forward, back, left, right, grow, shrink;

	@Override
	public void onInitializeClient() {
		STATE.load();

		menu = key("menu", GLFW.GLFW_KEY_B);
		toggle = key("toggle", GLFW.GLFW_KEY_N);
		spawn = key("spawn", GLFW.GLFW_KEY_UNKNOWN);
		blend = key("blend", GLFW.GLFW_KEY_V);
		up = new HeldKey(key("up", GLFW.GLFW_KEY_PAGE_UP));
		down = new HeldKey(key("down", GLFW.GLFW_KEY_PAGE_DOWN));
		forward = new HeldKey(key("forward", GLFW.GLFW_KEY_UP));
		back = new HeldKey(key("back", GLFW.GLFW_KEY_DOWN));
		left = new HeldKey(key("left", GLFW.GLFW_KEY_LEFT));
		right = new HeldKey(key("right", GLFW.GLFW_KEY_RIGHT));
		grow = new HeldKey(key("grow", GLFW.GLFW_KEY_EQUAL));
		shrink = new HeldKey(key("shrink", GLFW.GLFW_KEY_MINUS));

		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		WorldRenderEvents.END_MAIN.register(context -> renderer.render(context, STATE));
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> STATE.save());
	}

	private static KeyBinding key(String name, int code) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + MOD_ID + "." + name, code, CATEGORY));
	}

	private void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) return;

		while (menu.wasPressed()) {
			// Первое открытие меню сразу ставит плоскость 20×20 у игрока
			if (!STATE.spawned) STATE.spawnAt(player.getX(), player.getY(), player.getZ());
			client.setScreen(new PlaneScreen());
		}
		while (toggle.wasPressed()) {
			if (!STATE.spawned) STATE.spawnAt(player.getX(), player.getY(), player.getZ());
			else STATE.visible = !STATE.visible;
			changed(STATE.visible ? "Голограмма включена" : "Голограмма скрыта");
		}
		while (blend.wasPressed()) {
			STATE.hueCycle = !STATE.hueCycle;
			changed(STATE.hueCycle ? "Переливание: вкл" : "Переливание: выкл");
		}
		while (spawn.wasPressed()) {
			STATE.spawnAt(player.getX(), player.getY(), player.getZ());
			changed("Голограмма перенесена к игроку");
		}

		if (STATE.visible && STATE.spawned) {
			Direction facing = player.getHorizontalFacing();
			Direction side = facing.rotateYClockwise();
			if (up.fire()) { STATE.move(0, 1, 0); changed(null); }
			if (down.fire()) { STATE.move(0, -1, 0); changed(null); }
			if (forward.fire()) moveBy(facing, 1);
			if (back.fire()) moveBy(facing, -1);
			if (right.fire()) moveBy(side, 1);
			if (left.fire()) moveBy(side, -1);
			if (grow.fire()) { STATE.resize(2, 2); changed(null); }
			if (shrink.fire()) { STATE.resize(-2, -2); changed(null); }
		} else {
			for (HeldKey k : new HeldKey[]{up, down, forward, back, left, right, grow, shrink}) k.reset();
		}

		if (dirty && ++saveTimer >= 40) {
			STATE.save();
			dirty = false;
			saveTimer = 0;
		}
	}

	private void moveBy(Direction dir, int amount) {
		STATE.move(dir.getOffsetX() * amount, 0, dir.getOffsetZ() * amount);
		changed(null);
	}

	/** Помечает состояние для сохранения и показывает инфо над хотбаром. */
	public static void changed(String prefix) {
		dirty = true;
		saveTimer = 0;
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		String info = String.format("§bHolo Plane§r  %s  |  %d×%d  |  Y %d",
				STATE.block.label, STATE.sizeX, STATE.sizeZ, STATE.y);
		player.sendMessage(Text.literal(prefix == null ? info : prefix + "  —  " + info), true);
	}

	/** Клавиша с автоповтором при удержании. */
	private static final class HeldKey {
		private static final int REPEAT_DELAY = 8;
		private static final int REPEAT_INTERVAL = 2;
		private final KeyBinding key;
		private int held;

		HeldKey(KeyBinding key) {
			this.key = key;
		}

		boolean fire() {
			boolean pressedNow = false;
			while (key.wasPressed()) pressedNow = true;
			if (!key.isPressed()) {
				int prev = held;
				held = 0;
				return pressedNow && prev == 0;
			}
			held++;
			return held == 1 || (held > REPEAT_DELAY && (held - REPEAT_DELAY) % REPEAT_INTERVAL == 0);
		}

		void reset() {
			while (key.wasPressed()) { }
			held = 0;
		}
	}
}
